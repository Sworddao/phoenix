package com.sworddao.phoenix.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Shared JSON helpers for persisting nested (non-columnar) domain values
 * inside Room entities. All serialized models must be @Serializable.
 */
object RoomJson {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    inline fun <reified T> toJson(value: T): String = json.encodeToString(value)

    inline fun <reified T> toJsonOrNull(value: T?): String? =
        value?.let { runCatching { json.encodeToString(it) }.getOrNull() }

    inline fun <reified T> fromJsonOrNull(jsonString: String?): T? =
        jsonString?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }

    inline fun <reified T> toJsonList(value: List<T>): String = json.encodeToString(value)

    inline fun <reified T> fromJsonList(jsonString: String?): List<T> =
        fromJsonOrNull<List<T>>(jsonString) ?: emptyList()

    inline fun <reified T> fromJsonMap(jsonString: String?): Map<String, T> =
        fromJsonOrNull<Map<String, T>>(jsonString) ?: emptyMap()
}
