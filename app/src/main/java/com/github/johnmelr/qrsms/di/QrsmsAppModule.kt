package com.github.johnmelr.qrsms.di

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.graphics.DiscretePathEffect
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Database
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.github.johnmelr.qrsms.crypto.KeyManager
import com.github.johnmelr.qrsms.crypto.KeyStoreManager
import com.github.johnmelr.qrsms.crypto.KeysRepository
import com.github.johnmelr.qrsms.data.contacts.ContactsRepository
import com.github.johnmelr.qrsms.data.messages.SmsRepository
import com.github.johnmelr.qrsms.data.preferencesDataStore.PreferencesRepository
import com.github.johnmelr.qrsms.data.room.DatabasePassphrase
import com.github.johnmelr.qrsms.data.room.KeyPairDatabase
import com.github.johnmelr.qrsms.data.room.KeyPairDatabaseFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.KeyStore
import javax.inject.Singleton

const val USER_PREFERENCES_KEY = "user_preference"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = USER_PREFERENCES_KEY)

@Module
@InstallIn(SingletonComponent::class)
object QrsmsAppModule {
    @Provides
    @Singleton
    fun provideDispatcherIO(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideSmsRepository(
        dispatcher: CoroutineDispatcher,
        contentResolver: ContentResolver,
        contactsRepository: ContactsRepository
    ) :SmsRepository {
        return SmsRepository(dispatcher, contentResolver, contactsRepository)
    }

    @Provides
    @Singleton
    fun provideContactsRepository(
        dispatcher: CoroutineDispatcher,
        contentResolver: ContentResolver,
    ): ContactsRepository {
        return ContactsRepository(dispatcher, contentResolver)
    }

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext app: Context): DataStore<Preferences> {
       return app.dataStore
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferencesDataStore: DataStore<Preferences>
    ): PreferencesRepository {
       return PreferencesRepository(preferencesDataStore)
    }

    @Provides
    @Singleton
    fun provideDatabasePassphrase(
        @ApplicationContext app: Context,
        preferencesRepository: PreferencesRepository,
    ): ByteArray {
        return DatabasePassphrase(app, preferencesRepository).getPassphrase()
    }

    @Provides
    @Singleton
    fun provideSqlCipherFactory(passphrase: ByteArray): SupportOpenHelperFactory {
        return SupportOpenHelperFactory(passphrase)
    }

    @Provides
    @Singleton
    fun provideKeyManager(keyStore: KeyStore, keysRepository: KeysRepository): KeyManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return KeyStoreManager(keyStore)
        }
        return keysRepository
    }

    @Provides
    @Singleton
    fun provideAndroidKeyStore(): KeyStore {
       return KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }

    @Provides
    @Singleton
    fun provideKeysRepository(db: KeyPairDatabase, keyStore: KeyStore): KeysRepository {
        return KeysRepository(db.keysDao, keyStore)
    }

    @Provides
    @Singleton
    fun provideKeyPairDatabase(
       app: Application,
       factory: SupportOpenHelperFactory
    ): KeyPairDatabase {
       return Room.databaseBuilder(
           app,
           KeyPairDatabase::class.java,
           KeyPairDatabase.DATABASE_NAME
       ).openHelperFactory(factory).build()
    }
}