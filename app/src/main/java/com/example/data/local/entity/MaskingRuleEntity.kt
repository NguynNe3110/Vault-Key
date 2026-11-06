package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Positions are one-based and count characters, including spaces. Values are masked before storage. */
@Entity(tableName = "masking_rules")
data class MaskingRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pattern: String = "",
    val keepLast: Int = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false
)
