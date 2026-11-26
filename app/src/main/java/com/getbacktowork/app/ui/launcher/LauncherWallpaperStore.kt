package com.getbacktowork.app.ui.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.getbacktowork.app.data.FocusSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles offline storage, downscaling, and loading of the user's optional launcher wallpaper
 * inside app-private filesDir without requiring storage permissions.
 */
object LauncherWallpaperStore {

    private const val WALLPAPER_FILE_NAME = "launcher_wallpaper.jpg"
    private const val MAX_WALLPAPER_EDGE_PX = 2048

    fun wallpaperFile(context: Context): File = File(context.filesDir, WALLPAPER_FILE_NAME)

    fun hasSavedWallpaperFile(context: Context): Boolean {
        val file = wallpaperFile(context)
        return file.exists() && file.length() > 0L
    }

    internal fun computeScaledDimensions(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return maxEdge to maxEdge
        val largest = maxOf(width, height)
        if (largest <= maxEdge) return width to height
        val scale = maxEdge.toFloat() / largest.toFloat()
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }

    private fun computeInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while ((width / sample) > maxEdge * 2 || (height / sample) > maxEdge * 2) {
            sample *= 2
        }
        return sample
    }

    suspend fun saveWallpaperFromUri(
        context: Context,
        uri: Uri,
        sessionManager: FocusSessionManager
    ): Boolean = withContext(Dispatchers.IO) {
        var decoded: Bitmap? = null
        try {
            decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val (targetW, targetH) = computeScaledDimensions(
                        info.size.width,
                        info.size.height,
                        MAX_WALLPAPER_EDGE_PX
                    )
                    decoder.setTargetSize(targetW, targetH)
                }
            } else {
                decodeLegacyBitmap(context, uri)
            }
            if (decoded == null) return@withContext false

            val destFile = wallpaperFile(context)
            val tempFile = File(context.filesDir, "$WALLPAPER_FILE_NAME.tmp")
            FileOutputStream(tempFile).use { out ->
                decoded.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }
            sessionManager.markLauncherWallpaperUpdated(enabled = true)
            true
        } catch (_: Exception) {
            false
        } finally {
            decoded?.recycle()
        }
    }

    private fun decodeLegacyBitmap(context: Context, uri: Uri): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(
                boundsOpts.outWidth,
                boundsOpts.outHeight,
                MAX_WALLPAPER_EDGE_PX
            )
        }
        val sampled = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val (targetW, targetH) = computeScaledDimensions(
            sampled.width,
            sampled.height,
            MAX_WALLPAPER_EDGE_PX
        )
        if (targetW == sampled.width && targetH == sampled.height) return sampled
        val scaled = Bitmap.createScaledBitmap(sampled, targetW, targetH, true)
        if (scaled !== sampled) sampled.recycle()
        return scaled
    }

    suspend fun loadWallpaperBitmap(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val file = wallpaperFile(context)
            if (!file.exists() || file.length() == 0L) return@withContext null
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun clearCustomWallpaper(context: Context, sessionManager: FocusSessionManager) {
        try {
            val file = wallpaperFile(context)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
        sessionManager.clearLauncherWallpaper()
    }
}
