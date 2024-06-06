package com.github.johnmelr.qrsms.crypto

import android.os.Build
import java.security.Key
import java.util.Base64
import android.util.Base64 as AndroidBase64

object Base64Utils {
    fun keyToBase64(key: Key): String {
        return if (Build.VERSION.SDK_INT >= 26) {
            Base64.getEncoder().encodeToString(key.encoded).toString()
        } else {
            AndroidBase64.encodeToString(key.encoded, AndroidBase64.DEFAULT).toString()
        }
    }

    fun byteToBase64(byte: ByteArray): String {
        return if (Build.VERSION.SDK_INT >= 26) {
            Base64.getEncoder().encodeToString(byte).toString()
        } else {
            AndroidBase64.encodeToString(byte, AndroidBase64.DEFAULT).toString()
        }
    }

    fun base64ToByteArray(base64String: String): ByteArray {
        return if (Build.VERSION.SDK_INT >= 26) {
            Base64.getDecoder().decode(base64String)
        } else {
            AndroidBase64.decode(base64String, AndroidBase64.DEFAULT)
        }
    }

    fun stringToBase64(string: String): String {
        val byte = string.toByteArray()

        return if (Build.VERSION.SDK_INT >= 26) {
            Base64.getEncoder().encodeToString(byte)
        } else {
            AndroidBase64.encodeToString(byte, AndroidBase64.DEFAULT)
        }
    }
}