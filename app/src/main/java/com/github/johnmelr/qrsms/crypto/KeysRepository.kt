package com.github.johnmelr.qrsms.crypto

import android.security.keystore.KeyProperties
import com.github.johnmelr.qrsms.data.room.KeyPairEntry
import com.github.johnmelr.qrsms.data.room.KeysDao
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

/**
 * Keys Entry From the room database
 */
class KeysRepository @Inject constructor(
    private val keysDao: KeysDao,
    keyStore: KeyStore,
): KeyManager(keyStore) {
    suspend fun insertKeyPair(entry: KeyPairEntry) {
        keysDao.insertKeyPair(entry)
    }

    override suspend fun getAllKeyPair(): List<String> {
        return keysDao.getAllKeyPair()
    }
    override suspend fun doesKeyPairExist(phoneNumber: String): Boolean {
        val entry: KeyPairEntry? = keysDao.getKeyPairOfAddress(phoneNumber)

        // If entry is not null, has entry (true) else (false)
        return entry != null
    }

    override suspend fun getPrivateKeyForNumber(phoneNumber: String): PrivateKey? {
        val privateKeyBytes: ByteArray? = keysDao.getPrivateKeyOfAddress(phoneNumber)

        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val encodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)

        return keyFactory.generatePrivate(encodedKeySpec)
    }

    override suspend fun getPublicKeyForNumber(phoneNumber: String): PublicKey? {
        val publicKeyBytes: ByteArray? = keysDao.getPublicKeyOfAddress(phoneNumber)

        val keyFactory =  KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val encodedKeySpec = X509EncodedKeySpec(publicKeyBytes)

        return keyFactory.generatePublic(encodedKeySpec)
    }

    override suspend fun deleteKeyPair(phoneNumber: String) {
        keysDao.getKeyPairOfAddress(phoneNumber)
    }

}