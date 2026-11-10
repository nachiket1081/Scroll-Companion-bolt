package com.example.scrollcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.scrollcompanion.ui.navigation.AppNavGraph
import com.example.scrollcompanion.ui.theme.ScrollCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScrollCompanionTheme {
                AppNavGraph()
            }
        }
    }
}
