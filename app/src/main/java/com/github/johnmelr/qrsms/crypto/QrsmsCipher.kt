package com.github.johnmelr.qrsms.crypto

import java.lang.StringBuilder
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Class responsible for providing encryption/decryption functionality.
 * Keys are obtained from the AndroidKeyStore provider.
 */
class QrsmsCipher(private val phoneNumber: String) {
    private val ks: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")

    /**
     * Encrypt given text to CipherText
     *
     * @param message Message to be encrypted
     * @return the ciphertext in ByteArray
     */
    fun encrypt(message: String): String {
        val secretKey: SecretKey = KeyStoreManager(ks).getSecretKey(phoneNumber)
            ?: throw NullPointerException()

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val cipherByte: ByteArray = cipher.doFinal(message.toByteArray())

        val iv = cipher.iv
        return Base64Utils.byteToBase64(iv) + Base64Utils.byteToBase64(cipherByte)
    }

    /**
     * Decrypt message to its original Plain Text
     *
     * @param cipherBase64 Cipher text to be decrypted received in base64 format
     * @return the message in plain text
     */
    fun decrypt(cipherBase64: String): String {
        val secretKey: SecretKey = KeyStoreManager(ks).getSecretKey(phoneNumber)
            ?: throw NullPointerException()

        val ivByte = Base64Utils.base64ToByteArray(cipherBase64.substring(0, 24))
        val iv = IvParameterSpec(ivByte)

        val cipherText: ByteArray = Base64Utils.base64ToByteArray(
            cipherBase64.substring(24, cipherBase64.length)
        )

        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val textByte: ByteArray = cipher.doFinal(cipherText)

        return String(textByte, Charsets.UTF_8)
    }

    companion object {
        /**
         * Hash the given string using the SHA-256 algorithm and return its string representation.
         *
         * @param valueToHash is the string to generate SHA-256 hash for
         */
        fun getSha256HashString(valueToHash: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(valueToHash.toByteArray(Charsets.UTF_8))
                .fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }
                .toString()
        }
    }
}