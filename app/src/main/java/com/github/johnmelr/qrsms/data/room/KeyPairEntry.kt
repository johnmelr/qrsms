package com.github.johnmelr.qrsms.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class KeyPairEntry(
    @PrimaryKey @ColumnInfo(name = "address") val address: String,

    @ColumnInfo(name = "private_key") val privateKey: ByteArray,
    @ColumnInfo(name = "public_key") val publicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyPairEntry

        if (address != other.address) return false
        if (!privateKey.contentEquals(other.privateKey)) return false
        return publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + publicKey.contentHashCode()
        return result
    }
}