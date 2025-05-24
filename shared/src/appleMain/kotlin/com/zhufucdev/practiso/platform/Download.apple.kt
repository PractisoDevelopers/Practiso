package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.convert.toNSURL
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import okio.Path
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileManagerItemReplacementUsingNewMetadataOnly
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.darwin.NSObject
import platform.posix.int64_t

actual fun downloadRecursively(
    walker: DirectoryWalker,
    destination: Path,
): Flow<DownloadState> = channelFlow {
    val files = walker.files
        .runningFold(mutableListOf<DownloadableFile>()) { acc, file ->
            acc.add(file)
            send(DownloadState.Preparing(acc))
            acc
        }
        .lastOrNull() ?: emptyList()
    val config = Configuration()
    suspend {
        val configChannel = Channel<Configuration.() -> Unit>()
        send(DownloadState.Configure(configChannel))
        val builder = configChannel.receive()
        builder.invoke(config)
    }()

    val urlSession = NSURLSession.sessionWithConfiguration(
        configuration = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
            setDiscretionary(config.discretion == DownloadDiscretion.Discretionary)
        }
    )
    coroutineScope {
        files.map { file ->
            async {
                val downloadTask = urlSession.downloadTaskWithURL(file.url.toNSURL()!!)
                val progressChannel = Channel<SingleFileURLSessionDownloadDelegate.Update>()
                val delegate =
                    SingleFileURLSessionDownloadDelegate(file.name, destination, progressChannel)

                downloadTask.setDelegate(delegate)
                if (file.size != null) {
                    downloadTask.setCountOfBytesClientExpectsToReceive(file.size)
                }
                downloadTask.resume()

                try {
                    progressChannel.receiveAsFlow()
                        .map { it.toDownloadState(file) }
                        .collect { send(it) }
                } finally {
                    downloadTask.cancel()
                }
            }
        }.awaitAll()
    }
}

actual fun downloadSingle(
    file: DownloadableFile,
    destination: Path,
): Flow<DownloadState> = flow {
    val config = Configuration()
    emit(DownloadState.Preparing(listOf(file)))
    suspend {
        val configChannel = Channel<Configuration.() -> Unit>()
        emit(DownloadState.Configure(configChannel))
        configChannel.receive().invoke(config)
    }()
    val session = NSURLSession.sessionWithConfiguration(
        configuration = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
            setDiscretionary(config.discretion == DownloadDiscretion.Discretionary)
        },
    )
    val downloadTask = session.downloadTaskWithURL(file.url.toNSURL()!!)
    val updateChannel = Channel<SingleFileURLSessionDownloadDelegate.Update>()
    downloadTask.setDelegate(
        SingleFileURLSessionDownloadDelegate(null, destination, updateChannel)
    )
    downloadTask.resume()
    updateChannel.receiveAsFlow().collect { emit(it.toDownloadState(file)) }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class SingleFileURLSessionDownloadDelegate internal constructor(
    private val fileName: String?,
    private val destination: Path,
    private val updateChannel: SendChannel<Update>,
) : NSObject(),
    NSURLSessionDownloadDelegateProtocol {
    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL,
    ) {
        updateChannel.trySend(Update.Progress(1f))
        val destinationPath = destination.let { if (fileName != null) it.resolve(fileName) else it }
        memScoped {
            val destinationUrl = alloc<ObjCObjectVar<NSURL?>> {
                value = destinationPath.toNSURL()
            }

            val err = alloc<ObjCObjectVar<NSError?>>()
            val destinationContainerPath =
                destinationUrl.value!!.URLByDeletingLastPathComponent!!.path!!
            if (!NSFileManager.defaultManager.fileExistsAtPath(destinationContainerPath)) {
                NSFileManager.defaultManager.createDirectoryAtPath(
                    path = destinationContainerPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = err.ptr
                )
                err.value?.localizedDescription?.let {
                    updateChannel.close(DownloadException(it))
                    return@memScoped
                }
            }

            NSFileManager.defaultManager.replaceItemAtURL(
                withItemAtURL = didFinishDownloadingToURL,
                originalItemURL = destinationUrl.value!!,
                backupItemName = null,
                options = NSFileManagerItemReplacementUsingNewMetadataOnly,
                resultingItemURL = destinationUrl.ptr,
                error = err.ptr
            )
            err.value?.localizedDescription?.let {
                updateChannel.close(DownloadException(it))
                return@memScoped
            }
        }

        updateChannel.trySend(Update.Completed)
        updateChannel.close()
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: int64_t,
        totalBytesWritten: int64_t,
        totalBytesExpectedToWrite: int64_t,
    ) {
        updateChannel.trySend(Update.Progress(totalBytesWritten.toFloat() / totalBytesExpectedToWrite))
    }

    internal sealed class Update {
        data object Completed : Update()
        data class Progress(val value: Float) : Update()
    }
}

internal fun SingleFileURLSessionDownloadDelegate.Update.toDownloadState(file: DownloadableFile): DownloadState =
    when (this) {
        SingleFileURLSessionDownloadDelegate.Update.Completed -> {
            DownloadState.Completed(file)
        }

        is SingleFileURLSessionDownloadDelegate.Update.Progress -> {
            DownloadState.Downloading(file, value)
        }
    }
