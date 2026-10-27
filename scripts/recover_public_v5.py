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
 # apksigner can add local alignment padding; derive offsets from the
 # already uploaded public signed records rather than guessing the padding.
 with zipfile.ZipFile(unsigned) as z:infos={i.filename:i for i in z.infolist()}
 offsets={};pos=0
 while True:
  pos=prefix.find(b'PK\x03\x04',pos,start)
  if pos<0:break
  h=struct.unpack_from('<4s5H3I2H',prefix,pos)
  try:name=prefix[pos+30:pos+30+h[9]].decode('utf-8')
  except UnicodeDecodeError:name=None
  if name in infos:
   info=infos[name]
   if h[3]==info.compress_type and h[7] in (0,info.compress_size):
    assert name not in offsets,('Duplicate public record',name)
    offsets[name]=pos
  pos+=4
 assert set(offsets)==set(infos),('Missing public local records',len(set(infos)-set(offsets)))
 directory=bytearray(data[central:eocd]);pos=0
 while pos<len(directory):
  h=struct.unpack_from('<4s6H3I5H2I',directory,pos)
  assert h[0]==b'PK\x01\x02'
  name=directory[pos+46:pos+46+h[10]].decode('utf-8')
  struct.pack_into('<I',directory,pos+42,offsets[name])
  pos+=46+h[10]+h[11]+h[12]
 assert pos==len(directory)
 end=bytearray(data[eocd:])
 struct.pack_into('<I',end,16,signed_central)
 candidate=prefix[:signed_central]+directory+end
 if candidate[:len(prefix)]!=prefix:
  first=next(i for i,(a,b) in enumerate(zip(candidate,prefix)) if a!=b)
  print('Public directory first difference',first,'relative',first-signed_central,'aligned central',central,'signed central',signed_central)
  print('Actual public bytes',prefix[max(0,first-24):first+40].hex())
  print('Rebuilt public bytes',candidate[max(0,first-24):first+40].hex())
  for label,payload,offset in [('actual',prefix,signed_central),('aligned',data,central)]:
   for _ in range(3):
    h=struct.unpack_from('<4s6H3I5H2I',payload,offset)
    size=46+h[10]+h[11]+h[12]
    print(label,'central entry',h,'name',payload[offset+46:offset+46+h[10]])
    offset+=size
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
