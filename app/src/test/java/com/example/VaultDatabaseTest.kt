package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.mapper.*
import com.example.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [36])
class VaultDatabaseTest {
    @Test fun upgradesExistingVaultWithoutDeletingAccounts() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${System.nanoTime()}"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE accounts (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, category TEXT NOT NULL, username TEXT NOT NULL, securityLevelId INTEGER NOT NULL, customFieldsJson TEXT NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE security_levels (id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL, formatDescription TEXT NOT NULL)")
            old.execSQL("INSERT INTO accounts VALUES ('old', 'Gmail', 'Work', 'me@example.com', 3, '{}', 10, 0)")
            old.execSQL("INSERT INTO security_levels VALUES (3, 'Mức 3', 'My convention')")
            old.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val record = database.accountDao().getAccountById("old")!!
            assertEquals("Gmail", record.title)
            assertEquals("ACCOUNT", record.kind)
            assertEquals("", record.maskedIdentifier)
            assertFalse(record.isFavorite)
            assertEquals("My convention", database.securityLevelDao().getSecurityLevelsList().single().formatDescription)
            database.maskingRuleDao().upsert(MaskingRule.defaults().map { it.toEntity() })
            assertEquals(2, database.maskingRuleDao().getAll().size)
        } finally { database.close(); context.deleteDatabase(name) }
    }
    @Test fun maskedRecordsSurviveReloadAndDeletionKeepsATombstone() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val masked = MaskingEngine.mask("123456", MaskingRule("r", "Rule", "1=*,5=%"))
            database.accountDao().insertAccount(AccountItem("card", "Visa", "Finance", "Owner", 1, emptyMap(), kind = "CARD", maskedIdentifier = masked, maskRuleId = "r").toEntity())
            assertEquals("*234%6", database.accountDao().getAccountById("card")!!.maskedIdentifier)
            database.accountDao().softDeleteAccount("card", 50)
            assertTrue(database.accountDao().getAccountList().isEmpty())
            assertTrue(database.accountDao().getAllIncludingDeleted().single().isDeleted)
        } finally { database.close() }
    }
    @Test fun googleProfilesAndDeviceVaultUseDifferentDatabaseFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val local = AppDatabase.getDatabase(context)
        val first = AppDatabase.getDatabase(context, "google_a")
        val second = AppDatabase.getDatabase(context, "google_b")
        assertNotEquals(local.openHelper.databaseName, first.openHelper.databaseName)
        assertNotEquals(first.openHelper.databaseName, second.openHelper.databaseName)
    }
}
