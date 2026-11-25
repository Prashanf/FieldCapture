# FieldCapture

Hands-free field capture Android application built with Kotlin, Jetpack Compose, Meta DAT SDK (Mock Device Kit), and Google Gemini Vision API.

## Tech Stack
- **Android / Kotlin:** Kotlin `2.2.20`, AGP `8.7.0`, `compileSdk = 35`, `minSdk = 29`, `targetSdk = 35`
- **UI:** Jetpack Compose (BOM `2024.12.01`)
- **Architecture:** MVVM + Clean Architecture (`data`, `domain`, `presentation`)
- **DI:** Hilt `2.57.2`
- **Local Storage:** Room `2.7.2`
- **Background Sync:** WorkManager `2.10.0`
- **Wearables:** Meta Device Access Toolkit (`mwdat-core`, `mwdat-camera`, `mwdat-mockdevice` `1.0.1` via Maven Central)

## Hands-Free Voice Architecture (Phone Mic Fallback)
- **Why phone mic?** Meta's **Mock Device Kit** simulates the glasses camera feed (`CameraFacing.BACK` or test video URI), but does **not** stream microphone audio from the simulated glasses.
- To support hands-free operation in mock mode, `VoiceCommandService` falls back to the phone's built-in microphone using Android's native `SpeechRecognizer` in a continuous auto-restart loop, paired with `AudioFeedbackManager` (`TextToSpeech` + `ToneGenerator`).
- On real Meta AI glasses hardware, voice audio would come from the DAT SDK microphone capability (`MWDATMedia`).

## Setup (< 10 min)
<!-- TODO: Expand with final architecture diagram, tools used, known limitations, and future work -->
1. Open `FieldCapture/` in Android Studio.
2. Meta DAT SDK `1.0.1` resolves from Maven Central (`metaDatVersion=1.0.1` in `gradle.properties`); Developer Mode manifest metadata (`APPLICATION_ID="0"`, `CLIENT_TOKEN="AR|0|1"`) is pre-configured.
3. Add your free Gemini API key (from [Google AI Studio](https://aistudio.google.com/apikey)) to `FieldCapture/local.properties` (gitignored):
   ```properties
   GEMINI_API_KEY=your_key_here
   ```
   `app/build.gradle.kts` exposes this via `BuildConfig.GEMINI_API_KEY` for `gemini-2.0-flash` vision extraction.
4. Sync Gradle and run on an Android emulator or device (Mock Device Kit mode enabled).

## Project Layout
- `app/` — Android application source, unit tests, and instrumentation tests
- `eval/` — Offline extraction evaluation script, frames directory, results CSV, report, and sample job records
- `docs/` — Research memo, architecture diagram, and assumptions
