package com.istiak.auraplayer

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlin.math.ceil

/**
 * Current brute-force lockout status for the Private Vault PIN gate.
 */
data class VaultLockoutState(
    val failedAttempts: Int = 0,
    val maxAttempts: Int = BiometricAuth.MAX_FAILED_ATTEMPTS,
    val lockoutUntilEpochMs: Long = 0L,
    val remainingLockoutSeconds: Int = 0
) {
    val isLockedOut: Boolean
        get() = remainingLockoutSeconds > 0

    val remainingAttempts: Int
        get() = (maxAttempts - failedAttempts).coerceAtLeast(0)

    val cooldownProgress: Float
        get() {
            if (!isLockedOut) return 0f
            val totalSeconds = (BiometricAuth.LOCKOUT_DURATION_MS / 1000f).coerceAtLeast(1f)
            return (remainingLockoutSeconds / totalSeconds).coerceIn(0f, 1f)
        }
}

/**
 * Result of verifying an entered Vault PIN with PBKDF2-HMAC-SHA256, constant-time digest comparison,
 * and 5-attempt / 30-second brute-force lockout protection.
 */
sealed class VaultPinVerificationResult {
    data object Success : VaultPinVerificationResult()
    data class InvalidPin(val failedAttempts: Int, val remainingAttempts: Int) : VaultPinVerificationResult()
    data class LockedOut(val remainingSeconds: Int, val lockoutUntilEpochMs: Long) : VaultPinVerificationResult()
}

/**
 * Hardened Private Vault Authentication & Cryptography Layer (`BiometricAuth.kt`):
 * 1. Zero plaintext PIN storage:
 *    - Generates a fresh 16-byte cryptographic salt (`SecureRandom`) per PIN.
 *    - Hashes the PIN using `PBKDF2WithHmacSHA256` (65,536 iterations, 256-bit derived key;
 *      safe `PBKDF2WithHmacSHA1` / iterated salted SHA-256 fallback if a legacy JCA provider lacks HmacSHA256).
 *    - Verifies using constant-time `MessageDigest.isEqual`.
 *    - Automatically migrates any legacy plaintext PIN (`vault_fallback_pin` or DataStore plaintext keys)
 *      on first check/unlock and immediately deletes the plaintext entry.
 * 2. Encrypted Storage via AndroidX Security (`EncryptedSharedPreferences`) + Android Keystore:
 *    - Persists vault credential hashes and brute-force lockout counters inside
 *      `EncryptedSharedPreferences` backed by an Android Keystore `MasterKey` (`AES256_GCM`).
 *    - Additionally wraps vault secrets (recovery answer, vault metadata cache, and backup PIN hash record)
 *      with an Android Keystore `AES/GCM/NoPadding` 256-bit key (128-bit auth tag + fresh 12-byte IV).
 * 3. Brute-force lockout protection:
 *    - After 5 consecutive failed PIN attempts, enforces a 30-second cooldown persisted in encrypted storage.
 * 4. BiometricPrompt:
 *    - Bound to `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` on Android 11+ (with compliant
 *      `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` fallback on API 24–29) as an optional quick unlock.
 */
object BiometricAuth {

    private const val ENCRYPTED_PREFS_NAME = "aura_vault_encrypted_prefs"
    private const val LEGACY_PREFS_NAME = "aura_vault_security"
    private const val KEY_LEGACY_FALLBACK_PIN = "vault_fallback_pin"
    private const val KEY_ENCRYPTED_PIN_RECORD = "vault_pin_hash_enc_v2"
    private const val KEY_ENCRYPTED_LOCKOUT = "vault_lockout_enc_v2"

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "aura_vault_aes256_gcm_master_key_v1"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ENC_PREFIX_KEYSTORE = "ks_gcm_v1:"
    private const val ENC_PREFIX_FALLBACK_HASH = "pbkdf2_raw_v1:"

    private const val SALT_BYTES = 16
    private const val PBKDF2_ITERATIONS = 65_536
    private const val PBKDF2_KEY_BITS = 256
    private const val ALGO_PBKDF2_SHA256 = "PBKDF2WithHmacSHA256"
    private const val ALGO_PBKDF2_SHA1 = "PBKDF2WithHmacSHA1"

    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 30_000L

    private val secureRandom = SecureRandom()

    @Volatile
    private var cachedSecurePrefs: Triple<Int, SharedPreferences, Boolean>? = null

    private fun allowedAuthenticators(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }
    }

    /**
     * Opens the Android Keystore-backed `EncryptedSharedPreferences` store, caching per Application
     * context instance. Falls back gracefully to private SharedPreferences only on test JVMs
     * (e.g., Robolectric) where the hardware-backed `AndroidKeyStore` provider is unavailable.
     */
    @Synchronized
    private fun getSecurePrefs(context: Context): Pair<SharedPreferences, Boolean> {
        val appContext = context.applicationContext ?: context
        val contextId = System.identityHashCode(appContext)
        cachedSecurePrefs?.let { (cachedId, prefs, isEncrypted) ->
            if (cachedId == contextId) return prefs to isEncrypted
        }

        val encrypted = runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()

        val resolved = if (encrypted != null) {
            Triple(contextId, encrypted, true)
        } else {
            val fallback = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            Triple(contextId, fallback, false)
        }
        cachedSecurePrefs = resolved
        return resolved.second to resolved.third
    }

    private fun getLegacyPrefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext ?: context
        return appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isKeystoreProtected(context: Context? = null): Boolean {
        if (context != null) {
            val (_, usingEncryptedPrefs) = getSecurePrefs(context)
            if (usingEncryptedPrefs) return true
        }
        return getOrCreateKeystoreSecretKey() != null
    }

    fun canUseSystemBiometricOrCredential(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(allowedAuthenticators()) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Private Vault",
        subtitle: String = "Use fingerprint, face unlock, or device credential to open your vault",
        onSuccess: () -> Unit,
        onFallbackPinRequired: () -> Unit,
        onError: (String) -> Unit,
        onPromptFinished: () -> Unit = {}
    ) {
        val authenticators = allowedAuthenticators()
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(authenticators)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onPromptFinished()
            onFallbackPinRequired()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                resetLockoutState(activity)
                onPromptFinished()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onPromptFinished()
                if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                    errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                    errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                ) {
                    onFallbackPinRequired()
                } else if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onFallbackPinRequired()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onPromptFinished()
                onError("Biometric not recognized. Try again or enter your Vault PIN.")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        runCatching {
            BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
        }.onFailure {
            onPromptFinished()
            onFallbackPinRequired()
        }
    }

    /**
     * Automatically migrates any legacy plaintext PIN stored under `vault_fallback_pin`
     * (and any pre-EncryptedSharedPreferences hash/lockout records) into `EncryptedSharedPreferences`
     * with a salted PBKDF2-HMAC-SHA256 hash wrapped in Android Keystore AES-256-GCM, then immediately
     * deletes the plaintext key from legacy SharedPreferences.
     *
     * @return The encrypted salted-hash record if a PIN exists (migrated or already hardened), else null.
     */
    @Synchronized
    fun migrateLegacyPinIfNeeded(context: Context): String? {
        val (securePrefs, isUsingEncryptedStore) = getSecurePrefs(context)
        val legacyPrefs = getLegacyPrefs(context)

        var existingEncrypted = securePrefs.getString(KEY_ENCRYPTED_PIN_RECORD, null)
        val legacyPlaintextPin = legacyPrefs.getString(KEY_LEGACY_FALLBACK_PIN, null)
        val legacyHashedRecord = if (isUsingEncryptedStore) {
            legacyPrefs.getString(KEY_ENCRYPTED_PIN_RECORD, null)
        } else {
            null
        }
        val legacyLockoutRecord = if (isUsingEncryptedStore) {
            legacyPrefs.getString(KEY_ENCRYPTED_LOCKOUT, null)
        } else {
            null
        }

        // Move legacy hash/lockout records from plain SharedPreferences into EncryptedSharedPreferences
        if (isUsingEncryptedStore && existingEncrypted.isNullOrBlank() && !legacyHashedRecord.isNullOrBlank()) {
            securePrefs.edit(commit = true) {
                putString(KEY_ENCRYPTED_PIN_RECORD, legacyHashedRecord)
            }
            existingEncrypted = legacyHashedRecord
        }
        if (isUsingEncryptedStore && !legacyLockoutRecord.isNullOrBlank() &&
            securePrefs.getString(KEY_ENCRYPTED_LOCKOUT, null).isNullOrBlank()
        ) {
            securePrefs.edit(commit = true) {
                putString(KEY_ENCRYPTED_LOCKOUT, legacyLockoutRecord)
            }
        }

        // Migrate any legacy plaintext PIN into salted PBKDF2 hash and immediately purge plaintext
        if (!legacyPlaintextPin.isNullOrEmpty()) {
            val hardenedRecord = if (existingEncrypted.isNullOrBlank()) {
                val created = createEncryptedPinRecord(legacyPlaintextPin)
                securePrefs.edit(commit = true) {
                    putString(KEY_ENCRYPTED_PIN_RECORD, created)
                }
                created
            } else {
                existingEncrypted
            }
            legacyPrefs.edit(commit = true) {
                remove(KEY_LEGACY_FALLBACK_PIN)
                if (isUsingEncryptedStore) {
                    remove(KEY_ENCRYPTED_PIN_RECORD)
                    remove(KEY_ENCRYPTED_LOCKOUT)
                }
            }
            return hardenedRecord
        }

        // Clean up any leftover non-encrypted records after migrating into EncryptedSharedPreferences
        if (isUsingEncryptedStore && (!legacyHashedRecord.isNullOrBlank() || !legacyLockoutRecord.isNullOrBlank())) {
            legacyPrefs.edit(commit = true) {
                remove(KEY_ENCRYPTED_PIN_RECORD)
                remove(KEY_ENCRYPTED_LOCKOUT)
            }
        }

        return existingEncrypted?.takeIf { it.isNotBlank() }
    }

    /**
     * Migrates a legacy plaintext PIN discovered in DataStore into PBKDF2-HMAC-SHA256 +
     * `EncryptedSharedPreferences` without overwriting an already hardened PIN record.
     */
    @Synchronized
    fun migrateLegacyPlaintextPin(context: Context, legacyPlaintextPin: String): String? {
        if (legacyPlaintextPin.isBlank()) return null
        val existing = migrateLegacyPinIfNeeded(context)
        if (!existing.isNullOrBlank()) return existing
        return setCustomVaultPin(context, legacyPlaintextPin)
    }

    /**
     * Ensures a DataStore-mirrored encrypted PIN record is synchronized into local encrypted storage
     * if local prefs were cleared while DataStore still holds the salted PBKDF2 record.
     */
    @Synchronized
    fun syncEncryptedPinRecordFromDataStore(context: Context, dataStoreRecord: String?) {
        if (dataStoreRecord.isNullOrBlank()) return
        val (securePrefs, _) = getSecurePrefs(context)
        if (securePrefs.getString(KEY_ENCRYPTED_PIN_RECORD, null).isNullOrBlank()) {
            securePrefs.edit {
                putString(KEY_ENCRYPTED_PIN_RECORD, dataStoreRecord)
            }
        }
        val legacyPrefs = getLegacyPrefs(context)
        if (legacyPrefs.contains(KEY_LEGACY_FALLBACK_PIN)) {
            legacyPrefs.edit(commit = true) {
                remove(KEY_LEGACY_FALLBACK_PIN)
            }
        }
    }

    fun getEncryptedPinRecord(context: Context): String? {
        return migrateLegacyPinIfNeeded(context)
    }

    fun hasCustomVaultPin(context: Context): Boolean {
        return !migrateLegacyPinIfNeeded(context).isNullOrBlank()
    }

    /**
     * Hashes [pin] with a fresh 16-byte cryptographic salt (`SecureRandom`) using `PBKDF2WithHmacSHA256`,
     * wraps the resulting salt+hash with Android Keystore AES-256-GCM, persists it inside
     * `EncryptedSharedPreferences` without ever writing plaintext, deletes any legacy plaintext key,
     * and resets any prior lockout state.
     *
     * @return The encrypted salted-hash record (safe to mirror in DataStore as well).
     */
    @Synchronized
    fun setCustomVaultPin(context: Context, pin: String): String {
        val encryptedRecord = createEncryptedPinRecord(pin)
        val (securePrefs, isUsingEncryptedStore) = getSecurePrefs(context)
        securePrefs.edit(commit = true) {
            putString(KEY_ENCRYPTED_PIN_RECORD, encryptedRecord)
            remove(KEY_LEGACY_FALLBACK_PIN)
            remove(KEY_ENCRYPTED_LOCKOUT)
        }
        if (isUsingEncryptedStore) {
            val legacyPrefs = getLegacyPrefs(context)
            if (legacyPrefs.contains(KEY_LEGACY_FALLBACK_PIN) ||
                legacyPrefs.contains(KEY_ENCRYPTED_PIN_RECORD) ||
                legacyPrefs.contains(KEY_ENCRYPTED_LOCKOUT)
            ) {
                legacyPrefs.edit(commit = true) {
                    remove(KEY_LEGACY_FALLBACK_PIN)
                    remove(KEY_ENCRYPTED_PIN_RECORD)
                    remove(KEY_ENCRYPTED_LOCKOUT)
                }
            }
        }
        return encryptedRecord
    }

    /**
     * Verifies [pin] against the stored PBKDF2 salted hash using constant-time `MessageDigest.isEqual`,
     * enforcing 5-attempt / 30-second brute-force lockout protection.
     */
    fun verifyCustomVaultPin(context: Context, pin: String): Boolean {
        return verifyVaultPinWithLockout(context, pin) is VaultPinVerificationResult.Success
    }

    @Synchronized
    fun verifyVaultPinWithLockout(
        context: Context,
        pin: String,
        nowMs: Long = System.currentTimeMillis()
    ): VaultPinVerificationResult {
        val encryptedRecord = migrateLegacyPinIfNeeded(context)
        val lockout = getLockoutState(context, nowMs)
        if (lockout.isLockedOut) {
            return VaultPinVerificationResult.LockedOut(
                remainingSeconds = lockout.remainingLockoutSeconds,
                lockoutUntilEpochMs = lockout.lockoutUntilEpochMs
            )
        }

        val decryptedHashRecord = encryptedRecord?.let { unwrapEncryptedPayload(it) }
        val isMatch = if (!decryptedHashRecord.isNullOrBlank()) {
            verifyPinHashRecord(pin, decryptedHashRecord)
        } else {
            false
        }

        if (isMatch) {
            resetLockoutState(context)
            return VaultPinVerificationResult.Success
        }

        val currentAttempts = if (lockout.lockoutUntilEpochMs in 1..nowMs) 0 else lockout.failedAttempts
        val updatedAttempts = currentAttempts + 1
        return if (updatedAttempts >= MAX_FAILED_ATTEMPTS) {
            val lockoutUntil = nowMs + LOCKOUT_DURATION_MS
            saveLockoutState(context, updatedAttempts, lockoutUntil)
            VaultPinVerificationResult.LockedOut(
                remainingSeconds = (LOCKOUT_DURATION_MS / 1000L).toInt(),
                lockoutUntilEpochMs = lockoutUntil
            )
        } else {
            saveLockoutState(context, updatedAttempts, 0L)
            VaultPinVerificationResult.InvalidPin(
                failedAttempts = updatedAttempts,
                remainingAttempts = (MAX_FAILED_ATTEMPTS - updatedAttempts).coerceAtLeast(0)
            )
        }
    }

    @Synchronized
    fun getLockoutState(
        context: Context,
        nowMs: Long = System.currentTimeMillis()
    ): VaultLockoutState {
        val (securePrefs, _) = getSecurePrefs(context)
        val raw = securePrefs.getString(KEY_ENCRYPTED_LOCKOUT, null) ?: return VaultLockoutState()
        val decoded = unwrapEncryptedPayload(raw) ?: return VaultLockoutState()
        val parts = decoded.split(":")
        if (parts.size != 2) return VaultLockoutState()
        val attempts = parts[0].toIntOrNull()?.coerceAtLeast(0) ?: 0
        val lockoutUntil = parts[1].toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        return if (lockoutUntil > nowMs) {
            val remainingSec = ceil((lockoutUntil - nowMs) / 1000.0).toInt().coerceAtLeast(1)
            VaultLockoutState(
                failedAttempts = attempts,
                lockoutUntilEpochMs = lockoutUntil,
                remainingLockoutSeconds = remainingSec
            )
        } else if (lockoutUntil > 0L) {
            resetLockoutState(context)
            VaultLockoutState()
        } else {
            VaultLockoutState(
                failedAttempts = attempts,
                lockoutUntilEpochMs = 0L,
                remainingLockoutSeconds = 0
            )
        }
    }

    @Synchronized
    fun resetLockoutState(context: Context) {
        val (securePrefs, _) = getSecurePrefs(context)
        securePrefs.edit {
            remove(KEY_ENCRYPTED_LOCKOUT)
        }
    }

    @Synchronized
    fun clearVaultCredentials(context: Context) {
        val (securePrefs, isUsingEncryptedStore) = getSecurePrefs(context)
        securePrefs.edit(commit = true) {
            remove(KEY_LEGACY_FALLBACK_PIN)
            remove(KEY_ENCRYPTED_PIN_RECORD)
            remove(KEY_ENCRYPTED_LOCKOUT)
        }
        if (isUsingEncryptedStore) {
            getLegacyPrefs(context).edit(commit = true) {
                remove(KEY_LEGACY_FALLBACK_PIN)
                remove(KEY_ENCRYPTED_PIN_RECORD)
                remove(KEY_ENCRYPTED_LOCKOUT)
            }
        }
    }

    /**
     * Constant-time comparison helper for sensitive recovery answers, hashing normalized UTF-8 bytes
     * with SHA-256 prior to `MessageDigest.isEqual` so mismatched lengths do not leak timing info.
     */
    fun verifySecretConstantTime(
        entered: String,
        expected: String,
        ignoreCase: Boolean = true
    ): Boolean {
        val normalizedEntered = entered.trim().let { if (ignoreCase) it.lowercase() else it }
        val normalizedExpected = expected.trim().let { if (ignoreCase) it.lowercase() else it }
        if (normalizedEntered.isEmpty() || normalizedExpected.isEmpty()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        val enteredHash = digest.digest(normalizedEntered.toByteArray(StandardCharsets.UTF_8))
        digest.reset()
        val expectedHash = digest.digest(normalizedExpected.toByteArray(StandardCharsets.UTF_8))
        return MessageDigest.isEqual(enteredHash, expectedHash)
    }

    /**
     * Encrypts sensitive vault metadata (e.g., recovery security answer, vault metadata cache)
     * using Android Keystore AES-256-GCM before persisting to app storage.
     */
    fun encryptSecretValue(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return encryptWithKeystore(plaintext) ?: plaintext
    }

    /**
     * Decrypts a Keystore-protected value stored in app storage; transparently returns legacy
     * non-prefixed strings so existing data migrates seamlessly on first read.
     */
    fun decryptSecretValue(stored: String): String {
        if (stored.isEmpty()) return ""
        return if (stored.startsWith(ENC_PREFIX_KEYSTORE)) {
            decryptWithKeystore(stored).orEmpty()
        } else {
            stored
        }
    }

    fun isEncryptedPayload(value: String): Boolean {
        return value.startsWith(ENC_PREFIX_KEYSTORE)
    }

    private fun saveLockoutState(context: Context, failedAttempts: Int, lockoutUntilEpochMs: Long) {
        val payload = "$failedAttempts:$lockoutUntilEpochMs"
        val wrapped = encryptWithKeystore(payload) ?: "$ENC_PREFIX_FALLBACK_HASH$payload"
        val (securePrefs, _) = getSecurePrefs(context)
        securePrefs.edit {
            putString(KEY_ENCRYPTED_LOCKOUT, wrapped)
        }
    }

    private fun createEncryptedPinRecord(pin: String): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val (algoTag, derivedHash) = derivePinHash(pin, salt, PBKDF2_ITERATIONS, preferredAlgoTag = "pbkdf2_sha256")
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(derivedHash, Base64.NO_WRAP)
        val hashRecord = "$algoTag:$PBKDF2_ITERATIONS:$saltB64:$hashB64"
        return encryptWithKeystore(hashRecord) ?: "$ENC_PREFIX_FALLBACK_HASH$hashRecord"
    }

    private fun unwrapEncryptedPayload(stored: String): String? {
        return when {
            stored.startsWith(ENC_PREFIX_KEYSTORE) -> decryptWithKeystore(stored)
            stored.startsWith(ENC_PREFIX_FALLBACK_HASH) -> stored.removePrefix(ENC_PREFIX_FALLBACK_HASH)
            else -> null
        }
    }

    private fun verifyPinHashRecord(enteredPin: String, record: String): Boolean {
        val parts = record.split(":")
        if (parts.size != 4) return false
        val algoTag = parts[0]
        val iterations = parts[1].toIntOrNull()?.coerceIn(10_000, 500_000) ?: return false
        val salt = runCatching { Base64.decode(parts[2], Base64.NO_WRAP) }.getOrNull() ?: return false
        val expectedHash = runCatching { Base64.decode(parts[3], Base64.NO_WRAP) }.getOrNull() ?: return false
        if (salt.isEmpty() || expectedHash.isEmpty()) return false

        val (_, computedHash) = derivePinHash(
            pin = enteredPin,
            salt = salt,
            iterations = iterations,
            preferredAlgoTag = algoTag
        )
        // Constant-time comparison to defend against timing side-channel attacks
        return MessageDigest.isEqual(computedHash, expectedHash)
    }

    private fun derivePinHash(
        pin: String,
        salt: ByteArray,
        iterations: Int,
        preferredAlgoTag: String
    ): Pair<String, ByteArray> {
        val pinChars = pin.toCharArray()
        try {
            if (preferredAlgoTag == "pbkdf2_sha256") {
                val sha256Result = runCatching {
                    val factory = SecretKeyFactory.getInstance(ALGO_PBKDF2_SHA256)
                    val spec = PBEKeySpec(pinChars, salt, iterations, PBKDF2_KEY_BITS)
                    try {
                        factory.generateSecret(spec).encoded
                    } finally {
                        spec.clearPassword()
                    }
                }.getOrNull()
                if (sha256Result != null) {
                    return "pbkdf2_sha256" to sha256Result
                }
            }

            if (preferredAlgoTag == "pbkdf2_sha1" || preferredAlgoTag == "pbkdf2_sha256") {
                val sha1Result = runCatching {
                    val factory = SecretKeyFactory.getInstance(ALGO_PBKDF2_SHA1)
                    val spec = PBEKeySpec(pinChars, salt, iterations, PBKDF2_KEY_BITS)
                    try {
                        factory.generateSecret(spec).encoded
                    } finally {
                        spec.clearPassword()
                    }
                }.getOrNull()
                if (sha1Result != null) {
                    return "pbkdf2_sha1" to sha1Result
                }
            }

            // Final fallback: iterated salted SHA-256 (never plaintext)
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            var current = digest.digest(pin.toByteArray(StandardCharsets.UTF_8))
            repeat(10_000) {
                digest.reset()
                digest.update(salt)
                current = digest.digest(current)
            }
            return "salted_sha256" to current
        } finally {
            pinChars.fill('\u0000')
        }
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey? {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
            val existing = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
            if (existing != null) return@runCatching existing

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val keySpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }.getOrNull()
    }

    private fun encryptWithKeystore(plaintext: String): String? {
        val secretKey = getOrCreateKeystoreSecretKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            "$ENC_PREFIX_KEYSTORE$ivB64:$cipherB64"
        }.getOrNull()
    }

    private fun decryptWithKeystore(encoded: String): String? {
        if (!encoded.startsWith(ENC_PREFIX_KEYSTORE)) return null
        val secretKey = getOrCreateKeystoreSecretKey() ?: return null
        return runCatching {
            val body = encoded.removePrefix(ENC_PREFIX_KEYSTORE)
            val parts = body.split(":")
            if (parts.size != 2) return@runCatching null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
        }.getOrNull()
    }
}
