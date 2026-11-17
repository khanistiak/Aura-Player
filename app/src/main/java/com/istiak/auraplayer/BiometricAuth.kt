package com.istiak.auraplayer

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Biometric & Device Credential authentication helper using androidx.biometric.BiometricPrompt.
 * Supports Fingerprint, Face Unlock, and Device PIN/Pattern/Password fallback.
 */
object BiometricAuth {

    private const val PREFS_NAME = "aura_vault_security"
    private const val KEY_FALLBACK_PIN = "vault_fallback_pin"

    fun canUseSystemBiometricOrCredential(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Private Vault",
        subtitle: String = "Use fingerprint, face unlock, or device PIN to view hidden videos",
        onSuccess: () -> Unit,
        onFallbackPinRequired: () -> Unit,
        onError: (String) -> Unit
    ) {
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(authenticators)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // If running on an emulator or device without enrolled biometrics or lock screen PIN,
            // open the in-app Liquid Glass Vault PIN sheet so the user can still authenticate securely.
            onFallbackPinRequired()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                    errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                    errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                ) {
                    onFallbackPinRequired()
                } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Try again.")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    fun hasCustomVaultPin(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_FALLBACK_PIN, null).isNullOrEmpty()
    }

    fun setCustomVaultPin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FALLBACK_PIN, pin).apply()
    }

    fun verifyCustomVaultPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_FALLBACK_PIN, null)
        return saved != null && saved == pin
    }
}
