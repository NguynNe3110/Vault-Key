package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String, // UUID to easily sync with Firestore
    val title: String,
    val category: String, // e.g., "Giải trí", "Công việc", "Tài chính", "Xã hội"
    val username: String,
    val securityLevelId: Int, // references SecurityLevel.id
    val customFieldsJson: String, // Moshi serialized Map<String, String>
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false, // soft delete for synchronization
    @ColumnInfo(defaultValue = "'ACCOUNT'") val kind: String = "ACCOUNT",
    @ColumnInfo(defaultValue = "''") val maskedIdentifier: String = "",
    @ColumnInfo(defaultValue = "''") val maskRuleId: String = "",
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false
)
