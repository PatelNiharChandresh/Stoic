package com.rudy.stoic.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.rudy.stoic.domain.model.InstalledApp
import com.rudy.stoic.domain.model.RingItem
import com.rudy.stoic.domain.model.RingItemType
import com.rudy.stoic.domain.model.SystemActions
import com.rudy.stoic.ui.theme.BackgroundDark
import com.rudy.stoic.ui.theme.BlockBackground
import com.rudy.stoic.ui.theme.CyanPrimary
import com.rudy.stoic.ui.theme.SurfaceDarkAlt
import com.rudy.stoic.ui.theme.TextPrimary
import com.rudy.stoic.ui.theme.TextSecondary

@Composable
fun RingConfigScreen(
    onDismiss: () -> Unit,
    viewModel: RingConfigViewModel = hiltViewModel()
) {
    val ringItems by viewModel.ringItems.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<RingItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 48.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ring Config",
                    color = CyanPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(onClick = onDismiss) {
                    Text("Done", color = CyanPrimary, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${ringItems.size}/8 slots used",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ring items list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(ringItems) { index, item ->
                    RingItemRow(
                        item = item,
                        index = index,
                        totalItems = ringItems.size,
                        canRemove = ringItems.size > 1,
                        onMoveUp = {
                            if (index > 0) viewModel.moveItem(index, index - 1)
                        },
                        onMoveDown = {
                            if (index < ringItems.size - 1) viewModel.moveItem(index, index + 1)
                        },
                        onRemove = { viewModel.removeItem(item) },
                        onEdit = if (item.type == RingItemType.FOLDER) {
                            { editingFolder = item }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add button
            if (ringItems.size < RingConfigViewModel.MAX_ITEMS) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary.copy(alpha = 0.2f),
                        contentColor = CyanPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "+ Add Item",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Add item type chooser
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = SurfaceDarkAlt,
            title = {
                Text("Add Item", color = CyanPrimary, fontFamily = FontFamily.Monospace)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogOption("App") {
                        showAddDialog = false
                        showAppPicker = true
                    }
                    DialogOption("Folder") {
                        showAddDialog = false
                        showFolderDialog = true
                    }
                    DialogOption("System Action") {
                        showAddDialog = false
                        showActionPicker = true
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // App picker
    if (showAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            onAppSelected = { app ->
                viewModel.addAppItem(app)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    // Folder name dialog
    if (showFolderDialog) {
        FolderNameDialog(
            onConfirm = { name ->
                viewModel.addFolder(name)
                showFolderDialog = false
            },
            onDismiss = { showFolderDialog = false }
        )
    }

    // System action picker
    if (showActionPicker) {
        SystemActionPickerDialog(
            onActionSelected = { actionId, label ->
                viewModel.addSystemAction(actionId, label)
                showActionPicker = false
            },
            onDismiss = { showActionPicker = false }
        )
    }

    // Folder editor
    if (editingFolder != null) {
        FolderEditorDialog(
            folder = editingFolder!!,
            allApps = installedApps,
            onAddApp = { packageName ->
                viewModel.addAppToFolder(editingFolder!!.id, packageName)
                // Refresh the editing folder reference with updated data
                val updated = viewModel.ringItems.value.find { it.id == editingFolder!!.id }
                editingFolder = updated
            },
            onRemoveApp = { packageName ->
                viewModel.removeAppFromFolder(editingFolder!!.id, packageName)
                val updated = viewModel.ringItems.value.find { it.id == editingFolder!!.id }
                editingFolder = updated
            },
            onDismiss = { editingFolder = null }
        )
    }
}

@Composable
private fun RingItemRow(
    item: RingItem,
    index: Int,
    totalItems: Int,
    canRemove: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onEdit != null) Modifier.clickable { onEdit() } else Modifier
            )
            .background(BlockBackground.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Type indicator
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(CyanPrimary.copy(alpha = 0.2f), CircleShape)
        ) {
            Text(
                text = when (item.type) {
                    RingItemType.APP -> "A"
                    RingItemType.FOLDER -> "F"
                    RingItemType.SYSTEM_ACTION -> "S"
                },
                color = CyanPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Label and type
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                color = TextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when (item.type) {
                    RingItemType.APP -> "App"
                    RingItemType.FOLDER -> "Folder (${item.appList?.size ?: 0} apps) — tap to edit"
                    RingItemType.SYSTEM_ACTION -> "System"
                },
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Reorder buttons
        if (index > 0) {
            Text(
                text = "↑",
                color = CyanPrimary,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onMoveUp() }
                    .padding(horizontal = 6.dp)
            )
        }
        if (index < totalItems - 1) {
            Text(
                text = "↓",
                color = CyanPrimary,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onMoveDown() }
                    .padding(horizontal = 6.dp)
            )
        }

        // Remove button
        if (canRemove) {
            Text(
                text = "✕",
                color = TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable { onRemove() }
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun DialogOption(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 16.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(BlockBackground.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(14.dp)
    )
}

@Composable
private fun AppPickerDialog(
    apps: List<InstalledApp>,
    onAppSelected: (InstalledApp) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkAlt,
        title = {
            Text("Select App", color = CyanPrimary, fontFamily = FontFamily.Monospace)
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(app) }
                            .padding(8.dp)
                    ) {
                        val bitmap = remember(app.packageName) {
                            try {
                                app.icon?.toBitmap(64, 64)?.asImageBitmap()
                            } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap,
                                contentDescription = app.name,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = app.name,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun FolderNameDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkAlt,
        title = {
            Text("New Folder", color = CyanPrimary, fontFamily = FontFamily.Monospace)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanPrimary
                ),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create", color = if (name.isNotBlank()) CyanPrimary else TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun SystemActionPickerDialog(
    onActionSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val actions = listOf(
        SystemActions.SEARCH to "Search",
        SystemActions.SETTINGS to "Settings",
        SystemActions.CALLS to "Calls",
        SystemActions.MESSAGES to "Messages"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkAlt,
        title = {
            Text("System Action", color = CyanPrimary, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { (actionId, label) ->
                    DialogOption(text = label) {
                        onActionSelected(actionId, label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun FolderEditorDialog(
    folder: RingItem,
    allApps: List<InstalledApp>,
    onAddApp: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val folderApps = folder.appList.orEmpty()
    var showAddAppPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkAlt,
        title = {
            Text(
                text = "Edit: ${folder.label}",
                color = CyanPrimary,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Text(
                    text = "${folderApps.size} apps in folder",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(folderApps) { packageName ->
                        val app = allApps.find { it.packageName == packageName }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BlockBackground.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            if (app != null) {
                                val bitmap = remember(app.packageName) {
                                    try {
                                        app.icon?.toBitmap(64, 64)?.asImageBitmap()
                                    } catch (_: Exception) { null }
                                }
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap,
                                        contentDescription = app.name,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = app.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = packageName,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text(
                                text = "✕",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { onRemoveApp(packageName) }
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "+ Add App",
                            color = CyanPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddAppPicker = true }
                                .background(CyanPrimary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CyanPrimary)
            }
        },
        dismissButton = {}
    )

    if (showAddAppPicker) {
        val availableApps = allApps.filter { it.packageName !in folderApps }
        MultiSelectAppPickerDialog(
            apps = availableApps,
            onAppsSelected = { selectedApps ->
                selectedApps.forEach { app -> onAddApp(app.packageName) }
                showAddAppPicker = false
            },
            onDismiss = { showAddAppPicker = false }
        )
    }
}

@Composable
private fun MultiSelectAppPickerDialog(
    apps: List<InstalledApp>,
    onAppsSelected: (List<InstalledApp>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedPackages = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }

    val sortedAndFilteredApps = remember(apps, searchQuery) {
        apps.sortedBy { it.name.lowercase() }
            .filter {
                searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDarkAlt,
        title = {
            Column {
                Text("Select Apps", color = CyanPrimary, fontFamily = FontFamily.Monospace)
                if (selectedPackages.isNotEmpty()) {
                    Text(
                        text = "${selectedPackages.size} selected",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search apps...", color = TextSecondary, fontSize = 13.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(sortedAndFilteredApps) { app ->
                        val isSelected = app.packageName in selectedPackages
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) CyanPrimary.copy(alpha = 0.15f)
                                    else androidx.compose.ui.graphics.Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedPackages.remove(app.packageName)
                                    } else {
                                        selectedPackages.add(app.packageName)
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        if (isSelected) CyanPrimary else BlockBackground,
                                        RoundedCornerShape(4.dp)
                                    )
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = BackgroundDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            val bitmap = remember(app.packageName) {
                                try {
                                    app.icon?.toBitmap(64, 64)?.asImageBitmap()
                                } catch (_: Exception) { null }
                            }
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap,
                                    contentDescription = app.name,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.name,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = apps.filter { it.packageName in selectedPackages }
                    onAppsSelected(selected)
                },
                enabled = selectedPackages.isNotEmpty()
            ) {
                Text(
                    text = if (selectedPackages.isNotEmpty()) "Add (${selectedPackages.size})" else "Add",
                    color = if (selectedPackages.isNotEmpty()) CyanPrimary else TextSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
