# Axora

> A minimalistic, optimized fork of [AxManager](https://github.com/fahrez182/AxManager) for efficient ADB/non-root Android device control with optional root support.

**Axora** is a streamlined fork of [AxManager](https://github.com/fahrez182/AxManager), focused on delivering a lightweight, high-performance client for Android device management. It retains core ADB/non-root control capabilities while removing unnecessary bloat, prioritizing speed, simplicity, and resource efficiency.

Unlike root-dependent managers such as KernelSU, Axora is designed **ADB/Non-Root first** — root access is fully supported but never required, ensuring compatibility with a wide range of devices.

## Key Features

- **Minimalistic Design**  
  Clean, uncluttered interface and workflow focused solely on essential device control tasks.
- **Performance Optimizations**  
  Tuned for faster startup times, reduced memory usage, and smoother operation compared to upstream forks.
- **Full ADB/Non-Root Support**  
  Complete functionality without root access; optional root integration for advanced use cases.
- **Core Tools**  
  Built-in shell executor, unrooted plugin management, and WebUI for browser-based shell access.
- **Optional Root Compatibility**  
  Seamlessly leverages root access when available, with no dependency on root permissions.

## Build & Install

Clone the repository and build using Android Studio or Gradle:

```bash
git clone https://github.com/corvexis/Axora.git
cd Axora
./gradlew assembleDebug
```

Install the built APK to your device via ADB:

```bash
adb install manager/build/outputs/apk/debug/Axora_v*.apk
```

## Contribution

Contributions are welcome! Open issues, submit pull requests, or start discussions to propose improvements or report bugs.

## Credits

### Original Project
**[AxManager](https://github.com/fahrez182/AxManager)** by **fahrez182**  
Axora is derived from AxManager. Redistributions or modifications must retain attribution to the original developer, fahrez182.

### References & Inspirations
- **[Magisk](https://github.com/topjohnwu/Magisk)** — Inspiration for BusyBox integration and unrooted plugin concepts
- **[Shizuku](https://github.com/RikkaApps/Shizuku)** — Reference for ADB-based permission handling and Android IPC
- **[KernelSU](https://github.com/tiann/KernelSU)** — Inspiration for UI components and WebUI features

## Legal Disclaimer

This project includes adapted code from:
- AxManager (© Fahrez182), licensed under Apache License 2.0
- Shizuku Manager (© Rikka Apps), licensed under Apache License 2.0
- Other open-source projects as credited above

Axora does not distribute original visual assets from AxManager or Shizuku Manager, nor does it claim to be an official replacement. All adapted code complies with the Apache License 2.0.

## License

Licensed under the [Apache License 2.0](LICENSE).
