package com.greetingsorter.ui

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greetingsorter.MainViewModel
import com.greetingsorter.Screen

private val Colors = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E2000),
    secondaryContainer = Color(0xFFFFECD2),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5),
)

@Composable
fun App(
    viewModel: MainViewModel,
    permissions: Array<String>,
    hasPermission: () -> Boolean,
    partialAccess: () -> Boolean,
    onShare: (List<Uri>) -> Unit,
    onDelete: (Collection<Long>) -> Unit,
) {
    MaterialTheme(colorScheme = Colors) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        var granted by remember { mutableStateOf(hasPermission()) }
        var partial by remember { mutableStateOf(partialAccess()) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            granted = hasPermission()
            partial = partialAccess()
            if (granted) viewModel.scan()
        }
        // Re-check when coming back from the phone's Settings screen.
        LifecycleResumeEffect(Unit) {
            granted = hasPermission()
            partial = partialAccess()
            onPauseOrDispose { }
        }

        // Android 10 and older have no system confirmation, so ask here before deleting.
        var confirmDelete by remember { mutableStateOf<Collection<Long>?>(null) }
        val delete: (Collection<Long>) -> Unit = { ids ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                confirmDelete = ids
            } else {
                onDelete(ids)
            }
        }

        BackHandler(enabled = state.screen != Screen.Home || state.selection.isNotEmpty()) {
            viewModel.back()
        }

        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(state.message) {
            state.message?.let {
                snackbar.showSnackbar(it)
                viewModel.clearMessage()
            }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            val modifier = Modifier.padding(padding)
            if (!granted) {
                PermissionScreen(modifier) { permissionLauncher.launch(permissions) }
            } else {
                when (val screen = state.screen) {
                    Screen.Home -> HomeScreen(
                        state = state,
                        partialAccess = partial,
                        onToggleSource = viewModel::toggleSource,
                        onScan = { viewModel.scan() },
                        onRescanAll = { viewModel.scan(rescanAll = true) },
                        onStop = viewModel::stopScan,
                        onChangeAccess = { permissionLauncher.launch(permissions) },
                        onOpenGroup = { viewModel.open(Screen.Group(it)) },
                        modifier = modifier,
                    )
                    is Screen.Group -> GroupScreen(
                        category = screen.category,
                        images = state.groups[screen.category].orEmpty(),
                        selection = state.selection,
                        onBack = { viewModel.back() },
                        onOpen = { viewModel.open(Screen.Viewer(screen.category, it)) },
                        onToggle = viewModel::toggleSelected,
                        onSelectAll = { viewModel.selectAll(screen.category) },
                        onShare = onShare,
                        onDelete = delete,
                        modifier = modifier,
                    )
                    // Keyed so moving an image to another group opens a fresh viewer on that image.
                    is Screen.Viewer -> key(screen.category) { ViewerScreen(
                        category = screen.category,
                        images = state.groups[screen.category].orEmpty(),
                        startId = screen.imageId,
                        onBack = { viewModel.back() },
                        onShare = onShare,
                        onDelete = delete,
                        onMove = viewModel::moveTo,
                        modifier = modifier,
                    ) }
                }
            }
        }

        confirmDelete?.let { ids ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text(if (ids.size == 1) "Delete this image?" else "Delete ${ids.size} images?") },
                text = { Text("They will be removed from your phone.") },
                confirmButton = {
                    TextButton(onClick = { confirmDelete = null; onDelete(ids) }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun PermissionScreen(modifier: Modifier, onAllow: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌅🌙🎂🪔", style = MaterialTheme.typography.displaySmall)
        Text(
            "Greeting Sorter",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Sorts the Good Morning, Good Night, festival and birthday pictures you receive on " +
                "WhatsApp and Telegram into groups, so you can view, share or clear them out in one go.\n\n" +
                "The text on each picture is read on your phone. Nothing is uploaded.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onAllow) { Text("Allow access to photos") }
    }
}
