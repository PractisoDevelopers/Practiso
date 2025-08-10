package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.DownloadManager
import com.zhufucdev.practiso.platform.DownloadDiscretion
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.DownloadStopped
import com.zhufucdev.practiso.platform.DownloadableFile
import com.zhufucdev.practiso.platform.downloadSingle
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import opacity.client.ArchiveMetadata
import opacity.client.OpacityClient

class ArchiveHandle(
    val metadata: ArchiveMetadata,
    private val client: OpacityClient,
) {
    val taskId = "archive[id=${metadata.id}]"

    @Throws(DownloadException::class)
    suspend fun getAsSource(downloadManager: DownloadManager? = null): NamedSource {
        val fs = getPlatform().filesystem
        if (downloadManager != null) {
            val state = downloadManager[taskId].value
            if (state is DownloadState.Completed) {
                return NamedSource(
                    metadata.name,
                    fs.source(state.destination)
                )
            }
        }
        val url = with(client) {
            metadata.resourceUrl
        }
        val destination = getPlatform().createTemporaryFile("remote-archive", suffix = ".psarchive")

        return coroutineScope {
            val downloadManager = downloadManager ?: DownloadManager(this)
            val (tracker, state) =
                downloadManager.join(
                    taskId,
                    downloadSingle(
                        file = DownloadableFile(metadata.name, url),
                        destination = destination
                    ).onEach {
                        if (it is DownloadState.Configure) {
                            it.build.trySend {
                                discretion = DownloadDiscretion.Immediate
                            }
                        }
                    }
                )
            tracker.join()
            if (tracker.isCancelled) {
                fs.delete(destination)
                throw CancellationException("Tracker is cancelled")
            }
            val lastState = state.first()
            when (lastState) {
                is DownloadState.Completed ->
                    NamedSource(metadata.name, fs.source(destination))
                is DownloadStopped.Error ->
                    throw lastState.model
                else ->
                    throw AssertionError("Download not completed. Instead, last state is ${lastState.let { it::class.simpleName }}.")
            }
        }
    }
}
