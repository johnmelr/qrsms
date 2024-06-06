package com.github.johnmelr.qrsms.crypto

import android.os.Build
import android.os.Build.VERSION
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.telephony.PhoneNumberUtils
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import android.util.Base64 as ABase64

const val SECP256R1 = "secp256r1"

object EcKeyGen {
    private val secpParameterSpec = ECGenParameterSpec(SECP256R1)

    fun generateEcKeyPair(): KeyPair {
        val keyGen: KeyPairGenerator = KeyPairGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_EC)

        keyGen.initialize(secpParameterSpec)

        return keyGen.generateKeyPair()
    }

    fun generateEcKeyPairInKeyStore(keyFor: String): KeyPair {
        val alias = "eckey_" + PhoneNumberUtils.formatNumberToE164(keyFor, "PH")

        val keyPairGen = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            keyPairGen.initialize(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_AGREE_KEY)
                    .setAlgorithmParameterSpec(secpParameterSpec)
                    .build())
        }

        return keyPairGen.generateKeyPair()
    }
}