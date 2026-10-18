# Gesture Drawing Studio

A desktop app for timed gesture and figure drawing practice. Point it at a folder of reference images, choose how long each pose lasts, and it shows the images one at a time with a countdown — as a simple session or as a sequence of phases such as `2 × 1 min → 5 × 2 min → 1 × 10 min`.

Built with Java 21 and JavaFX 21. Packaged for Windows as a standalone app with its own Java runtime.

---

## Features

**Sessions**
- **Simple sessions** — one duration for every pose, with presets for 30 s, 1, 2 and 5 minutes.
- **Phased sessions** — chain up to 10 phases, each with its own pose count and duration. Includes three presets (Warm-up, Classic, Long Study), a live total of poses and time, and a phase indicator next to the timer.
- **Shuffled, non-repeating poses** — images are shuffled for every session. When the session needs more poses than there are images, they are reused, but the same image is never shown twice in a row.
- **Pause, previous and next** — going back to a pose from an earlier phase restores that phase's duration.
- **Session summary** — total practice time, poses completed (a pose counts once you have spent time on it and moved on), and a thumbnail gallery of the images practiced.

**Viewing**
- **Horizontal flip and grayscale** — toggle during a session or set as defaults.
- **Always on top** — keep the window above your drawing app.
- **Sharp at any size** — images are shown at native resolution up to your screen's physical resolution; larger photos are decoded directly at screen size to save memory.
- **Instant navigation** — the next image is preloaded in the background and the previous one is kept, so Next and Prev don't wait for decoding.
- **Resilient loading** — unreadable files and images above 100 megapixels are skipped automatically with a short notice instead of stalling the session.

**Convenience**
- Remembers your last folder, session settings, phase plan, display options, and window size and position.
- Select a whole folder or a hand-picked set of files.

Supported formats: `.jpg`, `.jpeg`, `.png`, `.gif`, `.bmp`.

---

## Keyboard shortcuts

| Key | Action |
|---|---|
| `Space` | Pause / resume |
| `→` | Next pose |
| `←` | Previous pose |
| `H` | Flip horizontally |
| `G` | Grayscale |
| `T` | Always on top |
| `Esc` | Finish the session |

Right-click the image for the same display toggles. Holding a toggle key switches it only once.

---

## Getting started

### Download (Windows)

Download the latest build from the [Releases](https://github.com/leomedina66/gesture-drawing-studio/releases) page, extract it, and run `Gesture Drawing Studio.exe`. No Java installation is needed.

> **"Windows protected your PC" warning**
> The app is not code-signed yet, so Microsoft Defender SmartScreen may block it the first time you run it. This warning appears for unsigned apps downloaded from the internet; it does not mean a threat was detected. To open the app, either:
> - click **More info** → **Run anyway** (pt-BR: *Mais informações* → *Executar assim mesmo*), or
> - before extracting, right-click the `.zip` → **Properties** → check **Unblock** → **OK** (pt-BR: *Propriedades* → *Desbloquear*).
>
> You can compare the file against the SHA-256 checksum listed in the release notes.

### Build from source

**Requirements:** JDK 21 or newer and Apache Maven 3.8+ on `PATH`.

```bash
git clone https://github.com/leomedina66/gesture-drawing-studio.git
cd gesture-drawing-studio
mvn javafx:run
```

Run the tests:

```bash
mvn test
```

### Package the Windows app

```cmd
build-installer.bat
```

This runs the tests, builds a self-contained app image with a trimmed Java runtime (about 85 MB in total) and places it in `dist\Gesture Drawing Studio\`. The previous build in `dist\` is only replaced once the new one has been created successfully.

The bundled runtime comes from the JDK that provides `jpackage`. To choose a specific JDK, set `JPACKAGE_JDK` to its folder before running the script.

Other launchers:
- `run-app.bat` — runs the jar with the Java on `PATH` (builds it first if needed).
- `run-app-silent.vbs` — same, without a console window.

> The packaged app and the bundled JavaFX native libraries target Windows. Building on macOS or Linux requires adjusting the JavaFX dependencies and packaging options.

---

## Project structure

```
src/main/java/com/gesturedrawing/
├── Launcher.java              Entry point for the packaged jar
├── MainApp.java               Navigation between screens, window size and position
├── domain/
│   ├── GestureImage.java      Image file reference
│   ├── SessionPhase.java      One phase: pose count × duration (with limits and formatting)
│   ├── SessionConfig.java     Image source, phase plan and display defaults
│   └── SessionResult.java     Summary of a finished session
├── service/
│   ├── ImageLoaderService.java     Finds supported images, shuffles and recycles poses
│   ├── ImagePreloaderService.java  Previous/current/next image cache and size limits
│   ├── PreferencesService.java     Saved settings
│   └── SessionManager.java         Session timing, navigation and progress
└── ui/
    ├── MainScreen.java        Session setup
    ├── PhaseEditor.java       Phase plan editor
    ├── SessionScreen.java     Drawing session view
    └── SummaryScreen.java     End-of-session summary

src/main/resources/styles.css  Dark theme
src/test/java/                 JUnit 5 tests
packaging/app-icon.ico         Windows app icon
```

---

## Roadmap

- [x] **v1** — Timed sessions, shuffling, image cache, saved settings, summary gallery
- [x] **v3** — Phased sessions (warm-up sequences)
- [ ] **v2** — Practice history (dates, durations and pose counts saved locally)
- [ ] **v4** — Practice statistics and charts
- [ ] **v5** — Goals and habit tracking

---

## License

[MIT](LICENSE)
