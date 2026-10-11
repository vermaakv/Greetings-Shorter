package com.greetingsorter.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greetingsorter.Category
import com.greetingsorter.SortedImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupScreen(
    category: Category,
    images: List<SortedImage>,
    selection: Set<Long>,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onShare: (List<Uri>) -> Unit,
    onDelete: (Collection<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selecting = selection.isNotEmpty()
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(if (selecting) "${selection.size} selected" else "${category.emoji} ${category.title} (${images.size})")
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (selecting) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                titleContentColor = if (selecting) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = if (selecting) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = if (selecting) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
            ),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    if (selecting) Icon(Icons.Default.Close, contentDescription = "Clear selection")
                    else Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                }
                if (selecting) {
                    val chosen = images.filter { it.image.id in selection }
                    IconButton(onClick = { onShare(chosen.map { it.image.uri }) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share selected")
                    }
                    IconButton(onClick = { onDelete(chosen.map { it.image.id }) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                    }
                }
            },
        )
        if (!selecting && images.isNotEmpty()) {
            Text(
                "Tap a picture to view it. Long-press to select several, or use Select all.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            contentPadding = PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(images, key = { it.image.id }) { item ->
                val id = item.image.id
                val selected = id in selection
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .combinedClickable(
                            onClick = { if (selecting) onToggle(id) else onOpen(id) },
                            onLongClick = { onToggle(id) },
                        )
                ) {
                    AsyncImage(
                        model = item.image.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (selected) {
                        Box(Modifier.fillMaxSize().background(Color(0x55000000)))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
            }
        }
    }
}
