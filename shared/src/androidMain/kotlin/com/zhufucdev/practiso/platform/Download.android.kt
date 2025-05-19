package com.zhufucdev.practiso.platform

import kotlinx.coroutines.flow.Flow
import okio.Path

actual fun downloadRecursively(
    walker: DirectoryWalker,
    destination: Path,
): Flow<DownloadState> {

}