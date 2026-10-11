# D-Tox

A tiny, calm Android app that does exactly one job:

> **Before a distracting app opens, you breathe — 30 seconds — first.**

Built to be the opposite of the usual "detox" apps: no bloat, no ads, no
account, no subscription, and **no special permissions at all**.

## Why it stays lightweight

- **Zero permissions.** Open `app/src/main/AndroidManifest.xml` — there is not
  a single `<uses-permission>` line. No accessibility service, no usage access,
  no overlay, no internet.
- **No background services.** Nothing runs when D-Tox is closed, so it can
  never touch your calls, notifications, or battery.
- **No accounts, ads, analytics, or subscriptions.**
- **Release APK ≈ 1.2 MB** (full Compose app, minified by R8 with resource
  shrinking). The only permission in the built APK is
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — a signature-level, app-internal
  permission that AndroidX generates automatically; users are never prompted
  for anything.

## How the gate works

Android doesn't let a normal app know when you launch another app — that's why
every other blocker demands the Accessibility permission. D-Tox flips the
model: **D-Tox becomes your entry point.**

1. In D-Tox, tap **Choose apps** and tick the apps you want to slow down.
2. Tap **Pin** on each one and accept the launcher's "Add to home screen" dialog.
   You get a shortcut with the app's icon and a small green ring.
3. Remove the original app icon from your home screen (touch and hold → Remove).
4. From now on you tap the D-Tox shortcut. You get a 30-second guided breath
   (inhale / hold / exhale), then "Open <app>" unlocks.

That's the whole trick: zero permissions, zero battery cost. The honest
trade-off is that it works only when you enter through D-Tox — pulling the app
open from the app drawer or recent apps bypasses it. For most people, replacing
the home-screen icon is exactly the speed bump that's needed.

### Phone calls

D-Tox has no background components, so it cannot interfere with incoming or
outgoing calls in any way. If a call arrives while you're breathing, the timer
pauses itself and resumes when you're back.

## Build

Requirements: Android Studio (recent) or just a terminal + JDK 17–21.

```
gradlew.bat assembleDebug      # debug build
gradlew.bat assembleRelease    # minified release build
```

A `gradle.properties` line points the build at Android Studio's bundled JDK
(21), because the system JDK (26) is too new for the Android Gradle plugin.
If your Android Studio lives elsewhere, adjust `org.gradle.java.home` or set
`JAVA_HOME` to a JDK 17–21.

Output: `app/build/outputs/apk/`

- `minSdk 26` (Android 8) · `compileSdk 37` · `targetSdk 37`
- Kotlin 2.4.21 · AGP 9.4.1 (built-in Kotlin — no `kotlin-android` plugin) ·
  Gradle 9.8.1 · Compose BOM 2026.09

> The release APK is unsigned. To install it on a phone, either flash the
> debug APK (`assembleDebug`) or add a `signingConfig` to the `release` build
> type.

## Project map

```
app/src/main/java/com/dtox/app/
├── MainActivity.kt          single activity; receives gate requests
├── data/
│   ├── Apps.kt              lists launcher apps (permission-free)
│   ├── BreathPattern.kt     box-breathing phases, all in plain arithmetic
│   └── Prefs.kt             SharedPreferences: gated apps, settings, stats
├── ui/
│   ├── DToxRoot.kt          tiny state-based navigation
│   ├── HomeScreen.kt        gated apps + stats
│   ├── PickerScreen.kt      searchable app picker
│   ├── SettingsScreen.kt    breath length, pattern, haptics
│   ├── BreathScreen.kt      the 30-second gate (call-safe timer)
│   └── DToxIcons.kt         four hand-rolled icons, no extra dependency
└── util/
    ├── ContextExt.kt
    └── Shortcuts.kt         pins the home-screen gate shortcuts
```

## Roadmap

- **v0.2 (opt-in): "Shield mode"** — usage access + overlay so launches from
  anywhere are caught. Adds real permissions, so it stays off by default and
  is call-safe (never draws over the dialer or in-call UI).
- Ideas: in-app blocking (Reels/Shorts feeds), home-screen widget, streaks,
  gentle sound cues, custom breathing patterns.
