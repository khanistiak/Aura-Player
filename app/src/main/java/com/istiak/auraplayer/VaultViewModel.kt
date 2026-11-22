package com.istiak.auraplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Immutable UI state for the Private Vault PIN authentication & lockout gate.
 * Keeps raw PIN digits out of exposed UI state (only `enteredPinLength` is rendered by the dots).
 */
data class VaultAuthUiState(
    val hasExistingPin: Boolean = false,
    val isConfirmingNewPin: Boolean = false,
    val enteredPinLength: Int = 0,
    val isVerifying: Boolean = false,
    val isErrorFlash: Boolean = false,
    val statusError: String? = null,
    val lockoutState: VaultLockoutState = VaultLockoutState(),
    val shakeNonce: Int = 0,
    val isKeystoreProtected: Boolean = true,
    val isBiometricAvailable: Boolean = false
)

/**
 * ViewModel owning Private Vault authentication, PBKDF2-HMAC-SHA256 verification off the UI thread,
 * legacy plaintext PIN migration, and tamper-resistant 5-attempt / 30-second brute-force lockout
 * countdown state across configuration changes.
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsStore = SettingsDataStore(appContext)

    private val _uiState = MutableStateFlow(
        VaultAuthUiState(
            hasExistingPin = BiometricAuth.hasCustomVaultPin(appContext),
            lockoutState = BiometricAuth.getLockoutState(appContext),
            isKeystoreProtected = BiometricAuth.isKeystoreProtected(appContext),
            isBiometricAvailable = BiometricAuth.canUseSystemBiometricOrCredential(appContext)
        )
    )
    val uiState: StateFlow<VaultAuthUiState> = _uiState.asStateFlow()

    private var currentPinBuffer: String = ""
    private var firstSetupPinBuffer: String? = null
    private var lockoutTickerJob: Job? = null

    init {
        val currentLockout = _uiState.value.lockoutState
        if (currentLockout.isLockedOut) {
            startLockoutCountdown(currentLockout.lockoutUntilEpochMs)
        }
    }

    /**
     * Runs automatic legacy plaintext PIN migration (both SharedPreferences and DataStore) and
     * refreshes Keystore / Biometric availability when entering the Private Vault.
     */
    fun initializeAndMigrate(
        isBiometricUnlockEnabled: Boolean,
        onMigratedRecordReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            val migratedRecord = withContext(Dispatchers.IO) {
                val fromDataStore = settingsStore.migrateAndPurgeLegacyVaultSecrets()
                val fromPrefs = BiometricAuth.migrateLegacyPinIfNeeded(appContext)
                fromPrefs ?: fromDataStore
            }
            if (!migratedRecord.isNullOrBlank()) {
                onMigratedRecordReady(migratedRecord)
            }
            refreshSecurityState(isBiometricUnlockEnabled)
        }
    }

    fun refreshSecurityState(isBiometricUnlockEnabled: Boolean) {
        val hasPin = BiometricAuth.hasCustomVaultPin(appContext)
        val lockout = BiometricAuth.getLockoutState(appContext)
        val keystoreActive = BiometricAuth.isKeystoreProtected(appContext)
        val biometricReady = isBiometricUnlockEnabled &&
            BiometricAuth.canUseSystemBiometricOrCredential(appContext)

        _uiState.update {
            it.copy(
                hasExistingPin = hasPin,
                lockoutState = lockout,
                isKeystoreProtected = keystoreActive,
                isBiometricAvailable = biometricReady
            )
        }
        if (lockout.isLockedOut) {
            startLockoutCountdown(lockout.lockoutUntilEpochMs)
        } else {
            lockoutTickerJob?.cancel()
        }
    }

    fun onDigitPressed(
        digit: String,
        onPinAuthenticated: () -> Unit,
        onPinCreatedAndSaved: (String) -> Unit
    ) {
        val state = _uiState.value
        if (state.isVerifying || state.lockoutState.isLockedOut || currentPinBuffer.length >= PIN_LENGTH) {
            return
        }

        currentPinBuffer += digit
        val entered = currentPinBuffer
        _uiState.update {
            it.copy(
                enteredPinLength = entered.length,
                statusError = null,
                isErrorFlash = false
            )
        }

        if (entered.length < PIN_LENGTH) return

        if (!state.hasExistingPin) {
            handleFirstTimePinSetup(entered, onPinCreatedAndSaved)
        } else {
            verifyExistingPin(entered, onPinAuthenticated)
        }
    }

    fun onBackspacePressed() {
        val state = _uiState.value
        if (state.isVerifying || state.lockoutState.isLockedOut) return

        if (currentPinBuffer.isNotEmpty()) {
            currentPinBuffer = currentPinBuffer.dropLast(1)
            _uiState.update {
                it.copy(
                    enteredPinLength = currentPinBuffer.length,
                    statusError = null,
                    isErrorFlash = false
                )
            }
        } else if (!state.hasExistingPin && firstSetupPinBuffer != null) {
            firstSetupPinBuffer = null
            _uiState.update {
                it.copy(
                    isConfirmingNewPin = false,
                    statusError = null,
                    isErrorFlash = false
                )
            }
        }
    }

    fun onErrorShakeFinished() {
        currentPinBuffer = ""
        _uiState.update {
            it.copy(
                enteredPinLength = 0,
                isErrorFlash = false
            )
        }
    }

    fun onBiometricAuthenticated() {
        lockoutTickerJob?.cancel()
        currentPinBuffer = ""
        firstSetupPinBuffer = null
        BiometricAuth.resetLockoutState(appContext)
        _uiState.update {
            it.copy(
                enteredPinLength = 0,
                isConfirmingNewPin = false,
                isVerifying = false,
                isErrorFlash = false,
                statusError = null,
                lockoutState = VaultLockoutState()
            )
        }
    }

    fun onVaultLocked() {
        currentPinBuffer = ""
        firstSetupPinBuffer = null
        _uiState.update {
            it.copy(
                hasExistingPin = BiometricAuth.hasCustomVaultPin(appContext),
                enteredPinLength = 0,
                isConfirmingNewPin = false,
                isVerifying = false,
                isErrorFlash = false,
                statusError = null,
                lockoutState = BiometricAuth.getLockoutState(appContext)
            )
        }
    }

    private fun handleFirstTimePinSetup(
        enteredPin: String,
        onPinCreatedAndSaved: (String) -> Unit
    ) {
        val initial = firstSetupPinBuffer
        if (initial == null) {
            firstSetupPinBuffer = enteredPin
            currentPinBuffer = ""
            _uiState.update {
                it.copy(
                    isConfirmingNewPin = true,
                    enteredPinLength = 0,
                    statusError = null
                )
            }
            return
        }

        if (initial == enteredPin) {
            _uiState.update { it.copy(isVerifying = true, statusError = null) }
            viewModelScope.launch {
                val encryptedRecord = withContext(Dispatchers.Default) {
                    BiometricAuth.setCustomVaultPin(appContext, enteredPin)
                }
                currentPinBuffer = ""
                firstSetupPinBuffer = null
                _uiState.update {
                    it.copy(
                        hasExistingPin = true,
                        isConfirmingNewPin = false,
                        enteredPinLength = 0,
                        isVerifying = false,
                        statusError = null,
                        lockoutState = VaultLockoutState()
                    )
                }
                onPinCreatedAndSaved(encryptedRecord)
            }
        } else {
            firstSetupPinBuffer = null
            _uiState.update {
                it.copy(
                    isConfirmingNewPin = false,
                    isErrorFlash = true,
                    statusError = "PINs didn't match. Please try again.",
                    shakeNonce = it.shakeNonce + 1
                )
            }
        }
    }

    private fun verifyExistingPin(
        enteredPin: String,
        onPinAuthenticated: () -> Unit
    ) {
        _uiState.update { it.copy(isVerifying = true, statusError = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                BiometricAuth.verifyVaultPinWithLockout(appContext, enteredPin)
            }
            when (result) {
                is VaultPinVerificationResult.Success -> {
                    lockoutTickerJob?.cancel()
                    currentPinBuffer = ""
                    _uiState.update {
                        it.copy(
                            enteredPinLength = 0,
                            isVerifying = false,
                            isErrorFlash = false,
                            statusError = null,
                            lockoutState = VaultLockoutState()
                        )
                    }
                    onPinAuthenticated()
                }
                is VaultPinVerificationResult.InvalidPin -> {
                    val updatedLockout = BiometricAuth.getLockoutState(appContext)
                    val attemptWord = if (result.remainingAttempts == 1) "attempt" else "attempts"
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            isErrorFlash = true,
                            statusError = "Incorrect PIN • ${result.remainingAttempts} $attemptWord left",
                            lockoutState = updatedLockout,
                            shakeNonce = it.shakeNonce + 1
                        )
                    }
                }
                is VaultPinVerificationResult.LockedOut -> {
                    val lockedState = VaultLockoutState(
                        failedAttempts = BiometricAuth.MAX_FAILED_ATTEMPTS,
                        lockoutUntilEpochMs = result.lockoutUntilEpochMs,
                        remainingLockoutSeconds = result.remainingSeconds
                    )
                    _uiState.update {
                        it.copy(
                            isVerifying = false,
                            isErrorFlash = true,
                            statusError = null,
                            lockoutState = lockedState,
                            shakeNonce = it.shakeNonce + 1
                        )
                    }
                    startLockoutCountdown(result.lockoutUntilEpochMs)
                }
            }
        }
    }

    private fun startLockoutCountdown(lockoutUntilEpochMs: Long) {
        lockoutTickerJob?.cancel()
        if (lockoutUntilEpochMs <= System.currentTimeMillis()) return
        lockoutTickerJob = viewModelScope.launch {
            while (true) {
                val updated = BiometricAuth.getLockoutState(appContext)
                _uiState.update {
                    it.copy(
                        lockoutState = updated,
                        statusError = if (!updated.isLockedOut) null else it.statusError
                    )
                }
                if (!updated.isLockedOut) {
                    break
                }
                delay(250L)
            }
        }
    }

    override fun onCleared() {
        lockoutTickerJob?.cancel()
        currentPinBuffer = ""
        firstSetupPinBuffer = null
        super.onCleared()
    }

    companion object {
        const val PIN_LENGTH = 4
    }
}
