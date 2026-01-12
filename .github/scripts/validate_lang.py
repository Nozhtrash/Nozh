import glob
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LANG_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "nozh", "lang")

REQUIRED_KEYS = [
    "key.nozh.apply_suggestion",
    "category.nozh",
]


def fail(msg: str) -> None:
    print(msg, file=sys.stderr)
    sys.exit(1)


def main() -> None:
    if not os.path.isdir(LANG_DIR):
        fail(f"Lang dir not found: {LANG_DIR}")

    paths = sorted(glob.glob(os.path.join(LANG_DIR, "*.json")))
    if not paths:
        fail(f"No lang json files found in: {LANG_DIR}")

    bad = 0
    for p in paths:
        rel = os.path.relpath(p, ROOT)
        try:
            with open(p, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            bad += 1
            print(f"[FAIL] {rel}: invalid JSON ({e})", file=sys.stderr)
            continue

        if not isinstance(data, dict):
            bad += 1
            print(f"[FAIL] {rel}: root is not an object", file=sys.stderr)
            continue

        missing = [k for k in REQUIRED_KEYS if k not in data]
        if missing:
            bad += 1
            print(f"[FAIL] {rel}: missing keys: {', '.join(missing)}", file=sys.stderr)
            continue

        print(f"[OK]   {rel} ({len(data)} keys)")

    if bad:
        fail(f"Lang validation failed: {bad} file(s) invalid/missing keys")


if __name__ == "__main__":
    main()
