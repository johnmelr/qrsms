package com.github.johnmelr.qrsms.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * When sending an encrypted message, apart from the receiver's first 4 character in their phone
 * number's SHA-256 hash, a hidden string is appended so that for multipart message, we can identify
 * if the following message is part of that sequence. We can identify this if their hidden string
 * is the same. However, if we want to know which hidden string to use, the toggle column servers as
 * a flag on which string to use.
 */
@Entity
data class MessageSequenceToggle(
    @PrimaryKey val phoneNumber: String,
    @ColumnInfo(name = "toggle") val toggle: Int
)
