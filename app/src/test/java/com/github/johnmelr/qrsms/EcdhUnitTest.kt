package com.github.johnmelr.qrsms;

import android.os.Build
import android.telephony.PhoneNumberUtils
import at.favre.lib.hkdf.HKDF
import com.github.johnmelr.qrsms.crypto.EcdhKeyAgreement
import org.junit.Test;
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.MessageDigest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Arrays
import java.util.Base64
import javax.crypto.KeyAgreement

const val MY_PHONE_NUMBER = "+63 999 123 4567"
const val OTHER_PHONE_NUMBER = "+63 999 123 1234"

/**
 * Test class for checking Key Exchange Functionalities
 */
class EcdhUnitTest {
    @Test
    fun performExchange() {
        val ecgen = EcUnitTest()

        val myKeyPair = ecgen.generateKeyPair()
        val otherKeyPair = ecgen.generateKeyPair()

        // Perform ECDH on user's side
        val mySharedSecret = generateSharedSecret(
            Base64.getEncoder().encodeToString(otherKeyPair.public.encoded),
            myKeyPair.private.encoded
        )

        // Perform ECDH on contact's side
        val otherSharedSecret = generateSharedSecret(
            Base64.getEncoder().encodeToString(myKeyPair.public.encoded),
            otherKeyPair.private.encoded
        )

        println("EcdhUnitTest: Shared secret_${Base64.getEncoder().encodeToString(mySharedSecret)}")
        println("EcdhUnitTest: Shared secret_${Base64.getEncoder().encodeToString(otherSharedSecret)}")

        val md = MessageDigest.getInstance("SHA-256")

        // Check if hash of shared secrets match
        assert(md.digest(mySharedSecret).contentEquals(md.digest(otherSharedSecret)))

        // Generate Secret Key on user's side
        val mySecretKey = deriveSecretKey(
            mySharedSecret,
            myKeyPair,
            MY_PHONE_NUMBER,
            otherKeyPair.public.encoded,
            OTHER_PHONE_NUMBER
        )

        // Generate Secret Key on contact's side
        val otherSecretKey = deriveSecretKey(
            otherSharedSecret,
            otherKeyPair,
            OTHER_PHONE_NUMBER,
            myKeyPair.public.encoded,
            MY_PHONE_NUMBER
        )

        println("EcdhUnitTest: Secret key_${Base64.getEncoder().encodeToString(mySecretKey)}")
        println("EcdhUnitTest: Secret key_${Base64.getEncoder().encodeToString(otherSecretKey)}")

        assert(md.digest(mySecretKey).contentEquals(md.digest(otherSecretKey)))
    }

    private fun generateSharedSecret(
        otherPublicKey: String,
        myPrivateKey: ByteArray
    ): ByteArray {
        val keyFactory = KeyFactory.getInstance("EC")
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        // Convert public key from base64 string to byte array
        val otherPublicKeyAsByteArray: ByteArray = Base64.getDecoder().decode(otherPublicKey)

        val x509encode = X509EncodedKeySpec(otherPublicKeyAsByteArray)
        val publicKey: ECPublicKey = keyFactory.generatePublic(x509encode) as ECPublicKey

        val pkcs8encode = PKCS8EncodedKeySpec(myPrivateKey)
        val privateKey: ECPrivateKey = keyFactory.generatePrivate(pkcs8encode) as ECPrivateKey

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        return keyAgreement.generateSecret()
    }

    fun deriveSecretKey(
        secretKeyByte: ByteArray,
        myKeyPair: KeyPair,
        myPhoneNumber: String,
        otherPublicKeyByte: ByteArray,
        otherPhoneNumber: String
    ): ByteArray {
        val myPublicKeyByte: ByteArray = myKeyPair.public.encoded

        // Initialize Hash Based Key Derivation Function
        val hkdf: HKDF = HKDF.fromHmacSha256()

        // Generate Salt using the user's and contact's phone numbers combined
        // and converted to a SHA-256 hash
        val phoneNumbers = arrayOf(myPhoneNumber, otherPhoneNumber).sorted().joinToString("")
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        val salt: ByteArray = md.digest(phoneNumbers.toByteArray(Charsets.UTF_8))

        println(salt.fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }.toString())

        // HKDF Extract Phase
        val pseudoRandomKey: ByteArray = hkdf.extract(salt, secretKeyByte)

        // Create Info as ByteArrayOutputStream.
        // To be fed in the HKDF to provide additional randomness
        val info = ByteArrayOutputStream()
        info.write("ECDH secp256r1 AES-256-CBC".toByteArray(Charsets.UTF_8))

        // Combine Public Keys in a ByteArrayOutputStream so that the bytes can be sorted.
        // This ensures that info will be the same in both the user and the contact. If
        // the info parameter is different, the resulting key will be different causing
        // symmetric encryption impossible.
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
