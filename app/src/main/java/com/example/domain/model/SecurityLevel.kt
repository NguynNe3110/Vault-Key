package com.example.domain.model


data class SecurityLevel(
    val id: Int,
    val name: String,
    val formatDescription: String
) {
    companion object {
        fun getDefaultLevels(): List<SecurityLevel> {
            return listOf(
                SecurityLevel(1, "Mức 1", "abc (Chỉ chứa chữ thường)"),
                SecurityLevel(2, "Mức 2", "Abc (Chữ thường và viết hoa)"),
                SecurityLevel(3, "Mức 3", "@Abc (Chữ hoa, thường và ký tự đặc biệt)")
            )
        }
    }
}
