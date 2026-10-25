"""Runs over an unauthenticated local test emulator; never targets physical devices."""
from adb_shell.adb_device import AdbDeviceTcp
from pathlib import Path
import time
out=Path(__file__).resolve().parents[1]/'build'
version=dict(line.split('=',1) for line in (out.parent/'version.properties').read_text().splitlines() if '=' in line)['versionName']
d=AdbDeviceTcp('127.0.0.1',5555,default_transport_timeout_s=30)
d.connect(auth_timeout_s=15)
deadline=time.monotonic()+240
while time.monotonic()<deadline:
    status=d.shell('getprop sys.boot_completed',timeout_s=30)
    if status.strip()=='1':break
    time.sleep(5)
else:raise RuntimeError('Android services did not become ready')
print('Android services ready',flush=True)
d.push(str(out/f'dot-home-{version}.apk'),'/data/local/tmp/dot-home.apk',read_timeout_s=60)
result=d.shell('pm install -r /data/local/tmp/dot-home.apk',timeout_s=180);print(result,flush=True);assert 'Success' in result
print(d.shell('am start -W -n dev.dothome/.HomeActivity',timeout_s=120),flush=True)
time.sleep(8)
log=d.shell('logcat -d -s AndroidRuntime',timeout_s=30)
(out/'runtime-log.txt').write_text(log)
print(log,flush=True)
d.shell('input keyevent 82; screencap -p /data/local/tmp/dot-home.png',timeout_s=30)
d.pull('/data/local/tmp/dot-home.png',str(out/'home-screen.png'),read_timeout_s=60)
print('Screenshot saved',flush=True)
