# Video Player (mpv engine, overlay, panels, layout)

> Read this first for: Everything that plays a video: mpv controller, player screen, controls overlay, glass selection panels (audio/subtitle/speed/zoom/chapters/EQ/playlist), gestures, PiP, A-B loop, screenshot, control-layout editor, per-player settings.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `ui/screens/VideoPlayerScreen.kt` | 2552 | Player shell: creates `MpvPlayerController`, resolves URIs (`getPlayableFilePath`), resume position, next-episode, external audio/subtitle attach, wires overlay + history. |
| `ui/screens/VideoPlayerOverlay.kt` | 4221 | On-screen controls: gestures (seek/brightness/volume), scrubber with chapter/loop markers, lock, PiP, A-B loop button, frame capture, cast picker, clock badge. Defines `PlayerPanelType`, `DecoderMode`, `PlayerRepeatMode`, `AspectRatioMode`, `GestureLockMode`. |
| `player/MpvPlayerController.kt` | 2680 | Owner of the mpv instance: playback state flows, tracks, chapters, filters, decoder mode, subtitle add/select, advanced-ASS swap, translated-subtitle loading. Biggest engine file. |
| `player/VideoPlayerMpvView.kt` | 140 | Compose `AndroidView` wrapper around the native mpv surface. |
| `ui/components/PlayerGlassSelectionSheet.kt` | 6534 | All player bottom-sheet panels (see symbol index): subtitle, audio, delays, chapters, more-menu, speed, zoom, video settings, manual adjustments, EQ, A-B loop, playlist, seekbar style, layout, quality. |
| `ui/components/PlayerChapterSkipButton.kt` | 132 | Skip intro/outro pill. |
| `player/ChapterSkipMarker.kt` | 191 | Chapter-based skip detection (`SkipMarkerType`, `ChapterSkipMarker`, `ChapterSkipDetector`). |
| `player/VideoFilterModels.kt` | 146 | Video filter presets + manual adjustments (`VideoFilterPreset`, `ManualVideoAdjustments`). |
| `player/AudioSettingsModels.kt` | 413 | Audio track config models: channel mode, surround mode, `TrackAudioConfig`, `AudioLanguageMatcher`, `EqualizerPreset`. Shared with audio-player and settings. |
| `ui/components/AudioFileExplorerSheet.kt` | 692 | External audio-track file browser (`AudioFileItem`) used from the player. |
| `ui/components/AutoScrollingText.kt` | 57 | Marquee text for long titles in the overlay. |
| `ui/components/ControlLayoutEditor.kt` | 873 | Editor for arranging on-screen control buttons (NOT a subtitle editor). |
| `ui/state/PlayerLayoutModels.kt` | 516 | Models for the control layout (`LayoutOrientationMode`, `ControlArea`, `PlayerControlId`, `PlayerLayoutConfig`). |
| `ui/state/PlayerSettings.kt` | 525 | Runtime player settings (`PlayerSettings`), SharedPreferences `lumora_player_settings`. |
| `util/VideoScreenshotSaver.kt` | 118 | Clean frame capture to gallery. |

## Flow

- HomeScreen tap → Screen.PLAYER → VideoPlayerScreen → `getPlayableFilePath()` → MpvPlayerController loads file → VideoPlayerMpvView renders → VideoPlayerOverlay draws controls bound to controller state.
- Position saved by PlaybackHistoryManager (see playlists-history) periodically and on exit.
- Subtitles/fonts/ASS are handled by the subtitles and ass-editor areas; the player only calls into them.

## Storage owned (SharedPreferences names / cache & files dirs)

- `ui/screens/VideoPlayerScreen.kt`: `temp_player_assets`
- `player/MpvPlayerController.kt`: `sub_transcoded`, `translated_subtitles`, `lumora_embedded_subtitle`, `lumora_adv_ass`
- `ui/components/PlayerGlassSelectionSheet.kt`: `player_panel_heights_pref`
- `ui/state/PlayerSettings.kt`: `lumora_player_settings`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| mpv property/command behavior | MpvPlayerController.kt |
| Controls layout, gestures, scrubber | VideoPlayerOverlay.kt |
| A panel in the player bottom sheets | PlayerGlassSelectionSheet.kt (function named in symbol index) |
| Resume / next-episode / open external file | VideoPlayerScreen.kt |
| Which buttons appear where | ControlLayoutEditor.kt + PlayerLayoutModels.kt |
| Skip intro/outro | ChapterSkipMarker.kt + PlayerChapterSkipButton.kt |
| Video filters | VideoFilterModels.kt + MpvPlayerController (applyFilter/applyVf) |

## Rules / gotchas

- Everything plays through mpv-android-lib. Never reintroduce Media3/ExoPlayer.
- MpvPlayerController is the single source of playback state; do not keep parallel copies in the overlay beyond recomposition state.
- PlayerGlassSelectionSheet.kt (~6.5k lines) and VideoPlayerOverlay.kt (~4k) are huge: open only the function you need.
- Video-embedded audio/subtitle track selection is remembered per video – see subtitles brain before touching.

## Talks to other areas

- Depends on: [ai-translation](../ai-translation/brain.md), [app-shell](../app-shell/brain.md), [ass-editor](../ass-editor/brain.md), [audio-player](../audio-player/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [playlists-history](../playlists-history/brain.md), [settings](../settings/brain.md), [subtitles](../subtitles/brain.md)
- Used by: [ai-translation](../ai-translation/brain.md), [app-shell](../app-shell/brain.md), [ass-editor](../ass-editor/brain.md), [audio-player](../audio-player/brain.md), [settings](../settings/brain.md), [subtitles](../subtitles/brain.md)

## Symbol index (functions, `name:line`) for large files

**`ui/screens/VideoPlayerScreen.kt`**: VideoPlayerScreen:106, toSavedSubtitleRef:416, saveSubtitleSelection:436, resolveSavedSubtitle:456, applySavedSubtitleSelection:476, restoreSubtitlesForVideo:508, persistPlaybackPosition:920, exitPlayerNow:975, playEpisodeAtIndex:999, currentDisplayRotation:1338, getPlayableFilePath:2466, formatPlaybackTime:2540

**`ui/screens/VideoPlayerOverlay.kt`**: next:222, TimeNetworkClockBadge:241, openSystemCastPicker:332, getEffectiveGlassBg:353, getEffectiveGlassBorder:371, getEffectiveGlassIconTint:396, VideoPlayerOverlay:425, openPanel:665, openRootPanel:671, dismissCurrentPanel:676, notifyInteraction:758, FloatingCircleButton:2890, formatTimeShort:2935, AbLoopInlineMorphButton:2953, CameraFrameCaptureInlineMorphButton:3127, PlayerBrightnessPillHud:3298, PlayerVolumePillHud:3432, drawPlayerScrubberLoopMarkers:3586, drawPlayerScrubberChapterMarkers:3631, drawPlayerScrubberChapterBoundaries:3680, ChapterPillButton:3719, PlayerScrubberBar:3880

**`player/MpvPlayerController.kt`**: releaseSubtitleAutoSelectHold:93, captureCurrentFrame:207, setAudioDelay:227, setSubtitleDelay:235, setHwAccelMode:243, attach:257, onSurfaceCreated:294, onStart:318, onStop:334, onPause:345, onResume:354, refreshSurface:366, startPolling:494, awaitFileLoaded:617, discoverTracksPromptly:639, loadVideo:649, loadVideoInternal:698, togglePlayPause:849, play:857, pause:866, seekTo:875, updateAudioFilters:902, setEqualizer:1006, setVideoTransform:1048, resolveHwdecValue:1061, setVideoFilterAndAdjustments:1073, applyVideoAdjustments:1083, syncHwdecWithVideoFilters:1164, setVolume:1172, setPlaybackSpeed:1192, setAudioPitchCorrection:1208, setAudioChannelMode:1223, setVolumeNormalization:1232, setDynamicRangeCompression:1241, extractRealTracksFromFile:1250, refreshTracksAndChapters:1353, applySubtitleSettings:1520, setSubtitleScale:1555, setSubtitlePosition:1560, mpvColorFromArgb:1565, applySubtitleAppearance:1577, withMpvLock:1800, readAdvancedAssSource:1831, scheduleAdvancedAssSync:1846, setAdvancedAssActive:1867, resetAdvancedAssBindings:1874, resolveAdvancedAssOriginalId:1887, isAdvancedAssCodec:1892, readAdvancedAssTrackInfo:1894, findAdvancedAssTrackIdByPath:1917, isAdvancedAssEligible:1928, resolveAdvancedAssBinding:1948, ensureAdvancedAssSourceText:1972, removeAdvancedAssDerived:1992, setMpvStringIfChanged:2010, syncAdvancedAssNow:2016, setAudioTrack:2143, setSubtitleTrack:2156, setSecondarySubtitleTrack:2203, removeSubtitleTrack:2231, removeAudioTrack:2259, translateSubtitleTrack:2283, extractEmbeddedSubtitleToTemp:2334, formatSrtTime:2421, setSubtitleVisibility:2430, addExternalSubtitle:2447, addExternalAudio:2583, seekToChapter:2630, release:2634, formatPlaybackTime:2665

**`ui/components/PlayerGlassSelectionSheet.kt`**: getHeight:231, saveHeight:240, PlayerGlassModalSheet:285, cleanVideoTitleForSearch:565, searchLocalDeviceSubtitles:576, scanDir:592, SubtitleSelectionPanel:639, AudioSelectionPanel:1671, AudioDelayPanel:2108, SubtitleDelayPanel:2432, ChapterSelectionPanel:2752, MoreMenuSelectionPanel:2825, SpeedSelectionPanel:3134, VideoZoomSelectionPanel:3334, VideoSettingSelectionPanel:3518, ManualVideoAdjustmentPanel:3646, ManualAdjustmentSliderItem:3809, VideoEqSelectionPanel:3864, VerticalEqChunkySlider:3900, HorizontalBoostChunkySlider:4024, ChunkyEqBandColumn:4135, AudioEqualizerPanel:4186, AbLoopSelectionPanel:4430, PlaylistSelectionPanel:4556, PlaylistEpisodeGridCard:4906, PlaylistEpisodeRow:5085, SwipeableTrackItem:5353, SelectableTrackItem:5507, SelectableChapterItem:5663, MenuActionItem:5706, GlassPresetChip:5773, GlassStepperSliderRow:5812, formatLanguageName:5888, resolveTrackFormatBadge:5936, normalizeCodecLabel:5949, cleanTrackTitle:5981, cleanChapterTitle:6001, formatPlaybackMs:6018, SeekbarPreviewTrack:6034, SeekbarStyleSelectionCard:6225, PlayerLayoutSelectionPanel:6350, VideoQualitySelectionPanel:6488

**`ui/components/ControlLayoutEditor.kt`**: ControlLayoutEditor:110, ReorderableControlList:535, AvailableControlPill:810
