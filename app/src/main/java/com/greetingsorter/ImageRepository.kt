package com.greetingsorter

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File

/** Where an image came from. */
enum class Source(val title: String, val pathHints: List<String>) {
    WHATSAPP("WhatsApp", listOf("whatsapp")),
    TELEGRAM("Telegram", listOf("telegram")),
    DOWNLOADS("Downloads", listOf("download")),
    OTHER("Other folders", emptyList());

    companion object {
        fun fromPath(path: String): Source {
            val lower = path.lowercase()
            return entries.firstOrNull { s -> s.pathHints.any { lower.contains(it) } } ?: OTHER
        }
    }
}

data class MediaImage(
    val id: Long,
    val uri: Uri,
    val dateModified: Long,
    val source: Source,
)

data class SortedImage(val image: MediaImage, val category: Category)

/** Reads images from the phone's media library and remembers how each one was sorted. */
class ImageRepository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "classifications.json")

    /** Image id -> "dateModified|CATEGORY" (or "dateModified|CATEGORY|manual" for user overrides). */
    private val cache = HashMap<Long, String>()

    init {
        loadCache()
    }

    fun queryImages(sources: Set<Source>): List<MediaImage> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.DATA
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_MODIFIED,
            pathColumn,
        )
        val result = ArrayList<MediaImage>()
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val modCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val pathCol = c.getColumnIndexOrThrow(pathColumn)
            while (c.moveToNext()) {
                val path = c.getString(pathCol) ?: ""
                val source = Source.fromPath(path)
                if (source !in sources) continue
                // Skip WhatsApp's "Sent" folder: those are images the user sent, not received.
                if (path.lowercase().contains("/sent/")) continue
                val id = c.getLong(idCol)
                result += MediaImage(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    dateModified = c.getLong(modCol),
                    source = source,
                )
            }
        }
        return result
    }

    /** The saved category for an image, or null if it is new or changed since it was sorted. */
    @Synchronized
    fun cachedCategory(image: MediaImage): Category? {
        val parts = cache[image.id]?.split("|") ?: return null
        if (parts.size < 2 || parts[0].toLongOrNull() != image.dateModified) return null
        return runCatching { Category.valueOf(parts[1]) }.getOrNull()
    }

    @Synchronized
    fun remember(image: MediaImage, category: Category, manual: Boolean = false) {
        cache[image.id] = "${image.dateModified}|${category.name}" + if (manual) "|manual" else ""
    }

    @Synchronized
    fun forget(ids: Collection<Long>) {
        ids.forEach { cache.remove(it) }
    }

    /** Clears automatic results so the next scan re-reads every image; manual moves are kept. */
    @Synchronized
    fun clearAutomatic() {
        cache.entries.removeAll { !it.value.endsWith("|manual") }
        saveCache()
    }

    @Synchronized
    fun saveCache() {
        val json = JSONObject()
        cache.forEach { (id, value) -> json.put(id.toString(), value) }
        val tmp = File(cacheFile.path + ".tmp")
        tmp.writeText(json.toString())
        tmp.renameTo(cacheFile)
    }

    @Synchronized
    private fun loadCache() {
        if (!cacheFile.exists()) return
        runCatching {
            val json = JSONObject(cacheFile.readText())
            json.keys().forEach { key -> cache[key.toLong()] = json.getString(key) }
        }
    }
}
