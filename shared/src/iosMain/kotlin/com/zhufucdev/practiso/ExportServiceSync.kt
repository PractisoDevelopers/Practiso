package com.zhufucdev.practiso

import com.zhufucdev.practiso.bridge.Data
import com.zhufucdev.practiso.bridge.toPath
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.ExportService
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.GzipSink
import okio.IOException
import okio.buffer
import okio.gzip
import okio.use
import platform.Foundation.NSURL

class ExportServiceSync(db: AppDatabase) {
    private val service = ExportService(db)

    @Throws(IOException::class)
    fun exportAsData(quizIds: List<Long>) =
        runBlocking {
            val buffer = Buffer()
            GzipSink(buffer).use { sink ->
                service.exportAsSource(quizIds)
                    .buffer()
                    .readAll(sink)
            }
            Data(source = buffer)
        }

    fun exportOneAsData(quizId: Long) = exportAsData(listOf(quizId))

    fun exportAsFile(quizIds: List<Long>, destination: NSURL) = runBlocking {
        getPlatform()
            .filesystem
            .sink(destination.toPath())
            .gzip()
            .use { sink ->
                service.exportAsSource(quizIds)
                    .buffer()
                    .readAll(sink)
            }
    }

    fun exportOneAsFile(quizId: Long, destination: NSURL) = exportAsFile(listOf(quizId), destination)
}