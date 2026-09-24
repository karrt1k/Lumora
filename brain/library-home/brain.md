# Library / Home (video library, folders, tabs shell)

> Read this first for: Home screen, video folder scanning, library cache, folder/video cards, sort & view options, thumbnails, bottom tabs, file operations on videos.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `ui/screens/HomeScreen.kt` | 6116 | The library screen and host of all bottom tabs (Home / Music / Recents / Playlists): folder tree, grid/list, search, multi-select, rename/delete/move dialogs, folder info sheets, resume dialog. Hosts Music/Recents/Playlists tab content and MediaInformationScreen. |
| `ui/screens/VideoFolderScanner.kt` | 645 | Video discovery (MediaStore fast path + filesystem fallback). Defines `VideoItem`, `VideoFolder`, `VideoFolderScanner`. Used by ~23 files. |
| `util/VideoLibraryCache.kt` | 541 | On-disk cache of the scanned video library and per-file metadata (`CachedVideoMeta`). |
| `util/VideoFileManager.kt` | 526 | Video file IO: share (FileProvider), delete, rename, move/copy. |
| `util/FolderBlockListManager.kt` | 67 | Persisted list of blocked/hidden folders (prefs `lumora_folder_blocklist`). |
| `util/FolderDateUtils.kt` | 70 | Accurate folder creation date resolution (NIO attributes with fallback). |
| `util/StorageRoots.kt` | 56 | `accessibleStorageRoots(context)` – internal + SD/USB volumes. |
| `util/NaturalOrderComparator.kt` | 119 | Natural sort (Episode 2 before Episode 10). |
| `util/MediaSeenManager.kt` | 180 | Unified NEW/unplayed badge logic for video and audio (prefs `lumora_media_seen_v1`). |
| `ui/components/VideoItemCard.kt` | 695 | Video list/grid item cards (`VideoItemCard`, `VideoItemGridCard`, `formatDuration`). |
| `ui/components/VideoThumbnailLoader.kt` | 464 | Video thumbnail extraction + `ThumbnailCache`. |
| `ui/components/ModernFolderIcon.kt` | 421 | Folder icon with glyph variants (`FolderGlyphType`). |
| `ui/components/SortAndViewDialog.kt` | 1423 | Sort/view popup and its enums (`SortField`, `SortDirection`, `ViewMode`, `LayoutMode`, `VisibleFields`). |
| `ui/components/HeaderBar.kt` | 258 | Shared top app bar. |
| `ui/components/DynamicBottomNavigation.kt` | 365 | Bottom tab bar, `NavigationTab` enum (HOME, MUSIC, RECENTS, PLAYLISTS). |
| `ui/components/FloatingDirectoryPickerSheet.kt` | 791 | Generic folder-picker sheet (`DirectoryPickerAction`, `DirEntry`). |
| `ui/components/InlineRenameField.kt` | 153 | Inline rename text field (videos/folders/playlists). |
| `ui/components/BottomMoveCopyProgressBar.kt` | 159 | Progress bar for move/copy operations. |
| `ui/components/PlayGradientLogoIcon.kt` | 73 | Gradient play/logo icon used in headers. |

## Flow

- Scan: HomeScreen (launch / pull-to-refresh) → VideoFolderScanner → MediaMetadataExtractor (per-file probe, see media-info-mkv) → VideoLibraryCache → UI state.
- Tap video: HomeScreen → AppViewModel/MainActivity sets Screen.PLAYER → VideoPlayerScreen (see video-player).
- Sort/view preferences live in AppConfig (see settings).

## Storage owned (SharedPreferences names / cache & files dirs)

- `util/FolderBlockListManager.kt`: `lumora_folder_blocklist`
- `util/MediaSeenManager.kt`: `lumora_media_seen_v1`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Library UI / folder tree / dialogs | HomeScreen.kt (locate the composable in the symbol index below) |
| How videos/folders are discovered | VideoFolderScanner.kt (+ VideoLibraryCache.kt) |
| Video share/delete/rename/move | VideoFileManager.kt |
| Card look (list/grid) | VideoItemCard.kt |
| Sort fields / view modes | SortAndViewDialog.kt |
| Hidden folders | FolderBlockListManager.kt (+ FoldersSettingsTabContent in settings) |
| Bottom tabs | DynamicBottomNavigation.kt |

## Rules / gotchas

- HomeScreen.kt is ~6k lines: never re-read it fully. Jump to the composable via the symbol index; grep for the name.
- Do not re-scan storage inside individual screens; VideoFolderScanner + VideoLibraryCache are the single source for the video catalog.
- Audio has its own parallel stack (AudioLibraryCache / AudioFileManager) – do not merge; they persist different data.

## Talks to other areas

- Depends on: [app-shell](../app-shell/brain.md), [audio-player](../audio-player/brain.md), [design-system](../design-system/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [online-music](../online-music/brain.md), [playlists-history](../playlists-history/brain.md), [private-vault](../private-vault/brain.md), [settings](../settings/brain.md)
- Used by: [ai-translation](../ai-translation/brain.md), [app-shell](../app-shell/brain.md), [audio-player](../audio-player/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [online-music](../online-music/brain.md), [playlists-history](../playlists-history/brain.md), [private-vault](../private-vault/brain.md), [settings](../settings/brain.md), [subtitles](../subtitles/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`ui/screens/HomeScreen.kt`**: pruneDeletedFromFolder:222, findFolderInTree:261, HomeScreen:271, syncFolderStackToViewModel:576, pushFolder:584, popFolder:606, clearSelection:691, toggleFolderSelection:702, toggleVideoSelection:714, reloadFolders:741, reloadFoldersAndWaitAfterMutation:816, handleFolderRename:854, handleVideoRename:880, VideoFolderCard:3636, TreeFolderCard:3925, TreeSubFolderRow:4082, TreeVideoRow:4210, VideoFolderGridCard:4316, CreateFolderDialog:4428, LastWatchedResumeDialog:4617, SelectionTopBar:4801, BottomSelectionBar:4900, BottomSelectionBarItem:4992, BottomDeleteConfirmationBar:5034, RenameDialog:5186, DeleteConfirmDialog:5317, SlashPlaylistFinderContent:5440, FolderInformationSheet:5830, FolderMediaPreviewRow:5983, AudioFolderInformationSheet:6013, InfoStatGrid:6083, formatFolderSize:6100, formatFolderDuration:6109

**`ui/components/SortAndViewDialog.kt`**: SortAndViewOptionsPopup:119, SortTypeCard:1009, SortDirectionPillToggle:1062, ViewModePillToggle:1160, LayoutPillToggle:1290, FieldChip:1385

**`ui/components/FloatingDirectoryPickerSheet.kt`**: isRestrictedDirectory:70, FloatingDirectoryPickerSheet:92
