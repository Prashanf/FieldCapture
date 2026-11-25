# FieldCapture

Hands-free field capture Android application built with Kotlin, Jetpack Compose, Meta DAT SDK (Mock Device Kit), and Google Gemini Vision API.

## Tech Stack
- **Android / Kotlin:** Kotlin `2.0.21`, AGP `8.7.0`, `compileSdk = 35`, `minSdk = 28`, `targetSdk = 35`
- **UI:** Jetpack Compose (BOM `2024.12.01`)
- **Architecture:** MVVM + Clean Architecture (`data`, `domain`, `presentation`)
- **DI:** Hilt `2.51.1`
- **Local Storage:** Room `2.6.1`
- **Background Sync:** WorkManager `2.10.0`
- **Wearables:** Meta Device Access Toolkit (`mwdat-*` 1.0.x via GitHub Maven)

## Setup (< 10 min)
<!-- TODO: Complete setup instructions, architecture diagram, tools used, known limitations, and future work -->
1. Open `FieldCapture/` in Android Studio.
2. Confirm `metaDatVersion` in `gradle.properties` (and GitHub Packages PAT with `read:packages` in `local.properties` or env vars `GITHUB_USER` / `GITHUB_TOKEN` if required).
3. Add your Gemini API key in `local.properties` (or `Constants.kt` for local dev).
4. Sync Gradle and run on an Android emulator or device (Mock Device Kit mode enabled).

## Project Layout
- `app/` — Android application source, unit tests, and instrumentation tests
- `eval/` — Offline extraction evaluation script, frames directory, results CSV, report, and sample job records
- `docs/` — Research memo, architecture diagram, and assumptions
