package com.github.johnmelr.qrsms.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [KeyPairEntry::class], version = 1)
abstract class KeyPairDatabase : RoomDatabase() {

    abstract val keysDao: KeysDao

    companion object {
        const val DATABASE_NAME = "keypair_db"
    }
}