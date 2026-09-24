# Design system (theme, glass components, icons, resources)

> Read this first for: Theme/colors/typography, liquid-glass components, icon wrapper, bounce interaction, pull-to-refresh, drawables, fonts, branding.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `ui/theme/Color.kt` | 34 | Color palette. |
| `ui/theme/Theme.kt` | 139 | `VideoPlayerTheme` wrapper. |
| `ui/theme/Type.kt` | 143 | Typography + LTWave font family. |
| `ui/components/LiquidGlassComponents.kt` | 226 | Glass card/dialog primitives (`LiquidGlassDefaults`, `LiquidGlassCard`, `LiquidGlassDialog`, `configureEdgeToEdgeDialogWindow`). |
| `ui/components/StyledIcon.kt` | 182 | Universal icon wrapper (used by ~33 files). |
| `ui/components/BounceInteraction.kt` | 179 | Press-bounce modifiers (`bounceClick`, `cardBounceClick`, `cardBounceCombinedClick`). |
| `ui/components/LiquidPullToRefresh.kt` | 633 | Pull-to-refresh gesture/animation. |

## Non-Kotlin files / config

| Path | Role |
|---|---|
| `app/src/main/res/drawable/` | Vector icons `lumora_*.xml` (+ launcher layers `ic_launcher_*`, `ic_homeicon`, `ic_abouticon`). Only R.drawable.* referenced names are kept. |
| `app/src/main/res/font/ltwave_bold.otf` | UI font (used by Type.kt). |
| `app/src/main/assets/fonts/LTWave-Bold.otf` | Identical copy on purpose: SubtitleFontManager copies everything under assets/fonts into the subtitle font dir, so LT Wave is selectable as a subtitle font. |
| `branding/ic_homeicon.svg` | Source SVG of the launcher icon. |

## Flow

- Icons: always `StyledIcon(R.drawable.lumora_x)`. Dialogs/cards: compose the LiquidGlass primitives.

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Colors/typography | Color.kt / Type.kt / Theme.kt |
| Glass look | LiquidGlassComponents.kt |
| Add an icon | Drop `lumora_<name>.xml` into res/drawable and use StyledIcon |
| App icon | branding/ic_homeicon.svg → ic_launcher_* drawables |

## Rules / gotchas

- Check res/drawable for an existing icon before adding a new one.
- Never create a second resource folder.

## Talks to other areas

- Depends on: [settings](../settings/brain.md)
- Used by: [ai-translation](../ai-translation/brain.md), [app-shell](../app-shell/brain.md), [ass-editor](../ass-editor/brain.md), [audio-player](../audio-player/brain.md), [library-home](../library-home/brain.md), [media-info-mkv](../media-info-mkv/brain.md), [online-music](../online-music/brain.md), [playlists-history](../playlists-history/brain.md), [settings](../settings/brain.md), [subtitles](../subtitles/brain.md), [video-player](../video-player/brain.md)
