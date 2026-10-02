# Barcode-Pages

Barcode-Pages is an Android app that redraws tote barcodes when a dispense or scan-out fails after the customer has left. Type the tote labels in, tap Next, and scan the barcodes straight off the phone screen.

**Download: [latest release](https://github.com/Hellreaver/Barcode-Pages/releases/latest)**, then tap `Barcode-Pages-<version>.apk` under Assets.

## Using it

1. **Enter missing totes.** Type the code from the bottom-right corner of each tote label (`Z13334`, `Y98760`), zeros included, or the code just above a trip label's bars (`TL0a1b-2`). The Dispense Order list shows the same codes. That code is the only thing the label's barcode holds; the long number printed above the bars is not in it. Letters switch to capitals as you type, except a trip label's trip id, which stays lowercase the way the label encodes it (typing `TL0A1B-2` gives `TL0a1b-2`). Until the first code is entered, a drawing of a tote label under the list highlights where the code sits, with a trip label drawing one tap away; its bars are decoration and can't be scanned. A new box opens as soon as you type in the last one. The keyboard's Next key jumps to the next box. Pasting a list separated by spaces, commas or line breaks fills one box per label. A label entered twice gets a yellow `Listed twice` tag.
2. **Next.** Works from the first tote on. Each tote gets a card with the label on top and a full-width Code 128 barcode under it. While this screen is open the phone stays awake at full brightness, and the brightness goes back to normal when you leave it.
3. **Mark scanned.** Optional. Tap it after the scanner takes a tote, and the header counts how many are done. Back or `‹ Totes` returns to the list to fix a label.
4. **Share & update.** Shows a QR code and a Send link button for the releases page, plus the update check. On every launch the app asks GitHub for the newest release. If it's newer than the installed version, the `Share` button turns into a highlighted `Update` button, and `Update to 1.NN` downloads the APK in Chrome to install over the app. The check gets a 404 from GitHub while the repository is private; the card then offers the releases page instead.

Nothing is saved between launches. Swiping the app away or reopening it gives a blank list. If Android closes the app in the background while you're in another app, it comes back with the same list.

Code 128 keeps case, so `z13334` and `Z13334` are different barcodes. The app sets the case to match the printed label. Each bar is drawn on whole screen pixels with a 10-module blank margin on both sides, the minimum the Code 128 spec asks for.

## Installing

Built for the Pixel 8a and Pixel 8 Pro (Android 14 and later). It runs on any phone with Android 10 or newer.

1. On the phone, open the latest release and download `Barcode-Pages-<version>.apk`.
2. Open the download. Android asks you to allow installs from the browser or Files app the first time.
3. Play Protect warns about apps that don't come from the Play Store. Tap Install anyway.

A work phone under company management may block installs from outside the Play Store.

## Builds

Every push to `main` runs `.github/workflows/release.yml`. It runs the tests, builds a signed release APK and publishes it as the next release, with the file named after it (`Barcode-Pages-1.15.apk`). Versions have two digits after the dot and go up by one per release: 1.15, 1.16 … 1.19, 1.20 … 1.99, 2.00. The release notes come from `CHANGELOG.md`: changes are written under its "Unreleased" heading, and each release renames that heading to its version and commits the file back.

The signing key is not in this repository. The workflow reads it from two GitHub Actions secrets (Settings > Secrets and variables > Actions):

- `SIGNING_KEYSTORE`: the PKCS12 keystore, base64-encoded, key alias `barcode-pages`.
- `SIGNING_PASSWORD`: its password.

Without them the workflow still builds and tests but publishes no release. Every release must be signed with the same key or Android refuses to install it over the previous version, so keep an offline copy of the keystore.

Local build: `./gradlew assembleRelease` (needs JDK 17+ and the Android SDK). Without the key it signs with your machine's debug key; set `SIGNING_KEYSTORE_FILE` and `SIGNING_PASSWORD` to sign with the release key.

Tests: `./gradlew testDebugUnitTest`. They decode rendered barcodes with ZXing, check the tote list behavior, and render every screen at Pixel 8a (411 x 914 dp) and Pixel 8 Pro (448 x 997 dp) sizes. Add `-Proborazzi.test.record=true` to save those screens as PNGs under `app/build/screenshots/`.

## Source

Kotlin with Jetpack Compose, one activity, under `app/src/main/java/com/hellreaver/barcodepages/`:

- `EntryScreen.kt`: the tote list.
- `BarcodeScreen.kt`: barcode cards and the brightness hold.
- `ShareScreen.kt`: QR code and link.
- `ToteStore.kt`: list state and the auto-added blank row.
- `Barcode.kt`: Code 128 encoding (ZXing) and pixel layout.
- `Theme.kt`, `Components.kt`: colors and parts from the Lemon-Checklists house style.

Fonts: Barlow, Barlow Condensed and IBM Plex Mono, SIL Open Font License 1.1 (`third_party/`).
