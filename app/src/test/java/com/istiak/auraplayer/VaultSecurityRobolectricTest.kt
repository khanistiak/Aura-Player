package com.istiak.auraplayer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VaultSecurityRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BiometricAuth.clearVaultCredentials(context)
    }

    @Test
    fun `vault pin is hashed with unique random salt and never stored in plaintext`() {
        val pin = "7391"
        val firstRecord = BiometricAuth.setCustomVaultPin(context, pin)
        val secondRecord = BiometricAuth.setCustomVaultPin(context, pin)

        assertFalse("Stored record must never contain plaintext PIN", firstRecord.contains(pin))
        assertFalse("Stored record must never contain plaintext PIN", secondRecord.contains(pin))
        assertNotEquals("Each PIN save must generate a unique 16-byte random salt", firstRecord, secondRecord)

        val legacyPrefs = context.getSharedPreferences("aura_vault_security", Context.MODE_PRIVATE)
        assertNull("Plaintext legacy key must not exist", legacyPrefs.getString("vault_fallback_pin", null))
        assertTrue(BiometricAuth.verifyCustomVaultPin(context, pin))
        assertFalse(BiometricAuth.verifyCustomVaultPin(context, "0000"))
    }

    @Test
    fun `legacy plaintext pin is automatically migrated and immediately deleted`() {
        val legacyPrefs = context.getSharedPreferences("aura_vault_security", Context.MODE_PRIVATE)
        legacyPrefs.edit().putString("vault_fallback_pin", "2580").commit()

        assertTrue(BiometricAuth.hasCustomVaultPin(context))
        assertNull(
            "Legacy plaintext PIN must be deleted immediately after migration",
            legacyPrefs.getString("vault_fallback_pin", null)
        )
        assertTrue(BiometricAuth.verifyCustomVaultPin(context, "2580"))
        assertFalse(BiometricAuth.verifyCustomVaultPin(context, "1111"))
    }

    @Test
    fun `five failed pin attempts enforce 30 second brute force cooldown`() {
        BiometricAuth.setCustomVaultPin(context, "4829")
        val t0 = 1_700_000_000_000L

        for (attempt in 1..4) {
            val res = BiometricAuth.verifyVaultPinWithLockout(context, "9999", nowMs = t0 + attempt * 100L)
            assertTrue(res is VaultPinVerificationResult.InvalidPin)
            val invalid = res as VaultPinVerificationResult.InvalidPin
            assertEquals(attempt, invalid.failedAttempts)
            assertEquals(5 - attempt, invalid.remainingAttempts)
        }

        // 5th wrong attempt triggers 30-second lockout
        val fifth = BiometricAuth.verifyVaultPinWithLockout(context, "9999", nowMs = t0 + 500L)
        assertTrue(fifth is VaultPinVerificationResult.LockedOut)
        val locked = fifth as VaultPinVerificationResult.LockedOut
        assertEquals(30, locked.remainingSeconds)

        // Even correct PIN is blocked during the 30-second cooldown
        val duringCooldown = BiometricAuth.verifyVaultPinWithLockout(context, "4829", nowMs = t0 + 10_500L)
        assertTrue(duringCooldown is VaultPinVerificationResult.LockedOut)
        assertEquals(20, (duringCooldown as VaultPinVerificationResult.LockedOut).remainingSeconds)

        // After 30 seconds elapse, cooldown clears and correct PIN unlocks
        val afterCooldown = BiometricAuth.verifyVaultPinWithLockout(context, "4829", nowMs = t0 + 31_000L)
        assertTrue(afterCooldown is VaultPinVerificationResult.Success)
        assertFalse(BiometricAuth.getLockoutState(context, nowMs = t0 + 31_100L).isLockedOut)
    }

    @Test
    fun `constant time security answer verification handles case and whitespace`() {
        assertTrue(BiometricAuth.verifySecretConstantTime("  Luna ", "luna"))
        assertFalse(BiometricAuth.verifySecretConstantTime("Luna2", "Luna"))
        assertFalse(BiometricAuth.verifySecretConstantTime("", "Luna"))
    }
}
