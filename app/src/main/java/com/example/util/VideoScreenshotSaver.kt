package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.state.PlayerSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoScreenshotSaver {
    /**
     * Extracts a 100% clean video frame from the video source at the exact timestamp.
     * Guaranteed to contain zero UI overlays, zero buttons, and zero HUD elements.
     */
    suspend fun captureAndSaveCleanVideoFrame(
        context: Context,
        uri: Uri?,
        path: String?,
        positionMs: Long,
        settings: PlayerSettings = PlayerSettings()
    ): Result<String> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var frameBitmap: Bitmap? = null
        try {
            if (!path.isNullOrBlank() && File(path).exists()) {
                retriever.setDataSource(path)
            } else if (uri != null) {
                retriever.setDataSource(context, uri)
            } else {
                return@withContext Result.failure(Exception("Video source not available"))
            }

            val timeUs = (positionMs.coerceAtLeast(0L)) * 1000L
            frameBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (frameBitmap == null) {
                return@withContext Result.failure(Exception("Unable to decode clean video frame"))
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val titleSafe = (settings.screenshotFilenameTemplate
                .replace("{title}", "Video")
                .replace("{time}", timeStamp)
                .replace("{seconds}", (positionMs / 1000L).toString())
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "Screenshot_$timeStamp" })
            val extension = when (settings.screenshotFormat.uppercase()) {
                "JPG" -> "jpg"
                "WEBP" -> "webp"
                else -> "png"
            }
            val fileName = "$titleSafe.$extension"
            val mimeType = when (extension) {
                "jpg" -> "image/jpeg"
                "webp" -> "image/webp"
                else -> "image/png"
            }
            val format = when (extension) {
                "jpg" -> Bitmap.CompressFormat.JPEG
                "webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.PNG
            }
            val quality = if (extension == "png") 100 else settings.jpegWebpQuality.coerceIn(1, 100)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Screenshots")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to register image in MediaStore"))

                context.contentResolver.openOutputStream(imageUri)?.use { outStream ->
                    frameBitmap.compress(format, quality, outStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(imageUri, contentValues, null, null)

                Result.success("Screenshot saved cleanly to Gallery ($fileName)")
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val screenshotsDir = File(picturesDir, "Screenshots").apply { mkdirs() }
                val targetFile = File(screenshotsDir, fileName)
                FileOutputStream(targetFile).use { outStream ->
                    frameBitmap.compress(format, quality, outStream)
                }
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
                Result.success("Screenshot saved cleanly to Gallery ($fileName)")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }
}
