package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.datamodel.ArchiveHandle
import com.zhufucdev.practiso.datamodel.NextPointerBasedPaginated
import com.zhufucdev.practiso.datamodel.Paginated
import com.zhufucdev.practiso.platform.PlatformHttpClientFactory
import opacity.client.ArchiveMetadata
import opacity.client.ArchivePreview
import opacity.client.BonjourResponse
import opacity.client.DimensionMetadata
import opacity.client.OpacityClient
import opacity.client.SortOptions

class CommunityService(
    endpoint: String = DEFAULT_COMMUNITY_SERVER_URL,
) {
    private val client = OpacityClient(endpoint, PlatformHttpClientFactory)

    fun getArchivePagination(sortOptions: SortOptions = SortOptions()): Paginated<ArchiveMetadata> =
        object : NextPointerBasedPaginated<ArchiveMetadata, String>() {
            override suspend fun getFirstPage(): Pair<List<ArchiveMetadata>, String?> {
                return client.getArchiveList(sortOptions).let { it.page to it.next }
            }

            override suspend fun getFollowingPages(priorPointer: String): Pair<List<ArchiveMetadata>, String?> {
                return client.getArchiveList(sortOptions, priorPointer).let { it.page to it.next }
            }
        }

    fun getDimensionArchivePagination(
        dimensionName: String,
        sortOptions: SortOptions = SortOptions(),
    ): Paginated<ArchiveMetadata> =
        object : NextPointerBasedPaginated<ArchiveMetadata, String>() {
            override suspend fun getFirstPage(): Pair<List<ArchiveMetadata>, String?> {
                return client.getDimensionArchiveList(dimensionName, sortOptions)
                    .let { it.page to it.next }
            }

            override suspend fun getFollowingPages(priorPointer: String): Pair<List<ArchiveMetadata>, String?> {
                return client.getDimensionArchiveList(dimensionName, sortOptions, priorPointer)
                    .let { it.page to it.next }
            }
        }


    fun ArchiveMetadata.toHandle(): ArchiveHandle = ArchiveHandle(this, client)

    suspend fun getDimensions(takeFirst: Int = 20): List<DimensionMetadata> =
        client.getDimensionList(takeFirst)

    suspend fun getArchivePreview(archiveId: String): List<ArchivePreview> =
        client.getArchivePreview(archiveId)

    suspend fun getServerInfo(): BonjourResponse = client.getBonjour()
}


const val DEFAULT_COMMUNITY_SERVER_URL = "https://opacity.zhufucdev.com"