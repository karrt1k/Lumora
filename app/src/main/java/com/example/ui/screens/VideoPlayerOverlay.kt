package com.example.ui.screens

import com.example.R
import com.example.ai.TrackTranslationStatus
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.components.StyledIcon
import androidx.compose.runtime.rememberCoroutineScope

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.ui.components.AutoScrollingText
import com.example.ui.components.bounceClick
import com.example.ui.components.cardBounceClick
import com.example.ui.components.cardBounceCombinedClick
import com.example.ui.state.ControlsAnimationStyle
import com.example.ui.components.SubtitleSelectionPanel
import com.example.ui.components.AudioSelectionPanel
import com.example.ui.components.AudioDelayPanel
import com.example.ui.components.AudioEqualizerPanel
import com.example.ui.components.SubtitleDelayPanel
import com.example.ui.components.SubtitleStylePanel
import com.example.ui.components.ChapterSelectionPanel
import com.example.ui.components.MoreMenuSelectionPanel
import com.example.ui.components.SpeedSelectionPanel
import com.example.player.TrackAudioConfig
import com.example.player.AudioChannelMode
import com.example.player.VideoFilterPreset
import com.example.player.ManualVideoAdjustments
import com.example.player.ChapterSkipMarker
import com.example.player.ChapterSkipDetector
import com.example.ui.components.PlayerChapterSkipButton
import com.example.ui.components.VideoZoomSelectionPanel
import com.example.ui.components.VideoSettingSelectionPanel
import com.example.ui.components.ManualVideoAdjustmentPanel
import com.example.ui.components.AbLoopSelectionPanel
import com.example.ui.components.PlaylistSelectionPanel
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.state.PlayerControlId
import com.example.ui.state.PlayerLayoutConfig
import com.example.ui.state.PlayerSettings
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.util.AppHaptics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ==========================================
// ENUMS & DATA MODELS
// ==========================================

enum class PlayerPanelType {
    SUBTITLE_TRACK,
    SUBTITLE_DELAY,
    SUBTITLE_STYLE,
    AUDIO_TRACK,
    AUDIO_DELAY,
    EQUALIZER,
    CHAPTERS,
    MORE_MENU,
    SPEED,
    VIDEO_ZOOM,
    VIDEO_SETTING_FILTER,
    MANUAL_VIDEO_ADJUSTMENT,
    AB_LOOP,
    PLAYLIST,
    PLAYER_LAYOUT,
    VIDEO_QUALITY
}

enum class DecoderMode(val label: String) {
    HW_PLUS("HW+"),
    HW("HW"),
    SW("SW")
}

enum class PlayerRepeatMode {
    OFF, ONE, ALL
}

enum class AspectRatioMode(
    val modeKey: String,
    val label: String,
    val accessibilityLabel: String,
    val iconRes: Int
) {
    FIT("fit", "Fit", "Video display mode: Fit", R.drawable.lumora_minimize_square_2),
    FILL("fill", "Fill", "Video display mode: Fill", R.drawable.lumora_maximize_square_2),
    CROP("crop", "Zoom In", "Video display mode: Zoom In", R.drawable.lumora_maximize_square),
    ORIGINAL("original", "Full Screen", "Video display mode: Full Screen", R.drawable.lumora_full_screen_square);

    fun next(): AspectRatioMode = when (this) {
        FIT -> FILL
        FILL -> CROP
        CROP -> ORIGINAL
        ORIGINAL -> FIT
    }
}

// Visual color palette matching the screenshot:
// Dynamic glass transparency: 100% = Pure Solid White Image, Lower = Progressive Translucency
val LocalGlassBlurTransparency = compositionLocalOf { 85f }
val LocalPlayerSettings = compositionLocalOf { com.example.ui.state.PlayerSettings() }

/**
 * Clean floating time and network clock pill badge.
 * Displays real-time clock formatted according to timeNetworkClockFormat (System, 12h, or 24h),
 * along with network/WiFi and battery status.
 */
@Composable
fun TimeNetworkClockBadge(
    format: String,
    modifier: Modifier = Modifier
) {
    if (format == "NONE") return
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMs = System.currentTimeMillis()
            try {
                val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) {
                    batteryPercent = (level * 100) / scale
                }
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            } catch (_: Throwable) {}
            delay(5000)
        }
    }

    val formattedTime = remember(currentTimeMs, format) {
        try {
            val is24h = when (format) {
                "24_HOUR" -> true
                "12_HOUR" -> false
                else -> android.text.format.DateFormat.is24HourFormat(context)
            }
            val pattern = if (is24h) "HH:mm" else "hh:mm a"
            java.text.SimpleDateFormat(pattern, Locale.getDefault()).format(java.util.Date(currentTimeMs))
        } catch (_: Throwable) {
            ""
        }
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(getEffectiveGlassBg())
            .border(1.dp, getEffectiveGlassBorder(), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StyledIcon(imageVector = Icons.Outlined.Wifi,
                contentDescription = "Network",
                tint = GlassIconTint,
                modifier = Modifier.size(15.dp)
            )
            StyledIcon(imageVector = if (isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryStd,
                contentDescription = "Battery",
                tint = GlassIconTint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "$batteryPercent%",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = GlassIconTint
            )
            Text(
                text = "•",
                fontSize = 12.sp,
                color = GlassIconTint.copy(alpha = 0.5f)
            )
            Text(
                text = formattedTime,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = GlassIconTint
            )
        }
    }
}

/**
 * Opens the system-level Cast device picker so the user can discover and connect to
 * nearby Chromecast / Cast-enabled devices, exactly like the native "Cast" screen
 * used across Android system apps. Falls back gracefully with a notification when
 * no cast-capable settings screen is available on the device/ROM.
 */
fun openSystemCastPicker(context: Context, onUnavailable: (String) -> Unit) {
    val candidateActions = listOf(
        android.provider.Settings.ACTION_CAST_SETTINGS,
        "android.settings.WIFI_DISPLAY_SETTINGS"
    )
    for (action in candidateActions) {
        try {
            val intent = android.content.Intent(action)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Throwable) {
            // Try next fallback
        }
    }
    onUnavailable("No cast-capable devices found on this network")
}

@Composable
fun getEffectiveGlassBg(glassBg: Color? = null): Color {
    val settings = LocalPlayerSettings.current
    if (settings.hidePlayerButtonsBackground) return Color.Transparent
    if (glassBg != null) return glassBg
    val trans = LocalGlassBlurTransparency.current
    val isDark = if (settings.alwaysDarkPlayerButtonBackground) true else isAppInDarkTheme()
    return remember(trans, isDark, settings.alwaysDarkPlayerButtonBackground) {
        val factor = (trans / 100f).coerceIn(0.05f, 1.0f)
        val baseColor = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF)
        if (trans < 25f) {
            baseColor.copy(alpha = (factor * 0.45f).coerceIn(0.06f, 0.20f))
        } else {
            if (factor >= 0.99f) baseColor else baseColor.copy(alpha = factor)
        }
    }
}

@Composable
fun getEffectiveGlassBorder(glassBorder: Color? = null): Color {
    val settings = LocalPlayerSettings.current
    if (settings.hidePlayerButtonsBackground) return Color.Transparent
    if (glassBorder != null) return glassBorder
    val trans = LocalGlassBlurTransparency.current
    val isDark = if (settings.alwaysDarkPlayerButtonBackground) true else isAppInDarkTheme()
    return remember(trans, isDark, settings.alwaysDarkPlayerButtonBackground) {
        val factor = (trans / 100f).coerceIn(0.05f, 1.0f)
        if (isDark) {
            if (trans < 25f) {
                Color.White.copy(alpha = 0.22f)
            } else {
                if (factor >= 0.98f) Color(0xFF334155) else Color.White.copy(alpha = (factor * 0.35f).coerceIn(0.08f, 0.35f))
            }
        } else {
            if (trans < 25f) {
                Color(0xFFFFFFFF).copy(alpha = 0.38f)
            } else {
                if (factor >= 0.98f) Color(0xFFCBD5E1) else Color(0xFFFFFFFF).copy(alpha = (factor * 0.70f).coerceIn(0.08f, 0.90f))
            }
        }
    }
}

@Composable
fun getEffectiveGlassIconTint(customTint: Color? = null): Color {
    if (customTint != null) return customTint
    val settings = LocalPlayerSettings.current
    if (settings.hidePlayerButtonsBackground) return Color.White
    val trans = LocalGlassBlurTransparency.current
    val isDark = if (settings.alwaysDarkPlayerButtonBackground) true else isAppInDarkTheme()
    return remember(trans, isDark, settings.alwaysDarkPlayerButtonBackground, settings.hidePlayerButtonsBackground) {
        if (isDark) {
            Color(0xFFF8FAFC)
        } else {
            if (trans < 25f) Color(0xFFFFFFFF) else Color(0xFF0F172A)
        }
    }
}

val GlassIconTint: Color
    @Composable get() = getEffectiveGlassIconTint()

private enum class GestureLockMode {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    PINCH_ZOOM,
    HOLD_SPEED,
    SUBTITLE_DRAG
}

@Composable
fun VideoPlayerOverlay(
    videoTitle: String,
    currentEpisodeIndex: Int,
    totalEpisodes: Int,
    chapterTitle: String = "Chapter 1",
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit,
    hasPrevious: Boolean = true,
    hasNext: Boolean = true,
    onBack: () -> Unit,
    playbackSpeed: Double = 1.0,
    onSpeedChange: (Double) -> Unit = {},
    decoderMode: DecoderMode = DecoderMode.HW_PLUS,
    onDecoderModeChange: (DecoderMode) -> Unit = {},
    repeatMode: PlayerRepeatMode = PlayerRepeatMode.OFF,
    onRepeatModeChange: (PlayerRepeatMode) -> Unit = {},
    isShuffle: Boolean = false,
    onShuffleChange: (Boolean) -> Unit = {},
    isAudioOnly: Boolean = false,
    onAudioOnlyChange: (Boolean) -> Unit = {},
    aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    onAspectRatioChange: (AspectRatioMode) -> Unit = {},
    loopPointA: Long? = null,
    loopPointB: Long? = null,
    onSetLoopPointA: (Long) -> Unit = {},
    onSetLoopPointB: (Long) -> Unit = {},
    onClearLoop: () -> Unit = {},
    brightness: Float = 0.5f,
    onBrightnessChange: (Float) -> Unit = {},
    volumeBoostCap: Int = 150,
    onVolumeBoostChange: (Int) -> Unit = {},
    contrast: Float = 1.0f,
    onContrastChange: (Float) -> Unit = {},
    saturation: Float = 1.0f,
    onSaturationChange: (Float) -> Unit = {},
    videoFilterPreset: VideoFilterPreset = VideoFilterPreset.NONE,
    onSelectVideoFilterPreset: (VideoFilterPreset) -> Unit = {},
    manualVideoAdjustments: ManualVideoAdjustments = ManualVideoAdjustments(),
    onManualVideoAdjustmentsChange: (ManualVideoAdjustments) -> Unit = {},
    selectedAudioTrackId: Int = 1,
    onSelectAudioTrack: (Int) -> Unit = {},
    trackAudioConfigs: Map<Int, TrackAudioConfig> = emptyMap(),
    onTrackChannelModeChange: (trackId: Int, mode: AudioChannelMode) -> Unit = { _, _ -> },
    onTrackVolumeNormalizationChange: (trackId: Int, enabled: Boolean) -> Unit = { _, _ -> },
    onTrackDynamicRangeCompressionChange: (trackId: Int, enabled: Boolean) -> Unit = { _, _ -> },
    selectedSubtitleTrackId: Int = 1,
    secondarySubtitleTrackId: Int = 0,
    lastTouchedSubtitleTrackId: Int = 0,
    onSelectSubtitleTrack: (Int) -> Unit = {},
    onSelectSecondarySubtitleTrack: (Int) -> Unit = {},
    onLastTouchedSubtitleTrackIdChange: ((Int) -> Unit)? = null,
    isSubtitleVisible: Boolean = true,
    onToggleSubtitleVisibility: () -> Unit = {},
    subtitleOffsetMs: Long = 0L,
    onSubtitleOffsetChange: (Long) -> Unit = {},
    audioDelayMs: Long = 0L,
    onAudioDelayChange: (Long) -> Unit = {},
    equalizerEnabled: Boolean = false,
    onToggleEqualizer: (Boolean) -> Unit = {},
    eq60Hz: Float = 0f,
    onEq60HzChange: (Float) -> Unit = {},
    eq230Hz: Float = 0f,
    onEq230HzChange: (Float) -> Unit = {},
    eq910Hz: Float = 0f,
    onEq910HzChange: (Float) -> Unit = {},
    eq3600Hz: Float = 0f,
    onEq3600HzChange: (Float) -> Unit = {},
    eq14000Hz: Float = 0f,
    onEq14000HzChange: (Float) -> Unit = {},
    volumeBoostDb: Float = 0f,
    onVolumeBoostDbChange: (Float) -> Unit = {},
    equalizerPreset: String = "Flat",
    onSelectEqualizerPreset: (com.example.player.EqualizerPreset) -> Unit = {},
    onResetEqualizer: () -> Unit = {},
    chapters: List<com.example.player.PlayerVideoChapter> = emptyList(),
    onSeekToChapter: (com.example.player.PlayerVideoChapter) -> Unit = {},
    skipMarkers: List<com.example.player.ChapterSkipMarker> = emptyList(),
    onSkipToPosition: (Long) -> Unit = onSeekTo,
    audioTracks: List<com.example.player.PlayerMediaTrack> = emptyList(),
    subtitleTracks: List<com.example.player.PlayerMediaTrack> = emptyList(),
    availableVideoQualities: List<String> = emptyList(),
    selectedVideoQuality: String? = null,
    onSelectVideoQuality: ((String) -> Unit)? = null,
    onRemoveSubtitleTrack: (Int) -> Unit = {},
    onRemoveAudioTrack: (Int) -> Unit = {},
    playlistVideos: List<String> = emptyList(),
    playlistItems: List<com.example.ui.screens.VideoItem> = emptyList(),
    onSelectPlaylistItem: (Int) -> Unit = {},
    onReorderPlaylist: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onImportSubtitle: () -> Unit = {},
    onImportAudio: () -> Unit = {},
    onSelectSubtitleFile: ((java.io.File) -> Unit)? = null,
    translationStatusByTrackId: Map<Int, TrackTranslationStatus> = emptyMap(),
    onStartTranslate: (Int) -> Unit = {},
    onTranslateSubtitleTrack: suspend (Int, (Int) -> Unit) -> Result<Int> = { _, _ -> Result.failure(IllegalStateException("Subtitle translation is not connected")) },
    onTakeScreenshot: () -> Unit = {},
    onTogglePiP: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    isLandscape: Boolean = true,
    glassBlurTransparency: Float = 85f,
    videoWidth: Int = 0,
    videoHeight: Int = 0,
    videoZoomScale: Float = 1.0f,
    onVideoZoomChange: (Float) -> Unit = {},
    isVideoFlippedVertically: Boolean = false,
    isVideoMirrored: Boolean = false,
    onToggleVideoFlipVertical: () -> Unit = {},
    onMirrorVideoRight: () -> Unit = {},
    onMirrorVideoLeft: () -> Unit = {},
    isPanAndZoomEnabled: Boolean = false,
    onTogglePanAndZoom: (Boolean) -> Unit = {},
    onResetZoomAndPan: () -> Unit = {},
    isPitchCorrectionEnabled: Boolean = true,
    onTogglePitchCorrection: (Boolean) -> Unit = {},
    gestureSensitivityMode: GestureSensitivityMode = GestureSensitivityMode.EXPONENTIAL,
    onGestureSensitivityModeChange: ((GestureSensitivityMode) -> Unit)? = null,
    seekbarStyle: com.example.ui.state.SeekbarStyle = com.example.ui.state.SeekbarStyle.STANDARD,
    onSeekbarStyleChange: ((com.example.ui.state.SeekbarStyle) -> Unit)? = null,
    playerLayoutConfig: PlayerLayoutConfig = PlayerLayoutConfig.default,
    onPlayerLayoutConfigChange: ((PlayerLayoutConfig) -> Unit)? = null,
    onResetPlayerLayoutConfig: (() -> Unit)? = null,
    showPlayerNotifications: Boolean = true,
    controlsAnimationStyle: ControlsAnimationStyle = ControlsAnimationStyle.DEFAULT,
    animationSpeed: Float = 1.0f,
    enableDoubleTap: Boolean = true,
    doubleTapSeekSeconds: Int = 10,
    doubleTapSeekAreaWidthPercent: Int = 35,
    doubleTapLeftAction: String = "SEEK_BACK",
    doubleTapCenterAction: String = "PLAY_PAUSE",
    doubleTapRightAction: String = "SEEK_FORWARD",
    enableSingleTap: Boolean = false,
    singleTapSeekSeconds: Int = 10,
    singleTapSeekAreaWidthPercent: Int = 35,
    singleTapLeftAction: String = "SEEK_BACK",
    singleTapCenterAction: String = "PLAY_PAUSE",
    singleTapRightAction: String = "SEEK_FORWARD",
    singleTapCenterGesture: Boolean = false,
    brightnessGesturesEnabled: Boolean = true,
    volumeGesturesEnabled: Boolean = true,
    swapVolumeBrightnessGestures: Boolean = false,
    pinchToZoomEnabled: Boolean = true,
    pinchToZoomSubtitlesEnabled: Boolean = true,
    swipeSubtitlesToSeekDialogEnabled: Boolean = true,
    horizontalSwipeToSeekEnabled: Boolean = true,
    swipeUpCenterForPlaylistEnabled: Boolean = true,
    horizontalSwipeSensitivity: Int = 10,
    holdMultiSpeedEnabled: Boolean = true,
    holdSpeedMultiplier: Double = 2.0,
    dynamicSpeedOverlayEnabled: Boolean = true,
    holdDragMovesSubtitlesEnabled: Boolean = true,
    mediaPreviousControlEnabled: Boolean = true,
    mediaPlayPauseControlEnabled: Boolean = true,
    mediaNextControlEnabled: Boolean = true,
    subtitleScale: Float = 1.0f,
    subtitlePosition: Float = 100f,
    onSubtitleScaleChange: (Float) -> Unit = {},
    onSubtitlePositionChange: (Float) -> Unit = {},
    showDoubleTapSeekFeedback: Boolean = true,
    showVolumeSliderOverlay: Boolean = true,
    showBrightnessSliderOverlay: Boolean = true,
    showHoldSpeedOverlay: Boolean = true,
    showAspectRatioFeedback: Boolean = true,
    showZoomLevelFeedback: Boolean = true,
    showRepeatShuffleFeedback: Boolean = true,
    showActionFeedbackPills: Boolean = true,
    playerSettings: com.example.ui.state.PlayerSettings = com.example.ui.state.PlayerSettings(),
    subtitleSettings: com.example.ui.state.PlayerSettings = playerSettings,
    onSubtitleSettingsChange: (com.example.ui.state.PlayerSettings) -> Unit = {},
    onLoadAdvancedAssSource: (suspend (Int) -> com.example.player.AdvancedAssSource?)? = null,
    externalNotification: String? = null,
    externalNotificationKey: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8)
    }

    // Auto-hide controls timer
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Persistent scroll states for control sliders across hide/show cycles
    val portraitTopScrollState = rememberScrollState()
    val landscapeTopScrollState = rememberScrollState()
    val portraitBottomScrollState = rememberScrollState()
    val landscapeBottomScrollState = rememberScrollState()

    // Picture-in-Picture mode: hide all control chrome and switch to a plain
    // tap-to-play/pause surface for the duration of the PiP session.
    val isInPictureInPictureMode by com.example.util.PipModeState.isInPictureInPicture
    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            areControlsVisible = false
        }
    }

    // Screen Lock
    var isScreenLocked by remember { mutableStateOf(false) }

    // Player Action Notification Overlay State
    var playerNotificationMessage by remember { mutableStateOf<String?>(null) }
    var playerNotificationKey by remember { mutableLongStateOf(0L) }

    val notifyAction: (String) -> Unit = { msg ->
        if (showPlayerNotifications && msg.isNotBlank()) {
            playerNotificationMessage = msg
            playerNotificationKey = System.currentTimeMillis()
        }
    }

    LaunchedEffect(externalNotificationKey) {
        if (externalNotificationKey > 0L && !externalNotification.isNullOrBlank() && showPlayerNotifications) {
            playerNotificationMessage = externalNotification
            playerNotificationKey = System.currentTimeMillis()
        }
    }

    LaunchedEffect(playerNotificationKey) {
        if (playerNotificationKey > 0L && playerNotificationMessage != null) {
            delay(1800L)
            playerNotificationMessage = null
        }
    }

    // Active Dialogs & Sheets (Strict Hierarchy Stack)
    val panelStack = remember { mutableStateListOf<PlayerPanelType>() }
    val activePanel = panelStack.lastOrNull()
    val anyDialogOpen = panelStack.isNotEmpty()

    fun openPanel(panel: PlayerPanelType) {
        if (panelStack.lastOrNull() != panel) {
            panelStack.add(panel)
        }
    }

    fun openRootPanel(panel: PlayerPanelType) {
        panelStack.clear()
        panelStack.add(panel)
    }

    fun dismissCurrentPanel() {
        if (panelStack.isNotEmpty()) {
            panelStack.removeAt(panelStack.lastIndex)
        }
    }

    BackHandler(enabled = anyDialogOpen) {
        dismissCurrentPanel()
    }

    var isAbInlineExpanded by remember { mutableStateOf(false) }
    var isCameraMorphExpanded by remember { mutableStateOf(false) }

    // HUD Gestures Feedback
    var showBrightnessHud by remember { mutableStateOf(false) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var isAdjustingBrightness by remember { mutableStateOf(false) }
    var isAdjustingVolume by remember { mutableStateOf(false) }
    var hudLastInteractedTime by remember { mutableLongStateOf(0L) }
    var hudBrightnessValue by remember { mutableFloatStateOf(brightness) }
    var hudVolumePercent by remember {
        mutableFloatStateOf((currentVolume.toFloat() / maxVolume.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f))
    }

    // Double Tap / Single Tap seek animation
    var currentSeekFeedbackSeconds by remember { mutableIntStateOf(doubleTapSeekSeconds) }
    var showDoubleTapSeekLeft by remember { mutableStateOf(false) }
    var showDoubleTapSeekRight by remember { mutableStateOf(false) }
    var isHoldSpeedActive by remember { mutableStateOf(false) }
    var holdOriginalSpeed by remember { mutableStateOf(playbackSpeed) }
    var holdSpeedValue by remember { mutableStateOf(holdSpeedMultiplier) }

    // Render-thread animation states (zero UI stutter, reads deferred to graphicsLayer)
    val effectiveControlsVisible = areControlsVisible && !anyDialogOpen
    val controlsAlpha = animateFloatAsState(
        targetValue = if (effectiveControlsVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (effectiveControlsVisible) 180 else 220,
            easing = FastOutSlowInEasing
        ),
        label = "ControlsAlpha"
    )
    val controlsSlideY = animateFloatAsState(
        targetValue = if (effectiveControlsVisible) 0f else 18f,
        animationSpec = tween(
            durationMillis = if (effectiveControlsVisible) 180 else 220,
            easing = FastOutSlowInEasing
        ),
        label = "ControlsSlideY"
    )
    val doubleTapLeftAlpha = animateFloatAsState(
        targetValue = if (showDoubleTapSeekFeedback && showDoubleTapSeekLeft) 1f else 0f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "DoubleTapLeftAlpha"
    )
    val doubleTapRightAlpha = animateFloatAsState(
        targetValue = if (showDoubleTapSeekFeedback && showDoubleTapSeekRight) 1f else 0f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "DoubleTapRightAlpha"
    )
    val brightnessHudAlpha = animateFloatAsState(
        targetValue = if (showBrightnessSliderOverlay && showBrightnessHud) 1f else 0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "BrightnessHudAlpha"
    )
    val volumeHudAlpha = animateFloatAsState(
        targetValue = if (showVolumeSliderOverlay && showVolumeHud) 1f else 0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "VolumeHudAlpha"
    )
    val hudBrightnessPercent by remember(hudBrightnessValue) {
        derivedStateOf { (hudBrightnessValue * 100).roundToInt() }
    }
    val hudVolumePercentInt by remember(hudVolumePercent) {
        derivedStateOf { (hudVolumePercent * 100).roundToInt() }
    }

    var showSwipeSeekHud by remember { mutableStateOf(false) }
    var swipeSeekTargetMs by remember { mutableLongStateOf(0L) }
    var swipeSeekDeltaMs by remember { mutableLongStateOf(0L) }
    var swipeSeekLastInteractionTime by remember { mutableLongStateOf(0L) }

    fun notifyInteraction() {
        lastInteractionTime = System.currentTimeMillis()
    }

    val handlePlayPauseToggle: () -> Unit = {
        notifyInteraction()
        onPlayPauseToggle()
    }

    // Active Chapter calculation with derivedStateOf
    val currentChapter by remember(chapters, currentPositionMs) {
        derivedStateOf {
            chapters.lastOrNull { it.timeMs <= currentPositionMs } ?: chapters.firstOrNull()
        }
    }
    val chapterDisplayLabel by remember(currentChapter, chapters) {
        derivedStateOf {
            val ch = currentChapter
            if (ch != null && chapters.isNotEmpty()) {
                val title = if (ch.title.isNotBlank()) ch.title else "Chapter ${ch.index + 1}"
                "Ch ${ch.index + 1} • $title"
            } else {
                "Chapters"
            }
        }
    }

    // Active Skip Marker calculation (Opening/Ending/Preview/Recap)
    val activeSkipMarker = remember(skipMarkers, currentPositionMs / 500L) {
        ChapterSkipDetector.getActiveMarker(skipMarkers, currentPositionMs)
    }

    // Next Episode calculation & state removed per user fix request

    // Auto-hide ticker (configured inactivity timeout when playing)
    val hideTimeoutMs = playerSettings.hidePlayerControlsTimeoutMs.toLong()
    LaunchedEffect(
        areControlsVisible,
        lastInteractionTime,
        isPlaying,
        isScreenLocked,
        anyDialogOpen,
        isCameraMorphExpanded,
        isAbInlineExpanded,
        hideTimeoutMs
    ) {
        if (areControlsVisible && isPlaying && !isScreenLocked && !anyDialogOpen && !isCameraMorphExpanded && !isAbInlineExpanded) {
            while (isActive) {
                delay(250)
                if (System.currentTimeMillis() - lastInteractionTime > hideTimeoutMs) {
                    areControlsVisible = false
                }
            }
        }
    }

    // Latest state holders for gesture processor to prevent recomposition cancellations
    val updatedBrightness by rememberUpdatedState(brightness)
    val updatedOnBrightnessChange by rememberUpdatedState(onBrightnessChange)
    val updatedOnVolumeBoostChange by rememberUpdatedState(onVolumeBoostChange)
    val updatedOnSeekTo by rememberUpdatedState(onSeekTo)
    val updatedCurrentPositionMs by rememberUpdatedState(currentPositionMs)
    val updatedDurationMs by rememberUpdatedState(durationMs)
    val updatedIsScreenLocked by rememberUpdatedState(isScreenLocked)
    val updatedAnyDialogOpen by rememberUpdatedState(anyDialogOpen)
    val updatedVideoWidth by rememberUpdatedState(videoWidth)
    val updatedVideoHeight by rememberUpdatedState(videoHeight)
    val updatedAspectRatioMode by rememberUpdatedState(aspectRatioMode)
    val updatedPlaybackSpeed by rememberUpdatedState(playbackSpeed)
    val updatedOnSpeedChange by rememberUpdatedState(onSpeedChange)
    val updatedVideoZoomScale by rememberUpdatedState(videoZoomScale)
    val updatedOnVideoZoomChange by rememberUpdatedState(onVideoZoomChange)
    val updatedSubtitleScale by rememberUpdatedState(subtitleScale)
    val updatedOnSubtitleScaleChange by rememberUpdatedState(onSubtitleScaleChange)
    val updatedSubtitlePosition by rememberUpdatedState(subtitlePosition)
    val updatedOnSubtitlePositionChange by rememberUpdatedState(onSubtitlePositionChange)

    CompositionLocalProvider(
        LocalGlassBlurTransparency provides glassBlurTransparency,
        LocalPlayerSettings provides playerSettings
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // -------------------------------------------------------------
            // DEDICATED BACKGROUND GESTURE SURFACE
            // Sits behind controls; unconsumed touches on the video surface
            // trigger swipe/brightness/volume/seek gestures, double-tap, and single-tap.
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        enableDoubleTap, doubleTapSeekSeconds, doubleTapSeekAreaWidthPercent, doubleTapLeftAction,
                        doubleTapCenterAction, doubleTapRightAction, enableSingleTap, singleTapSeekSeconds,
                        singleTapSeekAreaWidthPercent, singleTapLeftAction, singleTapCenterAction, singleTapRightAction,
                        singleTapCenterGesture, brightnessGesturesEnabled, volumeGesturesEnabled, swapVolumeBrightnessGestures,
                        pinchToZoomEnabled, pinchToZoomSubtitlesEnabled, horizontalSwipeToSeekEnabled,
                        swipeUpCenterForPlaylistEnabled, horizontalSwipeSensitivity, holdMultiSpeedEnabled,
                        holdSpeedMultiplier, holdDragMovesSubtitlesEnabled
                    ) {
                        var lastTapTimestamp = 0L
                        var lastTapX = 0f
                        var lastTapY = 0f

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            if (updatedAnyDialogOpen) return@awaitEachGesture

                            if (updatedIsScreenLocked) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) {
                                        if (!change.isConsumed) {
                                            areControlsVisible = true
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                        break
                                    }
                                }
                                return@awaitEachGesture
                            }

                            val startPos = down.position
                            val startTime = System.currentTimeMillis()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val curVW = updatedVideoWidth
                            val curVH = updatedVideoHeight
                            val videoAspect = if (curVW > 0 && curVH > 0) curVW.toFloat() / curVH.toFloat() else 16f / 9f
                            val curAsp = updatedAspectRatioMode
                            val videoRect: Rect = when (curAsp) {
                                AspectRatioMode.FIT -> {
                                    val containerAspect = w / h
                                    if (videoAspect > containerAspect) {
                                        val renderedH = w / videoAspect
                                        val top = (h - renderedH) / 2f
                                        Rect(0f, top, w, top + renderedH)
                                    } else {
                                        val renderedW = h * videoAspect
                                        val left = (w - renderedW) / 2f
                                        Rect(left, 0f, left + renderedW, h)
                                    }
                                }
                                AspectRatioMode.ORIGINAL -> {
                                    val renderedW = if (curVW > 0) curVW.toFloat().coerceAtMost(w) else w
                                    val renderedH = if (curVH > 0) curVH.toFloat().coerceAtMost(h) else h
                                    val left = (w - renderedW) / 2f
                                    val top = (h - renderedH) / 2f
                                    Rect(left, top, left + renderedW, top + renderedH)
                                }
                                else -> Rect(0f, 0f, w, h)
                            }

                            val isStartInsideVideo = startPos.x in videoRect.left..videoRect.right && startPos.y in videoRect.top..videoRect.bottom
                            val leftBoundary = videoRect.left + videoRect.width * 0.35f
                            val rightBoundary = videoRect.right - videoRect.width * 0.35f
                            val isStartInLeft = startPos.x < leftBoundary
                            val isStartInRight = startPos.x > rightBoundary
                            val isStartInCenter = startPos.x in leftBoundary..rightBoundary

                            val initialBrightness = hudBrightnessValue
                            val initialVolumePercent = hudVolumePercent
                            val initialSeekPosition = updatedCurrentPositionMs
                            var totalDragY = 0f
                            var totalDragX = 0f
                            var lockMode = GestureLockMode.NONE
                            var lastHapticSeekSec = -1L
                            var lastHapticVolumeStep = -1
                            var lastHapticBrightnessStep = -1
                            var isPinching = false
                            var pinchInitialDistance = 0f
                            var pinchInitialVideoZoom = updatedVideoZoomScale
                            var pinchInitialSubtitleScale = updatedSubtitleScale
                            var holdJob: Job? = null

                            if (holdMultiSpeedEnabled && isStartInsideVideo) {
                                holdJob = coroutineScope.launch {
                                    delay(450L)
                                    if (lockMode == GestureLockMode.NONE && !updatedIsScreenLocked) {
                                        lockMode = GestureLockMode.HOLD_SPEED
                                        holdOriginalSpeed = updatedPlaybackSpeed
                                        holdSpeedValue = holdSpeedMultiplier.coerceIn(1.1, 4.0)
                                        isHoldSpeedActive = true
                                        updatedOnSpeedChange(holdSpeedValue)
                                        if (dynamicSpeedOverlayEnabled) notifyAction("${String.format(java.util.Locale.US, "%.2f", holdSpeedValue)}x")
                                    }
                                }
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val changes = event.changes
                                    val change = changes.firstOrNull { it.id == down.id } ?: break

                                    if (changes.count { it.pressed } >= 2 && pinchToZoomEnabled && isStartInsideVideo) {
                                        holdJob?.cancel()
                                        lockMode = GestureLockMode.PINCH_ZOOM
                                        val active = changes.filter { it.pressed }.take(2)
                                        val p1 = active[0].position
                                        val p2 = active[1].position
                                        val distance = kotlin.math.hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()).toFloat().coerceAtLeast(1f)
                                        if (!isPinching) {
                                            isPinching = true
                                            pinchInitialDistance = distance
                                            pinchInitialVideoZoom = updatedVideoZoomScale
                                            pinchInitialSubtitleScale = updatedSubtitleScale
                                        }
                                        val ratio = (distance / pinchInitialDistance).coerceIn(0.5f, 3.0f)
                                        if (pinchToZoomSubtitlesEnabled && startPos.y > h * 0.62f) {
                                            updatedOnSubtitleScaleChange((pinchInitialSubtitleScale * ratio).coerceIn(0.5f, 3.0f))
                                        } else {
                                            updatedOnVideoZoomChange((pinchInitialVideoZoom * ratio).coerceIn(1.0f, 4.0f))
                                        }
                                        changes.forEach { it.consume() }
                                        continue
                                    }

                                    if (!change.pressed) {
                                        holdJob?.cancel()
                                        if (isHoldSpeedActive) {
                                            updatedOnSpeedChange(holdOriginalSpeed)
                                            isHoldSpeedActive = false
                                        }
                                        isAdjustingBrightness = false
                                        isAdjustingVolume = false
                                        if (lockMode != GestureLockMode.NONE) {
                                            hudLastInteractedTime = System.currentTimeMillis()
                                        } else if (!change.isConsumed) {
                                            // Tap or Double-Tap processing
                                            val elapsed = System.currentTimeMillis() - startTime
                                            val dx = kotlin.math.abs(change.position.x - startPos.x)
                                            val dy = kotlin.math.abs(change.position.y - startPos.y)

                                             if (elapsed < 350 && dx < 24f && dy < 24f) {
                                                val now = System.currentTimeMillis()
                                                val timeDiff = now - lastTapTimestamp
                                                val distX = kotlin.math.abs(change.position.x - lastTapX)
                                                val distY = kotlin.math.abs(change.position.y - lastTapY)
                                                val isDoubleTap = enableDoubleTap && (timeDiff in 30..280) && distX < 80f && distY < 80f

                                                if (isDoubleTap) {
                                                    lastTapTimestamp = 0L
                                                    val doubleTapLeftBoundary = w * (doubleTapSeekAreaWidthPercent.coerceIn(10, 50) / 100f)
                                                    val doubleTapRightBoundary = w - doubleTapLeftBoundary
                                                    val tapX = change.position.x

                                                    // Double tap in center / screen -> Seek 10 seconds forward/backward (DO NOT show any controls)
                                                    val isSeekBack = if (tapX < doubleTapLeftBoundary) {
                                                        doubleTapLeftAction == "SEEK_BACK"
                                                    } else if (tapX > doubleTapRightBoundary) {
                                                        doubleTapRightAction == "SEEK_BACK"
                                                    } else {
                                                        tapX < (w / 2f)
                                                    }

                                                    val seekSec = if (doubleTapSeekSeconds > 0) doubleTapSeekSeconds else 10
                                                    AppHaptics.performGestureThreshold(view, haptic)
                                                    currentSeekFeedbackSeconds = seekSec
                                                    if (isSeekBack) {
                                                        updatedOnSeekTo((updatedCurrentPositionMs - seekSec * 1000L).coerceAtLeast(0L))
                                                        showDoubleTapSeekLeft = true
                                                    } else {
                                                        updatedOnSeekTo((updatedCurrentPositionMs + seekSec * 1000L).coerceAtMost(updatedDurationMs))
                                                        showDoubleTapSeekRight = true
                                                    }
                                                    if (areControlsVisible) {
                                                        lastInteractionTime = now
                                                    }
                                                } else {
                                                    lastTapTimestamp = now
                                                    lastTapX = change.position.x
                                                    lastTapY = change.position.y

                                                    val tapX = change.position.x
                                                    val tapY = change.position.y

                                                    // Calculate coordinate bounds for designated control zones vs center 40%
                                                    val isOutsideVideo = tapX < videoRect.left || tapX > videoRect.right || tapY < videoRect.top || tapY > videoRect.bottom
                                                    val isInTopZone = tapY < (h * 0.30f)
                                                    val isInBottomZone = tapY > (h * 0.70f)
                                                    val isLeftRightBlackBar = tapX < videoRect.left || tapX > videoRect.right

                                                    val isDesignatedControlZone = isInTopZone || isInBottomZone || isLeftRightBlackBar || isOutsideVideo
                                                    val isInCenterArea = !isDesignatedControlZone // Middle 40% area inside video

                                                    if (isInCenterArea) {
                                                        // Single tap in center -> Toggle Play/Pause. DO NOT show any controls.
                                                        AppHaptics.performGestureThreshold(view, haptic)
                                                        onPlayPauseToggle()
                                                        if (areControlsVisible) {
                                                            lastInteractionTime = now
                                                        }
                                                    } else if (isDesignatedControlZone) {
                                                        // Controls toggle (show/hide) ONLY on designated areas (Top 30%, Bottom 30%, or Left/Right black bars)
                                                        areControlsVisible = !areControlsVisible
                                                        if (areControlsVisible) {
                                                            lastInteractionTime = now
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        break
                                    }

                                    val dragDeltaX = change.position.x - change.previousPosition.x
                                    val dragDeltaY = change.position.y - change.previousPosition.y
                                    totalDragX += dragDeltaX
                                    totalDragY += dragDeltaY
                                    val absX = kotlin.math.abs(totalDragX)
                                    val absY = kotlin.math.abs(totalDragY)

                                    // Directional and zone locking on initial move
                                    if (lockMode == GestureLockMode.NONE && isStartInsideVideo && (absX > 12f || absY > 12f)) {
                                        holdJob?.cancel()
                                        if (isStartInLeft) {
                                            if (absY >= absX) {
                                                lockMode = if (swapVolumeBrightnessGestures) GestureLockMode.VOLUME else GestureLockMode.BRIGHTNESS
                                                AppHaptics.performGestureThreshold(view, haptic)
                                            } else if (horizontalSwipeToSeekEnabled && absX > absY * 1.3f) {
                                                lockMode = GestureLockMode.SEEK
                                                lastHapticSeekSec = initialSeekPosition / 1000L
                                                AppHaptics.performGestureThreshold(view, haptic)
                                            }
                                        } else if (isStartInRight) {
                                            if (absY >= absX) {
                                                lockMode = if (swapVolumeBrightnessGestures) GestureLockMode.BRIGHTNESS else GestureLockMode.VOLUME
                                                AppHaptics.performGestureThreshold(view, haptic)
                                            } else if (horizontalSwipeToSeekEnabled && absX > absY * 1.3f) {
                                                lockMode = GestureLockMode.SEEK
                                                lastHapticSeekSec = initialSeekPosition / 1000L
                                                AppHaptics.performGestureThreshold(view, haptic)
                                            }
                                        } else {
                                            // Center zone
                                            if (holdDragMovesSubtitlesEnabled && System.currentTimeMillis() - startTime >= 400L && absY > absX) {
                                                lockMode = GestureLockMode.SUBTITLE_DRAG
                                                AppHaptics.performGestureThreshold(view, haptic)
                                            } else if (horizontalSwipeToSeekEnabled && absX > absY * 1.15f) {
                                                lockMode = GestureLockMode.SEEK
                                                lastHapticSeekSec = initialSeekPosition / 1000L
                                                AppHaptics.performGestureThreshold(view, haptic)
                                            } else if (swipeUpCenterForPlaylistEnabled && totalDragY < -80f && absY > absX * 1.2f) {
                                                AppHaptics.performGestureThreshold(view, haptic)
                                                openRootPanel(PlayerPanelType.PLAYLIST)
                                                notifyInteraction()
                                                change.consume()
                                                break
                                            }
                                        }
                                    }

                                    // Execute strictly by locked mode to prevent any gesture interference or cross-switching
                                    when (lockMode) {
                                        GestureLockMode.BRIGHTNESS -> {
                                            if (brightnessGesturesEnabled) {
                                                isAdjustingBrightness = true
                                                isAdjustingVolume = false
                                                val compHeight = h.coerceAtLeast(200f)
                                                val delta = -totalDragY / (compHeight * 0.70f)
                                                val newB = (initialBrightness + delta).coerceIn(0.01f, 1.0f)
                                                hudBrightnessValue = newB
                                                val bStep = (newB * 20f).toInt()
                                                if (bStep != lastHapticBrightnessStep) {
                                                    lastHapticBrightnessStep = bStep
                                                    AppHaptics.performTick(view, haptic)
                                                }
                                                updatedOnBrightnessChange(newB)
                                                showBrightnessHud = true
                                                showVolumeHud = false
                                                hudLastInteractedTime = System.currentTimeMillis()
                                                change.consume()
                                            }
                                        }
                                        GestureLockMode.VOLUME -> {
                                            if (volumeGesturesEnabled) {
                                                isAdjustingVolume = true
                                                isAdjustingBrightness = false
                                                val compHeight = h.coerceAtLeast(200f)
                                                val delta = -totalDragY / (compHeight * 0.70f)
                                                val maxRatio = (volumeBoostCap / 100f).coerceIn(1.0f, 2.0f)
                                                val newVolRatio = (initialVolumePercent + delta).coerceIn(0f, maxRatio)
                                                hudVolumePercent = newVolRatio
                                                var volChanged = false
                                                if (newVolRatio <= 1.0f) {
                                                    val newVol = (newVolRatio * maxVolume).roundToInt().coerceIn(0, maxVolume)
                                                    if (newVol != currentVolume) {
                                                        currentVolume = newVol
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                                                        volChanged = true
                                                    }
                                                    updatedOnVolumeBoostChange(100)
                                                } else {
                                                    if (currentVolume != maxVolume) {
                                                        currentVolume = maxVolume
                                                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
                                                        volChanged = true
                                                    }
                                                    updatedOnVolumeBoostChange((newVolRatio * 100).roundToInt().coerceIn(100, volumeBoostCap))
                                                }
                                                val volStep = ((newVolRatio * 100f) / 5f).toInt()
                                                if (volChanged || volStep != lastHapticVolumeStep) {
                                                    lastHapticVolumeStep = volStep
                                                    AppHaptics.performTick(view, haptic)
                                                }
                                                showVolumeHud = true
                                                showBrightnessHud = false
                                                hudLastInteractedTime = System.currentTimeMillis()
                                                change.consume()
                                            }
                                        }
                                        GestureLockMode.SEEK -> {
                                            if (horizontalSwipeToSeekEnabled) {
                                                val maxSeekFraction = horizontalSwipeSensitivity.coerceIn(1, 100) / 100f
                                                val dragFraction = (totalDragX / w.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                                                val seekDelta = dragFraction * (updatedDurationMs.coerceAtLeast(1000L) * maxSeekFraction)
                                                val targetPos = (initialSeekPosition + seekDelta.toLong()).coerceIn(0L, updatedDurationMs)
                                                val currentSec = targetPos / 1000L
                                                if (kotlin.math.abs(currentSec - lastHapticSeekSec) >= 1L) {
                                                    lastHapticSeekSec = currentSec
                                                    AppHaptics.performTick(view, haptic)
                                                }
                                                showSwipeSeekHud = true
                                                swipeSeekTargetMs = targetPos
                                                swipeSeekDeltaMs = seekDelta.toLong()
                                                updatedOnSeekTo(targetPos)
                                                if (areControlsVisible) lastInteractionTime = System.currentTimeMillis()
                                                change.consume()
                                            }
                                        }
                                        GestureLockMode.HOLD_SPEED -> {
                                            if (dynamicSpeedOverlayEnabled) {
                                                val speedDelta = -totalDragY / h.coerceAtLeast(200f) * 2.5
                                                val newSpeed = (holdSpeedMultiplier + speedDelta).coerceIn(0.5, 4.0)
                                                holdSpeedValue = newSpeed
                                                updatedOnSpeedChange(newSpeed)
                                                if (areControlsVisible) lastInteractionTime = System.currentTimeMillis()
                                                change.consume()
                                            }
                                        }
                                        GestureLockMode.SUBTITLE_DRAG -> {
                                            val delta = -dragDeltaY / h.coerceAtLeast(1f) * 100f
                                            updatedOnSubtitlePositionChange((updatedSubtitlePosition + delta).coerceIn(0f, 100f))
                                            if (areControlsVisible) lastInteractionTime = System.currentTimeMillis()
                                            change.consume()
                                        }
                                        else -> {
                                            // Not locked yet
                                        }
                                    }
                                }
                            } finally {
                                holdJob?.cancel()
                                if (isHoldSpeedActive) {
                                    updatedOnSpeedChange(holdOriginalSpeed)
                                    isHoldSpeedActive = false
                                }
                                if (lockMode == GestureLockMode.SEEK) {
                                    swipeSeekLastInteractionTime = System.currentTimeMillis()
                                }
                            }
                        }
                    }
            )
        // -------------------------------------------------------------
        // DOUBLE TAP SEEK WAVE ANIMATIONS & PLAY/PAUSE SPLASH
        // -------------------------------------------------------------
        LaunchedEffect(showDoubleTapSeekLeft) {
            if (showDoubleTapSeekLeft) {
                delay(650)
                showDoubleTapSeekLeft = false
            }
        }
        LaunchedEffect(showDoubleTapSeekRight) {
            if (showDoubleTapSeekRight) {
                delay(650)
                showDoubleTapSeekRight = false
            }
        }

        if (showDoubleTapSeekFeedback && (showDoubleTapSeekLeft || doubleTapLeftAlpha.value > 0.005f)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
                    .graphicsLayer {
                        val a = doubleTapLeftAlpha.value
                        alpha = a
                        scaleX = 0.85f + (0.15f * a)
                        scaleY = 0.85f + (0.15f * a)
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "-${currentSeekFeedbackSeconds}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
            }
        }

        if (showDoubleTapSeekFeedback && (showDoubleTapSeekRight || doubleTapRightAlpha.value > 0.005f)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
                    .graphicsLayer {
                        val a = doubleTapRightAlpha.value
                        alpha = a
                        scaleX = 0.85f + (0.15f * a)
                        scaleY = 0.85f + (0.15f * a)
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+${currentSeekFeedbackSeconds}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // GESTURE HUD (Brightness on Left / Volume on Right)
        // -------------------------------------------------------------
        LaunchedEffect(hudLastInteractedTime) {
            if (hudLastInteractedTime > 0L) {
                delay(1400)
                showBrightnessHud = false
                showVolumeHud = false
            }
        }

        LaunchedEffect(swipeSeekLastInteractionTime) {
            if (swipeSeekLastInteractionTime > 0L) {
                delay(650)
                showSwipeSeekHud = false
            }
        }

        // Brightness HUD
        if (showBrightnessSliderOverlay && (showBrightnessHud || brightnessHudAlpha.value > 0.005f)) {
            val isBrightnessOnRight = swapVolumeBrightnessGestures
            PlayerBrightnessPillHud(
                brightnessValue = hudBrightnessValue,
                onBrightnessDelta = { delta ->
                    val newB = (hudBrightnessValue + delta).coerceIn(0.01f, 1.0f)
                    hudBrightnessValue = newB
                    updatedOnBrightnessChange(newB)
                    hudLastInteractedTime = System.currentTimeMillis()
                },
                onInteraction = {
                    showBrightnessHud = true
                    showVolumeHud = false
                    hudLastInteractedTime = System.currentTimeMillis()
                },
                isAdjusting = isAdjustingBrightness,
                modifier = Modifier
                    .align(if (isBrightnessOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .then(
                        if (isBrightnessOnRight) {
                            Modifier.padding(end = if (isLandscape) 32.dp else 16.dp)
                        } else {
                            Modifier.padding(start = if (isLandscape) 32.dp else 16.dp)
                        }
                    )
                    .graphicsLayer {
                        val a = brightnessHudAlpha.value
                        alpha = a
                        scaleX = 0.90f + (0.10f * a)
                        scaleY = 0.90f + (0.10f * a)
                    }
            )
        }

        // Volume HUD
        if (showVolumeSliderOverlay && (showVolumeHud || volumeHudAlpha.value > 0.005f)) {
            val isVolumeOnLeft = swapVolumeBrightnessGestures
            PlayerVolumePillHud(
                volumePercent = hudVolumePercent,
                volumeBoostCap = volumeBoostCap,
                onVolumeDelta = { delta ->
                    val maxRatio = (volumeBoostCap / 100f).coerceIn(1.0f, 2.0f)
                    val newVolRatio = (hudVolumePercent + delta).coerceIn(0f, maxRatio)
                    hudVolumePercent = newVolRatio
                    if (newVolRatio <= 1.0f) {
                        val newVol = (newVolRatio * maxVolume).roundToInt().coerceIn(0, maxVolume)
                        if (newVol != currentVolume) {
                            currentVolume = newVol
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                        }
                        updatedOnVolumeBoostChange(100)
                    } else {
                        if (currentVolume != maxVolume) {
                            currentVolume = maxVolume
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
                        }
                        val boostVal = (newVolRatio * 100).roundToInt().coerceIn(100, volumeBoostCap)
                        updatedOnVolumeBoostChange(boostVal)
                    }
                    hudLastInteractedTime = System.currentTimeMillis()
                },
                onInteraction = {
                    showVolumeHud = true
                    showBrightnessHud = false
                    hudLastInteractedTime = System.currentTimeMillis()
                },
                isAdjusting = isAdjustingVolume,
                modifier = Modifier
                    .align(if (isVolumeOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .then(
                        if (isVolumeOnLeft) {
                            Modifier.padding(start = if (isLandscape) 32.dp else 16.dp)
                        } else {
                            Modifier.padding(end = if (isLandscape) 32.dp else 16.dp)
                        }
                    )
                    .graphicsLayer {
                        val a = volumeHudAlpha.value
                        alpha = a
                        scaleX = 0.90f + (0.10f * a)
                        scaleY = 0.90f + (0.10f * a)
                    }
            )
        }

        if (isHoldSpeedActive && dynamicSpeedOverlayEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", holdSpeedValue).trimEnd('0').trimEnd('.')}X",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.90f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
            }
        }

        // -------------------------------------------------------------
        // HORIZONTAL SWIPE SEEK NOTIFICATION BANNER (TOP NOTIFICATION STYLE)
        // Replaces the center HUD with the system glass notification UI
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = showSwipeSeekHud,
            enter = fadeIn(animationSpec = tween(160)) + slideInVertically(animationSpec = tween(160), initialOffsetY = { -30 }),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200), targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isLandscape) 52.dp else 64.dp)
        ) {
            val trans = LocalGlassBlurTransparency.current
            val factor = (trans / 100f).coerceIn(0.10f, 1.0f)
            val notifBg = Color(0xFF0F172A).copy(alpha = (0.55f + 0.40f * factor).coerceIn(0.45f, 0.95f))
            val notifBorder = Color.White.copy(alpha = (0.15f + 0.35f * factor).coerceIn(0.18f, 0.50f))

            val isForward = swipeSeekDeltaMs >= 0L
            val totalSec = kotlin.math.abs(swipeSeekDeltaMs) / 1000L
            val deltaMin = totalSec / 60
            val deltaSec = totalSec % 60
            val deltaText = if (deltaMin > 0) {
                "${if (isForward) "+" else "-"}${deltaMin}m ${deltaSec}s"
            } else {
                "${if (isForward) "+" else "-"}${deltaSec}s"
            }
            val percentOfVideo = if (durationMs > 0) {
                (kotlin.math.abs(swipeSeekDeltaMs).toFloat() / durationMs.toFloat() * 100f)
            } else 0f
            val seekProgress = if (durationMs > 0) {
                (swipeSeekTargetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(notifBg)
                    .border(1.dp, notifBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = "${formatTimeShort(swipeSeekTargetMs)} / ${formatTimeShort(durationMs)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "[ $deltaText · ${String.format(Locale.US, "%.1f%%", percentOfVideo)} ]",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFFA855F7),
                                        Color(0xFFEC4899)
                                    )
                                )
                            )
                        )
                    }

                    // Thin seek progress line
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(seekProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF38BDF8),
                                            Color(0xFFA855F7),
                                            Color(0xFFEC4899)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PERSISTENT AI SUBTITLE TRANSLATION BANNER
        // (Visible in player top area independent of active panel, auto-hides when completed/failed)
        // -------------------------------------------------------------
        val activeTranslation = translationStatusByTrackId.values
            .filterIsInstance<TrackTranslationStatus.Translating>()
            .firstOrNull()

        AnimatedVisibility(
            visible = activeTranslation != null,
            enter = fadeIn(animationSpec = tween(160)) + slideInVertically(animationSpec = tween(160), initialOffsetY = { -30 }),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200), targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isLandscape) 14.dp else 20.dp)
        ) {
            if (activeTranslation != null) {
                val trans = LocalGlassBlurTransparency.current
                val factor = (trans / 100f).coerceIn(0.10f, 1.0f)
                val bannerBg = Color(0xFF0F172A).copy(alpha = (0.70f + 0.25f * factor).coerceIn(0.60f, 0.95f))
                val bannerBorder = Color(0xFF38BDF8).copy(alpha = (0.25f + 0.35f * factor).coerceIn(0.25f, 0.60f))

                val bannerText = if (activeTranslation.etaSeconds != null) {
                    "Translating… ${activeTranslation.progressPercent}% • ~${activeTranslation.etaSeconds}s left"
                } else {
                    "Translating… ${activeTranslation.progressPercent}%"
                }

                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.35f),
                            spotColor = Color.Black.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(bannerBg)
                        .border(1.dp, bannerBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { (activeTranslation.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(13.dp),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bannerText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // CUSTOM SYSTEM GLASS ACTION NOTIFICATION BANNER
        // (Shape: Rounded Rectangle, System Gradient Text, No App Icon,
        //  Transparency controlled by glassBlurTransparency setting)
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = playerNotificationMessage != null && !showSwipeSeekHud,
            enter = fadeIn(animationSpec = tween(160)) + slideInVertically(animationSpec = tween(160), initialOffsetY = { -30 }),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200), targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (isLandscape) 52.dp else 64.dp)
        ) {
            playerNotificationMessage?.let { msg ->
                val trans = LocalGlassBlurTransparency.current
                val factor = (trans / 100f).coerceIn(0.10f, 1.0f)
                val notifBg = Color(0xFF0F172A).copy(alpha = (0.55f + 0.40f * factor).coerceIn(0.45f, 0.95f))
                val notifBorder = Color.White.copy(alpha = (0.15f + 0.35f * factor).coerceIn(0.18f, 0.50f))

                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color.Black.copy(alpha = 0.35f),
                            spotColor = Color.Black.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(notifBg)
                        .border(1.dp, notifBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = msg,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFFA855F7),
                                    Color(0xFFEC4899)
                                )
                            )
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // SCREEN LOCKED OVERLAY
        // -------------------------------------------------------------
        if (isScreenLocked) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable {
                            isScreenLocked = false
                            areControlsVisible = true
                            lastInteractionTime = System.currentTimeMillis()
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StyledIcon(
                                imageVector = Icons.Outlined.Lock,
                            contentDescription = "Unlock",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to Unlock",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            return@Box
        }

        // -------------------------------------------------------------
        // DYNAMIC CONTROL RENDERER
        // -------------------------------------------------------------
        @Composable
        fun RenderDynamicControl(controlId: PlayerControlId) {
            when (controlId) {
                PlayerControlId.CAST -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_screencast,
                        contentDescription = "Cast",
                        onClick = {
                            notifyInteraction()
                            openSystemCastPicker(context) { msg -> notifyAction(msg) }
                        }
                    )
                }
                PlayerControlId.CHAPTER -> {
                    ChapterPillButton(
                        chapterLabel = chapterDisplayLabel,
                        chapters = chapters,
                        currentPositionMs = currentPositionMs,
                        onSeekToChapter = onSeekToChapter,
                        onOpenDialog = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.CHAPTERS)
                        },
                        notifyInteraction = { notifyInteraction() },
                        notifyAction = { msg -> notifyAction(msg) }
                    )
                }
                PlayerControlId.DECODER -> {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(getEffectiveGlassBg())
                            .border(1.dp, getEffectiveGlassBorder(), RoundedCornerShape(22.dp))
                            .cardBounceClick(scaleDown = 0.94f) {
                                notifyInteraction()
                                val nextDec = when (decoderMode) {
                                    DecoderMode.HW_PLUS -> DecoderMode.HW
                                    DecoderMode.HW -> DecoderMode.SW
                                    DecoderMode.SW -> DecoderMode.HW_PLUS
                                }
                                onDecoderModeChange(nextDec)
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StyledIcon(
                                drawableRes = R.drawable.lumora_cpu,
                                contentDescription = "Decoder",
                                tint = GlassIconTint,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = decoderMode.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GlassIconTint
                            )
                        }
                    }
                }
                PlayerControlId.AUDIO_TRACK -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_song,
                        contentDescription = "Audio Track",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.AUDIO_TRACK)
                        }
                    )
                }
                PlayerControlId.SUBTITLES -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_subtitles,
                        contentDescription = "Subtitles",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.SUBTITLE_TRACK)
                        }
                    )
                }
                PlayerControlId.PLAYER_LAYOUT -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_widget_5,
                        contentDescription = "Player Layout",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.PLAYER_LAYOUT)
                        }
                    )
                }
                PlayerControlId.MORE_OPTIONS -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_menu_dots,
                        contentDescription = "More Options",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.MORE_MENU)
                        }
                    )
                }
                PlayerControlId.AUDIO_ONLY -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_headphones_round,
                        contentDescription = "Audio-Only Mode",
                        onClick = {
                            notifyInteraction()
                            onAudioOnlyChange(!isAudioOnly)
                        }
                    )
                }
                PlayerControlId.LOCK_SCREEN -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_lock_unlocked,
                        contentDescription = "Lock Screen",
                        onClick = {
                            isScreenLocked = true
                            areControlsVisible = false
                        }
                    )
                }
                PlayerControlId.SCREEN_ROTATION -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_smartphone_rotate_orientation,
                        contentDescription = "Rotate Screen",
                        onClick = {
                            notifyInteraction()
                            onToggleOrientation()
                        }
                    )
                }
                PlayerControlId.PLAYBACK_SPEED -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_speed,
                        contentDescription = "Playback Speed",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.SPEED)
                        }
                    )
                }
                PlayerControlId.REPEAT_MODE -> {
                    FloatingCircleButton(
                        drawableRes = if (repeatMode == PlayerRepeatMode.ONE) R.drawable.lumora_repeat_one else R.drawable.lumora_repeat,
                        contentDescription = "Repeat Mode",
                        onClick = {
                            notifyInteraction()
                            val nextRpt = when (repeatMode) {
                                PlayerRepeatMode.OFF -> PlayerRepeatMode.ONE
                                PlayerRepeatMode.ONE -> PlayerRepeatMode.ALL
                                PlayerRepeatMode.ALL -> PlayerRepeatMode.OFF
                            }
                            onRepeatModeChange(nextRpt)
                        }
                    )
                }
                PlayerControlId.SHUFFLE -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_shuffle,
                        contentDescription = "Shuffle",
                        onClick = {
                            notifyInteraction()
                            onShuffleChange(!isShuffle)
                        }
                    )
                }
                PlayerControlId.AB_LOOP -> {
                    AbLoopInlineMorphButton(
                        isExpanded = isAbInlineExpanded,
                        onToggleExpand = {
                            isAbInlineExpanded = !isAbInlineExpanded
                            if (isAbInlineExpanded) isCameraMorphExpanded = false
                        },
                        loopPointA = loopPointA,
                        loopPointB = loopPointB,
                        currentPositionMs = currentPositionMs,
                        onSetPointA = { ptA ->
                            onSetLoopPointA(ptA)
                        },
                        onSetPointB = { ptB ->
                            onSetLoopPointB(ptB)
                        },
                        onClearLoop = {
                            onClearLoop()
                        },
                        notifyInteraction = {
                            notifyInteraction()
                        }
                    )
                }
                PlayerControlId.FRAME_NAVIGATION -> {
                    CameraFrameCaptureInlineMorphButton(
                        isExpanded = isCameraMorphExpanded,
                        onToggleExpand = {
                            isCameraMorphExpanded = !isCameraMorphExpanded
                            if (isCameraMorphExpanded) isAbInlineExpanded = false
                        },
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        isPlaying = isPlaying,
                        onPlayPauseToggle = onPlayPauseToggle,
                        onSeekTo = onSeekTo,
                        onTakeScreenshot = onTakeScreenshot,
                        notifyInteraction = {
                            notifyInteraction()
                        }
                    )
                }
                PlayerControlId.VIDEO_ZOOM -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_magnifer_zoom_in,
                        contentDescription = "Video Zoom",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.VIDEO_ZOOM)
                        }
                    )
                }
                PlayerControlId.PIP -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_pip,
                        contentDescription = "Picture in Picture",
                        onClick = {
                            notifyInteraction()
                            onTogglePiP()
                        }
                    )
                }
                PlayerControlId.ASPECT_RATIO -> {
                    val nextMode = aspectRatioMode.next()
                    FloatingCircleButton(
                        drawableRes = nextMode.iconRes,
                        contentDescription = "Display Mode: Switch to ${nextMode.label}",
                        onClick = {
                            notifyInteraction()
                            onAspectRatioChange(nextMode)
                        }
                    )
                }
                PlayerControlId.FLIP_VERTICAL -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_flip_vertical,
                        contentDescription = if (isVideoFlippedVertically) "Flip Vertical: Restore" else "Flip Vertical",
                        glassBg = if (isVideoFlippedVertically) AccentSkyBlue.copy(alpha = 0.82f) else null,
                        glassBorder = if (isVideoFlippedVertically) AccentSkyBlue else null,
                        onClick = {
                            notifyInteraction()
                            onToggleVideoFlipVertical()
                        }
                    )
                }
                PlayerControlId.MIRROR_RIGHT -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_mirror_right,
                        contentDescription = if (isVideoMirrored) "Mirror Right: Active" else "Mirror Right",
                        glassBg = if (isVideoMirrored) AccentSkyBlue.copy(alpha = 0.82f) else null,
                        glassBorder = if (isVideoMirrored) AccentSkyBlue else null,
                        onClick = {
                            notifyInteraction()
                            onMirrorVideoRight()
                        }
                    )
                }
                PlayerControlId.MIRROR_LEFT -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_mirror_left,
                        contentDescription = "Mirror Left",
                        onClick = {
                            notifyInteraction()
                            onMirrorVideoLeft()
                        }
                    )
                }
                PlayerControlId.VIDEO_EQ -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_video_setting,
                        contentDescription = "Video Equalizer",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.VIDEO_SETTING_FILTER)
                        }
                    )
                }
                PlayerControlId.PITCH_CORRECTION -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_translation,
                        contentDescription = "Pitch Correction",
                        glassBg = if (isPitchCorrectionEnabled) AccentSkyBlue.copy(alpha = 0.85f) else null,
                        glassBorder = if (isPitchCorrectionEnabled) AccentSkyBlue else null,
                        onClick = {
                            notifyInteraction()
                            onTogglePitchCorrection(!isPitchCorrectionEnabled)
                        }
                    )
                }
                PlayerControlId.PLAYLIST -> {
                    FloatingCircleButton(
                        drawableRes = R.drawable.lumora_library,
                        contentDescription = "Playlist",
                        onClick = {
                            notifyInteraction()
                            openRootPanel(PlayerPanelType.PLAYLIST)
                        }
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // MAIN PLAYER CONTROLS (EXACT REPLICA OF REFERENCE SCREENSHOT)
        // -------------------------------------------------------------
        val motionDuration = { base: Int ->
            (base / animationSpeed.coerceIn(0.25f, 2f)).toInt().coerceIn(0, 1400)
        }
        val controlsEnter = when (controlsAnimationStyle) {
            ControlsAnimationStyle.NONE -> fadeIn(tween(0))
            ControlsAnimationStyle.MINIMAL_FADE -> fadeIn(tween(motionDuration(180)))
            ControlsAnimationStyle.CINEMATIC_SCALE -> androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = tween(motionDuration(260), easing = FastOutSlowInEasing)) + fadeIn(tween(motionDuration(180)))
            ControlsAnimationStyle.FLUID_EXPAND -> androidx.compose.animation.expandIn(animationSpec = tween(motionDuration(280))) + fadeIn(tween(motionDuration(160)))
            ControlsAnimationStyle.GENTLE_BOUNCE -> androidx.compose.animation.scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f)) + fadeIn(tween(motionDuration(160)))
            ControlsAnimationStyle.DEFAULT -> fadeIn(tween(motionDuration(180))) + slideInVertically(initialOffsetY = { 24 }, animationSpec = tween(motionDuration(180)))
        }
        val controlsExit = when (controlsAnimationStyle) {
            ControlsAnimationStyle.NONE -> fadeOut(tween(0))
            ControlsAnimationStyle.MINIMAL_FADE -> fadeOut(tween(motionDuration(160)))
            ControlsAnimationStyle.CINEMATIC_SCALE -> androidx.compose.animation.scaleOut(targetScale = 0.94f, animationSpec = tween(motionDuration(220))) + fadeOut(tween(motionDuration(160)))
            ControlsAnimationStyle.FLUID_EXPAND -> androidx.compose.animation.shrinkOut(animationSpec = tween(motionDuration(240))) + fadeOut(tween(motionDuration(140)))
            ControlsAnimationStyle.GENTLE_BOUNCE -> androidx.compose.animation.scaleOut(targetScale = 0.84f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f)) + fadeOut(tween(motionDuration(150)))
            ControlsAnimationStyle.DEFAULT -> fadeOut(tween(motionDuration(220))) + slideOutVertically(targetOffsetY = { 24 }, animationSpec = tween(motionDuration(180)))
        }
        AnimatedVisibility(
            visible = effectiveControlsVisible,
            enter = controlsEnter,
            exit = controlsExit
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // =========================================================
                // 1. TOP CONTROL BAR (Portrait: Unified Single Row, Landscape: Two-Section Balanced Row)
                // =========================================================
                if (!isLandscape) {
                    // =========================================================
                    // 1A. PORTRAIT TOP CONTROL BAR (Single Unified Slideable Row)
                    // Episode Name + All Controls (Chapter, Audio, Subtitles, HW+, Cast, etc.)
                    // Back and Three-Dot buttons fixed at ends; content slides behind them with smooth blur/fade
                    // =========================================================
                    val scrollState = portraitTopScrollState

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = controlsAlpha.value
                                translationY = -controlsSlideY.value
                            }
                            .height(44.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Scrollable content track with smooth blur-fade masking behind anchored buttons
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    val fadeWidthPx = 28.dp.toPx()
                                    val backAnchorWidthPx = 52.dp.toPx()
                                    val threeDotAnchorWidthPx = 52.dp.toPx()
                                    val canFadeLeft = scrollState.value > 4
                                    val canFadeRight = scrollState.maxValue > 4 && scrollState.value < (scrollState.maxValue - 4)

                                    // Smooth blur-fade when sliding left behind the Back button
                                    if (canFadeLeft && size.width > (backAnchorWidthPx + fadeWidthPx)) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color.Black),
                                                startX = backAnchorWidthPx - 8.dp.toPx(),
                                                endX = backAnchorWidthPx + fadeWidthPx
                                            ),
                                            blendMode = BlendMode.DstIn,
                                            topLeft = Offset(0f, 0f),
                                            size = Size(backAnchorWidthPx + fadeWidthPx, size.height)
                                        )
                                    }

                                    // Smooth blur-fade when sliding right behind the Three-Dot button
                                    if (canFadeRight && size.width > (threeDotAnchorWidthPx + fadeWidthPx)) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.Black, Color.Transparent),
                                                startX = size.width - threeDotAnchorWidthPx - fadeWidthPx,
                                                endX = size.width - threeDotAnchorWidthPx + 8.dp.toPx()
                                            ),
                                            blendMode = BlendMode.DstIn,
                                            topLeft = Offset(size.width - threeDotAnchorWidthPx - fadeWidthPx, 0f),
                                            size = Size(threeDotAnchorWidthPx + fadeWidthPx, size.height)
                                        )
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .horizontalScroll(scrollState)
                                    .padding(start = 52.dp, end = 52.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Episode Name UI: Full uncompressed readable pill with smooth marquee for long titles
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 180.dp, max = 280.dp)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(getEffectiveGlassBg())
                                        .border(1.dp, getEffectiveGlassBorder(), RoundedCornerShape(22.dp))
                                        .cardBounceClick(scaleDown = 0.94f) {
                                            notifyInteraction()
                                            openRootPanel(PlayerPanelType.PLAYLIST)
                                        }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    AutoScrollingText(
                                        text = if (totalEpisodes > 1) "$videoTitle • $currentEpisodeIndex/$totalEpisodes" else videoTitle,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassIconTint
                                        ),
                                        maxLines = 1,
                                        startDelayMillis = 1000,
                                        pauseMillis = 1000,
                                        speedMillis = 3200
                                    )
                                }

                                // 2. Time Network Clock Badge (if enabled in settings)
                                TimeNetworkClockBadge(
                                    format = playerSettings.timeNetworkClockFormat
                                )

                                // 3. All Active Top Action Controls (Chapter, HW+, Audio, Subtitles, Cast, etc.)
                                val activeTopControls = if (playerLayoutConfig.isPortraitTopEnabled) {
                                    playerLayoutConfig.portraitTopControls.filter { it != PlayerControlId.MORE_OPTIONS }
                                } else emptyList()

                                activeTopControls.forEach { controlId ->
                                    RenderDynamicControl(controlId)
                                }
                            }
                        }

                        // Fixed Back Button: Always crisp, anchored at far left
                        FloatingCircleButton(
                            drawableRes = R.drawable.lumora_leave,
                            contentDescription = "Back",
                            size = 44.dp,
                            iconSize = 22.dp,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(44.dp)
                                .aspectRatio(1f, matchHeightConstraintsFirst = true),
                            onClick = onBack
                        )

                        // Fixed Three-Dot More Options Button: Always crisp, anchored at far right
                        FloatingCircleButton(
                            drawableRes = R.drawable.lumora_menu_dots,
                            contentDescription = "More Options",
                            size = 44.dp,
                            iconSize = 22.dp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(44.dp)
                                .aspectRatio(1f, matchHeightConstraintsFirst = true),
                            onClick = {
                                notifyInteraction()
                                openRootPanel(PlayerPanelType.MORE_MENU)
                            }
                        )
                    }
                } else {
                    // =========================================================
                    // 1B. LANDSCAPE TOP CONTROL BAR (2 Balanced Sections)
                    // Left: Back Button + Episode Name Pill
                    // Right: Action Buttons + Three-Dot Button (Stable, crisp, smooth blur-fade ONLY on actual overlap)
                    // =========================================================
                    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
                    val scrollState = landscapeTopScrollState

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                alpha = controlsAlpha.value
                                translationY = -controlsSlideY.value
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. LEFT SECTION: Back Button + File Name Pill (Balanced proportional max width)
                        Row(
                            modifier = Modifier
                                .widthIn(min = 160.dp, max = (screenWidthDp * 0.40f).coerceIn(240.dp, 380.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FloatingCircleButton(
                                drawableRes = R.drawable.lumora_leave,
                                contentDescription = "Back",
                                size = 44.dp,
                                iconSize = 22.dp,
                                modifier = Modifier
                                    .size(44.dp)
                                    .aspectRatio(1f, matchHeightConstraintsFirst = true),
                                onClick = onBack
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(getEffectiveGlassBg())
                                    .border(1.dp, getEffectiveGlassBorder(), RoundedCornerShape(22.dp))
                                    .cardBounceClick(scaleDown = 0.94f) {
                                        notifyInteraction()
                                        openRootPanel(PlayerPanelType.PLAYLIST)
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                AutoScrollingText(
                                    text = if (totalEpisodes > 1) "$videoTitle • $currentEpisodeIndex/$totalEpisodes" else videoTitle,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlassIconTint
                                    ),
                                    maxLines = 1,
                                    startDelayMillis = 1000,
                                    pauseMillis = 1000,
                                    speedMillis = 3200
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 2. RIGHT SECTION: Action Buttons & Controls (Original positions stable, smooth fade ONLY when items actually overlap/scroll behind)
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        val fadeWidthPx = 24.dp.toPx()
                                        val canFadeLeft = scrollState.value > 6
                                        val canFadeRight = scrollState.maxValue > 6 && scrollState.value < (scrollState.maxValue - 6)

                                        // Left smooth blur-fade ONLY when scrolled to the right (items sliding behind towards episode name)
                                        if (canFadeLeft && size.width > fadeWidthPx * 2) {
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black),
                                                    startX = 0f,
                                                    endX = fadeWidthPx
                                                ),
                                                blendMode = BlendMode.DstIn,
                                                size = Size(fadeWidthPx, size.height)
                                            )
                                        }

                                        // Right smooth blur-fade ONLY when items overflow and actually scroll towards/behind Three-Dot button
                                        if (canFadeRight && size.width > fadeWidthPx * 2) {
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(Color.Black, Color.Transparent),
                                                    startX = size.width - fadeWidthPx,
                                                    endX = size.width
                                                ),
                                                blendMode = BlendMode.DstIn,
                                                topLeft = Offset(size.width - fadeWidthPx, 0f),
                                                size = Size(fadeWidthPx, size.height)
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.horizontalScroll(scrollState),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Time Network Clock Badge (if enabled in settings)
                                    TimeNetworkClockBadge(
                                        format = playerSettings.timeNetworkClockFormat
                                    )

                                    // Additional Tool Icons (Chapter, HW+, Audio, Subtitles, Grid, Cast, etc.)
                                    val activeTopControls = if (playerLayoutConfig.isLandscapeTopRightEnabled) {
                                        playerLayoutConfig.topRightControls.filter { it != PlayerControlId.MORE_OPTIONS }
                                    } else emptyList()

                                    activeTopControls.forEach { controlId ->
                                        RenderDynamicControl(controlId)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Three-Dot More Options Button (Always anchored at far right, crisp & unblurred)
                            FloatingCircleButton(
                                drawableRes = R.drawable.lumora_menu_dots,
                                contentDescription = "More Options",
                                size = 44.dp,
                                iconSize = 22.dp,
                                modifier = Modifier
                                    .size(44.dp)
                                    .aspectRatio(1f, matchHeightConstraintsFirst = true),
                                onClick = {
                                    notifyInteraction()
                                    openRootPanel(PlayerPanelType.MORE_MENU)
                                }
                            )
                        }
                    }
                }

                // =========================================================
                // 2. PLAYBACK BUTTONS POSITION RESOLUTION
                // =========================================================
                val currentPlaybackButtonsPosition = if (isLandscape) {
                    playerSettings.landscapePlaybackButtonsPosition
                } else {
                    playerSettings.portraitPlaybackButtonsPosition
                }

                if (currentPlaybackButtonsPosition == "CENTER") {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val a = controlsAlpha.value
                                alpha = a
                                scaleX = 0.85f + (0.15f * a)
                                scaleY = 0.85f + (0.15f * a)
                            },
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mediaPreviousControlEnabled) {
                            FloatingCircleButton(
                                drawableRes = R.drawable.lumora_arrow_left_solid,
                                contentDescription = "Previous",
                                size = 52.dp,
                                iconSize = 28.dp,
                                enabled = hasPrevious,
                                modifier = Modifier.graphicsLayer { alpha = if (hasPrevious) 1f else 0.35f },
                                onClick = { if (hasPrevious) { notifyInteraction(); onPreviousVideo() } }
                            )
                        } else {
                            Spacer(Modifier.size(52.dp))
                        }

                        if (mediaPlayPauseControlEnabled) {
                            FloatingCircleButton(
                                drawableRes = if (isPlaying) R.drawable.lumora_pause_solid else R.drawable.lumora_play_solid,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                size = 64.dp,
                                iconSize = 36.dp,
                                onClick = { handlePlayPauseToggle() }
                            )
                        } else {
                            Spacer(Modifier.size(64.dp))
                        }

                        if (mediaNextControlEnabled) {
                            FloatingCircleButton(
                                drawableRes = R.drawable.lumora_arrow_right_solid,
                                contentDescription = "Next",
                                size = 52.dp,
                                iconSize = 28.dp,
                                enabled = hasNext,
                                modifier = Modifier.graphicsLayer { alpha = if (hasNext) 1f else 0.35f },
                                onClick = { if (hasNext) { notifyInteraction(); onNextVideo() } }
                            )
                        } else {
                            Spacer(Modifier.size(52.dp))
                        }
                    }
                }

                // =========================================================
                // 3. BOTTOM SECTION: ACTION CIRCLES + SLEEK PROGRESS BAR
                // =========================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .graphicsLayer {
                            alpha = controlsAlpha.value
                            translationY = controlsSlideY.value
                        },
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Action Circles Row (Left Group + Right Group)
                    // NO BACKGROUND BAR BEHIND THE GROUPS!
                    if (!isLandscape) {
                        // Portrait Mode: Horizontally scrollable row containing all action tools
                        if (playerLayoutConfig.isPortraitBottomEnabled && playerLayoutConfig.portraitBottomControls.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(portraitBottomScrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                playerLayoutConfig.portraitBottomControls.forEach { controlId ->
                                    RenderDynamicControl(controlId)
                                }
                            }
                        }
                    } else {
                        // Landscape Mode: Left and Right floating circle groups
                        val showBL = playerLayoutConfig.isLandscapeBottomLeftEnabled && playerLayoutConfig.bottomLeftControls.isNotEmpty()
                        val showBR = playerLayoutConfig.isLandscapeBottomRightEnabled && playerLayoutConfig.bottomRightControls.isNotEmpty()

                        if (showBL || showBR) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Bottom Left Floating Circles (scrollable if many icons)
                                if (showBL) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxWidth(if (showBR) 0.58f else 1f)
                                            .horizontalScroll(landscapeBottomScrollState),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        playerLayoutConfig.bottomLeftControls.forEach { controlId ->
                                            RenderDynamicControl(controlId)
                                        }
                                    }
                                }

                                // Bottom Right Floating Circles
                                if (showBR) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        playerLayoutConfig.bottomRightControls.forEach { controlId ->
                                            RenderDynamicControl(controlId)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Playback Buttons: Between seekbar and controls when selected
                    if (currentPlaybackButtonsPosition == "BOTTOM") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (mediaPreviousControlEnabled) {
                                FloatingCircleButton(
                                    drawableRes = R.drawable.lumora_arrow_left_solid,
                                    contentDescription = "Previous",
                                    size = 46.dp,
                                    iconSize = 24.dp,
                                    enabled = hasPrevious,
                                    modifier = Modifier.graphicsLayer { alpha = if (hasPrevious) 1f else 0.35f },
                                    onClick = { if (hasPrevious) { notifyInteraction(); onPreviousVideo() } }
                                )
                            } else {
                                Spacer(Modifier.size(46.dp))
                            }

                            Spacer(Modifier.width(18.dp))

                            if (mediaPlayPauseControlEnabled) {
                                FloatingCircleButton(
                                    drawableRes = if (isPlaying) R.drawable.lumora_pause_solid else R.drawable.lumora_play_solid,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    size = 56.dp,
                                    iconSize = 32.dp,
                                    onClick = { handlePlayPauseToggle() }
                                )
                            } else {
                                Spacer(Modifier.size(56.dp))
                            }

                            Spacer(Modifier.width(18.dp))

                            if (mediaNextControlEnabled) {
                                FloatingCircleButton(
                                    drawableRes = R.drawable.lumora_arrow_right_solid,
                                    contentDescription = "Next",
                                    size = 46.dp,
                                    iconSize = 24.dp,
                                    enabled = hasNext,
                                    modifier = Modifier.graphicsLayer { alpha = if (hasNext) 1f else 0.35f },
                                    onClick = { if (hasNext) { notifyInteraction(); onNextVideo() } }
                                )
                            } else {
                                Spacer(Modifier.size(46.dp))
                            }
                        }
                    }

                    // Bottom Scrubber Bar: 00:05 [=========|===] 23:59 (Recomposition isolated)
                    PlayerScrubberBar(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        seekbarStyle = seekbarStyle,
                        loopPointA = loopPointA,
                        loopPointB = loopPointB,
                        chapters = chapters,
                        skipMarkers = skipMarkers,
                        onSeekTo = onSeekTo,
                        notifyInteraction = ::notifyInteraction
                    )
                }
            }
        }

        // Chapter-based Skip Button (Visible when playback position enters a detected skippable chapter)
        val playbackPos = if (isLandscape) {
            playerSettings.landscapePlaybackButtonsPosition
        } else {
            playerSettings.portraitPlaybackButtonsPosition
        }
        val skipButtonBottomPadding by animateDpAsState(
            targetValue = if (areControlsVisible) {
                if (playbackPos == "BOTTOM") {
                    if (isLandscape) 210.dp else 218.dp
                } else {
                    if (isLandscape) 148.dp else 156.dp
                }
            } else {
                if (isLandscape) 36.dp else 44.dp
            },
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            label = "skipButtonBottomPadding"
        )

        PlayerChapterSkipButton(
            activeMarker = activeSkipMarker,
            currentPositionMs = currentPositionMs,
            onSkip = { targetMs ->
                onSkipToPosition(targetMs)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(
                    end = if (isLandscape) 24.dp else 16.dp,
                    bottom = skipButtonBottomPadding
                )
        )

        // =============================================================
        // GLASSMORPHIC SELECTION PANELS & SHEETS (STRICT HIERARCHY / CLEAN LAYERING)
        // =============================================================
        when (activePanel) {
            PlayerPanelType.SUBTITLE_TRACK -> {
                SubtitleSelectionPanel(
                    videoTitle = videoTitle,
                    subtitleTracks = subtitleTracks,
                    selectedTrackId = selectedSubtitleTrackId,
                    secondaryTrackId = secondarySubtitleTrackId,
                    lastTouchedTrackId = lastTouchedSubtitleTrackId,
                    isSubtitleVisible = isSubtitleVisible,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onSelectTrack = onSelectSubtitleTrack,
                    onSelectSecondaryTrack = onSelectSecondarySubtitleTrack,
                    onLastTouchedTrackIdChange = onLastTouchedSubtitleTrackIdChange,
                    onRemoveTrack = onRemoveSubtitleTrack,
                    onToggleVisibility = onToggleSubtitleVisibility,
                    onImportSubtitle = onImportSubtitle,
                    onSelectSubtitleFile = onSelectSubtitleFile,
                    onShowNotification = notifyAction,
                    onOpenSubtitleDelay = { openPanel(PlayerPanelType.SUBTITLE_DELAY) },
                    onOpenSubtitleStyle = { openPanel(PlayerPanelType.SUBTITLE_STYLE) },
                    translationStatusByTrackId = translationStatusByTrackId,
                    onStartTranslate = onStartTranslate,
                    onTranslateSubtitleTrack = onTranslateSubtitleTrack,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.SUBTITLE_DELAY -> {
                SubtitleDelayPanel(
                    subtitleDelayMs = subtitleOffsetMs,
                    currentPositionMs = currentPositionMs,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onSubtitleDelayChange = onSubtitleOffsetChange,
                    onSetAsDefault = { defaultMs: Long ->
                        val currentSettings = PlayerSettings.load(context)
                        PlayerSettings.save(context, currentSettings.copy(defaultSubtitleDelayMs = defaultMs))
                    },
                    onShowNotification = notifyAction,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.SUBTITLE_STYLE -> {
                val targetTrack = subtitleTracks.firstOrNull { it.id == lastTouchedSubtitleTrackId }
                    ?: subtitleTracks.firstOrNull { it.id == selectedSubtitleTrackId }
                    ?: subtitleTracks.firstOrNull { it.isSelected }

                SubtitleStylePanel(
                    settings = subtitleSettings,
                    context = context,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    activeSubtitleTrack = targetTrack,
                    onSettingsChange = onSubtitleSettingsChange,
                    onDismissRequest = { dismissCurrentPanel() },
                    onBackToSubtitleTracks = { dismissCurrentPanel() },
                    onLoadAdvancedAssSource = onLoadAdvancedAssSource
                )
            }
            PlayerPanelType.AUDIO_TRACK -> {
                AudioSelectionPanel(
                    audioTracks = audioTracks,
                    selectedTrackId = selectedAudioTrackId,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    trackAudioConfigs = trackAudioConfigs,
                    onSelectTrack = onSelectAudioTrack,
                    onRemoveTrack = onRemoveAudioTrack,
                    onTrackChannelModeChange = onTrackChannelModeChange,
                    onTrackVolumeNormalizationChange = onTrackVolumeNormalizationChange,
                    onTrackDynamicRangeCompressionChange = onTrackDynamicRangeCompressionChange,
                    onImportAudio = onImportAudio,
                    onShowNotification = notifyAction,
                    onOpenAudioDelay = { openPanel(PlayerPanelType.AUDIO_DELAY) },
                    onOpenEqualizer = { openPanel(PlayerPanelType.EQUALIZER) },
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.AUDIO_DELAY -> {
                AudioDelayPanel(
                    audioDelayMs = audioDelayMs,
                    currentPositionMs = currentPositionMs,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onAudioDelayChange = onAudioDelayChange,
                    onSetAsDefault = { defaultMs: Long ->
                        val currentSettings = PlayerSettings.load(context)
                        PlayerSettings.save(context, currentSettings.copy(defaultAudioDelayMs = defaultMs))
                    },
                    onShowNotification = notifyAction,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.EQUALIZER -> {
                AudioEqualizerPanel(
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    equalizerEnabled = equalizerEnabled,
                    onToggleEqualizer = onToggleEqualizer,
                    eq60Hz = eq60Hz,
                    onEq60HzChange = onEq60HzChange,
                    eq230Hz = eq230Hz,
                    onEq230HzChange = onEq230HzChange,
                    eq910Hz = eq910Hz,
                    onEq910HzChange = onEq910HzChange,
                    eq3600Hz = eq3600Hz,
                    onEq3600HzChange = onEq3600HzChange,
                    eq14000Hz = eq14000Hz,
                    onEq14000HzChange = onEq14000HzChange,
                    volumeBoostDb = volumeBoostDb,
                    onVolumeBoostDbChange = onVolumeBoostDbChange,
                    currentPreset = equalizerPreset,
                    onSelectPreset = onSelectEqualizerPreset,
                    onReset = onResetEqualizer,
                    onShowNotification = notifyAction,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.CHAPTERS -> {
                ChapterSelectionPanel(
                    chapters = chapters,
                    currentPositionMs = currentPositionMs,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onSeekToChapter = onSeekToChapter,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.MORE_MENU -> {
                MoreMenuSelectionPanel(
                    playbackSpeed = playbackSpeed,
                    onOpenSpeedDialog = { openPanel(PlayerPanelType.SPEED) },
                    onOpenVideoZoomDialog = { openPanel(PlayerPanelType.VIDEO_ZOOM) },
                    onOpenVideoEqDialog = { openPanel(PlayerPanelType.VIDEO_SETTING_FILTER) },
                    onOpenAbLoopDialog = { openPanel(PlayerPanelType.AB_LOOP) },
                    onOpenAudioDelay = { openPanel(PlayerPanelType.AUDIO_DELAY) },
                    onOpenEqualizer = { openPanel(PlayerPanelType.EQUALIZER) },
                    aspectRatioMode = aspectRatioMode,
                    onAspectRatioChange = onAspectRatioChange,
                    decoderMode = decoderMode,
                    onDecoderModeChange = onDecoderModeChange,
                    isAudioOnly = isAudioOnly,
                    onAudioOnlyChange = onAudioOnlyChange,
                    repeatMode = repeatMode,
                    onRepeatModeChange = onRepeatModeChange,
                    isShuffle = isShuffle,
                    onShuffleChange = onShuffleChange,
                    gestureSensitivityMode = gestureSensitivityMode,
                    onGestureSensitivityModeChange = { mode ->
                        onGestureSensitivityModeChange?.invoke(mode)
                        notifyAction("Gesture Sensitivity: ${mode.displayName}")
                    },
                    onTakeScreenshot = onTakeScreenshot,
                    onTogglePiP = onTogglePiP,
                    onToggleOrientation = onToggleOrientation,
                    onOpenPlayerLayoutDialog = { openPanel(PlayerPanelType.PLAYER_LAYOUT) },
                    onOpenVideoQualityDialog = if (availableVideoQualities.isNotEmpty()) { { openPanel(PlayerPanelType.VIDEO_QUALITY) } } else null,
                    selectedOnlineQuality = selectedVideoQuality,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.VIDEO_QUALITY -> {
                com.example.ui.components.VideoQualitySelectionPanel(
                    qualities = availableVideoQualities,
                    selectedQuality = selectedVideoQuality,
                    onSelectQuality = { quality ->
                        onSelectVideoQuality?.invoke(quality)
                    },
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.SPEED -> {
                SpeedSelectionPanel(
                    currentSpeed = playbackSpeed,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    isPitchCorrectionEnabled = isPitchCorrectionEnabled,
                    onTogglePitchCorrection = onTogglePitchCorrection,
                    onSpeedChange = onSpeedChange,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.VIDEO_ZOOM -> {
                VideoZoomSelectionPanel(
                    zoomScale = videoZoomScale,
                    onZoomChange = onVideoZoomChange,
                    isPanAndZoomEnabled = isPanAndZoomEnabled,
                    onTogglePanAndZoom = onTogglePanAndZoom,
                    onResetZoomAndPan = onResetZoomAndPan,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.VIDEO_SETTING_FILTER -> {
                VideoSettingSelectionPanel(
                    selectedPreset = videoFilterPreset,
                    onSelectPreset = onSelectVideoFilterPreset,
                    manualAdjustments = manualVideoAdjustments,
                    onManualAdjustmentsChange = onManualVideoAdjustmentsChange,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onOpenManualAdjustments = {
                        openPanel(PlayerPanelType.MANUAL_VIDEO_ADJUSTMENT)
                    },
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.MANUAL_VIDEO_ADJUSTMENT -> {
                ManualVideoAdjustmentPanel(
                    manualAdjustments = manualVideoAdjustments,
                    onManualAdjustmentsChange = onManualVideoAdjustmentsChange,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.AB_LOOP -> {
                AbLoopSelectionPanel(
                    currentPositionMs = currentPositionMs,
                    loopPointA = loopPointA,
                    loopPointB = loopPointB,
                    onSetPointA = onSetLoopPointA,
                    onSetPointB = onSetLoopPointB,
                    onClearLoop = onClearLoop,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.PLAYLIST -> {
                PlaylistSelectionPanel(
                    playlistVideos = playlistVideos,
                    playlistItems = playlistItems,
                    currentIndex = currentEpisodeIndex,
                    totalEpisodes = totalEpisodes,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onSelectPlaylistItem = onSelectPlaylistItem,
                    onReorderPlaylist = onReorderPlaylist,
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            PlayerPanelType.PLAYER_LAYOUT -> {
                com.example.ui.components.PlayerLayoutSelectionPanel(
                    currentSeekbarStyle = seekbarStyle,
                    playerLayoutConfig = playerLayoutConfig,
                    isLandscape = isLandscape,
                    glassBlurTransparency = glassBlurTransparency,
                    onSelectSeekbarStyle = { selectedStyle ->
                        onSeekbarStyleChange?.invoke(selectedStyle)
                        notifyAction("Seekbar Style: ${selectedStyle.displayName}")
                    },
                    onConfigChange = { newConfig ->
                        onPlayerLayoutConfigChange?.invoke(newConfig)
                    },
                    onResetToDefault = {
                        onResetPlayerLayoutConfig?.invoke()
                        notifyAction("Player Layout Reset")
                    },
                    onDismissRequest = { dismissCurrentPanel() }
                )
            }
            null -> {
                // No panel active
            }
        }

        // -------------------------------------------------------------
        // PICTURE-IN-PICTURE MODE SURFACE
        // While the app is in system PiP mode, no control chrome should be
        // visible. This topmost layer consumes all touches: a single tap
        // simply toggles play/pause, matching standard PiP player behavior.
        // -------------------------------------------------------------
        if (isInPictureInPictureMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { onPlayPauseToggle() }
                    )
            )
        }
    }
}
}

/**
 * Clean floating circular button with frosty glass background and crisp dark icon.
 * Has NO background container behind it, so it floats cleanly on the video surface.
 */
@Composable
fun FloatingCircleButton(
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    customContent: (@Composable () -> Unit)? = null,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    glassBg: Color? = null,
    glassBorder: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveBg = getEffectiveGlassBg(glassBg)
    val effectiveBorder = getEffectiveGlassBorder(glassBorder)

    Box(
        modifier = modifier
            .size(size)
            .bounceClick(enabled = enabled, scaleDown = 0.86f, onClick = onClick)
            .clip(CircleShape)
            .background(effectiveBg)
            .border(1.dp, effectiveBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (customContent != null) {
            customContent()
        } else if (drawableRes != null) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = drawableRes),
                contentDescription = contentDescription,
                tint = GlassIconTint,
                modifier = Modifier.size(iconSize)
            )
        } else if (icon != null) {
            StyledIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = GlassIconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

fun formatTimeShort(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Interactive A-B Repeat Loop button that smoothly expands in-place towards the right.
 * Matches exact UI in Screenshot 1 (collapsed icon) and Screenshot 2 (expanded [ A (✕) B ] pill).
 * Features rich AccentGradient active pills for A and B upon touch.
 */
@Composable
fun AbLoopInlineMorphButton(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    loopPointA: Long?,
    loopPointB: Long?,
    currentPositionMs: Long,
    onSetPointA: (Long) -> Unit,
    onSetPointB: (Long) -> Unit,
    onClearLoop: () -> Unit,
    notifyInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) 152.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
        label = "AbInlineButtonWidth"
    )

    val isLoopActive = loopPointA != null && loopPointB != null
    val effectiveBg = getEffectiveGlassBg(null)
    val effectiveBorder = getEffectiveGlassBorder(null)

    Box(
        modifier = modifier
            .height(44.dp)
            .width(animatedWidth)
            .clip(RoundedCornerShape(22.dp))
            .background(effectiveBg)
            .border(
                1.dp,
                if (isLoopActive) Color(0x66FBBF24) else effectiveBorder,
                RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            // Collapsed: Clean Circular A-B button with standard matching tool icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .cardBounceClick(scaleDown = 0.88f) {
                        notifyInteraction()
                        onToggleExpand()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.lumora_ab_loop),
                    contentDescription = "A-B Repeat Loop",
                    tint = GlassIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // Expanded: In-line [ A   (✕)   B ] capsule (Screenshot 2) with AccentGradient style
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Point A touch button
                val isASet = loopPointA != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isASet) AccentGradient
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .cardBounceClick(scaleDown = 0.88f) {
                            notifyInteraction()
                            onSetPointA(currentPositionMs)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isASet) Color.White else GlassIconTint
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // 2. Middle Close / Reset (✕) circular button (matches light-grey circular badge in Screenshot 2)
                val isDark = isAppInDarkTheme()
                val isLowOpacity = LocalGlassBlurTransparency.current < 25f
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) {
                                if (isLowOpacity) Color(0x33FFFFFF)
                                else {
                                    val factor = (LocalGlassBlurTransparency.current / 100f).coerceIn(0.20f, 1.0f)
                                    if (factor >= 0.99f) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = factor)
                                }
                            } else {
                                if (isLowOpacity) Color(0x55FFFFFF)
                                else {
                                    val factor = (LocalGlassBlurTransparency.current / 100f).coerceIn(0.20f, 1.0f)
                                    if (factor >= 0.99f) Color(0xFFE5E7EB) else Color(0xFFE5E7EB).copy(alpha = factor)
                                }
                            }
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.25f)
                            else if (isLowOpacity) Color(0x66FFFFFF) else Color(0x33000000),
                            CircleShape
                        )
                        .cardBounceClick(scaleDown = 0.88f) {
                            notifyInteraction()
                            onClearLoop()
                            onToggleExpand()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark || isLowOpacity) Color.White else Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // 3. Point B touch button
                val isBSet = loopPointB != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isBSet) AccentGradient
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .cardBounceClick(scaleDown = 0.88f) {
                            notifyInteraction()
                            val targetB = if (loopPointA != null && currentPositionMs <= loopPointA) {
                                loopPointA + 2000L
                            } else {
                                currentPositionMs
                            }
                            onSetPointB(targetB)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBSet) Color.White else GlassIconTint
                    )
                }
            }
        }
    }
}

/**
 * Camera Frame Capture Inline Morphing Capsule matching screenshot.
 * Collapsed: Circular Camera Button.
 * Expanded: [ FastRewind (<< 1 Frame)  |  (📷 Capture)  |  FastForward (1 Frame >>) ]
 */
@Composable
fun CameraFrameCaptureInlineMorphButton(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTakeScreenshot: () -> Unit,
    notifyInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) 152.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
        label = "CameraInlineButtonWidth"
    )

    val isDark = isAppInDarkTheme()
    val effectiveBg = getEffectiveGlassBg(null)
    val effectiveBorder = getEffectiveGlassBorder(null)
    val isLowOpacity = LocalGlassBlurTransparency.current < 25f
    val buttonShape = if (isExpanded) RoundedCornerShape(22.dp) else CircleShape

    Box(
        modifier = modifier
            .height(44.dp)
            .width(animatedWidth)
            .clip(buttonShape)
            .background(effectiveBg)
            .border(
                1.dp,
                if (isExpanded) {
                    if (isDark) Color.White.copy(alpha = 0.35f)
                    else if (isLowOpacity) Color.White.copy(alpha = 0.45f)
                    else Color(0x440284C7)
                } else effectiveBorder,
                buttonShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded) {
            // Collapsed: Standard Camera Icon matching other 44dp circular buttons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .cardBounceClick(scaleDown = 0.86f) {
                        notifyInteraction()
                        onToggleExpand()
                    },
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                                imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Camera Frame Capture",
                    tint = GlassIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // Expanded: In-line [ <<   (📷)   >> ] capsule
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Previous Frame button (<<)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .cardBounceClick(scaleDown = 0.88f) {
                            notifyInteraction()
                            if (isPlaying) {
                                onPlayPauseToggle()
                            }
                            val target = (currentPositionMs - 42L).coerceAtLeast(0L)
                            onSeekTo(target)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                                imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Previous Frame (1 frame step back)",
                        tint = GlassIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // 2. Middle Circular Camera / Shutter Capture button
                // Single Tap -> Screenshot capture
                // Long Press (Hold) -> Collapse back to standard single Camera icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) {
                                if (isLowOpacity) Color(0x33FFFFFF) else Color(0xFF1E293B).copy(alpha = 0.85f)
                            } else {
                                if (isLowOpacity) Color(0x55FFFFFF)
                                else Color(0xFFF1F5F9).copy(alpha = 0.95f)
                            }
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.25f)
                            else if (isLowOpacity) Color(0x66FFFFFF) else Color(0x33000000),
                            CircleShape
                        )
                        .cardBounceCombinedClick(
                            scaleDown = 0.86f,
                            onLongClick = {
                                notifyInteraction()
                                onToggleExpand()
                            },
                            onClick = {
                                notifyInteraction()
                                onTakeScreenshot()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        drawableRes = R.drawable.lumora_capture,
                        contentDescription = "Frame Capture",
                        tint = if (isDark || isLowOpacity) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))

                // 3. Next Frame button (>>)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .cardBounceClick(scaleDown = 0.88f) {
                            notifyInteraction()
                            if (isPlaying) {
                                onPlayPauseToggle()
                            }
                            val target = (currentPositionMs + 42L).coerceAtMost(durationMs)
                            onSeekTo(target)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                                imageVector = Icons.Filled.FastForward,
                        contentDescription = "Next Frame (1 frame step forward)",
                        tint = GlassIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Vertical Gesture Indicator for Brightness (Left Side of Screen).
 * Squarish rounded-corner card with responsive dynamic thick/thin animated progress bar.
 */
@Composable
fun PlayerBrightnessPillHud(
    brightnessValue: Float,
    onBrightnessDelta: (Float) -> Unit,
    onInteraction: () -> Unit,
    isAdjusting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var isDirectDragging by remember { mutableStateOf(false) }
    val isActive = isAdjusting || isDirectDragging

    val percent = (brightnessValue * 100).roundToInt().coerceIn(0, 100)
    val fillFraction = brightnessValue.coerceIn(0.04f, 1.0f)

    val sunIcon = when {
        brightnessValue < 0.25f -> Icons.Filled.BrightnessLow
        brightnessValue < 0.70f -> Icons.Filled.BrightnessMedium
        else -> Icons.Filled.WbSunny
    }

    val trans = LocalGlassBlurTransparency.current
    val transFactor = (trans / 100f).coerceIn(0.08f, 1.0f)
    val cardBg = if (transFactor >= 0.99f) Color(0xFF131722) else Color(0xFF131722).copy(alpha = transFactor)
    val cardBorder = Color.White.copy(alpha = (transFactor * 0.22f).coerceIn(0.08f, 0.35f))
    val hudShape = RoundedCornerShape(18.dp)

    // Responsive dynamic thickness animation for middle bar
    val trackWidth by animateDpAsState(
        targetValue = if (isActive) 16.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BrightnessTrackWidth"
    )
    val trackCorner by animateDpAsState(
        targetValue = if (isActive) 8.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BrightnessTrackCorner"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(168.dp)
            .clip(hudShape)
            .background(cardBg)
            .border(1.dp, cardBorder, hudShape)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDirectDragging = true
                        AppHaptics.performGestureStart(view, haptic)
                        onInteraction()
                    },
                    onDragEnd = {
                        isDirectDragging = false
                    },
                    onDragCancel = {
                        isDirectDragging = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / 150f
                        onBrightnessDelta(delta)
                        AppHaptics.performTick(view, haptic)
                        onInteraction()
                    }
                )
            }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Percentage Label at top
            Text(
                text = "$percent%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp
            )

            // Vertical Progress Slider Track with dynamic thickness animation
            Box(
                modifier = Modifier
                    .width(trackWidth)
                    .height(92.dp)
                    .clip(RoundedCornerShape(trackCorner))
                    .background(Color(0x55000000)),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Filled Track with 3-stop System Gradient (Magenta -> Purple -> Sky Blue)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fillFraction)
                        .clip(RoundedCornerShape(trackCorner))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFEC4899), // Accent Magenta/Pink
                                    Color(0xFFA855F7), // Vibrant Purple
                                    Color(0xFF38BDF8)  // Sky Blue
                                )
                            )
                        )
                )
            }

            // Dynamic Brightness Icon at bottom
            StyledIcon(
                                imageVector = sunIcon,
                contentDescription = "Brightness",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Vertical Gesture Indicator for Volume (Right Side of Screen).
 * Squarish rounded-corner card with responsive dynamic thick/thin animated progress bar.
 */
@Composable
fun PlayerVolumePillHud(
    volumePercent: Float,
    volumeBoostCap: Int = 200,
    onVolumeDelta: (Float) -> Unit,
    onInteraction: () -> Unit,
    isAdjusting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var isDirectDragging by remember { mutableStateOf(false) }
    val isActive = isAdjusting || isDirectDragging

    val percent = (volumePercent * 100).roundToInt().coerceAtLeast(0)
    val isBoosted = volumePercent > 1.0f
    val maxBoostRatio = (volumeBoostCap / 100f).coerceIn(1.0f, 2.0f)
    val fillFraction = (volumePercent / maxBoostRatio).coerceIn(0.04f, 1.0f)

    val speakerIcon = when {
        volumePercent <= 0.001f -> Icons.AutoMirrored.Filled.VolumeOff
        volumePercent <= 0.45f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }

    val trans = LocalGlassBlurTransparency.current
    val transFactor = (trans / 100f).coerceIn(0.08f, 1.0f)
    val cardBg = if (transFactor >= 0.99f) Color(0xFF131722) else Color(0xFF131722).copy(alpha = transFactor)
    val cardBorder = if (isBoosted) {
        Color(0x88F43F5E)
    } else {
        Color.White.copy(alpha = (transFactor * 0.22f).coerceIn(0.08f, 0.35f))
    }
    val hudShape = RoundedCornerShape(18.dp)

    val iconTint = when {
        volumePercent <= 0.001f -> Color(0xFF94A3B8)
        isBoosted -> Color(0xFFF43F5E)
        else -> Color(0xFF38BDF8)
    }

    val trackBg = if (isBoosted) Color(0x444C1D95) else Color(0x55000000)

    val fillGradient = if (isBoosted) {
        listOf(
            Color(0xFFFDA4AF), // rose boost top
            Color(0xFFF43F5E), // coral rose
            Color(0xFFBE123C)  // deep energetic rose
        )
    } else {
        listOf(
            Color(0xFFEC4899), // Accent Magenta/Pink
            Color(0xFFA855F7), // Vibrant Purple
            Color(0xFF38BDF8)  // Sky Blue
        )
    }

    // Responsive dynamic thickness animation for middle bar
    val trackWidth by animateDpAsState(
        targetValue = if (isActive) 16.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VolumeTrackWidth"
    )
    val trackCorner by animateDpAsState(
        targetValue = if (isActive) 8.dp else 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VolumeTrackCorner"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(168.dp)
            .clip(hudShape)
            .background(cardBg)
            .border(1.dp, cardBorder, hudShape)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDirectDragging = true
                        AppHaptics.performGestureStart(view, haptic)
                        onInteraction()
                    },
                    onDragEnd = {
                        isDirectDragging = false
                    },
                    onDragCancel = {
                        isDirectDragging = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / 150f
                        onVolumeDelta(delta)
                        AppHaptics.performTick(view, haptic)
                        onInteraction()
                    }
                )
            }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Percentage Label at top (with rose highlight if > 100%)
            Text(
                text = "$percent%",
                color = if (isBoosted) Color(0xFFFDA4AF) else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp
            )

            // Vertical Progress Slider Track with dynamic thickness animation
            Box(
                modifier = Modifier
                    .width(trackWidth)
                    .height(92.dp)
                    .clip(RoundedCornerShape(trackCorner))
                    .background(trackBg),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Filled Track with 3-stop System Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fillFraction)
                        .clip(RoundedCornerShape(trackCorner))
                        .background(
                            Brush.verticalGradient(
                                colors = fillGradient
                            )
                        )
                )
            }

            // Dynamic Volume Icon at bottom
            StyledIcon(
                                imageVector = speakerIcon,
                contentDescription = "Volume",
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun drawPlayerScrubberLoopMarkers(
    drawScope: DrawScope,
    w: Float,
    centerY: Float,
    trackH: Float,
    durationMs: Long,
    loopPointA: Long?,
    loopPointB: Long?
) {
    if (durationMs <= 0) return
    val ptA = loopPointA
    val ptB = loopPointB
    if (ptA != null) {
        val fracA = (ptA.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val xA = w * fracA

        if (ptB != null && ptB > ptA) {
            val fracB = (ptB.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val xB = w * fracB
            // Golden yellow loop interval highlight
            drawScope.drawRoundRect(
                color = Color(0xCCFBBF24),
                topLeft = Offset(xA, centerY - trackH / 2f),
                size = Size((xB - xA).coerceAtLeast(2f), trackH),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
            // Bright Yellow Marker Line B
            drawScope.drawRoundRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(xB - 1.5f, centerY - 8f),
                size = Size(3f, 16f),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }

        // Bright Yellow Marker Line A
        drawScope.drawRoundRect(
            color = Color(0xFFFFD54F),
            topLeft = Offset(xA - 1.5f, centerY - 8f),
            size = Size(3f, 16f),
            cornerRadius = CornerRadius(1.5f, 1.5f)
        )
    }
}

private fun drawPlayerScrubberChapterMarkers(
    drawScope: DrawScope,
    w: Float,
    centerY: Float,
    trackH: Float,
    durationMs: Long,
    skipMarkers: List<com.example.player.ChapterSkipMarker>
) {
    if (durationMs <= 0 || skipMarkers.isEmpty()) return
    for (marker in skipMarkers) {
        val fracStart = (marker.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val fracEnd = (marker.endTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val startX = w * fracStart
        val endX = w * fracEnd
        val segmentW = (endX - startX).coerceAtLeast(3f)

        // Opening / Intro / Recap: Subtle yellow/orange highlighted segment (Color(0xEEF59E0B))
        // Ending / Outro / Preview: Blue highlighted segment (Color(0xEE38BDF8))
        val segColor = when (marker.type) {
            com.example.player.SkipMarkerType.OPENING,
            com.example.player.SkipMarkerType.RECAP,
            com.example.player.SkipMarkerType.PROLOGUE -> Color(0xDDF59E0B)
            else -> Color(0xDD38BDF8)
        }

        // Draw highlighted segment on seek bar track
        drawScope.drawRoundRect(
            color = segColor,
            topLeft = Offset(startX, centerY - (trackH * 1.15f) / 2f),
            size = Size(segmentW, trackH * 1.15f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Subtle boundary notch lines at start and end of chapter range
        drawScope.drawRoundRect(
            color = Color(0xEEFFFFFF),
            topLeft = Offset(startX, centerY - (trackH + 3f) / 2f),
            size = Size(1.5f, trackH + 3f),
            cornerRadius = CornerRadius(1f, 1f)
        )
        drawScope.drawRoundRect(
            color = Color(0xEEFFFFFF),
            topLeft = Offset((endX - 1.5f).coerceAtLeast(startX), centerY - (trackH + 3f) / 2f),
            size = Size(1.5f, trackH + 3f),
            cornerRadius = CornerRadius(1f, 1f)
        )
    }
}

private fun drawPlayerScrubberChapterBoundaries(
    drawScope: DrawScope,
    w: Float,
    centerY: Float,
    trackH: Float,
    durationMs: Long,
    chapters: List<com.example.player.PlayerVideoChapter>
) {
    if (durationMs <= 0 || chapters.isEmpty()) return
    with(drawScope) {
        val markerW = 1.5.dp.toPx()
        val markerH = (trackH + 3f).coerceAtLeast(6.dp.toPx())
        val markerColor = Color(0xB3FFFFFF) // Subtle semi-transparent white marker

        for (chapter in chapters) {
            val startMs = chapter.timeMs
            if (startMs > 500L && startMs < durationMs - 500L) {
                val frac = (startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val markerX = (w * frac - markerW / 2f).coerceIn(0f, w - markerW)
                drawRoundRect(
                    color = markerColor,
                    topLeft = Offset(markerX, centerY - markerH / 2f),
                    size = Size(markerW, markerH),
                    cornerRadius = CornerRadius(0.75.dp.toPx(), 0.75.dp.toPx())
                )
            }
        }
    }
}

/**
 * Chapter Pill Button with touch event consumption, single-tap to open chapter sheet,
 * and horizontal swipe gestures:
 * - Swipe Left -> Right: Jump to NEXT chapter (1 chapter forward)
 * - Swipe Right -> Left: Jump to PREVIOUS chapter (1 chapter backward)
 * - Single Tap / Click: Open Chapter Selection Dialog immediately
 * Fully consumes touch events to prevent pass-through to the background video gesture surface.
 */
@Composable
fun ChapterPillButton(
    chapterLabel: String,
    chapters: List<com.example.player.PlayerVideoChapter>,
    currentPositionMs: Long,
    onSeekToChapter: (com.example.player.PlayerVideoChapter) -> Unit,
    onOpenDialog: () -> Unit,
    notifyInteraction: () -> Unit,
    notifyAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val effectiveBg = getEffectiveGlassBg()
    val effectiveBorder = getEffectiveGlassBorder()
    val iconTint = if (isDark || LocalGlassBlurTransparency.current < 25f) Color(0xFF2DD4BF) else Color(0xFF0D9488)

    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // Keep updated state references so pointerInput(Unit) is never cancelled during playback
    val currentChaptersState by rememberUpdatedState(chapters)
    val currentPositionState by rememberUpdatedState(currentPositionMs)
    val onSeekToChapterState by rememberUpdatedState(onSeekToChapter)
    val onOpenDialogState by rememberUpdatedState(onOpenDialog)
    val notifyInteractionState by rememberUpdatedState(notifyInteraction)
    val notifyActionState by rememberUpdatedState(notifyAction)

    // Tactile press state for spring bounce micro-interaction
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ChapterButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(effectiveBg)
            .border(1.dp, effectiveBorder, RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                val swipeThresholdPx = 18.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    notifyInteractionState()

                    val pointerId = down.id
                    var totalDragX = 0f
                    var isSwipeTriggered = false

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            if (!change.pressed) {
                                // Touch released (UP event)
                                change.consume()
                                isPressed = false
                                if (!isSwipeTriggered) {
                                    // Single click / tap -> open Chapter Sheet immediately
                                    AppHaptics.performTick(view, haptic)
                                    notifyInteractionState()
                                    onOpenDialogState()
                                }
                                break
                            }

                            val dragAmount = change.position.x - change.previousPosition.x
                            totalDragX += dragAmount

                            if (!isSwipeTriggered && kotlin.math.abs(totalDragX) >= swipeThresholdPx) {
                                isSwipeTriggered = true
                                change.consume()
                                notifyInteractionState()
                                AppHaptics.performGestureThreshold(view, haptic)

                                val chList = currentChaptersState
                                val pos = currentPositionState

                                if (chList.isEmpty()) {
                                    notifyActionState("No chapters available in this video")
                                } else {
                                    val sorted = chList.sortedBy { it.timeMs }
                                    val currentIdx = sorted.indexOfLast { pos >= (it.timeMs - 500L) }.coerceAtLeast(0)

                                    if (totalDragX > 0f) {
                                        // User swiped LEFT -> RIGHT: Jump to NEXT chapter (Aage badhna)
                                        if (currentIdx < sorted.size - 1) {
                                            val next = sorted[currentIdx + 1]
                                            onSeekToChapterState(next)
                                            val title = if (next.title.isNotBlank()) next.title else "Chapter ${next.index + 1}"
                                            notifyActionState("Next Chapter: $title")
                                        } else {
                                            val last = sorted.last()
                                            val title = if (last.title.isNotBlank()) last.title else "Chapter ${last.index + 1}"
                                            notifyActionState("Already at last chapter: $title")
                                        }
                                    } else {
                                        // User swiped RIGHT -> LEFT: Jump to PREVIOUS chapter (Piche jana)
                                        if (currentIdx > 0) {
                                            val prev = sorted[currentIdx - 1]
                                            onSeekToChapterState(prev)
                                            val title = if (prev.title.isNotBlank()) prev.title else "Chapter ${prev.index + 1}"
                                            notifyActionState("Previous Chapter: $title")
                                        } else {
                                            val first = sorted[0]
                                            val title = if (first.title.isNotBlank()) first.title else "Chapter 1"
                                            if (pos > first.timeMs + 1500L) {
                                                onSeekToChapterState(first)
                                                notifyActionState("Chapter 1: $title")
                                            } else {
                                                notifyActionState("Already at first chapter: $title")
                                            }
                                        }
                                    }
                                }
                            } else if (isSwipeTriggered) {
                                change.consume()
                            }
                        }
                    } finally {
                        isPressed = false
                    }
                }
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.BookmarkBorder,
                contentDescription = "Chapters",
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = chapterLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GlassIconTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlayerScrubberBar(
    currentPositionMs: Long,
    durationMs: Long,
    seekbarStyle: com.example.ui.state.SeekbarStyle,
    loopPointA: Long?,
    loopPointB: Long?,
    chapters: List<com.example.player.PlayerVideoChapter> = emptyList(),
    skipMarkers: List<com.example.player.ChapterSkipMarker> = emptyList(),
    onSeekTo: (Long) -> Unit,
    notifyInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var lastScrubberHapticSec by remember { mutableLongStateOf(-1L) }
    val scrubberProgressFraction by remember(currentPositionMs, durationMs) {
        derivedStateOf {
            if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        }
    }
    val currentScrubberTimeText by remember(currentPositionMs) {
        derivedStateOf { formatTimeShort(currentPositionMs) }
    }
    val durationScrubberTimeText by remember(durationMs) {
        derivedStateOf { formatTimeShort(durationMs) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentScrubberTimeText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Custom sleek scrubber track dynamically rendering selected SeekbarStyle
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(durationMs) {
                        // Drag-to-scrub and tap-to-seek combined in one
                        // pointerInput block so both detectors read the same
                        // pointer stream instead of racing across two separate
                        // stacked pointerInput modifiers (that race is what
                        // made the video seek bar sometimes miss the first
                        // touch/drag).
                        kotlinx.coroutines.coroutineScope {
                            launch {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        AppHaptics.performGestureStart(view, haptic)
                                    },
                                    onHorizontalDrag = { change, _ ->
                                        change.consume()
                                        val newFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        val targetMs = (newFraction * durationMs).toLong()
                                        val currentSec = targetMs / 1000L
                                        if (kotlin.math.abs(currentSec - lastScrubberHapticSec) >= 1L) {
                                            lastScrubberHapticSec = currentSec
                                            AppHaptics.performTick(view, haptic)
                                        }
                                        onSeekTo(targetMs)
                                        notifyInteraction()
                                    }
                                )
                            }
                            launch {
                                detectTapGestures { offset ->
                                    AppHaptics.performGestureThreshold(view, haptic)
                                    val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    val targetMs = (newFraction * durationMs).toLong()
                                    onSeekTo(targetMs)
                                    notifyInteraction()
                                }
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f
                val activeW = w * scrubberProgressFraction

                when (seekbarStyle) {
                    com.example.ui.state.SeekbarStyle.NORMAL -> {
                        // Slim line with round circle thumb
                        val trackH = 3.5.dp.toPx()
                        val corner = CornerRadius(trackH / 2f, trackH / 2f)

                        // Background track
                        drawRoundRect(
                            color = Color(0x40FFFFFF),
                            topLeft = Offset(0f, centerY - trackH / 2f),
                            size = Size(w, trackH),
                            cornerRadius = corner
                        )

                        // Chapter Skip Markers
                        drawPlayerScrubberChapterMarkers(this, w, centerY, trackH, durationMs, skipMarkers)

                        // Active progress
                        if (activeW > 0) {
                            drawRoundRect(
                                color = Color(0xFF0284C7),
                                topLeft = Offset(0f, centerY - trackH / 2f),
                                size = Size(activeW, trackH),
                                cornerRadius = corner
                            )
                        }

                        // Chapter boundary markers
                        drawPlayerScrubberChapterBoundaries(this, w, centerY, trackH, durationMs, chapters)

                        // A-B Repeat Loop markers
                        drawPlayerScrubberLoopMarkers(this, w, centerY, trackH, durationMs, loopPointA, loopPointB)

                        // Circular thumb with halo
                        val thumbRadius = 6.5.dp.toPx()
                        val thumbX = activeW.coerceIn(thumbRadius, w - thumbRadius)
                        drawCircle(
                            color = Color(0x400284C7),
                            radius = thumbRadius + 3.dp.toPx(),
                            center = Offset(thumbX, centerY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius,
                            center = Offset(thumbX, centerY)
                        )
                        drawCircle(
                            color = Color(0xFF0284C7),
                            radius = 3.dp.toPx(),
                            center = Offset(thumbX, centerY)
                        )
                    }

                    com.example.ui.state.SeekbarStyle.STANDARD -> {
                        // Standard medium rounded track with vertical pin divider
                        val trackH = 6.dp.toPx()
                        val corner = CornerRadius(trackH / 2f, trackH / 2f)

                        // Background track
                        drawRoundRect(
                            color = Color(0x330D9488),
                            topLeft = Offset(0f, centerY - trackH / 2f),
                            size = Size(w, trackH),
                            cornerRadius = corner
                        )

                        // Chapter Skip Markers
                        drawPlayerScrubberChapterMarkers(this, w, centerY, trackH, durationMs, skipMarkers)

                        // Active teal progress
                        if (activeW > 0) {
                            drawRoundRect(
                                color = Color(0xFF0D9488),
                                topLeft = Offset(0f, centerY - trackH / 2f),
                                size = Size(activeW, trackH),
                                cornerRadius = corner
                            )
                        }

                        // Chapter boundary markers
                        drawPlayerScrubberChapterBoundaries(this, w, centerY, trackH, durationMs, chapters)

                        // A-B Repeat Loop markers
                        drawPlayerScrubberLoopMarkers(this, w, centerY, trackH, durationMs, loopPointA, loopPointB)

                        // Thumb indicator bar (vertical bright cyan rectangle)
                        val pinW = 4.dp.toPx()
                        val pinH = 14.dp.toPx()
                        val thumbX = (activeW - pinW / 2f).coerceIn(0f, w - pinW)
                        drawRoundRect(
                            color = Color(0xFF2DD4BF),
                            topLeft = Offset(thumbX, centerY - pinH / 2f),
                            size = Size(pinW, pinH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }

                    com.example.ui.state.SeekbarStyle.WAVY -> {
                        // Sine Wave Progress Track with vertical pin
                        val waveAmplitude = 4.dp.toPx()
                        val waveLength = 28.dp.toPx()
                        val strokeW = 3.5.dp.toPx()

                        // Inactive straight track from activeW to end
                        if (activeW < w) {
                            drawLine(
                                color = Color(0x40FFFFFF),
                                start = Offset(activeW, centerY),
                                end = Offset(w, centerY),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }

                        // Chapter Skip Markers
                        drawPlayerScrubberChapterMarkers(this, w, centerY, strokeW * 1.8f, durationMs, skipMarkers)

                        // Active wavy sine wave path
                        if (activeW > 0) {
                            val wavePath = Path()
                            wavePath.moveTo(0f, centerY)
                            var currentX = 0f
                            val step = 2.dp.toPx()
                            while (currentX <= activeW) {
                                val waveY = centerY + (kotlin.math.sin((currentX / waveLength) * 2 * Math.PI) * waveAmplitude).toFloat()
                                wavePath.lineTo(currentX, waveY)
                                currentX += step
                            }
                            drawPath(
                                path = wavePath,
                                color = Color(0xFF0284C7),
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }

                        // Chapter boundary markers
                        drawPlayerScrubberChapterBoundaries(this, w, centerY, strokeW * 2f, durationMs, chapters)

                        // A-B Repeat Loop markers
                        drawPlayerScrubberLoopMarkers(this, w, centerY, strokeW * 2f, durationMs, loopPointA, loopPointB)

                        // Pin thumb divider
                        val pinW = 3.5.dp.toPx()
                        val pinH = 18.dp.toPx()
                        val thumbX = (activeW - pinW / 2f).coerceIn(0f, w - pinW)
                        drawRoundRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(thumbX, centerY - pinH / 2f),
                            size = Size(pinW, pinH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }

                    com.example.ui.state.SeekbarStyle.THICK -> {
                        // Extra thick rounded pill track
                        val trackH = 12.dp.toPx()
                        val corner = CornerRadius(trackH / 2f, trackH / 2f)

                        // Background track
                        drawRoundRect(
                            color = Color(0x330284C7),
                            topLeft = Offset(0f, centerY - trackH / 2f),
                            size = Size(w, trackH),
                            cornerRadius = corner
                        )

                        // Chapter Skip Markers
                        drawPlayerScrubberChapterMarkers(this, w, centerY, trackH, durationMs, skipMarkers)

                        // Active progress
                        if (activeW > 0) {
                            drawRoundRect(
                                color = Color(0xFF0284C7),
                                topLeft = Offset(0f, centerY - trackH / 2f),
                                size = Size(activeW, trackH),
                                cornerRadius = corner
                            )
                        }

                        // Chapter boundary markers
                        drawPlayerScrubberChapterBoundaries(this, w, centerY, trackH, durationMs, chapters)

                        // A-B Repeat Loop markers
                        drawPlayerScrubberLoopMarkers(this, w, centerY, trackH, durationMs, loopPointA, loopPointB)

                        // Vertical divider pin
                        val pinW = 4.dp.toPx()
                        val pinH = 18.dp.toPx()
                        val thumbX = (activeW - pinW / 2f).coerceIn(0f, w - pinW)
                        drawRoundRect(
                            color = Color(0xFF38BDF8),
                            topLeft = Offset(thumbX, centerY - pinH / 2f),
                            size = Size(pinW, pinH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }

                    com.example.ui.state.SeekbarStyle.SLIM -> {
                        // Minimalist continuous rounded pill
                        val trackH = 7.dp.toPx()
                        val corner = CornerRadius(trackH / 2f, trackH / 2f)

                        // Background track
                        drawRoundRect(
                            color = Color(0x30FFFFFF),
                            topLeft = Offset(0f, centerY - trackH / 2f),
                            size = Size(w, trackH),
                            cornerRadius = corner
                        )

                        // Chapter Skip Markers
                        drawPlayerScrubberChapterMarkers(this, w, centerY, trackH, durationMs, skipMarkers)

                        // Active progress
                        if (activeW > 0) {
                            drawRoundRect(
                                color = Color(0xFF0284C7),
                                topLeft = Offset(0f, centerY - trackH / 2f),
                                size = Size(activeW, trackH),
                                cornerRadius = corner
                            )
                        }

                        // Chapter boundary markers
                        drawPlayerScrubberChapterBoundaries(this, w, centerY, trackH, durationMs, chapters)

                        // A-B Repeat Loop markers
                        drawPlayerScrubberLoopMarkers(this, w, centerY, trackH, durationMs, loopPointA, loopPointB)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = durationScrubberTimeText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

