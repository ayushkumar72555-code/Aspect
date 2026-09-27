package com.ayush.aspect.core.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val mimeType: String,
    val displayName: String,
    val dateTakenMillis: Long,
    val dateAddedSeconds: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val bucketId: String?,
    val bucketName: String?,
    val isVideo: Boolean,
    val durationMillis: Long = 0L
)
