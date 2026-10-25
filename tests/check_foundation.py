#!/usr/bin/env python3
"""Packaged privacy/navigation gates for the new, separately prepared launcher engine."""
import subprocess
import sys
import zipfile

aapt, apk = sys.argv[1:3]
permissions = subprocess.check_output([aapt,'dump','permissions',apk],text=True)
for name in ('INTERNET','ACCESS_NETWORK_STATE','CHANGE_NETWORK_STATE','WRITE_SECURE_SETTINGS','SYSTEM_ALERT_WINDOW','INTERACT_ACROSS_USERS_FULL','READ_FRAME_BUFFER'):
    assert 'android.permission.'+name not in permissions, 'Unexpected packaged permission: '+name
manifest = subprocess.check_output([aapt,'dump','xmltree',apk,'AndroidManifest.xml'],text=True)
for component in ('BIND_ACCESSIBILITY_SERVICE','LawnchairAccessibilityService','UploaderService','TouchInteractionService'):
    assert component not in manifest, 'Unexpected packaged component: '+component
assert 'dev.dothome.HomeActivity' in manifest, 'Installed Home component changed'
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if name.endswith('.dex'):
            data=archive.read(name)
            for forbidden in (b'Lokhttp3/',b'Lretrofit2/',b'Lcoil/',b'Lapp/lawnchair/bugreport/'):
                assert forbidden not in data, f'Unexpected network library: {forbidden!r}'
print('Packaged foundation: no Internet/network libraries, Accessibility or navigation override permissions.')
