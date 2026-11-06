package com.example.data.repository

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.example.data.remote.datasource.CloudSnapshot
import com.example.data.remote.datasource.VaultCloud
import com.example.data.local.AppDatabase
import com.example.domain.model.AccountItem
import com.example.domain.model.MaskingRule
import com.example.domain.model.SecurityLevel
import com.example.data.mapper.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SyncResult(val categories: List<String>, val categoryRevision: Long)

/** A repository belongs to one profile for its whole lifetime, including in-flight syncs. */
class VaultProfileStore(val db: AppDatabase, val uid: String?, val preferences: SharedPreferences,
    private val firebase: VaultCloud) {
    private val syncMutex = Mutex()
    private val localMutex = Mutex()
    val accounts = db.accountDao().getAllAccounts().map { rows -> rows.map { it.toDomain() } }
    val levels = db.securityLevelDao().getAllSecurityLevels().map { rows -> rows.map { it.toDomain() } }
    val rules = db.maskingRuleDao().observe().map { rows -> rows.map { it.toDomain() } }

    suspend fun initialize() = db.withTransaction {
        if (db.securityLevelDao().getSecurityLevelsList().isEmpty())
            db.securityLevelDao().insertSecurityLevels(SecurityLevel.getDefaultLevels().map { it.toEntity() })
        if (db.maskingRuleDao().getAll().isEmpty()) db.maskingRuleDao().upsert(MaskingRule.defaults().map { it.toEntity() })
    }

    suspend fun save(account: AccountItem) = localMutex.withLock { db.accountDao().insertAccount(account.toEntity()) }
    suspend fun delete(id: String) = localMutex.withLock { db.accountDao().softDeleteAccount(id) }
    suspend fun saveRule(rule: MaskingRule) = localMutex.withLock { db.maskingRuleDao().upsert(listOf(rule.toEntity())) }
    suspend fun saveLevel(level: SecurityLevel) = localMutex.withLock {
        db.securityLevelDao().insertSecurityLevels(listOf(level.toEntity()))
        preferences.edit().putLong("levelsUpdatedAt", System.currentTimeMillis()).apply()
    }
    suspend fun setMaxLevel(max: Int) = localMutex.withLock {
        db.withTransaction {
            val old = db.securityLevelDao().getSecurityLevelsList()
            db.accountDao().reassignLevelsAbove(max)
            db.securityLevelDao().deleteLevelsAbove(max)
            db.securityLevelDao().insertSecurityLevels((1..max).filter { id -> old.none { it.id == id } }
                .map { SecurityLevel(it, "Mức $it", "Quy tắc của tôi").toEntity() })
        }
        preferences.edit().putLong("levelsUpdatedAt", System.currentTimeMillis()).apply()
    }

    suspend fun sync(categories: () -> List<String>): SyncResult = syncMutex.withLock {
        val userId = uid ?: return@withLock SyncResult(categories(), preferences.getLong("categoriesUpdatedAt", 0))
        val cloud = firebase.download(userId)
        check(firebase.currentUser.value?.uid == userId)
        // Network waits never hold the local-write lock: offline editing remains immediate.
        val snapshot = localMutex.withLock {
            val accounts = mergeAccounts(db.accountDao().getAllIncludingDeleted().map { it.toDomain() }, cloud.accounts)
            val rules = (cloud.rules + db.maskingRuleDao().getAll().map { it.toDomain() }).groupBy { it.id }
                .values.map { versions -> versions.maxWith(compareBy<MaskingRule> { it.updatedAt }.thenBy { it.isDeleted }) }
            val localTime = preferences.getLong("levelsUpdatedAt", 0)
            val levels = if (cloud.levels.isNotEmpty() && cloud.levelsUpdatedAt >= localTime) cloud.levels
                else db.securityLevelDao().getSecurityLevelsList().map { it.toDomain() }
            val catTime = preferences.getLong("categoriesUpdatedAt", 0)
            val mergedCategories = if (cloud.categories.isNotEmpty() && cloud.categoriesUpdatedAt >= catTime)
                cloud.categories else categories()
            db.withTransaction {
                db.accountDao().insertAccounts(accounts.map { it.toEntity() })
                db.maskingRuleDao().upsert(rules.map { it.toEntity() })
                db.securityLevelDao().clearAllLevels()
                db.securityLevelDao().insertSecurityLevels(levels.map { it.toEntity() })
            }
            val categoryRevision = maxOf(catTime, cloud.categoriesUpdatedAt)
            val edit = preferences.edit().putLong("levelsUpdatedAt", maxOf(localTime, cloud.levelsUpdatedAt))
            if (preferences.getLong("categoriesUpdatedAt", 0) == catTime) edit.putLong("categoriesUpdatedAt", categoryRevision)
            edit.apply()
            CloudSnapshot(accounts, levels, rules, mergedCategories,
                maxOf(localTime, cloud.levelsUpdatedAt), categoryRevision)
        }
        firebase.upload(userId, snapshot)
        SyncResult(snapshot.categories, snapshot.categoriesUpdatedAt)
    }

    companion object {
        /** Tombstones participate in conflict resolution so deleted items cannot reappear. */
        fun mergeAccounts(local: List<AccountItem>, remote: List<AccountItem>): List<AccountItem> =
            (remote + local).groupBy { it.id }.values.map { versions ->
                versions.maxWith(compareBy<AccountItem> { it.updatedAt }.thenBy { it.isDeleted })
            }
    }
}
