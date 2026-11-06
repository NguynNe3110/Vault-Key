package com.example

import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow

class TestAuthRepository : AuthRepository {
    override val currentUser = MutableStateFlow<UserSession?>(null)
    override val isAvailable = MutableStateFlow(true)
    override val webClientId = "test-client"
    override suspend fun authenticate(token: String) { currentUser.value = UserSession("test", "test@example.com", "Minh An") }
    override fun signOut() { currentUser.value = null }
}

class TestVaultRepository : VaultRepository {
    override val contents = MutableStateFlow(VaultContents())
    override val categories = MutableStateFlow(VaultDefaults.categories)
    override val ready = MutableStateFlow(true)
    override val loadError = MutableStateFlow<String?>(null)
    override val syncStatus = MutableStateFlow("Đã đồng bộ")
    override val isSyncing = MutableStateFlow(false)
    var syncRequests = 0
    override suspend fun save(account: AccountItem) { contents.value = contents.value.copy(accounts = contents.value.accounts.filterNot { it.id == account.id } + account); requestSync() }
    override suspend fun delete(id: String) { contents.value = contents.value.copy(accounts = contents.value.accounts.filterNot { it.id == id }); requestSync() }
    override suspend fun saveRule(rule: MaskingRule) { contents.value = contents.value.copy(rules = contents.value.rules.filterNot { it.id == rule.id } + rule) }
    override suspend fun deleteRule(rule: MaskingRule) { contents.value = contents.value.copy(rules = contents.value.rules.filterNot { it.id == rule.id }) }
    override suspend fun saveLevel(level: SecurityLevel) { contents.value = contents.value.copy(levels = contents.value.levels.map { if (it.id == level.id) level else it }) }
    override suspend fun setMaxLevel(max: Int) { contents.value = contents.value.copy(levels = (1..max).map { id -> contents.value.levels.find { it.id == id } ?: SecurityLevel(id, "Mức $id", "Quy ước của tôi") }) }
    override suspend fun addCategory(name: String) { categories.value = categories.value + name }
    override suspend fun deleteCategory(name: String) { categories.value = categories.value - name }
    override fun requestSync() { syncRequests++ }
}
