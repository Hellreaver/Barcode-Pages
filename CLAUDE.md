# Barcode-Pages

Android app (Kotlin, Jetpack Compose) that redraws store tote labels as Code 128 barcodes after a failed dispense. See README.md for how it works.

- Push straight to `main`; the owner does not use pull requests. Each push runs `.github/workflows/release.yml`, which tests, builds and publishes release `v1.0.<run number>`.
- Each release's notes are the subject lines of the commits since the previous release. Write commit subjects for the person installing the app ("Fix Samsung keyboard dropping the first typed letter"), not for developers, and keep one user-visible change per commit.
- Name every release APK `Barcode-Pages-<version>.apk` (for example `Barcode-Pages-1.0.5.apk`) so downloads are identifiable on the phone. Keep the version in the file name in any new build or release step.
- The signing key lives only in the `SIGNING_KEYSTORE` and `SIGNING_PASSWORD` Actions secrets. Never commit a keystore or password; the repository may go public.
- Tote labels are shown and encoded in capitals (Code 128 keeps case, so the barcode has to match the printed label). The text field keeps exactly what the keyboard typed and capitalizes only on screen (`UppercaseTransformation`) and in `Tote.label`. Never rewrite the field value while typing, and never change a field's `KeyboardOptions` while it has focus: either one restarts the keyboard connection, and Samsung's keyboard (Galaxy Note9) then drops the first letter.
- Target phones: Pixel 8a and Pixel 8 Pro. Screenshot tests render at 411 x 914 dp and 448 x 997 dp.
- Colors and type follow the Lemon-Checklists house style (`Theme.kt`).
- Run `./gradlew testDebugUnitTest` before pushing. Add `-Proborazzi.test.record=true` to write screen PNGs to `app/build/screenshots/`, and decode the barcodes in them to confirm they scan.
