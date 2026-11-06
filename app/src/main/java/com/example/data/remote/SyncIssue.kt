package com.example.data.remote

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException

data class SyncIssue(val message: String, val retryable: Boolean)

class NetworkUnavailableException : Exception()

object SyncIssues {
    fun from(error: Throwable): SyncIssue {
        val firestore = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        return when (firestore?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> SyncIssue(
                "Firebase chưa cấp quyền đồng bộ (PERMISSION_DENIED). Dữ liệu vẫn được lưu trên máy.", false)
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> SyncIssue(
                "Phiên đăng nhập đã hết hạn. Đăng nhập Google lại để tiếp tục đồng bộ.", false)
            FirebaseFirestoreException.Code.NOT_FOUND -> SyncIssue(
                "Chưa tìm thấy cơ sở dữ liệu Firestore. Dữ liệu vẫn được lưu trên máy.", false)
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> SyncIssue(
                "Cấu hình Firestore chưa sẵn sàng (FAILED_PRECONDITION). Dữ liệu vẫn được lưu trên máy.", false)
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> SyncIssue(
                "Không thể gửi dữ liệu lên máy chủ (INVALID_ARGUMENT). Dữ liệu vẫn được lưu trên máy.", false)
            else -> if (error is NetworkUnavailableException || error is TimeoutCancellationException || firestore != null) {
                SyncIssue("Đã lưu trên máy • Sẽ tự đồng bộ khi kết nối ổn định", true)
            } else SyncIssue("Chưa thể đồng bộ. Dữ liệu vẫn được lưu trên máy; thử mở lại ứng dụng.", false)
        }
    }
}
