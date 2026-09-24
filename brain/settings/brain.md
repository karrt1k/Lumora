# Settings & app configuration

> Read this first for: Settings sheet, every settings enum/state, persistence, export/import of settings, storage cleanup, haptics.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `ui/state/AppConfig.kt` | 1859 | Central state file: all settings enums (theme, thumbnails, animations, language, hw-accel, seekbar, gestures…), `UiState`, `SettingsPreferencesManager` (SharedPreferences `video_player_settings_prefs`), `AppViewModel`, `AppStrings` (localized UI strings). |
| `ui/components/SettingsSheet.kt` | 6023 | Whole Settings UI (categories, player/gesture/subtitle tabs, rows). |
| `ui/components/AdvancedSettingsPanel.kt` | 530 | Export/import of settings as 'Lumora Setting save.xml' and AI section entry. |
| `util/LumoraSettingsXmlManager.kt` | 857 | XML serialization/deserialization of all settings (defensive parsing). |
| `ui/components/AboutSettingsPanel.kt` | 992 | About page: device/hardware info, links. |
| `ui/components/FoldersSettingsTabContent.kt` | 1887 | Folder-related settings (selected/blocked/private folders, cache). |
| `ui/components/AudioFolderVideoSelectorSheet.kt` | 982 | Folder/video picker for preferred-audio rules. |
| `util/AppStorageManager.kt` | 176 | Automatic cleanup of temp caches (`sub_transcoded`, `temp_player_assets`). |
| `util/AppHaptics.kt` | 100 | Global haptic controller (setting: Haptic Feedback). |

## Flow

- Change setting in SettingsSheet → AppViewModel → SettingsPreferencesManager (write) → UiState → recomposition everywhere.

## Storage owned (SharedPreferences names / cache & files dirs)

- `ui/state/AppConfig.kt`: `video_player_settings_prefs`
- `util/AppStorageManager.kt`: `sub_transcoded`, `temp_player_assets`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Add/change a persisted setting | AppConfig.kt (UiState + SettingsPreferencesManager + AppViewModel) and LumoraSettingsXmlManager.kt if it must be exported |
| A settings screen/row | SettingsSheet.kt (function in symbol index) |
| UI text / languages | AppStrings in AppConfig.kt |
| Settings export/import | AdvancedSettingsPanel.kt + LumoraSettingsXmlManager.kt |

## Rules / gotchas

- Legacy fields from the old standalone subtitle editor (paste-box/text-editor/dialogue-offset settings) still exist in `UiState`, prefs and the settings XML. They are unused by any UI but kept for backward-compatible import/export; removing them needs a coordinated change in AppConfig.kt AND LumoraSettingsXmlManager.kt.
- Never keep a persisted setting in screen-local state.

## Talks to other areas

- Depends on: [ai-translation](../ai-translation/brain.md), [app-shell](../app-shell/brain.md), [audio-player](../audio-player/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [online-music](../online-music/brain.md), [private-vault](../private-vault/brain.md), [subtitles](../subtitles/brain.md), [video-player](../video-player/brain.md)
- Used by: [app-shell](../app-shell/brain.md), [audio-player](../audio-player/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [online-music](../online-music/brain.md), [playlists-history](../playlists-history/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`ui/state/AppConfig.kt`**: getDisplayName:115, getDescription:121, getDisplayName:167, getDescription:172, getPrefs:378, getLastExtractDirectory:382, setLastExtractDirectory:393, loadSettings:399, loadPlayerLayoutConfig:638, savePlayerLayoutConfig:677, saveSettings:693, loadSortField:785, saveSortField:803, loadSortDirection:808, saveSortDirection:826, loadViewMode:831, saveViewMode:849, loadLayoutMode:854, saveLayoutMode:872, loadVisibleFields:877, saveVisibleFields:908, loadCustomFolders:928, saveCustomFolders:932, setHasMediaPermission:948, triggerLibraryRefresh:952, setThemeMode:956, setAppScale:962, setGlassBlurTransparency:968, setForceSideBySide:974, setLanguage:980, setFrameStepAmount:986, setCopyTimestampOnDoubleTap:992, setHwAccelMode:998, setGestureSensitivityMode:1004, setSeekbarStyle:1010, setShowPlayerNotifications:1016, setPlayerLayoutConfig:1022, resetPlayerLayoutConfig:1029, applyImportedState:1033, setPlayerSettings:1038, setPreferredAudioLanguages:1044, setApplyAudioLanguageToSelectedContentOnly:1050, setSelectedAudioFolders:1056, setSelectedAudioVideos:1062, setEnableAudioPitchCorrection:1068, setVolumeNormalization:1074, setBackgroundPlayback:1080, setAudioChannelMode:1086, setVolumeBoostCap:1092, setRememberSelectedAudioTrack:1098, setAppTheme:1105, setAmoledBlackMode:1111, setUseSystemFont:1117, setHapticFeedback:1123, setShowFullName:1131, setShowNewVideoLabel:1137, setNewVideoDaysThreshold:1143, setShowFolderUnplayedBadge:1149, setAutoScrollToLastPlayed:1155, setTreePathCompression:1161, setDualPaneView:1167, setWatchedThresholdPercent:1173, setShowVideoThumbnails:1180, setThumbnailStrategy:1186, setThumbnailQuality:1192, setTapThumbnailToSelect:1198, setShowNetworkThumbnails:1204, setShowHomeTab:1211, setShowMusicTab:1217, setShowRecentsTab:1223, setShowPlaylistsTab:1229, setControlsAnimationStyle:1236, setVideoOpeningAnimation:1242, setScreenNavigationStyle:1248, setTabNavigationStyle:1254, setAnimationSpeed:1260, startPlaying:1289, openChannel:1297, exitChannel:1303, openSharedPlaylist:1307, exitSharedPlaylist:1312, exitPlayer:1316, getTitle:1326, getSettingsDashboard:1330, getDone:1336, getTabPlayer:1342, getTabAudio:1348, getTabDecoder:1354, getTabSubtitles:1360, getTabAbout:1366, getAboutCategory:1372, getTabPlayerLayout:1378, getTabGestures:1384, getPlayerLayoutTitle:1390, getSeekbarStyleTitle:1396, getAppearanceCategory:1402, getFoldersCategory:1408, getPlayerCategory:1414, getAudioCategory:1420, getDecoderCategory:1426, getSubtitlesCategory:1432, getSectionAppearance:1438, getLightMode:1444, getDarkMode:1450, getSectionLanguage:1456, getSectionScaling:1462, getAppScaleZoom:1468, getSectionGlassBlur:1474, getGlassBlurTitle:1480, getGlassBlurSubtitle:1486, getGlassBlurLow:1492, getGlassBlurHigh:1498, getSectionPlayback:1504, getCopyTimestamp:1510, getCopyTimestampDesc:1516, getForceSideBySide:1522, getForceSideBySideDesc:1528, getSectionFastSeek:1534, getSectionGestureSensitivity:1540, getGestureLinearTitle:1546

**`ui/components/SettingsSheet.kt`**: getTitle:194, SettingsSheet:218, CategoryBanner:3498, EngineFeatureRow:3547, SubtitlesTab:3592, GestureSettingsPanel:4630, PlayerSettingsPanel:5090, PlayerSettingsCategory:5496, PlayerSwitchRow:5532, PlayerChoiceRow:5577, PlayerSliderRow:5740, PlayerTextRow:5837, PlayerActionRow:5901, PlayerActionRow:5933, PlayerActionRow:5965

**`util/LumoraSettingsXmlManager.kt`**: buildSettingsXml:36, exportSettingsToFile:360, parseSettingsFile:395, applySettings:801

**`ui/components/AboutSettingsPanel.kt`**: detectDisplayInfo:114, detectAudioInfo:262, detectHardwareInfo:363, AboutSettingsPanel:443, AlignLeftHeader:951, AboutInfoRow:964

**`ui/components/FoldersSettingsTabContent.kt`**: FoldersSettingsTabContent:120, refreshFolderLists:148, enterBlockListSelection:217, enterVaultSelection:226, exitSelectionMode:233, applyBlockListChanges:242, applyVaultChanges:264, FolderMetadataCard:1635, BlockedFolderRowItem:1759, PrivateFolderItemCard:1820

**`ui/components/AudioFolderVideoSelectorSheet.kt`**: AudioFolderVideoSelectorSheet:121, SelectableFolderItemCard:685, SelectableVideoItemCard:839
