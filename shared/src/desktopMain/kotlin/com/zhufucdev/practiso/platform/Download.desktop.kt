package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.Path

actual fun downloadRecursively(
    walker: DirectoryWalker,
    destination: Path,
): Flow<DownloadState> = flow {
    TODO()
}

actual fun downloadSingle(file: DownloadableFile, destination: Path): Flow<DownloadState> = flow {
    TODO()
}