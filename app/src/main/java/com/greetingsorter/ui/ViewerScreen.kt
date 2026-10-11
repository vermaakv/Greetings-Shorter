package com.greetingsorter.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greetingsorter.Category
import com.greetingsorter.SortedImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    category: Category,
    images: List<SortedImage>,
    startId: Long,
    onBack: () -> Unit,
    onShare: (List<Uri>) -> Unit,
    onDelete: (Collection<Long>) -> Unit,
    onMove: (Long, Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val startIndex = images.indexOfFirst { it.image.id == startId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = startIndex, pageCount = { images.size })
    val current = images[pager.currentPage.coerceIn(0, images.lastIndex)]
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(Color.Black)) {
        TopAppBar(
            title = { Text("${pager.currentPage + 1} / ${images.size}") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { onShare(listOf(current.image.uri)) }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Move to another group")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        Text("Move to…", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        Category.entries.filter { it != category }.forEach { target ->
                            DropdownMenuItem(
                                text = { Text("${target.emoji} ${target.title}") },
                                onClick = {
                                    menuOpen = false
                                    onMove(current.image.id, target)
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { onDelete(listOf(current.image.id)) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            },
        )
        HorizontalPager(state = pager, key = { images[it].image.id }, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = images[page].image.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
