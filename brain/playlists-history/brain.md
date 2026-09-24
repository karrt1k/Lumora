# Playlists & Recently played history

> Read this first for: Custom playlists, add-to-playlist, Recents tab, video playback history/resume records.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `util/PlaylistManager.kt` | 256 | Custom playlist persistence (`CustomPlaylist`), prefs `custom_playlists_prefs`. |
| `ui/screens/CustomPlaylistsTabContent.kt` | 1884 | Playlists tab UI (`PlaylistCard`, reorder, rename). |
| `ui/components/AddToPlaylistSheet.kt` | 634 | Add-to-playlist dialog (`AddToPlaylistDialog`). |
| `util/PlaybackHistoryManager.kt` | 442 | Video history/resume (`VideoPlaybackRecord`), prefs `video_playback_history_v2`. |
| `ui/screens/RecentlyPlayedTabContent.kt` | 1580 | Recents tab UI (`RecentsFolder`, cards). |

## Flow

- Video plays → PlaybackHistoryManager saves position → RecentlyPlayedTabContent lists it; audio equivalent is AudioPlaybackHistoryManager (audio-player brain).

## Storage owned (SharedPreferences names / cache & files dirs)

- `util/PlaylistManager.kt`: `custom_playlists_prefs`
- `util/PlaybackHistoryManager.kt`: `video_playback_history_v2`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Playlist data/rules | PlaylistManager.kt |
| Playlists tab UI | CustomPlaylistsTabContent.kt |
| Add-to-playlist flow | AddToPlaylistSheet.kt |
| Resume/watched threshold | PlaybackHistoryManager.kt |
| Recents tab UI | RecentlyPlayedTabContent.kt |

## Rules / gotchas

- Do not merge video and audio history stores.

## Talks to other areas

- Depends on: [app-shell](../app-shell/brain.md), [audio-player](../audio-player/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [online-music](../online-music/brain.md), [settings](../settings/brain.md)
- Used by: [audio-player](../audio-player/brain.md), [library-home](../library-home/brain.md), [online-music](../online-music/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`ui/screens/CustomPlaylistsTabContent.kt`**: CustomPlaylistsTabContent:133, setSelectedPl:183, refreshPlaylists:232, PlaylistCard:1689, MetaPill:1874

**`ui/screens/RecentlyPlayedTabContent.kt`**: RecentlyPlayedTabContent:106, setSelectedF:138, refreshAudioHistory:158, refreshHistory:168, playRecord:307, removeRecord:334, RecentsFolderCard:888, RecentVideoCard:1039, RecentAudioCard:1263, toAudioTrackItem:1522, formatRecentsRelativeTime:1539, formatRecentsDuration:1555, formatRecentsFileSize:1568
