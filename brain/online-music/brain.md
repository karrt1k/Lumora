# Online Music (search, stream, cache, recommendations, channels, shared playlists)

> Read this first for: YouTube-sourced search & streaming via NewPipeExtractor, prebuffer/cache, recommendations, artist channel screen, shared playlist screen, online track metadata cache.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `util/OnlineMusicService.kt` | 969 | NewPipeExtractor wrapper: search, stream URL resolution, channel/playlist details (`OnlineTrackResult`, `OnlineChannelDetail`, `OnlinePlaylistDetail`, …). |
| `ui/components/OnlineMusicPanel.kt` | 710 | Search bar + results + recommended feed; shown when the Online folder is opened in the Music tab. |
| `ui/components/OnlineChannelResultCard.kt` | 198 | Card for artist/channel results. |
| `util/OnlineChannelResult.kt` | 14 | Channel result model. |
| `player/OnlineAudioCache.kt` | 218 | LRU on-disk cache `online_audio_cache` (resumable range downloads to `.part`, only complete files served by `cachedFile`). |
| `util/OnlineTrackMetadataCache.kt` | 138 | Persistent metadata (thumb/title/artist/duration) so playlists of online tracks survive restarts. |
| `util/OnlineRecommendationMixer.kt` | 200 | 65% similar / 35% discovery mix for the feed and auto-queue. |
| `ui/screens/ChannelDetailScreen.kt` | 986 | Artist/channel page (`Screen.CHANNEL_DETAIL`). |
| `ui/screens/SharedPlaylistScreen.kt` | 1706 | Imported/shared online playlist page (`Screen.SHARED_PLAYLIST`). |

## Flow

- Search result tap → OnlineMusicService resolves stream URL (+ LyricsFetcher) → AudioPlaybackManager.playTrack (streams immediately, caches via OnlineAudioCache, pre-resolves and prefetches upcoming tracks).
- Channel/playlist screens are separate `Screen` values handled in MainActivity.

## Storage owned (SharedPreferences names / cache & files dirs)

- `player/OnlineAudioCache.kt`: `online_audio_cache`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Search/stream resolution | OnlineMusicService.kt |
| Next-track loading, prebuffer, cache | AudioPlaybackManager.kt (`preloadUpcomingOnlineStreams`, `requestPrefetch`) + OnlineAudioCache.kt |
| Recommendation ratio/topics | OnlineRecommendationMixer.kt |
| Shared playlist UI/playback | SharedPlaylistScreen.kt |
| Channel page | ChannelDetailScreen.kt |

## Rules / gotchas

- NewPipeExtractor comes from JitPack (settings.gradle.kts) – keep that repository.
- Online tracks have no local artwork path; AudioThumbnailLoader only reads local files.

## Talks to other areas

- Depends on: [app-shell](../app-shell/brain.md), [ass-editor](../ass-editor/brain.md), [audio-player](../audio-player/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [playlists-history](../playlists-history/brain.md), [settings](../settings/brain.md)
- Used by: [app-shell](../app-shell/brain.md), [audio-player](../audio-player/brain.md), [library-home](../library-home/brain.md), [playlists-history](../playlists-history/brain.md), [settings](../settings/brain.md)

## Symbol index (functions, `name:line`) for large files

**`util/OnlineMusicService.kt`**: ensureInit:107, getSearchSuggestions:150, parseUploadDate:162, searchMusicPage:190, toOnlineChannelResult:232, formatCount:245, fetchNextMusicPage:255, search:289, resolveStreamUrl:324, isAudioOrMusicContent:364, resolveStreamWithRelated:394, getCachedAudioMeta:459, setCachedAudioMeta:464, detectFormatFromTitle:471, detectAudioTypeFromTitle:481, probeAudioMetadata:500, toOnlineTrackResult:550, fetchChannelDetail:570, fetchNextChannelTrackPage:655, fetchPlaylistTracks:675, getHighQualityThumbnailUrl:693, extractBestThumbnailUrl:746, isMusicPlaylistOrTrackUrl:760, extractUrlFromText:774, fetchPlaylistDetails:784, fetchSpotifyPlaylistDetails:854

**`ui/components/OnlineMusicPanel.kt`**: toAudioTrackItem:76, OnlineMusicPanel:107, loadFeed:154, loadMoreFeed:190, executeSearch:217, loadMore:261, playOnlineTrack:319, OnlineFeedHeader:695

**`ui/screens/ChannelDetailScreen.kt`**: ChannelDetailScreen:132, loadMoreTracks:229, openPlaylist:275, playChannelAudio:288, ChannelHeader:786, EmptySectionMessage:856, PlaylistCard:863, PlaylistExpandedList:922

**`ui/screens/SharedPlaylistScreen.kt`**: SharedPlaylistScreen:64, playPlaylist:167, AudiosSectionHeader:637, PlaylistHeaderSection:671, SharedTrackListCard:876, SharedTrackGridCard:1080, TrackOptionsDialog:1261, PlaylistCollageHeader:1438, CollageImageItem:1510, SavePlaylistDialog:1552
