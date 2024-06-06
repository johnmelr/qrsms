package com.github.johnmelr.qrsms

import android.util.Log
import org.junit.Test

import org.junit.Assert.*
import java.nio.charset.Charset
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun unicodeTest() {
        val str = String(byteArrayOf(
            243.toByte(),
            160.toByte(),
            130.toByte(),
            129.toByte()), Charsets.UTF_8)

        println("Unicode String: $str Length: ${str.length}")

        val latin15 = String(byteArrayOf(
            "01".toInt(16).toByte(),
            "17".toInt(16).toByte()
        ), Charsets.ISO_8859_1)

        println("Latin 1 String: ${latin15.length}")
    }

    @Test
    fun hello() {
        val hello = "Hello"
        val world = "World"

        val h = Base64.getEncoder().encodeToString(hello.toByteArray() + world.toByteArray())
        println(h)

        val back = Base64.getDecoder().decode(h)
        println(String(back, Charsets.UTF_8))
    }
}