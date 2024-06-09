package com.github.johnmelr.qrsms.crypto

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
    fun getPublicKeyForNumber(phoneNumber: String): PublicKey?
    fun getPrivateKeyForNumber(phoneNumber: String): PrivateKey?
    fun doesKeyPairExist(phoneNumber: String): Boolean
}