package com.ayush.aspect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ayush.aspect.core.designsystem.AspectTheme
import com.ayush.aspect.core.navigation.AspectShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AspectTheme {
                AspectShell()
            }
        }
    }
}
