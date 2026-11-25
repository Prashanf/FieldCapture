# Documented Assumptions

<!-- TODO: Document technical, product, and evaluation assumptions for FieldCapture. -->

1. **Mock Device Kit Only:** Physical Meta Ray-Ban glasses are not required; camera frames are injected via Mock Device Kit.
2. **Phone Mic Voice Fallback:** Mock Device Kit does not stream microphone audio from glasses, so voice commands use Android `SpeechRecognizer` on the phone.
3. **Offline-First Capture Queue:** Captures are saved locally in Room first and synced via WorkManager when connectivity is available.
4. **Minimum SDK 29:** `com.meta.wearable:mwdat-camera:1.0.1` declares `minSdkVersion 29`, so `minSdk` is set to `29` (Android 10+).
5. **Kotlin 2.2.20 / Hilt 2.57.2 / Room 2.7.2:** `mwdat-*:1.0.1` and its transitive `kotlinx-coroutines:1.11.0` are compiled with Kotlin 2.2 metadata, requiring Kotlin `2.2.20`, KSP `2.2.20-2.0.4`, Hilt `2.57.2`, and Room `2.7.2` for metadata and KSP2 compatibility.
