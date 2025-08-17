package com.zhufucdev.practiso

import opacity.client.ArchiveMetadata

const val DEFAULT_DIMOJI = "📝"
val ArchiveMetadata.uiSharedId get() = "archive-$id"