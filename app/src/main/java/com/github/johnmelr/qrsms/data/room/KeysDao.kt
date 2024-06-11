package com.github.johnmelr.qrsms.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import java.security.PrivateKey
import java.security.PublicKey

@Dao
interface KeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyPair(keypair: KeyPairEntry)

    @Delete
    suspend fun delete(keypair: KeyPairEntry)

    @Query("SELECT * FROM KeyPairEntry WHERE address=:address")
    suspend fun getKeyPairOfAddress(address: String): KeyPairEntry?

    @Query("SELECT private_key FROM KeyPairEntry where address=:address")
    suspend fun getPrivateKeyOfAddress(address: String): ByteArray?

    @Query("SELECT public_key FROM KeyPairEntry where address=:address")
    suspend fun getPublicKeyOfAddress(address: String): ByteArray?
}