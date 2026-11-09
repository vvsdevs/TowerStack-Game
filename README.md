<div align="center">

### Build Android apps with AndroAI

**[Register for the free webinar](https://androai.io/webinar/register)**

[Website](https://androai.io) · [Courses](https://androai.io/courses) · [Source Code Market](https://androai.io/market)

# Tower Stack

**One tap. One level higher.**

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square)
![Offline](https://img.shields.io/badge/Play-Offline-8065E8?style=flat-square)

</div>

An offline Android stacking game built with Kotlin and Jetpack Compose. Find your rhythm, line up each slab, and build your tallest tower.

## Features

- One-tap stacking with movement alternating between two world axes.
- Precise overlap cuts and animated falling pieces.
- Perfect-placement snapping, feedback, and celebration particles.
- Colorful isometric slabs over a navy-to-indigo gradient.
- Gradually increasing speed and smooth camera tracking.
- Home screen, pause, restart, game over, and Play Again.
- Saved best score, sound preferences, and vibration settings.
- Automatic background pause with explicit resume.
- No accounts, backend, ads, purchases, or network permissions.

## Download the source

Click **Code → Download ZIP** and extract the archive, or **[download the ZIP directly](https://github.com/vvsdevs/TowerStack-Game/archive/refs/heads/main.zip)**.

To clone with Git:

```bash
git clone https://github.com/vvsdevs/TowerStack-Game.git
cd TowerStack-Game
```

## Open and run

Open this folder in Android Studio, sync Gradle, select the `app` configuration, and run on an Android 7.0 (API 24) or newer device/emulator. The existing Gradle wrapper, Android plugin, SDK configuration, and package name are preserved. Initial dependency resolution requires internet; installed gameplay is offline.

Install the Android SDK requested by the project (compile SDK 37). The SDK location is configured through `local.properties`, which is not committed. The Gradle daemon configuration requests JDK 25; allow Android Studio/Gradle to provision it or configure a matching JDK.

From PowerShell with JAVA_HOME pointing to a working JDK:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

On macOS/Linux, use `./gradlew testDebugUnitTest assembleDebug`.

## Play

Tap Play, then tap the play area to place the moving slab. Movement alternates between the two world axes. Overhang falls away, leaving a smaller target. Precise placements snap into alignment. A complete miss ends the run. The pause menu offers Resume, Restart, and Home. Back pauses an active run and returns home from an overlay. Leaving the app pauses the run; returning requires Resume. Short input guards prevent transition taps from placing another block.

Sound and vibration can be switched on the home screen. Haptics use platform view feedback and respect device settings. Best score and preferences are stored with Preferences DataStore. Audio uses generated platform tones, released when the composition is disposed.

## Tech stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Rendering | Procedural Compose Canvas |
| Game logic | Pure Kotlin engine with world-space geometry |
| State | Android ViewModel |
| Persistence | Preferences DataStore |
| Timing | Lifecycle-aware frame timestamps and clamped delta time |
| Minimum / target SDK | 24 / 37 |
| Build | Gradle Kotlin DSL + version catalog |
| Application ID | `com.game.towerstack` |

## Project structure

```text
app/src/main/java/com/game/towerstack/
|-- GameEngine.kt       # Geometry, movement, scoring, and states
|-- GameViewModel.kt    # State coordination and persistence
|-- MainActivity.kt     # Screens, rendering, timing, and feedback
`-- ui/theme/           # Theme resources

app/src/test/java/com/game/towerstack/
|-- GameEngineTest.kt   # Geometry and engine tests
`-- ExampleUnitTest.kt  # Original template test
```

## Code and tuning

- `GameEngine.kt`: Android-independent states, movement, overlap/cut geometry, falling pieces, particles, camera, restart, and culling. Adjust the base speed (1.35), score increment (0.045), maximum speed (3.2), travel range (2.6), and `Geometry.PERFECT_FRACTION` (3.5% of active dimension) here. Delta time is capped at 50 ms.
- `GameViewModel.kt`: UI coordination and persistent best score/settings.
- `MainActivity.kt`: home/game/pause/game-over screens, lifecycle frame loop, feedback, and procedural isometric Canvas. Change `palette` / `blockColor` for slab colors and the root gradient for the background. Canvas observes frame changes in its draw phase to avoid recomposing the screen each frame. A reused Path draws slab faces without allocating per-face point arrays.
- `GameEngineTest.kt`: focused geometry, restart, input guard, pause, and delta-time tests.

## Validation performed

`testDebugUnitTest` and `assembleDebug` both PASSED on this workspace. Seven game test methods cover full overlap, partial overlap from both sides on both axes, edge contact, complete misses, proportional perfect tolerance, retained and removed sizes/centers, restart, pause/resume, double-placement guards, and delta clamping. The existing template unit test also passed.

No device was connected (`adb devices` was empty). On-device visual inspection, instrumentation tests, audio/haptic checks, and refresh-rate/lifecycle interaction checks have not been run.

Build outputs, local SDK settings, IDE files, and signing keys are excluded from version control.

---

<div align="center">

### Learn to build apps like this

Explore Android app development with **[AndroAI](https://androai.io)**.

**[Join the free webinar](https://androai.io/webinar/register)**

If you enjoy Tower Stack, give the repository a star!

</div>
