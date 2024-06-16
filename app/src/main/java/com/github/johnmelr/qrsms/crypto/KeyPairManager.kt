package com.github.johnmelr.qrsms.crypto

import com.github.johnmelr.qrsms.data.room.KeyPairEntry
import java.security.PrivateKey
import java.security.PublicKey

/**
 * Interface for KeyManager. In the application, there are two ways to manage keys. One
 * is through the AndroidKeyStore system, and the other is using the SqlCipher.
 * For android api level 33 and above, these devices can simply use the AndroidKeyStore for
 * KeyAgreement since the KeyProperties.PURPOSE_KEY_AGREE is available starting from Android 13.
 * On the other hand, lower api levels has to rely on storing key pairs inside an encrypted
 * database. Through this interface, it is possible to abstract the logic of each KeyManager
 * instance that provides all the needed function for Key Management (more specifically,
 * obtaining Public and Private EC Key Pairs). Functions for storing and saving
 * secret key can be done in the keystore since these operations are covered until the
 * minimum api level this app requires (API 24).
 */
interface KeyPairManager {

    /**
     * Retrieves all alias that containing EC Key Pair inside the AndroidKeyStore
     *
     * @return a list of strings containing the alias of each key pair
     */
    suspend fun getAllKeyPair(): List<String>

    /**
     * Retrieves Public Key associated with the given phone number. This association
     * is done when user generates a QR Code after selecting a contact.
     *
     * @param phoneNumber normalized number of contact
     */
    suspend fun getPublicKeyForNumber(phoneNumber: String): PublicKey?

    /**
     * Retrieves Private Key associated with the given phone number. This association
     * is done when user generates a QR Code after selecting a contact. This private key
     * will be used to perform an ECDH key agreement.
     *
     * @param phoneNumber normalized number of contact
     */
    suspend fun getPrivateKeyForNumber(phoneNumber: String): PrivateKey?

    /**
     * Checks if a secret key exist
     *
     * @param phoneNumber the normalized address of the contact used as a keystore alias
     */
    suspend fun doesKeyPairExist(phoneNumber: String): Boolean

    /**
     * Delete Key Pair entry associated with the given contact
     *
     * @param phoneNumber normalized number of contact
     */
    suspend fun deleteKeyPair(phoneNumber: String)
}