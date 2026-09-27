package com.ayush.aspect.feature.timeline

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
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
    private val onMediaLongPress: (MediaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = ArrayList<MediaGridItem>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = when (val item = items[position]) {
        is MediaGridItem.Header -> (Long.MIN_VALUE + position)
        is MediaGridItem.Media -> item.item.id
    }

    override fun getItemViewType(position: Int): Int = when (val item = items[position]) {
        is MediaGridItem.Header -> VIEW_TYPE_HEADER
        is MediaGridItem.Media -> if (item.item.isVideo) VIEW_TYPE_VIDEO else VIEW_TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(HeaderView(context))
            else -> MediaViewHolder(MediaCellView(context), imageLoader, onMediaLongPress)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MediaGridItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is MediaGridItem.Media -> (holder as MediaViewHolder).bind(item.item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitItems(newItems: List<MediaGridItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class HeaderViewHolder(private val view: HeaderView) : RecyclerView.ViewHolder(view) {
        fun bind(title: String) = view.bind(title)
    }

    class MediaViewHolder(
        private val view: MediaCellView,
        private val imageLoader: ImageLoader,
        private val onLongPress: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        fun bind(item: MediaItem) {
            view.bind(item, imageLoader, onLongPress)
        }

        override fun onViewRecycled() {
            view.clearImage()
            super.onViewRecycled()
        }
    }
}

private class HeaderView(context: Context) : TextView(context) {
    init {
        setTextColor(Color.WHITE)
        setTextSize(14f)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dp(16), dp(9), dp(16), dp(9))
        setBackgroundColor(Color.argb(238, 20, 20, 20))
        gravity = Gravity.CENTER_VERTICAL
    }

    fun bind(title: String) {
        text = title
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class MediaCellView(context: Context) : FrameLayout(context) {
    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(Color.rgb(28, 28, 28))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
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
        val badgeSize = dp(30)
        addView(
            videoBadge,
            LayoutParams(badgeSize, badgeSize, Gravity.BOTTOM or Gravity.END).apply {
                setMargins(0, 0, dp(6), dp(6))
            }
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val exact = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, exact)
    }

    fun bind(item: MediaItem, imageLoader: ImageLoader, onLongPress: (MediaItem) -> Unit) {
        contentDescription = item.displayName
        videoBadge.visibility = if (item.isVideo) View.VISIBLE else View.GONE
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
