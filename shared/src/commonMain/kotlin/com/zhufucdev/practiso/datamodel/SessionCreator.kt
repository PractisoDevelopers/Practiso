package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.database.Dimension

sealed interface SessionCreator : PractisoOption {
    val selection: Selection

    data class RecentlyCreatedQuizzes(
        override val selection: Selection,
        val leadingQuizName: String?,
    ) : SessionCreator {
        override val id = RandomAccess.id++
    }

    data class RecentlyCreatedDimension(
        override val selection: Selection,
        val quizCount: Int,
        val dimensionName: String,
    ) : SessionCreator {
        override val id: Long = RandomAccess.id++
    }

    data class LeastAccessed(
        override val selection: Selection,
        val leadingItemName: String?,
        val itemCount: Int,
    ) : SessionCreator {
        override val id: Long = RandomAccess.id++
    }

    data class FailMuch(
        override val selection: Selection,
        val leadingItemName: String?,
        val itemCount: Int,
    ) : SessionCreator {
        override val id: Long = RandomAccess.id++
    }

    data class FailMuchDimension(
        val dimension: Dimension,
        override val selection: Selection,
        val itemCount: Int,
    ) : SessionCreator {
        override val id: Long = RandomAccess.id++
    }

    object RandomAccess {
        var id: Long = 0
    }
}

