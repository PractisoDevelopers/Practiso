package com.zhufucdev.practiso.helper

import com.zhufucdev.practiso.service.ImportState
import kotlinx.coroutines.flow.Flow

suspend fun Flow<ImportState>.simpleHandleQuestions() =
    collect {
        when (it) {
            is ImportState.Confirmation -> it.ok.send(Unit)
            is ImportState.Error -> {
                if (it.error.cause != null) {
                    it.cancel.trySend(Unit)
                    throw it.error.cause
                } else {
                    error("Unspecified error while importing an archive.")
                }
            }

            is ImportState.Idle -> {}
            is ImportState.Importing -> {}
            is ImportState.Unarchiving -> {}
        }
    }
