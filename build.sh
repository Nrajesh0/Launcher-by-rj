#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
python3 scripts/prepare_version.py
bash scripts/build_foundation.sh
if [[ "${1:-}" != '--unsigned' ]]; then
  version=$(python3 -c 'from pathlib import Path; print(dict(x.split("=",1) for x in Path("version.properties").read_text().splitlines() if "=" in x)["versionName"])')
  bash scripts/sign_apk.sh "$PWD/build/dot-home-$version-unsigned.apk"
fi
