package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.database.AppDatabase
import kotlinx.serialization.Serializable

@Serializable
sealed interface Edit {
    fun applyTo(db: AppDatabase, quizId: Long)

    @Serializable
    data class Append(val frame: Frame, val insertIndex: Int) : Edit {
        override fun applyTo(db: AppDatabase, quizId: Long) {
            frame.insertInto(db, quizId, insertIndex.toLong())
        }
    }

    @Serializable
    data class Remove(val frame: Frame, val oldIndex: Int) : Edit {
        override fun applyTo(db: AppDatabase, quizId: Long) {
            when (frame) {
                is Frame.Image ->
                    db.transaction {
                        db.quizQueries.removeImageFrame(frame.id)
                    }

                is Frame.Options -> {
                    frame.frames.map(KeyedPrioritizedFrame::frame)
                        .forEachIndexed { index, frame ->
                            (Remove(frame, index) as Edit).applyTo(db, index.toLong())
                        }
                    db.transaction {
                        db.quizQueries.removeOptionsFrame(frame.id)
                    }
                }

                is Frame.Text ->
                    db.transaction {
                        db.quizQueries.removeTextFrame(frame.id)
                    }
            }
        }
    }

    @Serializable
    data class Update(val old: Frame, val new: Frame) : Edit {
        override fun applyTo(db: AppDatabase, quizId: Long) {
            when (new) {
                is Frame.Image -> db.transaction {
                    val oldFrame = (old as Frame.Image).imageFrame
                    if (oldFrame.altText != new.imageFrame.altText) {
                        db.quizQueries.updateImageFrameAltText(
                            new.imageFrame.altText,
                            new.imageFrame.id
                        )
                    } else {
                        val f = new.imageFrame
                        db.quizQueries.updateImageFrameContent(f.filename, f.width, f.height, f.id)
                    }
                }

                is Frame.Options -> {
                    val oldFrameIds = (old as Frame.Options).frames.map { it.frame.id }.toSet()
                    val newFrameIds = new.frames.map { it.frame.id }.toSet()
                    val removals = (oldFrameIds - newFrameIds)
                        .map { removedId -> old.frames.first { it.frame.id == removedId } }
                    removals
                        .forEachIndexed { index, removal ->
                            Remove(removal.frame, index).applyTo(db, quizId)
                        }
                    val additions =
                        (newFrameIds - oldFrameIds)
                            .map { newId ->
                                new.frames.mapIndexedNotNull { index, frame -> index.takeIf { frame.frame.id == newId } }
                            }
                            .flatten()
                    additions
                        .forEach { index ->
                            val keyedPrioritizedFrame = new.frames[index]
                            when (val frame = keyedPrioritizedFrame.frame) {
                                is Frame.Image -> db.transaction {
                                    frame.insertTo(db)
                                    db.quizQueries.assoicateLastImageFrameWithOption(
                                        new.id,
                                        index.toLong(),
                                        keyedPrioritizedFrame.isKey
                                    )
                                }

                                is Frame.Options -> throw UnsupportedOperationException("Options frame inception")
                                is Frame.Text -> db.transaction {
                                    db.quizQueries.insertTextFrame(frame.textFrame.content)
                                    db.quizQueries.assoicateLastTextFrameWithOption(
                                        new.id,
                                        keyedPrioritizedFrame.isKey,
                                        index.toLong()
                                    )
                                }
                            }
                        }
                    val updates = new.frames.mapNotNull { n ->
                        val id = n.frame.id
                        old.frames.firstOrNull { it.frame.id == id && it != n }
                            ?.let { it to n }
                    }
                    updates
                        .forEach { (old, new) ->
                            if (old.frame != new.frame) {
                                Update(old.frame, new.frame).applyTo(db, quizId)
                            }
                            if (old.isKey != new.isKey) {
                                when (new.frame) {
                                    is Frame.Image -> db.transaction {
                                        db.quizQueries.updateIsKeyImageFrameAssocicated(
                                            imageFrameId = new.frame.id,
                                            isKey = new.isKey
                                        )
                                    }

                                    is Frame.Text ->
                                        db.transaction {
                                            db.quizQueries.updateIsKeyTextFrameAssoicated(
                                                textFrameId = new.frame.id,
                                                isKey = new.isKey
                                            )
                                        }

                                    is Frame.Options -> throw UnsupportedOperationException(
                                        "Option frame inception"
                                    )
                                }
                            }
                        }
                }

                is Frame.Text -> db.transaction {
                    db.quizQueries.updateTextFrameContent(new.textFrame.content, new.textFrame.id)
                }
            }
        }
    }

    @Serializable
    data class Rename(val old: String, val new: String) : Edit {
        override fun applyTo(db: AppDatabase, quizId: Long) {
            db.transaction {
                db.quizQueries.updateQuizName(new.takeIf(String::isNotEmpty), quizId)
            }
        }
    }
}

fun List<Edit>.optimized(): List<Edit> {
    val appends = mutableListOf<Edit.Append>()
    val updates = mutableListOf<Edit.Update>()
    val removals = mutableListOf<Edit.Remove>()
    var rename: Pair<String, String>? = null
    forEach {
        when (it) {
            is Edit.Append -> appends.add(it)
            is Edit.Remove -> {
                appends.indexOfFirst { a -> a.frame.id == it.frame.id }
                    .takeIf { i -> i >= 0 }
                    ?.let(appends::removeAt)
                    ?: updates.indexOfFirst { u -> u.new.id == it.frame.id }
                        .takeIf { i -> i >= 0 }
                        ?.let(updates::removeAt)
                    ?: removals.add(it)
            }

            is Edit.Rename -> {
                rename = rename?.copy(second = it.new) ?: (it.old to it.new)
            }

            is Edit.Update -> {
                appends.indexOfFirst { a -> a.frame.id == it.new.id }
                    .takeIf { i -> i >= 0 }
                    ?.let { i -> appends[i] = appends[i].copy(frame = it.new) }
                    ?: updates.indexOfFirst { u -> u.new.id == it.new.id }
                        .takeIf { i -> i >= 0 }
                        ?.let { i -> updates[i] = updates[i].copy(new = it.new) }
                    ?: updates.add(it)
            }
        }
    }
    return removals + appends + updates + (rename?.let { r ->
        listOf(
            Edit.Rename(
                r.first,
                r.second
            )
        )
    } ?: emptyList())
}

fun List<Edit>.applyTo(db: AppDatabase, quizId: Long) {
    forEach {
        it.applyTo(db, quizId)
    }
}