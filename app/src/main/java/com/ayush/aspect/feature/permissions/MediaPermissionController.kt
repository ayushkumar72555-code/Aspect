package com.ayush.aspect.feature.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object MediaPermissionController {
    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun state(context: Context): MediaPermissionState {
        val image = Manifest.permission.READ_MEDIA_IMAGES
        val video = Manifest.permission.READ_MEDIA_VIDEO
        val selected = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED

        return when {
            Build.VERSION.SDK_INT >= 34 -> {
                val imagesGranted = ContextCompat.checkSelfPermission(context, image) == PackageManager.PERMISSION_GRANTED
                val videosGranted = ContextCompat.checkSelfPermission(context, video) == PackageManager.PERMISSION_GRANTED
                val selectedGranted = ContextCompat.checkSelfPermission(context, selected) == PackageManager.PERMISSION_GRANTED
                when {
                    imagesGranted && videosGranted -> MediaPermissionState.Full
                    selectedGranted || imagesGranted || videosGranted -> MediaPermissionState.Partial
                    else -> MediaPermissionState.Denied
                }
            }
            Build.VERSION.SDK_INT >= 33 -> {
                val imagesGranted = ContextCompat.checkSelfPermission(context, image) == PackageManager.PERMISSION_GRANTED
                val videosGranted = ContextCompat.checkSelfPermission(context, video) == PackageManager.PERMISSION_GRANTED
                when {
                    imagesGranted && videosGranted -> MediaPermissionState.Full
                    imagesGranted || videosGranted -> MediaPermissionState.Partial
                    else -> MediaPermissionState.Denied
                }
            }
            else -> if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                MediaPermissionState.Full
            } else {
                MediaPermissionState.Denied
            }
        }
    }
}
