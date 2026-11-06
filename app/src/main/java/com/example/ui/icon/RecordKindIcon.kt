package com.example.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.domain.model.RecordKind

fun RecordKind.icon(): ImageVector = when (this) {
    RecordKind.ACCOUNT -> Icons.Outlined.Key
    RecordKind.CARD -> Icons.Outlined.CreditCard
    RecordKind.BANK -> Icons.Outlined.AccountBalance
    RecordKind.NOTE -> Icons.Outlined.Description
}
fun RecordKind.shortLabel(): String = when(this) {
    RecordKind.ACCOUNT -> "Tài khoản"; RecordKind.CARD -> "Thẻ"; RecordKind.BANK -> "Ngân hàng"; RecordKind.NOTE -> "Ghi chú"
}
