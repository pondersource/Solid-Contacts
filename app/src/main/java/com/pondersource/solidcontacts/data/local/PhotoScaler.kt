package com.pondersource.solidcontacts.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-encodes contact photos to sizes the rest of the app can actually carry.
 *
 * A pod may hold a photo of any size — one account here had a 3.5 MB portrait — and nothing
 * downstream wants it at that size: a SQLite row must fit in a 2 MB cursor window, the IPC call
 * to the host app has roughly a 1 MB transaction budget, and a 48 dp list row needs a few
 * kilobytes. So a photo is kept at two sizes and the original is never stored.
 */
@Singleton
class PhotoScaler @Inject constructor() {

    /** The small copy that lives in the contact row and draws list avatars. */
    fun thumbnail(bytes: ByteArray): ByteArray? = scaled(bytes, THUMBNAIL_SIZE, THUMBNAIL_QUALITY)

    /**
     * The copy kept on disk and sent to the pod: large enough for a detail screen, small enough
     * to cross the Binder boundary without being refused.
     */
    fun displayCopy(bytes: ByteArray): ByteArray? = scaled(bytes, DISPLAY_SIZE, DISPLAY_QUALITY)

    /** Returns a JPEG whose longest side is at most [maxSize], or `null` if this is not an image. */
    fun scaled(bytes: ByteArray, maxSize: Int, quality: Int): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Sub-sample while decoding, so a huge source never lands in memory at full size.
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSize)
        }
        val decoded = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }.getOrNull() ?: return null

        val scaled = if (decoded.width > maxSize || decoded.height > maxSize) {
            val ratio = minOf(maxSize.toFloat() / decoded.width, maxSize.toFloat() / decoded.height)
            val target = Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * ratio).toInt().coerceAtLeast(1),
                (decoded.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
            if (target !== decoded) decoded.recycle()
            target
        } else {
            decoded
        }

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    /** The power of two that gets the decode closest to the target without going under it. */
    private fun sampleSizeFor(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val THUMBNAIL_SIZE = 128
        const val THUMBNAIL_QUALITY = 80
        const val DISPLAY_SIZE = 640
        const val DISPLAY_QUALITY = 85
    }
}
