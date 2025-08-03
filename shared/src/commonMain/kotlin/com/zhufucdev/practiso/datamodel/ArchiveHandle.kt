package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.Download
import com.zhufucdev.practiso.DownloadContext
import com.zhufucdev.practiso.platform.DownloadDiscretion
import com.zhufucdev.practiso.platform.DownloadState
import com.zhufucdev.practiso.platform.DownloadableFile
import com.zhufucdev.practiso.platform.downloadSingle
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import opacity.client.ArchiveMetadata
import opacity.client.OpacityClient

class ArchiveHandle(
    val metadata: ArchiveMetadata,
    private val client: OpacityClient,
    private val coroutineScope: CoroutineScope,
) {
    val taskId = "archive[id=${metadata.id}]"

    suspend fun download(): NamedSource {
        val url = with(client) {
            metadata.resourceUrl
        }

        val destination = getPlatform().createTemporaryFile("remote-archive", suffix = ".psarchive")

        with(coroutineScope) {
            launch {
                withContext(Dispatchers.Download + DownloadContext(taskId)) {
                    downloadSingle(
                        file = DownloadableFile(metadata.name, url),
                        destination = destination
                    )
                }.collect {
                    Dispatchers.Download[taskId] = it
                }
            }

            launch {
                Dispatchers.Download[taskId].collect {
                    if (it is DownloadState.Configure) {
                        it.build.trySend {
                            discretion = DownloadDiscretion.Immediate
                        }
                    }
                }
            }
        }


        val lastState = Dispatchers.Download[taskId].last()
        if (lastState !is DownloadState.Completed) {
            throw IllegalStateException("Download not completed. Instead, last state is ${lastState?.let { it::class.simpleName }}.")
        }
        return NamedSource(metadata.name, getPlatform().filesystem.source(destination))
    }
}
