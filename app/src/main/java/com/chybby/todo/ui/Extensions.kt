package com.chybby.todo.ui

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.isItemWithIndexVisible(index: Int): Boolean {
    val lastVisibleItemIndex = firstVisibleItemIndex + layoutInfo.visibleItemsInfo.size - 1
    return index in firstVisibleItemIndex + 1 until lastVisibleItemIndex
}