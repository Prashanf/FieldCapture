"""Offline evaluation runner for FieldCapture extraction prompts.

Runs test frames in eval/test_frames/ through Gemini Vision extraction and writes eval_results.csv.
"""

from pathlib import Path
import csv

FRAMES_DIR = Path(__file__).parent / "test_frames"
RESULTS_CSV = Path(__file__).parent / "eval_results.csv"


def main() -> None:
    # TODO: Implement Gemini API calls for nameplate and damage test frames,
    #       score field-level accuracy, and write rows to eval_results.csv
    print(f"Scanning frames in {FRAMES_DIR}...")
    with RESULTS_CSV.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["frame_path", "category", "field", "expected", "predicted", "confidence", "correct"])
    print(f"Initialized {RESULTS_CSV}")


if __name__ == "__main__":
    main()
