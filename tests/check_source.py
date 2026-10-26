"""Build input checks complement the final APK manifest/DEX checks."""
from pathlib import Path
import ast
root=Path(__file__).resolve().parents[1]
for path in (root/'foundation/patches').glob('*.py'):ast.parse(path.read_text())
for path in (root/'foundation/overlay').rglob('*'):
    if path.suffix in ('.java','.kt'):
        value=path.read_text()
        for banned in ('import okhttp','import retrofit','import java.net.','import android.webkit.'):
            assert banned not in value,(path,banned)
assert 'scripts/build_foundation.sh' in (root/'build.sh').read_text()
assert 'assembleRelease' not in (root/'build.sh').read_text()
print('Foundation sources and release entry point checked.')
