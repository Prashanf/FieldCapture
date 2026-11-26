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
   `app/build.gradle.kts` exposes this via `BuildConfig.GEMINI_API_KEY` for `gemini-3.5-flash` (with `gemini-3.8-flash` fallback) vision extraction.
4. Sync Gradle and run on an Android emulator or device (Mock Device Kit mode enabled).

## Job Record Schema & Draft Pricing
- **Schema Versioning:** Exported job records include `"schema_version": "1.0.0"` for forward/backward compatibility, capturing `equipment`, `damage_assessments` (with on-device `quality_scores`), `notes`, `line_items` (each linked back via `source_capture_id`), `summary`, and `metadata`.
- **Draft Estimate Pricing Disclaimer:** Unit prices in `LineItemCode` (all 15 restoration codes) are **placeholder draft values** based on general industry ranges to demonstrate end-to-end estimate generation. Production pricing would integrate with Xactimate or a contractor pricing database.
- **Sample Deliverables:** 3 sample job records covering water damage, mold remediation, and multi-room water migration are included in `samples/` and `eval/sample_outputs/`.

## Evaluation (40-Frame Benchmark)
- **Ground Truth Dataset:** `eval/ground_truth.json` defines 40 annotated captures (`15` nameplate, `15` damage, `10` voice notes) with per-field ground truth and difficulty labels (`easy`, `medium`, `hard`).
- **Evaluation Framework:** `com.fieldcapture.eval.EvalRunner` computes per-field exact/fuzzy accuracy (case/whitespace normalization, `?` wildcards, 1-char OCR tolerance), line-item precision/recall/F1, confidence calibration buckets, accuracy by difficulty, and the top 5 worst failures.
- **Automated Pipeline Verification:** `com.fieldcapture.eval.ProgrammaticTestImageGenerator` renders synthetic JPEG frames using Android `Canvas`/`Paint` so `EvalRunnerTest` verifies the extraction and scoring pipeline end-to-end offline.
- **Full Evaluation with Real Images:** To run the full evaluation, place test images in `eval/test_set/` matching the filenames in `eval/ground_truth.json` and run the evaluation script/runner (`EvalRunner` via unit/instrumented test) against `ExtractionService`.
- **Report:** See [`eval/eval_report.md`](eval/eval_report.md) for field-level accuracy tables, confidence calibration analysis, quality threshold tuning, and the 5 worst failures with root-cause mitigations.

## Project Layout
- `app/` — Android application source (`com.fieldcapture.eval`, extraction, quality, voice, glasses, sync), unit tests, and instrumentation tests
- `samples/` — 3 sample exported `JobRecord` JSON deliverables
- `eval/` — 40-frame `ground_truth.json`, `test_set/` directory structure, `eval_report.md`, `eval_results.csv`, `eval_runner.py`, and `sample_outputs/`
- `docs/` — Research memo, architecture diagram, and assumptions
