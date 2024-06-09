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

class KeysRepository(
    private val keysDao: KeysDao,
    keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
        .apply { load(null) }
): KeyManager(keyStore) {

    override fun doesKeyPairExist(phoneNumber: String): Boolean {
        val entry: KeyPairEntry? = keysDao.getKeyPairOfAddress(phoneNumber)

        // If entry is not null, has entry (true) else (false)
        return entry != null
    }

    override fun getPrivateKeyForNumber(phoneNumber: String): PrivateKey? {
        val privateKeyBytes: ByteArray? = keysDao.getPrivateKeyOfAddress(phoneNumber)

        val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val encodedKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)

        return keyFactory.generatePrivate(encodedKeySpec)
    }

    override fun getPublicKeyForNumber(phoneNumber: String): PublicKey? {
        val publicKeyBytes: ByteArray? = keysDao.getPublicKeyOfAddress(phoneNumber)

        val keyFactory =  KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
        val encodedKeySpec = PKCS8EncodedKeySpec(publicKeyBytes)

        return keyFactory.generatePublic(encodedKeySpec)
    }
}