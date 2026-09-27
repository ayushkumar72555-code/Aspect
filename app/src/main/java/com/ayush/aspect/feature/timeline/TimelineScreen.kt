package com.ayush.aspect.feature.timeline

import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.imageLoader
import com.ayush.aspect.core.data.MediaItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var columns by remember { mutableIntStateOf(3) }
    var zoomAccumulator by remember { mutableFloatStateOf(1f) }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.error != null -> ErrorState(state.error!!)
        state.items.isEmpty() -> EmptyState()

        else -> {
            val timelineItems = remember(state.items) { buildTimelineItems(state.items) }
            val adapter = remember(context) {
                MediaGridAdapter(
                    context = context,
                    imageLoader = context.imageLoader,
                    onMediaLongPress = { /* Selection mode is Phase 5. */ }
                )
            }

            LaunchedEffect(timelineItems) {
                adapter.submitItems(timelineItems)
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomAccumulator *= zoom
                            if (zoomAccumulator > 1.16f) {
                                columns = (columns - 1).coerceAtLeast(2)
                                zoomAccumulator = 1f
                            } else if (zoomAccumulator < 0.86f) {
                                columns = (columns + 1).coerceAtMost(5)
                                zoomAccumulator = 1f
                            }
                        }
                    },
                factory = { ctx ->
                    RecyclerView(ctx).apply {
                        setHasFixedSize(true)
                        itemAnimator = null
                        setItemViewCacheSize(12)
                        overScrollMode = RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS
                        recycledViewPool.setMaxRecycledViews(1, 24)
                        recycledViewPool.setMaxRecycledViews(2, 24)

                        val layoutManager = GridLayoutManager(ctx, columns).apply {
                            initialPrefetchItemCount = columns * 4
                            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                                override fun getSpanSize(position: Int): Int =
                                    if (adapter.getItemViewType(position) == 0) spanCount else 1
                            }
                        }
                        this.layoutManager = layoutManager
                        this.adapter = adapter
                    }
                },
                update = { recyclerView ->
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    if (layoutManager.spanCount != columns) {
                        layoutManager.spanCount = columns
                        layoutManager.initialPrefetchItemCount = columns * 4
                        recyclerView.invalidateItemDecorations()
                    }
                }
            )
        }
    }
}

private fun buildTimelineItems(items: List<MediaItem>): List<MediaGridItem> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    val result = ArrayList<MediaGridItem>(items.size + 64)
    var lastTitle: String? = null

    items.asSequence()
        .sortedByDescending { it.dateTakenMillis }
        .forEach { item ->
            val date = Calendar.getInstance().apply { timeInMillis = item.dateTakenMillis }
            val title = when {
                sameDay(date, today) -> "Today"
                sameDay(date, yesterday) -> "Yesterday"
                date.after(sevenDaysAgo) -> "Last 7 days"
                date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == today.get(Calendar.MONTH) ->
                    monthFormat.format(Date(item.dateTakenMillis))
                else -> yearFormat.format(Date(item.dateTakenMillis))
            }

            if (title != lastTitle) {
                result += MediaGridItem.Header(title)
                lastTitle = title
            }
            result += MediaGridItem.Media(item)
        }

    return result
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("Couldn't load your gallery\n$message")
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("No media yet\nPhotos and videos will appear here.")
    }
}
