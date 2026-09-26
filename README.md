# Barcode-Pages

Tote Barcodes, an Android app that redraws tote barcodes when a dispense or scan-out fails after the customer has left. Type the tote labels in, tap Next, and scan the barcodes straight off the phone screen.

**Download: [latest release](https://github.com/Hellreaver/Barcode-Pages/releases/latest)**, then tap `ToteBarcodes.apk` under Assets.

## Using it

1. **Enter missing totes.** Type a label (`e3397`, `2965`, `e22560`) exactly as printed. A new box opens as soon as you type in the last one. The keyboard's Next key jumps to the next box. Pasting a list separated by spaces, commas or line breaks fills one box per label. A label entered twice gets a yellow `Listed twice` tag.
2. **Next.** Works from the first tote on. Each tote gets a card with the label on top and a full-width Code 128 barcode under it. While this screen is open the phone stays awake at full brightness, and the brightness goes back to normal when you leave it.
3. **Mark scanned.** Optional. Tap it after the scanner takes a tote, and the header counts how many are done. Back or `‹ Totes` returns to the list to fix a label.
4. **Share.** Shows a QR code and a Send link button for the releases page.

Nothing is saved between launches. Swiping the app away or reopening it gives a blank list. If Android closes the app in the background while you're in another app, it comes back with the same list.

Code 128 keeps upper and lower case, so `e3397` scans as `e3397`, not `E3397`. Each bar is drawn on whole screen pixels with a 10-module blank margin on both sides, the minimum the Code 128 spec asks for.

## Installing

Built for the Pixel 8a and Pixel 8 Pro (Android 14 and later). It runs on any phone with Android 10 or newer.

1. On the phone, open the latest release and download `ToteBarcodes.apk`.
2. Open the download. Android asks you to allow installs from the browser or Files app the first time.
3. Play Protect warns about apps that don't come from the Play Store. Tap Install anyway.

A work phone under company management may block installs from outside the Play Store.

## Builds

Every push to `main` runs `.github/workflows/release.yml`. It runs the tests, builds a signed release APK and publishes it as release `v1.0.<run number>`. Each build is signed with `signing/tote-barcodes.jks`, so a new version installs over the old one. That key is committed on purpose. Move it into a GitHub Actions secret before making this repository public.

Local build: `./gradlew assembleRelease` (needs JDK 17+ and the Android SDK).

Tests: `./gradlew testDebugUnitTest`. They decode rendered barcodes with ZXing, check the tote list behavior, and render every screen at Pixel 8a (411 x 914 dp) and Pixel 8 Pro (448 x 997 dp) sizes. Add `-Proborazzi.test.record=true` to save those screens as PNGs under `app/build/screenshots/`.

## Source

Kotlin with Jetpack Compose, one activity, under `app/src/main/java/com/hellreaver/totebarcodes/`:

- `EntryScreen.kt`: the tote list.
- `BarcodeScreen.kt`: barcode cards and the brightness hold.
- `ShareScreen.kt`: QR code and link.
- `ToteStore.kt`: list state and the auto-added blank row.
- `Barcode.kt`: Code 128 encoding (ZXing) and pixel layout.
- `Theme.kt`, `Components.kt`: colors and parts from the Lemon-Checklists house style.

Fonts: Barlow, Barlow Condensed and IBM Plex Mono, SIL Open Font License 1.1 (`third_party/`).
