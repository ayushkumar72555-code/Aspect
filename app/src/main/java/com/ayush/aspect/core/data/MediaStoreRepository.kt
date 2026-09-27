package com.ayush.aspect.core.data

import android.content.ContentResolver
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {
    suspend fun getMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Video.VideoColumns.DURATION
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val result = ArrayList<MediaItem>()
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        contentResolver.query(
            collection,
            projection,
            selection,
            args,
            "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC, ${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val type = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val mime = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val name = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateTaken = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val width = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val height = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val size = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val bucketId = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val duration = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

            while (cursor.moveToNext()) {
                val isVideo = cursor.getInt(type) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val mediaId = cursor.getLong(id)
                val uri = if (isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, mediaId)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, mediaId)
                }

                result += MediaItem(
                    id = mediaId,
                    uri = uri,
                    mimeType = cursor.getString(mime).orEmpty(),
                    displayName = cursor.getString(name).orEmpty(),
                    dateTakenMillis = cursor.getLong(dateTaken),
                    dateAddedSeconds = cursor.getLong(dateAdded),
                    width = cursor.getInt(width),
                    height = cursor.getInt(height),
                    sizeBytes = cursor.getLong(size),
                    bucketId = cursor.getString(bucketId),
                    bucketName = cursor.getString(bucketName),
                    isVideo = isVideo,
                    durationMillis = if (duration >= 0) cursor.getLong(duration) else 0L
                )
            }
        }
        result
    }
}
