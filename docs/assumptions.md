# Documented Assumptions

<!-- TODO: Document technical, product, and evaluation assumptions for FieldCapture. -->

1. **Mock Device Kit Only:** Physical Meta Ray-Ban glasses are not required; camera frames are injected via Mock Device Kit.
2. **Phone Mic Voice Fallback:** Mock Device Kit does not stream microphone audio from glasses, so voice commands use Android `SpeechRecognizer` on the phone.
3. **Offline-First Capture Queue:** Captures are saved locally in Room first and synced via WorkManager when connectivity is available.
