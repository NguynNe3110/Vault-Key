package com.example.domain.model

data class VaultContents(
    val accounts: List<AccountItem> = emptyList(),
    val levels: List<SecurityLevel> = SecurityLevel.getDefaultLevels(),
    val rules: List<MaskingRule> = MaskingRule.defaults()
)

object VaultDefaults {
    val categories = listOf("Cá nhân", "Công việc", "Tài chính", "Giải trí", "Xã hội")
}
