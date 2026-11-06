package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MaskingRuleDao {
    @Query("SELECT * FROM masking_rules WHERE isDeleted = 0 ORDER BY name")
    fun observe(): Flow<List<MaskingRuleEntity>>

    @Query("SELECT * FROM masking_rules")
    suspend fun getAll(): List<MaskingRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rules: List<MaskingRuleEntity>)
}
