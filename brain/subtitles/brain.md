# Subtitles (formats, fonts, style, external files, persistence)

> Read this first for: Subtitle format conversion to ASS, font resolution for libass, style panel, external subtitle files, attached-subtitle persistence, per-video subtitle selection, session memory across episodes.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `player/UniversalSubtitleEngine.kt` | 875 | Converts every supported subtitle format (SRT/VTT/SAMI/LRC/TTML/MicroDVD/SBV/…) into a synthetic ASS script so one libass/mpv path renders all. Uses AssParser. |
| `ui/screens/VideoSubtitleProcessor.kt` | 425 | Now only `SubtitleCue` (data model) and `AssParser` (ASS/SSA parse/export). The old SubtitleEngine/format parsers were dead and removed. |
| `player/SubtitleFontManager.kt` | 937 | Fills mpv `sub-fonts-dir` (assets + system + video-folder + embedded-in-file fonts). Multi-script/Devanagari fallback is load-bearing. |
| `ui/components/SubtitleStylePanel.kt` | 1890 | Subtitle style panel: font, size, colors, position, background, outline. Hosts the Advanced ASS toggle. |
| `ui/components/SubtitleFileExplorerSheet.kt` | 698 | Browser to add an external subtitle file (`SubtitleFileItem`). |
| `util/CustomSubtitlePersistenceManager.kt` | 525 | Permanently attached user subtitles (`AttachedSubtitleRecord`) and per-video subtitle selection (`VideoSubtitleSelection`, `SavedSubtitleRef`). Prefs `custom_subtitles_persistence_v1`, dir `user_attached_subtitles`. |
| `util/SubtitleSessionMemory.kt` | 106 | In-session memory so the next episode auto-selects the same subtitle track number / on-off state. |

## Flow

- User picks/attaches a subtitle → SubtitleFileExplorerSheet / PlayerGlassSelectionSheet.SubtitleSelectionPanel → VideoPlayerScreen → CustomSubtitlePersistenceManager.addAttachedSubtitle → MpvPlayerController.add/select track.
- Non-ASS formats: UniversalSubtitleEngine → temp ASS in cache `sub_transcoded` → libass through mpv.
- Selection per video: CustomSubtitlePersistenceManager.saveSubtitleSelection / getSubtitleSelection; next-episode auto-pick: SubtitleSessionMemory.findMatchingTrackForNextEpisode.
- AI translated subtitles are written to filesDir `translated_subtitles` (see ai-translation).

## Storage owned (SharedPreferences names / cache & files dirs)

- `player/UniversalSubtitleEngine.kt`: `sub_transcoded`
- `player/SubtitleFontManager.kt`: `sub_fonts`
- `util/CustomSubtitlePersistenceManager.kt`: `custom_subtitles_persistence_v1`, `user_attached_subtitles`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Subtitle format support | UniversalSubtitleEngine.kt |
| ASS parse/export | VideoSubtitleProcessor.kt (`AssParser`) |
| Fonts not rendering / tofu boxes | SubtitleFontManager.kt |
| Subtitle appearance controls | SubtitleStylePanel.kt |
| Attached vs uploaded lists, saved selection | CustomSubtitlePersistenceManager.kt + SubtitleSelectionPanel in PlayerGlassSelectionSheet.kt |
| Subtitle track choice on next episode | SubtitleSessionMemory.kt |

## Rules / gotchas

- Do not touch subtitle parsing, mpv decoding or the playback engine when fixing UI/persistence bugs.
- Global 'Override ASS/SSA' is suppressed for ASS tracks while Advanced ASS is ON (see ass-editor).
- Keep `GoNotoCurrent-Regular.ttf`-style fallback logic in SubtitleFontManager intact: it prevents missing-glyph boxes.

## Talks to other areas

- Depends on: [ass-editor](../ass-editor/brain.md), [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [video-player](../video-player/brain.md)
- Used by: [app-shell](../app-shell/brain.md), [ass-editor](../ass-editor/brain.md), [audio-player](../audio-player/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [settings](../settings/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`player/UniversalSubtitleEngine.kt`**: prepareSubtitleForPlayback:43, readTextWithCharsetDetection:117, parseSrt:172, parseWebVtt:240, parseSami:308, parseLrc:357, parseTtml:408, parseTtmlTime:451, extractAttribute:475, parseMicroDvd:486, parseSbv:550, parseMpl2:602, processHtmlAndPositionTags:631, processVttTagsAndSettings:677, parseColorToAssBgr:733, decodeHtmlEntities:779, formatAssTime:792, generateAssScript:806

**`player/SubtitleFontManager.kt`**: registerVideoEmbeddedFont:23, clearVideoEmbeddedFonts:42, normalizeFontName:46, getSubFontsDir:50, initializeFontEnvironment:66, syncFonts:83, ensureDefaultFontAliases:156, sanitizeAssForFallback:165, importFontFromUri:228, importFontsFromTree:265, getPreferredFamilyName:318, getFontFamilyName:379, getInstalledFonts:388, deleteCustomFont:411, deleteAllCustomFonts:422, copyAssetFonts:437, copyVideoFolderFonts:467, extractEmbeddedAssFonts:506, saveEncodedFontFile:546, decodeAssFontData:564, cleanupLegacyGeneratedFonts:599, isFontAvailable:638, extractEmbeddedMkvFonts:707, parseAndSaveMkvAttachedFile:752, readEbmlId:823, readEbmlVint:842, extractFontNamesFromTtfBytes:865, extractFontNamesFromTtfFile:924

**`ui/components/SubtitleStylePanel.kt`**: SubtitleStylePanel:137, AccordionSectionCard:302, EnhancedTypographyContent:366, GradientIconPill:792, FullSliderCard:847, EnhancedColorsContent:947, ThreeArcCircularColorPickerCard:1087, emitColor:1106, drawThreeArcColorPicker:1345, EnhancedMiscContent:1525, DetailedSwitchRow:1803, FontItemRow:1853
