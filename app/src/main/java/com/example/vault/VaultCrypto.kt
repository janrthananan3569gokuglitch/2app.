package com.example.vault

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * High-Security Cryptographic Engine for XP Music Vault Ultra.
 *
 * Implements AES-256-GCM authenticated encryption with PBKDF2-HMAC-SHA256
 * key derivation to securely isolate private audio files in the encrypted vault.
 */
class VaultCrypto(private val context: Context) {
    companion object {
        private const val TAG = "VaultCrypto"
        private const val ITERATION_COUNT = 10000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_LENGTH_BYTES = 12
        private const val SALT_LENGTH_BYTES = 16
        private const val PREFS_NAME = "vault_security_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val DEFAULT_PIN = "1234"
    }

    private val vaultDir: File = File(context.filesDir, "audio_vault").apply {
        if (!exists()) mkdirs()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Initialize default PIN if not set
        if (!prefs.contains(KEY_PIN_HASH)) {
            setPin(DEFAULT_PIN)
        }
    }

    /**
     * Verifies the user PIN against the stored PBKDF2 hash.
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)

        val computedKey = deriveKey(pin, salt)
        val computedHash = Base64.encodeToString(computedKey.encoded, Base64.NO_WRAP)
        return computedHash == storedHash
    }

    /**
     * Sets or updates the vault master PIN.
     */
    fun setPin(newPin: String) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        random.nextBytes(salt)

        val key = deriveKey(newPin, salt)
        val keyHash = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PIN_HASH, keyHash)
            .putString(KEY_PIN_SALT, saltBase64)
            .apply()
    }

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    /**
     * Encrypts an audio file into the Vault sandbox directory.
     * File format: [16 bytes salt][12 bytes IV][Ciphertext with 16 bytes GCM Auth Tag]
     */
    fun encryptFileToVault(sourceFile: File, pin: String, vaultFileName: String): File? {
        return try {
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES)
            random.nextBytes(salt)

            val iv = ByteArray(IV_LENGTH_BYTES)
            random.nextBytes(iv)

            val key = deriveKey(pin, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            val targetFile = File(vaultDir, vaultFileName)
            FileOutputStream(targetFile).use { fos ->
                fos.write(salt)
                fos.write(iv)

                val inputBytes = sourceFile.readBytes()
                val encryptedBytes = cipher.doFinal(inputBytes)
                fos.write(encryptedBytes)
            }
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            null
        }
    }

    /**
     * Decrypts an encrypted vault file directly into memory (zero-disk-footprint).
     */
    fun decryptVaultAudioInMemory(vaultFileName: String, pin: String): ByteArray? {
        return try {
            val targetFile = File(vaultDir, vaultFileName)
            if (!targetFile.exists()) return null

            FileInputStream(targetFile).use { fis ->
                val salt = ByteArray(SALT_LENGTH_BYTES)
                if (fis.read(salt) != SALT_LENGTH_BYTES) return null

                val iv = ByteArray(IV_LENGTH_BYTES)
                if (fis.read(iv) != IV_LENGTH_BYTES) return null

                val encryptedBytes = fis.readBytes()

                val key = deriveKey(pin, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

                cipher.doFinal(encryptedBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}", e)
            null
        }
    }

    /**
     * Removes an audio file from the vault permanently.
     */
    fun deleteFromVault(vaultFileName: String): Boolean {
        val file = File(vaultDir, vaultFileName)
        return if (file.exists()) file.delete() else false
    }

    fun getVaultDir(): File = vaultDir
}
