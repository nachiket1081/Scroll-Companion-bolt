package com.example.scrollcompanion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.scrollcompanion.domain.TagNormalizer

/**
 * Shows the current tags as removable chips, a field to type a new one (press Done or type a comma),
 * and one-tap suggestions taken from tags used on other items.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditor(
    tags: List<String>,
    suggestions: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }

    fun commit() {
        if (input.isNotBlank()) onTagsChange(TagNormalizer.add(tags, input))
        input = ""
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onTagsChange(tags - tag) },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }

        if (tags.size < TagNormalizer.MAX_TAGS) {
            OutlinedTextField(
                value = input,
                onValueChange = { value ->
                    if (value.endsWith(",")) {
                        input = value.dropLast(1)
                        commit()
                    } else {
                        input = value
                    }
                },
                label = { Text("Add a tag") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier = Modifier.fillMaxWidth(),
            )

            val unused = suggestions.filter { it !in tags }.take(MAX_SUGGESTIONS)
            if (unused.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    unused.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onTagsChange(TagNormalizer.add(tags, suggestion)) },
                            label = { Text("#$suggestion") },
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_SUGGESTIONS = 8
