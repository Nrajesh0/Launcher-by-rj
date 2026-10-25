"""Verify a locally signed preview against its successful CI build artifact.

Preview publication deliberately does not imply a passing UI suite. Stable release
verification remains in verify_prebuilt.py and requires the entire CI run to pass.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CERTIFICATE = "b5b244be833dd77faa0f6bd94ad59367032246d302be40865c3bead78586f8a1"


def api(endpoint):
    return json.loads(subprocess.check_output(["gh", "api", endpoint], text=True))


version = dict(line.split("=", 1) for line in (ROOT / "version.properties").read_text().splitlines() if "=" in line)["versionName"]
assert re.fullmatch(r"\d+\.\d+\.\d+", version)
folder = ROOT / "release-assets" / f"v{version}"
apk = folder / f"dot-home-{version}.apk"
expected = (folder / f"{apk.name}.sha256").read_text().split()[0]
assert hashlib.sha256(apk.read_bytes()).hexdigest() == expected, "APK checksum mismatch"
provenance = json.loads((folder / "provenance.json").read_text())
assert provenance["certificate_sha256"] == CERTIFICATE, "Unrecognized signing identity"
sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "/workspace/android-sdk")))
build_tools = sdk / "build-tools" / "36.0.0"
if not build_tools.exists():
    build_tools = sdk / "build-tools" / "android-16"
certs = subprocess.check_output([str(build_tools / "apksigner"), "verify", "--print-certs", str(apk)], text=True)
assert CERTIFICATE in certs, "APK signing identity mismatch"
subprocess.run(["python3", str(ROOT / "tests/check_offline.py"), str(build_tools / "aapt"), str(apk)], check=True)
repo = os.environ["GH_REPO"]
run_id = provenance["actions_run_id"]
assert isinstance(run_id, int) and run_id > 0
run = api(f"repos/{repo}/actions/runs/{run_id}")
assert run["head_sha"] == provenance["source_commit"], "Source build mismatch"
assert run["path"] == ".github/workflows/android.yml", "Unexpected build workflow"
jobs = api(f"repos/{repo}/actions/runs/{run_id}/jobs?per_page=100")["jobs"]
assert any(job["name"] == "build" and job["conclusion"] == "success" for job in jobs), "Compilation/model/offline build job must pass"
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(["gh", "run", "download", str(run_id), "--repo", repo, "--name", f"dot-home-{version}", "--dir", tmp], check=True)
    candidates = list(Path(tmp).glob("*-unsigned.apk"))
    assert len(candidates) == 1, "Expected one unsigned CI APK"
    with zipfile.ZipFile(candidates[0]) as original, zipfile.ZipFile(apk) as signed:
        original_names = set(original.namelist())
        signed_names = set(signed.namelist())
        assert original_names <= signed_names, "APK entry removed from CI artifact"
        # apksigner adds these v1 certificate entries; v2/v3 signatures live
        # outside ZIP entries. No other additions, removals or edits are allowed.
        signature_entries = {"META-INF/MANIFEST.MF", "META-INF/DOTHOME.SF", "META-INF/DOTHOME.RSA"}
        assert signed_names - original_names <= signature_entries, "Unexpected APK entries added"
        for name in original.namelist():
            assert original.read(name) == signed.read(name), f"APK entry differs from CI: {name}"
print(f"Verified preview {version}: CI build payload, signature, checksum and offline contract. Full UI verification is pending.")
