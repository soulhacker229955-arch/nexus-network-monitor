# Nexus Network Monitor (v0.2.0)

Created by Asad. Native Android (Kotlin, Jetpack Compose, Material 3). Package: `com.asad.nexus.networkmonitor`.

## Status (honest)
- Source written, **NOT compiled or run yet**. No APK has been built or tested on a device.
- Build and test results must come from GitHub Actions or your own build.

## What it does
- Tabs: Home, Apps (choose which apps appear in the notification), Settings.
- Status bar shows the live speed number (Download / Upload / Both / Off).
- Notification settings: total on/off, apps on/off, compact, rows, download/upload visibility, sorting, refresh interval.
- Permission card hides itself once granted; English setup guide (incl. Allow restricted settings).

## Original phase 1 list
- Foreground service (you start/stop it; notification is always visible while running).
- Expanded notification: total download/upload plus top apps with icons and speeds. Compact mode available.
- Dashboard: total speed, network type, bytes since boot, app list, permission status, notification settings.

## How measurements work
- Total speed: `TrafficStats` total counters sampled every 1 s (real).
- Per-app speed: `NetworkStatsManager` per-UID bytes, queried every ~3 s, speed = byte difference / elapsed time.
  This is an **interval average** and can lag (Android updates these stats in batches). It is not true real-time per-app speed.
- True real-time per-app speed would need `VpnService`; not implemented.
- Per-app needs **Usage Access**. Without it, only total speed is shown and the app says so.
- Nothing is sent off the device. No INTERNET permission is declared.

## Permissions
ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE,
PACKAGE_USAGE_STATS (special access), QUERY_ALL_PACKAGES (to resolve names/icons of all apps in traffic stats).

## Build with GitHub Actions (recommended on a phone)
1. Create a GitHub repo, push this folder (branch `main`).
2. Actions tab > "Build APK" runs tests and `assembleDebug`.
3. Download artifact `nexus-network-monitor-debug` (app-debug.apk), install it.

Termux push example:
```
pkg install git unzip
unzip nexus-network-monitor.zip && cd nexus-network-monitor
git init -b main && git add . && git commit -m "phase 1"
git remote add origin https://github.com/USER/REPO.git
git push -u origin main
```
(Use a GitHub personal access token as the password.)

Local build needs JDK 17, Android SDK 35 and Gradle 8.9 (`gradle assembleDebug`). Not tested on Termux.

## Setup on the phone
1. Install APK, open app, tap Start monitoring, allow notifications.
2. Settings > Usage Access > Nexus Network Monitor > allow (for per-app rows).
3. Pull down the notification to expand it.

## Verify accuracy
Compare totals with Android Settings > Network > Data usage, and run a known download while watching total speed.

## Not done yet (later phases)
Room history, DataStore settings, graphs, app list screen with filters, privacy/about screens, start on boot, export, instrumented tests.
