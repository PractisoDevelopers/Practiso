package com.zhufucdev.practiso.platform

import android.app.DownloadManager
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.zhufucdev.practiso.SharedContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import okio.Path
import kotlin.time.Duration.Companion.milliseconds

actual fun downloadRecursively(
    walker: DirectoryWalker,
    destination: Path,
): Flow<DownloadState> = channelFlow {
    val downloadManager = SharedContext.getSystemService<DownloadManager>()
        ?: error("DownloadManager is unavailable.")
    val files = walker.files
        .runningFold(mutableListOf<DownloadableFile>()) { accumulator, value ->
            accumulator.add(value)
            accumulator
        }
        .onEach {
            send(DownloadState.Preparing(it))
        }
        .lastOrNull() ?: emptyList()

    val config = Configuration()
    suspend {
        val builderChannel = Channel<Configuration.() -> Unit>()
        send(DownloadState.Configure(builderChannel))
        builderChannel.receive()(config)
    }()

    coroutineScope {
        files.map { file ->
            val taskId = downloadManager.enqueue(file, config)
            async {
                monitorDownload(downloadManager, taskId, file.size)
                    .collect { download ->
                        if (download is DMState.Completed) {
                            download.moveTo(destination.resolve(file.name))
                        }
                        send(download.toDownloadState(file))
                    }
            }
        }
            .awaitAll()
    }
}

actual fun downloadSingle(
    file: DownloadableFile,
    destination: Path,
): Flow<DownloadState> = flow {
    emit(DownloadState.Preparing(listOf(file)))

    val config = Configuration()
    val builderChannel = Channel<Configuration.() -> Unit>()
    emit(DownloadState.Configure(builderChannel))
    builderChannel.receive()(config)

    val downloadManager = SharedContext.getSystemService<DownloadManager>()
        ?: error("DownloadManager is unavailable.")
    val taskId = downloadManager.enqueue(file, config)
    monitorDownload(downloadManager, taskId, file.size).collect { download ->
        if (download is DMState.Completed) {
            download.moveTo(destination.resolve(file.name))
        }
        emit(download.toDownloadState(file))
    }
}

private fun DownloadManager.enqueue(file: DownloadableFile, configuration: Configuration): Long =
    enqueue(
        DownloadManager.Request(file.url.toString().toUri())
            .apply {
                setAllowedNetworkTypes(
                    if (configuration.discretion == DownloadDiscretion.Immediate) {
                        DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                    } else {
                        DownloadManager.Request.NETWORK_WIFI
                    }
                )
                setTitle(file.name)
            }
    )

private fun monitorDownload(
    downloadManager: DownloadManager,
    taskId: Long,
    fileSize: Long?,
): Flow<DMState> = flow {
    while (downloadManager.query(DownloadManager.Query().apply {
            setFilterById(taskId)
        }).use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use true
            }

            val status =
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_FAILED -> {
                    val code =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    throw DownloadException("Task failed with ${code.dmErrorDescription}")
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    val destination =
                        cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            .toUri()
                    emit(DMState.Completed(destination))
                    return@use false
                }
            }
            val bytesTransferred =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val totalBytes =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    .takeIf { b -> b > 0 }
                    ?: fileSize
                    ?: (1 shr 12).toLong()
            emit(
                DMState.Progress(bytesTransferred.toFloat() / totalBytes)
            )
            true
        }) {
        delay(200.milliseconds)
    }
}

private val Int.dmErrorDescription
    get() =
        when (this) {
            in 0 until 1000 -> "HTTP $this"
            DownloadManager.ERROR_UNKNOWN -> "unknown error"
            DownloadManager.ERROR_FILE_ERROR -> "file error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "data error"
            DownloadManager.ERROR_CANNOT_RESUME -> "resumption failure"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> error("This error should not happen")
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "file already existing"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "incompatible HTTP response"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "no enough space left"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "too many redirects"
            else -> error("Unknown Download Manager error code: $this")
        }

sealed class DMState {
    data class Completed(val destination: Uri) : DMState() {
        override fun toDownloadState(file: DownloadableFile): DownloadState {
            return DownloadState.Completed(file)
        }
    }

    data class Progress(val value: Float) : DMState() {
        override fun toDownloadState(file: DownloadableFile): DownloadState {
            return DownloadState.Downloading(file, progress = value)
        }
    }

    abstract fun toDownloadState(file: DownloadableFile): DownloadState
}

fun DMState.Completed.moveTo(destination: Path) {
    destination.toFile().outputStream().use {
        SharedContext.contentResolver.openInputStream(this.destination)!!.copyTo(it)
    }
}