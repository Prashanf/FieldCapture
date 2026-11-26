# Research Memo: Meta AI Glasses & DAT SDK for Field Property Inspection

## What Today's Meta AI Glasses Can Do
- **Camera:** Meta Ray-Ban glasses provide a 12 MP ultra-wide (~120° FOV) camera well suited for room-level damage framing, showing multiple surfaces (wall, baseboard, flooring) in one shot without backing into tight corners. However, wide-angle lens distortion shrinks small equipment serial numbers unless the technician holds their head within ~2 feet of the data plate.
- **Voice & Audio:** Built-in microphones handle wake-word and voice triggers well in quiet spaces, but active restoration job sites run loud air movers, dehumidifiers, and wet-vacs; our phone-mic `SpeechRecognizer` fallback works in mock mode while underscoring that noisy-environment beam-forming is critical on hardware.
- **Connectivity & Display:** Glasses tether to the companion phone over Bluetooth with no standalone cellular radio, making our local Room + WorkManager offline queue mandatory in basements and crawl spaces. Head-worn indicators work for short capture/error feedback ("captured", "too dark"), not reviewing multi-line extraction JSON or estimates.

## What Today's Glasses Cannot Do for Field Estimation
1. **No Depth / Spatial Measurement:** Without depth sensing or LiDAR, all damage dimensions (e.g., `"~12 sq ft"`) are VLM visual estimates rather than billable adjuster quantities.
2. **No Thermal or Moisture Sensing:** RGB imagery captures discoloration and buckled paint but cannot separate active moisture migration from dried old stains—technicians still need a handheld moisture meter.
3. **Continuous Walk-Through Battery Limits:** Multi-room inspections running 30–60 minutes strain continuous wearable camera streaming alongside Bluetooth transfer.

## Original Findings Building on Meta DAT SDK `1.0.1`
1. **Kotlin Metadata & `minSdk 29` Coupling:** `com.meta.wearable:mwdat-*:1.0.1` ships with Kotlin `2.2` metadata (`kotlinx-coroutines 1.11.0` transitive) and requires `minSdk = 29`, necessitating Kotlin `2.2.20`, KSP `2.2.20-2.0.4`, Hilt `2.57.2`, and Room `2.7.2` alignment. Native initialization (`Wearables.initialize`) also throws `UnsatisfiedLinkError` under JVM unit tests unless isolated via Robolectric test `Application`.
2. **Stream Presets & Frame Quality:** `GlassesSessionManager` configures `StreamConfiguration(videoQuality = VideoQuality.MEDIUM, frameRate = 15, compressVideo = false)` (~504×896 frames) and decodes incoming `VideoFrame` buffers (including `YuvImage` NV21 conversion). Because `MEDIUM` frames are softer than 48 MP phone stills, `QualityThresholds` had to be calibrated lower (`BLUR_THRESHOLD_GENERAL = 80.0`, `BLUR_THRESHOLD_NAMEPLATE = 100.0`, `BRIGHTNESS_MIN = 30`).
3. **Terminal `STOPPED` Lifecycle & Reconnect:** Once `DeviceSessionState` hits terminal `STOPPED`, calling `start()` on the old instance fails—`GlassesSessionManager` must tear down session resources, poll `Wearables.devices`, and create a fresh session (`MAX_RECONNECT_ATTEMPTS = 3`).
4. **No Mock Audio Stream & `capturePhoto()` Movement Delay:** `MockDeviceKit` (`GlassesModel.RAYBAN_META`) injects `CameraFacing.BACK` or video/image URIs (`MockDeviceManager`) but streams zero audio, forcing a phone `SpeechRecognizer` + `AudioFeedbackManager` (`TextToSpeech` + tone cues) workaround. Still capture via `stream.capturePhoto()` (`PhotoData.HEIC` / `PhotoData.Bitmap`) also exhibits perceptible shutter-to-bitmap latency during movement, making spoken `"captured"` confirmation essential so techs hold steady on nameplates.

## Model Selection & Deployment Architecture
- **Cloud Choice (`gemini-3.5-flash` → `gemini-3.8-flash`):** Chosen for multimodal JSON adherence, schema field confidence scoring, and free-tier prototyping (`1,048,576` input / `65,536` output token limits). On real EPA images, `damage_type` reached `87.5%` (`7/8` completed), though free-tier `20` RPD limits and `503` spikes required implementing 3-key zero-backoff `429` rotation in `GeminiApiClient`.
- **Hybrid Future Fallback (`Gemma 3 4B INT4` vs `Qwen2.5-VL 3B`):** For network-dead basements, routing simple/offline captures to quantized **Google Gemma 3 4B (INT4, ~2.5 GB via MediaPipe LLM Inference)** provides ~8–15s on-device extraction with acceptable accuracy loss (~15–20% below cloud Gemini), while `Qwen2.5-VL 3B` (~1.8 GB, ~5–10s) sacrifices nameplate OCR fidelity. Hybrid routing tries rotated cloud Gemini first, falls back to local VLM when disconnected/exhausted, and queues via `ExtractionQueueWorker` otherwise.

## First Hardware Change: Multi-Zone Time-of-Flight Depth Sensor (`STMicroelectronics VL53L5CX`)
- **Why First:** Insurance adjusters require measured square footage (`"Remove & replace drywall: 48 sq ft"`), not qualitative severity labels (`25%` exact VLM match on real EPA images).
- **Target Component & Verified Specs:** **STMicroelectronics VL53L5CX** (`6.4 × 3.0 × 1.5 mm` reflowable module; **$8 \times 8$ (64-zone)** or $4 \times 4$ ranging up to **400 cm / 4 m**; **$45^\circ \times 45^\circ$ / $63^\circ$ diagonal FoV**; up to **15 Hz** in 8×8 mode; **$8\text{–}11\text{ mW}$** in autonomous 8×8 @ 1 Hz on-demand mode, or ~`216–313 mW` continuous).
- **What It Enables & Tradeoff:** Pairing 64-zone range matrices with camera FOV intrinsics yields instant wall distance, damage bounding area, and real quantities for `MapToLineItemsUseCase`—turning draft placeholders into billable estimates at the cost of ~2 weeks of optical-ToF extrinsics calibration and slight frame bezel integration.

## Production Recommendations
1. **Confidence-Gated Review:** Route extractions `< 0.70` overall/serial confidence to adjuster verification while auto-approving high-confidence captures.
2. **Progressive Voice Guidance:** Prompt techs dynamically on low-clarity or narrow-angle shots (`"Move closer to nameplate"` / `"Step back for full wall"`).
3. **Combined Quality + Multimodal Transcript:** Combine blur × luminance scoring for dark/shaky shots and retain raw verbal transcripts alongside extracted JSON.
