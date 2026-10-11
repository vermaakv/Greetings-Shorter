package com.greetingsorter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** Reads the text printed on an image using ML Kit, entirely on the phone. */
class TextReader(private val context: Context) {

    private val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    /** Reads an image and returns its category. Hindi text is only tried when English finds no greeting. */
    suspend fun classify(uri: Uri): Category {
        val bitmap = decode(uri) ?: return Category.NOT_GREETING
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val english = latin.process(image).await().text
            val category = GreetingClassifier.classify(english)
            if (category != Category.NOT_GREETING) return category
            val hindi = devanagari.process(image).await().text
            return GreetingClassifier.classify("$english\n$hindi")
        } finally {
            bitmap.recycle()
        }
    }

    /** Decodes the image scaled down to roughly [MAX_SIDE] pixels, which is plenty for big greeting text. */
    private fun decode(uri: Uri): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun close() {
        latin.close()
        devanagari.close()
    }

    private companion object {
        const val MAX_SIDE = 1280
    }
}
