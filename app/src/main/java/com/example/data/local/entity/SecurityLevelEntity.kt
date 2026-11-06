package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_levels")
data class SecurityLevelEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val formatDescription: String
)
