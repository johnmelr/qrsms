package com.github.johnmelr.qrsms.crypto

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.telephony.PhoneNumberUtils
import android.util.Log
import java.security.Key
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

const val KEYSTORE_MANAGER_TAG = "KeyStoreManager"
const val NO_ENTRY_FOR_ALIAS = "No entry with the given alias exist in the Android Key Store"

object KeyStoreManager {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val EC_KEY_PREFIX = "eckey_"
    private const val SECRET_KEY_PREFIX = "skey_"

    private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    /**
     * Retrieves Public Key associated with the given phone number. This association
     * is done when user generates a QR Code after selecting a contact.
     */
    fun getPublicKeyForNumber(phoneNumber: String): PublicKey? {
        val alias = EC_KEY_PREFIX +
                PhoneNumberUtils.formatNumberToE164(phoneNumber, "PH")

        val certificate = keyStore.containsAlias(alias)

        if (!certificate) {
            Log.v(KEYSTORE_MANAGER_TAG, NO_ENTRY_FOR_ALIAS)
            return null
        }

        return keyStore.getCertificate(alias).publicKey
    }

    /**
     * Retrieves Private Key associated with the given phone number. This association
     * is done when user generates a QR Code after selecting a contact. This private key
     * will be used to perform an ECDH key agreement.
     */
    fun getPrivateKeyForNumber(phoneNumber: String): PrivateKey? {
        val alias = EC_KEY_PREFIX +
                PhoneNumberUtils.formatNumberToE164(phoneNumber, "PH")

        if (!keyStore.isKeyEntry(alias)) {
            Log.v(KEYSTORE_MANAGER_TAG, NO_ENTRY_FOR_ALIAS)
            return null
        }

        val key: Key = keyStore.getKey(alias, null)

        return key as PrivateKey?
    }

    /**
     * Retrieves Secret Key entry to be used for encrypting a message for a contact.
     *
     * @param phoneNumber Contact's phone number used as a the alias for the KeyStore entry
     * @return the associated SecretKey for the given contact
     */
    fun getSecretKey(phoneNumber: String): SecretKey? {
        val alias = SECRET_KEY_PREFIX +
                PhoneNumberUtils.formatNumberToE164(phoneNumber, "PH")

        val key = keyStore.getKey(alias, null)

        return if (key != null) key as SecretKey else null
    }

    /**
     * Stores the generated secret key in keystore using the phone number as the alias.
     */
    fun saveSecretKey(phoneNumber: String, secretKeyByte: ByteArray) {
        // Create SecretKey instance from ByteArray
        val secretKeySpec = SecretKeySpec(secretKeyByte, KeyProperties.KEY_ALGORITHM_AES)
        val secretKeyEntry = SecretKeyEntry(secretKeySpec)

        val alias = SECRET_KEY_PREFIX +
                PhoneNumberUtils.formatNumberToE164(phoneNumber, "PH")
        keyStore.setEntry(
            alias,
            secretKeyEntry,
            KeyProtection.Builder(
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()
        )
    }

    fun doesKeyPairExist(phoneNumber: String): Boolean {
        val alias = EC_KEY_PREFIX + phoneNumber

        return keyStore.containsAlias(alias)
    }

    fun doesSecretKeyExist(phoneNumber:String): Boolean {
        val alias = SECRET_KEY_PREFIX +
                PhoneNumberUtils.formatNumberToE164(phoneNumber, "PH")

        return keyStore.containsAlias(alias)
    }
}