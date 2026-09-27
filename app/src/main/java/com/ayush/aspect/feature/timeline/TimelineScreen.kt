package com.ayush.aspect.feature.timeline

import android.content.ContentResolver
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.stickyHeader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.ayush.aspect.core.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var columns by remember { mutableIntStateOf(3) }
    var zoomAccumulator by remember { mutableFloatStateOf(1f) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            state.error != null -> ErrorState(state.error!!)
            state.items.isEmpty() -> EmptyState()
            else -> {
                val grouped = remember(state.items) { groupByDate(state.items) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                zoomAccumulator *= zoom
                                if (zoomAccumulator > 1.18f) {
                                    columns = (columns - 1).coerceAtLeast(2)
                                    zoomAccumulator = 1f
                                } else if (zoomAccumulator < 0.84f) {
                                    columns = (columns + 1).coerceAtMost(5)
                                    zoomAccumulator = 1f
                                }
                            }
                        },
                    contentPadding = PaddingValues(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    grouped.forEach { group ->
                        stickyHeader(key = "header-${group.title}", contentType = "date") {
                            Surface(
                                modifier = Modifier.fillMaxWidth().animateContentSize(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            ) {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                        items(
                            items = group.items,
                            key = { it.id },
                            span = { GridItemSpan(1) }
                        ) { item ->
                            MediaThumbnail(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(item: MediaItem) {
    val context = LocalContext.current
    val bitmap by rememberThumbnail(context.contentResolver, item)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }

        if (item.isVideo) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).graphicsLayer { alpha = 0.92f },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    modifier = Modifier.padding(4.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun rememberThumbnail(contentResolver: ContentResolver, item: MediaItem) = produceState<Bitmap?>(
    initialValue = null,
    key1 = item.uri
) {
    value = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(item.uri, android.util.Size(720, 720), null)
            } else {
                null
            }
        }.getOrNull()
    }
}

private data class DateGroup(val title: String, val items: List<MediaItem>)

private fun groupByDate(items: List<MediaItem>): List<DateGroup> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    return items
        .sortedByDescending { it.dateTakenMillis }
        .groupBy { item ->
            val date = Calendar.getInstance().apply { timeInMillis = item.dateTakenMillis }
            when {
                sameDay(date, today) -> "Today"
                sameDay(date, yesterday) -> "Yesterday"
                date.after(sevenDaysAgo) -> "Last 7 days"
                date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    date.get(Calendar.MONTH) == today.get(Calendar.MONTH) -> monthFormat.format(Date(item.dateTakenMillis))
                else -> yearFormat.format(Date(item.dateTakenMillis))
            }
        }
        .map { (title, media) -> DateGroup(title, media) }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

@Composable
private fun ErrorState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("Couldn't load your gallery\n$message", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("No media yet\nPhotos and videos will appear here.", style = MaterialTheme.typography.bodyLarge)
    }
}
