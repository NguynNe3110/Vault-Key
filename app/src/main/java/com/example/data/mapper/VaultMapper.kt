package com.example.data.mapper

import com.example.data.local.entity.*
import com.example.domain.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object CustomFieldsJson {
    private val adapter = Moshi.Builder().build().adapter<Map<String, String>>(
        Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
    fun decode(json: String): Map<String, String> = runCatching { adapter.fromJson(json) }.getOrNull().orEmpty()
    fun encode(fields: Map<String, String>): String = adapter.toJson(fields)
}

fun AccountEntity.toDomain() = AccountItem(id, title, category, username, securityLevelId,
    CustomFieldsJson.decode(customFieldsJson), updatedAt, isDeleted, kind, maskedIdentifier, maskRuleId, isFavorite)
fun AccountItem.toEntity() = AccountEntity(id, title, category, username, securityLevelId,
    CustomFieldsJson.encode(customFields), updatedAt, isDeleted, kind, maskedIdentifier, maskRuleId, isFavorite)
fun SecurityLevelEntity.toDomain() = SecurityLevel(id, name, formatDescription)
fun SecurityLevel.toEntity() = SecurityLevelEntity(id, name, formatDescription)
fun MaskingRuleEntity.toDomain() = MaskingRule(id, name, pattern, keepLast, updatedAt, isDeleted)
fun MaskingRule.toEntity() = MaskingRuleEntity(id, name, pattern, keepLast, updatedAt, isDeleted)
