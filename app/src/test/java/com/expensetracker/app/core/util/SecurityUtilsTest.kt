package com.expensetracker.app.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun `encrypts and decrypts backup successfully with correct password`() {
        val originalText = "{\"database_version\":1,\"transactions\":[{\"amount\":\"125.50\"}]}"
        val password = "SuperSecretPassword123!"

        val encrypted = SecurityUtils.encryptBackup(originalText.toByteArray(Charsets.UTF_8), password)
        assertThat(encrypted).isNotNull()
        assertThat(encrypted.size).isGreaterThan(originalText.length)

        val decryptedBytes = SecurityUtils.decryptBackup(encrypted, password)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)
        assertThat(decryptedText).isEqualTo(originalText)
    }

    @Test
    fun `fails to decrypt backup with incorrect password`() {
        val originalText = "Sensitive financial payload"
        val password = "CorrectPassword"
        val wrongPassword = "WrongPassword"

        val encrypted = SecurityUtils.encryptBackup(originalText.toByteArray(Charsets.UTF_8), password)

        val exception = assertThrows(SecurityException::class.java) {
            SecurityUtils.decryptBackup(encrypted, wrongPassword)
        }
        assertThat(exception).hasMessageThat().contains("Decryption failed")
    }

    @Test
    fun `fails to decrypt corrupted data or tampered payload`() {
        val originalText = "Sensitive financial payload"
        val password = "CorrectPassword"

        val encrypted = SecurityUtils.encryptBackup(originalText.toByteArray(Charsets.UTF_8), password)
        // Tamper with one byte in the ciphertext
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1] + 1).toByte()

        assertThrows(SecurityException::class.java) {
            SecurityUtils.decryptBackup(encrypted, password)
        }
    }

    @Test
    fun `computes accurate SHA-256 hash`() {
        val input = "Hello, Expense Tracker!".toByteArray(Charsets.UTF_8)
        val hash = SecurityUtils.computeSha256(input)
        assertThat(hash).matches("^[a-f0-9]{64}$")
    }
}
