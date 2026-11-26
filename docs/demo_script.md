# FieldCapture Demo Video Script (5 Minutes Max)

> **Recorded Demo Video:** [Watch on Google Drive](https://drive.google.com/file/d/13XdZDxWriiBYlHyolZaSANX7PsDozu6r/view?usp=sharing)

## Pre-Recording Checklist
- Android phone (`API 29+`) with latest **FieldCapture** debug APK installed (`GEMINI_API_KEY_1..3` compiled from `local.properties`).
- Test props within walking distance:
  1. An appliance or utility label with readable model/serial numbers (water heater, furnace, fridge, dishwasher, or printed nameplate).
  2. A wall, baseboard, ceiling corner, or floor area to simulate water/mold damage.
  3. An area near plumbing/valves/flooring for a spoken field note.
- Screen recorder enabled on phone (`1080p` landscape or portrait) + clear room audio so voice commands and `AudioFeedbackManager` TTS cues are audible.

---

## Walkthrough Script

### `[0:00 – 0:30]` Introduction & Job Setup
- **Action on screen:** Launch **FieldCapture** (`SessionScreen`). Point briefly at the top `ConnectionStatusBar` (`Glasses`, `Voice`, `Online`) and tap **Start Job** → enter Technician Name / optional Address → tap **Start**.
- **Narration:** *"This is FieldCapture—an offline-resilient Android app built on Meta's DAT SDK 1.0 and Google Gemini Vision. Using Mock Device Kit on this phone to simulate Ray-Ban Meta AI glasses, I'll walk through a hands-free property damage inspection and generate a structured estimate JSON."*

### `[0:30 – 1:00]` Start Glasses Camera Stream & Voice Loop
- **Action on screen:** Tap **Start Session** (watch glasses state transition through `Connecting` → `Connected` and live camera preview appear at `504×896` 15 fps). Tap **Start Listening** (grant audio/camera permissions if not pre-granted).
- **Narration:** *"Mock Device Kit is now streaming my phone's rear camera as the simulated glasses feed, while `VoiceCommandService` runs a continuous `SpeechRecognizer` loop so I can capture evidence completely hands-free."*

### `[1:00 – 2:00]` Capture Equipment Nameplate
- **Action on screen:** Walk up to an appliance label and hold steady (~1.5–2 ft away).
- **Speak clearly:** **`"capture nameplate"`**
- **Show on screen:**
  - `VoiceCommandIndicator` displays recognized `CaptureNameplate` command.
  - On-device quality gate runs (`BLUR_THRESHOLD_NAMEPLATE = 100.0`, luminance `30–240`, edge density `>= 2.0`).
  - TTS audio cue confirms capture; `CaptureCard` updates with extracted **equipment type, manufacturer, model number, serial number, capacity**, and per-field confidence scores.
- **Narration:** *"FieldCapture grabbed the frame, passed the stricter nameplate blur gate, and extracted structured model and serial fields via Gemini with per-field confidence."*

### `[2:00 – 3:00]` Capture Damage & Auto-Mapped Line Items
- **Action on screen:** Point phone camera at a lower wall / baseboard.
- **Speak clearly:** **`"capture damage kitchen north wall"`**
- **Show on screen:**
  - Parser extracts `room = "kitchen"` and `surface = "north wall"`.
  - Gemini identifies `damage_type`, `severity`, `affected_materials`, and `MapToLineItemsUseCase` maps restoration codes (`REMOVE_REPLACE_DRYWALL`, `REMOVE_REPLACE_BASEBOARD`, `PAINT_PRIME_WALLS`, etc.).
- **Narration:** *"Speaking the room and surface gives Gemini spatial context in the prompt, and `MapToLineItemsUseCase` immediately maps the damage assessment to standardized Xactimate-style line item codes."*

### `[3:00 – 3:30]` Dictate a Field Note
- **Action on screen:** Point at a pipe connection, valve, or flooring seam.
- **Speak clearly:** **`"note subfloor feels soft near the dishwasher"`**
- **Show on screen:**
  - `CaptureCard` logs the frame context plus cleaned `transcribed_note`, `category`, and `key_facts`.
- **Narration:** *"Field notes pair a reference frame with categorized homeowner statements, moisture observations, or safety concerns."*

### `[3:30 – 4:15]` Failure Detection & Recovery Demo *(Must Include)*
Choose **Option A** (fastest) or **Option B**:
- **Option A — On-Device Quality Gate Rejection & Recovery:**
  1. Partially cover the camera lens with a finger (or point into a dark cabinet) and say **`"capture nameplate"`**.
  2. Show `ImageQualityAnalyzer` failing brightness/blur check and `AudioFeedbackManager` speaking the retake prompt (`"too dark"` / `"too blurry"`), preventing wasted API calls.
  3. Uncover lens / improve lighting and say **`"capture nameplate"`** again → capture passes quality check and extracts cleanly.
- **Option B — Offline Queue & WorkManager Sync:**
  1. Enable **Airplane Mode** (`OfflineIndicator` shows offline state) and say **`"capture damage bathroom east wall"`**.
  2. Show capture saved locally in Room (`PENDING`, queued count increments).
  3. Disable Airplane Mode → `ExtractionQueueWorker` triggers and completes queued extraction automatically.
- **Narration:** *"Instead of uploading blurry or dark frames—or dropping data in dead-zone basements—the app catches bad captures on-device in milliseconds and queues pending extractions locally until connectivity returns."*

### `[4:15 – 4:45]` Complete Job, Review & Export JSON
- **Action on screen:** Tap **Stop Listen** → **Stop** → **Review (N)** (or **Complete Job**). On `ReviewScreen`, scroll `CaptureCard`s and tap **Generate Job Record** → inspect `JobRecordScreen` (**Job Record & Estimate**), then tap **Export JSON** and **Share JSON**.
- **Narration:** *"Every capture links via `source_capture_id` into a versioned `1.0.0` `JobRecord` JSON containing equipment, damage assessments, notes, and draft pricing line items—ready to save or share directly with back-office estimating tools."*

### `[4:45 – 5:00]` Wrap-Up
- **Narration:** *"That's FieldCapture: hands-free inspection built with Kotlin, Compose, Meta DAT SDK 1.0, and multi-key Gemini extraction. With two more weeks, next steps would be on-device Gemma 3 4B fallback for fully offline basements and VL53L5CX Time-of-Flight depth sensing for measured repair quantities. Thanks for watching."*

---

## Recording Tips
- Hold steady for ~0.5s after speaking `"capture nameplate"` so `capturePhoto()` latency doesn't blur small text.
- If a single Gemini key hits its `20` RPD limit during testing, `GeminiApiClient` automatically rotates across keys `1..3` with zero backoff delay.
- Trim dead silence during API waits if needed; export video at `1080p`.
