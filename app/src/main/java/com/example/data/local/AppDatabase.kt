package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SecurityLevelEntity
import com.example.data.local.entity.MaskingRuleEntity
import com.example.data.local.dao.*

@Database(entities = [AccountEntity::class, SecurityLevelEntity::class, MaskingRuleEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun securityLevelDao(): SecurityLevelDao
    abstract fun maskingRuleDao(): MaskingRuleDao

    companion object {
        private val instances = mutableMapOf<String, AppDatabase>()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN kind TEXT NOT NULL DEFAULT 'ACCOUNT'")
                db.execSQL("ALTER TABLE accounts ADD COLUMN maskedIdentifier TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN maskRuleId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE accounts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS masking_rules (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, pattern TEXT NOT NULL, keepLast INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL)")
            }
        }

        fun getDatabase(context: Context, uid: String? = null): AppDatabase {
            val name = if (uid == null) "vault_database" else "vault_${uid.replace(Regex("[^a-zA-Z0-9_-]"), "_")}"
            return synchronized(this) {
                instances.getOrPut(name) { Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    name
                )
                .addMigrations(MIGRATION_1_2)
                .build() }
            }
        }
    }
}
