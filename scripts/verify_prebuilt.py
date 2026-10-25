"""Verify a public, locally signed APK before uploading it from the GitHub runner."""
import hashlib, json, os, re, subprocess
from pathlib import Path

root=Path(__file__).resolve().parents[1]
version=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line)['versionName']
assert re.fullmatch(r'\d+\.\d+\.\d+',version)
folder=root/'release-assets'/f'v{version}'
apk=folder/f'dot-home-{version}.apk'
expected=(folder/f'dot-home-{version}.apk.sha256').read_text().split()[0]
assert hashlib.sha256(apk.read_bytes()).hexdigest()==expected, 'APK checksum mismatch'
provenance=json.loads((folder/'provenance.json').read_text())
sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/workspace/android-sdk')))
tools=sdk/'build-tools'/'36.0.0'
if not tools.exists(): tools=sdk/'build-tools'/'android-16'
result=subprocess.check_output([str(tools/'apksigner'),'verify','--print-certs',str(apk)],text=True)
assert provenance['certificate_sha256'] in result, 'Signing identity mismatch'
subprocess.run(['python3',str(root/'tests/check_offline.py'),str(tools/'aapt'),str(apk)],check=True)
tag_sha=subprocess.check_output(['git','rev-list','-n','1',f'v{version}'],cwd=root,text=True).strip()
assert tag_sha==provenance['source_commit'], 'Source tag mismatch'
run=json.loads(subprocess.check_output(['gh','api',f'repos/{os.environ["GH_REPO"]}/actions/runs/{provenance["actions_run_id"]}'],text=True))
assert run['head_sha']==tag_sha and run['conclusion']=='success', 'Source build did not pass CI'
print('Signed offline APK checksum, identity, source tag and passing CI verified')
