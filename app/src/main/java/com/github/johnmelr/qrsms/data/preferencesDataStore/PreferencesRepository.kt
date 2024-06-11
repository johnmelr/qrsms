package com.github.johnmelr.qrsms.data.preferencesDataStore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

const val TAG = "PreferencesRepository"

/**
 *
 */
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {
    val defaultPhoneNumberFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val defaultPhoneNumber = preferences[DEFAULT_PHONE_NUMBER] ?: "none"
            defaultPhoneNumber
        }

    val databasePassphrase: Flow<ByteArray> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG,"Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val databasePassphrase = preferences[DATABASE_PASSPHRASE] ?: byteArrayOf()
            databasePassphrase
        }

    val isFirstLaunch: Flow<Boolean?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG,"Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val databasePassphrase = preferences[IS_FIRST_LAUNCH]
            databasePassphrase
        }

    suspend fun setDefaultPhoneNumber(newValue: String) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_PHONE_NUMBER] = newValue }
    }

    suspend fun completeFirstLaunch() {
        dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }

    suspend fun updatePassphrase(passphrase: ByteArray) {
        dataStore.edit { preferences ->
            preferences[DATABASE_PASSPHRASE] = passphrase
        }
    }

    suspend fun clearDataStore() {
        Log.v(TAG, "Clearing Preferences DataStore")
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object PreferencesKey {
        val DEFAULT_PHONE_NUMBER = stringPreferencesKey("default_phone_number")
        val DATABASE_PASSPHRASE = byteArrayPreferencesKey("database_passphrase")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }
}