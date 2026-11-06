package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.NetworkUnavailableException
import com.example.data.remote.SyncIssues
import com.example.data.remote.datasource.FirebaseDataSource
import com.example.domain.model.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.VaultRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Owns persistence, profile isolation and automatic synchronization; independent of screen lifecycles. */
class VaultRepositoryImpl(
    private val context: Context,
    private val auth: AuthRepository,
    private val cloud: FirebaseDataSource,
    private val scope: CoroutineScope,
    private val databaseFactory: (String?) -> AppDatabase = { AppDatabase.getDatabase(context, it) }
) : VaultRepository {
    private val listAdapter = Moshi.Builder().build().adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java))
    private val _contents = MutableStateFlow(VaultContents())
    override val contents = _contents.asStateFlow()
    private val _categories = MutableStateFlow(VaultDefaults.categories)
    override val categories = _categories.asStateFlow()
    private val _ready = MutableStateFlow(false)
    override val ready = _ready.asStateFlow()
    private val _loadError = MutableStateFlow<String?>(null)
    override val loadError = _loadError.asStateFlow()
    override val syncStatus = cloud.syncStatus
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing = _isSyncing.asStateFlow()
    private var profile: VaultProfileStore? = null
    private var autoSync: AutoSyncQueue? = null
    private val network = NetworkMonitor(context) { scope.launch { requestSync() } }

    init {
        scope.launch {
            auth.currentUser.collectLatest { user ->
                _ready.value = false; _loadError.value = null; _contents.value = VaultContents()
                autoSync?.close(); autoSync = null; _isSyncing.value = false
                val preferences = context.getSharedPreferences("vault_settings_${user?.uid ?: "local"}", 0)
                val store = VaultProfileStore(databaseFactory(user?.uid), user?.uid, preferences, cloud)
                profile = store
                _categories.value = runCatching { listAdapter.fromJson(preferences.getString("categories", "").orEmpty()) }
                    .getOrNull()?.takeIf { it.isNotEmpty() } ?: VaultDefaults.categories
                try {
                    store.initialize()
                    coroutineScope {
                        launch {
                            combine(store.accounts, store.levels, store.rules) { accounts, levels, rules -> VaultContents(accounts, levels, rules) }
                                .collect { _contents.value = it; _ready.value = true }
                        }
                        if (user != null) {
                            autoSync = AutoSyncQueue(this, attempt = {
                                if (profile !== store || auth.currentUser.value?.uid != user.uid) throw CancellationException()
                                if (!network.isConnected) throw NetworkUnavailableException()
                                _isSyncing.value = true
                                try {
                                    val result = withTimeout(25_000) { store.sync { _categories.value } }
                                    if (profile === store && preferences.getLong("categoriesUpdatedAt", 0) == result.categoryRevision) {
                                        _categories.value = result.categories
                                        preferences.edit().putString("categories", listAdapter.toJson(result.categories)).apply()
                                    }
                                } finally { _isSyncing.value = false }
                            }, onFailure = { error ->
                                val issue = SyncIssues.from(error)
                                cloud.setSyncStatus(user.uid, issue.message)
                                issue.retryable
                            })
                            requestSync()
                        }
                        awaitCancellation()
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { _loadError.value = "Không mở được kho. Hãy đóng rồi mở lại ứng dụng." }
            }
        }
    }

    override fun requestSync() { autoSync?.request() }
    private suspend fun write(action: suspend (VaultProfileStore) -> Unit) {
        val store = checkNotNull(profile) { "Kho chưa sẵn sàng." }
        action(store)
        if (profile === store) {
            store.uid?.let { cloud.setSyncStatus(it, "Đã lưu trên máy • Đang chờ tự đồng bộ") }
            requestSync()
        }
    }
    override suspend fun save(account: AccountItem) = write { store ->
        require(contents.value.levels.any { it.id == account.securityLevelId }) { "Chọn lại mức mật khẩu trước khi lưu." }
        store.save(account)
    }
    override suspend fun delete(id: String) = write { it.delete(id) }
    override suspend fun saveRule(rule: MaskingRule) {
        require(rule.validate() == null) { rule.validate().orEmpty() }
        require(contents.value.rules.none { it.id != rule.id && it.name.equals(rule.name.trim(), true) }) { "Tên quy tắc đã tồn tại." }
        write { it.saveRule(rule.copy(name = rule.name.trim(), updatedAt = System.currentTimeMillis())) }
    }
    override suspend fun deleteRule(rule: MaskingRule) {
        require(contents.value.rules.size > 1) { "Giữ lại ít nhất một quy tắc." }
        write { it.saveRule(rule.copy(isDeleted = true, updatedAt = System.currentTimeMillis())) }
    }
    override suspend fun saveLevel(level: SecurityLevel) {
        require(level.name.isNotBlank() && level.formatDescription.isNotBlank()) { "Nhập tên và quy ước mật khẩu." }
        write { it.saveLevel(level.copy(name = level.name.trim(), formatDescription = level.formatDescription.trim())) }
    }
    override suspend fun setMaxLevel(max: Int) {
        require(max in 1..10)
        write { it.setMaxLevel(max) }
    }
    override suspend fun addCategory(name: String) {
        val value = name.trim()
        require(value.isNotBlank() && categories.value.none { it.equals(value, true) }) { "Tên danh mục trống hoặc đã tồn tại." }
        saveCategories(categories.value + value)
    }
    override suspend fun deleteCategory(name: String) {
        require(categories.value.size > 1) { "Giữ lại ít nhất một danh mục." }
        require(contents.value.accounts.none { it.category == name }) { "Chuyển các mục sang danh mục khác trước khi xóa." }
        saveCategories(categories.value - name)
    }
    private fun saveCategories(values: List<String>) {
        _categories.value = values
        profile?.preferences?.edit()?.putString("categories", listAdapter.toJson(values))
            ?.putLong("categoriesUpdatedAt", System.currentTimeMillis())?.apply()
        requestSync()
    }
    fun close() { network.close() }
}
