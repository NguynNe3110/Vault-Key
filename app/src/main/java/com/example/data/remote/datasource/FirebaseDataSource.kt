package com.example.data.remote.datasource

import android.content.Context
import com.example.domain.model.UserSession
import com.example.data.mapper.CustomFieldsJson
import com.example.domain.model.AccountItem
import com.example.domain.model.MaskingRule
import com.example.domain.model.SecurityLevel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await


data class CloudSnapshot(
    val accounts: List<AccountItem>,
    val levels: List<SecurityLevel>,
    val rules: List<MaskingRule>,
    val categories: List<String>,
    val levelsUpdatedAt: Long = 0,
    val categoriesUpdatedAt: Long = 0
)

interface VaultCloud {
    val currentUser: StateFlow<UserSession?>
    suspend fun download(uid: String): CloudSnapshot
    suspend fun upload(uid: String, snapshot: CloudSnapshot)
}

class FirebaseDataSource(context: Context) : VaultCloud {
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private val _currentUser = MutableStateFlow<UserSession?>(null)
    override val currentUser = _currentUser.asStateFlow()
    private val _isFirebaseConfigured = MutableStateFlow(false)
    val isFirebaseConfigured = _isFirebaseConfigured.asStateFlow()
    private val _syncStatus = MutableStateFlow("Chỉ lưu trên thiết bị")
    val syncStatus = _syncStatus.asStateFlow()
    private val authListener = FirebaseAuth.AuthStateListener { updateUser() }

    init {
        runCatching {
            val app = FirebaseApp.initializeApp(context) ?: return@runCatching
            auth = FirebaseAuth.getInstance(app)
            firestore = FirebaseFirestore.getInstance(app)
            _isFirebaseConfigured.value = true
            auth?.addAuthStateListener(authListener)
            updateUser()
        }
    }

    private fun updateUser() {
        val previousUid = _currentUser.value?.uid
        _currentUser.value = auth?.currentUser?.let {
            UserSession(it.uid, it.email.orEmpty(), it.displayName ?: "Người dùng Google", it.photoUrl?.toString())
        }
        if (_currentUser.value == null || previousUid != _currentUser.value?.uid) {
            _syncStatus.value = if (_currentUser.value == null) "Chỉ lưu trên thiết bị" else "Đang chuẩn bị tự đồng bộ…"
        }
    }

    suspend fun loginWithGoogleToken(token: String) {
        val firebaseAuth = checkNotNull(auth) { "Chưa cấu hình đăng nhập Google." }
        firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        updateUser()
        check(_currentUser.value != null) { "Không thể xác thực tài khoản Google." }
    }

    fun logout() {
        auth?.signOut()
        updateUser()
    }

    fun dispose() { auth?.removeAuthStateListener(authListener) }

    fun setSyncStatus(uid: String, message: String) {
        if (_currentUser.value?.uid == uid) _syncStatus.value = message
    }

    private fun userDocument(uid: String) = checkNotNull(firestore).collection("users").document(uid)

    override suspend fun download(uid: String): CloudSnapshot {
        check(_currentUser.value?.uid == uid) { "Phiên đăng nhập đã thay đổi." }
        setSyncStatus(uid, "Đang đồng bộ…")
        val user = userDocument(uid)
        val levelDoc = user.collection("settings").document("security_levels").get(Source.SERVER).await()
        val categoryDoc = user.collection("settings").document("categories").get(Source.SERVER).await()
        val ruleDocs = user.collection("masking_rules").get(Source.SERVER).await()
        val accountDocs = user.collection("accounts").get(Source.SERVER).await()
        val levels = (levelDoc.get("levels") as? List<*>)?.mapNotNull { entry ->
            val row = entry as? Map<*, *> ?: return@mapNotNull null
            SecurityLevel((row["id"] as? Number)?.toInt() ?: return@mapNotNull null,
                row["name"] as? String ?: "", row["formatDescription"] as? String ?: "")
        }.orEmpty()
        val rules = ruleDocs.documents.mapNotNull { doc ->
            MaskingRule(doc.id, doc.getString("name").orEmpty(), doc.getString("pattern").orEmpty(),
                doc.getLong("keepLast")?.toInt() ?: 0, doc.getLong("updatedAt") ?: 0,
                doc.getBoolean("isDeleted") ?: false).takeIf { it.validate() == null }
        }
        val accounts = accountDocs.documents.map { doc ->
            AccountItem(id = doc.id, title = doc.getString("title").orEmpty(),
                category = doc.getString("category").orEmpty(), username = doc.getString("username").orEmpty(),
                securityLevelId = doc.getLong("securityLevelId")?.toInt() ?: 1,
                customFields = CustomFieldsJson.decode(doc.getString("customFieldsJson") ?: "{}"),
                updatedAt = doc.getLong("updatedAt") ?: 0, isDeleted = doc.getBoolean("isDeleted") ?: false,
                kind = doc.getString("kind") ?: "ACCOUNT", maskedIdentifier = doc.getString("maskedIdentifier").orEmpty(),
                maskRuleId = doc.getString("maskRuleId").orEmpty(), isFavorite = doc.getBoolean("isFavorite") ?: false)
        }
        return CloudSnapshot(accounts, levels, rules,
            (categoryDoc.get("categories") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            levelDoc.getLong("updatedAt") ?: 0, categoryDoc.getLong("updatedAt") ?: 0)
    }

    override suspend fun upload(uid: String, snapshot: CloudSnapshot) {
        check(_currentUser.value?.uid == uid) { "Phiên đăng nhập đã thay đổi." }
        val store = checkNotNull(firestore)
        val user = userDocument(uid)
        // Await every commit. A successful status means all writes were acknowledged by the server.
        for (chunk in snapshot.accounts.chunked(400)) {
            check(_currentUser.value?.uid == uid)
            val batch = store.batch()
            chunk.forEach { account -> batch.set(user.collection("accounts").document(account.id), mapOf(
                "id" to account.id, "title" to account.title, "category" to account.category,
                "username" to account.username, "securityLevelId" to account.securityLevelId,
                "customFieldsJson" to CustomFieldsJson.encode(account.customFields), "updatedAt" to account.updatedAt,
                "isDeleted" to account.isDeleted, "kind" to account.kind,
                "maskedIdentifier" to account.maskedIdentifier, "maskRuleId" to account.maskRuleId,
                "isFavorite" to account.isFavorite)) }
            batch.commit().await()
        }
        for (chunk in snapshot.rules.chunked(400)) {
            check(_currentUser.value?.uid == uid)
            val batch = store.batch()
            chunk.forEach { rule -> batch.set(user.collection("masking_rules").document(rule.id), mapOf(
                "id" to rule.id, "name" to rule.name, "pattern" to rule.pattern,
                "keepLast" to rule.keepLast, "updatedAt" to rule.updatedAt, "isDeleted" to rule.isDeleted)) }
            batch.commit().await()
        }
        check(_currentUser.value?.uid == uid)
        val batch = store.batch()
        batch.set(user.collection("settings").document("security_levels"),
            mapOf("levels" to snapshot.levels, "updatedAt" to snapshot.levelsUpdatedAt))
        batch.set(user.collection("settings").document("categories"),
            mapOf("categories" to snapshot.categories, "updatedAt" to snapshot.categoriesUpdatedAt))
        batch.commit().await()
        setSyncStatus(uid, "Đã đồng bộ")
    }
}
