# 🌸 StudyBloom

### Plan Smarter. Learn Better. Grow Every Day.

StudyBloom is an AI-powered study and productivity planner built for students, developers, and lifelong learners.

It helps users transform study materials and personal learning goals into organized daily and weekly schedules using AI.

With intelligent scheduling, live study timers, reminders, and progress analytics, StudyBloom aims to make learning more structured, productive, and consistent.

---

## ✨ Features

### 🤖 AI-Powered Study Planning
- Upload study materials and documents.
- Extract chapters, topics, and subtopics.
- Analyze topic difficulty and estimated study time.
- Generate personalized study schedules using AI.
- Adjust schedules according to available time and deadlines.

### 📅 Smart Scheduling
- Create daily and weekly study plans.
- Set available study hours and preferred time slots.
- Organize tasks according to priority and difficulty.
- Include breaks, revision sessions, and coding practice.
- Reschedule unfinished tasks intelligently.

### ⏱️ Live Study Timer
- Start a timer for an individual scheduled task.
- Track actual focused study time.
- Pause, resume, finish, or extend a session.
- View elapsed and remaining time.
- Preserve timer state when navigating away from the screen.
- Recover active sessions after closing and reopening the app.
- Track planned versus actual study duration.

### 🔔 Smart Reminders
- Receive notifications before scheduled tasks.
- Get reminders when study sessions begin.
- Receive gentle notifications when planned sessions end.
- Manage notification preferences.

### 📊 Progress Analytics
- Track daily and weekly study hours.
- Monitor completed and pending tasks.
- Compare planned study time with actual focused time.
- View learning consistency and productivity trends.

### 📄 Schedule Export
- Download daily schedules as PDF.
- Export weekly study plans.
- Keep a personal copy of your learning schedule.

### 🌐 Online and Offline Support

StudyBloom is designed to work both online and offline.

**Offline functionality:**
- View saved schedules.
- Manage tasks.
- Use the study timer.
- Track progress.
- Access locally stored materials.
- Receive locally scheduled reminders.

**Online functionality:**
- AI document analysis.
- AI-generated study plans.
- AI study assistant.
- Cloud synchronization and backup.

---

## 🎨 Design Philosophy

StudyBloom uses a clean, soft, and aesthetic interface designed to make studying feel organized and comfortable.

Design direction:
- Elegant English cursive calligraphy logo.
- Pastel lavender and cream color palette.
- Soft purple accents.
- Rounded cards and smooth animations.
- Beautiful circular study timer.
- Minimal navigation and readable typography.

**Brand tagline:** Let Your Knowledge Bloom.

---

## 🛠️ Technology Stack

The intended architecture uses native Android development.

| Component | Technology |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 |
| Architecture | MVVM |
| Local Database | Room |
| Preferences | DataStore |
| Background Tasks | WorkManager |
| AI Integration | Google Gemini API |
| Document Access | Android Storage Access Framework |
| Notifications | Android Notification System |
| Backend | Secure API server |
| Export | PDF generation |

*Update this section to reflect the actual technologies and versions used in the implementation.*

---

## 🏗️ Application Architecture

StudyBloom follows a modular architecture to keep the application maintainable and scalable.

Suggested project structure:

```text
StudyBloom/
│
├── app/
│   ├── src/main/
│   │   ├── java/com/studybloom/
│   │   │
│   │   ├── data/
│   │   │   ├── local/
│   │   │   ├── remote/
│   │   │   └── repository/
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── usecase/
│   │   │   └── repository/
│   │   │
│   │   ├── presentation/
│   │   │   ├── home/
│   │   │   ├── planner/
│   │   │   ├── timer/
│   │   │   ├── materials/
│   │   │   ├── progress/
│   │   │   └── profile/
│   │   │
│   │   └── navigation/
│   │
│   └── build.gradle.kts
│
├── backend/
│   ├── api/
│   ├── services/
│   ├── ai/
│   └── configuration/
│
├── README.md
└── .gitignore
```

The exact structure may differ depending on the final implementation.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio.
- Kotlin-compatible Android development environment.
- Android SDK.
- JDK version supported by the project.
- Internet connection for AI functionality.

### Installation

1. Clone the repository:

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

2. Open the project in Android Studio.

3. Allow Gradle to synchronize dependencies.

4. Configure the backend API endpoint.

5. Run the application on an Android emulator or physical device.

### AI Configuration

StudyBloom uses Gemini-powered AI functionality.

For security:
- Keep Gemini API credentials on the backend.
- Never hardcode API keys inside the Android application.
- Use authenticated API requests between the Android app and backend.
- Configure environment variables on the backend.

Example backend environment variable:

```env
GEMINI_API_KEY=your_api_key_here
```

Do not commit your real API key to GitHub.

---

## 🔐 Privacy and Data Management

StudyBloom should prioritize user privacy.

- Store essential application data locally.
- Request document access only when needed.
- Avoid uploading documents without user consent.
- Protect communication between the app and backend.
- Never expose API credentials in the APK.
- Provide appropriate controls for deleting user data.

---

## 🗺️ Future Improvements

Potential future development:

- Cloud synchronization across multiple devices.
- More advanced AI-generated revision strategies.
- Spaced repetition and flashcards.
- Coding practice planner.
- Integration with calendar applications.
- Study performance recommendations.
- More detailed productivity analytics.
- Personalized learning recommendations.

---

## 🤝 Contributing

Contributions, suggestions, and bug reports are welcome.

If you would like to contribute:

1. Fork the repository.
2. Create a feature branch.
3. Make your changes.
4. Test your implementation.
5. Submit a pull request.

---

## 📌 Project Status

StudyBloom is under development.

Features and implementation details may change as development progresses.

---

## 👨‍💻 Developer

**Project:** StudyBloom  
**Category:** AI-Powered Education & Productivity  
**Platform:** Android  
**Primary Language:** Kotlin

---

## 📜 License

Choose and add an appropriate open-source license before distributing the project.

---

<p align="center">

### 🌸 StudyBloom

**Let Your Knowledge Bloom.**

*Plan Smarter. Learn Better. Grow Every Day.*

</p>
