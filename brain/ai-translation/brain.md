# AI Subtitle Translation (cloud)

> Read this first for: Cloud-based subtitle translation, provider/API-key management.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `ai/SubtitleTranslationService.kt` | 361 | Format-preserving translator for SRT/VTT/ASS: batching, protected-token validation, progress. |
| `ai/AiProviderService.kt` | 355 | HTTP client for OpenAI-compatible providers (model discovery, validation, structured chat completion). |
| `ai/AiFeatureModels.kt` | 157 | Models/enums: `AiProvider`, `AiModelInfo`, `ApiKeyVerificationState`, `TrackTranslationStatus`, `TranslateLanguages`. |
| `ai/AiFeaturesSettingsStore.kt` | 108 | Persistence (`lumora_ai_features_settings`): provider, encrypted key, models, prompts, languages. |
| `ai/ApiKeyCrypto.kt` | 68 | Android Keystore AES-GCM encryption of API keys. |
| `ui/components/AiFeaturesSettingsPanel.kt` | 1282 | Settings UI for AI providers/keys/prompts. |

## Flow

- Subtitle panel → SubtitleTranslationService → AiProviderService → translated file in filesDir `translated_subtitles` → MpvPlayerController loads it.

## Storage owned (SharedPreferences names / cache & files dirs)

- `ai/AiFeaturesSettingsStore.kt`: `lumora_ai_features_settings`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Provider list/model selection | AiFeatureModels.kt + AiProviderService.kt |
| Translation prompt/batching | SubtitleTranslationService.kt |
| Key storage | AiFeaturesSettingsStore.kt + ApiKeyCrypto.kt |
| Settings UI | AiFeaturesSettingsPanel.kt |

## Rules / gotchas

- Cloud only; no on-device model. API keys are never stored in plain text.
- `.env` GEMINI_API_KEY is unused by app code.

## Talks to other areas

- Depends on: [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [video-player](../video-player/brain.md)
- Used by: [settings](../settings/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`ui/components/AiFeaturesSettingsPanel.kt`**: AiFeaturesSettingsPanel:50, update:61, checkBalance:77, SectionHeader:991, GlassCard:997, SettingsDivider:1011, ToggleRow:1019, PlayerChoiceRow:1072, OutlinedFieldLike:1230, OutlinedActionButton:1258
