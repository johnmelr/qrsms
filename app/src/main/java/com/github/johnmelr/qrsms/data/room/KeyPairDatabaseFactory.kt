package com.github.johnmelr.qrsms.data.room

import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class KeyPairDatabaseFactory(passphraseString: ByteArray):
SupportSQLiteOpenHelper.Factory {
    private val factory: SupportOpenHelperFactory = SupportOpenHelperFactory(passphraseString)

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration):
            SupportSQLiteOpenHelper {
        return factory.create(configuration)
    }

}