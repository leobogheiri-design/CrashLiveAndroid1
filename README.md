# Crash Live Analyzer — multi-game prototype

## Floating Crash panel
The app includes a real Android overlay service for the Crash screen:
- Start the app and tap **نمایش پنل شناور روی بازی**.
- On the first run, allow **Display over other apps** for Crash Live Analyzer, return to the app, and tap the same button again.
- The floating panel appears above other apps. Drag it by its title bar; use **جمع/باز** to collapse or expand it, **شفافیت** to cycle opacity, and **بستن** to close it.
- Start **شروع بررسی زنده** separately if you want screen capture and OCR to read visible multiplier history. Android will ask for screen-capture permission.

The panel can display historical sample statistics and capture status. It does not know or predict the next random Crash multiplier and cannot access VegasBet's private game server or place bets.

## Current modules
- **Crash analyzer:** screen-capture and OCR prototype for multiplier-like text. OCR still needs validation against the target game's live history panel.
- **Mines / diamonds and bombs:** probability calculator for a randomly and uniformly placed bomb grid. It cannot reveal hidden bomb positions.
- **Blackjack:** basic hit/stand/double decision helper using the player's cards and dealer's up-card.
- **Chicken Road / chicken game:** risk calculator based on a user-entered per-step success probability.
- **Shell / ball / cup game:** manual tracker updated by recording each observed swap; it does not automatically track motion from video.

## Important limitations
This is a prototype and has not been fully validated on every device or VegasBet layout. Screen OCR can miss or misread multipliers. Server-side random outcomes cannot be inferred from visible prior results. Do not stake money based on estimates.

## Build
The GitHub Actions workflow builds a debug APK when a commit is pushed to `main`. Open the latest run under Actions, wait until it completes successfully, then download the `crash-live-debug-apk` artifact and unzip it to get `app-debug.apk`.

Minimum Android version is API 27 (Android 8.1). Screen capture and overlay require explicit Android permissions.
