package com.zhufucdev.practiso.datamodel

const val EmbeddingsDimensionsLength = 1024L

abstract class Embeddings {
    abstract val id: Long
    abstract val value: FloatArray
}

interface EmbeddingsDatabase {
    suspend fun getNeighbors(id: Long, cap: Int): List<Embeddings>
    suspend fun getNeighbors(value: FloatArray, cap: Int): List<Embeddings>
    suspend fun put(embeddings: Embeddings)
    suspend fun put(list: List<Embeddings>)
}

interface VectorDatabaseDriver {
    suspend fun createFrameDb(): EmbeddingsDatabase
}