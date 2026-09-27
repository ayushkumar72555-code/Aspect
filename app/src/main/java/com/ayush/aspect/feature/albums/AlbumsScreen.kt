package com.ayush.aspect.feature.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AlbumUi(val name: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: Color)

@Composable
fun AlbumsScreen() {
    val albums = listOf(
        AlbumUi("Camera", "Photos & videos", Icons.Default.Image, Color(0xFFD0BCFF)),
        AlbumUi("Screenshots", "Screen captures", Icons.Default.ScreenshotMonitor, Color(0xFFB9D9FF)),
        AlbumUi("WhatsApp", "Shared media", Icons.Default.Collections, Color(0xFFBCECCB)),
        AlbumUi("Downloads", "Downloaded media", Icons.Default.Download, Color(0xFFFFD7A3)),
        AlbumUi("Videos", "All videos", Icons.Default.Videocam, Color(0xFFFFB9C7)),
        AlbumUi("Favorites", "Starred memories", Icons.Default.Favorite, Color(0xFFFFC4D7)),
        AlbumUi("Other folders", "More on device", Icons.Default.Folder, Color(0xFFD1C5FF))
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF09090D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF171426), Color(0xFF09090D))))
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
        ) {
            Text("Albums", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Your folders, organized beautifully", color = Color(0xFFA8A2B4), style = MaterialTheme.typography.bodyMedium)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album -> AlbumCard(album) }
        }
    }
}

@Composable
private fun AlbumCard(album: AlbumUi) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(168.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF17151D)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(album.tint.copy(alpha = 0.25f), Color(0xFF15131B))
                    )
                )
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = album.tint.copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(album.icon, contentDescription = null, tint = album.tint, modifier = Modifier.size(25.dp))
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(album.name, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(album.subtitle, color = Color(0xFFA8A2B4), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
