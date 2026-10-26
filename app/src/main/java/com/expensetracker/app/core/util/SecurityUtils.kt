package com.expensetracker.app.core.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ExpenseTrackerMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private val BACKUP_MAGIC = byteArrayOf('E'.code.toByte(), 'T'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val BACKUP_VERSION: Byte = 1

    /**
     * Retrieves or generates an AES-256 key in Android Keystore.
     */
    fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Derives a 256-bit AES key from a passphrase and salt using PBKDF2WithHmacSHA256.
     */
    fun deriveKeyFromPassword(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM with a user-supplied password or master key.
     * Generates a random 16-byte salt and 12-byte IV.
     * Output format: [MAGIC: 4 bytes][VERSION: 1 byte][SALT: 16 bytes][IV: 12 bytes][CIPHERTEXT + TAG]
     */
    fun encryptBackup(plaintext: ByteArray, password: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val iv = ByteArray(GCM_IV_LENGTH)
        random.nextBytes(iv)

        val secretKey = deriveKeyFromPassword(password.toCharArray(), salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        cipher.updateAAD(BACKUP_MAGIC)

        val ciphertext = cipher.doFinal(plaintext)

        val output = ByteArrayOutputStream()
        output.write(BACKUP_MAGIC)
        output.write(byteArrayOf(BACKUP_VERSION))
        output.write(salt)
        output.write(iv)
        output.write(ciphertext)
        return output.toByteArray()
    }

    /**
     * Decrypts encrypted backup bytes using the provided password.
     * Validates magic bytes, version, and authenticated encryption tag.
     * Throws [SecurityException] on invalid password or corrupted data.
     */
    fun decryptBackup(encryptedBytes: ByteArray, password: String): ByteArray {
        if (encryptedBytes.size < 4 + 1 + 16 + GCM_IV_LENGTH + (GCM_TAG_LENGTH / 8)) {
            throw SecurityException("Corrupted backup file: Payload too small.")
        }

        val buffer = ByteBuffer.wrap(encryptedBytes)
        val magic = ByteArray(4)
        buffer.get(magic)
        if (!magic.contentEquals(BACKUP_MAGIC)) {
            throw SecurityException("Invalid backup file: Unrecognized format or corrupted header.")
        }

        val version = buffer.get()
        if (version != BACKUP_VERSION) {
            throw SecurityException("Unsupported backup version: $version")
        }

        val salt = ByteArray(16)
        buffer.get(salt)

        val iv = ByteArray(GCM_IV_LENGTH)
        buffer.get(iv)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        return try {
            val secretKey = deriveKeyFromPassword(password.toCharArray(), salt)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipher.updateAAD(BACKUP_MAGIC)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed. Incorrect password or data has been tampered with.", e)
        }
    }

    /**
     * Computes the SHA-256 checksum of the given bytes as a lowercase hex string.
     */
    fun computeSha256(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
