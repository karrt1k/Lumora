package com.example.player


import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.ui.state.HwAccelMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.util.AppStorageManager
import com.example.util.SubtitleSessionMemory
import com.example.util.MkvManagerEngine
import com.example.ai.AiFeaturesSettings
import com.example.ai.SubtitleTranslationService
import com.example.ai.TranslateLanguages
import android.os.ParcelFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class PlayerMediaTrack(
    val id: Int,
    val type: String, // "audio" or "sub"
    val title: String,
    val language: String,
    val codec: String,
    val isSelected: Boolean,
    val isExternal: Boolean = false,
    val originalFilename: String = "",
    val isSecondarySelected: Boolean = false
)

data class PlayerVideoChapter(
    val index: Int,
    val title: String,
    val timeMs: Long,
    val formattedTime: String
)

class MpvPlayerController {
    // Guards against use-after-release: once the player screen is exited we tear this
    // controller down immediately (see release()). Any late/queued callback or coroutine
    // tick that still fires afterwards (surface callbacks, polling loop, pending seeks)
    // must no-op instead of touching a destroyed native mpv session, which is what caused
    // crashes when the user tapped something immediately after leaving the player.
    @Volatile
    private var isReleased = false
    private var mpvView: VideoPlayerMpvView? = null
    private var pollingJob: Job? = null
    private var currentFilePath: String? = null
    private var subtitlesLoadedOnce = false
    private val loadedExternalSubtitles = mutableListOf<String>()
    private val externalSubtitlesMap = mutableMapOf<String, String>()
    private val subtitleTrackSourcePaths = mutableMapOf<Int, String>()
    private val subtitleTrackExtractorIndices = mutableMapOf<Int, Int>()
    private val loadedExternalAudios = mutableListOf<String>()
    private val externalAudiosMap = mutableMapOf<String, String>()
    private val externalAudiosLangMap = mutableMapOf<String, String>()
    private var lastSurfaceRefreshTimestamp = 0L

    var preferredSubtitleTrackNumber: Int? = null

    /**
     * While true no subtitle is auto-selected (neither by mpv's default pick nor by the fallback in
     * refreshTracksAndChapters): a saved per-video subtitle selection is about to be restored and
     * must not be overridden or flashed over by a default track. Always released by the caller.
     */
    @Volatile
    var holdSubtitleAutoSelect: Boolean = false

    fun releaseSubtitleAutoSelectHold() {
        holdSubtitleAutoSelect = false
    }

    // Track discovery bookkeeping (see startPolling / discoverTracksPromptly)
    @Volatile private var trackDiscoveryStartedAt = 0L
    @Volatile private var lastTrackListCount = -1
    @Volatile private var lastTrackRefreshAt = 0L

    // Live subtitle preferences supplied by the Settings screen. These are applied
    // before every new media load and can also be changed while playback is active.
    private var subtitleAutoLoadExternal = true
    private var subtitleOverrideAssSsa = false
    private var subtitleScaleWithWindow = true
    private var subtitleFontDirectory: String? = null
    private var subtitleFontFamily: String? = null
    private var subtitleScale = 1.0
    private var subtitlePosition = 100.0
    private var subtitleBlendWithVideo = false
    private var subtitleBold = false
    private var subtitleItalic = false
    private var subtitleUnderline = false
    private var subtitleAlignment = "center"
    private var subtitleFontSize = 52.0
    private var subtitleBorderStyle = "outline_shadow"
    private var subtitleBorderSize = 3.0
    private var subtitleShadowOffset = 1.5
    private var subtitleTextColor = "1.0/1.0/1.0/1.0"
    private var subtitleBorderColor = "0.0/0.0/0.0/1.0"
    private var subtitleBackgroundColor = "0.0/0.0/0.0/0.60"
    private var subtitleShadowColor = "0.0/0.0/0.0/0.75"
    private var subtitleUsesGeneratedAss = false
    private var lastAppliedFontDirectory: String? = null

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(60000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0)
    val playbackSpeed: StateFlow<Double> = _playbackSpeed.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _eofReached = MutableStateFlow(false)
    val eofReached: StateFlow<Boolean> = _eofReached.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _droppedFrameCount = MutableStateFlow(0)
    val droppedFrameCount: StateFlow<Int> = _droppedFrameCount.asStateFlow()

    private val _estimatedFps = MutableStateFlow(0.0)
    val estimatedFps: StateFlow<Double> = _estimatedFps.asStateFlow()

    private val _isHwAccelActive = MutableStateFlow(true)
    val isHwAccelActive: StateFlow<Boolean> = _isHwAccelActive.asStateFlow()

    private val _videoWidth = MutableStateFlow(0)
    val videoWidth: StateFlow<Int> = _videoWidth.asStateFlow()

    private val _videoHeight = MutableStateFlow(0)
    val videoHeight: StateFlow<Int> = _videoHeight.asStateFlow()

    private val _performanceStatus = MutableStateFlow("MediaCodec HW Accelerated")
    val performanceStatus: StateFlow<String> = _performanceStatus.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<PlayerMediaTrack>>(emptyList())
    val audioTracks: StateFlow<List<PlayerMediaTrack>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<PlayerMediaTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<PlayerMediaTrack>> = _subtitleTracks.asStateFlow()

    private val _secondarySubtitleTrackId = MutableStateFlow(0)
    val secondarySubtitleTrackId: StateFlow<Int> = _secondarySubtitleTrackId.asStateFlow()

    private val _chapters = MutableStateFlow<List<PlayerVideoChapter>>(emptyList())
    val chapters: StateFlow<List<PlayerVideoChapter>> = _chapters.asStateFlow()

    private val _lastCapturedFrame = MutableStateFlow<Bitmap?>(null)
    val lastCapturedFrame: StateFlow<Bitmap?> = _lastCapturedFrame.asStateFlow()

    private var lastDroppedFrames = 0
    private var currentHwMode: HwAccelMode = HwAccelMode.FORCE
    private var pendingFilePath: String? = null
    private var pendingStartPositionMs: Long = 0L
    private var isSurfaceReady = false

    @Volatile
    var isResumeSeekVerified: Boolean = true
        private set
    @Volatile
    private var targetResumePositionMs: Long = 0L

    private var currentAudioDelayMs: Long = 0L
    private var currentSubtitleDelayMs: Long = 0L
    private var controllerScope: CoroutineScope? = null

    /**
     * Captures the live hardware surface buffer into state memory.
     * Used across screen rotation transitions to eliminate black screen flashes.
     */
    fun captureCurrentFrame(onComplete: ((Bitmap?) -> Unit)? = null) {
        val view = mpvView
        if (view != null && view.holder.surface?.isValid == true) {
            view.captureSurfaceFrame { bmp ->
                if (bmp != null) {
                    _lastCapturedFrame.value = bmp
                }
                onComplete?.invoke(bmp ?: _lastCapturedFrame.value)
            }
        } else {
            onComplete?.invoke(_lastCapturedFrame.value)
        }
    }

    /**
     * Shifts the audio track relative to the video timeline.
     * Positive [delayMs] delays the audio (plays later); negative advances it (plays earlier).
     * Safe to call at any time; if the native surface isn't ready yet, the value is
     * cached and re-applied automatically once the video (re)loads.
     */
    fun setAudioDelay(delayMs: Long) {
        currentAudioDelayMs = delayMs
        val view = mpvView ?: return
        try {
            view.mpv.setPropertyDouble("audio-delay", delayMs / 1000.0)
        } catch (_: Throwable) {}
    }

    fun setSubtitleDelay(delayMs: Long) {
        currentSubtitleDelayMs = delayMs
        val view = mpvView ?: return
        try {
            view.mpv.setPropertyDouble("sub-delay", delayMs / 1000.0)
        } catch (_: Throwable) {}
    }

    fun setHwAccelMode(mode: HwAccelMode) {
        currentHwMode = mode
        val view = mpvView ?: return
        try {
            view.mpv.setPropertyString("hwdec", resolveHwdecValue())
            _isHwAccelActive.value = (mode != HwAccelMode.DISABLE)
            _performanceStatus.value = when (mode) {
                HwAccelMode.FORCE -> "MediaCodec HW Forced"
                HwAccelMode.PREFER -> "MediaCodec Auto/Copy Active"
                HwAccelMode.DISABLE -> "Hardware Dec Disabled (SW CPU)"
            }
        } catch (_: Throwable) {}
    }

    fun attach(view: VideoPlayerMpvView, scope: CoroutineScope) {
        if (isReleased) return
        mpvView = view
        controllerScope = scope
        view.onSurfaceCreatedCallback = {
            onSurfaceCreated()
        }
        view.onSurfaceChangedCallback = {
            // Screen orientation change or resize: MPV handles viewport resize natively.
            // Only attach surface if it was not ready yet.
            if (!isSurfaceReady) {
                try {
                    val surface = mpvView?.holder?.surface
                    if (surface != null && surface.isValid) {
                        mpvView?.mpv?.attachSurface(surface)
                        isSurfaceReady = true
                    }
                } catch (_: Throwable) {}
            }
        }
        view.onSurfaceDestroyedCallback = {
            isSurfaceReady = false
        }
        startPolling(scope)

        if (view.holder.surface?.isValid == true) {
            onSurfaceCreated()
        } else {
            val path = pendingFilePath ?: currentFilePath
            if (!path.isNullOrBlank()) {
                loadVideoInternal(path, pendingStartPositionMs)
            }
        }
    }

    private var lastActiveSubtitleTrackId: Int = 1

    private fun onSurfaceCreated() {
        if (isReleased) return
        isSurfaceReady = true
        val path = pendingFilePath ?: currentFilePath
        if (!path.isNullOrBlank() && mpvView != null) {
            if (currentFilePath == path && _durationMs.value > 0L && pendingStartPositionMs == 0L) {
                // Video is already actively loaded and playing; seamlessly attach surface without reloading file
                try {
                    val surface = mpvView?.holder?.surface
                    if (surface != null && surface.isValid) {
                        mpvView?.mpv?.attachSurface(surface)
                    }
                } catch (_: Throwable) {}
            } else {
                val targetPos = if (pendingStartPositionMs > 500L) pendingStartPositionMs else targetResumePositionMs
                pendingFilePath = null
                pendingStartPositionMs = 0L
                loadVideoInternal(path, targetPos)
            }
        }
    }

    private var wasPlayingBeforeStop: Boolean = false

    fun onStart() {
        // Initial entry is already loaded by the surface callback. Do not trigger a
        // second load here, which caused the startup black/blur flash.
        if (!wasPlayingBeforeStop) return
        try {
            playerScope.launch {
                delay(40)
                val view = mpvView ?: return@launch
                try {
                    view.mpv.setPropertyBoolean("pause", false)
                    _isPlaying.value = true
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }

    fun onStop() {
        try {
            wasPlayingBeforeStop = _isPlaying.value
            val view = mpvView ?: return
            try {
                view.mpv.setPropertyBoolean("pause", true)
            } catch (_: Throwable) {}
            _isPlaying.value = false
        } catch (_: Throwable) {}
    }

    fun onPause() {
        try {
            val view = mpvView ?: return
            try {
                view.mpv.setPropertyBoolean("pause", true)
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    fun onResume() {
        // Only rebuild the native surface after a genuine background/foreground cycle.
        if (!wasPlayingBeforeStop) return
        try {
            playerScope.launch {
                delay(50)
                refreshSurface()
                wasPlayingBeforeStop = false
            }
        } catch (_: Throwable) {}
    }

    fun refreshSurface() {
        val now = System.currentTimeMillis()
        if (now - lastSurfaceRefreshTimestamp < 350L) {
            return
        }
        lastSurfaceRefreshTimestamp = now

        try {
            val view = mpvView ?: return
            val path = currentFilePath ?: pendingFilePath ?: return
            val isFdOrStream = path.startsWith("fd://") || path.startsWith("http://") || path.startsWith("https://") || path.startsWith("rtsp://")
            if (!isFdOrStream && !File(path).exists()) return

            val surface = view.holder.surface
            if (surface == null || !surface.isValid) {
                return
            }

            val currentPos = _currentPositionMs.value
            val wasPlaying = _isPlaying.value
            val currentSpeed = _playbackSpeed.value
            val activeAid = _audioTracks.value.firstOrNull { it.isSelected }?.id
            val activeSid = _subtitleTracks.value.firstOrNull { it.isSelected }?.id ?: lastActiveSubtitleTrackId

            // 1. Reconnect native surface and configure video output
            try {
                view.mpv.attachSurface(surface)
            } catch (_: Throwable) {}

            view.mpv.setOptionString("force-window", "yes")
            view.mpv.setPropertyString("vo", "gpu")
            view.mpv.setPropertyString("hwdec", resolveHwdecValue())
            view.mpv.setPropertyString("framedrop", "vo")
            _isBuffering.value = true

            // 2. Re-instantiate media stream decoder cleanly on the new Surface
            val startSec = (currentPos / 1000.0).coerceAtLeast(0.0)
            if (currentPos > 500L) {
                targetResumePositionMs = currentPos
                isResumeSeekVerified = false
                try {
                    view.mpv.setOptionString("start", "$startSec")
                } catch (_: Throwable) {}
                try {
                    view.mpv.setPropertyString("start", "+$startSec")
                } catch (_: Throwable) {}
            }

            resetAdvancedAssBindings()
            try {
                view.mpv.command("loadfile", path, "replace")
            } catch (_: Throwable) {
                view.playFile(path)
            }

            view.mpv.setPropertyDouble("speed", currentSpeed.coerceIn(0.25, 4.0))
            view.mpv.setPropertyString("sub-ass", "yes")
            view.mpv.setPropertyBoolean("sub-visibility", true)
            view.mpv.setPropertyString("sub-ass-override", if (subtitleUsesGeneratedAss || subtitleOverrideAssSsa) "force" else "no")
            view.mpv.setPropertyBoolean("embeddedfonts", true)
            view.mpv.setPropertyBoolean("pause", !wasPlaying)
            _isPlaying.value = wasPlaying

            // 3. Re-inject external subtitles if any were loaded
            loadedExternalSubtitles.forEach { subPath ->
                val isSubStream = subPath.startsWith("http://") || subPath.startsWith("https://")
                if (isSubStream || File(subPath).exists()) {
                    try {
                        val subTitle = externalSubtitlesMap[subPath] ?: "Subtitle"
                        view.mpv.command("sub-add", subPath, "auto", subTitle)
                    } catch (_: Throwable) {}
                }
            }
            loadedExternalAudios.forEach { audioPath ->
                val isAudioStream = audioPath.startsWith("http://") || audioPath.startsWith("https://")
                if (isAudioStream || File(audioPath).exists()) {
                    try {
                        val audioTitle = externalAudiosMap[audioPath] ?: "Audio"
                        view.mpv.command("audio-add", audioPath, "auto", audioTitle)
                    } catch (_: Throwable) {}
                }
            }

            // 4. Fine seek & track restoration
            playerScope.launch {
                if (currentPos > 500L) {
                    var verified = false
                    val sec = currentPos / 1000.0
                    repeat(8) { attempt ->
                        if (verified || isReleased) return@repeat
                        delay(if (attempt == 0) 100L else 150L)
                        try {
                            val v = mpvView ?: return@repeat
                            val realPosSec = v.mpv.getPropertyDouble("time-pos") ?: -1.0
                            val realPosMs = if (realPosSec >= 0.0) (realPosSec * 1000.0).toLong() else -1L
                            if (realPosMs >= 0L && kotlin.math.abs(realPosMs - currentPos) <= 2500L) {
                                verified = true
                                isResumeSeekVerified = true
                                _currentPositionMs.value = realPosMs
                            } else {
                                try {
                                    v.mpv.command("seek", "$sec", "absolute+exact")
                                } catch (_: Throwable) {
                                    v.mpv.command("seek", "$sec", "absolute")
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                    isResumeSeekVerified = true
                }
                try {
                    val v = mpvView ?: return@launch
                    if (activeAid != null && activeAid > 0) {
                        try { v.mpv.setPropertyInt("aid", activeAid) } catch (_: Throwable) {}
                    }
                    if (activeSid > 0) {
                        try { v.mpv.setPropertyInt("sid", activeSid) } catch (_: Throwable) {}
                    }
                    v.mpv.setPropertyBoolean("pause", !wasPlaying)
                    _isPlaying.value = wasPlaying
                } catch (_: Throwable) {}
                scheduleAdvancedAssSync(900L)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun startPolling(scope: CoroutineScope) {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val view = mpvView
                    if (!isReleased && view != null && view.holder.surface?.isValid == true) {
                        val posSec = view.mpv.getPropertyDouble("time-pos")
                        val durSec = view.mpv.getPropertyDouble("duration")
                        val paused = view.mpv.getPropertyBoolean("pause") ?: true

                        if (posSec != null && posSec >= 0.0) {
                            val realMs = (posSec * 1000.0).toLong()
                            if (!isResumeSeekVerified && targetResumePositionMs > 500L) {
                                if (kotlin.math.abs(realMs - targetResumePositionMs) <= 2500L || realMs > 500L) {
                                    isResumeSeekVerified = true
                                    _currentPositionMs.value = realMs
                                }
                                // Keep target position displayed until first positive position is reached
                            } else {
                                _currentPositionMs.value = realMs
                            }
                            val frameWidth = view.mpv.getPropertyInt("video-params/dw")
                                ?: view.mpv.getPropertyInt("dwidth") ?: 0
                            val frameHeight = view.mpv.getPropertyInt("video-params/dh")
                                ?: view.mpv.getPropertyInt("dheight") ?: 0
                            if (posSec >= 0.0) {
                                _isBuffering.value = false
                            }
                        }
                        if (durSec != null && durSec > 0.0) {
                            _durationMs.value = (durSec * 1000.0).toLong()
                        }
                        _isPlaying.value = !paused

                        val eof = try {
                            view.mpv.getPropertyBoolean("eof-reached") ?: false
                        } catch (_: Throwable) { false }
                        if (eof) {
                            _eofReached.value = true
                        }

                        val vw = view.mpv.getPropertyInt("dwidth")
                            ?: view.mpv.getPropertyInt("video-params/dw")
                            ?: view.mpv.getPropertyInt("video-params/w")
                            ?: 0
                        val vh = view.mpv.getPropertyInt("dheight")
                            ?: view.mpv.getPropertyInt("video-params/dh")
                            ?: view.mpv.getPropertyInt("video-params/h")
                            ?: 0
                        if (vw > 0 && vh > 0) {
                            if (_videoWidth.value != vw) _videoWidth.value = vw
                            if (_videoHeight.value != vh) _videoHeight.value = vh
                        }

                        // Performance Monitor: Check dropped frames & hardware acceleration status
                        try {
                            val dropped = view.mpv.getPropertyInt("frame-drop-count")
                                ?: view.mpv.getPropertyInt("vo-drop-frame-count")
                                ?: view.mpv.getPropertyInt("decoder-frame-drop-count")
                                ?: 0
                            val estFps = view.mpv.getPropertyDouble("container-fps")
                                ?: view.mpv.getPropertyDouble("estimated-vf-fps")
                                ?: 0.0
                            val hwdec = view.mpv.getPropertyString("hwdec-current") ?: "mediacodec"

                            _droppedFrameCount.value = dropped
                            if (estFps > 0.0) {
                                _estimatedFps.value = estFps
                            }
                            val isHw = hwdec != "no" && hwdec.isNotBlank()
                            _isHwAccelActive.value = isHw

                            if (dropped > lastDroppedFrames && !paused) {
                                val delta = dropped - lastDroppedFrames
                                if (delta >= 2) {
                                    view.mpv.setPropertyString("hwdec", if (lastAppliedVfVideoFilter.isNotEmpty()) HwAccelMode.PREFER.mpvValue else "mediacodec")
                                    view.mpv.setPropertyString("framedrop", "vo")
                                    _performanceStatus.value = "Frame drops ($delta) resolved via MediaCodec HW alignment"
                                }
                            } else {
                                _performanceStatus.value = if (isHw) "MediaCodec HW Accelerated" else "Software Decoder Active"
                            }
                            lastDroppedFrames = dropped
                        } catch (_: Throwable) {
                            // Non-critical metric error
                        }

                        // Update tracks/chapters: immediately whenever mpv's track list changes
                        // (embedded tracks discovered, external subtitle/audio added), every 400 ms
                        // during the first seconds after a load, then every 2 s. Frame snapshot
                        // keeps its own ~2 s cadence.
                        try {
                            val now = System.currentTimeMillis()
                            val trackCount = try { view.mpv.getPropertyInt("track-list/count") } catch (_: Throwable) { null } ?: 0
                            val sinceLoad = now - trackDiscoveryStartedAt
                            val interval = if (sinceLoad in 0L..6000L) 400L else 2000L
                            if (trackCount != lastTrackListCount || now - lastTrackRefreshAt >= interval) {
                                lastTrackListCount = trackCount
                                lastTrackRefreshAt = now
                                refreshTracksAndChapters()
                            }
                            if (now % 2000 < 150 && view.holder.surface?.isValid == true) {
                                captureCurrentFrame()
                            }
                        } catch (_: Throwable) {}
                    }
                } catch (t: Throwable) {
                    // Ignore transient polling errors
                }
                delay(100)
            }
        }
    }

    private val playerScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    /**
     * Suspends until mpv has actually opened the CURRENT file (its `path` is set and it reports
     * tracks), i.e. the moment `sub-add`/`audio-add` and track discovery become possible.
     * Returns false on timeout. Replaces guessing with fixed sleeps, which made subtitle restore
     * and track lists appear late or only "sometimes".
     */
    suspend fun awaitFileLoaded(timeoutMs: Long = 15000L): Boolean {
        val started = System.currentTimeMillis()
        val expected = currentFilePath
        while (!isReleased) {
            val elapsed = System.currentTimeMillis() - started
            if (elapsed > timeoutMs) return false
            val ready = withMpvLock(false) { v ->
                val playing = try { v.mpv.getPropertyString("path") } catch (_: Throwable) { null }
                val count = try { v.mpv.getPropertyInt("track-list/count") } catch (_: Throwable) { null } ?: 0
                val samePath = playing != null && (
                    expected == null || playing == expected ||
                        playing.removePrefix("file://") == expected.removePrefix("file://")
                    )
                // After 1.5 s accept any opened file, in case mpv reports the path differently.
                count > 0 && playing != null && (samePath || elapsed > 1500L)
            }
            if (ready) return true
            delay(20L)
        }
        return false
    }

    private suspend fun discoverTracksPromptly() {
        awaitFileLoaded(20000L)
        if (isReleased) return
        refreshTracksAndChapters()
        delay(150L)
        refreshTracksAndChapters()
        delay(600L)
        refreshTracksAndChapters()
    }

    fun loadVideo(filePath: String, startPositionMs: Long = 0L) {
        if (filePath.isBlank()) {
            _errorMessage.value = "Invalid video file path"
            return
        }
        val isFdOrStream = filePath.startsWith("fd://") || filePath.startsWith("http://") || filePath.startsWith("https://") || filePath.startsWith("rtsp://")
        if (!isFdOrStream) {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                _errorMessage.value = "Video file does not exist or is inaccessible"
                return
            }
        }
        _errorMessage.value = null
        _isBuffering.value = true
        if (currentFilePath != filePath) {
            loadedExternalSubtitles.clear()
            loadedExternalAudios.clear()
            externalAudiosMap.clear()
            externalAudiosLangMap.clear()
            SubtitleFontManager.clearVideoEmbeddedFonts()
        }
        currentFilePath = filePath
        pendingFilePath = filePath
        pendingStartPositionMs = startPositionMs
        targetResumePositionMs = startPositionMs
        isResumeSeekVerified = (startPositionMs <= 500L)
        if (startPositionMs > 500L) {
            _currentPositionMs.value = startPositionMs
        }
        AppStorageManager.setActivePlaybackPath(filePath)

        val view = mpvView
        if (view != null && view.holder.surface?.isValid == true && isSurfaceReady) {
            if (!isFdOrStream) {
                playerScope.launch(Dispatchers.IO) {
                    try {
                        SubtitleFontManager.syncFonts(view.context, filePath)
                    } catch (_: Throwable) {}
                }
            }
            pendingFilePath = null
            pendingStartPositionMs = 0L
            loadVideoInternal(filePath, startPositionMs)
        }
        // If the native surface is not ready yet, onSurfaceReady() performs the
        // single initial load from pendingFilePath. This avoids a duplicate decoder load.
    }

    private fun loadVideoInternal(filePath: String, startPositionMs: Long = 0L) {
        if (isReleased) return
        val view = mpvView ?: return
        _isBuffering.value = true
        _eofReached.value = false
        subtitlesLoadedOnce = false
        subtitleUsesGeneratedAss = false
        _durationMs.value = 0L
        _subtitleTracks.value = emptyList()
        resetAdvancedAssBindings()
        _audioTracks.value = emptyList()
        // Selections belong to ONE video: never carry the 2nd subtitle over to the next file.
        _secondarySubtitleTrackId.value = 0
        trackDiscoveryStartedAt = System.currentTimeMillis()
        lastTrackListCount = -1
        lastTrackRefreshAt = 0L

        val initialPos = if (startPositionMs > 500L) startPositionMs else 0L
        targetResumePositionMs = initialPos
        isResumeSeekVerified = (initialPos <= 500L)
        _currentPositionMs.value = initialPos

        try {
            val startSec = initialPos / 1000.0
            try {
                view.mpv.command("loadfile", filePath, "replace")
            } catch (_: Throwable) {
                view.playFile(filePath)
            }

            try {
                view.mpv.setPropertyString("hwdec", resolveHwdecValue())
                view.mpv.setPropertyString("framedrop", "vo")
                view.mpv.setPropertyDouble("speed", _playbackSpeed.value.coerceIn(0.25, 4.0))
                view.mpv.setPropertyBoolean("pause", false)
                view.mpv.setPropertyString("sub-ass", "yes")
                view.mpv.setPropertyBoolean("sub-visibility", true)
                view.mpv.setPropertyString("sub-ass-override", if (subtitleOverrideAssSsa) "force" else "no")
                view.mpv.setPropertyString("sub-auto", if (subtitleAutoLoadExternal) "exact" else "no")
                view.mpv.setPropertyBoolean("embeddedfonts", true)
                if (subtitleOverrideAssSsa) {
                    view.mpv.setPropertyString("sub-scale-with-window", if (subtitleScaleWithWindow) "yes" else "no")
                    view.mpv.setPropertyDouble("sub-scale", subtitleScale.coerceIn(0.5, 3.0))
                    view.mpv.setPropertyDouble("sub-pos", subtitlePosition.coerceIn(0.0, 150.0))
                } else {
                    // Neutral values are required because mpv documents that sub-scale and
                    // sub-pos can affect ASS/SSA even when the main override is disabled.
                    view.mpv.setPropertyString("sub-scale-with-window", "yes")
                    view.mpv.setPropertyDouble("sub-scale", 1.0)
                    view.mpv.setPropertyDouble("sub-pos", 100.0)
                    view.mpv.setPropertyBoolean("sub-ass-scale-with-window", false)
                    view.mpv.setPropertyBoolean("sub-ass-force-margins", false)
                    view.mpv.setPropertyString("sub-ass-force-style", "")
                }
                if (!subtitleFontDirectory.isNullOrBlank()) {
                    view.mpv.setPropertyString("sub-fonts-dir", subtitleFontDirectory!!)
                }
                if (!subtitleFontFamily.isNullOrBlank()) {
                    view.mpv.setPropertyString("sub-font", subtitleFontFamily!!)
                }
                view.mpv.setPropertyString("sid", if (holdSubtitleAutoSelect) "no" else "auto")
                try { view.mpv.setPropertyString("secondary-sid", "no") } catch (_: Throwable) {}
                view.mpv.setPropertyString("volume-max", "200")
                view.mpv.setPropertyInt("volume", currentVolumeLevel)
                view.mpv.setPropertyBoolean("audio-pitch-correction", currentAudioPitchCorrection)
                view.mpv.setPropertyString("audio-pitch-correction", if (currentAudioPitchCorrection) "yes" else "no")
                updateAudioFilters()
                applyVideoAdjustments(immediate = true)
            } catch (_: Throwable) {}
            _isPlaying.value = true

            // Discover audio/subtitle tracks the instant mpv has the file open (no fixed sleeps,
            // and not blocked by the resume-seek verification below).
            playerScope.launch { discoverTracksPromptly() }

            playerScope.launch {
                if (initialPos > 500L) {
                    val sec = initialPos / 1000.0
                    var confirmed = false
                    for (attempt in 0..20) {
                        if (confirmed || isReleased) break
                        delay(if (attempt == 0) 50L else 100L)
                        try {
                            val v = mpvView ?: break
                            val realPosSec = v.mpv.getPropertyDouble("time-pos") ?: -1.0
                            val realPosMs = if (realPosSec >= 0.0) (realPosSec * 1000.0).toLong() else -1L
                            if (realPosMs >= 0L && kotlin.math.abs(realPosMs - initialPos) <= 2500L) {
                                confirmed = true
                                isResumeSeekVerified = true
                                _currentPositionMs.value = realPosMs
                                _isBuffering.value = false
                            } else {
                                try {
                                    v.mpv.command("seek", "$sec", "absolute+exact")
                                } catch (_: Throwable) {
                                    v.mpv.command("seek", "$sec", "absolute")
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                    isResumeSeekVerified = true
                } else {
                    delay(50L)
                }

                try {
                    val v = mpvView ?: return@launch
                    v.mpv.setPropertyBoolean("pause", false)
                    _isPlaying.value = true
                    _isBuffering.value = false
                } catch (_: Throwable) {}

            }
        } catch (t: Throwable) {
            t.printStackTrace()
            _errorMessage.value = "Error loading video: ${t.localizedMessage ?: "Unknown codec or format error"}"
        }

        // Asynchronously extract real tracks, dimensions, and sibling subtitles in background IO to eliminate startup delay
        playerScope.launch {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                if (filePath.startsWith("fd://")) {
                    val fdInt = filePath.removePrefix("fd://").toIntOrNull()
                    if (fdInt != null) {
                        val pfd = ParcelFileDescriptor.fromFd(fdInt)
                        retriever.setDataSource(pfd.fileDescriptor)
                    }
                } else {
                    retriever.setDataSource(filePath)
                }
                var w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                var h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if ((rot == 90 || rot == 270) && w > 0 && h > 0) {
                    val tmp = w
                    w = h
                    h = tmp
                }
                retriever.release()
                if (w > 0 && h > 0) {
                    _videoWidth.value = w
                    _videoHeight.value = h
                }
            } catch (_: Throwable) {}

            extractRealTracksFromFile(filePath)

        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        _isPlaying.value = true
        try {
            mpvView?.mpv?.setPropertyBoolean("pause", false)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun pause() {
        _isPlaying.value = false
        try {
            mpvView?.mpv?.setPropertyBoolean("pause", true)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun seekTo(timeMs: Long) {
        try {
            _eofReached.value = false
            val sec = timeMs / 1000.0
            mpvView?.mpv?.command("seek", "$sec", "absolute")
            _currentPositionMs.value = timeMs
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private var currentVolumeLevel: Int = 100
    private var currentAudioChannelMode: AudioChannelMode = AudioChannelMode.AUTO_SAFE
    private var currentVolumeNormalization: Boolean = false
    private var currentDynamicRangeCompression: Boolean = false
    private var currentVoiceEnhance: Boolean = false
    private var currentAudioSurroundMode: AudioSurroundMode = AudioSurroundMode.OFF
    private var currentEqualizerEnabled: Boolean = false
    private var currentEq60Hz: Float = 0f
    private var currentEq230Hz: Float = 0f
    private var currentEq910Hz: Float = 0f
    private var currentEq3600Hz: Float = 0f
    private var currentEq14000Hz: Float = 0f
    private var currentVolumeBoostDb: Float = 0f
    private var lastAppliedAudioFilters: String = ""
    private var audioFilterUpdateJob: kotlinx.coroutines.Job? = null

    private fun updateAudioFilters(immediate: Boolean = false) {
        try {
            val view = mpvView ?: return
            
            val filterList = mutableListOf<String>()

            // 1. Audio Channel Mode routing filter (pan - zero device teardown, zero dropout)
            when (currentAudioChannelMode) {
                AudioChannelMode.MONO -> filterList.add("pan=stereo|c0=0.5*c0+0.5*c1|c1=0.5*c0+0.5*c1")
                AudioChannelMode.LEFT -> filterList.add("pan=stereo|c0=c0|c1=c0")
                AudioChannelMode.RIGHT -> filterList.add("pan=stereo|c0=c1|c1=c1")
                AudioChannelMode.REVERSED_STEREO -> filterList.add("pan=stereo|c0=c1|c1=c0")
                else -> {}
            }

            // 2. Real-Time 5-Band Equalizer (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
            if (currentEqualizerEnabled) {
                val hasNonZeroGain = kotlin.math.abs(currentEq60Hz) > 0.05f ||
                        kotlin.math.abs(currentEq230Hz) > 0.05f ||
                        kotlin.math.abs(currentEq910Hz) > 0.05f ||
                        kotlin.math.abs(currentEq3600Hz) > 0.05f ||
                        kotlin.math.abs(currentEq14000Hz) > 0.05f
                if (hasNonZeroGain) {
                    val eqFilter = "lavfi=[equalizer=f=60:t=q:w=1.0:g=%.2f,equalizer=f=230:t=q:w=1.0:g=%.2f,equalizer=f=910:t=q:w=1.0:g=%.2f,equalizer=f=3600:t=q:w=1.0:g=%.2f,equalizer=f=14000:t=q:w=1.0:g=%.2f]".format(
                        java.util.Locale.US,
                        currentEq60Hz,
                        currentEq230Hz,
                        currentEq910Hz,
                        currentEq3600Hz,
                        currentEq14000Hz
                    )
                    filterList.add(eqFilter)
                }
            }

            // 3. Real-Time Volume Boost (0 dB to +10 dB)
            if (currentVolumeBoostDb > 0.05f) {
                val boostFilter = "lavfi=[volume=volume=+%.2fdB]".format(java.util.Locale.US, currentVolumeBoostDb)
                filterList.add(boostFilter)
            }

            // 4. Voice Enhancement (dialogue clarity filter)
            if (currentVoiceEnhance) {
                filterList.add("lavfi=[equalizer=f=3000:t=q:w=1.5:g=5.0,highpass=f=100,lowpass=f=10000]")
            }

            // 5. Volume Normalization (low-latency dynaudnorm)
            if (currentVolumeNormalization) {
                // Low-latency dynamic normalizer (f=12 frames = ~50ms lookahead vs 1750ms of f=75)
                filterList.add("lavfi=[dynaudnorm=f=12:g=7:p=0.95:m=10.0]")
            }

            // 6. Dynamic Range Compression (DRC - responsive acompressor)
            if (currentDynamicRangeCompression) {
                filterList.add("lavfi=[acompressor=threshold=-20dB:ratio=4:attack=15:release=150:makeup=2dB]")
            }

            // 7. Surround / Spatial Sound Filter
            if (currentAudioSurroundMode != AudioSurroundMode.OFF && currentAudioSurroundMode.filterString.isNotBlank()) {
                filterList.add(currentAudioSurroundMode.filterString)
            }

            // 8. Peak Limiter / Soft-Clipping protection (asc=false for instant zero-latency processing)
            val hasBoostOrHighEq = currentVolumeLevel > 100 || currentVolumeBoostDb > 0.05f || (currentEqualizerEnabled && (currentEq60Hz > 0.5f || currentEq230Hz > 0.5f || currentEq910Hz > 0.5f || currentEq3600Hz > 0.5f || currentEq14000Hz > 0.5f))
            if (hasBoostOrHighEq) {
                filterList.add("lavfi=[alimiter=limit=0.98:attack=5:release=50:asc=false]")
            }

            val afString = filterList.joinToString(",")
            if (afString == lastAppliedAudioFilters) {
                return
            }

            fun applyFilter() {
                try {
                    if (isReleased) return
                    lastAppliedAudioFilters = afString
                    view.mpv.setPropertyString("af", afString)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }

            if (immediate) {
                audioFilterUpdateJob?.cancel()
                audioFilterUpdateJob = playerScope.launch(Dispatchers.Default) {
                    applyFilter()
                }
            } else {
                audioFilterUpdateJob?.cancel()
                audioFilterUpdateJob = playerScope.launch(Dispatchers.Default) {
                    delay(25) // Smooth 25ms micro-throttle during continuous slider dragging
                    applyFilter()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    /**
     * Real-time Equalizer and Volume Boost configuration.
     * Integrates seamlessly with MPV audio filter chain.
     */
    fun setEqualizer(
        enabled: Boolean,
        eq60Hz: Float,
        eq230Hz: Float,
        eq910Hz: Float,
        eq3600Hz: Float,
        eq14000Hz: Float,
        volumeBoostDb: Float,
        immediate: Boolean = false
    ) {
        currentEqualizerEnabled = enabled
        currentEq60Hz = eq60Hz.coerceIn(-12f, 12f)
        currentEq230Hz = eq230Hz.coerceIn(-12f, 12f)
        currentEq910Hz = eq910Hz.coerceIn(-12f, 12f)
        currentEq3600Hz = eq3600Hz.coerceIn(-12f, 12f)
        currentEq14000Hz = eq14000Hz.coerceIn(-12f, 12f)
        currentVolumeBoostDb = volumeBoostDb.coerceIn(0f, 10f)
        updateAudioFilters(immediate = immediate)
    }

    // ==========================================
    // REAL-TIME VIDEO FILTERS & MANUAL ADJUSTMENTS
    // ==========================================
    private var currentVideoFilterPreset: VideoFilterPreset = VideoFilterPreset.NONE
    private var currentManualVideoAdjustments: ManualVideoAdjustments = ManualVideoAdjustments()
    private var lastAppliedVfVideoFilter: String = ""
    private var videoAdjustmentDebounceJob: kotlinx.coroutines.Job? = null

    // Geometric video transform (flip / mirror). This lives in the controller — not only in
    // Compose UI state — because the video is drawn by mpv onto a native SurfaceView, which
    // ignores Compose graphicsLayer rotations. The transform is applied inside mpv's own `vf`
    // chain, so it affects the decoded frames only (audio, subtitles, resolution, aspect ratio
    // and seek/pause state are untouched) and survives seek, pause/play and fullscreen/orientation
    // changes because the mpv filter chain is not tied to the surface.
    private var videoFlipVertical: Boolean = false
    private var videoMirrorHorizontal: Boolean = false

    /**
     * Applies vertical flip and/or horizontal mirror to the actual rendered video frames.
     * Safe to call before the view is attached: the values are stored and applied as soon as a
     * file is loaded (see loadVideoInternal -> applyVideoAdjustments).
     */
    fun setVideoTransform(flipVertical: Boolean, mirrorHorizontal: Boolean) {
        videoFlipVertical = flipVertical
        videoMirrorHorizontal = mirrorHorizontal
        applyVideoAdjustments(immediate = true)
    }

    /**
     * Direct `mediacodec` hands opaque hardware surfaces to the renderer, which mpv's software
     * `vf` filters (hflip / vflip / colorbalance) cannot process. While any `vf` filter is
     * active, a forced direct MediaCodec mode is therefore swapped for `mediacodec-copy`
     * (still hardware decoding, with frames copied back so filters can run). All other modes
     * are returned unchanged.
     */
    private fun resolveHwdecValue(): String =
        if (currentHwMode == HwAccelMode.FORCE && lastAppliedVfVideoFilter.isNotEmpty()) {
            HwAccelMode.PREFER.mpvValue
        } else {
            currentHwMode.mpvValue
        }

    /**
     * Real-time Video Filter Preset and Fine Manual Adjustments.
     * Integrates with MPV hardware/GPU rendering properties (brightness, contrast, saturation, gamma, hue, sharpen, deband)
     * and temperature/tint color balance video filters.
     */
    fun setVideoFilterAndAdjustments(
        preset: VideoFilterPreset,
        manual: ManualVideoAdjustments,
        immediate: Boolean = false
    ) {
        currentVideoFilterPreset = preset
        currentManualVideoAdjustments = manual
        applyVideoAdjustments(immediate = immediate)
    }

    private fun applyVideoAdjustments(immediate: Boolean = false) {
        try {
            val view = mpvView ?: return

            val totalBrightness = (currentVideoFilterPreset.brightness + currentManualVideoAdjustments.brightness).coerceIn(-100, 100)
            val totalContrast = (currentVideoFilterPreset.contrast + currentManualVideoAdjustments.contrast).coerceIn(-100, 100)
            val totalSaturation = (currentVideoFilterPreset.saturation + currentManualVideoAdjustments.saturation).coerceIn(-100, 100)
            val totalGamma = (currentVideoFilterPreset.gamma + currentManualVideoAdjustments.gamma).coerceIn(-100, 100)
            val totalHue = (currentVideoFilterPreset.hue + currentManualVideoAdjustments.hue).coerceIn(-100, 100)
            val totalSharpen = (currentVideoFilterPreset.sharpness + currentManualVideoAdjustments.sharpness).coerceIn(0, 100)
            val totalDeband = currentVideoFilterPreset.deband || currentManualVideoAdjustments.deband

            // 1. Direct Hardware/GPU Video Properties (Instant, 0-cost, no reload)
            try {
                view.mpv.setPropertyInt("brightness", totalBrightness)
                view.mpv.setPropertyInt("contrast", totalContrast)
                view.mpv.setPropertyInt("saturation", totalSaturation)
                view.mpv.setPropertyInt("gamma", totalGamma)
                view.mpv.setPropertyInt("hue", totalHue)
                view.mpv.setPropertyBoolean("deband", totalDeband)
                // Sharpen scale in MPV (0.0 to 5.0)
                view.mpv.setPropertyDouble("sharpen", totalSharpen.toDouble() / 20.0)
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            // 2. Temperature & Tint Video Filter (colorbalance vf)
            val totalTemperature = (currentVideoFilterPreset.temperature + currentManualVideoAdjustments.temperature).coerceIn(-100, 100)
            val totalTint = (currentVideoFilterPreset.tint + currentManualVideoAdjustments.tint).coerceIn(-100, 100)

            // All video filters are combined into ONE lavfi graph because `vf` is set as a whole:
            // setting it for flips alone would wipe the colour-balance filter and vice versa.
            val vfParts = mutableListOf<String>()
            if (videoMirrorHorizontal) vfParts.add("hflip")
            if (videoFlipVertical) vfParts.add("vflip")
            if (totalTemperature != 0 || totalTint != 0) {
                val rs = (totalTemperature * 0.003 + totalTint * 0.0015).coerceIn(-1.0, 1.0)
                val gs = (-totalTint * 0.003).coerceIn(-1.0, 1.0)
                val bs = (-totalTemperature * 0.003 + totalTint * 0.0015).coerceIn(-1.0, 1.0)
                val rsStr = String.format(java.util.Locale.US, "%.3f", rs)
                val gsStr = String.format(java.util.Locale.US, "%.3f", gs)
                val bsStr = String.format(java.util.Locale.US, "%.3f", bs)
                vfParts.add("colorbalance=rs=$rsStr:gs=$gsStr:bs=$bsStr:rm=$rsStr:gm=$gsStr:bm=$bsStr:rh=$rsStr:gh=$gsStr:bh=$bsStr")
            }
            val vfFilterString = if (vfParts.isEmpty()) "" else "lavfi=[${vfParts.joinToString(",")}]"

            if (vfFilterString == lastAppliedVfVideoFilter) {
                return
            }

            fun applyVf() {
                try {
                    val enablingFilters = vfFilterString.isNotEmpty()
                    lastAppliedVfVideoFilter = vfFilterString
                    // Switch to a filter-capable decoder path BEFORE installing the filter chain,
                    // and back to the user's chosen mode only AFTER the chain has been cleared.
                    if (enablingFilters) syncHwdecWithVideoFilters(view)
                    view.mpv.setPropertyString("vf", vfFilterString)
                    if (!enablingFilters) syncHwdecWithVideoFilters(view)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }

            if (immediate) {
                videoAdjustmentDebounceJob?.cancel()
                videoAdjustmentDebounceJob = null
                applyVf()
            } else {
                videoAdjustmentDebounceJob?.cancel()
                val scope = controllerScope ?: kotlinx.coroutines.GlobalScope
                videoAdjustmentDebounceJob = scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    kotlinx.coroutines.delay(30)
                    applyVf()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun syncHwdecWithVideoFilters(view: VideoPlayerMpvView) {
        val target = resolveHwdecValue()
        // Only touch the property when it really differs: every hwdec change re-initialises the decoder.
        if (view.mpv.getPropertyString("hwdec") != target) {
            view.mpv.setPropertyString("hwdec", target)
        }
    }

    fun setVolume(volume: Int) {
        try {
            val clamped = volume.coerceIn(0, 200)
            val wasBoosted = currentVolumeLevel > 100
            val isBoosted = clamped > 100
            currentVolumeLevel = clamped
            playerScope.launch(Dispatchers.Default) {
                try {
                    mpvView?.mpv?.setPropertyString("volume-max", "200")
                    mpvView?.mpv?.setPropertyInt("volume", clamped)
                    if (wasBoosted != isBoosted) {
                        updateAudioFilters(immediate = true)
                    }
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setPlaybackSpeed(speed: Double) {
        try {
            val clamped = speed.coerceIn(0.25, 4.0)
            _playbackSpeed.value = clamped
            playerScope.launch(Dispatchers.Default) {
                try {
                    mpvView?.mpv?.setPropertyDouble("speed", clamped)
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private var currentAudioPitchCorrection: Boolean = true

    fun setAudioPitchCorrection(enabled: Boolean) {
        try {
            currentAudioPitchCorrection = enabled
            playerScope.launch(Dispatchers.Default) {
                try {
                    val v = mpvView?.mpv
                    v?.setPropertyBoolean("audio-pitch-correction", enabled)
                    v?.setPropertyString("audio-pitch-correction", if (enabled) "yes" else "no")
                } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setAudioChannelMode(mode: AudioChannelMode) {
        try {
            currentAudioChannelMode = mode
            updateAudioFilters(immediate = true)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setVolumeNormalization(enabled: Boolean) {
        try {
            currentVolumeNormalization = enabled
            updateAudioFilters(immediate = true)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun setDynamicRangeCompression(enabled: Boolean) {
        try {
            currentDynamicRangeCompression = enabled
            updateAudioFilters(immediate = true)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun extractRealTracksFromFile(filePath: String) {
        try {
            val extractor = android.media.MediaExtractor()
            if (filePath.startsWith("fd://")) {
                val fdInt = filePath.removePrefix("fd://").toIntOrNull()
                if (fdInt != null) {
                    val pfd = ParcelFileDescriptor.fromFd(fdInt)
                    extractor.setDataSource(pfd.fileDescriptor)
                } else {
                    return
                }
            } else {
                extractor.setDataSource(filePath)
            }
            val audioList = mutableListOf<PlayerMediaTrack>()
            val subList = mutableListOf<PlayerMediaTrack>()
            var audioIndex = 1
            var subIndex = 1

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                val lang = if (format.containsKey(android.media.MediaFormat.KEY_LANGUAGE)) {
                    format.getString(android.media.MediaFormat.KEY_LANGUAGE) ?: ""
                } else ""

                if (mime.startsWith("audio/")) {
                    val channels = if (format.containsKey(android.media.MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT) else 2
                    val codecName = mime.removePrefix("audio/").uppercase()
                    val langStr = if (lang.isNotBlank()) Locale(lang).displayLanguage else ""
                    val title = buildString {
                        append("Audio $audioIndex")
                        if (langStr.isNotBlank()) append(" • $langStr")
                        append(" [$codecName ${channels}ch]")
                    }
                    audioList.add(
                        PlayerMediaTrack(
                            id = audioIndex,
                            type = "audio",
                            title = title,
                            language = lang,
                            codec = codecName,
                            isSelected = (audioIndex == 1)
                        )
                    )
                    audioIndex++
                } else if (mime.startsWith("text/") || mime.contains("sub") || mime.contains("ass") || mime.contains("vtt")) {
                    val codecName = mime.substringAfter("/").uppercase()
                    val langStr = if (lang.isNotBlank()) Locale(lang).displayLanguage else ""
                    val title = buildString {
                        append("Subtitle $subIndex")
                        if (langStr.isNotBlank()) append(" • $langStr")
                        append(" [$codecName]")
                    }
                    subList.add(
                        PlayerMediaTrack(
                            id = subIndex,
                            type = "sub",
                            title = title,
                            language = lang,
                            codec = codecName,
                            isSelected = (subIndex == 1)
                        )
                    )
                    subIndex++
                }
            }
            extractor.release()

            // Also check for external subtitle files in same folder
            try {
                val videoFile = File(filePath)
                val parentDir = videoFile.parentFile
                if (parentDir != null && parentDir.exists()) {
                    val baseName = videoFile.nameWithoutExtension.lowercase()
                    val subExtensions = listOf(".srt", ".ass", ".ssa", ".vtt", ".ttml", ".xml", ".dfxp", ".smi", ".mpl2", ".sub", ".idx", ".sup", ".stl")
                    parentDir.listFiles()?.forEach { file ->
                        if (file.isFile && subExtensions.any { file.name.lowercase().endsWith(it) }) {
                            val ext = file.extension.uppercase()
                            subList.add(
                                PlayerMediaTrack(
                                    id = 100 + subList.size,
                                    type = "sub",
                                    title = file.name,
                                    language = "",
                                    codec = ext,
                                    isSelected = false,
                                    isExternal = true,
                                    originalFilename = file.name
                                )
                            )
                        }
                    }
                }
            } catch (_: Throwable) {}

            if (audioList.isNotEmpty()) _audioTracks.value = audioList
            if (subList.isNotEmpty()) _subtitleTracks.value = subList
        } catch (_: Throwable) {}
    }

    private var isSubtitleExplicitlyDisabled: Boolean = false

    fun refreshTracksAndChapters() {
        val view = mpvView ?: return
        try {
            val count = view.mpv.getPropertyInt("track-list/count") ?: 0
            if (count > 0) {
                val audioList = mutableListOf<PlayerMediaTrack>()
                val subList = mutableListOf<PlayerMediaTrack>()
                for (i in 0 until count) {
                    val type = view.mpv.getPropertyString("track-list/$i/type") ?: ""
                    val id = view.mpv.getPropertyInt("track-list/$i/id") ?: (i + 1)
                    val rawTitle = view.mpv.getPropertyString("track-list/$i/title") ?: ""
                    val lang = view.mpv.getPropertyString("track-list/$i/lang") ?: ""
                    val codec = view.mpv.getPropertyString("track-list/$i/codec") ?: ""
                    val selected = view.mpv.getPropertyBoolean("track-list/$i/selected") ?: false
                    val isExternal = view.mpv.getPropertyBoolean("track-list/$i/external") ?: false
                    val extFilename = view.mpv.getPropertyString("track-list/$i/external-filename")
                        ?: view.mpv.getPropertyString("track-list/$i/filename") ?: ""
                    if (type == "sub") {
                        view.mpv.getPropertyInt("track-list/$i/ff-index")?.let { ffIndex ->
                            if (ffIndex >= 0) subtitleTrackExtractorIndices[id] = ffIndex
                        }
                    }

                    if (type == "sub" && extFilename.isNotBlank()) {
                        val candidate = if (extFilename.startsWith("file://")) Uri.parse(extFilename).path else extFilename
                        if (!candidate.isNullOrBlank() && File(candidate).exists()) {
                            subtitleTrackSourcePaths[id] = candidate
                        }
                    }

                    val isExternalTrack = isExternal || (extFilename.isNotBlank() && (
                        externalSubtitlesMap.containsKey(extFilename) ||
                        externalSubtitlesMap.keys.any { extFilename.contains(File(it).nameWithoutExtension) } ||
                        loadedExternalSubtitles.contains(extFilename)
                    ))

                    val isExternalAudioTrack = isExternal || (extFilename.isNotBlank() && (
                        externalAudiosMap.containsKey(extFilename) ||
                        externalAudiosMap.keys.any { extFilename.contains(File(it).nameWithoutExtension) } ||
                        loadedExternalAudios.contains(extFilename)
                    ))

                    val matchedOriginalName = if (isExternalTrack && type == "sub") {
                        externalSubtitlesMap[extFilename]
                            ?: externalSubtitlesMap.entries.firstOrNull { extFilename.contains(File(it.key).nameWithoutExtension) }?.value
                            ?: if (extFilename.isNotBlank()) File(extFilename).name else rawTitle.ifBlank { "Uploaded Subtitle" }
                    } else ""

                    val matchedOriginalAudioName = if (isExternalAudioTrack && type == "audio") {
                        externalAudiosMap[extFilename]
                            ?: externalAudiosMap.entries.firstOrNull { extFilename.contains(it.key) || it.key.contains(extFilename) }?.value
                            ?: externalAudiosMap.entries.firstOrNull { extFilename.contains(File(it.key).nameWithoutExtension) }?.value
                            ?: if (extFilename.isNotBlank() && !extFilename.startsWith("http")) File(extFilename).name else rawTitle.ifBlank { "Online Audio" }
                    } else ""

                    val matchedAudioLang = if (isExternalAudioTrack && type == "audio") {
                        externalAudiosLangMap[extFilename]
                            ?: externalAudiosLangMap.entries.firstOrNull { extFilename.contains(it.key) || it.key.contains(extFilename) }?.value
                            ?: lang
                    } else lang

                    val cleanedTitle = rawTitle.trim().replace(Regex("^#\\s*\\d*\\s*[:\\-.]?\\s*"), "")
                    val finalTitle = if (isExternalTrack && type == "sub") {
                        matchedOriginalName
                    } else if (isExternalAudioTrack && type == "audio") {
                        matchedOriginalAudioName
                    } else {
                        when {
                            cleanedTitle.isNotBlank() && !cleanedTitle.matches(Regex("(?i)^[A-Z]?\\d+(sec|s)?$")) -> cleanedTitle
                            lang.isNotBlank() -> Locale(lang).displayLanguage.ifBlank { lang.uppercase() }
                            type == "sub" -> "Subtitle $id"
                            else -> "Audio Track $id"
                        }
                    }

                    val track = PlayerMediaTrack(
                        id = id,
                        type = type,
                        title = finalTitle,
                        language = if (type == "audio") matchedAudioLang else lang,
                        codec = codec,
                        isSelected = if (isSubtitleExplicitlyDisabled && type == "sub") false else selected,
                        isExternal = if (type == "sub") isExternalTrack else if (type == "audio") isExternalAudioTrack else isExternal,
                        originalFilename = if (type == "sub" && isExternalTrack) matchedOriginalName else if (type == "audio" && isExternalAudioTrack) matchedOriginalAudioName else "",
                        isSecondarySelected = (type == "sub" && _secondarySubtitleTrackId.value > 0 && id == _secondarySubtitleTrackId.value)
                    )
                    if (type == "audio") audioList.add(track)
                    else if (type == "sub") subList.add(track)
                    else if (type.equals("attachment", ignoreCase = true) || rawTitle.endsWith(".ttf", ignoreCase = true) || rawTitle.endsWith(".otf", ignoreCase = true)) {
                        if (rawTitle.isNotBlank()) SubtitleFontManager.registerVideoEmbeddedFont(rawTitle)
                    }
                }
                if (audioList.isNotEmpty()) _audioTracks.value = audioList
                if (subList.isNotEmpty()) {
                    // Deduplicate subtitle tracks by original filename / title to ensure uniqueness
                    val deduplicatedSubList = mutableListOf<PlayerMediaTrack>()
                    for (track in subList) {
                        val tName = (if (track.isExternal && track.originalFilename.isNotBlank()) track.originalFilename else track.title).trim().lowercase()
                        val tBase = if (tName.contains('.')) tName.substringBeforeLast('.') else tName
                        val existingIndex = deduplicatedSubList.indexOfFirst { existing ->
                            val eName = (if (existing.isExternal && existing.originalFilename.isNotBlank()) existing.originalFilename else existing.title).trim().lowercase()
                            val eBase = if (eName.contains('.')) eName.substringBeforeLast('.') else eName
                            eName == tName || (tBase.isNotBlank() && eBase == tBase && track.isExternal == existing.isExternal)
                        }
                        if (existingIndex == -1) {
                            deduplicatedSubList.add(track)
                        } else {
                            if (track.isSelected && !deduplicatedSubList[existingIndex].isSelected) {
                                deduplicatedSubList[existingIndex] = track
                            }
                        }
                    }

                    _subtitleTracks.value = deduplicatedSubList
                    if (advancedAssEnabled && !advancedAssSyncing) scheduleAdvancedAssSync()
                    val currentSid = view.mpv.getPropertyString("sid")
                    val isNoneSelected = deduplicatedSubList.none { it.isSelected } || currentSid == null || currentSid == "no" || currentSid == "0"
                    if (isNoneSelected && !isSubtitleExplicitlyDisabled && !holdSubtitleAutoSelect) {
                        val matched = SubtitleSessionMemory.findMatchingTrackForNextEpisode(deduplicatedSubList)
                        val chosenSubId = matched?.id ?: deduplicatedSubList.first().id
                        try {
                            view.mpv.setPropertyInt("sid", chosenSubId)
                            view.mpv.setPropertyBoolean("sub-visibility", true)
                            view.mpv.setPropertyString("sub-ass", "yes")
                            view.mpv.setPropertyString("sub-ass-override", if (subtitleOverrideAssSsa) "force" else "no")
                            val secId = _secondarySubtitleTrackId.value
                            _subtitleTracks.value = deduplicatedSubList.map {
                                it.copy(
                                    isSelected = (it.id == chosenSubId),
                                    isSecondarySelected = (secId > 0 && it.id == secId)
                                )
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }

            val chapCount = view.mpv.getPropertyInt("chapter-list/count") ?: 0
            if (chapCount > 0) {
                val list = mutableListOf<PlayerVideoChapter>()
                for (i in 0 until chapCount) {
                    val rawTitle = view.mpv.getPropertyString("chapter-list/$i/title") ?: ""
                    var cleanTitle = rawTitle.trim().replace(Regex("^#\\s*\\d*\\s*[:\\-.]?\\s*"), "")
                    if (cleanTitle.matches(Regex("(?i)^[A-Z]?\\d+(sec|s)?$")) || cleanTitle.matches(Regex("(?i)^ch(apter)?_?\\d+$")) || cleanTitle.isBlank()) {
                        cleanTitle = "Chapter ${i + 1}"
                    }
                    val timeSec = view.mpv.getPropertyDouble("chapter-list/$i/time") ?: 0.0
                    val ms = (timeSec * 1000.0).toLong()
                    list.add(
                        PlayerVideoChapter(
                            index = i,
                            title = cleanTitle,
                            timeMs = ms,
                            formattedTime = formatPlaybackTime(ms)
                        )
                    )
                }
                _chapters.value = list
                return
            } else {
                _chapters.value = emptyList()
            }
        } catch (_: Throwable) {
            _chapters.value = emptyList()
        }
    }

    fun applySubtitleSettings(
        autoLoadExternal: Boolean,
        overrideAssSsa: Boolean,
        scaleWithWindow: Boolean,
        fontDirectory: String? = null,
        fontFamily: String? = null
    ) {
        subtitleAutoLoadExternal = autoLoadExternal
        val effectiveOverrideAss = overrideAssSsa && !advancedAssGoverning
        subtitleOverrideAssSsa = effectiveOverrideAss
        subtitleScaleWithWindow = scaleWithWindow
        subtitleFontDirectory = fontDirectory?.takeIf { it.isNotBlank() }
        subtitleFontFamily = fontFamily?.takeIf { it.isNotBlank() }
        try {
            val view = mpvView ?: return
            view.mpv.setPropertyString("sub-auto", if (autoLoadExternal) "exact" else "no")
            view.mpv.setPropertyString("sub-ass-override", if (effectiveOverrideAss) "force" else "no")
            view.mpv.setPropertyString("sub-ass-force-style", "")
            view.mpv.setPropertyBoolean("sub-ass-scale-with-window", false)
            view.mpv.setPropertyBoolean("sub-ass-force-margins", false)
            view.mpv.setPropertyString("sub-scale-with-window", "yes")
            view.mpv.setPropertyDouble("sub-scale", if (effectiveOverrideAss) subtitleScale else 1.0)
            view.mpv.setPropertyDouble("sub-pos", if (effectiveOverrideAss) subtitlePosition else 100.0)
            if (subtitleFontDirectory != null && subtitleFontDirectory != lastAppliedFontDirectory) {
                lastAppliedFontDirectory = subtitleFontDirectory
                view.mpv.setPropertyString("sub-fonts-dir", subtitleFontDirectory!!)
            }
            if (!subtitleFontFamily.isNullOrBlank()) {
                view.mpv.setPropertyString("sub-font", subtitleFontFamily!!)
            } else {
                view.mpv.setPropertyString("sub-font", SubtitleFontManager.BUNDLED_FALLBACK_FONT_FAMILY)
            }
        } catch (_: Throwable) {}
    }

    fun setSubtitleScale(scale: Double) {
        subtitleScale = scale.coerceIn(0.5, 3.0)
        try { mpvView?.mpv?.setPropertyDouble("sub-scale", subtitleScale) } catch (_: Throwable) {}
    }

    fun setSubtitlePosition(position: Double) {
        subtitlePosition = position.coerceIn(0.0, 150.0)
        try { mpvView?.mpv?.setPropertyDouble("sub-pos", subtitlePosition) } catch (_: Throwable) {}
    }

    private fun mpvColorFromArgb(argb: Int): String {
        val a = ((argb ushr 24) and 0xFF) / 255f
        val r = ((argb ushr 16) and 0xFF) / 255f
        val g = ((argb ushr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return "${r.coerceIn(0f, 1f)}/${g.coerceIn(0f, 1f)}/${b.coerceIn(0f, 1f)}/${a.coerceIn(0f, 1f)}"
    }

    /**
     * Applies the live subtitle editor settings directly to mpv/libass with
     * instantaneous real-time updates and zero flickering or reload delays.
     */
    fun applySubtitleAppearance(
        bold: Boolean,
        italic: Boolean,
        underline: Boolean = false,
        alignment: String,
        fontSize: Float,
        borderStyle: String,
        borderSize: Float,
        shadowOffset: Float,
        textColorArgb: Int,
        borderColorArgb: Int,
        backgroundColorArgb: Int,
        shadowColorArgb: Int,
        overrideAssSsa: Boolean,
        scaleWithWindow: Boolean,
        scale: Float,
        position: Float,
        blendWithVideo: Boolean,
        fontDirectory: String? = null,
        fontFamily: String? = null,
        letterSpacing: Float = 0f,
        shadowOffsetX: Float = 2f,
        shadowOffsetY: Float = 2f
    ) {
        val effectiveOverrideAss = overrideAssSsa && !advancedAssGoverning
        lastAppearanceReplay = {
            applySubtitleAppearance(
                bold, italic, underline, alignment, fontSize, borderStyle, borderSize, shadowOffset,
                textColorArgb, borderColorArgb, backgroundColorArgb, shadowColorArgb, overrideAssSsa,
                scaleWithWindow, scale, position, blendWithVideo, fontDirectory, fontFamily,
                letterSpacing, shadowOffsetX, shadowOffsetY
            )
        }

        subtitleBold = bold
        subtitleItalic = italic
        subtitleUnderline = underline
        subtitleAlignment = alignment
        subtitleFontSize = fontSize.coerceIn(8f, 120f).toDouble()
        subtitleBorderStyle = borderStyle
        subtitleBorderSize = borderSize.coerceIn(0f, 20f).toDouble()
        subtitleShadowOffset = shadowOffset.coerceIn(0f, 20f).toDouble()
        subtitleTextColor = mpvColorFromArgb(textColorArgb)
        subtitleBorderColor = mpvColorFromArgb(borderColorArgb)
        subtitleBackgroundColor = mpvColorFromArgb(backgroundColorArgb)
        subtitleShadowColor = mpvColorFromArgb(shadowColorArgb)
        subtitleOverrideAssSsa = effectiveOverrideAss
        subtitleScaleWithWindow = scaleWithWindow
        subtitleScale = scale.coerceIn(0.5f, 3f).toDouble()
        subtitlePosition = position.coerceIn(0f, 150f).toDouble()
        subtitleBlendWithVideo = blendWithVideo
        subtitleFontDirectory = fontDirectory?.takeIf { it.isNotBlank() }
        subtitleFontFamily = fontFamily?.takeIf { it.isNotBlank() }

        val view = mpvView ?: return
        try {
            // Guarantee subtitle rendering pipeline is active without toggling sid
            if (!isSubtitleExplicitlyDisabled) {
                view.mpv.setPropertyBoolean("sub-visibility", true)
            }
            view.mpv.setPropertyString("sub-ass", "yes")
            // This is a rendering override, never a destructive mutation of embedded ASS/SSA.
            view.mpv.setPropertyString("sub-ass-override", if (effectiveOverrideAss) "force" else "no")
            view.mpv.setPropertyBoolean("embeddedfonts", true)

            // Direct native MPV properties for instantaneous styling
            view.mpv.setPropertyString("sub-bold", if (effectiveOverrideAss && bold) "yes" else "no")
            view.mpv.setPropertyString("sub-italic", if (effectiveOverrideAss && italic) "yes" else "no")
            view.mpv.setPropertyDouble("sub-font-size", if (effectiveOverrideAss) subtitleFontSize else 52.0)
            view.mpv.setPropertyString("sub-align-x", if (effectiveOverrideAss) alignment.lowercase() else "center")
            view.mpv.setPropertyString("sub-scale-with-window", if (effectiveOverrideAss) if (scaleWithWindow) "yes" else "no" else "yes")
            view.mpv.setPropertyDouble("sub-scale", if (effectiveOverrideAss) subtitleScale else 1.0)
            view.mpv.setPropertyDouble("sub-pos", if (effectiveOverrideAss) subtitlePosition else 100.0)
            view.mpv.setPropertyString("sub-color", if (effectiveOverrideAss) subtitleTextColor else "1.0/1.0/1.0/1.0")
            view.mpv.setPropertyString("sub-border-color", if (effectiveOverrideAss) subtitleBorderColor else "0.0/0.0/0.0/1.0")
            view.mpv.setPropertyDouble(
                "sub-border-size",
                if (effectiveOverrideAss && borderStyle != "none" && borderStyle != "shadow") subtitleBorderSize else 0.0
            )
            view.mpv.setPropertyString("sub-shadow-color", if (effectiveOverrideAss) subtitleShadowColor else "0.0/0.0/0.0/0.75")
            view.mpv.setPropertyDouble(
                "sub-shadow-offset",
                if (effectiveOverrideAss && borderStyle != "none" && borderStyle != "outline") subtitleShadowOffset else 0.0
            )
            val backColor = if (effectiveOverrideAss && (borderStyle == "opaque_box" || borderStyle == "background_box")) {
                subtitleBackgroundColor
            } else {
                subtitleShadowColor
            }
            view.mpv.setPropertyString("sub-back-color", backColor)
            val mpvBorderStyle = when {
                !effectiveOverrideAss -> "outline-and-shadow"
                borderStyle == "opaque_box" -> "opaque-box"
                borderStyle == "background_box" -> "background-box"
                else -> "outline-and-shadow"
            }
            view.mpv.setPropertyString("sub-border-style", mpvBorderStyle)
            view.mpv.setPropertyString("blend-subtitles", if (effectiveOverrideAss && blendWithVideo) "yes" else "no")
            try {
                view.mpv.setPropertyDouble("sub-spacing", if (effectiveOverrideAss) letterSpacing.toDouble() else 0.0)
            } catch (_: Throwable) {}

            if (fontDirectory != null && fontDirectory.isNotBlank() && fontDirectory != lastAppliedFontDirectory) {
                lastAppliedFontDirectory = fontDirectory
                view.mpv.setPropertyString("sub-fonts-dir", fontDirectory)
            }
            val effectiveFontFamily = if (!fontFamily.isNullOrBlank()) fontFamily else SubtitleFontManager.BUNDLED_FALLBACK_FONT_FAMILY
            view.mpv.setPropertyString("sub-font", if (effectiveOverrideAss) effectiveFontFamily else SubtitleFontManager.BUNDLED_FALLBACK_FONT_FAMILY)

            // Also configure sub-ass-force-style for fine-grained style override support
            val forceStyles = mutableListOf<String>()
            forceStyles.add("Bold=${if (bold) 1 else 0}")
            forceStyles.add("Italic=${if (italic) 1 else 0}")
            forceStyles.add("Underline=${if (underline) 1 else 0}")
            if (fontSize > 0f) {
                forceStyles.add("FontSize=${fontSize.toInt()}")
            }
            forceStyles.add("FontName=$effectiveFontFamily")
            if (letterSpacing != 0f) {
                forceStyles.add("Spacing=$letterSpacing")
            }
            val assAlign = when (alignment.lowercase()) {
                "left" -> 1
                "right" -> 3
                else -> 2
            }
            forceStyles.add("Alignment=$assAlign")

            // ASS color formatter: &HAABBGGRR with inverted alpha (00=opaque, FF=transparent)
            fun toAssHex(argb: Int): String {
                val aInv = 0xFF - ((argb ushr 24) and 0xFF)
                val r = (argb ushr 16) and 0xFF
                val g = (argb ushr 8) and 0xFF
                val b = argb and 0xFF
                return String.format(java.util.Locale.US, "&H%02X%02X%02X%02X", aInv, b, g, r)
            }

            forceStyles.add("PrimaryColour=${toAssHex(textColorArgb)}")
            forceStyles.add("OutlineColour=${toAssHex(borderColorArgb)}")

            when (borderStyle) {
                "none" -> {
                    forceStyles.add("BorderStyle=1")
                    forceStyles.add("Outline=0")
                    forceStyles.add("Shadow=0")
                    forceStyles.add("BackColour=${toAssHex(shadowColorArgb)}")
                }
                "outline" -> {
                    forceStyles.add("BorderStyle=1")
                    forceStyles.add("Outline=$borderSize")
                    forceStyles.add("Shadow=0")
                    forceStyles.add("BackColour=${toAssHex(shadowColorArgb)}")
                }
                "shadow" -> {
                    forceStyles.add("BorderStyle=1")
                    forceStyles.add("Outline=0")
                    forceStyles.add("Shadow=$shadowOffset")
                    forceStyles.add("BackColour=${toAssHex(shadowColorArgb)}")
                }
                "opaque_box", "background_box" -> {
                    forceStyles.add("BorderStyle=3")
                    forceStyles.add("Outline=$borderSize")
                    forceStyles.add("Shadow=0")
                    forceStyles.add("BackColour=${toAssHex(backgroundColorArgb)}")
                }
                else -> {
                    forceStyles.add("BorderStyle=1")
                    forceStyles.add("Outline=$borderSize")
                    forceStyles.add("Shadow=$shadowOffset")
                    forceStyles.add("BackColour=${toAssHex(shadowColorArgb)}")
                }
            }
            try {
                view.mpv.setPropertyString("sub-ass-force-style", if (effectiveOverrideAss) forceStyles.joinToString(",") else "")
                view.mpv.setPropertyBoolean("sub-ass-scale-with-window", false)
                view.mpv.setPropertyBoolean("sub-ass-force-margins", false)
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    // =============================================================================================
    // Advanced ASS/SSA per-style editor (data rules live in player/AdvancedAssStyleEngine.kt)
    //
    // The original subtitle track is never modified. When the user customises styles, the ORIGINAL
    // script text is re-written (Style lines only) into a cache file that is added as an extra mpv
    // subtitle track carrying the same title, and selected in place of the original. Only the
    // subtitle track changes: no loadfile / seek / pause, so position and audio are untouched.
    // =============================================================================================
    private class AdvancedAssBinding(
        val key: String,
        val title: String,
        val originalTrackId: Int,
        @Volatile var sourceText: String?,
        val embeddedTrack: PlayerMediaTrack?
    ) {
        @Volatile var derivedTrackId: Int = 0
        @Volatile var derivedPath: String? = null
        @Volatile var appliedSignature: String = ""
        @Volatile var unsupported: Boolean = false
    }

    private class AdvancedAssTrackInfo(
        val id: Int,
        val codec: String,
        val title: String,
        val language: String,
        val isExternal: Boolean,
        val externalPath: String?,
        val ffIndex: Int
    )

    @Volatile private var advancedAssEnabled = false
    @Volatile private var advancedAssOverridesJson = ""
    @Volatile private var advancedAssActive = false
    @Volatile private var advancedAssSyncing = false
    private val advancedAssBindings = ConcurrentHashMap<Int, AdvancedAssBinding>()
    private val advancedAssMutex = Mutex()

    // Every native mpv call made by the Advanced ASS code runs inside this monitor, and release()
    // takes it before destroying the native session. A queued/in-flight sync can therefore never
    // call mpv_get_property on a destroyed handle (that was a SIGSEGV in libmpv.so).
    private val advancedAssNativeLock = Any()

    private inline fun <T> withMpvLock(fallback: T, block: (VideoPlayerMpvView) -> T): T =
        synchronized(advancedAssNativeLock) {
            val v = mpvView
            if (isReleased || v == null) fallback else block(v)
        }
    private var advancedAssJob: Job? = null
    private var advancedAssFileCounter = 0
    private var lastAppearanceReplay: (() -> Unit)? = null

    /** True while Advanced ASS/SSA owns the styling of the current ASS/SSA track. */
    private val advancedAssGoverning: Boolean
        get() = advancedAssEnabled && advancedAssActive

    /**
     * Called whenever the subtitle settings change. Cheap when nothing relevant changed. Global
     * "Override ASS/SSA" is suppressed for an ASS/SSA track while Advanced ASS/SSA is enabled, so
     * global settings are never blindly applied on top of per-style values.
     */
    fun setAdvancedAssState(enabled: Boolean, overridesJson: String) {
        val changed = enabled != advancedAssEnabled || overridesJson != advancedAssOverridesJson
        advancedAssEnabled = enabled
        advancedAssOverridesJson = overridesJson
        if (!enabled) advancedAssActive = false
        if (changed) scheduleAdvancedAssSync()
    }

    /**
     * Returns the ORIGINAL ASS/SSA script of [trackId] (or of the original track when [trackId] is
     * the generated edited copy). Null for non-ASS/SSA, generated (SRT/VTT converted), streamed or
     * unreadable subtitles - the caller shows an "unsupported" state instead of an editor.
     */
    suspend fun readAdvancedAssSource(context: Context, trackId: Int): AdvancedAssSource? =
        withContext(Dispatchers.IO) {
            try {
                val appContext = context.applicationContext
                val info = withMpvLock<AdvancedAssTrackInfo?>(null) { view ->
                    readAdvancedAssTrackInfo(view, resolveAdvancedAssOriginalId(trackId))
                } ?: return@withContext null
                val binding = resolveAdvancedAssBinding(appContext, info) ?: return@withContext null
                val text = ensureAdvancedAssSourceText(appContext, binding) ?: return@withContext null
                AdvancedAssSource(binding.key, binding.title, text)
            } catch (_: Throwable) {
                null
            }
        }

    private fun scheduleAdvancedAssSync(delayMs: Long = ADVANCED_ASS_DEBOUNCE_MS) {
        if (isReleased) return
        val ctx = mpvView?.context?.applicationContext ?: return
        advancedAssJob?.cancel()
        advancedAssJob = playerScope.launch(Dispatchers.IO) {
            delay(delayMs)
            advancedAssMutex.withLock {
                withContext(NonCancellable) {
                    advancedAssSyncing = true
                    try {
                        syncAdvancedAssNow(ctx)
                    } catch (t: Throwable) {
                        Log.w("MpvPlayerController", "Advanced ASS sync failed", t)
                    } finally {
                        advancedAssSyncing = false
                    }
                }
            }
        }
    }

    private fun setAdvancedAssActive(active: Boolean) {
        if (advancedAssActive == active) return
        advancedAssActive = active
        // Re-apply the global appearance so the global override is (un)suppressed accordingly.
        try { lastAppearanceReplay?.invoke() } catch (_: Throwable) {}
    }

    private fun resetAdvancedAssBindings() {
        advancedAssJob?.cancel()
        val ctx = mpvView?.context
        for (b in advancedAssBindings.values) {
            b.derivedPath?.let { try { File(it).delete() } catch (_: Throwable) {} }
        }
        advancedAssBindings.clear()
        advancedAssActive = false
        if (ctx != null) {
            try { File(ctx.cacheDir, ADVANCED_ASS_CACHE_DIR).listFiles()?.forEach { it.delete() } } catch (_: Throwable) {}
        }
    }

    private fun resolveAdvancedAssOriginalId(trackId: Int): Int {
        if (trackId <= 0) return trackId
        return advancedAssBindings.values.firstOrNull { it.derivedTrackId == trackId }?.originalTrackId ?: trackId
    }

    private fun isAdvancedAssCodec(codec: String): Boolean = codec == "ass" || codec == "ssa"

    private fun readAdvancedAssTrackInfo(view: VideoPlayerMpvView, trackId: Int): AdvancedAssTrackInfo? {
        val count = view.mpv.getPropertyInt("track-list/count") ?: 0
        for (i in 0 until count) {
            if ((view.mpv.getPropertyString("track-list/$i/type") ?: "") != "sub") continue
            val id = view.mpv.getPropertyInt("track-list/$i/id") ?: continue
            if (id != trackId) continue
            val rawPath = (view.mpv.getPropertyString("track-list/$i/external-filename")
                ?: view.mpv.getPropertyString("track-list/$i/filename"))?.takeIf { it.isNotBlank() }
            val path = rawPath?.let { if (it.startsWith("file://")) Uri.parse(it).path else it }
            val isExternal = view.mpv.getPropertyBoolean("track-list/$i/external") ?: false
            return AdvancedAssTrackInfo(
                id = id,
                codec = (view.mpv.getPropertyString("track-list/$i/codec") ?: "").lowercase(Locale.ROOT),
                title = view.mpv.getPropertyString("track-list/$i/title") ?: "",
                language = view.mpv.getPropertyString("track-list/$i/lang") ?: "",
                isExternal = isExternal,
                externalPath = if (isExternal) path else null,
                ffIndex = view.mpv.getPropertyInt("track-list/$i/ff-index") ?: -1
            )
        }
        return null
    }

    private fun findAdvancedAssTrackIdByPath(view: VideoPlayerMpvView, path: String): Int? {
        val count = view.mpv.getPropertyInt("track-list/count") ?: 0
        for (i in 0 until count) {
            if ((view.mpv.getPropertyString("track-list/$i/type") ?: "") != "sub") continue
            val ext = view.mpv.getPropertyString("track-list/$i/external-filename") ?: continue
            val normalized = if (ext.startsWith("file://")) Uri.parse(ext).path else ext
            if (normalized == path) return view.mpv.getPropertyInt("track-list/$i/id")
        }
        return null
    }

    private fun isAdvancedAssEligible(context: Context, info: AdvancedAssTrackInfo): Boolean {
        if (!isAdvancedAssCodec(info.codec)) return false
        val path = info.externalPath
        if (path != null) {
            val f = File(path)
            if (!f.isFile) return false
            // SRT/VTT/... converted by Lumora, live previews and our own edited copies are not
            // real ASS/SSA subtitles.
            val generatedDir = File(context.cacheDir, "sub_transcoded").absolutePath
            if (f.absolutePath.startsWith(generatedDir)) return false
            if (f.name == "live_preview_subs.ass") return false
            if (f.parentFile?.name == ADVANCED_ASS_CACHE_DIR) return false
            return true
        }
        if (info.isExternal) return false
        val video = currentFilePath ?: return false
        if (video.startsWith("fd://") || video.startsWith("http://") || video.startsWith("https://") || video.startsWith("rtsp://")) return false
        return File(video).isFile
    }

    private fun resolveAdvancedAssBinding(context: Context, info: AdvancedAssTrackInfo): AdvancedAssBinding? {
        advancedAssBindings[info.id]?.let { return if (it.unsupported) null else it }
        if (!isAdvancedAssEligible(context, info)) return null
        val listed = _subtitleTracks.value.firstOrNull { it.id == info.id }
        val title = listed?.title?.takeIf { it.isNotBlank() }
            ?: info.title.ifBlank { info.language.ifBlank { "Subtitle ${info.id}" } }
        val path = info.externalPath
        val binding = if (path != null) {
            val text = UniversalSubtitleEngine.readTextWithCharsetDetection(File(path))
            if (AdvancedAssStyleEngine.parse(text) == null) return null
            AdvancedAssBinding(AdvancedAssStyleEngine.contentKey(text), title, info.id, text, null)
        } else {
            val video = currentFilePath ?: return null
            val key = "emb_" + AdvancedAssStyleEngine
                .sha1Hex("$video|${info.ffIndex}|${info.title}|${info.language}").take(16)
            AdvancedAssBinding(
                key, title, info.id, null,
                PlayerMediaTrack(info.id, "sub", info.title, info.language, info.codec, true)
            )
        }
        advancedAssBindings[info.id] = binding
        return binding
    }

    private suspend fun ensureAdvancedAssSourceText(context: Context, binding: AdvancedAssBinding): String? {
        binding.sourceText?.let { return it }
        val track = binding.embeddedTrack ?: return null
        val video = currentFilePath?.let { File(it) }?.takeIf { it.isFile } ?: return null
        val extracted = extractEmbeddedSubtitleToTemp(context, video, track)
        val text = try {
            extracted?.let { UniversalSubtitleEngine.readTextWithCharsetDetection(it) }
        } catch (_: Throwable) {
            null
        } finally {
            try { extracted?.delete() } catch (_: Throwable) {}
        }
        if (text.isNullOrBlank() || AdvancedAssStyleEngine.parse(text) == null) {
            binding.unsupported = true
            return null
        }
        binding.sourceText = text
        return text
    }

    private fun removeAdvancedAssDerived(view: VideoPlayerMpvView, b: AdvancedAssBinding, reselectOriginal: Boolean) {
        val id = b.derivedTrackId
        if (reselectOriginal && readAdvancedAssTrackInfo(view, b.originalTrackId) != null) {
            try { view.mpv.setPropertyInt("sid", b.originalTrackId) } catch (_: Throwable) {}
            lastActiveSubtitleTrackId = b.originalTrackId
        }
        if (id > 0) {
            try { view.mpv.command("sub-remove", id.toString()) } catch (_: Throwable) {}
        }
        b.derivedPath?.let { try { File(it).delete() } catch (_: Throwable) {} }
        b.derivedTrackId = 0
        b.derivedPath = null
        b.appliedSignature = ""
    }

    private class AdvancedAssPlan(val info: AdvancedAssTrackInfo, val originalId: Int)

    /** Sets a string option only when its current value differs (see syncAdvancedAssNow). */
    private fun setMpvStringIfChanged(view: VideoPlayerMpvView, name: String, value: String) {
        val current = try { view.mpv.getPropertyString(name) } catch (_: Throwable) { null }
        if (current == value || (value.isEmpty() && current.isNullOrEmpty())) return
        view.mpv.setPropertyString(name, value)
    }

    private suspend fun syncAdvancedAssNow(context: Context) {
        val enabled = advancedAssEnabled
        val json = advancedAssOverridesJson

        // ---- Phase A (native, locked): inspect the current subtitle track ----
        val plan = withMpvLock<AdvancedAssPlan?>(null) { view ->
            val sid = (try { view.mpv.getPropertyInt("sid") } catch (_: Throwable) { null }) ?: 0
            if (!enabled) {
                var removed = false
                for (b in advancedAssBindings.values) {
                    if (b.derivedTrackId > 0) {
                        removeAdvancedAssDerived(view, b, reselectOriginal = (sid == b.derivedTrackId))
                        removed = true
                    }
                }
                setAdvancedAssActive(false)
                if (removed) refreshTracksAndChapters()
                return@withMpvLock null
            }
            if (sid <= 0) {
                setAdvancedAssActive(false)
                return@withMpvLock null
            }
            val originalId = resolveAdvancedAssOriginalId(sid)
            val info = readAdvancedAssTrackInfo(view, originalId)
            if (info == null || !isAdvancedAssEligible(context, info)) {
                setAdvancedAssActive(false)
                return@withMpvLock null
            }
            setAdvancedAssActive(true)
            val existing = advancedAssBindings[originalId]
            if (!AssOverridesStore.hasAny(json) && (existing == null || existing.derivedTrackId <= 0)) {
                return@withMpvLock null
            }
            AdvancedAssPlan(info, originalId)
        } ?: return

        // ---- Phase B (no native calls): read source, build the edited script ----
        val binding = advancedAssBindings[plan.originalId]?.takeIf { !it.unsupported }
            ?: resolveAdvancedAssBinding(context, plan.info)
        if (binding == null) {
            withMpvLock(Unit) { setAdvancedAssActive(false) }
            return
        }
        val text = ensureAdvancedAssSourceText(context, binding)
        if (text == null) {
            withMpvLock(Unit) { setAdvancedAssActive(false) }
            return
        }
        val overrides = AssOverridesStore.get(json, binding.key)
        var signature = ""
        var newFile: File? = null
        if (!overrides.isEmpty) {
            val built = AdvancedAssStyleEngine.apply(text, overrides)
            signature = "${built.length}:${built.hashCode()}"
            val alreadyApplied = binding.derivedTrackId > 0 && binding.appliedSignature == signature
            if (!alreadyApplied) {
                val dir = File(context.cacheDir, ADVANCED_ASS_CACHE_DIR).apply { mkdirs() }
                advancedAssFileCounter++
                val file = File(dir, "adv_${binding.key}_$advancedAssFileCounter.ass")
                file.writeText(built, Charsets.UTF_8)
                newFile = file
            }
        }

        // ---- Phase C (native, locked): swap the subtitle track ----
        withMpvLock(Unit) { view ->
            val sid = (try { view.mpv.getPropertyInt("sid") } catch (_: Throwable) { null }) ?: 0
            if (overrides.isEmpty) {
                if (binding.derivedTrackId > 0) {
                    removeAdvancedAssDerived(view, binding, reselectOriginal = (sid == binding.derivedTrackId))
                    refreshTracksAndChapters()
                }
                return@withMpvLock
            }
            val derivedAlive = binding.derivedTrackId > 0 &&
                readAdvancedAssTrackInfo(view, binding.derivedTrackId) != null
            val file = newFile
            if (file == null) {
                // Same content already loaded: just make sure it is the selected track.
                if (derivedAlive && sid != binding.derivedTrackId) {
                    try { view.mpv.setPropertyInt("sid", binding.derivedTrackId) } catch (_: Throwable) {}
                    lastActiveSubtitleTrackId = binding.derivedTrackId
                    refreshTracksAndChapters()
                }
                return@withMpvLock
            }

            // Only touch global mpv/libass options that actually differ. Re-assigning an
            // unchanged option (fonts dir, override mode...) makes libass rebuild its state and
            // re-layout EVERY subtitle line, which is what made animations/timing of untouched
            // styles hiccup while a single style was being edited.
            val fontsDir = SubtitleFontManager.syncFonts(context, currentFilePath)
            setMpvStringIfChanged(view, "sub-fonts-dir", fontsDir.absolutePath)
            if ((try { view.mpv.getPropertyBoolean("embeddedfonts") } catch (_: Throwable) { null }) != true) {
                view.mpv.setPropertyBoolean("embeddedfonts", true)
            }
            setMpvStringIfChanged(view, "sub-ass", "yes")
            setMpvStringIfChanged(view, "sub-ass-override", "no")
            setMpvStringIfChanged(view, "sub-ass-force-style", "")

            externalSubtitlesMap[file.absolutePath] = binding.title
            externalSubtitlesMap[file.name] = binding.title
            val oldId = binding.derivedTrackId
            val oldPath = binding.derivedPath
            isSubtitleExplicitlyDisabled = false
            view.mpv.command("sub-add", file.absolutePath, "select", binding.title)
            val newId = findAdvancedAssTrackIdByPath(view, file.absolutePath)
                ?: (try { view.mpv.getPropertyInt("sid") } catch (_: Throwable) { null })
                ?: 0
            if (newId <= 0) {
                try { file.delete() } catch (_: Throwable) {}
                return@withMpvLock
            }
            binding.derivedTrackId = newId
            binding.derivedPath = file.absolutePath
            binding.appliedSignature = signature
            lastActiveSubtitleTrackId = newId
            view.mpv.setPropertyBoolean("sub-visibility", true)
            if (oldId > 0 && oldId != newId) {
                try { view.mpv.command("sub-remove", oldId.toString()) } catch (_: Throwable) {}
            }
            oldPath?.let { try { File(it).delete() } catch (_: Throwable) {} }
            refreshTracksAndChapters()
        }
    }

    fun setAudioTrack(trackId: Int) {
        try {
            if (trackId <= 0) {
                mpvView?.mpv?.setPropertyString("aid", "no")
            } else {
                mpvView?.mpv?.setPropertyInt("aid", trackId)
            }
            _audioTracks.value = _audioTracks.value.map {
                it.copy(isSelected = (it.id == trackId))
            }
        } catch (_: Throwable) {}
    }

    fun setSubtitleTrack(trackId: Int) {
        try {
            val view = mpvView ?: return
            if (trackId <= 0) {
                isSubtitleExplicitlyDisabled = true
                subtitleUsesGeneratedAss = false
                view.mpv.setPropertyBoolean("sub-visibility", false)
                view.mpv.setPropertyString("sid", "no")
                lastActiveSubtitleTrackId = 0
            } else {
                isSubtitleExplicitlyDisabled = false
                lastActiveSubtitleTrackId = trackId
                // Detect whether the selected subtitle points at one of Lumora's
                // generated ASS files. Native ASS/SSA must never receive a forced
                // override unless the user explicitly enabled it.
                subtitleUsesGeneratedAss = false
                val trackCount = view.mpv.getPropertyInt("track-list/count") ?: 0
                for (i in 0 until trackCount) {
                    val id = view.mpv.getPropertyInt("track-list/$i/id") ?: -1
                    if (id == trackId) {
                        val filename = view.mpv.getPropertyString("track-list/$i/filename") ?: ""
                        subtitleUsesGeneratedAss = loadedExternalSubtitles.any { it == filename || File(it).absolutePath == File(filename).absolutePath }
                        break
                    }
                }
                view.mpv.setPropertyInt("sid", trackId)
                view.mpv.setPropertyBoolean("sub-visibility", true)
                view.mpv.setPropertyString("sub-ass", "yes")
                view.mpv.setPropertyString("sub-ass-override", if (subtitleOverrideAssSsa) "force" else "no")
                if (!subtitleOverrideAssSsa) {
                    view.mpv.setPropertyString("sub-ass-force-style", "")
                }
                try {
                    view.mpv.command("sub-reload")
                } catch (_: Throwable) {}
            }
            if (advancedAssEnabled || advancedAssBindings.isNotEmpty()) scheduleAdvancedAssSync()
            val secId = _secondarySubtitleTrackId.value
            _subtitleTracks.value = _subtitleTracks.value.map {
                it.copy(
                    isSelected = (trackId > 0 && it.id == trackId),
                    isSecondarySelected = (secId > 0 && it.id == secId)
                )
            }
        } catch (_: Throwable) {}
    }

    fun setSecondarySubtitleTrack(trackId: Int) {
        try {
            val view = mpvView ?: return
            _secondarySubtitleTrackId.value = trackId
            if (trackId <= 0) {
                view.mpv.setPropertyString("secondary-sid", "no")
            } else {
                view.mpv.setPropertyInt("secondary-sid", trackId)
                view.mpv.setPropertyBoolean("secondary-sub-visibility", true)
                view.mpv.setPropertyString("secondary-sub-ass-override", if (subtitleOverrideAssSsa) "force" else "no")
                try {
                    // Position secondary subtitle at top of screen (pos 10) so it doesn't overlap primary (pos 100)
                    view.mpv.setPropertyDouble("secondary-sub-pos", 10.0)
                } catch (_: Throwable) {}
                try {
                    view.mpv.command("sub-reload")
                } catch (_: Throwable) {}
            }
            val primId = lastActiveSubtitleTrackId
            _subtitleTracks.value = _subtitleTracks.value.map {
                it.copy(
                    isSelected = (!isSubtitleExplicitlyDisabled && primId > 0 && it.id == primId),
                    isSecondarySelected = (trackId > 0 && it.id == trackId)
                )
            }
        } catch (_: Throwable) {}
    }

    fun removeSubtitleTrack(trackId: Int) {
        try {
            val view = mpvView ?: return
            val trackToRemove = _subtitleTracks.value.firstOrNull { it.id == trackId }
            // If primary or secondary, reset sid
            val currentSid = try { view.mpv.getPropertyInt("sid") } catch (_: Throwable) { 0 }
            if (currentSid == trackId) {
                view.mpv.setPropertyString("sid", "no")
            }
            if (_secondarySubtitleTrackId.value == trackId) {
                _secondarySubtitleTrackId.value = 0
                view.mpv.setPropertyString("secondary-sid", "no")
            }
            try {
                view.mpv.command("sub-remove", trackId.toString())
            } catch (_: Throwable) {}
            _subtitleTracks.value = _subtitleTracks.value.filter { it.id != trackId }
            if (trackToRemove != null) {
                loadedExternalSubtitles.removeAll {
                    it == trackToRemove.originalFilename ||
                    it == trackToRemove.title ||
                    File(it).name == trackToRemove.originalFilename ||
                    File(it).name == trackToRemove.title
                }
            }
        } catch (_: Throwable) {}
    }

    fun removeAudioTrack(trackId: Int) {
        try {
            val view = mpvView ?: return
            val currentAid = try { view.mpv.getPropertyInt("aid") } catch (_: Throwable) { 0 }
            if (currentAid == trackId) {
                view.mpv.setPropertyString("aid", "no")
            }
            try {
                view.mpv.command("audio-remove", trackId.toString())
            } catch (_: Throwable) {}
            _audioTracks.value = _audioTracks.value.filter { it.id != trackId }
            val remaining = _audioTracks.value
            if (currentAid == trackId && remaining.isNotEmpty()) {
                setAudioTrack(remaining.first().id)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Translates the selected subtitle without touching the original mpv track.
     * External files are read directly. MKV/Matroska embedded text tracks use the existing
     * EBML parser so ASS codec-private style information can be retained; other containers
     * fall back to Android's MediaExtractor text samples.
     */
    suspend fun translateSubtitleTrack(
        context: Context,
        trackId: Int,
        settings: AiFeaturesSettings,
        targetLanguageLabel: String,
        onProgress: (SubtitleTranslationService.TranslationProgress) -> Unit = {}
    ): Result<Int> = withContext(Dispatchers.IO) {
        val track = _subtitleTracks.value.firstOrNull { it.id == trackId }
            ?: return@withContext Result.failure(IllegalArgumentException("Subtitle track not found"))

        val sourceFile = subtitleTrackSourcePaths[trackId]?.let(::File)
        val videoFile = currentFilePath?.let(::File)
        val source = sourceFile ?: run {
            if (videoFile == null || !videoFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Embedded subtitle source video is unavailable"))
            }
            extractEmbeddedSubtitleToTemp(context, videoFile, track)
                ?: return@withContext Result.failure(IllegalStateException("Could not extract the selected embedded subtitle track"))
        }

        val target = TranslateLanguages.ALL.firstOrNull { it.code == settings.translateLanguageCode }
            ?: return@withContext Result.failure(IllegalArgumentException("Unsupported target language"))

        val safeBase = source.nameWithoutExtension.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetDir = File(context.filesDir, "translated_subtitles")
        targetDir.mkdirs()
        val output = File(targetDir, "${safeBase}.${target.code.replace("-", "_")}.${source.extension.ifBlank { "srt" }}")

        val result = SubtitleTranslationService.translateFile(
            context = context,
            sourceFile = source,
            destinationFile = output,
            settings = settings,
            onProgress = onProgress
        ).getOrElse { return@withContext Result.failure(it) }

        SubtitleTranslationService.validateTranslatedSubtitle(source, result.file)
            .getOrElse { return@withContext Result.failure(it) }

        val originalTitle = track.title.ifBlank { "Subtitle $trackId" }
        val translatedTitle = "$originalTitle • ${target.label}"
        val newTrackId = addExternalSubtitle(
            filePath = result.file.absolutePath,
            context = context,
            originalName = translatedTitle,
            select = false
        ) ?: return@withContext Result.failure(IllegalStateException("Translated subtitle was generated but could not be loaded into the player"))

        Result.success(newTrackId)
    }

    private suspend fun extractEmbeddedSubtitleToTemp(
        context: Context,
        video: File,
        track: PlayerMediaTrack
    ): File? {
        val extension = video.extension.lowercase(Locale.US)
        val tempDir = File(context.cacheDir, "lumora_embedded_subtitle")
        tempDir.mkdirs()
        val out = File(tempDir, "track_${track.id}_${System.currentTimeMillis()}.srt")

        // Matroska extraction already understands ASS/SSA codec-private headers and event
        // payloads. Prefer it for MKV/MKA/WebM so positioning/styles survive translation.
        if (extension in setOf("mkv", "mka", "webm")) {
            return try {
                val (_, tracks, _) = MkvManagerEngine.parseMkvFile(context, Uri.fromFile(video))
                val match = tracks.filter { it.trackType == com.example.util.MkvTrackType.SUBTITLE }.minByOrNull { candidate ->
                    val idDistance = kotlin.math.abs(candidate.trackNumber - track.id.toLong())
                    val langPenalty = if (candidate.language.equals(track.language, true)) 0 else 1000
                    val namePenalty = if (candidate.name.equals(track.title, true)) 0 else 100
                    idDistance + langPenalty + namePenalty
                } ?: return null
                val content = MkvManagerEngine.extractSubtitleTrackToString(context, Uri.fromFile(video), match)
                if (content.isBlank()) null else {
                    val ext = when {
                        content.contains("[Script Info]", true) -> "ass"
                        content.trimStart().startsWith("WEBVTT", true) -> "vtt"
                        else -> "srt"
                    }
                    val realOut = File(tempDir, "track_${track.id}_${System.currentTimeMillis()}.$ext")
                    realOut.writeText(content, Charsets.UTF_8)
                    realOut
                }
            } catch (_: Throwable) {
                null
            }
        }

        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(video.absolutePath)
            val extractorIndex = (subtitleTrackExtractorIndices[track.id] ?: (track.id - 1)).coerceAtLeast(0)
            if (extractorIndex >= extractor.trackCount) {
                extractor.release()
                return null
            }
            extractor.selectTrack(extractorIndex)
            val format = extractor.getTrackFormat(extractorIndex)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (!mime.startsWith("text/") && !mime.startsWith("application/")) {
                extractor.release()
                return null
            }
            val sb = StringBuilder()
            var cueIndex = 1
            val durationUs = format.getLong(MediaFormat.KEY_DURATION, 0L)
            val buffer = java.nio.ByteBuffer.allocate(1024 * 1024)
            while (true) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val startUs = extractor.sampleTime
                val sampleDurationUs = extractor.sampleTime.takeIf { it >= 0L }?.let {
                    if (durationUs > 0) durationUs else 3_000_000L
                } ?: 3_000_000L
                val bytes = ByteArray(size)
                buffer.rewind()
                buffer.get(bytes)
                val text = String(bytes, Charsets.UTF_8).trim()
                if (text.isNotBlank()) {
                    val start = formatSrtTime(startUs / 1000L)
                    val end = formatSrtTime((startUs + sampleDurationUs) / 1000L)
                    sb.append(cueIndex++).append('\n')
                        .append(start).append(" --> ").append(end).append('\n')
                        .append(text).append("\n\n")
                }
                if (!extractor.advance()) break
            }
            extractor.release()
            if (cueIndex == 1) null else {
                out.writeText(sb.toString(), Charsets.UTF_8)
                out
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatSrtTime(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val h = safe / 3_600_000L
        val m = (safe % 3_600_000L) / 60_000L
        val s = (safe % 60_000L) / 1_000L
        val milli = safe % 1_000L
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s, milli)
    }

    fun setSubtitleVisibility(visible: Boolean) {
        try {
            val view = mpvView ?: return
            view.mpv.setPropertyBoolean("sub-visibility", visible)
            if (!visible) {
                isSubtitleExplicitlyDisabled = true
                try { view.mpv.setPropertyString("sid", "no") } catch (_: Throwable) {}
                _subtitleTracks.value = _subtitleTracks.value.map { it.copy(isSelected = false) }
            } else {
                isSubtitleExplicitlyDisabled = false
                val targetSid = if (lastActiveSubtitleTrackId > 0) lastActiveSubtitleTrackId else 1
                try { view.mpv.setPropertyInt("sid", targetSid) } catch (_: Throwable) {}
                _subtitleTracks.value = _subtitleTracks.value.map { it.copy(isSelected = (it.id == targetSid)) }
            }
        } catch (_: Throwable) {}
    }

    fun addExternalSubtitle(filePath: String, context: Context? = null, originalName: String? = null, select: Boolean = true): Int? {
        try {
            val view = mpvView ?: return null
            val isStream = filePath.startsWith("http://") || filePath.startsWith("https://")
            val file = if (!isStream) File(filePath) else null
            if (!isStream && (file == null || !file.exists() || file.length() == 0L)) return null

            val origTitle = originalName?.ifBlank { null } ?: file?.name ?: "Online Subtitle"
            val origBase = if (origTitle.contains('.')) origTitle.substringBeforeLast('.') else origTitle

            // Prevent duplicate subtitle tracks: if a track with matching name or path is already loaded, reuse or replace it
            val existingTrack = _subtitleTracks.value.firstOrNull { track ->
                // An embedded track that merely shares a name must never be replaced/removed by an upload.
                if (!track.isExternal) return@firstOrNull false
                val tName = (if (track.originalFilename.isNotBlank()) track.originalFilename else track.title).trim()
                val tBase = if (tName.contains('.')) tName.substringBeforeLast('.') else tName
                tName.equals(origTitle, ignoreCase = true) ||
                tBase.equals(origBase, ignoreCase = true) ||
                (subtitleTrackSourcePaths[track.id]?.let { p ->
                    p == filePath || File(p).name.equals(origTitle, ignoreCase = true) ||
                    File(p).nameWithoutExtension.equals(origBase, ignoreCase = true)
                } == true) ||
                loadedExternalSubtitles.any {
                    it == filePath || File(it).name.equals(origTitle, ignoreCase = true) ||
                    File(it).nameWithoutExtension.equals(origBase, ignoreCase = true)
                }
            }

            if (existingTrack != null) {
                val existingSource = subtitleTrackSourcePaths[existingTrack.id]
                val isSameFile = existingSource == filePath || loadedExternalSubtitles.contains(filePath) ||
                    (file != null && existingSource != null && File(existingSource).name.equals(file.name, ignoreCase = true))
                if (isSameFile) {
                    if (select) {
                        setSubtitleTrack(existingTrack.id)
                        setSubtitleVisibility(true)
                    }
                    return existingTrack.id
                } else {
                    // If user is adding a new file that replaces the previous one with the same name, remove the old one first
                    removeSubtitleTrack(existingTrack.id)
                }
            }

            externalSubtitlesMap[filePath] = origTitle
            if (file != null) {
                externalSubtitlesMap[file.name] = origTitle
                externalSubtitlesMap[file.nameWithoutExtension] = origTitle
            }

            if (isStream) {
                if (!loadedExternalSubtitles.contains(filePath)) {
                    loadedExternalSubtitles.add(filePath)
                }
                isSubtitleExplicitlyDisabled = false
                view.mpv.setPropertyBoolean("sub-visibility", true)
                view.mpv.setPropertyString("sub-ass", "yes")
                view.mpv.command("sub-add", filePath, if (select) "select" else "auto", origTitle)
                refreshTracksAndChapters()
                val activeSid = try { view.mpv.getPropertyInt("sid") } catch (_: Throwable) { null }
                    ?: _subtitleTracks.value.firstOrNull { it.originalFilename == origTitle || it.title == origTitle }?.id
                    ?: _subtitleTracks.value.lastOrNull()?.id
                if (select && activeSid != null && activeSid > 0) {
                    lastActiveSubtitleTrackId = activeSid
                    return activeSid
                }
                return _subtitleTracks.value.lastOrNull()?.id
            }

            val ctx = context ?: view.context

            // Sync fonts with currently playing video and any embedded attachments synchronously
            val fontsDir = SubtitleFontManager.syncFonts(ctx, currentFilePath ?: file?.parentFile?.absolutePath)
            try {
                val count = view.mpv.getPropertyInt("track-list/count") ?: 0
                for (i in 0 until count) {
                    val type = view.mpv.getPropertyString("track-list/$i/type") ?: ""
                    val title = view.mpv.getPropertyString("track-list/$i/title") ?: ""
                    val filename = view.mpv.getPropertyString("track-list/$i/filename") ?: ""
                    if (type.equals("attachment", ignoreCase = true) ||
                        title.endsWith(".ttf", ignoreCase = true) || title.endsWith(".otf", ignoreCase = true) ||
                        filename.endsWith(".ttf", ignoreCase = true) || filename.endsWith(".otf", ignoreCase = true)
                    ) {
                        if (title.isNotBlank()) SubtitleFontManager.registerVideoEmbeddedFont(title)
                        if (filename.isNotBlank()) SubtitleFontManager.registerVideoEmbeddedFont(filename)
                    }
                }
            } catch (_: Throwable) {}

            val localFile = file ?: return null
            val isNativeAss = localFile.extension.equals("ass", ignoreCase = true) || localFile.extension.equals("ssa", ignoreCase = true)
            val pathToAdd = UniversalSubtitleEngine.prepareSubtitleForPlayback(
                context = ctx,
                file = localFile,
                videoFps = _estimatedFps.value.takeIf { it > 0.0 } ?: 24.0,
                videoPath = currentFilePath
            )
            // Only our own converted text subtitles are considered generated ASS.
            // Native ASS/SSA is kept completely untouched until the user enables override.
            subtitleUsesGeneratedAss = !isNativeAss && File(pathToAdd).extension.equals("ass", ignoreCase = true)

            externalSubtitlesMap[pathToAdd] = origTitle
            externalSubtitlesMap[File(pathToAdd).name] = origTitle
            externalSubtitlesMap[File(pathToAdd).nameWithoutExtension] = origTitle

            if (!loadedExternalSubtitles.contains(pathToAdd)) {
                loadedExternalSubtitles.add(pathToAdd)
            }

            isSubtitleExplicitlyDisabled = false
            view.mpv.setPropertyString("sub-fonts-dir", fontsDir.absolutePath)
            view.mpv.setPropertyBoolean("embeddedfonts", true)
            view.mpv.setPropertyBoolean("sub-visibility", true)
            view.mpv.setPropertyString("sub-ass", "yes")
            view.mpv.setPropertyString("sub-ass-override", if (subtitleOverrideAssSsa) "force" else "no")
            view.mpv.setPropertyString("sub-use-margins", "yes")
            if (!subtitleOverrideAssSsa) {
                view.mpv.setPropertyString("sub-ass-force-style", "")
            }
            view.mpv.command("sub-add", pathToAdd, if (select) "select" else "auto")
            refreshTracksAndChapters()

            val activeSid = try { view.mpv.getPropertyInt("sid") } catch (_: Throwable) { null }
                ?: _subtitleTracks.value.firstOrNull { it.originalFilename == origTitle || it.title == origTitle }?.id
                ?: _subtitleTracks.value.lastOrNull()?.id
            if (select && activeSid != null && activeSid > 0) {
                lastActiveSubtitleTrackId = activeSid
                setSubtitleTrack(activeSid)
                return activeSid
            }
            return _subtitleTracks.value.lastOrNull()?.id
        } catch (_: Throwable) {
            return null
        }
    }

    fun addExternalAudio(
        filePath: String,
        originalName: String? = null,
        language: String? = null,
        select: Boolean = false
    ): Int? {
        try {
            val view = mpvView ?: return null
            val isStream = filePath.startsWith("http://") || filePath.startsWith("https://")
            val file = if (!isStream) File(filePath) else null
            if (!isStream && (file == null || !file.exists() || file.length() == 0L)) return null
            val origTitle = originalName?.ifBlank { null } ?: file?.name ?: "Online Audio"
            externalAudiosMap[filePath] = origTitle
            if (!language.isNullOrBlank()) {
                externalAudiosLangMap[filePath] = language
            }
            if (file != null) {
                externalAudiosMap[file.name] = origTitle
                externalAudiosMap[file.nameWithoutExtension] = origTitle
                if (!language.isNullOrBlank()) {
                    externalAudiosLangMap[file.name] = language
                    externalAudiosLangMap[file.nameWithoutExtension] = language
                }
            }
            if (!loadedExternalAudios.contains(filePath)) {
                loadedExternalAudios.add(filePath)
            }
            val flag = if (select) "select" else "auto"
            if (!language.isNullOrBlank()) {
                view.mpv.command("audio-add", filePath, flag, origTitle, language)
            } else {
                view.mpv.command("audio-add", filePath, flag, origTitle)
            }
            refreshTracksAndChapters()

            val activeAid = try { view.mpv.getPropertyInt("aid") } catch (_: Throwable) { null }
                ?: _audioTracks.value.firstOrNull { it.originalFilename == origTitle || it.title == origTitle }?.id
                ?: _audioTracks.value.lastOrNull()?.id
            if (activeAid != null && activeAid > 0) {
                return activeAid
            }
            return _audioTracks.value.lastOrNull()?.id
        } catch (_: Throwable) {
            return null
        }
    }

    fun seekToChapter(chapter: PlayerVideoChapter) {
        seekTo(chapter.timeMs)
    }

    fun release() {
        // Idempotent: this can now be called eagerly on back-press AND again from the
        // Compose onDispose safety net. Without this guard the second call could destroy
        // an already-destroyed native surface.
        if (isReleased) return
        // Take the native lock so no Advanced ASS sync is inside an mpv call while we destroy it.
        synchronized(advancedAssNativeLock) {
            if (isReleased) return
            isReleased = true
            pollingJob?.cancel()
            pollingJob = null
            advancedAssJob?.cancel()
            try {
                playerScope.cancel()
            } catch (_: Throwable) {}
            AppStorageManager.setActivePlaybackPath(null)
            try {
                mpvView?.let { view ->
                    try {
                        view.mpv.setPropertyBoolean("pause", true)
                    } catch (_: Throwable) {}
                    view.destroy()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            mpvView = null
        }
    }
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val hours = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

// Coalesces rapid edits (typing digits, dragging the colour wheel) into ONE script reload.
private const val ADVANCED_ASS_DEBOUNCE_MS = 280L
private const val ADVANCED_ASS_CACHE_DIR = "lumora_adv_ass"
