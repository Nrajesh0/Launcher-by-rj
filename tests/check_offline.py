#!/usr/bin/env python3
"""Enforces the offline contract in source and, when provided, the packaged APK."""
from pathlib import Path
import subprocess, sys, re, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml')
android='{http://schemas.android.com/apk/res/android}'
for entry in manifest.getroot().findall('uses-permission'):
    assert entry.get(android+'name') not in {'android.permission.INTERNET','android.permission.ACCESS_NETWORK_STATE','android.permission.CHANGE_NETWORK_STATE'}, 'Network permission found'
for path in (root/'app/src/main/java').rglob('*.java'):
    source=path.read_text()
    assert not re.search(r'java\.net\.|javax\.net\.|android\.webkit\.|HttpURLConnection|URLConnection|\bSocket\b|https?://|open-meteo|ACTION_INTERNET_CONNECTIVITY',source,re.I), f'Network implementation found: {path}'
if len(sys.argv)>2:
    badging=subprocess.check_output([sys.argv[1],'dump','badging',sys.argv[2]],text=True)
    assert 'android.permission.INTERNET' not in badging, 'APK requests Internet'
    version=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line)['versionName']
    assert f"versionName='{version}'" in badging, 'APK version mismatch' 
print('Offline source / APK permission checks passed')
