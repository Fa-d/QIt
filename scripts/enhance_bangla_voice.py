#!/usr/bin/env python3
"""Restores the narrow-band Bangla voices (Toha, Baezeed) with VoiceFixer: their recordings carry
nothing above about 4 kHz, like a telephone line, and VoiceFixer (denoising plus bandwidth
extension to 44.1 kHz) rebuilds the missing high end. Each verse file of --src is written, enhanced,
under the same name in --out. Resumable: files already in --out are skipped.

Runs in the Python 3.12 venv that has torch (~/.cache/whisper-cpp/conv) with `voicefixer` installed:
  ~/.cache/whisper-cpp/conv/bin/python scripts/enhance_bangla_voice.py --src DIR --out DIR
"""
import argparse, glob, os, subprocess, sys, tempfile, time

from voicefixer import VoiceFixer

BITRATE = "128k"  # mono 44.1 kHz after restoration


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True, help="folder of verse mp3s (00001.mp3 ...)")
    ap.add_argument("--out", required=True, help="folder for the enhanced mp3s")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    todo = [f for f in sorted(glob.glob(f"{a.src}/*.mp3")) if not os.path.exists(f"{a.out}/{os.path.basename(f)}")]
    print(f"{len(todo)} file(s) to enhance", flush=True)
    vf = VoiceFixer()
    start = time.time()
    with tempfile.TemporaryDirectory() as td:
        for i, src in enumerate(todo, 1):
            name = os.path.basename(src)
            wav = os.path.join(td, "e.wav")
            trim = []
            try:
                vf.restore(input=src, output=wav, cuda=False, mode=0)
            except RuntimeError:
                # VoiceFixer works in 30 s segments and fails on a last one of a few ms (a 30.005 s verse):
                # pad a second of silence, then trim the result back to the verse's length
                padded = os.path.join(td, "p.wav")
                subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", src, "-af", "apad=pad_dur=1", padded], check=True)
                vf.restore(input=padded, output=wav, cuda=False, mode=0)
                length = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0",
                                         src], check=True, capture_output=True, text=True).stdout.strip()
                trim = ["-t", length]
            part = f"{a.out}/{name}.part"
            subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", wav, *trim, "-ac", "1", "-c:a", "libmp3lame",
                            "-b:a", BITRATE, "-f", "mp3", part], check=True)
            os.replace(part, f"{a.out}/{name}")
            if i % 50 == 0 or i == len(todo):
                rate = (time.time() - start) / i
                print(f"{i}/{len(todo)}  ~{rate * (len(todo) - i) / 3600:.1f} h left", flush=True)


if __name__ == "__main__":
    sys.exit(main())
