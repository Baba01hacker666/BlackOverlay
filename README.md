# Black Overlay

A minimal personal-use Android app that draws a full-screen black overlay (screen dimmer) on top of everything, with adjustable opacity.

## Features

- Full-screen black overlay via `TYPE_APPLICATION_OVERLAY`
- Opacity slider 0–100% + presets (25/50/75/90)
- Start / Stop with persistent notification (shows current opacity)
- Touch pass-through by default, optional "block touches" mode
- Restart after reboot (optional)
- Quick Settings tile for one-tap toggle
- Live updates: changing opacity while running applies instantly

## Requirements

- Android 8.0+ (minSdk 26)
- Grant **Display over other apps** when prompted
- On Android 13+, allow notifications so the foreground service stays alive

## Build locally

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release (unsigned unless you provide a keystore):

```bash
./gradlew assembleRelease
```

## CI — GitHub Actions

Workflow: `.github/workflows/android-apk.yml`

- On every push to `main`/`master`, PR, or manual dispatch: builds **debug APK** and uploads it as artifact `blackoverlay-debug`.
- On `main` pushes / manual / release: builds **release APK** (`blackoverlay-release`). On `release: published` it attaches the APK to the GitHub Release.

### Signed release setup (optional)

Add these repo secrets (`Settings → Secrets → Actions`):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 your-release.keystore` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

Without secrets, CI still builds an unsigned release APK (fine for personal sideloading of debug builds; for Play Store you need signing).

## Project structure

```
settings.gradle / build.gradle / gradle.properties
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/blackoverlay/app/
  MainActivity.kt        – UI: slider, presets, toggles
  OverlayService.kt      – foreground service + WindowManager black view
  Prefs.kt               – SharedPreferences helper
  BootReceiver.kt        – restart after reboot
  OverlayTileService.kt  – Quick Settings tile
app/src/main/res/...
.github/workflows/android-apk.yml
```

## Permissions

- `SYSTEM_ALERT_WINDOW` — draw the overlay
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — keep overlay alive
- `POST_NOTIFICATIONS` — persistent controls (Android 13+)
- `RECEIVE_BOOT_COMPLETED` — optional auto-restart
