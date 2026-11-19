# 🎬 Aura Player

[![Latest Release](https://img.shields.io/github/v/release/khanistiak/Aura-Player?style=for-the-badge&logo=github&color=2563EB)](https://github.com/khanistiak/Aura-Player/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/khanistiak/Aura-Player/total?style=for-the-badge&label=DOWNLOADS&color=10B981)](https://github.com/khanistiak/Aura-Player/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/khanistiak/Aura-Player/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

A modern, high-performance Android video player built with **Jetpack Compose** and **AndroidX Media3 (ExoPlayer)**, featuring a real-time **Liquid Glass** interface, hardware/software decoding with bundled FFmpeg audio support, and an instant zero-copy **Private Folder (Vault)**.

---

## 📥 Download

Download the latest signed release APK directly from **[GitHub Releases](https://github.com/khanistiak/Aura-Player/releases/latest)**.

---

## ✨ Key Features

### 🎨 Liquid Glass Interface & Navigation
- **🪟 Cut-to-Cut Real-Time Backdrop Blur:** Custom pure-Compose `GraphicsLayer` recording and hardware `BlurEffect` headers with seamless edge-to-edge scrolling, frosted folder cards, and a floating bottom navigation capsule.
- **🌈 Unified Ambient Canvas & Multi-Palette Themes:** Consistent sky-cyan and soft lavender ambient gradient across every screen and navigation transition, plus 16+ customizable color palettes (`Aurora`, `Catppuccin`, `Dracula`, `Nord`, `Emerald`, `Rosé Pine`, and more) and **App Disguise** launcher icons (`Aura Player`, `Calculator`, `Notes`).
- **💫 Smooth 60/120fps Navigation:** Gliding Aura Blue sliding tab indicator between **Home** and **All Videos**, plus optimized backdrop capture and thumbnail caching for stutter-free list scrolling.
- **📂 Dual Browsing & Safe File Management:** Hierarchical **Tree View** directory explorer and flat **All Videos** library with live search, quick filter chips (`Unwatched`, `4K / HD`, `Subtitled`, `Large >1 GB`), unwatched folder badges, SD Card / USB OTG volume switching, and redesigned Aura Blue dialogs for **Rename**, **Move**, **Copy**, **Playlists**, and **Delete** confirmations.

### ⚡ Advanced Media3 & FFmpeg Playback
- **🎞️ Universal Codec Support:** Hardware (`HW`), Hybrid (`HW+` with bundled FFmpeg audio decoding for EAC3, DTS, TrueHD, AC3), and Software (`SW`) decoding modes.
- **⏭️ Interactive Skip & Next Video Popups:** Smart chapter detection that displays tap-to-execute Liquid Glass popups for **Skip Intro**, **Skip Outro**, and **Next Video** (auto-hiding cleanly if ignored).
- **〰️ Pixel-Style Wavy Seekbar:** Animated sinusoidal progress bar with instant tap-to-seek, drag scrubbing, and chapter markers.
- **🔁 Precision Playback Tools:**
  - **A–B Loop Repeater** for seamless scene looping.
  - **Sleep Timer** (`15m`, `30m`, `45m`, `60m`, or `End of current video`) with smooth audio fade-out.
  - **Night-Mode Warmth Filter** for late-night eye comfort.
  - **Smart Orientation Engine:** Opens portrait and landscape videos in their native orientation on frame zero without rotation flashes.
  - **Seamless Auto-PiP:** Android 12+ Picture-in-Picture with native `Rewind 10s`, `Play/Pause`, and `Forward 10s` controls.
- **🔊 Rich Gesture Controls:** Vertical brightness and volume swipes (with up to **200% software boost** and customizable brightness memory modes), horizontal seek scrubbing, pinch-to-zoom, two-finger speed adjustment, and hold-to-fast-forward.
- **💬 Full Subtitle Suite:** External `.srt` / `.ass` loading, live sync/speed offset controls, and customizable font size, color, bolding, and margin.

### 🔒 Instant Zero-Copy Private Folder (Vault)
- **🛡️ Zero-Copy `.kv` Relocation:** Moves locked videos in milliseconds into a hidden `.nomedia`-protected `.private` directory—zero file duplication and dedicated confirmation dialogs before locking.
- **👆 Biometric & PIN Protection:** 4-digit PIN lock, fingerprint/face unlock via `BiometricPrompt`, security question recovery, and instant original-path restoration.

### 🔄 Built-In GitHub Releases Updater
- **🚀 Seamless Update Alerts:** Checks `khanistiak/Aura-Player` releases automatically on launch and from **Settings → About**, sending status-bar notifications and launching one-tap APK downloads.

---

## 🛠️ Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3 + Custom `GraphicsLayer` / `RenderEffect` Blur)
- **Media Engine:** AndroidX Media3 (`ExoPlayer`) + FFmpeg Audio Extension
- **Persistence:** Jetpack DataStore & EncryptedSharedPreferences
- **Minimum SDK:** Android 8.0 (API 26) | **Target SDK:** Android 17 (API 37)

---

## 🚀 Building from Source

1. Clone the repository:
   ```bash
   git clone [https://github.com/khanistiak/Aura-Player.git](https://github.com/khanistiak/Aura-Player.git)

 ##  👨‍💻 Developer
Developed with ❤️ in Bangladesh by **Khan Istiak Shahabuddin**
