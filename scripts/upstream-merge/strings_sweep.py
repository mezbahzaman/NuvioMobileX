#!/usr/bin/env python3
"""Merge-introduced Nuvio->Tuvora sweep on an already-merged strings.xml (in place).
usage: strings_sweep.py BASE MERGED [--skip-keys REGEX]
Rebrands 'Nuvio' in values of keys that are new vs BASE or whose BASE value had no 'Nuvio'."""
import re, sys

# Keys whose value legitimately names Nuvio (attribution / licence text) — never rebranded.
DEFAULT_SKIP = r'licenses_attributions|p2p_consent|library_local_tab_title|nuvio_title|settings_licenses_attributions|about_made_with'
ITEM_RE = re.compile(r'<(string|plurals|string-array)\s+name="([^"]+)"[^>]*?(?:/>|>.*?</\1>)', re.S)
args = [a for a in sys.argv[1:] if not a.startswith('--')]
skip = re.compile(DEFAULT_SKIP + ('|' + sys.argv[sys.argv.index('--skip-keys')+1] if '--skip-keys' in sys.argv else ''))
base_t = open(args[0], encoding='utf-8').read(); merged_t = open(args[1], encoding='utf-8').read()
base = {(m.group(1), m.group(2)): m.group(0) for m in ITEM_RE.finditer(base_t)}
fixed = []
def repl(m):
    key = (m.group(1), m.group(2)); raw = m.group(0)
    if 'Nuvio' not in raw: return raw
    if skip and skip.search(key[1]): return raw
    b = base.get(key)
    if b is not None and 'Nuvio' in b: return raw
    fixed.append(key[1]); return raw.replace('Nuvio', 'Tuvora')
out = ITEM_RE.sub(repl, merged_t)
if fixed:
    open(args[1], 'w', encoding='utf-8').write(out)
    print(f"  {args[1]}: fixed {len(fixed)}: {', '.join(fixed[:15])}{' …' if len(fixed)>15 else ''}")
