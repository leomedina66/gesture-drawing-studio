# 🎨 Gesture Drawing Studio

A modern desktop application built for artists to practice gesture drawing with automated reference image sessions, timed countdowns, and non-repeating image selection.

---

## ✨ Features

- 📁 **Local Folder Reference Support**: Scan any local folder for reference images (`.jpg`, `.jpeg`, `.png`, `.webp`, `.gif`).
- 🎲 **Non-Repeating Random Shuffle**: Automatically shuffles images so no pose repeats within a single session.
- ⏱️ **Discrete Countdown Timer**: Custom pose duration with quick preset buttons (`30s`, `1 min`, `2 min`, `5 min`).
- ⌨️ **Keyboard Controls**:
  - `Space` — Pause / Resume session
  - `Right Arrow` — Skip to next pose
  - `Left Arrow` — Go to previous pose
  - `Esc` — Finish session early
- 📐 **Modular & Responsive UI**: Responsive image scaling that automatically adapts to window dimensions without cropping or distortion.
- 💾 **Persistent Settings**: Automatically remembers your last selected reference folder, pose timer duration, pose count, window size, and monitor position across restarts.
- 🖼️ **Post-Session Gallery**: Summary screen with total practice time, completed pose counts, and a thumbnail grid of practiced poses.

---

## 🛠️ Technology Stack

- **Core Language**: Java 21 / 25
- **UI Framework**: JavaFX 21 (`javafx-controls`, `javafx-graphics`)
- **Build System**: Apache Maven (`pom.xml`)
- **Testing Framework**: JUnit 5 (`junit-jupiter`)

---

## 🏗️ Project Architecture

```
gesture-drawing-app/
├── pom.xml                                     # Maven Dependencies & Build Configuration
├── .gitignore                                  # Git exclusion rules
├── README.md                                   # Project Overview & Guide
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── gesturedrawing/
    │   │           ├── MainApp.java            # JavaFX Application Launcher & Scene Orchestrator
    │   │           ├── domain/
    │   │           │   ├── GestureImage.java   # Image metadata & URI conversion
    │   │           │   ├── SessionConfig.java  # Session configuration model & validations
    │   │           │   └── SessionResult.java # Post-session metrics model
    │   │           ├── service/
    │   │           │   ├── ImageLoaderService.java # Directory scanner & shuffle logic
    │   │           │   ├── PreferencesService.java # Persistent user settings manager
    │   │           │   └── SessionManager.java     # Active session state machine
    │   │           └── ui/
    │   │               ├── MainScreen.java     # Setup & config screen
    │   │               ├── SessionScreen.java  # Fullscreen image viewer & timer controls
    │   │               └── SummaryScreen.java  # End-of-session summary & thumbnail gallery
    │   └── resources/
    │       └── styles.css                      # Custom Studio Dark Theme stylesheet
    └── test/
        └── java/
            └── com/
                └── gesturedrawing/             # Unit tests (SessionConfigTest, ImageLoaderServiceTest)
```

---

## 🚀 How to Run

### Run from Command Line
```powershell
C:\Users\Amanda\.gemini\antigravity\tools\apache-maven-3.9.9\bin\mvn.cmd javafx:run -f C:\Users\Amanda\.gemini\antigravity\scratch\gesture-drawing-app
```

### Run Tests
```powershell
C:\Users\Amanda\.gemini\antigravity\tools\apache-maven-3.9.9\bin\mvn.cmd clean test -f C:\Users\Amanda\.gemini\antigravity\scratch\gesture-drawing-app
```

---

## 🗺️ Roadmap (Future Releases)

- [ ] **Version 2**: Practice History & Session Logs (saving date, duration, pose count to local file).
- [ ] **Version 3**: Varied Session Intervals (e.g. 10x30s + 5x1m + 2x5m warm-ups).
- [ ] **Version 4**: Practice Statistics & Graphs (Weekly/Monthly practice totals).
- [ ] **Version 5**: Goal System (Weekly target minutes/poses).
