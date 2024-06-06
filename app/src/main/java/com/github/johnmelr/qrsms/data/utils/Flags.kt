package com.github.johnmelr.qrsms.data.utils

// Constants containing Unicode characters used as flag for message encryption.
object Flags {
    // String constants containing Unicode characters
    const val HEADING_START = "\u0001"
    const val TEXT_START = "\u0002"
    const val TEXT_END = "\u0003"
    const val END_TRANSMISSION = "\u0004"

    // Flags for encryption
    const val CIPHER_START = HEADING_START + TEXT_START
    const val CIPHER_END = TEXT_END + END_TRANSMISSION
    // For multipart encrypted text,
    const val CIPHER_CONTINUE_START = TEXT_START + TEXT_START
    const val CIPHER_CONTINUE = TEXT_END + TEXT_START
}