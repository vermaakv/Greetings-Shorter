package com.greetingsorter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greetingsorter.Category
import com.greetingsorter.Source
import com.greetingsorter.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: UiState,
    partialAccess: Boolean,
    onToggleSource: (Source) -> Unit,
    onScan: () -> Unit,
    onRescanAll: () -> Unit,
    onStop: () -> Unit,
    onChangeAccess: () -> Unit,
    onOpenGroup: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Greeting Sorter") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            actions = {
                if (!state.scanning && state.hasScanned) {
                    IconButton(onClick = onRescanAll) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-read every image")
                    }
                }
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text("Look in", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Source.entries.forEach { source ->
                        FilterChip(
                            selected = source in state.sources,
                            onClick = { onToggleSource(source) },
                            label = { Text(source.title) },
                            enabled = !state.scanning,
                        )
                    }
                }
            }

            if (partialAccess) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("You allowed only some photos, so only those will be sorted.")
                            TextButton(onClick = onChangeAccess) { Text("Change photo access") }
                        }
                    }
                }
            }

            item {
                if (state.scanning) {
                    Column {
                        val fraction = if (state.total == 0) 0f else state.scanned.toFloat() / state.total
                        Text(
                            if (state.total == 0) "Finding images…"
                            else "Reading images… ${state.scanned} of ${state.total}"
                        )
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        )
                        OutlinedButton(onClick = onStop) { Text("Stop") }
                    }
                } else {
                    Button(
                        onClick = onScan,
                        enabled = state.sources.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.hasScanned) "Scan again for new images" else "Sort my greeting images")
                    }
                }
            }

            val groups = Category.entries.filter { state.groups[it].orEmpty().isNotEmpty() }
            if (state.hasScanned && !state.scanning && groups.isEmpty()) {
                item { Text("No images found yet. Try choosing more folders above.") }
            }
            items(groups, key = { it.name }) { category ->
                GroupCard(
                    category = category,
                    thumbnails = state.groups[category].orEmpty().take(4).map { it.image.uri },
                    count = state.groups[category].orEmpty().size,
                    onClick = { onOpenGroup(category) },
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    category: Category,
    thumbnails: List<android.net.Uri>,
    count: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(category.emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium)
                Text(if (count == 1) "1 image" else "$count images", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                thumbnails.take(3).forEach { uri ->
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
