package com.zhufucdev.practiso.datamodel

import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.database.DimensionQueries
import com.zhufucdev.practiso.database.Quiz
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

data class QuizIntensity(val quiz: Quiz, val intensity: Double)

fun DimensionQueries.getQuizIntensitiesById(dimId: Long) =
    getQuizIntensitiesByDimensionId(dimId) { id, name, creation, modification, intensity ->
        QuizIntensity(
            quiz = Quiz(id, name, creation, modification),
            intensity = intensity
        )
    }

data class DimensionQuizzes(val dimension: Dimension, val quizzes: List<Quiz>)

@Serializable
data class DimensionIntensity(
    val dimension: @Serializable(DimensionSerializer::class) Dimension,
    val intensity: Double,
)

suspend fun DimensionIntensity.associateWith(quizId: Long, db: AppDatabase) {
    val dimId = if (dimension.id < 0) {
        val existing = db.dimensionQueries.getDimensionByName(dimension.name).executeAsOneOrNull()
        existing?.id ?: db.transactionWithResult {
            db.dimensionQueries.insertDimension(dimension.name)
            db.quizQueries.lastInsertRowId().executeAsOne()
        }
    } else {
        dimension.id
    }
    db.dimensionQueries.associateQuizWithDimension(quizId, dimId, intensity)
}

suspend fun Collection<DimensionIntensity>.applyTo(quizId: Long, db: AppDatabase) {
    val diByName = associate { it.dimension.name to it }
    val existing = db.dimensionQueries.getDimensionByQuizId(quizId).executeAsList().map { it.name }
    val (removal, others) = diByName.keys.toSet().let {
        val r = existing - it
        r to it - r
    }

    coroutineScope {
        (others.map { name ->
            async { diByName[name]?.associateWith(quizId, db) }
        } + async {
            db.transaction {
                val ids = db.dimensionQueries.getDimensionsByName(removal)
                    .executeAsList()
                    .map(Dimension::id)
                db.dimensionQueries.dissoicateQuizFromDimensions(quizId, ids)
            }
        }).awaitAll()
    }
}

class DimensionSerializer : KSerializer<Dimension> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("dimension") {
        element("name", serialDescriptor<String>())
        element("id", serialDescriptor<Long>())
    }

    override fun serialize(
        encoder: Encoder,
        value: Dimension,
    ) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.name)
        encodeLongElement(descriptor, 1, value.id)
    }

    override fun deserialize(decoder: Decoder): Dimension = decoder.decodeStructure(descriptor) {
        var name = ""
        var id = -1L
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> name = decodeStringElement(descriptor, index)
                1 -> id = decodeLongElement(descriptor, index)
                DECODE_DONE -> break
            }
        }

        if (name.isEmpty() || id < 0) {
            error("Missing fields when decoding Dimension.")
        }

        Dimension(id, name)
    }
}