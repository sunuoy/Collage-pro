<div align="center">

# 🎨 Collage Pro

**The ultimate modern Android application for crafting stunning photo collages, custom layouts, and exporting professional A4 PDF documents.**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/badge/Release-v1.1.0-brightgreen.svg)](https://github.com/sunuoy/Collage-pro/releases)

---

</div>

## 🚀 Key Features

### 🖼️ Smart Photo Collage Creation
- **Flexible Grid Templates**: Choose from a variety of multi-photo grid templates tailored for portraits, landscapes, and social media posts.
- **Interactive Drag-and-Drop**: Effortlessly rearrange, swap, scale, and align photos with responsive touch controls.
- **Custom Borders & Drop Shadows**: Add depth to your creations with real-time drop shadow adjustments, border spacing, and rounded corner sliders.

### 📄 Professional A4 PDF Export
- **Orientation Toggle (Portrait & Landscape)**: Export your collages directly into print-ready A4 PDF documents with a single tap.
- **High-Resolution Output**: Preserves image quality for sharp printing and digital sharing.

### 🤖 AI Studio & Smart Enhancements
- **Powered by Gemini & Firebase AI**: Integrated AI capabilities for intelligent content analysis, auto-alignment, and creative layout suggestions.

### ⚡ Premium UI & Architecture
- **Material 3 Design System**: Beautiful dark mode and light mode aesthetics designed with Jetpack Compose.
- **Room Database Storage**: Save draft projects locally and resume editing anytime.

---

## 📲 Installation & Downloads

You can download the latest compiled APK directly from the **Releases** section:

👉 **[Download Latest APK (v1.1.0)](https://github.com/sunuoy/Collage-pro/releases)**

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Language**: Kotlin
- **Asynchronous Flow**: Kotlin Coroutines & Flow
- **Image Loading**: Coil Compose
- **Database**: Room Persistence Library
- **Networking & AI**: Retrofit, Moshi, OkHttp, Firebase AI SDK (Gemini)
- **CI/CD Automation**: GitHub Actions (Automated build artifacts & GitHub Releases)

---

## 👨‍💻 Local Development Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/sunuoy/Collage-pro.git
   cd Collage-pro
   ```

2. **Configure Environment Variables:**
   Create a `.env` file in the project root (refer to `.env.example`):
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Open and Run:**
   - Open the project in **Android Studio (Ladybug or newer)**.
   - Build and run on an Android Emulator (API 24+) or physical device.

---

<div align="center">
  <sub>Built with ❤️ using Android Jetpack Compose and Gemini AI.</sub>
</div>
