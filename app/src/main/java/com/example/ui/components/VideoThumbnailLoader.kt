package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy

/**
 * Small bounded in-memory cache for list thumbnails.
 * The cache is intentionally limited so scrolling cannot grow app memory without bound.
 */
object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 16).coerceIn(8 * 1024, 24 * 1024)

    val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    /**
     * Instantly retrieves a cached thumbnail for a video if already loaded anywhere in the app
     */
    fun getCachedThumbnail(videoId: Long, path: String): Bitmap? {
        memoryCache.get("${videoId}_${path}")?.let { if (!it.isRecycled) return it }
        if (path.isNotBlank()) {
            memoryCache.get(path)?.let { if (!it.isRecycled) return it }
        }
        if (videoId > 0) {
            memoryCache.get(videoId.toString())?.let { if (!it.isRecycled) return it }
        }
        try {
            val snap = memoryCache.snapshot()
            val prefix = "${videoId}_${path}"
            for ((k, bmp) in snap) {
                if (k.startsWith(prefix) && bmp != null && !bmp.isRecycled) {
                    return bmp
                }
            }
            if (path.isNotBlank()) {
                for ((k, bmp) in snap) {
                    if (k.contains(path) && bmp != null && !bmp.isRecycled) {
                        return bmp
                    }
                }
            }
        } catch (_: Throwable) {}
        return null
    }
}

/**
 * Dedicated bounded dispatcher for thumbnail generation so fast scrolling
 * never starves the CPU or UI thread, and queues do not accumulate stale tasks.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(3)

suspend fun loadVideoThumbnail(
    context: Context,
    videoId: Long,
    uri: Uri,
    path: String,
    strategy: ThumbnailStrategy = ThumbnailStrategy.SMART,
    quality: ThumbnailQuality = ThumbnailQuality.HIGH,
    allowNetwork: Boolean = false
): Bitmap? = withContext(thumbnailDispatcher) {
    val cacheKey = "${videoId}_${path}_${strategy.name}_${quality.name}_${allowNetwork}"
    ThumbnailCache.memoryCache.get(cacheKey)?.let { return@withContext it }
    ThumbnailCache.getCachedThumbnail(videoId, path)?.let { return@withContext it }

    if (!isActive) return@withContext null

    val targetWidth = quality.sizeDp.coerceIn(120, 720)
    val targetHeight = (targetWidth * 9f / 16f).roundToInt().coerceAtLeast(68)
    val isNetworkUri = uri.scheme == "http" || uri.scheme == "https"

    // Respect the experimental network-thumbnail switch.
    if (isNetworkUri && !allowNetwork) return@withContext null

    fun cache(bitmap: Bitmap): Bitmap {
        ThumbnailCache.memoryCache.put(cacheKey, bitmap)
        ThumbnailCache.memoryCache.put("${videoId}_${path}", bitmap)
        if (path.isNotBlank()) {
            ThumbnailCache.memoryCache.put(path, bitmap)
        }
        return bitmap
    }

    fun tryEmbedded(retriever: MediaMetadataRetriever): Bitmap? {
        return try {
            val bytes = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bmp ->
                if (bmp.width > targetWidth || bmp.height > targetHeight) {
                    val scaled = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
                    if (scaled !== bmp && !bmp.isRecycled) bmp.recycle()
                    scaled
                } else bmp
            }
        } catch (_: Throwable) { null }
    }

    // Embedded-only strategies should never silently generate a frame.
    if (strategy == ThumbnailStrategy.EMBEDDED || strategy == ThumbnailStrategy.EMBEDDED_AND_GENERATED) {
        try {
            val retriever = MediaMetadataRetriever()
            try {
                if (path.isNotBlank() && java.io.File(path).exists()) retriever.setDataSource(path)
                else if (uri.toString().isNotBlank()) retriever.setDataSource(context, uri)
                tryEmbedded(retriever)?.let { return@withContext cache(it) }
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        if (strategy == ThumbnailStrategy.EMBEDDED) return@withContext null
    }

    // Smart/Generated generation path. Smart first tries Android's native thumbnail API.
    if (strategy == ThumbnailStrategy.SMART) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.toString().isNotBlank() && !isNetworkUri) {
                val targetUri = if (uri.scheme == "content") uri else Uri.parse("content://media/external/video/media/$videoId")
                val bitmap = context.contentResolver.loadThumbnail(targetUri, Size(targetWidth, targetHeight), null)
                if (bitmap != null) return@withContext cache(bitmap)
            }
        } catch (_: Throwable) {}

        try {
            if (videoId > 0 && !isNetworkUri) {
                @Suppress("DEPRECATION")
                val thumb = android.provider.MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver, videoId, android.provider.MediaStore.Video.Thumbnails.MINI_KIND, null
                )
                if (thumb != null) return@withContext cache(thumb)
            }
        } catch (_: Throwable) {}
    }

    if (!isActive) return@withContext null

    // Generated keyframe path.
    try {
        val retriever = MediaMetadataRetriever()
        try {
            if (path.isNotBlank() && java.io.File(path).exists()) retriever.setDataSource(path)
            else if (uri.toString().isNotBlank()) retriever.setDataSource(context, uri)
            else return@withContext null

            if (!isActive) return@withContext null
            val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
                    ?: retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
                    ?: retriever.frameAtTime
            } else {
                retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            }

            if (frame != null) {
                val finalBitmap = if (frame.width > targetWidth || frame.height > targetHeight) {
                    val scaled = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
                    if (scaled !== frame && !frame.isRecycled) frame.recycle()
                    scaled
                } else frame
                return@withContext cache(finalBitmap)
            }
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    } catch (_: Throwable) {}

    null
}

private val FALLBACK_PALETTES = arrayOf(
    listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0284C7)),
    listOf(Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFF6366F1)),
    listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981)),
    listOf(Color(0xFF831843), Color(0xFFBE185D), Color(0xFFF43F5E)),
    listOf(Color(0xFF18181B), Color(0xFF3F3F46), Color(0xFF71717A)),
    listOf(Color(0xFF451A03), Color(0xFFB45309), Color(0xFFF59E0B))
)

/**
 * Video Thumbnail composable matching the exact requested UI:
 * - 16:9 rounded thumbnail container
 * - Red "NEW" badge on top-left
 * - Semi-transparent duration badge on bottom-right (e.g. "23:59")
 */
@Composable
fun VideoThumbnailCard(
    videoId: Long,
    videoUri: Uri,
    videoPath: String,
    durationText: String,
    isNew: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    customGradientSeed: Int = 0,
    showDuration: Boolean = true,
    showProgressBar: Boolean = true,
    watchedProgress: Float = 0f,
    isSelected: Boolean = false,
    isLastPlayed: Boolean = false,
    thumbnailStrategy: ThumbnailStrategy = ThumbnailStrategy.SMART,
    thumbnailQuality: ThumbnailQuality = ThumbnailQuality.HIGH,
    showNetworkThumbnails: Boolean = false,
    remoteThumbnailUrl: String? = null,
    onThumbnailClick: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val cacheKey = remember(videoId, videoPath, remoteThumbnailUrl) {
        if (!remoteThumbnailUrl.isNullOrBlank()) "remote_vid_${remoteThumbnailUrl}_400"
        else "${videoId}_$videoPath"
    }
    val initialCached = remember(cacheKey) { ThumbnailCache.memoryCache.get(cacheKey) }
    var thumbnailBitmap by remember(cacheKey) { mutableStateOf(initialCached) }

    LaunchedEffect(cacheKey) {
        if (thumbnailBitmap == null) {
            if (!remoteThumbnailUrl.isNullOrBlank()) {
                val bmp = loadRemoteAudioThumbnail(remoteThumbnailUrl, targetSizePx = 400)
                if (bmp != null) {
                    thumbnailBitmap = bmp
                }
            } else {
                val bmp = loadVideoThumbnail(
                    context, videoId, videoUri, videoPath,
                    strategy = thumbnailStrategy,
                    quality = thumbnailQuality,
                    allowNetwork = showNetworkThumbnails
                )
                if (bmp != null) {
                    thumbnailBitmap = bmp
                }
            }
        }
    }

    val paletteIndex = (customGradientSeed and 0x7FFFFFFF) % FALLBACK_PALETTES.size
    val fallbackColors = FALLBACK_PALETTES[paletteIndex]
    val shape = RoundedCornerShape(14.dp)

    val lastPlayedGradient = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF00E5FF),
                Color(0xFF6366F1),
                Color(0xFFEC4899),
                Color(0xFF00E5FF)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (onThumbnailClick != null) Modifier.clickable(onClick = onThumbnailClick) else Modifier)
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, com.example.ui.theme.AccentGradient, shape)
                } else if (isLastPlayed) {
                    Modifier.border(2.5.dp, lastPlayedGradient, shape)
                } else {
                    Modifier.border(
                        1.dp,
                        if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFCBD5E1).copy(alpha = 0.7f),
                        shape
                    )
                }
            )
    ) {
        // Thumbnail content (Image or artistic placeholder)
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(fallbackColors)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                                imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 1. Top-Left: Red "NEW" badge with white bold text (matching user screenshot)
        if (isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 6.dp, y = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE11D48)) // Vivid red matching screenshot
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "NEW",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            }
        }

        // 2. Bottom-Right: Duration badge (e.g. "23:59") with dark translucent background
        if (showDuration && durationText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = if (showProgressBar && watchedProgress > 0f) (-8).dp else (-6).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }

        // 3. Bottom Edge: Watched Progress Bar (Kitna aage tak dekha hai)
        if (showProgressBar && watchedProgress > 0f) {
            val clampedProgress = watchedProgress.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clampedProgress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFEF4444), Color(0xFFF97316))
                            )
                        )
                )
            }
        }
    }
}

/**
 * Lightweight Thumbnail Loader helper for simple path-based video thumbnails.
 */
@Composable
fun VideoThumbnailLoader(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val cacheKey = remember(videoPath) { "path_$videoPath" }
    var bitmap by remember(cacheKey) { mutableStateOf(ThumbnailCache.memoryCache.get(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bitmap == null && videoPath.isNotBlank()) {
            val bmp = loadVideoThumbnail(
                context = context,
                videoId = 0L,
                uri = Uri.EMPTY,
                path = videoPath
            )
            if (bmp != null) {
                bitmap = bmp
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

