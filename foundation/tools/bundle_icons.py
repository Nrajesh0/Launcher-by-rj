#!/usr/bin/env python3
"""Build-time import of pinned, licensed icon resources; never downloads from the app."""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

project = Path(__file__).resolve().parents[2]
icons, target = (Path(p).resolve() for p in sys.argv[1:3])
commit = subprocess.check_output(['git', '-C', str(icons), 'rev-parse', 'HEAD'], text=True).strip()
assert commit == '560a96149914db67e9c68000e1b23efe3914f3bd', 'Unpinned icon source'
cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home()/'.gradle'))/'caches/modules-2/files-2.1'
jars = sorted(cache.rglob('*.jar'))
# Prefer the Android converter matching the pinned AGP. Other jars are its
# normal build-time dependencies; none are added to the installed app here.
sdk = [p for p in jars if p.name == 'sdk-common-32.0.1.jar']
assert len(sdk) == 1, 'Resolve the foundation Gradle toolchain before importing icons'
classpath = os.pathsep.join(str(p) for p in sdk + [p for p in jars if p not in sdk])
classes = target/'build/dot-icon-tools'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run(['javac','-proc:none','-cp',classpath,'-d',str(classes),str(project/'foundation/tools/ConvertIcons.java')],check=True)
drawable = target/'lawnchair/res/drawable'
subprocess.run(['java','-cp',str(classes)+os.pathsep+classpath,'ConvertIcons',str(icons/'svgs'),str(drawable)],check=True)
doc = ET.Element('icons')
packages = {}
count = 0
for entry in ET.parse(icons/'app/assets/appfilter.xml').getroot().findall('item'):
    key = entry.get('component','').removeprefix('ComponentInfo{').removesuffix('}')
    name = entry.get('drawable','')
    if '/' not in key or not (drawable/('dot_icon_'+name+'.xml')).is_file(): continue
    package, component = key.split('/',1)
    if component.startswith('.'): component = package+component
    ET.SubElement(doc,'icon',{'package':package,'component':component,'drawable':'@drawable/dot_icon_'+name})
    packages.setdefault(package,set()).add(name)
    count += 1
for package, names in packages.items():
    if len(names)==1: ET.SubElement(doc,'icon',{'package':package,'drawable':'@drawable/dot_icon_'+next(iter(names))})
ET.indent(doc)
xml = target/'lawnchair/res/xml/grayscale_icon_map.xml'
ET.ElementTree(doc).write(xml,encoding='utf-8',xml_declaration=True)
licenses = target/'lawnchair/assets/licenses'
licenses.mkdir(parents=True, exist_ok=True)
shutil.copy2(icons/'LICENSE',licenses/'Lawnicons-Apache-2.0.txt')
(licenses/'Lawnicons-NOTICE.txt').write_text('Monochrome icon resources from Lawnicons / Lawnchair contributors.\nSource https://github.com/LawnchairLauncher/lawnicons\nPinned revision '+commit+'\nConverted from SVG to Android vector resources using Android build tools; app/component mappings retained.\n')
print(f'Bundled {count} exact component mappings and {len(packages)} package identities.')
