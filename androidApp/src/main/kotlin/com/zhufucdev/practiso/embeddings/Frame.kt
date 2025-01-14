package com.zhufucdev.practiso.embeddings

import com.zhufucdev.practiso.datamodel.Embeddings
import com.zhufucdev.practiso.datamodel.EmbeddingsDatabase
import io.objectbox.BoxStore

class Frame(private val store: BoxStore) : EmbeddingsDatabase {
    private val box = store.boxFor(DataModel::class.java)

    override suspend fun getNeighbors(
        id: Long,
        cap: Int,
    ): List<Embeddings> =
        store.callInReadTx {
            val target =
                box.query(DataModel_.id.equal(id))
                    .build()
                    .findUnique() ?: return@callInReadTx emptyList()
            box.query(DataModel_.value.nearestNeighbors(target.value, cap))
                .build()
                .find()
        }

    override suspend fun getNeighbors(
        value: FloatArray,
        cap: Int,
    ): List<Embeddings> =
        box.query(DataModel_.value.nearestNeighbors(value, cap))
            .build()
            .find()

    override suspend fun put(embeddings: Embeddings) {
        box.put(DataModel.from(embeddings))
    }

    override suspend fun put(list: List<Embeddings>) {
        box.put(list.map(DataModel::from))
    }
}
