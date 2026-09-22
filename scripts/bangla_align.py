#!/usr/bin/env python3
"""Word times for a known Bangla transcript: CTC forced alignment with torchaudio's MMS_FA model
(multilingual, over uroman romanization). The Bangla speech recognizer gives accurate text but no
timestamps; this places each of its words in the audio.

Runs in the conversion venv (Python 3.12 with torch, torchaudio, uroman), as a server reading JSON
lines on stdin: {"wav": path, "words": [...]} -> {"times": [[start_s, end_s] | null, ...]}.
"""
import json, sys

import soundfile
import torch, torchaudio
import torchaudio.functional as F
import uroman as ur

BUNDLE = torchaudio.pipelines.MMS_FA
DEVICE = "mps" if torch.backends.mps.is_available() else "cpu"
MODEL = BUNDLE.get_model(with_star=True).to(DEVICE).eval()
DICT = BUNDLE.get_dict(star="*")
UROMAN = ur.Uroman()


def romanize(word):
    text = UROMAN.romanize_string(word).lower()
    return "".join(c for c in text if c in DICT and c != "*")


def align(wav_path, words):
    data, sr = soundfile.read(wav_path, dtype="float32", always_2d=True)
    wave = torch.from_numpy(data.mean(axis=1)).unsqueeze(0)
    if sr != BUNDLE.sample_rate:
        wave = torchaudio.functional.resample(wave, sr, BUNDLE.sample_rate)
    roman = [romanize(w) for w in words]
    keep = [i for i, r in enumerate(roman) if r]
    if not keep:
        return [None] * len(words)
    with torch.inference_mode():
        emission, _ = MODEL(wave.to(DEVICE))
    emission = emission.cpu()
    tokens = [DICT[c] for i in keep for c in roman[i]]
    targets = torch.tensor([tokens], dtype=torch.int32)
    aligned, scores = F.forced_align(emission, targets, blank=0)
    spans = F.merge_tokens(aligned[0], scores[0].exp())
    ratio = wave.shape[1] / emission.shape[1] / BUNDLE.sample_rate
    out, pos = [None] * len(words), 0
    for i in keep:
        n = len(roman[i])
        chunk = spans[pos:pos + n]
        pos += n
        out[i] = [chunk[0].start * ratio, chunk[-1].end * ratio]
    return out


if __name__ == "__main__":
    for line in sys.stdin:
        req = json.loads(line)
        try:
            print(json.dumps({"times": align(req["wav"], req["words"])}), flush=True)
        except Exception as e:  # a bad chunk must not kill the server
            print(json.dumps({"times": None, "error": str(e)}), flush=True)
