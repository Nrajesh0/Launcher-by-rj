"""Recover only the known public signed ZIP tail; never sign or read private keys."""
from pathlib import Path
import hashlib,json,os,struct,subprocess,tempfile,zipfile
ROOT=Path(__file__).resolve().parents[1]
folder=ROOT/'release-assets/v5.0.0'
expected='79efbaf9dd9471558676414da25ac4a2aa079e030fb9da558099a46d36f67e85'
provenance=json.loads((folder/'provenance.json').read_text())
assert provenance['source_commit']=='d3b5f27e77bdae86b3e435ee506004d45ab412e3'
parts=provenance['apk_parts']
assert len(parts)==67
prefix=b''
for i,p in enumerate(parts[:66]):
 assert p['name']==f'dot-home-5.0.0.apk.part{i:03d}'
 data=(folder/p['name']).read_bytes()
 assert len(data)==524288 and hashlib.sha256(data).hexdigest()==p['sha256']
 prefix+=data
with tempfile.TemporaryDirectory() as td:
 tmp=Path(td)
 subprocess.run(['gh','run','download','40563188991','--repo',os.environ['GH_REPO'],'--name','dot-home-5.0.0','--dir',str(tmp/'ci')],check=True)
 unsigned=tmp/'ci/dot-home-5.0.0-unsigned.apk'
 assert hashlib.sha256(unsigned.read_bytes()).hexdigest()=='15d74121afd9f33cf5772dfcc1d4fa417042f7401f8dea5ad7e6f72c022ee94d'
 bt=Path(os.environ['ANDROID_HOME'])/'build-tools/36.1.0'
 aligned=tmp/'aligned.apk'
 subprocess.run([str(bt/'zipalign'),'-f','-p','4',str(unsigned),str(aligned)],check=True)
 data=aligned.read_bytes()
 eocd=data.rfind(b'PK\x05\x06')
 assert eocd>=0 and eocd+22+struct.unpack_from('<H',data,eocd+20)[0]==len(data)
 central=struct.unpack_from('<I',data,eocd+16)[0]
 assert data[central:central+4]==b'PK\x01\x02'
 magic=prefix.rfind(b'APK Sig Block 42')
 assert magic>=0
 signed_central=magic+16
 size=struct.unpack_from('<Q',prefix,magic-8)[0]
 start=signed_central-size-8
 assert struct.unpack_from('<Q',prefix,start)[0]==size
 assert prefix[signed_central:signed_central+4]==b'PK\x01\x02'
 end=bytearray(data[eocd:])
 struct.pack_into('<I',end,16,signed_central)
 candidate=prefix[:signed_central]+data[central:eocd]+end
 assert candidate[:len(prefix)]==prefix,'Reconstructed public directory differs from uploaded prefix'
 assert len(candidate)==34946740
 assert hashlib.sha256(candidate).hexdigest()==expected,'Must exactly equal previously locally signed artifact'
 tail=candidate[len(prefix):]
 assert len(tail)==343732 and hashlib.sha256(tail).hexdigest()==parts[-1]['sha256']
 assert hashlib.sha1(f'blob {len(tail)}\0'.encode()+tail).hexdigest()=='0d4d200ef407687343a3a2e9d2cdc4a81a4cd870'
 apk=tmp/'dot-home-5.0.0.apk';apk.write_bytes(candidate)
 certificate=subprocess.check_output([str(bt/'apksigner'),'verify','--print-certs',str(apk)],text=True)
 assert provenance['certificate_sha256'] in certificate
 with zipfile.ZipFile(unsigned) as original,zipfile.ZipFile(apk) as signed:
  assert set(original.namelist())==set(signed.namelist())
  for name in original.namelist():assert original.read(name)==signed.read(name),name
 subprocess.run(['python3',str(ROOT/'tests/check_foundation.py'),str(bt/'aapt'),str(apk)],check=True)
 (folder/parts[-1]['name']).write_bytes(tail)
 print('Recovered exact original public signed bytes, certificate and CI payload verified.')
 # Publish only the known synthetic app screenshots, never emulator files.
 subprocess.run(['gh','run','download','40563188991','--repo',os.environ['GH_REPO'],'--name','foundation-evidence','--dir',str(tmp/'evidence')],check=True)
 from PIL import Image,ImageDraw,ImageFont
 names=['v5-home-classic-light','v5-home-classic-dark','v5-home-lucent-light','v5-home-lucent-dark','v5-theme-lucent-dark','foundation-home-menu','v5-categories','v5-grid-choices','foundation-widget-tray','v5-weather-editor','foundation-expanded-folder','foundation-group-dragged','v5-offline-calculator','v5-selected-document-search','v5-large-text-theme','v5-denied-history']
 sheet=Image.new('RGB',(1000,2060),'#eeeeee');draw=ImageDraw.Draw(sheet)
 font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',11)
 for i,name in enumerate(names):
  matches=list((tmp/'evidence').rglob(name+'.png'))
  assert len(matches)==1,(name,len(matches))
  img=Image.open(matches[0]).convert('RGB');img.thumbnail((232,478))
  x=(i%4)*250;y=(i//4)*515
  draw.text((x+6,y+5),name.removeprefix('v5-').removeprefix('foundation-'),fill='#111111',font=font)
  sheet.paste(img,(x+(250-img.width)//2,y+25))
 out=ROOT/'docs/reference/v5-actual-review.jpg';out.parent.mkdir(parents=True,exist_ok=True);sheet.save(out,quality=88)
 print('Created public review sheet from the 16 explicitly selected synthetic captures.')
