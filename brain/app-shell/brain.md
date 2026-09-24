# App Shell (entry, navigation, permissions, manifest)

> Read this first for: MainActivity, top-level screen switching, runtime permissions, Picture-in-Picture, VIEW/SEND intents, AndroidManifest, res/xml.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `MainActivity.kt` | 643 | Only Activity. Owns `enum Screen { HOME, PLAYER, CHANNEL_DETAIL, SHARED_PLAYLIST }` (hand-rolled navigation, no Navigation-Compose), `AppViewModel`, permission flow, PiP, VIEW/SEND intent handling, audio-service/notification bootstrap. |
| `util/PipModeState.kt` | 24 | Tiny Picture-in-Picture flag holder shared by MainActivity, VideoPlayerScreen and VideoPlayerOverlay. |
| `ui/components/PermissionHandler.kt` | 503 | Permission helpers (`getRequiredMediaPermissions`, `hasAllFilesAccess`, `hasNotificationPermission`, `openAppSettings`, ...) plus `FirstLaunchPermissionHandler` and `StoragePermissionDialog` composables. |

## Non-Kotlin files / config

| Path | Role |
|---|---|
| `app/src/main/AndroidManifest.xml` | Permissions (media, notifications, foreground-service media playback, all-files access), MainActivity (launcher + VIEW filters for video/audio/content/file/http(s)/rtsp/rtmp/mms, PiP), `AudioNotificationReceiver`, `AudioPlaybackService` (foregroundServiceType=mediaPlayback), FileProvider `${applicationId}.provider`. |
| `app/src/main/res/xml/file_paths.xml` | FileProvider paths (cache, files, external-path) used by share features in VideoFileManager/AudioFileManager. |
| `app/src/main/res/xml/backup_rules.xml, data_extraction_rules.xml` | Android backup config referenced by the manifest. |
| `app/src/main/res/values/strings.xml, themes.xml` | App name string + base theme only. All UI text is in Kotlin (`AppStrings` in AppConfig.kt). |

## Flow

- Launch → `MainActivity.onCreate` → `VideoPlayerTheme { … }` → switch on `Screen`: HOME→`HomeScreen`, PLAYER→`VideoPlayerScreen`, CHANNEL_DETAIL→`ChannelDetailScreen`, SHARED_PLAYLIST→`SharedPlaylistScreen`.
- Screen changes are driven by state in `AppViewModel` (ui/state/AppConfig.kt), e.g. `exitChannel`, `exitSharedPlaylist`.
- External VIEW/SEND intents are parsed in MainActivity and forwarded to the player screen as a URI.

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Change top-level navigation / add a screen | MainActivity.kt (`Screen` enum + the `when` that renders it) and AppViewModel in AppConfig.kt |
| Change what file types/URLs the app opens | AndroidManifest.xml intent-filters |
| Change permission prompts | PermissionHandler.kt |
| Change PiP behavior | MainActivity.kt + PipModeState.kt + VideoPlayerScreen/VideoPlayerOverlay |

## Rules / gotchas

- Do NOT change `namespace = "com.example"` or `applicationId = "god.kartik.lumora"`.
- Never create a second resources folder (icon-res, res2 …). CI fails the build if one exists.
- New services/receivers/providers must be declared in AndroidManifest.xml.

## Talks to other areas

- Depends on: [audio-player](../audio-player/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [online-music](../online-music/brain.md), [settings](../settings/brain.md), [subtitles](../subtitles/brain.md), [video-player](../video-player/brain.md)
- Used by: [audio-player](../audio-player/brain.md), [library-home](../library-home/brain.md), [online-music](../online-music/brain.md), [playlists-history](../playlists-history/brain.md), [settings](../settings/brain.md), [video-player](../video-player/brain.md)
