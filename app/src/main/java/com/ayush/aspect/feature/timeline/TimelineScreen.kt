package com.ayush.aspect.feature.timeline

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        if (it.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.clearSelection()
            viewModel.refresh()
        }
    }

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
                    onMediaClick = { item ->
                        if (state.isSelectionMode) viewModel.toggleSelection(item.id)
                    },
                    onMediaLongPress = { item -> viewModel.toggleSelection(item.id) }
                )
            }

            LaunchedEffect(timelineItems) { adapter.submitItems(timelineItems) }
            LaunchedEffect(state.selectedIds) { adapter.setSelectedIds(state.selectedIds) }

            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
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

                            val scaleDetector = ScaleGestureDetector(
                                ctx,
                                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                                        zoomAccumulator *= detector.scaleFactor
                                        if (zoomAccumulator > 1.16f) {
                                            columns = (columns - 1).coerceAtLeast(2)
                                            zoomAccumulator = 1f
                                        } else if (zoomAccumulator < 0.86f) {
                                            columns = (columns + 1).coerceAtMost(5)
                                            zoomAccumulator = 1f
                                        }
                                        return true
                                    }
                                }
                            )
                            setOnTouchListener { _, event ->
                                scaleDetector.onTouchEvent(event)
                                false
                            }
                        }
                    },
                    update = { recyclerView ->
                        val layoutManager = recyclerView.layoutManager as GridLayoutManager
                        if (layoutManager.spanCount != columns) {
                            layoutManager.spanCount = columns
                            layoutManager.initialPrefetchItemCount = columns * 4
                        }
                    }
                )

                if (state.isSelectionMode) {
                    SelectionToolbar(
                        count = state.selectedCount,
                        onClose = viewModel::clearSelection,
                        onSelectAll = viewModel::selectAll,
                        onShare = { shareItems(context, viewModel.selectedItems()) },
                        onFavorite = {
                            viewModel.favoriteSelected(true)
                            viewModel.clearSelection()
                        },
                        onDelete = {
                            requestDelete(context, viewModel.selectedItems(), deleteLauncher)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionToolbar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Clear selection")
            }
            Text("$count selected", style = MaterialTheme.typography.titleMedium)
        }
        Row {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = onFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "Favorite")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun shareItems(context: android.content.Context, items: List<MediaItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList<Uri>(items.size)
    items.forEach { uris += it.uri }
    val mime = when {
        items.all { it.mimeType.startsWith("image/") } -> "image/*"
        items.all { it.mimeType.startsWith("video/") } -> "video/*"
        else -> "*/*"
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    ContextCompat.startActivity(context, Intent.createChooser(intent, "Share media"), null)
}

private fun requestDelete(
    context: android.content.Context,
    items: List<MediaItem>,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
) {
    if (items.isEmpty()) return
    val uris = items.map { it.uri }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pending: PendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
        launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
    } else {
        uris.forEach { context.contentResolver.delete(it, null, null) }
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

    items.asSequence().sortedByDescending { it.dateTakenMillis }.forEach { item ->
        val date = Calendar.getInstance().apply { timeInMillis = item.dateTakenMillis }
        val title = when {
            sameDay(date, today) -> "Today"
            sameDay(date, yesterday) -> "Yesterday"
            date.after(sevenDaysAgo) -> "Last 7 days"
            date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                date.get(Calendar.MONTH) == today.get(Calendar.MONTH) -> monthFormat.format(Date(item.dateTakenMillis))
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
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

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
