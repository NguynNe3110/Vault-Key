package com.example.domain.usecase

import com.example.domain.model.*
import com.example.domain.repository.VaultRepository
import java.util.UUID

data class RecordDraft(
    val original: AccountItem?, val title: String, val category: String, val username: String,
    val levelId: Int, val kind: RecordKind, val rawIdentifier: String, val rule: MaskingRule?,
    val fields: Map<String, String>, val favorite: Boolean
)

/** The full identifier is transformed here and never reaches repository persistence. */
class SaveRecordUseCase(private val repository: VaultRepository) {
    suspend operator fun invoke(draft: RecordDraft) {
        require(draft.title.isNotBlank()) { "Nhập tên mục." }
        val needsNumber = draft.kind == RecordKind.CARD || draft.kind == RecordKind.BANK
        val masked = if (needsNumber && draft.rawIdentifier.isNotBlank()) {
            val rule = requireNotNull(draft.rule) { "Chọn quy tắc che số." }
            require(rule.validate() == null && MaskingEngine.hidesAny(draft.rawIdentifier, rule)) { "Quy tắc cần che ít nhất một số." }
            MaskingEngine.mask(draft.rawIdentifier, rule)
        } else if (needsNumber) draft.original?.maskedIdentifier.orEmpty() else ""
        require(!needsNumber || masked.isNotBlank()) { "Nhập số cần lưu." }
        repository.save(AccountItem(
            id = draft.original?.id ?: UUID.randomUUID().toString(), title = draft.title.trim(),
            category = draft.category, username = draft.username.trim(), securityLevelId = draft.levelId,
            customFields = draft.fields, updatedAt = System.currentTimeMillis(), kind = draft.kind.name,
            maskedIdentifier = masked, maskRuleId = if (draft.rawIdentifier.isNotBlank()) draft.rule?.id.orEmpty() else draft.original?.maskRuleId.orEmpty(),
            isFavorite = draft.favorite
        ))
    }
}
