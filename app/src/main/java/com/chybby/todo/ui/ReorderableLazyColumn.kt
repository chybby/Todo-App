package com.chybby.todo.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ReorderableLazyColumn(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    itemContent: @Composable() (LazyItemScope.(Int, T) -> Unit),
) {
    val lazyListState = rememberLazyListState()

    var draggedElement by remember { mutableStateOf<LazyListItemInfo?>(null) }

    var draggedDistance by remember { mutableStateOf(0.0f) }

    LazyColumn(
        state = lazyListState,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        lazyListState.layoutInfo.visibleItemsInfo
                            .firstOrNull { item -> offset.y.toInt() in item.offset..item.offset + item.size}
                            ?.also {
                                draggedElement = it
                            }
                    },
                    onDrag = { change, offset ->
                        change.consume()
                        draggedDistance += offset.y

                        draggedElement?.let { draggedItem ->
                            val startOffset = draggedItem.offset + draggedDistance
                            val endOffset = startOffset + draggedItem.size

                            // Find first other item that overlaps more than halfway.
                            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { overlappedItem ->
                                if (overlappedItem.index == draggedItem.index) {
                                    false
                                } else {
                                    val midpoint = overlappedItem.offset + overlappedItem.size / 2
                                    startOffset < midpoint && endOffset > midpoint
                                }
                            }?.also { overlappedItem ->
                                onMove(draggedItem.index, overlappedItem.index)
                                draggedDistance += draggedItem.offset - overlappedItem.offset
                                draggedElement = overlappedItem
                            }
                        }
                    },
                    onDragEnd = {
                        draggedElement = null
                        draggedDistance = 0f
                    },
                    onDragCancel = {
                        draggedElement = null
                        draggedDistance = 0f
                    },
                )
            }
    ) {
        itemsIndexed(items, key = key?.let { {_, item -> it(item)} } ) { index, item ->
            Box(
                modifier = Modifier
                    .then(if (index == draggedElement?.index) {
                        Modifier
                            .graphicsLayer(translationY = draggedDistance)
                            .shadow(elevation = 2.dp)
                    } else {
                        Modifier
                            .animateItemPlacement()
                    })
            ) {
                itemContent(this@itemsIndexed, index, item)
            }
        }
    }
}

@Preview(device = "id:pixel_6", showBackground = true, showSystemUi = true)
@Composable
fun ReorderableLazyColumnPreview() {

    val list = listOf(1, 2, 3, 4, 5).toMutableStateList()

    ReorderableLazyColumn(
        items = list,
        key = { it },
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        onMove = { from, to ->
            if (from != to ) {
                val element = list.removeAt(from)
                list.add(to, element)
            }
        }
    ) { _, item ->
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = item.toString())
        }
    }
}