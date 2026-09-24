# Private Vault

> Read this first for: Password-protected private folders for video/audio.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `util/PrivateVaultManager.kt` | 530 | Vault setup/unlock (PBKDF2 salted credentials), private folders under filesDir `private_vault`, prefs `lumora_private_vault_secure`. Uses VideoFileManager/AudioFileManager for moves. |

## Flow

- Folders settings / Home → PrivateVaultManager (authenticate → list/move media in/out).

## Storage owned (SharedPreferences names / cache & files dirs)

- `util/PrivateVaultManager.kt`: `lumora_private_vault_secure`, `private_vault`

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Vault rules / auth | PrivateVaultManager.kt |
| Vault entry in UI | FoldersSettingsTabContent.kt, HomeScreen.kt, AudioMusicTabContent.kt |

## Rules / gotchas

- Plaintext credentials must never be persisted.

## Talks to other areas

- Depends on: [audio-player](../audio-player/brain.md), [library-home](../library-home/brain.md)
- Used by: [audio-player](../audio-player/brain.md), [library-home](../library-home/brain.md), [settings](../settings/brain.md)
