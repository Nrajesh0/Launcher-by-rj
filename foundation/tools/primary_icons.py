"""Original compact monochrome artwork; keep upstream component identities intact."""
from pathlib import Path
from xml.sax.saxutils import escape
import sys

target=Path(sys.argv[1])/'lawnchair/res/drawable'
# Path holes use evenOdd fill, so negative space survives theme inversion.
art={
'google_phone':'M5,2 C4,2 2,4 2,6 C2,13 11,22 18,22 C20,22 22,20 22,19 L22,17 C22,16 21,16 18,15 C17,15 16,16 15,17 C11,15 9,13 7,9 L9,7 C10,6 9,5 8,3 Z',
'google_messages':'M5,3 H19 A3,3 0,0 1,22,6 V14 A3,3 0,0 1,19,17 H11 L5,22 V17 A3,3 0,0 1,2,14 V6 A3,3 0,0 1,5,3 Z',
'generic_camera':'M4,5 H7 L8.5,3 H15.5 L17,5 H20 A2,2 0,0 1,22,7 V19 H2 V7 A2,2 0,0 1,4,5 Z M12,7 A5,5 0,1 0,12,17 A5,5 0,1 0,12,7 Z M18,7 H20 V9 H18 Z',
'generic_gallery':'M3,2 H21 V22 H3 Z M5,4 V16 L10,10 L14,15 L17,12 L19,15 V4 Z M15,5 A2,2 0,1 0,15,9 A2,2 0,1 0,15,5 Z',
'google_contacts':'M12,2 A5,5 0,1 0,12,12 A5,5 0,1 0,12,2 Z M3,22 V20 C3,11 21,11 21,20 V22 Z',
'generic_files':'M2,4 H10 L12,7 H22 V20 A2,2 0,0 1,20,22 H4 A2,2 0,0 1,2,20 Z',
'files_by_google':'M2,4 H10 L12,7 H22 V20 A2,2 0,0 1,20,22 H4 A2,2 0,0 1,2,20 Z',
'generic_clock':'M12,1 A11,11 0,1 0,12,23 A11,11 0,1 0,12,1 Z M11,5 H13 V11 L17,14 L15.8,15.6 L11,12 Z',
'generic_settings':'M9,1 H15 L16,5 L19,4 L23,9 L20,12 L23,15 L19,20 L16,19 L15,23 H9 L8,19 L5,20 L1,15 L4,12 L1,9 L5,4 L8,5 Z M12,7 A5,5 0,1 0,12,17 A5,5 0,1 0,12,7 Z',
'google_chrome':'M12,1 A11,11 0,0 1,21.6,6 H12 A6,6 0,0 0,6.8,9 Z M22.5,8 A11,11 0,0 1,13,23 L17.5,15 A6,6 0,0 0,17,8 Z M10.5,23 A11,11 0,0 1,2,6 L6.5,14 A6,6 0,0 0,14.5,17.5 Z M12,8 A4,4 0,1 0,12,16 A4,4 0,1 0,12,8 Z',
'generic_browser':'M12,1 A11,11 0,1 0,12,23 A11,11 0,1 0,12,1 Z M4,11 H8 C8,8 9,5 10,3 C6,4 4,7 4,11 Z M14,3 C16,6 16,8 16,11 H20 C20,7 18,4 14,3 Z M10,11 H14 C14,8 13,5 12,3 C11,5 10,8 10,11 Z M4,13 C4,17 6,20 10,21 C9,18 8,16 8,13 Z M10,13 C10,17 11,19 12,21 C13,19 14,17 14,13 Z M16,13 C16,16 15,19 14,21 C18,20 20,17 20,13 Z',
'google_photos':'M12,12 V2 A5,5 0,0 1,17,7 V12 Z M12,12 H22 A5,5 0,0 1,17,17 H12 Z M12,12 V22 A5,5 0,0 1,7,17 V12 Z M12,12 H2 A5,5 0,0 1,7,7 H12 Z',
'generic_calculator':'M4,1 H20 V23 H4 Z M7,4 V9 H17 V4 Z M7,12 V15 H10 V12 Z M14,12 V15 H17 V12 Z M7,18 V21 H10 V18 Z M14,18 V21 H17 V18 Z',
'generic_calendar':'M4,3 H6 V1 H8 V3 H16 V1 H18 V3 H20 A2,2 0,0 1,22,5 V21 H2 V5 A2,2 0,0 1,4,3 Z M4,8 V19 H20 V8 Z M7,11 H11 V15 H7 Z M14,11 H17 V15 H14 Z',
'generic_music':'M8,3 L21,1 V16 A4,4 0,1 1,18,12 V5 L11,6 V19 A4,4 0,1 1,8,15 Z',
}
aliases={'generic_phone':'google_phone','generic_messaging':'google_messages','generic_contacts':'google_contacts','google_clock':'generic_clock','google_camera':'generic_camera','generic_photos':'generic_gallery','settings':'generic_settings'}
for name,parent in aliases.items():art[name]=art[parent]
for name,path in art.items():
 (target/f'dot_icon_{name}.xml').write_text('<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#FF000000" android:fillType="evenOdd" android:pathData="'+escape(path)+'"/></vector>\n')
print(f'Applied {len(art)} original primary glyphs; existing icon mappings preserved.')
