package com.zhufucdev.practiso.helper

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.zhufucdev.practiso.database.Answer
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.Take
import com.zhufucdev.practiso.database.Template
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.parse
import kotlin.time.Instant

internal object DateTimeAdapter : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant {
        return try {
            Instant.parse(databaseValue)
        } catch (_: IllegalArgumentException) {
            Instant.parse(databaseValue, DateTimeComponents.Format {
                year()
                char('-')
                monthNumber()
                char('-')
                day()
                char(' ')
                hour()
                char(':')
                minute()
                char(':')
                second()
            })
        }
    }

    override fun encode(value: Instant): String {
        return value.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
    }
}

fun SqlDriver.toDatabase(): AppDatabase {
    AppDatabase.Schema.create(this)
    return AppDatabase(
        driver = this,
        quizAdapter = Quiz.Adapter(
            creationTimeISOAdapter = DateTimeAdapter,
            modificationTimeISOAdapter = DateTimeAdapter
        ),
        sessionAdapter = Session.Adapter(
            creationTimeISOAdapter = DateTimeAdapter,
            lastAccessTimeISOAdapter = DateTimeAdapter
        ),
        templateAdapter = Template.Adapter(
            creationTimeISOAdapter = DateTimeAdapter,
            modificationTimeISOAdapter = DateTimeAdapter
        ),
        takeAdapter = Take.Adapter(
            creationTimeISOAdapter = DateTimeAdapter,
            lastAccessTimeISOAdapter = DateTimeAdapter
        ),
        answerAdapter = Answer.Adapter(
            timeAdapter = DateTimeAdapter
        )
    )
}