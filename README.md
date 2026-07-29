# 🎨 Gesture Drawing Studio

A modern, high-performance desktop application designed for artists to practice gesture drawing and figure studies with timed reference image sessions, non-repeating pose shuffling, and memory-optimized image pipelines.

---

## ✨ Key Features

- 📁 **Local Reference Library**: Load any local directory containing reference images (`.jpg`, `.jpeg`, `.png`, `.webp`, `.gif`).
- 🎲 **Non-Repeating Random Shuffle**: Automatically shuffles images so no pose repeats within a single session.
- ⏱️ **Discrete Countdown Timer**: Custom pose duration with one-click presets (`30s`, `1 min`, `2 min`, `5 min`).
- ⚡ **2-Image Memory Pipeline (Zero-Lag)**: Asynchronously pre-loads upcoming poses in the background for instant transitions without UI stutters or RAM leaks.
- 🔍 **Crystal-Clear Native Resolution**: Renders full-resolution artwork with high-quality bilinear filtering for High-DPI / 4K displays.
- 📐 **Responsive Modular Layout**: Controls and timer remain anchored and unobstructed while reference artwork scales dynamically to fit screen dimensions.
- 💾 **Persistent Settings**: Automatically remembers your last used reference folder, duration, pose count, window dimensions, and screen position across restarts.
- 🖼️ **Post-Session Gallery**: Displays summary statistics and a thumbnail grid of all poses completed during the session.

---

## ⌨️ Keyboard Controls

| Key | Action |
|---|---|
| `Space` | Pause / Resume session timer |
| `Right Arrow` | Skip to next pose |
| `Left Arrow` | Return to previous pose |
| `Esc` | Finish session early |

---

## 🛠️ Technology Stack & Architecture

- **Language**: Java 21+
- **UI Framework**: JavaFX 21 (`javafx-controls`, `javafx-graphics`)
- **Build System**: Apache Maven (`pom.xml`)
- **Testing**: JUnit 5 (`junit-jupiter`)

### Directory Structure

```
gesture-drawing-app/
├── pom.xml                                     # Maven dependencies & build settings
├── README.md                                   # Project documentation
├── .gitignore                                  # Git exclusion rules
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── gesturedrawing/
    │   │           ├── MainApp.java            # Main JavaFX launcher & scene manager
    │   │           ├── domain/
    │   │           │   ├── GestureImage.java   # Image metadata representation
    │   │           │   ├── SessionConfig.java  # Session configuration domain model
    │   │           │   └── SessionResult.java # Session summary metrics
    │   │           ├── service/
    │   │           │   ├── ImageLoaderService.java   # Directory scanner & shuffle logic
    │   │           │   ├── ImagePreloaderService.java# 2-Image memory pipeline & preloading
    │   │           │   ├── PreferencesService.java   # Persistent user preferences
    │   │           │   └── SessionManager.java       # Session state machine
    │   │           └── ui/
    │   │               ├── MainScreen.java     # Session setup & settings view
    │   │               ├── SessionScreen.java  # Active gesture viewer & controls
    │   │               └── SummaryScreen.java  # End-of-session gallery & stats
    │   └── resources/
    │       └── styles.css                      # Studio dark theme stylesheet
    └── test/
        └── java/
            └── com/
                └── gesturedrawing/             # Unit tests (SessionConfigTest, ImageLoaderServiceTest)
```

---

## 🚀 Download & Running

### Option 1: Standalone Windows App (Zero Setup)
Download the latest pre-packaged **[Gesture-Drawing-Studio-Windows-x64.zip](https://github.com/leomedina66/gesture-drawing-studio/releases)**:
1. Extract the `.zip` folder.
2. Double-click **`Gesture Drawing Studio.exe`** to start drawing immediately!
*(No Java, Maven, or setup required!)*

---

### Option 2: Build & Run from Source

#### Prerequisites
- **Java JDK 21+**
- **Apache Maven 3.8+**

1. **Clone the repository:**
   ```bash
   git clone https://github.com/leomedina66/gesture-drawing-studio.git
   cd gesture-drawing-studio
   ```

2. **Run via Maven:**
   ```bash
   mvn javafx:run
   ```

3. **Build Standalone Executable Package (`.exe` + JRE):**
   ```cmd
   build-installer.bat
   ```

3. **Run unit tests:**
   ```bash
   mvn clean test
   ```

---

## 🗺️ Roadmap & Future Enhancements

- [x] **Version 1 (MVP)**: Automated timed sessions, non-repeating shuffle, 2-image memory pipeline, persistent settings, and summary gallery.
- [ ] **Version 2**: Practice History & Session Logs (saving practice dates, durations, and pose counts locally).
- [ ] **Version 3**: Custom Warm-up Pipelines (e.g., 10x 30s + 5x 1m + 2x 5m interval sequences).
- [ ] **Version 4**: Practice Analytics & Graphs (Weekly and monthly practice breakdown).
- [ ] **Version 5**: Goal & Habit Tracking System (Weekly target minutes and pose quotas).

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
