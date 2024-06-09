package com.github.johnmelr.qrsms.data.room;

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val ALIAS = "passphrase_dp"

/**
 * object class for obtaining and processing database passphrase. Since the password is stored in the
 * preferences repository, an instance of the repository is injected to this class for it
 * to gain access to the passphrase stored in the DataStore.
 */
object DatabasePassphrase {
    private val cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
    )

    fun encryptPassphrase(passphrase: ByteArray, secretKey: SecretKey): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv

        val encryptedPassphrase: ByteArray = cipher.doFinal(passphrase)
        return iv + encryptedPassphrase
    }

    fun getDecryptedPassphrase(encryptedPassphrase: ByteArray, secretKey: SecretKey): ByteArray {
        // First 16 bytes is IV
        val iv = encryptedPassphrase.copyOfRange(0, 16)
        val ivParam = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParam)

        val extractedPassphrase = encryptedPassphrase.copyOfRange(16, encryptedPassphrase.size)
        return cipher.doFinal(extractedPassphrase)
    }

    /**
     * Generate a KeyStore secret key to be used for encrypting the passphrase used for
     * sqlcipher. Since the passphrase cannot be stored in the keystore and passed to the
     * database factory, the passphrase will be encrypted and stored inside the
     * datastore preferences
     */
    fun generateSecretKey(): SecretKey {
        val keyGenerator =  KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build())

        return keyGenerator.generateKey()
    }

    /**
     * Retrieve secret key used for encrypting/decrypting passphrase from AndroidKeyStore.
     *
     * @return the secret key for the passphrase
     */
    fun getSecretKey(): SecretKey {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }

        return keyStore.getKey(ALIAS, null) as SecretKey
    }

    /**
     * Function to generate random passphrase with n number of bytes
     *
     * @param size size of random bytes to be generated
     * @return generated random as ByteArray
     */
    fun generateRandomPassphrase(size: Int): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(size)

        random.nextBytes(bytes)
        return bytes
    }
}
