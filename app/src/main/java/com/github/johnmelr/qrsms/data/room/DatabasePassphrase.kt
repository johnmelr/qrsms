package com.github.johnmelr.qrsms.data.room;

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asLiveData
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.github.johnmelr.qrsms.data.preferencesDataStore.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject


private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val ALIAS = "passphrase_dp"

/**
 * object class for obtaining and processing database passphrase. Since the password is stored in the
 * preferences repository, an instance of the repository is injected to this class for it
 * to gain access to the passphrase stored in the DataStore.
 */
@Suppress("SameParameterValue")
class DatabasePassphrase @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val preferencesRepository: PreferencesRepository,
) {
    private val cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" +
        KeyProperties.BLOCK_MODE_CBC + "/" +
        KeyProperties.ENCRYPTION_PADDING_PKCS7
    )

    fun retrievePassphrase(): ByteArray {
        val databasePassphrase = preferencesRepository
            .databasePassphrase
            .asLiveData(Dispatchers.IO).value

        // There is an existing passphrase in the datastore
        if (databasePassphrase?.isEmpty() == false) {
            val secretKey: SecretKey = getSecretKey()
            val passphrase: ByteArray = getDecryptedPassphrase(databasePassphrase, secretKey)

            val newEncryptedPassphrase: ByteArray = encryptPassphrase(passphrase, secretKey)
            suspend {
                preferencesRepository.updatePassphrase(newEncryptedPassphrase)
            }

            return passphrase
        }

        // User's first time creating a passphrase
        val newPassphrase = generateRandomPassphrase(32)

        val secretKey: SecretKey = generateSecretKey()
        val encryptedPassphrase = encryptPassphrase(newPassphrase, secretKey)

        suspend {
            preferencesRepository.updatePassphrase(encryptedPassphrase)
        }
        return newPassphrase
    }

    fun getPassphrase(): ByteArray {
        val file = File(applicationContext.filesDir, "passphrase.bin")
        val encryptedFile: EncryptedFile = EncryptedFile.Builder(
            file,
            applicationContext,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return if (file.exists()) {
            encryptedFile.openFileInput().use { it.readBytes() }
        } else {
            val passphrase = generateRandomPassphrase(32)
            encryptedFile.openFileOutput().use { it.write(passphrase) }

            return passphrase
        }
    }

    private fun encryptPassphrase(passphrase: ByteArray, secretKey: SecretKey): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv

        val encryptedPassphrase: ByteArray = cipher.doFinal(passphrase)
        return iv + encryptedPassphrase
    }

    private fun getDecryptedPassphrase(encryptedPassphrase: ByteArray, secretKey: SecretKey): ByteArray {
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
    private fun generateSecretKey(): SecretKey {
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
    private fun getSecretKey(): SecretKey {
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
    private fun generateRandomPassphrase(size: Int = 32): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(size)

        random.nextBytes(bytes)
        return bytes
    }
}