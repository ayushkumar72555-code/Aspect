package com.ayush.aspect.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ayush.aspect.R
import com.ayush.aspect.feature.albums.AlbumsScreen
import com.ayush.aspect.feature.timeline.TimelineScreen

@Composable
fun AspectShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val items = listOf(
        "timeline" to ("Photos" to R.drawable.ic_photo_library),
        "albums" to ("Albums" to R.drawable.ic_collections)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEach { (route, item) ->
                    val (label, icon) = item
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo("timeline") { saveState = true }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "timeline",
            modifier = Modifier.padding(padding)
        ) {
            composable("timeline") { TimelineScreen() }
            composable("albums") { AlbumsScreen() }
        }
    }
}
