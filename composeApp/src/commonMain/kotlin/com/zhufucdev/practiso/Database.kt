package com.zhufucdev.practiso

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.platform.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

object Database {
    val app: AppDatabase by lazy {
        getPlatform().createDbDriver().toDatabase()
    }
}

suspend fun List<Frame>.insertTo(db: AppDatabase, name: String?) =
    withContext(Dispatchers.IO) {
        val quizId = db.transactionWithResult {
            db.quizQueries.insertQuiz(name, Clock.System.now(), null)
            db.quizQueries.lastInsertRowId().executeAsOne()
        }

        mapIndexed { index, frame ->
            when (frame) {
                is Frame.Text -> async {
                    db.transaction {
                        db.quizQueries.insertTextFrame(frame.textFrame.content)
                        db.quizQueries.associateLastTextFrameWithQuiz(quizId, index.toLong())
                    }
                }

                is Frame.Image -> async {
                    db.transaction {
                        frame.insertTo(db)
                        db.quizQueries.associateLastImageFrameWithQuiz(quizId, index.toLong())
                    }
                }

                is Frame.Options -> async {
                    val frameId = db.transactionWithResult {
                        db.quizQueries.insertOptionsFrame(frame.optionsFrame.name)
                        val frameId = db.quizQueries.lastInsertRowId().executeAsOne()
                        db.quizQueries.associateOptionsFrameWithQuiz(
                            quizId,
                            frameId,
                            index.toLong()
                        )
                        frameId
                    }

                    frame.frames.map { optionFrame ->
                        when (optionFrame.frame) {
                            is Frame.Image -> async {
                                db.transaction {
                                    optionFrame.frame.insertTo(db)
                                    db.quizQueries.assoicateLastImageFrameWithOption(
                                        frameId,
                                        maxOf(optionFrame.priority, 0).toLong(),
                                        optionFrame.isKey
                                    )
                                }
                            }

                            is Frame.Text -> async {
                                db.transaction {
                                    db.quizQueries.insertTextFrame(optionFrame.frame.textFrame.content)
                                    db.quizQueries.assoicateLastTextFrameWithOption(
                                        frameId,
                                        optionFrame.isKey,
                                        maxOf(optionFrame.priority, 0).toLong()
                                    )
                                }
                            }

                            is Frame.Options -> throw UnsupportedOperationException("Options frame inception")
                        }
                    }.awaitAll()
                }
            }
        }.awaitAll()
    }

private fun Frame.Image.insertTo(db: AppDatabase) {
    db.quizQueries.insertImageFrame(
        imageFrame.filename,
        imageFrame.altText,
        imageFrame.width,
        imageFrame.height
    )
}
