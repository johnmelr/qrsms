package com.github.johnmelr.qrsms.crypto

import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.telephony.PhoneNumberUtils
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

private const val SECRET_KEY_PREFIX = "skey_"

abstract class KeyManager(private val keyStore: KeyStore): KeyPairManager {
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
        val secretKeyEntry = KeyStore.SecretKeyEntry(secretKeySpec)

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

    fun doesSecretKeyExist(phoneNumber:String): Boolean {
        val alias = SECRET_KEY_PREFIX +
                PhoneNumberUtils.formatNumberToE164(phoneNumber, "PH")

        return keyStore.containsAlias(alias)
    }
}