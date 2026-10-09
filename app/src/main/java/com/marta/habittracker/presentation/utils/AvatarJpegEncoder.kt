package com.marta.habittracker.presentation.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

/**
 * Presentation-only JPEG encoder for avatar uploads. Domain stays free of Bitmap APIs.
 * Decodes toward [MAX_EDGE_PX] so a full-resolution bitmap is never allocated.
 */
object AvatarJpegEncoder {
    const val MAX_EDGE_PX = 512
    const val JPEG_QUALITY = 85

    fun encode(contentResolver: ContentResolver, uri: Uri): ByteArray {
        val bitmap = decodeBitmap(contentResolver, uri)
        return try {
            compressJpeg(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun inSampleSizeForMaxEdge(
        width: Int,
        height: Int,
        maxEdgePx: Int = MAX_EDGE_PX,
    ): Int {
        val longEdge = maxOf(width, height).coerceAtLeast(1)
        var sample = 1
        while (longEdge / sample > maxEdgePx) {
            sample *= 2
        }
        return sample
    }

    fun targetSizeForMaxEdge(
        width: Int,
        height: Int,
        maxEdgePx: Int = MAX_EDGE_PX,
    ): Pair<Int, Int> {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val longEdge = maxOf(safeWidth, safeHeight)
        if (longEdge <= maxEdgePx) {
            return safeWidth to safeHeight
        }
        val scale = maxEdgePx / longEdge.toFloat()
        return (safeWidth * scale).toInt().coerceAtLeast(1) to
            (safeHeight * scale).toInt().coerceAtLeast(1)
    }

    private fun decodeBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(contentResolver, uri)
        } else {
            decodeWithBitmapFactory(contentResolver, uri)
        }
    }

    private fun decodeWithImageDecoder(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
            val (targetWidth, targetHeight) = targetSizeForMaxEdge(
                info.size.width,
                info.size.height,
            )
            decoder.setTargetSize(targetWidth, targetHeight)
        }
    }

    private fun decodeWithBitmapFactory(contentResolver: ContentResolver, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: error("Cannot open image")

        val options = BitmapFactory.Options().apply {
            inSampleSize = inSampleSizeForMaxEdge(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: error("Cannot decode image")

        val longEdge = maxOf(sampled.width, sampled.height)
        if (longEdge <= MAX_EDGE_PX) {
            return sampled
        }
        val (width, height) = targetSizeForMaxEdge(sampled.width, sampled.height)
        val scaled = Bitmap.createScaledBitmap(sampled, width, height, true)
        if (scaled !== sampled) {
            sampled.recycle()
        }
        return scaled
    }

    private fun compressJpeg(source: Bitmap): ByteArray {
        val compressible = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }
        try {
            val output = ByteArrayOutputStream()
            val compressed = compressible.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            if (!compressed) {
                error("JPEG compress failed")
            }
            return output.toByteArray()
        } finally {
            if (compressible !== source) {
                compressible.recycle()
            }
        }
    }
}
