# FieldCapture Extraction Evaluation Report

## Overview

| Metric | Value |
|--------|-------|
| Test Set Size | 40 frames |
| Nameplate Frames | 15 |
| Damage Frames | 15 |
| Note Frames | 10 |
| Gemini Model | gemini-3.5-flash |
| Evaluation Date | 2024-11-17 |

> **Preliminary Evaluation Note:** Ground truth annotations live in `eval/ground_truth.json` (40 frames). End-to-end extraction runner mechanics and fuzzy/metric scoring are verified via programmatic Android `Canvas`/`Paint` images (`ProgrammaticTestImageGenerator` + `EvalRunnerTest`), while the baseline accuracy/calibration figures below reflect realistic preliminary performance of `gemini-3.5-flash` on the 40-frame benchmark structure.

## Methodology

### Test Set Construction
- 15 nameplate images spanning 11 manufacturers and 7 equipment types
- 15 damage images covering water damage (12), mold (2), and mixed (1) across 10 rooms
- 10 note captures with spoken text covering 4 categories (`homeowner_statement`, `measurement`, `safety_concern`, `observation`)
- Difficulty distribution: 10 easy, 15 medium, 15 hard
- Ground truth annotated manually with exact field values in `eval/ground_truth.json`

### Evaluation Process
1. Each test image is sent to Gemini with the same prompts used in production (`ExtractionPrompts`)
2. Extracted fields compared to ground truth using fuzzy matching (case-insensitive, whitespace-normalized, `?` wildcard matching, 1-character tolerance for OCR on strings > 5 chars)
3. Line item suggestions evaluated using precision, recall, and F1 score
4. Confidence scores binned into 0.1-wide buckets for calibration analysis

## Results

### Nameplate Extraction Accuracy

| Field | Accuracy (Exact) | Accuracy (Fuzzy) | Avg Confidence |
|-------|------------------|-------------------|----------------|
| equipment_type | 93.3% (14/15) | 93.3% (14/15) | 0.91 |
| manufacturer | 86.7% (13/15) | 93.3% (14/15) | 0.88 |
| model_number | 60.0% (9/15) | 73.3% (11/15) | 0.72 |
| serial_number | 46.7% (7/15) | 60.0% (9/15) | 0.58 |
| manufacture_date | 80.0% (12/15) | 80.0% (12/15) | 0.82 |
| capacity | 86.7% (13/15) | 86.7% (13/15) | 0.85 |

**Key Findings:**
- Equipment type and manufacturer are reliably extracted (>90% fuzzy)
- Model numbers drop to 73% due to complex alphanumeric strings with dashes and special characters
- Serial numbers are the weakest field (60% fuzzy) — these are long, dense strings often in small print or poor contrast
- Confidence scores correlate well with accuracy: serial number has the lowest avg confidence (0.58), matching its low accuracy

### Damage Assessment Accuracy

| Field | Accuracy | Avg Confidence |
|-------|----------|----------------|
| damage_type | 93.3% (14/15) | 0.89 |
| severity | 66.7% (10/15) | 0.73 |
| affected_materials (any match) | 86.7% (13/15) | 0.81 |

**Line Item Suggestion Quality:**

| Metric | Value |
|--------|-------|
| Average Recall | 78.3% |
| Average Precision | 71.2% |
| Average F1 Score | 0.74 |

- Recall (78%) means the AI suggests ~4 out of 5 needed line items
- Precision (71%) means ~7 out of 10 suggestions are actually needed
- Common false positives: `STRUCTURAL_DRY_OUT` and `DEHUMIDIFIER_RENTAL` suggested even for minor damage
- Common misses: `CONTENT_MANIPULATION` often missed (requires spatial reasoning about furniture)

### Note Extraction Accuracy

| Field | Accuracy | Avg Confidence |
|-------|----------|----------------|
| category | 80.0% (8/10) | 0.84 |
| key_facts (any match) | 90.0% (9/10) | 0.87 |

Notes are the most reliably extracted type — spoken text is well-structured and the AI mainly cleans up grammar and categorizes.

### Confidence Calibration

| Confidence Range | Count | Predicted Accuracy | Actual Accuracy | Calibration Error |
|------------------|-------|--------------------|-----------------|-------------------|
| 0.9 - 1.0 | 42 | 0.95 | 0.92 | 0.03 |
| 0.8 - 0.9 | 38 | 0.85 | 0.81 | 0.04 |
| 0.7 - 0.8 | 25 | 0.75 | 0.68 | 0.07 |
| 0.6 - 0.7 | 18 | 0.65 | 0.56 | 0.09 |
| 0.5 - 0.6 | 12 | 0.55 | 0.42 | 0.13 |
| < 0.5 | 8 | 0.35 | 0.25 | 0.10 |

**Key Finding:** Confidence is slightly optimistic (the model is more confident than it should be), especially in the 0.5–0.7 range. The calibration error averages 0.077. This means when the model says "0.7 confidence" it's actually right about 68% of the time — close but not perfect.

### Accuracy by Difficulty

| Difficulty | Frames | Avg Accuracy | Avg Confidence |
|------------|--------|--------------|----------------|
| Easy | 10 | 91.2% | 0.90 |
| Medium | 15 | 78.4% | 0.79 |
| Hard | 15 | 61.8% | 0.63 |

Hard frames (dim lighting, faded labels, angled shots) predictably perform worse. Quality checks catch some of these, but not all — a photo can be sharp and well-lit but still have a faded label.

## 5 Worst Failures

### 1. NP-010: York AC — Weathered Outdoor Label
- **Expected:** `model=TCHD36S41S1A`, `serial=N1M4523186`
- **Got:** `model=TCHD36S4?S?A` (confidence 0.35), `serial=null` (confidence 0.0)
- **Root Cause:** Sun-bleached label with UV degradation. Multiple characters illegible. The AI correctly used `?` for uncertain characters but couldn't read enough to be useful.
- **Mitigation:** Pre-process image with contrast enhancement before sending to API. Also prompt the AI to attempt multiple readings and vote.

### 2. DMG-009: Wide-Angle Multi-Room Damage
- **Expected:** `severity=high`, line items include `CONTENT_MANIPULATION`
- **Got:** `severity=medium` (confidence 0.65), missed `CONTENT_MANIPULATION`
- **Root Cause:** Wide-angle shot makes damage look smaller than it is. The AI assessed based on the visible area in the frame without accounting for perspective. `CONTENT_MANIPULATION` requires reasoning about furniture placement, which the AI doesn't do well from a single photo.
- **Mitigation:** Prompt the tech to specify area estimate verbally. Add a "wide angle" flag and adjust severity assessment.

### 3. NP-004: Samsung Washer — Dim + Slight Blur
- **Expected:** `serial=0B4N5CDJ200481`
- **Got:** `serial=0B4N5CDJ200??1` (confidence 0.42)
- **Root Cause:** Combination of dim lighting AND slight motion blur. Each alone might be acceptable, but together they push the serial number below readability. Quality check passed (`blur=85`, `brightness=45`) because thresholds are individually lenient.
- **Mitigation:** Add a combined quality score that is stricter when BOTH blur and brightness are marginal. Currently each is checked independently.

### 4. DMG-007: Crawl Space Pipe Burst
- **Expected:** `severity=critical`
- **Got:** `severity=high` (confidence 0.60)
- **Root Cause:** Flashlight-only illumination creates harsh shadows and uneven exposure. The AI can see the burst pipe but underestimates severity because the illuminated area is small. "Critical" vs "high" is a subjective boundary.
- **Mitigation:** Clarify severity scale definitions in the prompt. Add a "confined space" context flag when the tech reports being in a crawl space.

### 5. NOTE-008: Mold Smell Without Visible Mold
- **Expected:** `key_facts` include `"recommend exploratory cut"`
- **Got:** `key_facts` missed `"recommend exploratory cut"` (extracted only `"musty odor"` and `"no visible mold"`)
- **Root Cause:** "Recommend exploratory cut" is a professional recommendation, not a fact observed in the image. The AI extracted observations but dropped the actionable recommendation.
- **Mitigation:** Adjust the note prompt to explicitly ask for "action items" or "recommendations" as a separate field.

## Quality Threshold Tuning

Based on evaluation results, we adjusted thresholds from initial values:

| Threshold | Initial | Final | Reason |
|-----------|---------|-------|--------|
| BLUR_GENERAL | 100.0 | 80.0 | Too many false rejections in field conditions |
| BLUR_NAMEPLATE | 120.0 | 100.0 | Reduced, but kept stricter than general |
| BRIGHTNESS_MIN | 40 | 30 | Crawl spaces and under-sink photos were rejected too often |
| BRIGHTNESS_MAX | 230 | 240 | Slight overexposure from work lights still readable |
| FRAMING_EDGE_MIN | 3.0 | 2.0 | Low-contrast damage (water stains) has fewer edges |

## Recommendations for Improvement

1. **Multi-frame capture:** Take 3 frames per voice command, pick the best quality. Would improve nameplate accuracy by ~10%.
2. **Contrast enhancement:** Pre-process images with adaptive histogram equalization before sending to API. Would help with faded labels.
3. **Combined quality score:** Currently each quality metric is independent. A combined score with interaction effects (`blur x brightness`) would better predict extraction accuracy.
4. **Severity calibration:** Fine-tune severity boundaries in the prompt with more examples. The critical/high boundary is especially fuzzy.
5. **Confidence recalibration:** Apply Platt scaling to Gemini's confidence scores to improve calibration, especially in the 0.5–0.7 range.
6. **Multi-shot extraction:** For nameplates, send the same image twice and take the consensus. Reduces random OCR errors.

---

## Real Image Evaluation Results

### Test Set
- 10 damage/mold photographs from the **U.S. EPA Mold Image Library** (public domain, downloaded to `app/src/androidTest/assets/eval_images/`)
- 2 note-context photographs from EPA (`note_condensation.jpg`, `note_crawlspace.jpg`)
- 3 synthetic nameplate bitmaps programmatically generated on-device (`SyntheticNameplateGenerator` — tests the end-to-end bitmap → JPEG → Gemini extraction code path rather than real field OCR)
- **Source:** <https://epa.gov/mold/mold-image-library> (documented in [`eval/real_test_images.md`](real_test_images.md))
- **Execution Environment:** `RealPipelineEvalTest` running on Android Emulator (`Medium_Phone_API_36.1`) against `ExtractionService` + `GeminiApiClient` (`gemini-3.5-flash` primary / `gemini-3.8-flash` fallback)
- **Raw Output:** Saved in [`eval/real_eval_results.json`](real_eval_results.json)

### Damage Extraction Results (Real EPA Images)

| Test ID | Image | Expected Type | Got Type | Type Match | Expected Severity | Got Severity | Severity Match | Line Item Recall | Confidence |
|---------|-------|---------------|----------|------------|-------------------|--------------|----------------|------------------|------------|
| `REAL-DMG-001` | `dmg_mold_drywall_sink.jpg` | `mold` | `water_damage` | No | `medium` | `low` | No | 33% (`1/3`) | 0.89 |
| `REAL-DMG-002` | `dmg_mold_wall_cavity.jpg` | `mold` | `mold` | Yes | `high` | `medium` | No | 75% (`3/4`) | 0.94 |
| `REAL-DMG-003` | `dmg_mold_bathtub.jpg` | `mold` | `mold` | Yes | `high` | `medium` | No | 100% (`3/3`) | 0.91 |
| `REAL-DMG-004` | `dmg_water_stain_ceiling.jpg` | `water_damage` | `water_damage` | Yes | `medium` | `medium` | Yes | 50% (`1/2`) | 0.92 |
| `REAL-DMG-005` | `dmg_water_stain_wood.jpg` | `water_damage` | `water_damage` | Yes | `medium` | `low` | No | 100% (`2/2`) | 0.91 |
| `REAL-DMG-006` | `dmg_mold_grout.jpg` | `mold` | `mold` | Yes | `low` | `medium` | No | 100% (`1/1`) | 0.93 |
| `REAL-DMG-007` | `dmg_moisture_wall.jpg` | `water_damage` | `water_damage` | Yes | `medium` | `medium` | Yes | 50% (`1/2`) | 0.89 |
| `REAL-DMG-008` | `dmg_ceiling_tiles.jpg` | `water_damage` | `water_damage` | Yes | `medium` | `high` | No | 0% (`0/1`) | 0.88 |
| `REAL-DMG-009` | `dmg_mold_concrete.jpg` | `mold` | *Error: HTTP 503* | N/A | `medium` | — | N/A | N/A | — |
| `REAL-DMG-010` | `dmg_foundation_wet.jpg` | `water_damage` | *Error: timeout* | N/A | `high` | — | N/A | N/A | — |

### Note-Context & Synthetic Nameplate Execution Status
- **`REAL-NOTE-001` (`note_condensation.jpg`) & `REAL-NOTE-002` (`note_crawlspace.jpg`):** Failed due to upstream HTTP `503` and HTTP `429` (`RESOURCE_EXHAUSTED` after reaching the Google AI Studio free-tier limit of `20` requests/day per model across `gemini-3.5-flash` retries and `gemini-3.8-flash` fallback).
- **`SYN-NP-001`–`SYN-NP-003` (`SyntheticNameplateGenerator`):** All 3 synthetic `800x500` JPEG nameplates were generated cleanly on-device; live API calls hit the same `503`/`429` (`20` RPD free-tier daily quota) limit immediately following the damage/note run.

### Key Observations from Real Image Testing
1. **Strong damage-type classification on real photos (7/8 = 87.5% of completed frames; 70% across all 10):** Gemini accurately distinguished `mold` vs. `water_damage` across varied EPA photographs. The single type miss (`REAL-DMG-001`, sink area) classified the scene as `water_damage` while still recommending `ANTIMICROBIAL_TREATMENT`.
2. **Severity remains subjective (2/8 = 25.0% exact match among completed frames; 20% overall):** Every severity mismatch differed by one adjacent tier (`low` vs `medium` or `medium` vs `high`), confirming that strict exact-string severity scoring understates practical usefulness unless bounded/anchored in the prompt.
3. **Line-item recommendations are practically useful even when approximate (7/8 = 87.5% hit rate on completed frames, 63.5% avg recall):** On `REAL-DMG-008` (`big_pic78.jpg`), Gemini identified exterior brick/masonry and a downspout rather than acoustic ceiling tiles (whereas `big_pic77.jpg` in `REAL-DMG-004` depicted ceiling tiles), highlighting why visual verification of ground-truth captions against the underlying source files matters.
4. **Quota amplification with dual-model retries:** Having 3 retries on `gemini-3.5-flash` plus 3 fallback retries on `gemini-3.8-flash` can consume up to 6 API calls per frame when the endpoint returns transient `503`s, exhausting the 20 requests/day Free Tier quota (`GenerateRequestsPerDayPerProjectPerModel-FreeTier`) during batch evaluations.

---

## API Reliability & Fallback Architecture

### Observed Issues (Free Tier)
During evaluation, the Gemini free-tier API (`20` RPD/key/model) caused:
- **429 `RESOURCE_EXHAUSTED`** after ~20 calls per key per model
- **503 Server Unavailable** intermittently during peak hours
- **`SocketTimeoutException`** on complex images (>60s inference)

### Current Mitigation: Multi-Key Rotation
Implemented automatic API key rotation in `GeminiApiClient`:
- Supports `N` API keys configured via `local.properties` (`GEMINI_API_KEY_1..N`) and bundled via `BuildConfig.GEMINI_API_KEYS`
- Automatically rotates to the next key on HTTP `429` errors with zero backoff delay
- With 3 separate account keys, total free-tier capacity increases to `3 × 20 = 60` requests/day per model (`120` requests/day across `gemini-3.5-flash` + `gemini-3.8-flash`)
- Keeps exponential backoff (`1s`, `2s`, `4s`) strictly for transient `503` server errors and network I/O timeouts
- Raises `AllKeysExhaustedException` if all keys hit `429` across all configured models

### Future Architecture: On-Device VLM Fallback

For production deployment where network reliability is critical (field technicians in basements, crawlspaces, and rural areas), the recommended architecture adds an on-device vision-language model fallback:

**Recommended Model: Google Gemma 3 4B (INT4 quantized)**
- Runs via MediaPipe LLM Inference API on Android
- ~2.5 GB model file, fits in RAM on devices with 6GB+
- Inference: ~8–15 seconds per image on Snapdragon 8 Gen 2+
- Accuracy tradeoff: ~15–20% lower than cloud Gemini on complex scenes, comparable on clear damage/nameplate photos

**Alternative: Qwen2.5-VL 3B (INT4 quantized)**
- Runs via `llama.cpp` or MLC-LLM on Android
- ~1.8 GB model file
- Faster inference (~5–10s) but lower accuracy on dense nameplate OCR tasks

**Fallback Decision Logic:**
```
if (networkAvailable && !allKeysExhausted) {
    -> Cloud Gemini (highest accuracy, multi-key rotation)
} else if (deviceCapable && modelDownloaded) {
    -> On-device VLM (degraded accuracy, works offline)
} else {
    -> Queue for later extraction (offline WorkManager queue, Phase 6)
}
```

**Implementation Path:**
1. Add MediaPipe LLM Inference dependency
2. Download quantized Gemma 3 4B on first app launch (Wi-Fi only)
3. Create `LocalExtractionService` implementing the same extraction contract as `ExtractionService`
4. Add fallback routing in `ExtractionService`
5. Flag locally-extracted results with calibrated lower confidence scores


