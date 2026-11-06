package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun getAccountList(): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY updatedAt DESC")
    suspend fun getAllIncludingDeleted(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Query("UPDATE accounts SET securityLevelId = :maxLevel, updatedAt = :timestamp WHERE securityLevelId > :maxLevel")
    suspend fun reassignLevelsAbove(maxLevel: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteAccount(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM accounts WHERE updatedAt > :lastSyncTime")
    suspend fun getUnsyncedAccounts(lastSyncTime: Long): List<AccountEntity>

}
