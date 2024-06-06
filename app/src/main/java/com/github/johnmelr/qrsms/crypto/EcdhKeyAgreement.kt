package com.github.johnmelr.qrsms.crypto

import android.os.Build
import android.security.keystore.KeyProperties
import at.favre.lib.hkdf.HKDF
import java.io.ByteArrayOutputStream
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Arrays
import java.util.Base64
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import android.util.Base64 as androidBase64

const val KEY_ALGORITHM_ECDH = "ECDH"

class EcdhKeyAgreement(
    private val phoneNumber: String,
    private val myPhoneNumber: String,
    private val publicKeyString: String,
) {
    private val keyAgreement: KeyAgreement = KeyAgreement.getInstance(KEY_ALGORITHM_ECDH)
    private val keyFactory: KeyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)   // Used to reconstruct keys

    fun performExchange() {
        try {
            val otherPublicKeyAsByteArray: ByteArray = Base64Utils.base64ToByteArray(publicKeyString)

            val sharedSecret = generateSharedSecret(
                otherPublicKeyAsByteArray
            )

            val myPublicKey = KeyStoreManager.getPublicKeyForNumber(phoneNumber) ?: throw InvalidKeyException("No key exist for $phoneNumber")

            val secretKey:ByteArray = deriveSecretKey(
                sharedSecret,
                myPublicKey,
                otherPublicKeyAsByteArray,
                myPhoneNumber,
                phoneNumber
            )

            KeyStoreManager.saveSecretKey(phoneNumber, secretKey)
        } catch (exception: Exception) {
            throw exception
        }
    }

    /**
     * Generates a shared secret using the ECDH key agreement protocol
     */
    private fun generateSharedSecret(
        otherPublicKey: ByteArray,
    ): ByteArray {
        val x509encode = X509EncodedKeySpec(otherPublicKey)
        val publicKey: ECPublicKey = keyFactory.generatePublic(x509encode) as ECPublicKey

        val myPrivateKey = KeyStoreManager.getPrivateKeyForNumber(phoneNumber)
            ?: throw InvalidKeyException("Empty Private Key")

        keyAgreement.init(myPrivateKey)
        keyAgreement.doPhase(publicKey, true)

        return keyAgreement.generateSecret()
    }


    /**
     * Generates a shared secret using the ECDH key agreement protocol
     */
    fun generateSharedSecret(
        otherPublicKey: String,
        myPrivateKey: ByteArray
    ): ByteArray {
        // Convert public key from base64 string to byte array
        val otherPublicKeyAsByteArray: ByteArray = Base64Utils.base64ToByteArray(otherPublicKey)

        val x509encode = X509EncodedKeySpec(otherPublicKeyAsByteArray)
        val publicKey: ECPublicKey = keyFactory.generatePublic(x509encode) as ECPublicKey

        val pkcs8encode = PKCS8EncodedKeySpec(myPrivateKey)
        val privateKey: ECPrivateKey = keyFactory.generatePrivate(pkcs8encode) as ECPrivateKey

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        return keyAgreement.generateSecret()
    }

    /**
     * Derive an AES Secret Key using the Hash Based Derivation Function (HKDF).
     *
     * @param secretKeyByte Generated secret key after performing ECDH key exchange
     * @param myPublicKey User's ECKeyPair used to perform the exchange
     * @param myPhoneNumber User's default phone number
     * @param otherPublicKeyByte Contact's public key represented as byte array
     * @param otherPhoneNumber Contact's phone number
     * @return the derived secret key as ByteArray
     */
    private fun deriveSecretKey(
        secretKeyByte: ByteArray,
        myPublicKey: PublicKey,
        otherPublicKeyByte: ByteArray,
        myPhoneNumber: String,
        otherPhoneNumber: String
    ): ByteArray {
        val myPublicKeyByte: ByteArray = myPublicKey.encoded

        // Initialize Hash Based Key Derivation Function
        val hkdf: HKDF = HKDF.fromHmacSha256()

        // Generate Salt using the user's and contact's phone numbers combined
        // and converted to a SHA-256 hash
        val phoneNumbers = arrayOf(myPhoneNumber, otherPhoneNumber).sorted().joinToString("")
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        val salt: ByteArray = md.digest(phoneNumbers.toByteArray(Charsets.UTF_8))

        // HKDF Extract Phase
        val pseudoRandomKey: ByteArray = hkdf.extract(salt, secretKeyByte)

        // Create Info as ByteArrayOutputStream.
        // To be fed in the HKDF to provide additional randomness
        val info = ByteArrayOutputStream()
        info.write("ECDH secp256r1 AES-CBC".toByteArray(Charsets.UTF_8))

        // Combine Public Keys in a ByteArrayOutputStream so that the bytes can be sorted.
        // This ensures that info will be the same in both the user and the contact.
        val keysOutputStream = ByteArrayOutputStream()
        keysOutputStream.write(myPublicKeyByte)
        keysOutputStream.write(otherPublicKeyByte)

        val combinedArray = keysOutputStream.toByteArray()
        Arrays.sort(combinedArray)

        info.write(combinedArray)

        return hkdf.expand(
            pseudoRandomKey,
            info.toByteArray(),
            32
        )
    }
}