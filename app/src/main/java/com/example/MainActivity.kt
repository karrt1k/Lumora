package com.example

import com.example.R

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.player.SubtitleFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.getRequiredMediaPermissions
import com.example.ui.components.hasVideoPermission
import com.example.ui.components.hasAllFilesAccess
import com.example.ui.components.openAllFilesAccessSettings
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.VideoItem
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.state.AppViewModel
import com.example.ui.state.ThemeMode
import com.example.ui.state.VideoOpeningAnimation
import com.example.ui.state.ScreenNavigationStyle
import com.example.ui.theme.VideoPlayerTheme
import com.example.util.PipModeState

enum class Screen {
    HOME, PLAYER, CHANNEL_DETAIL, SHARED_PLAYLIST
}

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    private var pendingPostNotificationAction: (() -> Unit)? = null

    val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val track = com.example.player.AudioPlaybackManager.currentTrack.value
            val isPlaying = com.example.player.AudioPlaybackManager.isPlaying.value
            if (track != null) {
                com.example.player.AudioNotificationManager.showOrUpdateNotification(applicationContext, track, isPlaying)
            }
        }
        pendingPostNotificationAction?.invoke()
        pendingPostNotificationAction = null
    }

    fun requestNotificationPermissions(after: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !com.example.player.AudioNotificationManager.hasNotificationPermission(this)
        ) {
            pendingPostNotificationAction = after
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            after?.invoke()
        }
    }

    private fun continueFullStorageAccessFlow(after: ((Boolean) -> Unit)? = null) {
        requestNotificationPermissions {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess(this)) {
                openAllFilesAccessSettings(this)
            }
            after?.invoke(hasVideoPermission(this))
        }
    }

    val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _: Map<String, Boolean> ->
        val granted = hasVideoPermission(this)
        viewModel.setHasMediaPermission(granted)
        if (granted) {
            viewModel.triggerLibraryRefresh()
        }
        val callback = pendingPermissionCallback
        pendingPermissionCallback = null
        continueFullStorageAccessFlow { fullAccessGranted ->
            val finalGranted = fullAccessGranted || hasVideoPermission(this)
            viewModel.setHasMediaPermission(finalGranted)
            if (finalGranted) viewModel.triggerLibraryRefresh()
            callback?.invoke(finalGranted)
        }
    }

    fun requestMediaPermissions(callback: ((Boolean) -> Unit)? = null) {
        pendingPermissionCallback = callback

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            continueFullStorageAccessFlow { granted ->
                viewModel.setHasMediaPermission(granted)
                if (granted) viewModel.triggerLibraryRefresh()
                val cb = pendingPermissionCallback
                pendingPermissionCallback = null
                cb?.invoke(granted)
            }
            return
        }

        val permissions = getRequiredMediaPermissions()
        val needsRuntimeMediaPermission = permissions.any {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (needsRuntimeMediaPermission) {
            mediaPermissionLauncher.launch(permissions)
        } else {
            continueFullStorageAccessFlow { granted ->
                viewModel.setHasMediaPermission(granted)
                if (granted) viewModel.triggerLibraryRefresh()
                val cb = pendingPermissionCallback
                pendingPermissionCallback = null
                cb?.invoke(granted)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initiallyGranted = hasVideoPermission(this)
        viewModel.setHasMediaPermission(initiallyGranted)
        // HomeScreen performs the initial silent/cache scan. Do not start a second
        // full-library scan here every time the Activity is recreated.
        com.example.util.AppStorageManager.performBackgroundCleanup(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            try { SubtitleFontManager.syncFonts(applicationContext) } catch (_: Throwable) {}
        }
        PipModeState.requestPipUpdate = { isPlaying, autoPip ->
            updatePipParams(isPlaying, autoPip)
        }
        val filter = IntentFilter("com.example.action.PIP_PLAY_PAUSE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipBroadcastReceiver, filter)
        }
        handleIncomingIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreenState.collectAsStateWithLifecycle()

            LaunchedEffect(currentScreen) {
                if (currentScreen != Screen.PLAYER) {
                    PipModeState.isPlayerActive = false
                    PipModeState.isCurrentlyPlaying = false
                    PipModeState.autoPipEnabled = false
                    updatePipParams(isPlaying = false, autoPipEnabled = false)
                    kotlinx.coroutines.delay(700)
                    viewModel.isPlayerInLandscape = false
                }
            }

            val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            VideoPlayerTheme(
                themeMode = uiState.themeMode,
                appScale = uiState.appScale,
                appTheme = uiState.appTheme,
                amoledBlackMode = uiState.amoledBlackMode,
                useSystemFont = uiState.useSystemFont
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        val speed = uiState.animationSpeed.coerceIn(0.25f, 2f)
                        val duration = { base: Int -> (base / speed).roundToInt().coerceIn(0, 1400) }
                        if (targetState == Screen.PLAYER) {
                            val opening = uiState.videoOpeningAnimation
                            when (opening) {
                                VideoOpeningAnimation.NONE -> ContentTransform(
                                    targetContentEnter = fadeIn(animationSpec = tween(0)),
                                    initialContentExit = fadeOut(animationSpec = tween(0)),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.FADE_FROM_BLACK -> ContentTransform(
                                    targetContentEnter = fadeIn(animationSpec = tween(duration(380), easing = FastOutSlowInEasing)),
                                    initialContentExit = fadeOut(animationSpec = tween(duration(180))),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.ZOOM_BURST -> ContentTransform(
                                    targetContentEnter = scaleIn(initialScale = 0.85f, animationSpec = tween(duration(320), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 0.96f, animationSpec = tween(duration(240))) + fadeOut(tween(duration(160))),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.CINEMA_BARS -> ContentTransform(
                                    targetContentEnter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(duration(360), easing = CubicBezierEasing(0.12f, 0.95f, 0.22f, 1.0f))) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 0.96f, animationSpec = tween(duration(260))) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.DEFAULT, VideoOpeningAnimation.SLIDE_UP -> ContentTransform(
                                    targetContentEnter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(duration(320), easing = CubicBezierEasing(0.12f, 0.95f, 0.22f, 1.0f))),
                                    initialContentExit = scaleOut(targetScale = 0.96f, animationSpec = tween(duration(320), easing = FastOutSlowInEasing)) + fadeOut(tween(duration(220))),
                                    targetContentZIndex = 1f
                                )
                            }
                        } else if (initialState == Screen.PLAYER && targetState == Screen.HOME) {
                            // From landscape: modal-bottom-sheet-style top-to-bottom dismiss (see below).
                            // From portrait: coordinated 300ms scale/slide back animation.
                            val wasLandscape = isLandscape || viewModel.isPlayerInLandscape
                            val backDuration = duration(600)
                            val backEasing = CubicBezierEasing(0.20f, 0.0f, 0.0f, 1.0f)
                            if (wasLandscape) {
                                // Modal bottom-sheet style dismiss: the player itself slides
                                // straight down off the bottom of the screen (no horizontal
                                // page-slide), while the folder screen behind it is already
                                // fully visible immediately, so it clearly reads as the
                                // player being "closed" rather than a page-to-page navigation.
                                ContentTransform(
                                    targetContentEnter = EnterTransition.None,
                                    initialContentExit = slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(backDuration, easing = backEasing)
                                    ),
                                    targetContentZIndex = -1f
                                )
                            } else {
                                ContentTransform(
                                    targetContentEnter = scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = tween(duration(280), easing = FastOutSlowInEasing)
                                    ) + fadeIn(tween(duration(180))),
                                    initialContentExit = slideOutHorizontally(
                                        targetOffsetX = { it },
                                        animationSpec = tween(duration(300), easing = FastOutSlowInEasing)
                                    ),
                                    targetContentZIndex = -1f
                                )
                            }
                        } else if ((initialState == Screen.SHARED_PLAYLIST || initialState == Screen.CHANNEL_DETAIL) && targetState == Screen.HOME) {
                            ContentTransform(
                                targetContentEnter = slideInHorizontally(
                                    initialOffsetX = { -it / 6 },
                                    animationSpec = tween(duration(280), easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(duration(200))),
                                initialContentExit = slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(duration(280), easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(duration(180))),
                                targetContentZIndex = -1f
                            )
                        } else if (initialState == Screen.HOME && (targetState == Screen.SHARED_PLAYLIST || targetState == Screen.CHANNEL_DETAIL)) {
                            ContentTransform(
                                targetContentEnter = slideInHorizontally(
                                    initialOffsetX = { it / 4 },
                                    animationSpec = tween(duration(300), easing = FastOutSlowInEasing)
                                ) + fadeIn(tween(duration(220))),
                                initialContentExit = slideOutHorizontally(
                                    targetOffsetX = { -it / 8 },
                                    animationSpec = tween(duration(260), easing = FastOutSlowInEasing)
                                ) + fadeOut(tween(duration(180))),
                                targetContentZIndex = 1f
                            )
                        } else {
                            when (uiState.screenNavigationStyle) {
                                ScreenNavigationStyle.NONE -> ContentTransform(fadeIn(tween(0)), fadeOut(tween(0)), targetContentZIndex = -1f)
                                ScreenNavigationStyle.MINIMAL_FADE -> ContentTransform(fadeIn(tween(duration(220))), fadeOut(tween(duration(180))), targetContentZIndex = -1f)
                                ScreenNavigationStyle.DEPTH_ZOOM -> ContentTransform(
                                    targetContentEnter = scaleIn(initialScale = 0.94f, animationSpec = tween(duration(320), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 0.98f, animationSpec = tween(duration(260))) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                                ScreenNavigationStyle.FLIP_FADE -> ContentTransform(
                                    targetContentEnter = scaleIn(initialScale = 0.90f, animationSpec = tween(duration(300))) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 1.02f, animationSpec = tween(duration(260))) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                                ScreenNavigationStyle.ELASTIC_SLIDE -> ContentTransform(
                                    targetContentEnter = slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(duration(320), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(duration(280), easing = FastOutSlowInEasing)) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                                ScreenNavigationStyle.DEFAULT -> ContentTransform(
                                    targetContentEnter = slideInHorizontally(initialOffsetX = { it / 8 }, animationSpec = tween(duration(300), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = slideOutHorizontally(targetOffsetX = { -it / 10 }, animationSpec = tween(duration(260), easing = FastOutSlowInEasing)) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                            }
                        }
                    },
                    label = "ScreenTransition"
                ) { target ->
                    when (target) {
                        Screen.HOME -> {
                            val currentConfig = LocalConfiguration.current
                            val effectiveConfig = remember(currentConfig) {
                                if (currentConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    currentConfig
                                } else {
                                    Configuration(currentConfig).apply {
                                        orientation = Configuration.ORIENTATION_PORTRAIT
                                        if (screenWidthDp > screenHeightDp) {
                                            val tmp = screenWidthDp
                                            screenWidthDp = screenHeightDp
                                            screenHeightDp = tmp
                                        }
                                    }
                                }
                            }
                            CompositionLocalProvider(LocalConfiguration provides effectiveConfig) {
                                HomeScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onRequestPermissions = { requestMediaPermissions() },
                                    onPlayVideo = { video, playlist ->
                                        com.example.util.MediaSeenManager.markSeen(this@MainActivity, video.path, video.id)
                                        viewModel.startPlaying(video, playlist)
                                    },
                                    onThemeChange = viewModel::setThemeMode,
                                    onScaleChange = viewModel::setAppScale,
                                    onGlassBlurTransparencyChange = viewModel::setGlassBlurTransparency,
                                    onSideBySideChange = viewModel::setForceSideBySide,
                                    onLanguageChange = viewModel::setLanguage,
                                    onFrameStepChange = viewModel::setFrameStepAmount,
                                    onCopyTimestampChange = viewModel::setCopyTimestampOnDoubleTap,
                                    onHwAccelModeChange = viewModel::setHwAccelMode,
                                    onGestureSensitivityModeChange = viewModel::setGestureSensitivityMode
                                )
                            }
                        }
                        Screen.PLAYER -> {
                            VideoPlayerScreen(
                                video = viewModel.activeVideoItem,
                                playlist = viewModel.activeVideoPlaylist,
                                uiState = uiState,
                                onGestureSensitivityModeChange = viewModel::setGestureSensitivityMode,
                                onSeekbarStyleChange = viewModel::setSeekbarStyle,
                                onPlayerLayoutConfigChange = viewModel::setPlayerLayoutConfig,
                                onResetPlayerLayoutConfig = viewModel::resetPlayerLayoutConfig,
                                onLandscapeChanged = { wasLand -> viewModel.isPlayerInLandscape = wasLand },
                                onBack = { viewModel.exitPlayer() }
                            )
                        }
                        Screen.CHANNEL_DETAIL -> {
                            com.example.ui.screens.ChannelDetailScreen(
                                channel = viewModel.activeChannelResult,
                                uiState = uiState,
                                viewModel = viewModel,
                                isDark = uiState.themeMode == ThemeMode.DARK ||
                                    (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                                isAudioMode = viewModel.isChannelAudioMode,
                                onBack = { viewModel.exitChannel() },
                                onPlayVideo = { video, playlist ->
                                    com.example.util.MediaSeenManager.markSeen(this@MainActivity, video.path, video.id)
                                    viewModel.startPlaying(video, playlist)
                                }
                            )
                        }
                        Screen.SHARED_PLAYLIST -> {
                            val pl = viewModel.activeSharedPlaylist
                            if (pl != null) {
                                com.example.ui.screens.SharedPlaylistScreen(
                                    playlist = pl,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    isDark = uiState.themeMode == ThemeMode.DARK ||
                                        (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                                    onBack = { viewModel.exitSharedPlaylist() }
                                )
                            } else {
                                viewModel.exitSharedPlaylist()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPerm = hasVideoPermission(this)
        if (hasPerm != viewModel.hasMediaPermission.value) {
            viewModel.setHasMediaPermission(hasPerm)
            if (hasPerm) {
                viewModel.triggerLibraryRefresh()
            }
        }
    }

    private val pipBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.action.PIP_PLAY_PAUSE") {
                PipModeState.onPipPlayPauseTriggered()
            }
        }
    }

    fun buildPipParams(isPlaying: Boolean, autoPipEnabled: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))

        val actions = ArrayList<RemoteAction>()
        val playPauseIntent = Intent("com.example.action.PIP_PLAY_PAUSE").setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            101,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(
            this,
            if (isPlaying) R.drawable.lumora_pause_solid else R.drawable.lumora_play_solid
        )
        val label = if (isPlaying) "Pause" else "Play"
        actions.add(RemoteAction(icon, label, label, pendingIntent))
        builder.setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isPlayerOnScreen = (viewModel.currentScreenState.value == Screen.PLAYER) && PipModeState.isPlayerActive
            builder.setAutoEnterEnabled(autoPipEnabled && isPlayerOnScreen && isPlaying)
        }
        return builder.build()
    }

    fun updatePipParams(isPlaying: Boolean, autoPipEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams(isPlaying, autoPipEnabled)
                if (params != null) {
                    setPictureInPictureParams(params)
                }
            } catch (_: Throwable) {}
        }
    }

    // Keep the global PiP state flag in sync so the player UI can hide its controls
    // and switch to a minimal tap-to-play/pause surface while in PiP mode.
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipModeState.isInPictureInPicture.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            updatePipParams(PipModeState.isCurrentlyPlaying, PipModeState.autoPipEnabled)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val isPlayerScreen = (viewModel.currentScreenState.value == Screen.PLAYER)
        if (isPlayerScreen && PipModeState.isPlayerActive && PipModeState.autoPipEnabled && PipModeState.isCurrentlyPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams(isPlaying = true, autoPipEnabled = true)
                if (params != null) {
                    enterPictureInPictureMode(params)
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PipModeState.requestPipUpdate = null
        try {
            unregisterReceiver(pipBroadcastReceiver)
        } catch (_: Throwable) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("EXTRA_OPEN_AUDIO_PLAYER", false)) {
            com.example.player.AudioPlaybackManager.openFullScreen()
        }
        val action = intent.action ?: return
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return
        }

        // Check if a shared link from YouTube Music / Spotify / YouTube was received
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.clipData?.getItemAt(0)?.text?.toString()
            ?: intent.dataString

        val extractedUrl = com.example.util.OnlineMusicService.extractUrlFromText(sharedText)
        if (extractedUrl != null && com.example.util.OnlineMusicService.isMusicPlaylistOrTrackUrl(extractedUrl)) {
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(applicationContext, "Loading shared playlist...", android.widget.Toast.LENGTH_SHORT).show()
                }
                val pl = com.example.util.OnlineMusicService.fetchPlaylistDetails(extractedUrl)
                withContext(Dispatchers.Main) {
                    if (pl != null && pl.tracks.isNotEmpty()) {
                        viewModel.openSharedPlaylist(pl)
                    } else {
                        android.widget.Toast.makeText(applicationContext, "Could not load playlist from link", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }

        val targetUri: Uri? = when (action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.getItemAt(0)?.uri ?: intent.data
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.firstOrNull() ?: intent.clipData?.getItemAt(0)?.uri ?: intent.data
            }
            else -> intent.data ?: intent.clipData?.getItemAt(0)?.uri
        }

        if (targetUri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    targetUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val videoItem = resolveMediaUri(targetUri, intent.type)
                    withContext(Dispatchers.Main) {
                        viewModel.startPlaying(videoItem, listOf(videoItem))
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("MainActivity", "Failed to resolve incoming media URI: $targetUri", e)
                }
            }
        }
    }

    private fun resolveMediaUri(uri: Uri, mimeType: String?): VideoItem {
        var displayName = ""
        var sizeBytes = 0L

        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameCol != -1) displayName = cursor.getString(nameCol) ?: ""
                        if (sizeCol != -1) sizeBytes = cursor.getLong(sizeCol)
                    }
                }
            } catch (_: Throwable) {}
        }

        if (displayName.isBlank()) {
            val lastSegment = uri.lastPathSegment ?: ""
            displayName = if (lastSegment.isNotBlank()) {
                if (lastSegment.contains("/")) lastSegment.substringAfterLast("/") else lastSegment
            } else {
                "Media_${System.currentTimeMillis()}"
            }
        }

        val directPath = if (uri.scheme == "file") uri.path ?: uri.toString() else uri.toString()

        val extracted = com.example.util.MediaMetadataExtractor.extractMetadata(
            filePath = directPath,
            context = applicationContext,
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateModified = System.currentTimeMillis()
        )

        return VideoItem(
            id = System.currentTimeMillis(),
            uri = uri,
            displayName = displayName,
            path = directPath,
            sizeBytes = if (sizeBytes > 0) sizeBytes else 0L,
            durationMs = extracted.durationMs,
            dateModified = System.currentTimeMillis(),
            isNew = false,
            resolution = extracted.resolution,
            framerate = extracted.framerate,
            hasSubtitles = extracted.effectiveSubtitleFormats.isNotEmpty() || extracted.embeddedSubtitleFormats.isNotEmpty() || extracted.externalSubtitleFormats.isNotEmpty(),
            subtitleFormats = extracted.effectiveSubtitleFormats,
            watchedProgress = 0.0f,
            embeddedSubtitleFormats = extracted.embeddedSubtitleFormats,
            externalSubtitleFormats = extracted.externalSubtitleFormats,
            embeddedFontCount = extracted.embeddedFontCount
        )
    }
}

