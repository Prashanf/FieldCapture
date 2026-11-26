# FieldCapture Extraction Evaluation Report

## Overview

| Metric | Value |
|--------|-------|
| Test Set Size | **30 real phone camera photos** (`4000×2252` JPEG) |
| Nameplate Frames | 13 (`REAL-NP-001` – `REAL-NP-013`, 12 equipment + 1 negative control) |
| Damage Frames | 13 (`REAL-DMG-001` – `REAL-DMG-013`) |
| Note Frames | 4 (`REAL-NOTE-001` – `REAL-NOTE-004`) |
| Models Evaluated | `gemini-3.5-flash` (initial frames) + `gemini-3.5-flash-lite` (fallback completion across 3 rotated keys) |
| Evaluation Date | 2026-11-27 |

> **All-Real Dataset Note:** All 30 frames in [`eval/ground_truth.json`](ground_truth.json) (`eval/test_set/real_captures/`) are genuine physical photographs captured directly on the phone camera — no downloaded stock/EPA images or mocked extraction outputs are used in these metrics. Raw per-frame quality scores, model responses, and aggregated metrics are saved in [`eval/real_eval_results.json`](real_eval_results.json) and [`eval/eval_results.csv`](eval_results.csv).

## Methodology

### Test Set Construction
- **13 nameplate/label photos:** Usha Air Fryer, Ant Esports Scorpion MK20 Keyboard, Logitech M90 Mouse, Nawab Parker packaging label (negative control), Dell WK118 Wireless Keyboard, Portronics Power Plate 10 (low-contrast embossed black-on-black), Samsung Travel Adapter `EP-TA20IBE`, Samsung Refrigerator BEE energy plate — including **5 bright vs. dim/motion-blurry retake pairs** (`REAL-NP-009` – `REAL-NP-013`).
- **13 damage/inspection scenes:** Peeling/blistered wall paint, wall corner staining, window sill paint flaking, open ceiling holes, hairline cracks, and bright/dim under-sink plumbing enclosures.
- **4 note-context captures:** Intact marble tile floor, under-sink P-trap check, upper wall moisture baseline, and dim under-sink clamp safety note paired with spoken transcripts.
- **Difficulty distribution:** 10 `easy`, 9 `medium`, 11 `hard`.
- Images were EXIF-normalized (`1280px` max side) to match mobile upload sizing and executed through FieldCapture's production `ImageQualityAnalyzer`, `ExtractionService`, `GeminiApiClient` (3-key rotation), and `EvalRunner`.

### On-Device Quality Gate Results (`ImageQualityAnalyzer`)
| Category | Passed | Rejected | Primary Rejection Reasons |
|----------|--------|----------|-------------------------|
| Nameplates (`13`) | `9/13` (`69.2%`) | `4/13` | `too blurry` (`29.5` on `REAL-NP-010`), `too dark` (`bright 7–16` on `REAL-NP-009/012/013`) |
| Damage (`13`) | `9/13` (`69.2%`) | `4/13` | `badly framed` (`edge 0.8–1.9% < 2.0%` on plain painted walls), `too blurry` (`58.0` on `REAL-DMG-006`) |
| Notes (`4`) | `3/4` (`75.0%`) | `1/4` | `badly framed` (`edge 0.9%` on plain upper wall `REAL-NOTE-003`) |
| **Total (`30`)** | **`21/30` (`70.0%`)** | **`9/30` (`30.0%`)** | Catching dark/blurry labels accurately prevented downstream hallucinations |

---

## Live Extraction Results

### Overall Pipeline Outcome
- **API Completion:** **30 / 30 frames** returned valid JSON from Gemini (`29` domain extractions + `1` clean negative-control rejection `"error": "not_a_nameplate"` on `REAL-NP-004` Nawab Parker garment box).

### Nameplate Extraction Accuracy (`12` valid nameplate captures)

| Field | Accuracy (Exact) | Accuracy (Fuzzy) | Avg Confidence |
|-------|------------------|-------------------|----------------|
| `manufacturer` | 83.3% (10/12) | **83.3% (10/12)** | 0.83 |
| `serial_number` | 75.0% (9/12) | **83.3% (10/12)** | 0.64 |
| `capacity` | 83.3% (10/12) | **83.3% (10/12)** | 0.50 |
| `manufacture_date` | 75.0% (9/12) | **75.0% (9/12)** | 0.39 |
| `equipment_type` | 50.0% (6/12) | **50.0% (6/12)** | 0.83 |
| `model_number` | 41.7% (5/12) | **41.7% (5/12)** | 0.83 |

**Key Findings:**
- `manufacturer` (`83.3%`) and `serial_number` (`83.3%` fuzzy, `75.0%` exact) performed well on clear plates (`Usha`, `Dell`, `Samsung`, `Logitech`, `Ant Esports`) and even read embossed black-on-black `Portronics Power Plate10` (`REAL-NP-006`).
- `model_number` scored `41.7%` primarily because consumer hardware labels often print **two** identifiers (e.g., `M90` vs `M/N: M-U0026`, `SCORPION MK20` vs `PRODUCT CODE: MK1000`), and the model captured the alternate string in `model_number` vs `additional_specs`.
- Dark/blurry retakes degraded sharply: `REAL-NP-009` (`bright=8`) returned `null` across fields (`conf=0.10`), `REAL-NP-010` (`blur=29.5`) garbled `"2138 ????? ?ST"`, and dim LED glare (`REAL-NP-011`) caused a 5-char serial misread (`2438APC7HBK9?`).

### Damage Assessment Accuracy (`13` frames)

| Field | Accuracy (Fuzzy) | Avg Confidence |
|-------|------------------|----------------|
| `affected_materials` (any match) | **100.0% (13/13)** | 0.88 |
| `severity` | **61.5% (8/13)** | 0.87 |
| `damage_type` | **7.7% (1/13)** | 0.87 |

**Line Item Suggestion Quality (`13` damage frames):**

| Metric | Value |
|--------|-------|
| Average Recall | **83.3%** |
| Average Precision | **79.5%** |
| Average F1 Score | **0.78** |

- `affected_materials` (`100%`) and line-item suggestions (`0.78` F1, `83.3%` recall) remained strong even on mild indoor wear.
- `damage_type` exact-string agreement dropped to `7.7%` because the vision model conservatively classified minor wall scuffs/repairs as `"other"` (`severity="low"`) and ceiling openings as `"structural"`, whereas ground-truth assumptions had expected `"water_damage"`.

### Note Extraction Accuracy (`4` frames)

| Field | Accuracy (Fuzzy) | Avg Confidence |
|-------|------------------|----------------|
| `category` | **100.0% (4/4)** | 0.98 |
| `key_facts` (any match) | **75.0% (3/4)** | 0.99 |

- Spoken metadata transcripts categorized cleanly (`observation`, `measurement`, `safety_concern`). The single `key_facts` miss (`REAL-NOTE-004`) extracted full semantic facts (`"Drain hose clamp tightness needs to be checked"`) phrased more verbosely than the ground-truth substring `"check drain hose clamp"`.

### Confidence Calibration (`EvalRunner` 6 Buckets)

| Confidence Range | Field Count | Predicted Confidence | Actual Accuracy | Calibration Gap |
|------------------|-------------|----------------------|-----------------|-----------------|
| `0.9 - 1.0` | 75 | 0.95 | 0.76 | +0.19 |
| `0.8 - 0.9` | 18 | 0.83 | 0.44 | +0.39 |
| `0.7 - 0.8` | 2 | 0.75 | 0.50 | +0.25 |
| `0.6 - 0.7` | 1 | 0.60 | 0.00 | +0.60 |
| `0.5 - 0.6` | 1 | 0.50 | 0.00 | +0.50 |
| `< 0.5` | 22 | 0.05 | 0.59 | -0.54 |

- **Finding:** High-confidence predictions (`0.8–1.0`) are over-confident (`0.95` predicted vs `0.76` actual accuracy), driven by taxonomy disagreements (`"other"` vs `"water_damage"`, `"Computer Mouse"` vs `"Mouse"`, `"M90"` vs `"M-U0026"`). Meanwhile the `< 0.5` bucket appears under-confident because optional `null` nameplate fields (`capacity`, `manufacture_date`) carry `0.0` confidence even when `null == null` matches ground truth.

### Accuracy by Difficulty

| Difficulty | Frames | Avg Field Accuracy |
|------------|--------|--------------------|
| Easy | 10 | **85.0%** |
| Medium | 9 | **62.1%** |
| Hard | 11 | **54.0%** |

Clear, bright captures (`85.0%`) dramatically outperform medium/hard captures (`62.1%` / `54.0%`), validating the value of paired bright vs dim testing.

---

## 5 Worst Failures (Measured from Live Run)

### 1. `REAL-NP-010`: Motion-Blurry Packaging Label (`20261126_235349.jpg`)
- **Expected:** `model=KS-20142`, `serial=NP123173`
- **Got:** `model="2138 ????? ?ST"`, `serial="MP??????"` (conf `0.20`), `manufacturer=null`
- **Root Cause:** Heavy hand motion blur pushed Laplacian variance down to `29.5` (below `100.0`). Bypassing the quality gate caused Gemini to output wildcards and partial misreads.
- **Mitigation:** Enforced automatically in production by `BlurDetector` (`29.5 < 100.0` triggers spoken retake).

### 2. `REAL-NP-009`: Under-Exposed Refrigerator BEE Label (`20261126_235224.jpg`)
- **Expected:** `manufacturer=Samsung`, `model=RR23H2H35HS/HL/2026`, `capacity=215 liters`
- **Got:** All four fields returned `null` (`conf=0.10`), `equipment_type="Energy Rating Label"`
- **Root Cause:** Mean brightness dropped to `8` (and blur `54.1`) in dark shadow, collapsing printed text contrast.
- **Mitigation:** Caught by `BrightnessAnalyzer` (`8 < 30`) + `BlurDetector`; prompt tech to turn on flash/work light.

### 3. `REAL-NP-011`: Dim Mouse Label with Red Optical Glare (`20261126_235438.jpg`)
- **Expected:** `serial=2435APCH9K68`
- **Got:** `serial="2438APC7HBK9?"` (conf `0.80` — 5-char OCR drift)
- **Root Cause:** Marginally passed both individual thresholds (`blur=129.9 >= 100.0`, `bright=42 >= 30`), while red optical LED bloom lowered local serial-number contrast.
- **Mitigation:** Add a combined low-light + marginal-sharpness gate (`blur < 150 && brightness < 50`) to warn techs on dark handheld plates.

### 4. `REAL-NP-003` & `REAL-NP-002`: Dual Model/Part Numbers (`M90`/`M-U0026` & `SCORPION MK20`/`MK1000`)
- **Expected:** `model=M-U0026` (`REAL-NP-003`), `model=SCORPION MK20` (`REAL-NP-002`)
- **Got:** `model="M90"` (`conf=1.0`), `model="MK1000"` (`conf=0.95`) — alternate code still extracted into `additional_specs`
- **Root Cause:** Labels carry both a series name (`M90`, `SCORPION MK20`) and a regulatory/product code (`M/N: M-U0026`, `PRODUCT CODE: MK1000`).
- **Mitigation:** Include secondary regulatory codes cleanly in fuzzy alias matching and refine `nameplatePrompt()` guidance on series vs part codes.

### 5. `REAL-DMG-004` / `007` / `010` & `REAL-NOTE-003`: Low-Contrast White Drywall Framing Rejections
- **Observed:** `FramingAnalyzer` rejected plain white/cream walls (`edge=0.8%–1.9% < 2.0%`), despite Gemini cleanly recognizing ceiling holes and wall scuffs (`0.78` line-item F1).
- **Root Cause:** `FRAMING_EDGE_MIN = 2.0%` assumes textured damage across the center `60%`, over-rejecting localized blemishes on smooth painted walls.
- **Mitigation:** Lower `FRAMING_EDGE_MIN` from `2.0` to `0.5–1.0` for `DAMAGE` captures (or evaluate local max grid cell edge density).

---

## Quality Threshold Tuning Summary

| Threshold | Current Value | Field Behavior on Real 30-Frame Set | Recommended Next Tuning |
|-----------|---------------|--------------------------------------|-------------------------|
| `BLUR_THRESHOLD_NAMEPLATE` | `100.0` | Caught severe blur (`29.5`, `54.1`) cleanly; bright plates measured `880–7708` | Keep `100.0` + add combined warning when `blur < 150` & `brightness < 50` |
| `BLUR_THRESHOLD_GENERAL` | `80.0` | Caught smooth blurry frames (`37.3`, `58.0`) | Keep `80.0` |
| `BRIGHTNESS_MIN` | `30` | Caught unusable dark shots (`7`, `8`, `16`); dim-but-readable shots scored `38–49` | Keep `30` |
| `BRIGHTNESS_MAX` | `240` | Bright white walls peaked at `181` (zero false overexposure rejections) | Keep `240` |
| `FRAMING_EDGE_MIN` | `2.0` | Over-rejected clean painted drywall defects (`0.8–1.9%`) | Lower to **`0.5–1.0`** for `DAMAGE`/`NOTE` |

---

## API Reliability & Fallback Architecture

### Observed Issues & Live Key Rotation Behavior
During the 30-frame benchmark (plus earlier emulator testing), Google AI Studio free-tier limits (`20` RPD/key/model on `gemini-3.5-flash` & `gemini-3.8-flash`) caused:
- **`503 Server Unavailable`** and **`SocketTimeoutException`** (>60s) on `gemini-3.5-flash` during frames `REAL-NP-008..013`, triggering `3×2` retries that consumed `3.5`/`3.8` daily quota across keys `1..3`.
- Switching/falling back to **`gemini-3.5-flash-lite`** (added as 3rd endpoint in `GeminiApiClient.DEFAULT_MODEL_ENDPOINTS`) completed the remaining **23 frames in 1m 53s** with **zero errors or timeouts**.

### Current Mitigation: 3-Key Rotation + 3-Model Fallback
- Supports `N` API keys via `local.properties` (`GEMINI_API_KEY_1..N`) and `BuildConfig.GEMINI_API_KEYS`.
- Rotates immediately with **zero backoff delay** on HTTP `429`, while retrying `503`/I/O errors on the same key with backoff (`1s`, `2s`, `4s`).
- Fallback order: `gemini-3.5-flash` → `gemini-3.8-flash` → `gemini-3.5-flash-lite`.

### Future Architecture: On-Device VLM Fallback
- **Recommended Model:** Google **Gemma 3 4B (INT4 quantized)** via MediaPipe LLM Inference API (~`2.5 GB`, `8–15s` per image on Snapdragon 8 Gen 2+) vs **Qwen2.5-VL 3B (INT4)** (`~1.8 GB`, lower OCR fidelity).
- **Decision Flow:** Cloud Gemini (rotated 3-key + 3-model) → On-device VLM (offline basements/exhausted quota) → Room + `ExtractionQueueWorker` sync.
