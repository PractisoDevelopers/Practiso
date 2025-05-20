package com.zhufucdev.practiso.platform

import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.takeWhile
import okio.Path

expect fun downloadRecursively(walker: DirectoryWalker, destination: Path): Flow<DownloadState>

expect fun downloadSingle(file: DownloadableFile, destination: Path): Flow<DownloadState>

sealed class DownloadState {
    data class Preparing(val filesFound: List<DownloadableFile>) : DownloadState()
    data class Downloading(val file: DownloadableFile, val progress: Float) : DownloadState()
    data class Completed(val file: DownloadableFile) : DownloadState()
}

/**
 * An asynchronized file sequence.
 */
interface DirectoryWalker {
    val identifier: String
    val files: Flow<DownloadableFile>
}

data class DownloadableFile(
    val name: String,
    val url: Url,
    val size: Long? = null,
    val sha256sum: String? = null,
)

class DownloadException : Exception {
    constructor(message: String) : super(message)
    constructor() : super()
}

sealed class GroupedDownloadState {
    data object Preparing : GroupedDownloadState()
    data class Progress(
        val ongoingDownloads: Map<DownloadableFile, Float>,
        val completedFiles: List<DownloadableFile>,
        val overallProgress: Float,
    ) : GroupedDownloadState()

    data object Completed : GroupedDownloadState()
}

fun Flow<DownloadState>.mapToGroup(): Flow<GroupedDownloadState> = flow {
    emit(GroupedDownloadState.Preparing)
    val files = (takeWhile { it is DownloadState.Preparing }
        .lastOrNull() as DownloadState.Preparing?)
        ?.filesFound ?: emptyList()
    val trackers = files.associateWith { 0f }.toMutableMap()
    val completed = mutableListOf<DownloadableFile>()

    fun calculateOverallProgress(): Float {
        val defaultSize = (1 shr 12).toLong()
        val totalTransfer = trackers.keys.sumOf { it.size ?: defaultSize }
        return trackers.entries.sumOf { (file, progress) ->
            (file.size ?: defaultSize).toDouble() / totalTransfer * progress
        }.toFloat()
    }

    collect {
        when (it) {
            is DownloadState.Completed -> {
                completed.add(it.file)
                if (completed.size == files.size) {
                    emit(GroupedDownloadState.Completed)
                } else {
                    trackers.remove(it.file)
                    emit(
                        GroupedDownloadState.Progress(
                            trackers,
                            completed,
                            calculateOverallProgress()
                        )
                    )
                }
            }

            is DownloadState.Downloading -> {
                trackers[it.file] = it.progress
                emit(GroupedDownloadState.Progress(trackers, completed, calculateOverallProgress()))
            }

            is DownloadState.Preparing -> {}
        }
    }
}