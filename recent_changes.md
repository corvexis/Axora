# Recent Changes

## Auto-Restart on Server Death

### Problem
When Android kills the `axeron_server` process (LMK, phantom process killer, etc.), the manager app logs the death but never attempts to restart. User must manually re-activate.

### Solution
Added automatic restart with exponential backoff when the server process dies unexpectedly (not user-initiated).

### Files Changed

**`api/api/src/main/java/frb/axeron/api/core/AxeronSettings.java`**
- Added `ENABLE_AUTO_RESTART` setting (default `true`), with `getEnableAutoRestart()` / `setEnableAutoRestart()`

**`manager/src/main/java/frb/axeron/manager/ui/viewmodel/ActivateViewModel.kt`**
- Added `MAX_AUTO_RETRIES = 10` constant
- Added auto-restart state: `autoRestartJob`, `retryCount`, `wasRunning`, `intentionalStop`, `restartContext`
- Added `markIntentionalStop()` — called before user-initiated shutdown/restart to suppress auto-restart
- Added `setRestartContext(context)` — called from composables to provide Context for ADB restart
- Modified `init` collector: on `Running` → resets retries and clears `intentionalStop`; on `Disable` + wasRunning + !intentionalStop + setting enabled → calls `launchAutoRestart()`
- Added `launchAutoRestart()` — retries with exponential backoff (1s, 2s, 4s, 8s… max 30s), up to 10 attempts
- Added `tryRootRestart()` — runs `Shell.cmd(Starter.internalCommand).exec()` if root available
- Added `tryAdbRestart()` — calls `AdbStarter.startAdbClient(context, tcpPort)` using stored TCP port
- All manual start methods (`startRoot`, `startAdbWireless`, `startAdbTcp`) now clear `intentionalStop`

**`manager/src/main/java/frb/axeron/manager/ui/screen/Home.kt`**
- PowerDialog `onShutdown` and `onRestart` now call `activateViewModel.markIntentionalStop()` before action
- Added `activateViewModel.setRestartContext(context)` in LaunchedEffect

**`manager/src/main/java/frb/axeron/manager/ui/screen/HomeCircle.kt`**
- Same PowerDialog and context changes as Home.kt

**`manager/src/main/java/frb/axeron/manager/ui/viewmodel/SettingsViewModel.kt`**
- Added `isAutoRestartEnabled` state + `setAutoRestart()` setter

**`manager/src/main/java/frb/axeron/manager/ui/screen/settings/ActivationSettings.kt`**
- Added auto-restart toggle using `Icons.Filled.Replay`
- New string resources: `auto_restart` / `auto_restart_desc`
- New parameter: `isAutoRestartEnabled` / `onAutoRestartChange`

**`manager/src/main/java/frb/axeron/manager/ui/screen/Settings.kt`**
- Wired `settings.isAutoRestartEnabled` and `{ settings.setAutoRestart(it) }`

**`manager/src/main/res/values/strings.xml`**
- Added `auto_restart` / `auto_restart_desc` string resources

### Behavior
| Scenario | Action |
|----------|--------|
| System kills server | Auto-restart kicks in with backoff (up to 10 retries) |
| User presses Shutdown | `intentionalStop` set, no auto-restart |
| User presses Restart | `intentionalStop` set, no auto-restart (restart itself already re-launches) |
| Binder comes back | Retry counter reset to 0 |
| Auto-restart setting OFF | No auto-restart attempted |
| Last launch = ROOT | Restarts via `Shell.cmd(Starter.internalCommand)` |
| Last launch = ADB + TCP | Restarts via `AdbStarter.startAdbClient()` using stored port/key |
| Last launch = ADB + Wireless | Falls back to TCP restart (if port stored); logs otherwise |

---

## UI Polish + QuickShell Saved Commands + WorkManager Health Check

### Heartbeat Pulse (HomeCircle.kt)
- Added `rememberInfiniteTransition` + `animateFloat` scale pulse (1.0 → 1.08, 1.5s cycle, `RepeatMode.Reverse`) on `CheckCircle` icon in `StatusCardCircle`.
- Pulse only active when server is running.

### Flash Status Animation (Flash.kt)
- **animateColorAsState**(tween 600ms) on `TopBar` title color for smooth ORANGE↔GREEN↔RED transitions.
- **LinearProgressIndicator** (fillMaxWidth) below TopAppBar, visible only during `FlashingStatus.FLASHING`.
- **FlashErrorCard** composable — `TonalCard` with `errorContainer` colors, `Warning` icon, exit code explanation via `flashErrorExplanation(code)` helper (143→SIGTERM hint, other→generic), dismiss button, expandable stderr with `AnimatedVisibility`.
- `flashResult` nullable state set only when `code != 0` to reduce recomposition.

### Saved Commands for QuickShell (QuickShell.kt / QuickShellViewModel.kt)
- Bookmark icon (`Icons.Outlined.Bookmark`) in top bar opens `SavedCommandsSheet` (ModalBottomSheet).
- Save bookmark icon beside TextField persists current command text.
- `SavedCommandsSheet` lists saved commands; tapping loads command into TextField for review (not auto-execute); X button removes.
- Empty state text when no commands saved.
- Persisted as JSON string array in SharedPreferences key `saved_commands` using Gson.
- `savedCommandsList` uses `by mutableStateOf(...)` with list replacement on every mutation (copy-on-write) for Compose recomposition.

### WorkManager Periodic Health Check (ServerHealthScheduler / ServerHealthWorker)
- **ServerHealthScheduler** — schedules 30-min periodic WorkManager job with no battery/charging/idle constraints.
- **ServerHealthWorker** — checks if server is dead (binder ping fails), was previously running, and auto-restart is enabled; if so, restarts via root (`Shell.cmd(Starter.internalCommand)`) or ADB TCP (`AdbStarter.startAdbClient`).
- **BootCompleteReceiver** — calls `ServerHealthScheduler.schedule(context)` after boot-initiated root/ADB start.
- **ActivateViewModel** — removed local `wasRunning` in favor of `AxeronSettings.setWasRunning()`. Schedules health check on `Running`, cancels on `markIntentionalStop()`. Added initial `pingBinder()` check at Flow collection start to emit `Disable` immediately if server already dead.
- Added `work-runtime-ktx` v2.10.0 dependency.

### AGENTS.md
- Removed stale API module list.
- Escaped asterisk in version scheme markdown.
- Added "Recent Feature Reference" section pointing to `recent_changes.md`.

### Files Changed

**New files:**
- `manager/src/main/java/frb/axeron/manager/service/ServerHealthScheduler.kt`
- `manager/src/main/java/frb/axeron/manager/service/ServerHealthWorker.kt`

**Modified files:**
- `AGENTS.md` — cleanup + recent_changes reference
- `gradle/libs.versions.toml` — added `work` v2.10.0 version + `androidx-work-runtime-ktx` library
- `manager/build.gradle.kts` — added `androidx.work.runtime.ktx` dependency
- `manager/src/main/java/frb/axeron/manager/receiver/BootCompleteReceiver.kt` — schedule health check after boot start
- `manager/src/main/java/frb/axeron/manager/ui/screen/Flash.kt` — animateColorAsState, progress indicator, FlashErrorCard
- `manager/src/main/java/frb/axeron/manager/ui/screen/HomeCircle.kt` — heartbeat pulse animation
- `manager/src/main/java/frb/axeron/manager/ui/screen/QuickShell.kt` — saved commands sheet + bookmark icons
- `manager/src/main/java/frb/axeron/manager/ui/viewmodel/ActivateViewModel.kt` — wasRunning via Settings, health check schedule/cancel, ping check
- `manager/src/main/java/frb/axeron/manager/ui/viewmodel/QuickShellViewModel.kt` — saved commands persistence with Gson

---

## newProcessDetached AIDL + Bookmark Save Flash

### Problem
Flash processes were killed after 5 minutes (ProcessPoolManager timeout) or when the manager's binder to the server dropped (RemoteProcessHolder DeathRecipient), producing exit code 143 (SIGTERM).

### Solution
Added `newProcessDetached` AIDL method that spawns processes without pool timeout and without DeathRecipient linking.

### Files Changed

**`api/server-shared/.../ProcessPoolManager.java`**
- Added `registerProcess(Process, long timeoutMs)` overload.
- When `timeoutMs <= 0`, no timeout callback is scheduled.
- Existing `registerProcess(Process)` delegates with `defaultTimeoutMs`.

**`api/aidl/.../IAxeronService.aidl`**
- Added `IRemoteProcess newProcessDetached(...)` at method ID 24.

**`server/.../AxeronService.kt`**
- Implemented `newProcessDetached`: acquires pool slot (still limits concurrency), starts process, calls `pool.registerProcess(process, 0)` (no timeout), returns `RemoteProcessHolder(process, null)` (no DeathRecipient).

**`api/api/.../Axeron.java`**
- Added 3 `newProcessDetached()` overloads mirroring `newProcess`.

**`api/api/.../AxeronPluginService.kt`**
- `execWithIO()` now calls `Axeron.newProcessDetached()` instead of `Axeron.newProcess()`.
- Fixes flash operations, web UI shell commands, and all other `execWithIO` callers.

**`manager/src/main/.../QuickShell.kt`**
- Bookmark save icon tint changes to `MaterialTheme.colorScheme.primary` (user's accent color) for 1.5s after tapping, then reverts to `onSurfaceVariant`.

**`manager/src/main/.../Flash.kt`**
- Updated `flashErrorExplanation(143)` to accurately describe the pool timeout rather than incorrectly blaming app closure.

---

## Floating Pill Bottom Nav + QuickShell Clear Command Toggle

### Floating Pill Bottom Navigation Bar

**`manager/src/main/java/frb/axeron/manager/ui/AxActivity.kt`**
- Bottom bar is now a floating pill: `Card` shape changed to `RoundedCornerShape(50)` (full pill), elevation `8.dp` for shadow, wrapped in `wrapContentWidth()` + `padding(16.dp)` margins so it floats in the bottom center.
- `NavigationBar` + `NavigationBarItem` replaced with a `Row` of custom `Box(clickable)` per tab.
- **Icons only** — no labels, no text, no `alwaysShowLabel`. Selected tab gets `secondaryContainer` background + `onSecondaryContainer` tint; unselected gets transparent + `onSurfaceVariant`.
- Badge on Plugin tab preserved.
- NavHost bottom padding reduced from `80.dp` to `72.dp`.

### QuickShell Clear Command Toggle

**`manager/src/main/java/frb/axeron/manager/ui/viewmodel/QuickShellViewModel.kt`**
- Added `isClearCommandEnabled` state (default `true`) + `setClearCommand()` persist to SharedPreferences key `clear_command`.
- `commandText = TextFieldValue("")` now guarded by `if (isClearCommandEnabled)` in both `onProcessCreated` and `onProcessRunning`.

**`manager/src/main/java/frb/axeron/manager/ui/screen/QuickShell.kt`**
- Added `SettingsItem` toggle in `ExtraSettings` bottom sheet with `Icons.Outlined.Input` icon.

**`manager/src/main/res/values/strings.xml`** (and zh-rCN, zh-rTW, ja)
- Added `clear_command` / `clear_command_desc` string resources.
