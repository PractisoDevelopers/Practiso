package com.zhufucdev.practiso.route

import kotlinx.serialization.Serializable
import opacity.client.ArchiveMetadata

@Serializable
data class ArchivePreviewRouteParams(val metadata: ArchiveMetadata)
