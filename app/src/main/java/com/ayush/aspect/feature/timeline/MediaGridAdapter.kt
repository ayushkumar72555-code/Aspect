package com.ayush.aspect.feature.timeline

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.target
import coil3.size.Size
import com.ayush.aspect.core.data.MediaItem

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_IMAGE = 1
private const val VIEW_TYPE_VIDEO = 2

internal sealed interface MediaGridItem {
    data class Header(val title: String) : MediaGridItem
    data class Media(val item: MediaItem) : MediaGridItem
}

internal class MediaGridAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val onMediaClick: (MediaItem) -> Unit,
    private val onMediaLongPress: (MediaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = ArrayList<MediaGridItem>()
    private var selectedIds: Set<Long> = emptySet()

    init { setHasStableIds(true) }

    override fun getItemId(position: Int): Long = when (val item = items[position]) {
        is MediaGridItem.Header -> Long.MIN_VALUE + position
        is MediaGridItem.Media -> item.item.id
    }

    override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
        is MediaGridItem.Header -> VIEW_TYPE_HEADER
        is MediaGridItem.Media -> if (item.item.isVideo) VIEW_TYPE_VIDEO else VIEW_TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(HeaderView(context))
            else -> MediaViewHolder(MediaCellView(context), imageLoader, onMediaClick, onMediaLongPress)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MediaGridItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is MediaGridItem.Media -> (holder as MediaViewHolder).bind(item.item, item.item.id in selectedIds)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is MediaViewHolder) holder.clearImage()
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = items.size

    fun submitItems(newItems: List<MediaGridItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setSelectedIds(ids: Set<Long>) {
        if (selectedIds == ids) return
        selectedIds = ids
        notifyDataSetChanged()
    }

    private class HeaderViewHolder(private val view: HeaderView) : RecyclerView.ViewHolder(view) {
        fun bind(title: String) = view.bind(title)
    }

    private class MediaViewHolder(
        private val view: MediaCellView,
        private val imageLoader: ImageLoader,
        private val onClick: (MediaItem) -> Unit,
        private val onLongPress: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        fun bind(item: MediaItem, selected: Boolean) = view.bind(item, selected, imageLoader, onClick, onLongPress)
        fun clearImage() = view.clearImage()
    }
}

private class HeaderView(context: Context) : TextView(context) {
    init {
        setTextColor(Color.WHITE)
        setTextSize(14f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(16), dp(9), dp(16), dp(9))
        setBackgroundColor(Color.argb(238, 20, 20, 20))
        gravity = Gravity.CENTER_VERTICAL
    }
    fun bind(title: String) { text = title }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class MediaCellView(context: Context) : FrameLayout(context) {
    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(Color.rgb(28, 28, 28))
    }
    private val selectionScrim = View(context).apply {
        setBackgroundColor(Color.argb(88, 255, 255, 255))
        visibility = View.GONE
    }
    private val check = TextView(context).apply {
        text = "✓"
        textSize = 17f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setBackgroundColor(Color.argb(225, 45, 115, 245))
        visibility = View.GONE
    }
    private val videoBadge = TextView(context).apply {
        text = "▶"
        textSize = 12f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.argb(185, 0, 0, 0))
        gravity = Gravity.CENTER
        visibility = View.GONE
    }
    private var request: coil3.request.Disposable? = null

    init {
        clipToPadding = false
        clipChildren = true
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(selectionScrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(check, LayoutParams(dp(34), dp(34), Gravity.TOP or Gravity.END).apply { setMargins(0, dp(8), dp(8), 0) })
        addView(videoBadge, LayoutParams(dp(30), dp(30), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(6), dp(6)) })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasure)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY))
    }

    fun bind(
        item: MediaItem,
        selected: Boolean,
        imageLoader: ImageLoader,
        onClick: (MediaItem) -> Unit,
        onLongPress: (MediaItem) -> Unit
    ) {
        contentDescription = item.displayName
        videoBadge.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        selectionScrim.visibility = if (selected) View.VISIBLE else View.GONE
        check.visibility = if (selected) View.VISIBLE else View.GONE
        request?.dispose()
        image.setImageDrawable(null)
        request = imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(item.uri)
                .size(Size(320, 320))
                .crossfade(false)
                .target(image)
                .build()
        )
        setOnClickListener { onClick(item) }
        setOnLongClickListener {
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            onLongPress(item)
            true
        }
    }

    fun clearImage() {
        request?.dispose()
        request = null
        image.setImageDrawable(null)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
