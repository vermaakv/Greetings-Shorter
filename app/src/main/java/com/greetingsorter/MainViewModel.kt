package com.greetingsorter

import android.app.Application
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data class Group(val category: Category) : Screen
    data class Viewer(val category: Category, val imageId: Long) : Screen
}

data class UiState(
    val sources: Set<Source> = setOf(Source.WHATSAPP, Source.TELEGRAM, Source.DOWNLOADS),
    val groups: Map<Category, List<SortedImage>> = emptyMap(),
    val scanning: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val hasScanned: Boolean = false,
    val screen: Screen = Screen.Home,
    val selection: Set<Long> = emptySet(),
    val message: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ImageRepository(app)
    private val reader = TextReader(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var sorted = LinkedHashMap<Long, SortedImage>()

    fun toggleSource(source: Source) {
        _state.update {
            val s = if (source in it.sources) it.sources - source else it.sources + source
            it.copy(sources = s)
        }
    }

    fun scan(rescanAll: Boolean = false) {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true, scanned = 0, total = 0, message = null) }
            try {
                if (rescanAll) withContext(Dispatchers.IO) { repository.clearAutomatic() }
                val images = withContext(Dispatchers.IO) { repository.queryImages(_state.value.sources) }
                sorted = LinkedHashMap()
                _state.update { it.copy(total = images.size) }
                images.forEachIndexed { index, image ->
                    if (!isActive) return@launch
                    val category = repository.cachedCategory(image)
                        ?: withContext(Dispatchers.Default) {
                            runCatching { reader.classify(image.uri) }.getOrDefault(Category.NOT_GREETING)
                        }.also { repository.remember(image, it) }
                    sorted[image.id] = SortedImage(image, category)
                    if (index % 20 == 19 || index == images.lastIndex) {
                        publishGroups(scanned = index + 1)
                        withContext(Dispatchers.IO) { repository.saveCache() }
                    }
                }
                if (images.isEmpty()) {
                    publishGroups(scanned = 0)
                    _state.update { it.copy(message = "No images found in the chosen folders.") }
                }
            } finally {
                withContext(NonCancellable + Dispatchers.IO) { repository.saveCache() }
                publishGroups(scanned = sorted.size)
                _state.update { it.copy(scanning = false, hasScanned = true) }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    private fun publishGroups(scanned: Int = _state.value.scanned) {
        val groups = sorted.values.groupBy { it.category }
        _state.update { it.copy(groups = groups, scanned = scanned) }
    }

    // --- Navigation -------------------------------------------------------------------------

    fun open(screen: Screen) = _state.update { it.copy(screen = screen, selection = emptySet()) }

    /** Returns false when already on the home screen, so the system can close the app. */
    fun back(): Boolean {
        val s = _state.value
        when {
            s.selection.isNotEmpty() -> _state.update { it.copy(selection = emptySet()) }
            s.screen is Screen.Viewer -> open(Screen.Group(s.screen.category))
            s.screen is Screen.Group -> open(Screen.Home)
            else -> return false
        }
        return true
    }

    // --- Selection --------------------------------------------------------------------------

    fun toggleSelected(id: Long) = _state.update {
        it.copy(selection = if (id in it.selection) it.selection - id else it.selection + id)
    }

    fun selectAll(category: Category) = _state.update {
        val ids = it.groups[category].orEmpty().map { img -> img.image.id }.toSet()
        it.copy(selection = if (it.selection == ids) emptySet() else ids)
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    // --- Changing a group by hand -----------------------------------------------------------

    fun moveTo(imageId: Long, category: Category) {
        val item = sorted[imageId] ?: return
        sorted[imageId] = item.copy(category = category)
        repository.remember(item.image, category, manual = true)
        viewModelScope.launch(Dispatchers.IO) { repository.saveCache() }
        publishGroups()
        _state.update { it.copy(screen = Screen.Viewer(category, imageId), message = "Moved to ${category.title}") }
    }

    // --- Deleting ---------------------------------------------------------------------------

    /**
     * On Android 11+ returns a system prompt the activity must show so the user confirms the delete.
     * On older versions deletes straight away and returns null.
     */
    fun deleteRequest(ids: Collection<Long>): IntentSender? {
        val uris = ids.mapNotNull { sorted[it]?.image?.uri }
        if (uris.isEmpty()) return null
        val resolver = getApplication<Application>().contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return MediaStore.createDeleteRequest(resolver, uris).intentSender
        }
        val deleted = ids.filter { id ->
            val uri = sorted[id]?.image?.uri ?: return@filter false
            runCatching { resolver.delete(uri, null, null) > 0 }.getOrDefault(false)
        }
        onDeleted(deleted)
        if (deleted.size < ids.size) {
            _state.update { it.copy(message = "Some images could not be deleted.") }
        }
        return null
    }

    fun onDeleted(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        ids.forEach { sorted.remove(it) }
        repository.forget(ids)
        viewModelScope.launch(Dispatchers.IO) { repository.saveCache() }
        publishGroups()
        _state.update { s ->
            val screen = s.screen
            val next = if (screen is Screen.Viewer && screen.imageId in ids) Screen.Group(screen.category) else screen
            s.copy(
                screen = next,
                selection = emptySet(),
                message = if (ids.size == 1) "Deleted 1 image" else "Deleted ${ids.size} images",
            )
        }
    }

    fun imagesIn(category: Category): List<SortedImage> = _state.value.groups[category].orEmpty()

    override fun onCleared() {
        reader.close()
    }
}
