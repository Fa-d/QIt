#!/usr/bin/env python3
"""Prints, for each verse file of a split surah, the words recognised in it next to a reference
translation, to review a split by eye.  usage: review_bangla_split.py toha|baezeed SURAH"""
import json, os, sys

ROOT = "/Users/faddy/MyLab/AndroidAllProjects/QIt/quran_audio/bangla"
voice, surah = sys.argv[1], int(sys.argv[2])
work = f"{ROOT}/.work-{voice}"
refs = json.load(open(f"{ROOT}/.ref/refs.json", encoding="utf-8"))
words = [w for chunk in json.load(open(f"{work}/{surah:03d}.words.json", encoding="utf-8")).values() for w in chunk]
rows = [r.split("\t") for r in open(f"{work}/{surah:03d}.tsv").read().strip().split("\n")[1:]]
held = []
for r in rows:
    g, s, e, flags = int(r[0]), float(r[5]), float(r[6]), r[8] if len(r) > 8 else ""
    held.append(g)
    if "shared:" in flags:
        continue
    said = " ".join(w for w, a, b in words if s <= (a + b) / 2 < e)
    print(f"== {'+'.join(str(h - held[0] + int(rows[0][2])) for h in held)}  [{s:.1f}-{e:.1f}]  {flags}")
    print(f"   heard: {said}")
    for h in held:
        print(f"   ref {h}: {refs[str(h)][0]}")
    held = []
