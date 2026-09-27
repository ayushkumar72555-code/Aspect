package com.ayush.aspect

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AspectApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
}
