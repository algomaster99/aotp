import csv
import re
from pathlib import Path

# Make paths relative to this script's location so it works
# no matter where you run it from.
SCRIPT_DIR = Path(__file__).resolve().parent
INPUT_PATH = SCRIPT_DIR / "test.map"
OUTPUT_PATH = SCRIPT_DIR / "classes.csv"

# Regex to capture: @@ Class <size> <classname>
pattern = re.compile(r"@@ Class\s+(\d+)\s+(\S+)")

def sanitize_classname(classname: str) -> str:
    # . -> / 
    # Lambda/0x -> Lambda+0x
    return classname.replace(".", "/").replace("/0x", "+0x")

def main() -> None:
    with INPUT_PATH.open("r", encoding="utf-8", errors="ignore") as infile, \
            OUTPUT_PATH.open("w", newline="", encoding="utf-8") as outfile:

        writer = csv.writer(outfile)

        for line in infile:
            if "@@ Class" not in line:
                continue

            m = pattern.search(line)
            if not m:
                continue  # skip lines that don't match exactly

            size, classname = m.groups()
            writer.writerow([sanitize_classname(classname), size])

    print(f"Written CSV to {OUTPUT_PATH}")

if __name__ == "__main__":
    main()