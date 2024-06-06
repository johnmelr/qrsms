package com.github.johnmelr.qrsms

import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.github.johnmelr.qrsms.crypto.QrsmsCipher
import org.junit.Test
import java.nio.charset.Charset
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "SmsManagerTest"

class SmsManagerTest {
    private val smsManager = SmsManager.getSmsManagerForSubscriptionId(0)
    private val contactPhoneNumber = "+63 999 123 1234"

    private val message = "Praesent aliquam, nunc vitae commodo posuere, velit quam porttitor neque, ut vulputate odio lorem in tortor. Fusce rutrum cursus elit vel tempor. Donec malesuad1"
    private val cipherPart = arrayListOf("6vYRFhrfQWZpftSJlSR7JH7kXRwChlNYHcNujsjLlTNlpdaVgQqE/C4MVC7zchQxlAeRNBmK+E1Fr9ocbwcxnidqQIHkFDv2jrsOc0I2WD99PCw4+AECn6XOELt2CQsyCLaZSOEJeuNv8L7+ry4gimAmY", "6EHebUOfORL9tgSxKHJpKCzbEPpoue/8xv8MfwwjOuAioRDOco+qMUQdtboM82g3C32+C7VpCt0SOPIYNY=")

    @Test
    fun divideMessage() {
        val divided: ArrayList<String> = smsManager
            .divideMessage(message)

        assert(divided[0].length == 153)
    }

    // Values for Testing encryption/decryption
    private val key: String = "c14984r9WVCqosQhTmwyzKDdRxN+7J/cF9Ney+vDq+g="

    private val keyByte: ByteArray = Base64.getDecoder().decode(key)
    private val secretKey: SecretKey = SecretKeySpec(
        keyByte, "AES"
    )
    private val iv = IvParameterSpec(secretKey.encoded.drop(16).toByteArray())
    private val stringPad = String(byteArrayOf(
        243.toByte(),
        160.toByte(),
        130.toByte(),
        129.toByte()), Charsets.UTF_8)

    @Test
    fun messageEncryptionTest() {
        val normalizedPhone: String = PhoneNumberUtils.formatNumberToE164(contactPhoneNumber, "PH")
        val phoneHash: String = QrsmsCipher.getSha256HashString(normalizedPhone)
        val padToggle = "_"

        // Create Cipher Instance
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)

        val cipherText = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        val cipherTextString = Base64.getEncoder().encodeToString(cipherText)

        val dividedMessage = smsManager.divideMessage(cipherTextString)
        Log.v(TAG, dividedMessage.toString())
        Log.v(TAG, phoneHash)

        val parts = arrayOf(dividedMessage.map { message ->
            val part = phoneHash.substring(IntRange(0, 3)) + stringPad + message
            Log.v(TAG, part + "Length: ${message.length}")
            part
        })
    }

    @Test
    fun messageDecryptionTest() {
        val normalizedPhone: String = PhoneNumberUtils.formatNumberToE164(contactPhoneNumber, "PH")
        val phoneHash: String = QrsmsCipher.getSha256HashString(normalizedPhone)

        val sb = StringBuilder()

        cipherPart.forEach { part ->
            // Verify if message is indeed encrypted
            if (part.substring(IntRange(0, 3)) == phoneHash.substring(IntRange(0, 3)) &&
                part.substring(IntRange(4,5)) == stringPad) {
                sb.append(part.substring(6))
            } else {
                sb.append(part)
            }
        }

        val cipherTextFinal = sb.toString()
        Log.v(TAG, cipherTextFinal)

        val cipherBytes: ByteArray = Base64.getDecoder().decode(cipherTextFinal)

        // Create Cipher Instance
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)

        val plainTextByte = cipher.doFinal(cipherBytes)
        val plainText = String(plainTextByte, Charsets.UTF_8)

        Log.v(TAG, plainText)
        assert(plainText == message)
    }

    @Test
    fun encryptionTest() {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)

        val plainText = "Hello World"
        val cipherText = cipher.doFinal(plainText.toByteArray())

        val cipherText64 = Base64.getEncoder().encodeToString(cipherText)
        assert(cipherText64 == "5Ud/GWF+R6IwIVgtfiNGRQ==")
    }

    @Test
    fun decryptionTest() {
        val cipherText64 = "5Ud/GWF+R6IwIVgtfiNGRQ=="
        val cipherText: ByteArray = Base64.getDecoder().decode(cipherText64)

        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)

        val plainTextByte: ByteArray = cipher.doFinal(cipherText)
        val plainText = String(plainTextByte, Charsets.UTF_8)

        assert(plainText == "Hello World")
    }

    @Test
    fun sendSms() {
        val start = "\u0001".toByteArray(Charsets.ISO_8859_1)
        val startString = String(start, Charsets.ISO_8859_1)

        Log.v(TAG, "Start: $startString")

        smsManager.sendTextMessage(
            "09151046272",
            null,
            "$startString${startString}Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do ei0$startString$startString",
            null,
            null
        )
    }
}