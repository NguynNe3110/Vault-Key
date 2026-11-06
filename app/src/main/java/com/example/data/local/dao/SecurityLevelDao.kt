package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityLevelDao {
    @Query("SELECT * FROM security_levels ORDER BY id ASC")
    fun getAllSecurityLevels(): Flow<List<SecurityLevelEntity>>

    @Query("SELECT * FROM security_levels ORDER BY id ASC")
    suspend fun getSecurityLevelsList(): List<SecurityLevelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityLevels(levels: List<SecurityLevelEntity>)

    @Query("DELETE FROM security_levels WHERE id > :maxLevel")
    suspend fun deleteLevelsAbove(maxLevel: Int)

    @Query("DELETE FROM security_levels")
    suspend fun clearAllLevels()
}
