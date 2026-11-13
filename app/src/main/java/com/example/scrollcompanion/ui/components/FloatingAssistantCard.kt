package com.example.scrollcompanion.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scrollcompanion.service.overlay.FloatingAssistantService

@Composable
fun FloatingAssistantCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isRunning by FloatingAssistantService.isRunning.collectAsStateWithLifecycle()
    var overlayGranted by remember { mutableStateOf(FloatingAssistantService.canDrawOverlays(context)) }
    var showOverlayRationale by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        overlayGranted = FloatingAssistantService.canDrawOverlays(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        FloatingAssistantService.start(context)
    }

    if (showOverlayRationale) {
        PermissionRationaleDialog(
            title = "Display over other apps",
            message = "Scroll Companion needs this so it can show the floating button you tap to " +
                "save something while you're in another app. You'll be taken to a system settings " +
                "screen next — turn the switch on there, then come back.",
            onContinue = {
                showOverlayRationale = false
                context.startActivity(FloatingAssistantService.overlayPermissionIntent(context))
            },
            onDismiss = { showOverlayRationale = false },
        )
    }
    if (showNotificationRationale) {
        PermissionRationaleDialog(
            title = "Notifications",
            message = "Scroll Companion uses a notification to let you know when your extracted " +
                "text is ready to view and save.",
            onContinue = {
                showNotificationRationale = false
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = { showNotificationRationale = false },
        )
    }

    val description = when {
        !overlayGranted -> "Allow Scroll Companion to appear over other apps. The bubble is the only thing shown over your apps."
        isRunning -> "The bubble is floating over your apps. Tap it to open the quick panel."
        else -> "Turn on a small bubble that floats over other apps, ready whenever you find something worth keeping."
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Floating assistant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when {
                !overlayGranted -> Button(
                    onClick = { showOverlayRationale = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Allow display over other apps")
                }

                isRunning -> OutlinedButton(
                    onClick = { FloatingAssistantService.stop(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Turn off")
                }

                else -> Button(
                    onClick = {
                        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        if (needsNotificationPermission) {
                            showNotificationRationale = true
                        } else {
                            FloatingAssistantService.start(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Turn on")
                }
            }
        }
    }
}
