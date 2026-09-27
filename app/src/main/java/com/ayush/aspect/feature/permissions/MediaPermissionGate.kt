package com.ayush.aspect.feature.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import android.app.Activity

@Composable
fun MediaPermissionGate(content: @Composable (MediaPermissionState) -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(MediaPermissionController.state(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        state = MediaPermissionController.state(context)
    }

    LaunchedEffect(Unit) {
        state = MediaPermissionController.state(context)
    }

    if (state == MediaPermissionState.Full || state == MediaPermissionState.Partial) {
        content(state)
    } else {
        PermissionEmptyState(
            onGrant = {
                launcher.launch(MediaPermissionController.requiredPermissions())
            },
            onOpenSettings = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                })
            },
            permanentlyDenied = (context as? Activity)?.let { activity ->
                MediaPermissionController.requiredPermissions().all { permission ->
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                }
            } == true
        )
    }
}

@Composable
private fun PermissionEmptyState(
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    permanentlyDenied: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your memories belong here", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Aspect needs access to your photos and videos to build your gallery. You can grant full or selected access.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )
        Button(onClick = if (permanentlyDenied) onOpenSettings else onGrant) {
            Text(if (permanentlyDenied) "Open Settings" else "Grant Access")
        }
    }
}
