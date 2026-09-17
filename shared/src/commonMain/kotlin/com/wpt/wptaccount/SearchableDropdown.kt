package com.wpt.wptaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun SearchableDropdown(
    label: String,
    options: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 150.dp,
    onSelect: (String) -> Unit
) {
    var searchText by remember(selected) { mutableStateOf(selected) }
    var isExpanded by remember { mutableStateOf(false) }
    val filteredOptions = options.filter { it.contains(searchText, ignoreCase = true) }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(isExpanded) {
        if (isExpanded) selectedIndex = 0
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(vertical = 4.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text = "$label : ", 
                style = MaterialTheme.typography.bodySmall, 
                modifier = Modifier.width(labelWidth),
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(8.dp))
        }
        
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = if (isExpanded) searchText else selected,
                onValueChange = {
                    searchText = it
                    isExpanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
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
                                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next)
                                    } else if (!isExpanded && searchText.isNotEmpty()) {
                                        val bestMatch = filteredOptions.firstOrNull() ?: searchText
                                        onSelect(bestMatch)
                                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Next)
                                    }
                                    true
                                }
                                Key.Escape -> {
                                    isExpanded = false
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            if (isExpanded && filteredOptions.isNotEmpty()) {
                Popup(
                    onDismissRequest = { isExpanded = false },
                    offset = androidx.compose.ui.unit.IntOffset(0, 60)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        shape = MaterialTheme.shapes.small,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = "List of $label",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(4.dp)
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
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isItemHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
