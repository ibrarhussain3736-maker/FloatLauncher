# Float Launcher

Floating bubble that lists your installed apps and opens them in resizable floating windows.

## Build
Push to GitHub: the Actions workflow builds the APK (Actions tab, Artifacts).

## Use
1. Install the APK, open it, grant overlay permission.
2. Developer options: enable "Enable freeform windows" and "Force activities to be resizable", reboot.
3. Tap Start, then tap the bubble and pick an app. Hold the bubble 1.5s to stop.

## Limits
Android decides if an app may run in a window. Without freeform support, apps open fullscreen or split-screen.
