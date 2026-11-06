package com.example

import com.example.domain.model.*
import com.example.data.repository.VaultProfileStore
import org.junit.Assert.*
import org.junit.Test

class VaultRulesTest {
    @Test fun masksDifferentSymbolsAtOneBasedPositions() {
        assertEquals("*234%6", MaskingEngine.mask("123456", MaskingRule("id", "Example", "1=*, 5=%")))
    }
    @Test fun rangesIgnorePositionsOutsideTheInput() {
        assertEquals("****56", MaskingEngine.mask("123456", MaskingRule("id", "Range", "1-4=*, 99=#")))
        assertEquals("123456", MaskingEngine.mask("123456", MaskingRule("id", "Outside", "99=*")))
    }
    @Test fun defaultKeepsOnlyLastFour() {
        val rule = MaskingRule.defaults().first()
        assertEquals("************1234", MaskingEngine.mask("1234567890121234", rule))
        assertFalse(MaskingEngine.hidesAny("1234", rule))
    }
    @Test fun invalidRulesNeverSilentlyExposeNumbers() {
        listOf("0=*", "5-1=*", "1=ab", "1=5", "1=A", "1=*;5=%", "1=* ,", "1001=*", "999999999999999999999=*", "1= ").forEach {
            assertNotNull(it, MaskingEngine.validate(it))
        }
        assertNotNull(MaskingRule("id", "Empty").validate())
    }
    @Test fun overlappingRulesUseTheLastReplacement() {
        assertEquals("**%456", MaskingEngine.mask("123456", MaskingRule("id", "Overlap", "1-3=*, 3=%")))
    }
    private fun account(id: String, timestamp: Long, deleted: Boolean = false) = AccountItem(id, "Example", "Work", "name", 1, emptyMap(), timestamp, deleted)
    @Test fun mergeKeepsNewerLocalEditsAndCloudOnlyRecords() {
        val merged = VaultProfileStore.mergeAccounts(listOf(account("a", 20)), listOf(account("a", 10), account("b", 15)))
        assertEquals(2, merged.size)
        assertEquals(20L, merged.first { it.id == "a" }.updatedAt)
    }
    @Test fun newerDeletionWinsAgainstOlderActiveRecord() {
        val merged = VaultProfileStore.mergeAccounts(listOf(account("a", 20, true)), listOf(account("a", 10)))
        assertTrue(merged.single().isDeleted)
    }
    @Test fun deletionWinsAtEqualTimestampsInEitherDirection() {
        assertTrue(VaultProfileStore.mergeAccounts(listOf(account("a", 20)), listOf(account("a", 20, true))).single().isDeleted)
        assertTrue(VaultProfileStore.mergeAccounts(listOf(account("a", 20, true)), listOf(account("a", 20))).single().isDeleted)
    }
}
