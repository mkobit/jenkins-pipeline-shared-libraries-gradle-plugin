#!/usr/bin/env python3
import json
import re
import subprocess
import sys

data = json.load(sys.stdin)
file_path = data.get("tool_input", {}).get("file_path", "")

if not file_path:
    sys.exit(0)
if "/build/" in file_path:
    sys.exit(0)
if not re.search(r"\.(kt|kts|groovy|java)$", file_path):
    sys.exit(0)

subprocess.run(["./gradlew", "spotlessApply", "--quiet"], check=True)
