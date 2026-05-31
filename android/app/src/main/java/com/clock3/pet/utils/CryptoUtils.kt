package com.clock3.pet.utils

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 100000
    private const val IV_LENGTH = 16
    private const val SALT_LENGTH = 32
    private val EXPORT_PASSWORD = "Clock3DataExport2026"
    fun getExportPassword(): String = EXPORT_PASSWORD
    private val secureRandom = SecureRandom()

    fun encrypt(data: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val combined = salt + iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedData: String, password: String): String? {
        return try {
            val combined = Base64.getDecoder().decode(encryptedData)
            if (combined.size < SALT_LENGTH + IV_LENGTH) return null
            val salt = combined.copyOfRange(0, SALT_LENGTH)
            val iv = combined.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
            val encrypted = combined.copyOfRange(SALT_LENGTH + IV_LENGTH, combined.size)
            if (encrypted.isEmpty()) return null
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            AppLog.e("CryptoUtils", "Decryption failed", e)
            null
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return try {
            val keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
            val keyBytes = keyFactory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
