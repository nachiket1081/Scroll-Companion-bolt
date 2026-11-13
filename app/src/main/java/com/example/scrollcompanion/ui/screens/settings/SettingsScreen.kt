package com.example.scrollcompanion.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import com.example.scrollcompanion.data.local.entities.CategoryEntity
import com.example.scrollcompanion.ui.components.PermissionRationaleDialog
import com.example.scrollcompanion.ui.components.CategoryNameDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import com.example.scrollcompanion.reminders.ReminderPrefs
import com.example.scrollcompanion.reminders.ReminderScheduler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.scrollcompanion.service.capture.ForegroundAppDetector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri

private const val PRIVACY_POLICY_URL = "https://nachiket1081.github.io/Scroll-Companion-Privacy-Policy/"
@Composable
fun SettingsScreen(
    categories: List<CategoryEntity>,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (id: Long, name: String) -> Unit,
    onDeleteCategory: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var usageAccess by remember { mutableStateOf(ForegroundAppDetector.hasUsageAccess(context)) }
    var showUsageAccessRationale by remember { mutableStateOf(false) }
    var showReminderNotificationRationale by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        usageAccess = ForegroundAppDetector.hasUsageAccess(context)
    }
    var addingCategory by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }
    var remindersOn by remember { mutableStateOf(ReminderPrefs.isEnabled(context)) }
    var reminderHour by remember { mutableIntStateOf(ReminderPrefs.hour(context)) }
    fun enableReminders() {
        ReminderPrefs.setEnabled(context, true)
        remindersOn = true
        ReminderScheduler.schedule(context, replace = true)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) enableReminders() }

    if (showUsageAccessRationale) {
        PermissionRationaleDialog(
            title = "Usage access",
            message = "This lets saved items show which app they came from, like \"Instagram\" or " +
                "\"Chrome\". It only reads which app is in the foreground, never what that app " +
                "shows on screen, and everything stays on this device. This is optional.",
            onContinue = {
                showUsageAccessRationale = false
                context.startActivity(ForegroundAppDetector.usageAccessIntent())
            },
            onDismiss = { showUsageAccessRationale = false },
        )
    }
    if (showReminderNotificationRationale) {
        PermissionRationaleDialog(
            title = "Notifications",
            message = "Daily reminders use a notification to show a few saved items worth " +
                "another look. Nothing is sent anywhere else.",
            onContinue = {
                showReminderNotificationRationale = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = { showReminderNotificationRationale = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Name the app you saved from",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (usageAccess) {
                        "On. Saved items are labelled with the app you were in."
                    } else {
                        "Allow usage access so saved items can show which app they came from. " +
                            "This only reads which app was open; it stays on this device."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!usageAccess) {
                    Button(onClick = { showUsageAccessRationale = true }) {
                        Text("Open usage access settings")
                    }
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Daily reminder",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = remindersOn,
                        onCheckedChange = { on ->
                            if (!on) {
                                ReminderPrefs.setEnabled(context, false)
                                remindersOn = false
                                ReminderScheduler.cancel(context)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                showReminderNotificationRationale = true
                            } else {
                                enableReminders()
                            }
                        },
                    )
                }
                Text(
                    text = "Get a notification with up to 3 saved items worth another look. " +
                        "Items come back after 1, 3, 7, 14 and 30 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (remindersOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(9 to "9 AM", 13 to "1 PM", 20 to "8 PM").forEach { (hour, label) ->
                            FilterChip(
                                selected = reminderHour == hour,
                                onClick = {
                                    reminderHour = hour
                                    ReminderPrefs.setHour(context, hour)
                                    ReminderScheduler.schedule(context, replace = true)
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    OutlinedButton(onClick = { ReminderScheduler.sendTest(context) }) {
                        Text("Send a test reminder now")
                    }
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                categories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { renaming = category }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename ${category.name}")
                        }
                        IconButton(onClick = { deleting = category }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${category.name}")
                        }
                    }
                }
                OutlinedButton(onClick = { addingCategory = true }) {
                    Text("Add category")
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Privacy & Data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your saved discoveries and screen text processing stay entirely on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)),
                            )
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("View privacy policy")
                }
            }
        }
    }

    if (addingCategory) {
        CategoryNameDialog(
            title = "New category",
            confirmLabel = "Add",
            existingNames = categories.map { it.name },
            onDismiss = { addingCategory = false },
            onConfirm = { name ->
                addingCategory = false
                onAddCategory(name)
            },
        )
    }
    renaming?.let { category ->
        CategoryNameDialog(
            title = "Rename category",
            confirmLabel = "Save",
            initialName = category.name,
            existingNames = categories.map { it.name },
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                onRenameCategory(category.id, name)
            },
        )
    }
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${category.name}\"?") },
            text = { Text("Items in this category are kept and become uncategorised.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    onDeleteCategory(category.id)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
