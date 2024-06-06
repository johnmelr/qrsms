package com.github.johnmelr.qrsms.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MessageSequenceToggleDao {
    @Query("SELECT * FROM messagesequencetoggle WHERE phoneNumber IS (:phoneNumber)")
    fun whichToggle(phoneNumber: String): MessageSequenceToggle

    @Insert
    fun insertToggleFor(phoneNumber: MessageSequenceToggle)

    @Update
    fun updateToggleOf(phoneNumber: MessageSequenceToggle)
}