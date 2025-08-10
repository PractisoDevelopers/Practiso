package com.zhufucdev.practiso.platform

import com.zhufucdev.practiso.datamodel.AppMessage
import com.zhufucdev.practiso.datamodel.AppScope
import com.zhufucdev.practiso.datamodel.DownloadException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.read
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.runningFold
import okio.FileSystem
import okio.Path
import okio.buffer

@Throws(DownloadException::class)
actual fun downloadRecursively(
    walker: DirectoryWalker,
    destination: Path,
): Flow<DownloadState> = flow {
    val client = HttpClient(OkHttp)
    val files = walker.files.runningFold(mutableListOf<DownloadableFile>()) { acc, file ->
        acc.add(file)
        emit(DownloadState.Preparing(acc))
        acc
    }.lastOrNull() ?: emptyList()

    coroutineScope {
        files.map { file ->
            async {
                client.downloadFileTo(file, destination)
                    .collect(this@flow)
            }
        }
    }.awaitAll()
}

@Throws(DownloadException::class)
actual fun downloadSingle(file: DownloadableFile, destination: Path): Flow<DownloadState> = flow {
    val client = HttpClient(OkHttp)
    emit(DownloadState.Preparing(listOf(file)))
    emitAll(client.downloadFileTo(file, destination))
}

private fun HttpClient.downloadFileTo(
    file: DownloadableFile,
    path: Path,
): Flow<DownloadState> = flow {
    val fs = FileSystem.SYSTEM

    val response = get(file.url)
    if (!response.status.isSuccess()) {
        throw DownloadException(
            scope = AppScope.DownloadExecutor,
            appMessage = AppMessage.HttpStatusFailure(response.status.value),
            kotlinMessage = "HTTP request responded with ${response.status}"
        )
    }

    val length = response.headers[HttpHeaders.ContentLength]?.toFloat()
        ?: Float.POSITIVE_INFINITY
    val body = response.bodyAsChannel()

    val sink = fs.sink(path).buffer()
    var downloaded = 0L
    while (!body.isClosedForRead) {
        body.read { ba, start, end ->
            val block = end - start
            downloaded += block
            sink.write(ba.sliceArray(start until end))
            emit(DownloadState.Downloading(file, downloaded.toFloat() / length))
            block
        }
    }
    sink.close()

    emit(DownloadState.Completed(file, path))
}