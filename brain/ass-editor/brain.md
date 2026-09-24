# Advanced ASS/SSA Style Editor

> Read this first for: Per-style editing of ASS/SSA subtitle scripts (all tracks), color picker cards, override store.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `player/AdvancedAssStyleEngine.kt` | 630 | Pure-Kotlin ASS style parser/writer (`AssStyleInfo`, `AssStylesDocument`, `AssOverridesStore`, `AdvancedAssSource`). Rewrites only `Style:` columns of the ORIGINAL script text. Overrides JSON kept in `PlayerSettings.assStyleOverridesJson`, keyed by subtitle identity. |
| `ui/components/AdvancedAssStyleEditor.kt` | 943 | Expandable per-style cards UI, reuses the three-arc color picker from SubtitleStylePanel and fonts from SubtitleFontManager. |

## Flow

- Toggle Advanced in SubtitleStylePanel → AdvancedAssStyleEditor reads styles via MpvPlayerController.readAdvancedAssSource → user edits → AdvancedAssStyleEngine builds edited script → MpvPlayerController.setAdvancedAssState writes copy to cache `lumora_adv_ass` and swaps the selected sub track (no loadfile/seek).

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Style fields / rewrite rules | AdvancedAssStyleEngine.kt |
| Editor UI, color picker placement | AdvancedAssStyleEditor.kt (+ `ThreeArcCircularColorPickerCard` in SubtitleStylePanel.kt) |
| Apply edited script to mpv | MpvPlayerController.kt (`setAdvancedAssState`, `readAdvancedAssSource`) |

## Rules / gotchas

- Original subtitle file is never modified; an edited copy goes to cache.
- Style edits must apply to all tracks when the user chooses so (overrides are stored per subtitle identity).

## Talks to other areas

- Depends on: [design-system](../design-system/brain.md), [subtitles](../subtitles/brain.md), [video-player](../video-player/brain.md)
- Used by: [online-music](../online-music/brain.md), [subtitles](../subtitles/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`ui/components/AdvancedAssStyleEditor.kt`**: AdvancedAssStyleEditorContent:89, effectiveName:173, AdvancedAssInfoCard:237, AssStyleCard:254, set:268, AssSectionTitle:589, AssFieldRow:600, AssFieldLabel:609, AssTextField:625, AssFontField:685, AssFontRow:763, AssAlignmentRow:795, AssChip:816, AssInlineColorPicker:852, AssColourField:887
