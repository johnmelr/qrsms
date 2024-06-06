package com.github.johnmelr.qrsms

import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * Test class for Key Pair generation
 */
class EcUnitTest {
    @Test
    fun testGeneratedKeyPair() {
        val kp:KeyPair = generateKeyPair()

        val privateByte = Base64.getEncoder().encodeToString(kp.private.encoded).toString()
        val publicByte = Base64.getEncoder().encodeToString(kp.public.encoded).toString()

        println("EcUnitTest: Private Key: $privateByte")
        println("EcUnitTest: Public Key: $publicByte")

        assert(kp.private is ECPrivateKey)
        assert(kp.public is ECPublicKey)
    }

    /**
     * Simplified version of the generateKeyPair function found in the EcKeyGen
     * class since this class relies on some android specific libraries which might not be loaded
     * for this unit test.
     */
    fun generateKeyPair(): KeyPair {
        val secp = ECGenParameterSpec("secp256r1")

        val keyGen: KeyPairGenerator = KeyPairGenerator
            .getInstance("EC")

        keyGen.initialize(secp)

        return keyGen.generateKeyPair()
    }
}