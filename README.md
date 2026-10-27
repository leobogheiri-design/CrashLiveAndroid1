# Crash Live Analyzer — multi-game prototype

## Current modules
- **Crash analyzer:** screen-capture and OCR prototype for multiplier-like text. The OCR has not yet been validated against a live game screen.
- **Mines / diamonds and bombs:** probability calculator for a randomly and uniformly placed bomb grid. It cannot reveal the hidden bomb positions.
- **Blackjack:** basic hit/stand/double decision helper using the player's cards and the dealer's up-card. This is a simplified guide, not a complete casino-rule strategy engine.
- **Chicken Road / chicken game:** risk calculator based on a user-entered per-step success probability. It is only meaningful when that probability is known and applicable.
- **Shell / ball / cup game:** manual tracker that updates the estimated cup containing the ball when the user records each observed swap. It does not track motion automatically from video.

## Important limitations
This is a prototype and has not been validated on-device. It does not predict hidden random outcomes with certainty, access a casino's server, or place bets. For games that use server-side randomness, visible prior outcomes do not reveal the next hidden result. Do not stake money based on these estimates.

The Crash OCR currently reads the whole screen and has known risks of stale/duplicate multipliers and missed values. It needs testing and tuning against the target game's actual history panel before relying on its data.

## Build
The GitHub Actions workflow builds a debug APK when a commit is pushed to `main`. Open the latest run under Actions, wait until it completes successfully, then download the `crash-live-debug-apk` artifact and unzip it to get `app-debug.apk`.

Minimum Android version is API 27 (Android 8.1). Screen capture requires explicit Android permission and should only be used on a device and content you are authorized to capture.
