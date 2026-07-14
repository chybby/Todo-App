package com.chybby.todo.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshots.SnapshotStateList

fun LazyListState.isItemWithIndexVisible(index: Int): Boolean =
    layoutInfo.visibleItemsInfo.any { it.index == index }

// Sync a locally-mutated copy of a list (kept for immediate feedback while dragging to reorder)
// with the authoritative items, matching elements by key.
fun <T> SnapshotStateList<T>.syncWith(items: List<T>, key: (T) -> Any) {
    val currentKeys = map(key)
    val newKeys = items.map(key)

    if (currentKeys != newKeys) {
        clear()
        addAll(items)
    } else {
        items.forEachIndexed { index, item ->
            if (this[index] != item) {
                this[index] = item
            }
        }
    }
}
