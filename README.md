# Linkon

Linkon is an Android app with a floating Crash analysis panel and additional probability/risk tools.

## Floating panel over VegasBet
- Open Linkon and tap **نمایش پنل شناور روی بازی**.
- Allow Android's **Display over other apps** permission, return to Linkon, and tap the button again.
- The panel is designed to remain above other apps, including VegasBet. Drag it by its title; use **جمع/باز** to collapse or expand, **شفافیت** to change opacity, and **بستن** to close it.
- Start **شروع بررسی زنده** separately to grant screen-capture permission and let OCR inspect visible multiplier history.

## Crash analysis
Linkon reads visible multiplier history using on-device OCR and calculates recency-weighted historical rates for thresholds such as 1.5x, 2x, and 5x. It also displays approximate uncertainty intervals and the median of recent observations. At least 30 valid observations are required before showing estimates.

**Important:** Crash outcomes are generally generated randomly and independently. Historical rates describe past observations; they do not reveal or reliably predict the next multiplier. Linkon does not connect to VegasBet's private server, guarantee a result, or place bets. OCR can miss or misread values, so check the capture status and treat the estimates as descriptive statistics, not betting signals.

## Other tools
- Mines: probability calculator for randomly placed bombs; it cannot reveal hidden positions.
- Blackjack: basic hit/stand/double decision helper.
- Chicken game: risk calculator based on a user-entered per-step success probability.
- Shell game: manual tracker; it does not automatically track video motion.

## Compatibility and build
Minimum Android version: API 27 (Android 8.1). Overlay and screen capture require explicit Android permissions. The app has not yet been validated on every VegasBet layout or on the target A5 2017 device.

GitHub Actions builds a debug APK on pushes to `main`. Download the latest successful `Linkon-APK` artifact from the repository's Actions page.
