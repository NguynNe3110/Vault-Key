package com.example

import com.example.data.remote.SyncIssues
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [36])
class SyncIssueTest {
    @Test fun permissionDeniedIdentifiesFirebaseConfiguration() {
        val issue = SyncIssues.from(FirebaseFirestoreException("Missing or insufficient permissions", FirebaseFirestoreException.Code.PERMISSION_DENIED))
        assertTrue(issue.message.contains("PERMISSION_DENIED"))
        assertFalse(issue.retryable)
    }
    @Test fun unavailableRetriesAutomatically() {
        assertTrue(SyncIssues.from(FirebaseFirestoreException("Offline", FirebaseFirestoreException.Code.UNAVAILABLE)).retryable)
    }
    @Test fun expiredLoginRequiresAuthentication() {
        val issue = SyncIssues.from(FirebaseFirestoreException("Expired", FirebaseFirestoreException.Code.UNAUTHENTICATED))
        assertFalse(issue.retryable)
        assertTrue(issue.message.contains("Đăng nhập Google lại"))
    }
    @Test fun missingDatabaseDoesNotPretendToBeANetworkFailure() {
        val issue = SyncIssues.from(FirebaseFirestoreException("Not found", FirebaseFirestoreException.Code.NOT_FOUND))
        assertFalse(issue.retryable)
        assertTrue(issue.message.contains("Firestore"))
    }
}
