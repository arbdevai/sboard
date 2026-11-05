#!/usr/bin/env python3
import os
import subprocess
import sys

def main():
    tag_name = os.environ.get("TAG_NAME", "v1.0.0")
    commit_msg = os.environ.get("COMMIT_MSG", "")
    
    # Extract git log since previous tag if available
    try:
        prev_tag_proc = subprocess.run(
            ["git", "describe", "--tags", "--abbrev=0", "HEAD~1"],
            capture_output=True,
            text=True
        )
        prev_tag = prev_tag_proc.stdout.strip()
    except Exception:
        prev_tag = ""

    if prev_tag:
        git_range = f"{prev_tag}..HEAD"
    else:
        git_range = "HEAD"

    try:
        log_proc = subprocess.run(
            ["git", "log", git_range, "--pretty=format:* %s (%h)"],
            capture_output=True,
            text=True
        )
        commits = log_proc.stdout.strip()
    except Exception:
        commits = ""

    if not commits:
        commits = f"* {commit_msg}" if commit_msg else "* Release update"

    notes = f"""# Sboard {tag_name}

## Ringkasan Perubahan
{commits}

## Informasi Rilis
- Target Platform: Android 11+ (API Level 30+)
- Arsitektur: Jetpack Compose + Modern IME Lifecycle
- Keamanan: Zero Internet Permission (100% Offline & Private)
"""

    with open("release_notes.md", "w", encoding="utf-8") as f:
        f.write(notes)
    print("release_notes.md generated successfully.")

if __name__ == "__main__":
    main()
