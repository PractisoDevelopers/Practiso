package com.zhufucdev.practiso.route

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import opacity.client.ArchiveMetadata

object ArchiveMetadataNavType : NavType<ArchiveMetadata>(false) {
    @OptIn(ExperimentalSerializationApi::class)
    override fun put(
        bundle: SavedState,
        key: String,
        value: ArchiveMetadata,
    ) {
        bundle.write {
            putString(key, Json.encodeToString(value))
        }
    }

    override fun get(
        bundle: SavedState,
        key: String,
    ): ArchiveMetadata? {
        return bundle.read {
            val source = getString(key)
            runCatching {
                Json.decodeFromString<ArchiveMetadata>(source)
            }.getOrNull()
        }
    }

    override fun parseValue(value: String): ArchiveMetadata = Json.decodeFromString(value)

    override fun serializeAsValue(value: ArchiveMetadata): String = Json.encodeToString(value)
}