package com.example.domain.repository

import com.example.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {
    val contents: StateFlow<VaultContents>
    val categories: StateFlow<List<String>>
    val ready: StateFlow<Boolean>
    val loadError: StateFlow<String?>
    val syncStatus: StateFlow<String>
    val isSyncing: StateFlow<Boolean>
    suspend fun save(account: AccountItem)
    suspend fun delete(id: String)
    suspend fun saveRule(rule: MaskingRule)
    suspend fun deleteRule(rule: MaskingRule)
    suspend fun saveLevel(level: SecurityLevel)
    suspend fun setMaxLevel(max: Int)
    suspend fun addCategory(name: String)
    suspend fun deleteCategory(name: String)
    fun requestSync()
}
