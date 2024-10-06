package com.zhufucdev.practiso

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.database.Session
import kotlinx.datetime.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents

private object DateTimeAdapter : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant {
        return Instant.parse(databaseValue)
    }

    override fun encode(value: Instant): String {
        return value.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
    }

}

fun SqlDriver.toDatabase() = AppDatabase(
    driver = this,
    quizAdapter = Quiz.Adapter(
        creationTimeISOAdapter = DateTimeAdapter,
        modificationTimeISOAdapter = DateTimeAdapter
    ),
    sessionAdapter = Session.Adapter(
        creationTimeISOAdapter = DateTimeAdapter,
        lastAccessTimeISOAdapter = DateTimeAdapter
    ),
)