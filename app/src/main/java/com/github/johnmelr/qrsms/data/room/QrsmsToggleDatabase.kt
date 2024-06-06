package com.github.johnmelr.qrsms.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageSequenceToggle::class], version = 1)
abstract class QrsmsToggleDatabase: RoomDatabase() {
    abstract fun messageToggleDao(): MessageSequenceToggleDao
}