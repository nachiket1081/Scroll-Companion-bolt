package com.example.scrollcompanion.ui.screens.rediscover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.ui.components.ContentCard
import com.example.scrollcompanion.ui.components.EmptyState
import com.example.scrollcompanion.viewmodel.RediscoverUiState

@Composable
fun RediscoverScreen(
    uiState: RediscoverUiState,
    onItemClick: (Long) -> Unit,
    onReviewed: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Rediscover",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Things you saved that are worth another look.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (uiState.items.isEmpty()) {
                    item {
                        EmptyState(
                            title = "You're all caught up",
                            message = "Saved items come back here after a day, then after 3, 7, 14 and 30 days.",
                        )
                    }
                } else {
                    items(uiState.items, key = { it.id }) { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ContentCard(item = item, onClick = { onItemClick(item.id) })
                            TextButton(
                                onClick = { onReviewed(item.id) },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Mark as reviewed")
                            }
                        }
                    }
                }
            }
        }
    }
}
