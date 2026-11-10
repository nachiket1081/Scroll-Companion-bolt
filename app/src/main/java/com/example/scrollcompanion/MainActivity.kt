package com.example.scrollcompanion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.scrollcompanion.ui.navigation.AppNavGraph
import com.example.scrollcompanion.ui.theme.ScrollCompanionTheme

class MainActivity : ComponentActivity() {

    private var requestedRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedRoute = intent.getStringExtra(EXTRA_START_ROUTE)
        setContent {
            ScrollCompanionTheme {
                AppNavGraph(
                    requestedRoute = requestedRoute,
                    onRouteHandled = { requestedRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedRoute = intent.getStringExtra(EXTRA_START_ROUTE)
    }

    companion object {
        const val EXTRA_START_ROUTE = "start_route"
    }
}
