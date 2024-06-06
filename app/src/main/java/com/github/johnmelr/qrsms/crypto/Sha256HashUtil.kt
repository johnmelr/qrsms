package com.github.johnmelr.qrsms.crypto

import java.lang.StringBuilder
import java.security.MessageDigest

object Sha256HashUtil {
    private val md = MessageDigest.getInstance("SHA-256")

    fun hash(string: String) : String {
        val stringBytes = string.toByteArray()

        return md.digest(stringBytes)
            .fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }
            .toString()
    }
}