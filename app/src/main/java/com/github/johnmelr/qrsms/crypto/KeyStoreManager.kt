package com.github.johnmelr.qrsms.crypto

import android.telephony.PhoneNumberUtils
import android.util.Log
import java.security.Key
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

private const val KEYSTORE_MANAGER_TAG = "KeyStoreManager"
private const val NO_ENTRY_FOR_ALIAS = "No entry with the given alias exist in the Android Key Store"
private const val EC_KEY_PREFIX = "eckey_"

class KeyStoreManager(
   private val keyStore: KeyStore = KeyStore
       .getInstance("AndroidKeyStore").apply { load(null) }
): KeyManager(keyStore) {

    /**
     * Retrieves Public Key associated with the given phone number. This association
     * is done when user generates a QR Code after selecting a contact.
     */
    override suspend fun getPublicKeyForNumber(phoneNumber: String): PublicKey? {
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
    override suspend fun getPrivateKeyForNumber(phoneNumber: String): PrivateKey? {
        val alias = EC_KEY_PREFIX +
                PhoneNumberUtils.formatNumberToE164(phoneNumber, "PH")

        if (!keyStore.isKeyEntry(alias)) {
            Log.v(KEYSTORE_MANAGER_TAG, NO_ENTRY_FOR_ALIAS)
            return null
        }

        val key: Key = keyStore.getKey(alias, null)

        return key as PrivateKey?
    }

    override suspend fun doesKeyPairExist(phoneNumber: String): Boolean {
        val alias = EC_KEY_PREFIX + phoneNumber

        return keyStore.containsAlias(alias)
    }
}