package com.example.domain.model


/** Positions are one-based and count characters, including spaces. Values are masked before storage. */
data class MaskingRule(
    val id: String,
    val name: String,
    val pattern: String = "",
    val keepLast: Int = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false
) {
    fun validate(): String? = when {
        name.isBlank() -> "Nhập tên quy tắc."
        keepLast !in 0..4 -> "Chỉ được giữ tối đa 4 ký tự cuối."
        keepLast == 0 && pattern.isBlank() -> "Nhập vị trí cần che, ví dụ 1=*, 5=%."
        else -> MaskingEngine.validate(pattern)
    }

    companion object {
        fun defaults() = listOf(
            MaskingRule("last-four", "Chỉ hiện 4 số cuối", keepLast = 4),
            MaskingRule("example-positions", "Che vị trí 1 và 5", "1=*, 5=%")
        )
    }
}

object MaskingEngine {
    private val segment = Regex("^([1-9][0-9]*)(?:-([1-9][0-9]*))?=(.)$")

    fun validate(pattern: String): String? {
        if (pattern.isBlank()) return null
        for (part in pattern.split(',')) {
            val match = segment.matchEntire(part.trim())
                ?: return "Dùng dạng 1=*, 5=% hoặc 1-4=*."
            val start = match.groupValues[1].toIntOrNull()
                ?: return "Vị trí không hợp lệ."
            val end = match.groupValues[2].ifEmpty { match.groupValues[1] }.toIntOrNull()
                ?: return "Vị trí không hợp lệ."
            if (start > end || end > 1000) return "Vị trí phải từ 1 đến 1000, khoảng tăng dần."
            val symbol = match.groupValues[3].single()
            if (symbol.isLetterOrDigit() || symbol.isWhitespace() || symbol == ',' || symbol == '=') {
                return "Ký tự che phải là một ký hiệu, ví dụ *, %, # hoặc •."
            }
        }
        return null
    }

    fun mask(value: String, rule: MaskingRule): String {
        require(rule.validate() == null) { rule.validate().orEmpty() }
        val chars = value.toCharArray()
        if (rule.keepLast > 0) {
            for (index in 0 until (chars.size - rule.keepLast).coerceAtLeast(0)) chars[index] = '*'
        }
        if (rule.pattern.isNotBlank()) {
            for (part in rule.pattern.split(',')) {
                val match = segment.matchEntire(part.trim())!!
                val start = match.groupValues[1].toInt() - 1
                val end = match.groupValues[2].ifEmpty { match.groupValues[1] }.toInt() - 1
                for (index in start..minOf(end, chars.lastIndex)) chars[index] = match.groupValues[3].single()
            }
        }
        return String(chars)
    }

    fun hidesAny(value: String, rule: MaskingRule): Boolean = value != mask(value, rule)
}
