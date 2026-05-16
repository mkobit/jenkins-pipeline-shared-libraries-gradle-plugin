#!/usr/bin/env python3
# PostToolUse hook: runs spotlessApply after any Edit/Write to a Kotlin, Groovy, or Java file.
# Skips generated files under build/ to avoid redundant formatting on outputs.
import json
import os
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

project_dir = os.environ.get("CLAUDE_PROJECT_DIR", ".")
subprocess.run(["./gradlew", "spotlessApply", "--quiet"], check=True, cwd=project_dir)
