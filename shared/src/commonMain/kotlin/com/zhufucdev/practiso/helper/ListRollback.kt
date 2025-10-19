package com.zhufucdev.practiso.helper

interface ListOperation {
    fun cancel()
}

private data class ListAddItem<T>(val item: T, val list: MutableList<T>, val pos: Int) :
    ListOperation {
    override fun cancel() {
        if (list[pos] == item) {
            list.removeAt(pos)
            return
        }
        list.remove(item)
    }
}

private data class ListRemoveItem<T>(
    val item: T,
    val predecessors: List<T>,
    val list: MutableList<T>
) : ListOperation {
    override fun cancel() {
        var insertPos = 0
        for (i in predecessors.lastIndex downTo 0) {
            val predecessor = predecessors[i]
            val firstIndex = list.indexOf(predecessor)
            if (firstIndex >= 0) {
                insertPos = firstIndex
                break
            }
        }
        list.add(insertPos, item)
    }
}

fun <T> MutableList<T>.addWithRollbackability(item: T): ListOperation {
    add(item)
    val pos = lastIndex
    return ListAddItem(item, this, pos)
}

fun <T> MutableList<T>.removeWithRollbackablity(at: Int): ListOperation? {
    if (at < 0) {
        return null
    }
    val item = get(at)
    removeAt(at)
    return ListRemoveItem(item, slice(0 until at), this)
}

fun <T> MutableList<T>.removeWithRollbackablity(item: T): ListOperation? =
    removeWithRollbackablity(at = indexOf(item))