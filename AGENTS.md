# AGENTS.md

## Project
Axora — Android app (Kotlin + Compose, Material 3) for ADB/non-root device control, optional root. Fork of AxManager.

## Setup
- The `api/` directory is a **git submodule** and is empty on a fresh clone. Builds fail without it:
  ```bash
  git submodule update --init --recursive
  ```
- Local override: `api.useLocal=true` + `api.dir=<path>` in `local.properties` (see `settings.gradle.kts:38-47`).

## Build
```bash
./gradlew :reignite:assembleDebug   # First — extracts DEX into manager assets (see Reignite Quirk)
./gradlew assembleDebug             # Then the manager
./gradlew assembleRelease           # Minified; FAILS without local keystore (see below)
```
- Output APKs: `manager/build/outputs/apk/*/Axora_v*_*.apk`
- No tests configured.
- **Release keystore gotcha**: `axora-release.keystore` is gitignored (was deleted from the repo in commit `7d1b800`) but `manager/build.gradle.kts:28` still references `../axora-release.keystore`. You must provide the file locally plus `keystore.password` / `key.password` in `local.properties`, or `assembleRelease` fails at signing.

## Modules
| Module | Type | Role |
|--------|------|------|
| `:manager` | app | Main UI, Compose + M3. `applicationId = dev.axora.manager`, namespace `frb.axeron.manager`. Source in `manager/src/main/java/` (flat `java/`, not `kotlin/`) |
| `:server` | lib | Binder IPC server (separate process); native `libaxeron.so` executable — selinux, cgroups, process starter |
| `:adb` | lib | ADB pairing; native code |
| `:server:stub` | lib | Compile-only stubs for hidden Android APIs |
| `:reignite` | app | Plugin manager (`app_process`), DEX extracted to manager assets post-build |
| `api/` | submodule | See `api/settings.gradle.kts` for its modules (aidl, api, shell, shared, provider, server-shared, rish, axerish, runtime, demo-axerish). `api/manifest.gradle.kts` defines the canonical version. AIDL source of truth lives here. |

## Toolchain
- Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21, JDK 21, NDK 29.0.14206865. compileSdk/targetSdk 36, minSdk 26, `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- `:server` and `:adb` build native code with `-DANDROID_STL=none`.
- Navigation: `io.github.raamcosta.compose-destinations` (KSP codegen — rebuild after changing nav routes).
- `AxeronApplication` extends `Engine()` (`frb.axeron.api.core.Engine`) — app init entrypoint. Loads `libadb` on Android 11+; sets `HiddenApiBypass` exemptions on SDK ≥ 28.

## Reignite Build Quirk
`reignite/build.gradle.kts` hooks `assembleProvider.get().doLast` to extract `classes*.dex` from the reignite APK into `manager/src/main/assets/scripts/ax_reignite.dex`. **Always build `:reignite` before `:manager`** when reignite changes. Minified mapping → `out/mapping/reignite-v*.txt`.

## Manager App Conventions
- **No DI framework**: ViewModels are created with `viewModel()` and manually aggregated into `ViewModelGlobal`, which is passed via compose-destinations' `dependency()` API (`AxActivity.kt:220`). When adding a new ViewModel, wire it into `ViewModelGlobal` and every call site that uses it.
- **Hidden IPC activities**: `RequestPermissionActivity`, `WebUIActivity`, `Toast`, and `ShellRequestHandlerActivity` are declared in `AndroidManifest.xml` but aren't navigation screens. They're Binder/shell IPC entry points. Don't remove or restructure them without understanding their callers.

## Version Scheme
- Canonical source: `api/manifest.gradle.kts` — `apiVersionCode = major*10000 + minor*1000 + patch` (e.g. 1.5.100 → 15100).
- `gradle.properties` **must be manually kept in sync** (`api_version`/`api_version_name`) — the top-level build reads `api_version` from it directly.
- Version name appends commit count: `{api_version_name}.r{git rev-list --count HEAD}`.

## CI (GitHub Actions)
- `deploy-website.yml` — build & deploy VitePress to GitHub Pages on `main` pushes touching `website/**`.
- `fastlane-metadata.yml` — generates `fastlane/metadata/android/en-US/changelogs/{versionCode}.txt` from release body on `release: published` (parses version from `api/manifest.gradle.kts`).

## Website (VitePress)
```bash
cd website && bun install && bun run dev    # dev server
cd website && bun run build                 # → website/docs/.vitepress/dist
```

## Recent Feature Reference
`recent_changes.md` documents recent features: server auto-restart (ActivateViewModel lifecycle), WorkManager server health checks, QuickShell saved commands, floating pill bottom nav. Read it before touching server lifecycle / shell UI code.
