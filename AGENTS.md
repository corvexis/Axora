# AGENTS.md

## Project Overview
Axora is an Android app (Kotlin + Compose) for ADB/non-root device control with optional root support. Fork of AxManager.

## Build Commands
```bash
./gradlew assembleDebug          # Build main manager APK
./gradlew assembleRelease        # Release build (minified)
./gradlew :reignite:assembleDebug  # Build reignite sub-app
```

Output APKs: `manager/build/outputs/apk/*/Axora_v*_*.apk`

## Module Structure
- `:manager` - Main app (applicationId: `frb.axeron.manager`)
- `:server` - Core server library with native CMake components
- `:adb` - ADB communication library with native code
- `:reignite` - Standalone app, deploys DEX into manager assets
- `:server:stub` - Compile-only stub for server

## API Submodule (Git Submodule)
The `api/` directory is a git submodule pointing to `https://github.com/matsuzaka-yuki/Axeron-API.git`
- Initialize: `git submodule update --init --recursive`
- Local override: Set `api.useLocal=true` and `api.dir=<path>` in `local.properties`

## Key Dependencies
- AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.12.00
- Compile SDK 36, Min SDK 26, Target SDK 36
- Java/Kotlin target: JVM 21
- Native: NDK 29.0.14206865, CMake with `-DANDROID_STL=none`
- Hidden API access via `rikka.hidden` libraries

## Reignite Build Quirk
Reignite extracts `classes*.dex` from its APK and copies it to `manager/src/main/assets/scripts/ax_reignite.dex` as a post-build step. If minified, mapping goes to `out/mapping/reignite-v*.txt`.

## Website (VitePress)
Located in `website/`, requires Bun:
```bash
cd website && bun install && bun run dev    # Dev server
cd website && bun run build                # Build static site
```
Deployed to GitHub Pages via CI on pushes to `main`.

## Version Scheme
Version code: `api_version` property + git commit count
Version name: `{api_version_name}.r{commit_count}`
API version defined in `api/manifest.gradle.kts` (submodule)

## Compose Navigation
Uses `io.github.raamcosta.compose-destinations` with KSP processor. Destinations generated at build time.
