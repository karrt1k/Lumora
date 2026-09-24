# Audio Player & Music tab

> Read this first for: Music tab, standalone audio player, background playback service, notification, metadata/lyrics/thumbnails, audio library cache, audio file operations.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `player/AudioPlaybackManager.kt` | 1729 | Background audio engine + media session (`AudioRepeatMode`, `AudioPlaybackManager`). Plays local files and online streams; online pre-resolve (`preloadedOnlineStreams`) and prefetch (`PREFETCH_AHEAD`). |
| `player/AudioPlaybackService.kt` | 178 | Foreground service that keeps playback alive in background. |
| `player/AudioNotificationManager.kt` | 504 | Builds/updates the playback notification (play/pause/next/prev/stop). |
| `player/AudioNotificationReceiver.kt` | 39 | BroadcastReceiver for notification buttons. |
| `player/AudioMetadataExtractor.kt` | 661 | Tag + embedded lyrics reading (ID3/MP4/FLAC). |
| `ui/screens/AudioPlayerScreen.kt` | 2815 | Full audio player screen: queue, lyrics (LRC auto-scroll), scrubber, add-to-playlist. |
| `ui/screens/AudioMusicTabContent.kt` | 2389 | Music tab: folder/track grid+list (`AudioTrackItem`, `AudioFolderItem`), virtual 'Online' folder card (entry to online-music). |
| `ui/components/MinimizedAudioPlayer.kt` | 367 | Mini-player bar. |
| `ui/components/InteractiveAudioScrubber.kt` | 278 | Draggable seek scrubber. |
| `ui/components/AudioThumbnailLoader.kt` | 397 | Audio artwork loading/caching. |
| `util/AudioLibraryCache.kt` | 460 | Scanned audio library cache. |
| `util/AudioFileManager.kt` | 517 | Audio file share/delete/rename/move. |
| `util/AudioPlaybackHistoryManager.kt` | 246 | Audio resume records (`AudioPlaybackResume`), prefs `audio_playback_history_v1`. |
| `util/LyricsFetcher.kt` | 71 | LRCLIB synced/plain lyrics by title+artist+duration. |

## Flow

- Play: AudioMusicTabContent/AudioPlayerScreen → AudioPlaybackManager.playTrack → AudioPlaybackService (foreground) + AudioNotificationManager → notification buttons → AudioNotificationReceiver → AudioPlaybackManager.
- Resume position: AudioPlaybackHistoryManager.

## Storage owned (SharedPreferences names / cache & files dirs)

- `ui/screens/AudioPlayerScreen.kt`: `lumora_audio_permissions`
- `util/AudioPlaybackHistoryManager.kt`: `audio_playback_history_v1`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Playback logic, queue, next-track, repeat/shuffle | AudioPlaybackManager.kt |
| Notification look/buttons | AudioNotificationManager.kt + AudioNotificationReceiver.kt |
| Audio player UI | AudioPlayerScreen.kt |
| Music tab UI | AudioMusicTabContent.kt |
| Artwork | AudioThumbnailLoader.kt |
| Lyrics | LyricsFetcher.kt + AudioMetadataExtractor.kt |

## Rules / gotchas

- AudioPlaybackHistoryManager (audio) and PlaybackHistoryManager (video) are intentionally separate stores.
- Online tracks reuse the same playTrack path as local files (path prefix `online://`). Do not fork a second player.

## Talks to other areas

- Depends on: [app-shell](../app-shell/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [online-music](../online-music/brain.md), [playlists-history](../playlists-history/brain.md), [private-vault](../private-vault/brain.md), [settings](../settings/brain.md), [subtitles](../subtitles/brain.md), [video-player](../video-player/brain.md)
- Used by: [app-shell](../app-shell/brain.md), [library-home](../library-home/brain.md), [online-music](../online-music/brain.md), [playlists-history](../playlists-history/brain.md), [private-vault](../private-vault/brain.md), [settings](../settings/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`player/AudioPlaybackManager.kt`**: setLoopPointA:152, setLoopPointB:163, toggleABLoop:182, clearABLoop:193, adjustLoopPointA:199, adjustLoopPointB:208, jumpToPointA:216, openFullScreen:220, playRecentTrack:224, collapseToMiniPlayer:277, closeAndStop:284, reorderQueue:295, setLyricsForCurrentTrack:313, updateQueue:327, playTrack:339, startMediaPlayer:429, expectedDurationMs:792, startCachePipeline:805, swapToCachedFile:829, isDirectStreamUrl:842, upcomingOnlineTrack:849, requestPrefetch:868, prefetchUpcomingOnline:886, standbyMatches:921, takeStandbyPlayer:924, releaseStandby:935, prepareStandbyForUpcoming:948, onQueueOrderChanged:997, preloadUpcomingOnlineStreams:1008, updateNotification:1051, handleTrackCompletion:1069, togglePlayPause:1114, pause:1147, resume:1167, seekTo:1184, seekRelative:1210, skipNext:1215, skipPrevious:1254, toggleShuffle:1295, toggleRepeat:1332, setPlaybackSpeed:1341, applyPlaybackSpeed:1346, stop:1362, setupAudioEffects:1391, releaseAudioEffects:1421, updateEqualizer:1440, setHeadphoneSurround:1463, applyAudioFxImmediately:1473, handleOnlineStreamStall:1537, startProgressTracker:1562, stopProgressTracker:1629, handleAudioFocusChange:1634, requestAudioFocus:1686, abandonAudioFocus:1716

**`ui/screens/AudioPlayerScreen.kt`**: AudioPlayerScreen:184, InfoRow:2522, formatAudioTime:2550, formatAudioFileSize:2562, ExpandedLyricsContainer:2575

**`ui/screens/AudioMusicTabContent.kt`**: AudioMusicTabContent:266, clearSelection:332, scanAudioFiles:359, selectAll:534, shareSelectedItems:546, handleFolderRename:584, handleTrackRename:601, deleteSelectedItems:623, OnlineFolderCard:1118, AudioFolderListCard:1172, AudioFolderGridCard:1389, AudioTrackListCard:1498, AudioReorderTwoLineHandle:1925, AudioTrackGridCard:1944, AnimatedAudioEqualizer:2192, EmptyAudioView:2276, EmptyAudioFoldersInlineView:2332
