package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.ArchivePack
import com.zhufucdev.practiso.datamodel.ImportException
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.datamodel.ResourceNotFoundException
import com.zhufucdev.practiso.helper.simpleHandleQuestions
import com.zhufucdev.practiso.service.ImportService
import com.zhufucdev.practiso.service.ImportState
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException

class ImportServiceSync(db: AppDatabase) {
    private val service = ImportService(db)

    @Throws(AssertionError::class, ResourceNotFoundException::class, CancellationException::class)
    fun importSingleton(namedSource: NamedSource) =
        runBlocking { service.importSingleton(namedSource) }

    @Throws(Exception::class, RuntimeException::class)
    fun importAll(pack: ArchivePack) = runBlocking {
        service.import(pack).simpleHandleQuestions()
    }

    @Throws(ImportException::class)
    fun import(namedSource: NamedSource) = runBlocking {
        service.import(namedSource).simpleHandleQuestions()
    }
}