# Build, CI, tests

> Read this first for: Gradle config, dependency catalog, ProGuard/R8, GitHub Actions, unit tests.
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Non-Kotlin files / config

| Path | Role |
|---|---|
| `build.gradle.kts, settings.gradle.kts, gradle.properties` | Root plugins, repositories (google, mavenCentral, JitPack), Gradle flags. |
| `app/build.gradle.kts` | namespace `com.example`, applicationId `god.kartik.lumora`, SDK 26/36, ABI splits + universal APK, signing fallback to debugConfig, R8 minify + shrinkResources, dependencies. |
| `gradle/libs.versions.toml` | Version catalog (only entries actually used). |
| `app/proguard-rules.pro` | Keep rules for mpv JNI, NewPipeExtractor/Rhino, `ui.state.**`, audio settings. |
| `.github/workflows/build-apk.yml` | CI: duplicate-resource / stray res-folder check, assembleRelease, upload 4 ABI APKs + universal. |
| `app/src/test/java/com/example/` | Real tests: ExampleUnitTest (embedded font preservation), util/OnlineRecommendationMixerTest, util/SubtitleExtractionTimingTest, ui/screens/MediaExtractionNamingTest. |
| `metadata.json, .env.example, README.md` | Google AI Studio bookkeeping – keep. `.env.example` is referenced by the Secrets plugin config. |

## Flow

- Push to main → GitHub Actions → assembleRelease → APK artifacts.

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Dependency version | gradle/libs.versions.toml |
| SDK/ABI/signing | app/build.gradle.kts |
| CI | build-apk.yml |
| Keep rules | proguard-rules.pro |

## Rules / gotchas

- No build tool is available in most AI sessions: make the smallest change and re-check imports/braces by hand.
- Never add a resource dir to sourceSets. Never change namespace/applicationId.
- Release APK is minified: unused Kotlin code/resources are stripped by R8 anyway, so cleanups mostly help source size, build time and AI navigation.

## Talks to other areas

- Depends on: none
- Used by: none
