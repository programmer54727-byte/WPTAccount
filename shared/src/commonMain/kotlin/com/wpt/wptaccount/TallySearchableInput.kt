package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun TallySearchableInput(
    label: String,
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onCreate: (() -> Unit)? = null,
    onSelect: (String) -> Unit
) {
    var searchText by remember(selected) { mutableStateOf(selected) }
    var isExpanded by remember { mutableStateOf(false) }
    val filteredOptions = options.filter { it.contains(searchText, ignoreCase = true) }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isExpanded) {
        if (isExpanded) selectedIndex = 0
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = if (isExpanded) searchText else selected,
            onValueChange = {
                searchText = it
                isExpanded = true
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        // Alt + C Shortcut
                        if (event.isAltPressed && event.key == Key.C) {
                            onCreate?.invoke()
                            true
                        } else {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    if (filteredOptions.isNotEmpty()) {
                                        isExpanded = true
                                        selectedIndex = (selectedIndex + 1) % filteredOptions.size
                                    }
                                    true
                                }
                                Key.DirectionUp -> {
                                    if (filteredOptions.isNotEmpty()) {
                                        isExpanded = true
                                        selectedIndex = (selectedIndex - 1 + filteredOptions.size) % filteredOptions.size
                                    }
                                    true
                                }
                                Key.Enter -> {
                                    if (isExpanded && filteredOptions.isNotEmpty()) {
                                        onSelect(filteredOptions[selectedIndex])
                                        isExpanded = false
                                    }
                                    true
                                }
                                Key.Escape -> {
                                    isExpanded = false
                                    true
                                }
                                else -> false
                            }
                        }
                    } else false
                },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )

        if (isExpanded && filteredOptions.isNotEmpty()) {
            Popup(
                onDismissRequest = { isExpanded = false },
                offset = androidx.compose.ui.unit.IntOffset(0, 65)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 250.dp),
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn {
                        item {
                            Text(
                                text = "List of Options",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(8.dp)
                            )
                        }
                        itemsIndexed(filteredOptions) { index, option ->
                            val isItemHighlighted = index == selectedIndex
                            Text(
                                text = option,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isItemHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        onSelect(option)
                                        isExpanded = false
                                    }
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isItemHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
