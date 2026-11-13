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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val BubbleSize = 56.dp
private val PanelWidth = 232.dp
private val DockedTouchWidth = 28.dp
private val DockedTabWidth = 14.dp
private val DockedTabHeight = 56.dp

enum class DockSide { LEFT, RIGHT }

@Composable
fun FloatingAssistantOverlay(
    expanded: Boolean,
    docked: Boolean,
    dockSide: DockSide,
    onBubbleTap: () -> Unit,
    onDockedTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onCollapse: () -> Unit,
    onExtractText: () -> Unit,
    onOpenVault: () -> Unit,
    onHide: () -> Unit,
) {
    if (docked && !expanded) {
        DockedTab(side = dockSide, onTap = onDockedTap)
        return
    }
    Column {
        Bubble(onTap = onBubbleTap, onDragStart = onDragStart, onDrag = onDrag, onDragEnd = onDragEnd)
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            ActionPanel(
                onCollapse = onCollapse,
                onExtractText = onExtractText,
                onOpenVault = onOpenVault,
                onHide = onHide,
            )
        }
    }
}

/** Slim tab shown on a screen edge when the bubble has been idle. Tap it to bring the bubble back. */
@Composable
private fun DockedTab(side: DockSide, onTap: () -> Unit) {
    val currentOnTap by rememberUpdatedState(onTap)
    val shape = if (side == DockSide.LEFT) {
        RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
    }
    Box(
        modifier = Modifier
            .width(DockedTouchWidth)
            .height(DockedTabHeight + 16.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) },
        contentAlignment = if (side == DockSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .width(DockedTabWidth)
                .height(DockedTabHeight)
                .shadow(4.dp, shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
        )
    }
}

@Composable
private fun Bubble(
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

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
                detectDragGestures(
                    onDragStart = { currentOnDragStart() },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                ) { change, dragAmount ->
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
    onExtractText: () -> Unit,
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
            ActionRow(label = "Extract text", onClick = onExtractText)
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
private fun ActionRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
