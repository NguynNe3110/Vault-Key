package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.datasource.*
import com.example.data.local.AppDatabase
import com.example.domain.model.*
import com.example.data.repository.VaultProfileStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [36])
class VaultSyncTest {
    private class Cloud : VaultCloud {
        override val currentUser = MutableStateFlow<UserSession?>(UserSession("user", "test@example.com", "Test"))
        var snapshot = CloudSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
        var uploaded: CloudSnapshot? = null
        var failure = false
        var gate: CompletableDeferred<Unit>? = null
        val entered = CompletableDeferred<Unit>()
        override suspend fun download(uid: String): CloudSnapshot {
            entered.complete(Unit)
            gate?.await()
            check(!failure) { "Offline" }
            return snapshot
        }
        override suspend fun upload(uid: String, snapshot: CloudSnapshot) { uploaded = snapshot }
    }
    private fun record(id: String, time: Long, deleted: Boolean = false) = AccountItem(id, "Record $id", "Cá nhân", "name", 1, emptyMap(), time, deleted)

    @Test fun emptyCloudPreservesLocalRecordsAndUploadsTombstones() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val cloud = Cloud()
            val repo = VaultProfileStore(db, "user", context.getSharedPreferences("sync-${System.nanoTime()}", 0), cloud)
            repo.initialize()
            repo.save(record("active", 20)); repo.save(record("deleted", 30, true))
            repo.sync { listOf("Cá nhân") }
            assertEquals(1, db.accountDao().getAccountList().size)
            assertEquals(2, cloud.uploaded!!.accounts.size)
            assertTrue(cloud.uploaded!!.accounts.first { it.id == "deleted" }.isDeleted)
        } finally { db.close() }
    }
    @Test fun failedDownloadDoesNotEraseLocalData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val cloud = Cloud().apply { failure = true }
            val repo = VaultProfileStore(db, "user", context.getSharedPreferences("sync-${System.nanoTime()}", 0), cloud)
            repo.initialize(); repo.save(record("local", 20))
            assertTrue(runCatching { repo.sync { listOf("Cá nhân") } }.isFailure)
            assertEquals("local", db.accountDao().getAccountList().single().id)
            assertNull(cloud.uploaded)
        } finally { db.close() }
    }
    @Test fun editingDuringSlowDownloadRemainsImmediateAndIsIncludedInMerge() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val cloud = Cloud().apply { gate = CompletableDeferred() }
            val repo = VaultProfileStore(db, "user", context.getSharedPreferences("sync-${System.nanoTime()}", 0), cloud)
            repo.initialize()
            val syncing = async { repo.sync { listOf("Cá nhân") } }
            cloud.entered.await()
            withTimeout(2000) { repo.save(record("new", 50)) }
            cloud.gate!!.complete(Unit)
            syncing.await()
            assertEquals("new", cloud.uploaded!!.accounts.single().id)
        } finally { db.close() }
    }
}
