package com.example.scrollcompanion.service.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val BubbleSize = 56.dp
private val PanelWidth = 232.dp

@Composable
fun FloatingAssistantOverlay(
    expanded: Boolean,
    onBubbleTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onCollapse: () -> Unit,
    onOpenVault: () -> Unit,
    onHide: () -> Unit,
) {
    Column {
        Bubble(onTap = onBubbleTap, onDrag = onDrag)
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            ActionPanel(
                onCollapse = onCollapse,
                onOpenVault = onOpenVault,
                onHide = onHide,
            )
        }
    }
}

@Composable
private fun Bubble(
    onTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = Modifier
            .size(BubbleSize)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnTap() })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Scroll Companion",
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun ActionPanel(
    onCollapse: () -> Unit,
    onOpenVault: () -> Unit,
    onHide: () -> Unit,
) {
    Card(
        modifier = Modifier.width(PanelWidth).padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Scroll Companion",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close panel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            PendingActionRow(label = "Save this screen")
            PendingActionRow(label = "Extract text")
            PendingActionRow(label = "View extracted content")
            PendingActionRow(label = "Save with notes")

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            TextButton(onClick = onOpenVault, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Open Knowledge Vault",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = onHide, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Hide bubble",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PendingActionRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Soon",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
