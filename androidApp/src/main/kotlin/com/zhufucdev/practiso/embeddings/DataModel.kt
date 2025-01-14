package com.zhufucdev.practiso.embeddings

import com.zhufucdev.practiso.datamodel.Embeddings
import com.zhufucdev.practiso.datamodel.EmbeddingsDimensionsLength
import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

@Entity
data class DataModel(
    @Id override var id: Long,
    @HnswIndex(dimensions = EmbeddingsDimensionsLength) override var value: FloatArray,
) : Embeddings() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataModel

        if (id != other.id) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }

    companion object {
        fun from(embeddings: Embeddings) = DataModel(embeddings.id, embeddings.value)
    }
}
