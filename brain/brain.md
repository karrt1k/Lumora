# Lumora — Project Brain (start here)

Native Android video + audio player (Kotlin, Jetpack Compose, mpv via `mpv-android-lib`). Built with Google AI Studio's Android APK build; pushed to GitHub, CI builds APKs.

- applicationId `god.kartik.lumora` · Gradle namespace/package `com.example` (do not change) · minSdk 26 · compile/target 36
- Source root: `app/src/main/java/com/example/` (`player/`, `ui/components/`, `ui/screens/`, `ui/state/`, `ui/theme/`, `util/`, `ai/`, `MainActivity.kt`)

## How to work (saves time)
1. Find your task in the router table below.
2. Open ONLY that area's `brain/<area>/brain.md`; it lists files, edit map, gotchas and a symbol index for huge files.
3. Open only the file/function named there. Do not scan the whole project.
4. After a change that adds/renames/removes files, storage keys or features, update that area's brain.md.

## Router

| Task involves… | Read |
|---|---|
| MainActivity, top-level screen switching, runtime permissions, Picture-in-Picture, VIEW/SEND intents, AndroidManifest, res/xml. | [app-shell](app-shell/brain.md) |
| Home screen, video folder scanning, library cache, folder/video cards, sort & view options, thumbnails, bottom tabs, file operations on videos. | [library-home](library-home/brain.md) |
| Everything that plays a video: mpv controller, player screen, controls overlay, glass selection panels (audio/subtitle/speed/zoom/chapters/EQ/playlist), gestures, PiP, A-B loop, screenshot, control-layout editor, per-player settings. | [video-player](video-player/brain.md) |
| Subtitle format conversion to ASS, font resolution for libass, style panel, external subtitle files, attached-subtitle persistence, per-video subtitle selection, session memory across episodes. | [subtitles](subtitles/brain.md) |
| Per-style editing of ASS/SSA subtitle scripts (all tracks), color picker cards, override store. | [ass-editor](ass-editor/brain.md) |
| Media Information screen, metadata probing, MKV/EBML reading & writing (tracks, chapters, attachments, embedded fonts). | [media-info-mkv](media-info-mkv/brain.md) |
| Music tab, standalone audio player, background playback service, notification, metadata/lyrics/thumbnails, audio library cache, audio file operations. | [audio-player](audio-player/brain.md) |
| YouTube-sourced search & streaming via NewPipeExtractor, prebuffer/cache, recommendations, artist channel screen, shared playlist screen, online track metadata cache. | [online-music](online-music/brain.md) |
| Custom playlists, add-to-playlist, Recents tab, video playback history/resume records. | [playlists-history](playlists-history/brain.md) |
| Settings sheet, every settings enum/state, persistence, export/import of settings, storage cleanup, haptics. | [settings](settings/brain.md) |
| Cloud-based subtitle translation, provider/API-key management. | [ai-translation](ai-translation/brain.md) |
| Password-protected private folders for video/audio. | [private-vault](private-vault/brain.md) |
| Theme/colors/typography, liquid-glass components, icon wrapper, bounce interaction, pull-to-refresh, drawables, fonts, branding. | [design-system](design-system/brain.md) |
| Gradle config, dependency catalog, ProGuard/R8, GitHub Actions, unit tests. | [build-ci](build-ci/brain.md) |

## Global rules
- Do not touch working features, playback engine, video decoding or subtitle parsing unless asked.
- Keep existing design language, colors, theme; animations smooth and subtle.
- Never reintroduce Media3/ExoPlayer; everything plays via mpv.
- One resources folder only (`app/src/main/res`); never add icon-res/res2 or new sourceSets.
- Reuse before creating: `StyledIcon`, LiquidGlass components, existing file explorers/pickers. No `XyzScreen2.kt` forks.
- Persisted settings live only in `ui/state/AppConfig.kt`.
- No compiler is guaranteed in AI sessions: keep edits minimal, re-check imports and braces.

## Cleanup history
- Cleanup pass: removed ~95 unreferenced functions/classes (old subtitle-editor remnants in VideoSubtitleProcessor/AppConfig, unused player preview, dead MKV helpers), 2 empty component files, 11 unused drawables, template test stubs, unused version-catalog entries, 184 unused imports. Single per-area brains replaced the old 106KB brain.md.
