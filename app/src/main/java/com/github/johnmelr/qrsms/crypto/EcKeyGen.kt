package com.github.johnmelr.qrsms.crypto

import android.os.Build
import android.os.Build.VERSION
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.telephony.PhoneNumberUtils
import com.github.johnmelr.qrsms.data.room.KeyPairDatabase
import com.github.johnmelr.qrsms.data.room.KeyPairEntry
import com.github.johnmelr.qrsms.data.room.KeysDao
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import javax.inject.Inject

const val SECP256R1 = "secp256r1"

/**
 * Class responsible for KeyPair generation that will be used for key exchange.
 * Encapsulates the logic for generating the KeyPair in either the AndroidKeyStore
 * system for Android API level 33 and above or in the encrypted room database otherwise.
 *
 * @property
 */
class EcKeyGen (
    private val keysRepository: KeysRepository
) {
    private val secpParameterSpec = ECGenParameterSpec(SECP256R1)

    suspend fun generateKeyPair(keyFor: String): KeyPair {
        val normalizedPhoneNumber = PhoneNumberUtils.formatNumberToE164(keyFor, "PH")
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return generateEcKeyPairInKeyStore(normalizedPhoneNumber)
        } else {
            // Key Pair generation.
            val keyPair: KeyPair = generateEcKeyPair()

            val entry = KeyPairEntry(
                normalizedPhoneNumber,
                keyPair.private.encoded,
                keyPair.public.encoded,
            )

            keysRepository.insertKeyPair(entry)

            return keyPair
        }
    }

    private fun generateEcKeyPair(): KeyPair {
        val keyGen: KeyPairGenerator = KeyPairGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_EC)

        keyGen.initialize(secpParameterSpec)

        return keyGen.generateKeyPair()
    }

    private fun generateEcKeyPairInKeyStore(normalizedPhoneNumber: String): KeyPair {
        val alias = "eckey_$normalizedPhoneNumber"

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