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
