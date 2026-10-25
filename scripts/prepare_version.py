#!/usr/bin/env python3
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (ROOT / 'version.properties').read_text().splitlines() if '=' in line)
name, code = properties['versionName'], properties['versionCode']
if not re.fullmatch(r'\d+\.\d+\.\d+', name) or not code.isdecimal() or int(code) <= 0:
    raise SystemExit('Invalid version.properties')
android = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', android)
tree = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
tree.getroot().set(f'{{{android}}}versionName', name)
tree.getroot().set(f'{{{android}}}versionCode', code)
output = ROOT / 'build/generated'
output.mkdir(parents=True, exist_ok=True)
tree.write(output / 'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
java = output / 'dev/dothome/BuildInfo.java'
java.parent.mkdir(parents=True, exist_ok=True)
java.write_text(f'package dev.dothome; public final class BuildInfo {{ public static final String VERSION_NAME="{name}"; public static final int VERSION_CODE={code}; }}\n')
print(name)
