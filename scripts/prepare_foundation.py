#!/usr/bin/env python3
"""Prepare pinned source and overlays in an owned, ignored build directory."""
from pathlib import Path
import hashlib
import os
import subprocess

project = Path(__file__).resolve().parents[1]
root = project/'build/foundation'
props = dict(line.split('=',1) for line in (project/'foundation/upstream.properties').read_text().splitlines() if '=' in line)

def run(*args, cwd=project):
    subprocess.run(list(args), cwd=cwd, check=True)

git = ['git','-c','credential.helper=','-c','http.extraheader=']
if not root.exists():
    root.parent.mkdir(parents=True,exist_ok=True)
    seed = os.environ.get('DOT_FOUNDATION_SEED', props['repository'])
    run(*git, 'clone','--quiet','--depth','1','--branch',props['tag'],seed,str(root))
head = subprocess.check_output(git+['-C',str(root),'rev-parse','HEAD'],text=True).strip()
assert head == props['commit'], 'Unexpected foundation revision; refusing to modify this checkout'
run(*git,'submodule','update','--init','--depth','1',cwd=root)
actual = subprocess.check_output(git+['-C',str(root/'platform_frameworks_libs_systemui'),'rev-parse','HEAD'],text=True).strip()
assert actual == props['systemuiCommit'], 'System UI library revision mismatch'
digest = hashlib.sha256()
for file in sorted((project/'foundation').rglob('*')):
    if file.is_file() and '__pycache__' not in file.parts:
        digest.update(str(file.relative_to(project)).encode()); digest.update(file.read_bytes())
marker = root/'.dot-overlay-version'
digest.update((project/'version.properties').read_bytes())
version = digest.hexdigest()
if marker.exists() and marker.read_text() == version:
    print('Pinned foundation and overlays unchanged.'); raise SystemExit(0)
# This checkout is created and owned by this script, never the user's working tree.
run(*git,'reset','--hard',props['commit'],cwd=root)
run(*git,'clean','-fd',cwd=root)
run(*git,'reset','--hard',props['systemuiCommit'],cwd=root/'platform_frameworks_libs_systemui')
run(*git,'clean','-fd',cwd=root/'platform_frameworks_libs_systemui')
for patch in ('offline.py','experience.py','polish.py','core_features.py','design_system.py'):
    run('python3',str(project/'foundation/patches'/patch),str(root))
# Resolve only build-time tooling before converting SVG resources.
run('bash','gradlew','help','--no-daemon','--max-workers=2',cwd=root)
icons = project/'build/icon-source'
if not icons.exists():
    run(*git,'init','--quiet',str(icons))
    run(*git,'remote','add','origin',props['iconsRepository'],cwd=icons)
    run(*git,'fetch','--quiet','--depth','1','origin',props['iconsCommit'],cwd=icons)
run(*git,'checkout','--detach',props['iconsCommit'],cwd=icons)
run('python3',str(project/'foundation/tools/bundle_icons.py'),str(icons),str(root))
marker.write_text(version)
print('Pinned source, offline removals, local assets and interface overlays prepared.')
