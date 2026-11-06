package com.example.domain.model

data class AccountItem(
    val id: String,
    val title: String,
    val category: String,
    val username: String,
    val securityLevelId: Int,
    val customFields: Map<String, String> = emptyMap(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val kind: String = RecordKind.ACCOUNT.name,
    val maskedIdentifier: String = "",
    val maskRuleId: String = "",
    val isFavorite: Boolean = false
)

enum class RecordKind(val label: String, val plural: String) {
    ACCOUNT("Tài khoản", "Tài khoản"),
    CARD("Thẻ ngân hàng", "Thẻ ngân hàng"),
    BANK("Tài khoản ngân hàng", "Ngân hàng"),
    NOTE("Thông tin khác", "Thông tin khác");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.name == value } ?: ACCOUNT
    }
}
