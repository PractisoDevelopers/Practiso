package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.ArchivePack
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.service.ImportService
import com.zhufucdev.practiso.service.ImportState
import com.zhufucdev.practiso.service.ResourceNotFoundException
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException

class ImportServiceSync(db: AppDatabase) {
    private val service = ImportService(db)

    @Throws(AssertionError::class, ResourceNotFoundException::class, CancellationException::class)
    fun importSingleton(namedSource: NamedSource) =
        runBlocking { service.importSingleton(namedSource) }

    @Throws(Exception::class, RuntimeException::class)
    fun importAll(pack: ArchivePack) = runBlocking {
        service.import(pack).collect {
            when (it) {
                is ImportState.Confirmation -> it.ok.send(Unit)
                is ImportState.Error -> {
                    if (it.model.error != null) {
                        it.cancel.trySend(Unit)
                        throw it.model.error
                    } else {
                        error("Unspecified error while importing an archive.")
                    }
                }

                ImportState.Idle -> {}
                is ImportState.Importing -> {}
                is ImportState.Unarchiving -> {}
            }
        }
    }
}