# 🌸 StudyBloom (v1.0.0)

<p align="center">
  <img src="app/src/main/res/drawable/img_studybloom_brand.png" alt="StudyBloom Banner" width="600"/>
</p>

<p align="center">
  <strong>Plan gently. Learn consistently. Grow every day.</strong><br>
  <em>Let Your Knowledge Bloom.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Release-v1.0.0-8B70DF?style=for-the-badge&logo=android" alt="Release v1.0.0"/>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android" alt="Platform Android"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Architecture-Offline--First_MVVM-34D399?style=for-the-badge" alt="Architecture"/>
  <img src="https://img.shields.io/badge/Database-Room_SQLite_v4-FFA000?style=for-the-badge&logo=sqlite" alt="Room SQLite"/>
</p>

---

## 📖 Overview

**StudyBloom** is a native, offline-capable Android application built with Kotlin and Jetpack Compose designed specifically for programmers, university students, and self-directed learners. 

Rather than functioning as a rudimentary to-do checklist, StudyBloom pairs an **intelligent AI study engine** with an **aesthetic task clock & live study timer**, automated scheduling, and a local-first Room database. Learners upload their syllabi, programming textbooks, or documentation, define their daily availability, and receive structured, realistic study blocks tailored to their personal cognitive rhythms.

---

## 🌟 What's New in Version 1.0 (v1)

### 🌸 Elegant Wordmark & Adaptive App Icon Branding
* **Custom Calligraphy Identity:** Deep purple calligraphy lettering with lavender wave accents, an open-book motif, fresh green leaves, and delicate sparkles.
* **Full Adaptive Icon Layering:** Multi-density launcher mipmaps (`hdpi`, `mdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) with a soft cream (`#FCF6EE`) vector background and 66dp safe-zone foreground artwork.
* **Integrated In-App Assets:** High-resolution brand hero banners and wordmark drawables featured across Home, Onboarding, and Profile screens.

### 🚀 Welcoming Onboarding & Unrestricted Guest Mode
* **First-Time Welcome Screen:** Warm greeting (*"Welcome to StudyBloom 🌸 • Your personal space to plan, learn, and grow."*) with zero forced registration.
* **Instant Guest Mode:** Full access to all study scheduling, document analysis, live study timers, local notifications, and progress tracking with zero account barriers.
* **Encrypted Local Storage:** All guest data is stored on-device in Room SQLite.
* **Non-Intrusive Profile Indicator:** Subtle `👤 Guest Account` top-bar badge keeping the UI clean and distraction-free.

### 🌱 Smart Account Creation & 5-Step Verified Migration
* **Gentle Reminders:** Non-intrusive dialog (*"Your progress is worth protecting! 🌱"*) outlining the benefits of cloud backup, multi-device sync, and device recovery.
* **Timer Safety Guard:** Never interrupts an active study session or running timer; triggers gently only after a completed session or from the Profile screen.
* **Verifiable Data Migration Pipeline:**
  1. *Confirm Local Data:* Audits and counts local Room entities (tasks, sessions, materials, topics).
  2. *Verify Credentials:* Validates email format and password strength while checking network connectivity.
  3. *Encrypted Cloud Upload:* Safely transfers existing records with live progress reporting.
  4. *Parity Verification:* Verifies 100% parity between uploaded records and local database entries.
  5. *Mark Registered:* Activates cloud backup flags and sync timestamps without duplicating data.
* **Zero Data Loss Guarantee:** If network disconnects during migration, local data is left completely intact for safe retry.

### ⏱️ Aesthetic Task Clock & Persistent Live Timer
* **Circular Analog Design:** Large, elegant circular clock face with a moving clock hand, smooth circular progress ring, and pastel lavender glowing accents.
* **Task Schedule Connected:** Directly linked to scheduled tasks; calculates actual start times, elapsed focus time, overtime, and pause durations.
* **Background & Navigation Resilience:** Powered by `TaskTimerManager`; active sessions persist when navigating between tabs, switching apps, or reopening the app.
* **Persistent Bottom Banner:** Interactive `ActiveTimerPill` floating above the navigation bar allowing 1-tap pause/resume and quick clock view.

### 🌐 Online-Enabled, Offline-Capable Architecture
* **Real-Time Connectivity Monitor:** Detects Wi-Fi, Cellular, and Offline modes instantly.
* **Offline AI Request Queue:** AI document analysis and scheduling requests initiated offline are queued in Room (`PendingAiRequestEntity`) and automatically processed upon reconnect.
* **Visual Status Pill:** Top-bar pill displaying connection status (`Online • Wi-Fi`, `Offline Mode Active`, or `Syncing...`) with tap-to-inspect guarantees.

---

## 📱 Key Features

| Feature | Description |
|---|---|
| **📚 AI Material Breakdown** | Upload textbooks, code snippets, or notes (PDF, TXT, Code). AI extracts modules, difficulty ratings (EASY, MEDIUM, HARD), prerequisite sequences, and time estimates. |
| **📅 Intelligent Schedule Wizard** | Multi-step planner that builds realistic daily blocks around your wake/sleep times, peak focus hours, and pomodoro breaks without double-booking. |
| **⏱️ Aesthetic Task Clock** | Circular analog study clock with live elapsed/remaining countdowns, overtime tracking, and completion logging. |
| **🤖 Contextual AI Assistant** | In-app study mentor sheet for real-time advice, debugging help, and gentle schedule adjustments. |
| **📈 Progress & Analytics** | Daily and weekly focus hour charts, completion rates, study streak tracking, and session histories. |
| **📄 PDF & CSV Schedule Export** | Generates beautifully styled printable daily/weekly schedule PDFs and CSV files for external calendars. |
| **🔔 Gentle Notifications** | Android notification system with dedicated channels for session reminders and completion alerts. |

---

## 🛠️ Technical Architecture & Stack

StudyBloom is built with modern, idiomatic Android practices following Clean Architecture and MVVM.

```text
app/src/main/java/com/example/
├── MainActivity.kt                     # Single activity entry point with edge-to-edge support
├── ui/
│   ├── StudyBloomApp.kt               # Central navigation scaffold, tabs, and sheet controllers
│   ├── theme/                         # Material 3 colors (Lavender, Mint, Pink, Cream), Type, Shape
│   ├── screens/
│   │   ├── HomeScreen.kt              # Daily schedule, streak pill, connection status, quick actions
│   │   ├── TaskClockScreen.kt         # Fullscreen aesthetic analog circular timer
│   │   ├── MyPlanScreen.kt            # Calendar-based daily & weekly planner
│   │   ├── MaterialsScreen.kt         # Study documents, topic breakdown & AI extraction
│   │   ├── ProgressScreen.kt          # Focus charts, streak statistics & session history
│   │   ├── ProfileScreen.kt           # Account status, cloud backup, theme & reminder settings
│   │   ├── OnboardingScreen.kt        # Welcome screen, Guest Mode entry & personalization
│   │   ├── ScheduleWizardScreen.kt    # Step-by-step AI schedule generator
│   │   └── FocusTimerScreen.kt        # Quick Pomodoro focus tool
│   ├── components/                    # Reusable M3 cards, badges, dialogs, sheets
│   │   ├── AuthDialog.kt              # Account registration & sign-in modal
│   │   ├── AccountCreationReminderDialog.kt # Gentle progress protection reminder
│   │   ├── ActiveTimerPill.kt         # Persistent timer indicator pill above navigation
│   │   ├── ConnectionStatusPill.kt    # Real-time online/offline network indicator
│   │   └── AiAssistantSheet.kt        # Interactive AI study assistant modal
│   └── viewmodel/
│       └── StudyBloomViewModel.kt     # Single source of truth for UI state & coroutine operations
├── data/
│   ├── local/                         # Room Database (v4)
│   │   ├── StudyBloomDatabase.kt      # Database builder, seed callback, migrations
│   │   ├── dao/                       # DAOs for Tasks, Sessions, Materials, Topics, Profile, AI Queue
│   │   └── entity/                    # Room entities with indexes and relations
│   ├── auth/
│   │   └── AuthManager.kt             # Guest-to-Account verified migration engine
│   ├── ai/
│   │   ├── AiRepository.kt            # Gemini API & offline fallback engine
│   │   └── AiRepositoryImpl.kt        # Document analysis & schedule generation algorithms
│   ├── sync/
│   │   └── SyncManager.kt             # Cloud sync orchestrator & offline queue processor
│   └── repository/
│       └── StudyBloomRepository.kt    # Unified data repository
└── util/
    ├── NetworkConnectivityMonitor.kt  # StateFlow-based network callback listener
    ├── TaskTimerManager.kt            # Timestamp-based resilient study clock manager
    ├── NotificationHelper.kt          # Notification channels & reminder triggers
    └── PdfExporter.kt                 # Native Android PdfDocument generation
```

### Technology Matrix

* **Language:** Kotlin 2.0.21
* **UI Toolkit:** Jetpack Compose (1.7.5) with Material 3 (1.3.1)
* **Local Persistence:** Room Database 2.6.1 with KSP (Kotlin Symbol Processing)
* **Asynchronous Flow:** Kotlin Coroutines & `StateFlow`
* **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
* **Document Export:** Native Android `android.graphics.pdf.PdfDocument`
* **Network Sensing:** Android `ConnectivityManager.NetworkCallback`
* **AI Capabilities:** Google Gemini AI Model Integration (with structured JSON generation)
* **Testing:** JUnit 4 + Robolectric unit testing suite (all tests passing)

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio:** Ladybug (2024.2.1) or newer
* **Android SDK:** Compile SDK 35, Minimum SDK 26 (Android 8.0 Oreo)
* **JDK:** Java 17 or Java 21

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/StudyBloom.git
   cd StudyBloom
   ```

2. **Open in Android Studio:**
   Select **File > Open** and choose the `StudyBloom` project directory.

3. **Configure Environment Secrets (Optional):**
   If integrating your personal Google Gemini API key for server-side AI processing, copy `.env.example`:
   ```bash
   cp .env.example .env
   ```
   Add your Gemini API key in the Secrets configuration or `.env`:
   ```env
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```

4. **Build and Run:**
   * Select your connected physical device or Android Virtual Device (AVD).
   * Click **Run** (`Shift + F10`) or execute via Gradle:
   ```bash
   gradle assembleDebug
   ```

5. **Run Unit Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 🔒 Privacy, Security & Data Ownership

* **Local-First Privacy:** All tasks, uploaded learning materials, topic notes, and focus logs are stored locally on your device in an encrypted Room SQLite database.
* **No Forced Cloud Lock-in:** Guest Mode allows unrestricted daily use without transmitting personal credentials.
* **Transparent Sync:** Users have complete control over cloud backups and can manually sync or sign out at any time.

---

## 📄 License

StudyBloom is open-source software licensed under the [Apache License, Version 2.0](LICENSE).

---

<p align="center">
  <strong>StudyBloom v1.0.0</strong><br>
  <em>Designed and engineered with care for students and developers worldwide. 🌸</em>
</p>
