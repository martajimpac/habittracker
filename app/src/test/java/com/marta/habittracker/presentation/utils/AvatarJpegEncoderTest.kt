package com.marta.habittracker.presentation.utils

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class AvatarJpegEncoderTest {

    @Test
    fun `inSampleSizeForMaxEdge is power of two that fits long edge`() {
        assertEquals(4, AvatarJpegEncoder.inSampleSizeForMaxEdge(2048, 1536))
        assertEquals(1, AvatarJpegEncoder.inSampleSizeForMaxEdge(512, 400))
        assertEquals(1, AvatarJpegEncoder.inSampleSizeForMaxEdge(256, 256))
        assertEquals(2, AvatarJpegEncoder.inSampleSizeForMaxEdge(1024, 768))
    }

    @Test
    fun `targetSizeForMaxEdge keeps aspect ratio at 512 long edge`() {
        assertEquals(512 to 384, AvatarJpegEncoder.targetSizeForMaxEdge(2048, 1536))
        assertEquals(400 to 300, AvatarJpegEncoder.targetSizeForMaxEdge(400, 300))
        assertEquals(512 to 512, AvatarJpegEncoder.targetSizeForMaxEdge(1024, 1024))
    }

    @Test
    fun `encode from large jpeg uri downsamples to max edge`() {
        val context = RuntimeEnvironment.getApplication()
        val source = Bitmap.createBitmap(2048, 1536, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "avatar-src.jpg")
        file.outputStream().use { out ->
            source.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        source.recycle()

        val bytes = AvatarJpegEncoder.encode(context.contentResolver, Uri.fromFile(file))

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertTrue(bytes.isNotEmpty())
        assertTrue(decoded.width <= AvatarJpegEncoder.MAX_EDGE_PX)
        assertTrue(decoded.height <= AvatarJpegEncoder.MAX_EDGE_PX)
        assertTrue(maxOf(decoded.width, decoded.height) <= AvatarJpegEncoder.MAX_EDGE_PX)
    }
}
