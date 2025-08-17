package com.zhufucdev.practiso.datamodel

import opacity.client.ArchiveMetadata

val ArchiveMetadata.downloadTaskId get() = "archive[id=${id}]"
