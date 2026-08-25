package com.smingoli.tracklogcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smingoli.tracklogcompanion.ui.TrackLogApp
import com.smingoli.tracklogcompanion.ui.theme.TrackLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackLogTheme {
                TrackLogApp()
            }
        }
    }
}

