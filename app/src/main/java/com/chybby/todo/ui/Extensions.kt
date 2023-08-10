package com.chybby.todo.ui

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.isItemWithIndexVisible(index: Int): Boolean {
    return layoutInfo.visibleItemsInfo.any{ it.index == index }
}