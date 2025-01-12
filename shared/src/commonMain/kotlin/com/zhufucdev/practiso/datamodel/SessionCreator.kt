package com.zhufucdev.practiso.datamodel

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.least_accessed_para
import resources.recently_created_para
import resources.recommended_for_you_para

sealed interface SessionCreator : PractisoOption {
    val selection: Selection
    val sessionName: String? get() = null

    data class ViaSelection(override val selection: Selection, val type: Type, val preview: String) :
        SessionCreator {
        companion object {
            private var id: Long = 0
        }

        enum class Type {
            RecentlyCreated, LeastAccessed, FailMuch
        }

        override val id: Long = Companion.id++

        @Composable
        override fun titleString(): String = stringResource(
            when (type) {
                Type.RecentlyCreated -> Res.string.recently_created_para
                Type.LeastAccessed -> Res.string.least_accessed_para
                Type.FailMuch -> Res.string.recommended_for_you_para
            }
        )

        @Composable
        override fun previewString(): String {
            return preview
        }

        override val sessionName: String?
            get() = preview
    }
}

