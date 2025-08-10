package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.Download
import com.zhufucdev.practiso.DownloadContext
import com.zhufucdev.practiso.DownloadDispatcher
import com.zhufucdev.practiso.platform.DownloadDiscretion
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.DownloadableFile
import com.zhufucdev.practiso.platform.downloadSingle
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import opacity.client.ArchiveMetadata
import opacity.client.OpacityClient

class ArchiveHandle(
    val metadata: ArchiveMetadata,
    private val client: OpacityClient,
    private val coroutineScope: CoroutineScope,
) {
    val taskId = "archive[id=${metadata.id}]"

    @Throws(DownloadException::class)
    suspend fun getAsSource(): NamedSource {
        val currentState = DownloadDispatcher[taskId].value
        if (currentState is DownloadState.Completed) {
            return NamedSource(
                metadata.name,
                getPlatform().filesystem.source(currentState.destination)
            )
        }

        val url = with(client) {
            metadata.resourceUrl
        }

        val destination = getPlatform().createTemporaryFile("remote-archive", suffix = ".psarchive")

        coroutineScope.launch(DownloadContext(taskId) + Dispatchers.Download) {
            downloadSingle(
                file = DownloadableFile(metadata.name, url),
                destination = destination
            ).collect {
                if (it is DownloadState.Configure) {
                    it.build.trySend {
                        discretion = DownloadDiscretion.Immediate
                    }
                }
                DownloadDispatcher[taskId] = it
            }
        }.join()

        val lastState = DownloadDispatcher[taskId].value
        if (lastState !is DownloadState.Completed) {
            throw AssertionError("Download not completed. Instead, last state is ${lastState.let { it::class.simpleName }}.")
        }
        return NamedSource(metadata.name, getPlatform().filesystem.source(destination))
    }
}
