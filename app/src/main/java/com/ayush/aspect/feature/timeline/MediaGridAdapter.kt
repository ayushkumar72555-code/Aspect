package com.ayush.aspect.feature.timeline

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import java.util.Locale

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
        setTextSize(18f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(16), dp(12), dp(16), dp(8))
        setBackgroundColor(Color.TRANSPARENT)
        gravity = Gravity.CENTER_VERTICAL
    }
    fun bind(title: String) { text = title }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class MediaCellView(context: Context) : FrameLayout(context) {
    private val cornerRadius = dp(10).toFloat()
    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = this@MediaCellView.cornerRadius
            setColor(Color.rgb(27, 25, 32))
        }
        clipToOutline = true
    }
    private val selectionScrim = View(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = this@MediaCellView.cornerRadius
            setColor(Color.argb(75, 205, 188, 255))
        }
        visibility = View.GONE
    }
    private val check = TextView(context).apply {
        text = "✓"
        textSize = 15f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.rgb(111, 82, 170))
        }
        visibility = View.GONE
    }
    private val videoBadge = TextView(context).apply {
        text = "▶"
        textSize = 10f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(Color.argb(190, 10, 9, 13))
        }
        visibility = View.GONE
    }
    private val durationBadge = TextView(context).apply {
        textSize = 10f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(7).toFloat()
            setColor(Color.argb(175, 10, 9, 13))
        }
        visibility = View.GONE
    }
    private var request: coil3.request.Disposable? = null

    init {
        clipChildren = true
        clipToOutline = true
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadius
            setColor(Color.rgb(27, 25, 32))
        }
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(selectionScrim, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(check, LayoutParams(dp(30), dp(30), Gravity.TOP or Gravity.END).apply { setMargins(0, dp(7), dp(7), 0) })
        addView(videoBadge, LayoutParams(dp(28), dp(28), Gravity.BOTTOM or Gravity.START).apply { setMargins(dp(7), 0, 0, dp(7)) })
        addView(durationBadge, LayoutParams(LayoutParams.WRAP_CONTENT, dp(25), Gravity.BOTTOM or Gravity.END).apply { setMargins(0, 0, dp(7), dp(7)) })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val squareSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, squareSpec)
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
        durationBadge.visibility = if (item.isVideo && item.durationMillis > 0) View.VISIBLE else View.GONE
        if (item.isVideo) durationBadge.text = formatDuration(item.durationMillis)
        selectionScrim.visibility = if (selected) View.VISIBLE else View.GONE
        check.visibility = if (selected) View.VISIBLE else View.GONE
        request?.dispose()
        image.setImageDrawable(null)
        request = imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(item.uri)
                .size(Size(360, 360))
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

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
