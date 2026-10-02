"""Reads CHANGELOG.md for the release workflow.

  changelog.py stamp VERSION   Rename the "Unreleased" heading to VERSION and open a new empty
                               "Unreleased" above it. Exit 0 if it had items, 1 if it was empty.
  changelog.py notes VERSION   Print release notes for VERSION (exit 1 if it has no section).
  changelog.py versions        Print every version that has a section, newest first.
"""
import re
import sys

PATH = "CHANGELOG.md"
HEAD = re.compile(r"^## (.+?)\s*$")


def sections(text):
    """Map heading -> list of '- ' items (continuation lines joined)."""
    out, cur = {}, None
    for line in text.splitlines():
        m = HEAD.match(line)
        if m:
            cur = out.setdefault(m.group(1), [])
        elif cur is not None and line.startswith("- "):
            cur.append(line[2:].strip())
        elif cur and line.startswith("  ") and line.strip():
            cur[-1] += " " + line.strip()
    return out


def main():
    cmd, *args = sys.argv[1:]
    text = open(PATH, encoding="utf-8").read()
    secs = sections(text)
    if cmd == "stamp":
        version = args[0]
        if not secs.get("Unreleased"):
            return 1
        text = re.sub(r"^## Unreleased[ \t]*$", "## Unreleased\n\n## " + version, text, count=1, flags=re.M)
        open(PATH, "w", encoding="utf-8", newline="\n").write(text)
        return 0
    if cmd == "notes":
        items = secs.get(args[0])
        if not items:
            return 1
        print("## What changed\n")
        for item in items:
            print("- " + item)
        print("\n## Install\n")
        print("Download the .apk file below on the phone, open it, and allow the install. "
              "It installs over the previous version.")
        return 0
    if cmd == "versions":
        for name in secs:
            if name != "Unreleased":
                print(name)
        return 0
    return 2


if __name__ == "__main__":
    sys.exit(main())
