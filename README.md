# Crash Live Analyzer — prototype

## What it does
- Android app prototype (minimum Android 8.1 / API 27).
- Requests the system screen-capture permission.
- Uses on-device ML Kit text recognition to look for multiplier-like text on screen.
- Saves recognized values locally.

## Important limitations
- This is an untested prototype source project, not a ready-to-install APK.
- OCR reads the whole screen, so it may capture duplicate/stale multipliers, amounts, or other text. It needs testing and tuning against the actual game screen.
- The current deduplication is only session-level and value-based; repeated values in different rounds can be incorrectly skipped. It is not yet robust enough for reliable live round-by-round logging.
- It does not predict the next crash multiplier or interact with betting controls.
- Android may stop capture when permission is revoked or the system kills the process.
- Screen capture is sensitive: only use it on your own device and stop it when finished.

## Build via GitHub Actions using a phone
1. Create a new GitHub repository from the GitHub mobile website/app.
2. Upload all files and folders from this project to the repository (keep the `.github/workflows/build.yml` path).
3. Open the repository's **Actions** tab and run **Build Android APK** (or push to `main`).
4. When it finishes, open the workflow run, download the `crash-live-debug-apk` artifact, unzip it, and install `app-debug.apk`.
5. Android may ask you to allow installation from that browser/file manager. Only install APKs you built and trust.

## Next development step
Crop OCR to the HISTORY area and detect changes in the ordered grid. This is required before relying on collected data.
