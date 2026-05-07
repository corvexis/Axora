# AGENTS.md

## Project Overview
Axora: Android app (Kotlin + Compose) for ADB/non-root device control, optional root. Fork of AxManager.

## Build Commands
```bash
./gradlew assembleDebug                # Main manager APK
./gradlew assembleRelease              # Minified release APK
./gradlew :reignite:assembleDebug      # Reignite sub-app
```

Output APKs: `manager/build/outputs/apk/*/Axora_v*_*.apk`

## Module Structure
- `:manager`: Main app (applicationId: `frb.axeron.manager`)
- `:server`: Core server library, native CMake components
- `:adb`: ADB communication library, native code
- `:reignite`: Standalone app, deploys DEX to manager assets
- `:server:stub`: Compile-only stub for server

## API Submodule
The `api/` directory is a git submodule pointing to `https://github.com/matsuzaka-yuki/Axeron-API.git`
- Initialize: `git submodule update --init --recursive`
- Local override: Set `api.useLocal=true` and `api.dir=<path>` in `local.properties`

## Toolchain Quirks
- Native: NDK 29.0.14206865, CMake with `-DANDROID_STL=none` (non-default STL)
- Hidden API access via `rikka.hidden` libraries
- Compose navigation: Uses `io.github.raamcosta.compose-destinations` with KSP; destinations generated at build time

## Reignite Build Quirk
Reignite extracts `classes*.dex` from its APK to `manager/src/main/assets/scripts/ax_reignite.dex` post-build. Build reignite first to update embedded dex. Minified mappings: `out/mapping/reignite-v*.txt`.

## Website (VitePress)
Located in `website/`, requires Bun:
```bash
cd website && bun install && bun run dev    # Dev server
cd website && bun run build                # Static build
```
Deployed to GitHub Pages via CI on `main` pushes modifying `website/**`.

## Version Scheme
- Version code: `api_version` (submodule) + git commit count
- Version name: `{api_version_name}.r{commit_count}`
- API version defined in `api/manifest.gradle.kts`


