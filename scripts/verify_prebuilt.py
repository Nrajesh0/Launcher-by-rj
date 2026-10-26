"""Stable publication has the same source, signature and payload gates."""
import runpy
from pathlib import Path
runpy.run_path(str(Path(__file__).with_name("verify_preview.py")),run_name="__main__")
