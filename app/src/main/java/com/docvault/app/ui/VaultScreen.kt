package com.docvault.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.docvault.app.DocMimeTypes
import com.docvault.app.VaultViewModel
import com.docvault.app.data.ItemKind
import com.docvault.app.data.SavedItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun SavedItem.isPhoto(): Boolean = kind == ItemKind.IMAGE

private fun List<SavedItem>.forTab(tab: VaultTab): List<SavedItem> = when (tab) {
    VaultTab.PHOTOS -> filter { it.isPhoto() }
    VaultTab.DOCUMENTS -> filter { !it.isPhoto() }
}

private fun List<SavedItem>.sortedBy(order: SortOrder): List<SavedItem> = when (order) {
    SortOrder.NEWEST -> sortedByDescending { it.addedAt }
    SortOrder.OLDEST -> sortedBy { it.addedAt }
    SortOrder.NAME_AZ -> sortedBy { it.displayName.lowercase(Locale.getDefault()) }
}

private fun formatAddedAt(millis: Long): String {
    val fmt = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    return fmt.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vm: VaultViewModel,
    onOpenSettings: () -> Unit,
    onFilePickerLifecycle: (Boolean) -> Unit = {}
) {
    val items by vm.items.collectAsState()
    val snackMsg by vm.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST) }
    var sourceFilter by remember { mutableStateOf<String?>(null) }

    val tab = if (selectedTab == 0) VaultTab.PHOTOS else VaultTab.DOCUMENTS
    val tabItems = remember(items, tab) { items.forTab(tab) }
    val sourcesInTab = remember(tabItems) {
        tabItems.map { it.sourceLabel }.distinct().sorted()
    }
    val filtered = remember(tabItems, sourceFilter) {
        if (sourceFilter == null) tabItems
        else tabItems.filter { it.sourceLabel == sourceFilter }
    }
    val displayList = remember(filtered, sortOrder) { filtered.sortedBy(sortOrder) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            onFilePickerLifecycle(false)
            vm.importUris(uris)
        }
    )

    LaunchedEffect(snackMsg) {
        val m = snackMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m)
        vm.dismissSnackbar()
    }

    LaunchedEffect(selectedTab) {
        sourceFilter = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Doc Vault")
                        Text(
                            text = "Rishav",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onFilePickerLifecycle(true)
                    pickLauncher.launch(DocMimeTypes.ALL)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add document or image")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Photos") },
                    icon = {
                        Icon(Icons.Default.Image, contentDescription = null)
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Documents") },
                    icon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Sort",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortOrder == SortOrder.NEWEST,
                        onClick = { sortOrder = SortOrder.NEWEST },
                        label = { Text("Newest") }
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.OLDEST,
                        onClick = { sortOrder = SortOrder.OLDEST },
                        label = { Text("Oldest") }
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.NAME_AZ,
                        onClick = { sortOrder = SortOrder.NAME_AZ },
                        label = { Text("Name A–Z") }
                    )
                }
                if (sourcesInTab.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Source",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sourceFilter == null,
                            onClick = { sourceFilter = null },
                            label = { Text("All") }
                        )
                        sourcesInTab.forEach { src ->
                            FilterChip(
                                selected = sourceFilter == src,
                                onClick = {
                                    sourceFilter = if (sourceFilter == src) null else src
                                },
                                label = { Text(src) }
                            )
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap + to add photos, PDFs, Word files, and more.\nYou can pick from Photos, Files, or apps like WhatsApp.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == VaultTab.PHOTOS) {
                            "No photos in this category for the selected filters."
                        } else {
                            "No documents in this category for the selected filters."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList, key = { it.id }) { item ->
                        VaultItemCard(
                            item = item,
                            onOpen = {
                                val file = vm.resolveFile(item)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, item.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            },
                            onDelete = { vm.delete(item) },
                            thumbFile = vm.resolveThumb(item)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultItemCard(
    item: SavedItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    thumbFile: java.io.File?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    thumbFile != null && thumbFile.exists() -> {
                        val ctx = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(thumbFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    item.kind == ItemKind.PDF -> {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.4f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    item.kind == ItemKind.WORD -> {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.4f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    item.kind == ItemKind.IMAGE -> {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.4f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.4f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = formatAddedAt(item.addedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "From: ${item.sourceLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onOpen) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove from vault")
                }
            }
        }
    }
}
