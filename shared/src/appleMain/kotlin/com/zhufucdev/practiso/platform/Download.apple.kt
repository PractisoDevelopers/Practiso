package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.bridge.toNSURL
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.runningFold
import okio.Path
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
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
): Flow<DownloadState> = flow {
    val files = walker.files
        .runningFold(mutableListOf<DownloadableFile>()) { acc, file ->
            acc.add(file)
            emit(DownloadState.Preparing(acc))
            acc
        }
        .lastOrNull() ?: emptyList()
    val urlSession = NSURLSession.sessionWithConfiguration(
        configuration = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(
            walker.identifier
        ).apply {
            setDiscretionary(true)
            setSessionSendsLaunchEvents(true)
        },
    )
    coroutineScope {
        files.map { file ->
            async {
                val backgroundTask = urlSession.downloadTaskWithURL(file.url.toNSURL())
                val progressChannel = Channel<SingleFileURLSessionDownloadDelegate.Update>()
                val delegate =
                    SingleFileURLSessionDownloadDelegate(file.name, destination, progressChannel)

                backgroundTask.setDelegate(delegate)
                if (file.size != null) {
                    backgroundTask.setCountOfBytesClientExpectsToReceive(file.size)
                }
                backgroundTask.resume()

                try {
                    progressChannel.receiveAsFlow()
                        .map { it.toDownloadState(file) }
                        .collect(this@flow)
                } finally {
                    backgroundTask.cancel()
                }
            }
        }.awaitAll()
    }
}

actual fun downloadSingle(
    file: DownloadableFile,
    destination: Path,
): Flow<DownloadState> {
    val updateChannel = Channel<SingleFileURLSessionDownloadDelegate.Update>()
    val session = NSURLSession.sessionWithConfiguration(
        configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
        delegate = SingleFileURLSessionDownloadDelegate(null, destination, updateChannel),
        delegateQueue = null
    )
    session.downloadTaskWithURL(file.url.toNSURL())
    return updateChannel.receiveAsFlow()
        .map { it.toDownloadState(file) }
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
        val destinationUrl = destination.let { if (fileName != null) it.resolve(fileName) else it }.toNSURL()
        memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            NSFileManager.defaultManager.createDirectoryAtURL(
                url = destinationUrl.URLByDeletingLastPathComponent!!,
                withIntermediateDirectories = false,
                attributes = null,
                error = err.ptr
            )
            err.value?.localizedDescription?.let {
                updateChannel.close(DownloadException(it))
                return@memScoped
            }

            NSFileManager.defaultManager.moveItemAtURL(
                srcURL = didFinishDownloadingToURL,
                toURL = destinationUrl,
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
