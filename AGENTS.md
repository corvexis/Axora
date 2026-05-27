# AGENTS.md

## Project
Axora — Android app (Kotlin + Compose, Material 3) for ADB/non-root device control, optional root. Fork of AxManager.

## Build
```bash
./gradlew assembleDebug                        # Manager debug APK
./gradlew :reignite:assembleDebug              # Build first to update embedded DEX, then rebuild manager
./gradlew assembleRelease                      # Minified release (needs signing key)
```
- Output APKs: `manager/build/outputs/apk/*/Axora_v*_*.apk`
- Release keystore: `axora-release.keystore` (alias `axora`), passwords in `local.properties` as `keystore.password` / `key.password`
- No tests, lint, or typecheck commands configured.

## Modules
| Module | Type | Role |
|--------|------|------|
| `:manager` | app | Main UI, Compose + Material 3. `applicationId = dev.axora.manager`, namespace `frb.axeron.manager` |
| `:server` | lib | Binder IPC server (separate process), native CMake (`-DANDROID_STL=none`) |
| `:adb` | lib | ADB pairing, native CMake |
| `:server:stub` | lib | Compile-only stubs for hidden Android APIs |
| `:reignite` | app | Plugin manager (`app_process`), DEX extracted to manager assets post-build |
| `api/` (9 modules) | mixed | Git submodule — see `api/AGENTS.md` for module details |

## API Submodule
`api/` → `https://github.com/corvexis/Axeron-API.git` (fork).
- Init: `git submodule update --init --recursive`
- Local override: `api.useLocal=true` + `api.dir=<path>` in `local.properties`
- **See `api/AGENTS.md`** — covers all submodule modules, AIDL source of truth, DEX extraction, publishing, ProcessPoolManager.
- `api/manifest.gradle.kts` is the canonical source for version (defines `apiVersionMajor/Minor/Patch` → `api_version_code`).

## Toolchain Quirks
- Gradle 8.13, Kotlin 2.2.21, AGP 8.13.2, JDK 21, NDK 29.0.14206865
- Compile/Target SDK 36, Min SDK 26, `RepositoriesMode.FAIL_ON_PROJECT_REPOS`
- Navigation: `io.github.raamcosta.compose-destinations` (KSP codegen — rebuild after changing nav routes)
- Hidden API bypass: `rikka.hidden` + `HiddenApiBypass` initialized in `AxeronApplication.kt`
- `libadb` native library loaded at app start on Android 11+ in `AxeronApplication.kt`
- Manager source: `manager/src/main/java/` (flat `java/`, not `kotlin/`)
- `:server` native code: `server/src/main/cpp/` — selinux, cgroups, process starter
- `AxeronApplication` extends `Engine()` (`frb.axeron.api.core.Engine`) — the app's init entrypoint

## Reignite Build Quirk
Building `:reignite` extracts `classes*.dex` to `manager/src/main/assets/scripts/ax_reignite.dex`. **Always build reignite before manager** when reignite changes. Minified mappings → `out/mapping/reignite-v*.txt`.

## Version Scheme
- Code: `api_version` from `gradle.properties` (currently `14800`) + git commit count via `git rev-list --count HEAD`
- Name: `{api_version_name}.r{commit_count}` (e.g. `1.4.8.r355`)
- `api/manifest.gradle.kts` is the canonical source for `api_version` computation (major\*10000 + minor\*1000 + patch)

## CI (GitHub Actions)
- `deploy-website.yml` — Build & deploy VitePress site to GitHub Pages on `main` pushes modifying `website/**`
- `fastlane-metadata.yml` — Generate fastlane changelog from release body on `release: published`

## Website (VitePress)
```bash
cd website && bun install && bun run dev   # Dev server at localhost
cd website && bun run build                # Static build → website/docs/.vitepress/dist
```

## Recent Feature Reference
`recent_changes.md` documents the auto-restart on server death feature (ActivateViewModel + PowerDialog changes). Read it for context on server lifecycle handling.
