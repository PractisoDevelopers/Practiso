package com.zhufucdev.practiso.platform

import io.ktor.http.Url
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.takeWhile
import okio.Path

expect fun downloadRecursively(walker: DirectoryWalker, destination: Path): Flow<DownloadState>

expect fun downloadSingle(file: DownloadableFile, destination: Path): Flow<DownloadState>

sealed class DownloadState {
    data class Preparing(val filesFound: List<DownloadableFile>) : DownloadState()
    data class Configure(val build: SendChannel<Configuration.() -> Unit>) : DownloadState()

    /**
     * @param progress ranging [0, 1) representing [no progress, completed),
     * though completion has its own state.
     */
    data class Downloading(val file: DownloadableFile, val progress: Float) : DownloadState()
    data class Completed(val file: DownloadableFile, val destination: Path) : DownloadState()
}

data class Configuration(
    var discretion: DownloadDiscretion = DownloadDiscretion.Immediate,
)

enum class DownloadDiscretion {
    Immediate,
    Discretionary
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
    data class Planed(
        val filesToDownload: List<DownloadableFile>,
        val configure: suspend (Configuration.() -> Unit) -> Unit,
    ) : GroupedDownloadState()

    data class Progress(
        val ongoingDownloads: Map<DownloadableFile, Float>,
        val completedFiles: List<DownloadableFile>,
        val overallProgress: Float,
    ) : GroupedDownloadState()

    data object Completed : GroupedDownloadState()
}

fun Flow<DownloadState>.mapToGroup(): Flow<GroupedDownloadState> = flow {
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

    collect { download ->
        when (download) {
            is DownloadState.Configure -> {
                emit(GroupedDownloadState.Planed(files) {
                    download.build.send(it)
                })
            }

            is DownloadState.Completed -> {
                completed.add(download.file)
                if (completed.size == files.size) {
                    emit(GroupedDownloadState.Completed)
                } else {
                    trackers.remove(download.file)
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
                trackers[download.file] = download.progress
                emit(GroupedDownloadState.Progress(trackers, completed, calculateOverallProgress()))
            }

            else -> {}
        }
    }
}