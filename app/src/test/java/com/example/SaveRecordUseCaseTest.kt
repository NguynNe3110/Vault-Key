package com.example

import com.example.domain.model.*
import com.example.domain.usecase.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SaveRecordUseCaseTest {
    private fun draft(raw: String, rule: MaskingRule) = RecordDraft(null, "Visa", "Tài chính", "Owner", 1, RecordKind.CARD, raw, rule, emptyMap(), false)
    @Test fun originalNumberNeverReachesTheRepository() = runTest {
        val repository = TestVaultRepository()
        SaveRecordUseCase(repository)(draft("123456", MaskingRule("example", "Example", "1=*, 5=%")))
        val saved = repository.contents.value.accounts.single()
        assertEquals("*234%6", saved.maskedIdentifier)
        assertFalse(saved.toString().contains("123456"))
    }
    @Test fun aRuleThatHidesNothingCannotSaveACard() = runTest {
        val repository = TestVaultRepository()
        try { SaveRecordUseCase(repository)(draft("1234", MaskingRule.defaults().first())); fail("Expected validation failure") }
        catch (expected: IllegalArgumentException) { assertTrue(repository.contents.value.accounts.isEmpty()) }
    }
    @Test fun editingMetadataPreservesPreviouslyMaskedNumberAndRule() = runTest {
        val repository = TestVaultRepository()
        val original = AccountItem("card", "Visa", "Tài chính", "Owner", 1, kind = "CARD", maskedIdentifier = "*234%6", maskRuleId = "old")
        SaveRecordUseCase(repository)(RecordDraft(original, "Visa mới", "Cá nhân", "Owner", 1, RecordKind.CARD, "", MaskingRule.defaults().first(), emptyMap(), true))
        val saved = repository.contents.value.accounts.single()
        assertEquals(original.id, saved.id)
        assertEquals(original.maskedIdentifier, saved.maskedIdentifier)
        assertEquals(original.maskRuleId, saved.maskRuleId)
        assertTrue(saved.isFavorite)
    }
}
