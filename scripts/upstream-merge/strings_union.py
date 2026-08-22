#!/usr/bin/env python3
"""3-way key-level merge for Android/Compose strings.xml.

usage: strings_union.py BASE OURS THEIRS OUT [--skip-keys REGEX]

Rules (per key = (tag,name) for <string>/<plurals>/<string-array>):
  - ordering follows THEIRS; fork-only keys (in OURS, not in THEIRS) are appended before </resources>
    unless upstream deliberately deleted them (present in BASE, absent in THEIRS, OURS==BASE).
  - collision, values differ:
      * OURS contains "Tuvora"            -> OURS (brand)
      * OURS != BASE and THEIRS == BASE   -> OURS (only the fork changed it)
      * OURS == BASE and THEIRS != BASE   -> THEIRS (only upstream changed it)
      * both changed                      -> THEIRS, then brand-sweep below
  - brand sweep: any resulting value containing "Nuvio" where the key is new or BASE had no "Nuvio"
    in that key (and key not in skip list) -> s/Nuvio/Tuvora/ (merge-introduced leak).
Prints a change report to stderr.
"""
import re, sys

# Keys whose value legitimately names Nuvio (attribution / licence text) — never rebranded.
DEFAULT_SKIP = r'licenses_attributions|p2p_consent|library_local_tab_title|nuvio_title|settings_licenses_attributions|about_made_with'

ITEM_RE = re.compile(r'<(string|plurals|string-array)\s+name="([^"]+)"[^>]*?(?:/>|>.*?</\1>)', re.S)

def parse(text):
    """Return (items, order): items[key] = full raw element text; order = list of keys in file order."""
    items, order = {}, []
    for m in ITEM_RE.finditer(text):
        key = (m.group(1), m.group(2))
        if key in items:   # duplicate key in a single file: keep first, warn
            print(f"  warn: duplicate {key[1]} in input, keeping first", file=sys.stderr)
            continue
        items[key] = m.group(0)
        order.append(key)
    return items, order

def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    skip = re.compile(DEFAULT_SKIP)
    if '--skip-keys' in sys.argv:
        skip = re.compile(DEFAULT_SKIP + '|' + sys.argv[sys.argv.index('--skip-keys') + 1])
    base_p, ours_p, theirs_p, out_p = args[:4]
    base_t, ours_t, theirs_t = (open(p, encoding='utf-8').read() for p in (base_p, ours_p, theirs_p))
    base, _ = parse(base_t); ours, ours_order = parse(ours_t); theirs, theirs_order = parse(theirs_t)

    report = {'ours_brand': [], 'ours_forkchange': [], 'theirs': [], 'fork_only_kept': [], 'upstream_deleted': [], 'leak_fixed': []}
    out_items = []
    for k in theirs_order:
        t = theirs[k]
        if k in ours and ours[k] != t:
            o = ours[k]; b = base.get(k)
            if 'Tuvora' in o:
                out_items.append(o); report['ours_brand'].append(k[1]); continue
            if b is not None and o != b and t == b:
                out_items.append(o); report['ours_forkchange'].append(k[1]); continue
            out_items.append(t); report['theirs'].append(k[1])
        else:
            out_items.append(t)
    # fork-only keys
    appended = []
    for k in ours_order:
        if k in theirs: continue
        if k in base and ours[k] == base[k]:
            report['upstream_deleted'].append(k[1]); continue
        appended.append(ours[k]); report['fork_only_kept'].append(k[1])

    # brand sweep
    def sweep(raw, key):
        if 'Nuvio' not in raw: return raw
        if skip and skip.search(key[1]): return raw
        b = base.get(key)
        if b is not None and 'Nuvio' in b: return raw   # pre-existing, legit legacy
        report['leak_fixed'].append(key[1])
        return raw.replace('Nuvio', 'Tuvora')
    # rebuild output: take THEIRS text as skeleton, replace each item body, then insert appended before </resources>
    pos = 0; out = []
    idx = 0
    for m in ITEM_RE.finditer(theirs_t):
        out.append(theirs_t[pos:m.start()])
        key = (m.group(1), m.group(2))
        raw = out_items[idx]; idx += 1
        out.append(sweep(raw, key))
        pos = m.end()
    tail = theirs_t[pos:]
    if appended:
        appended_text = ''.join('    ' + sweep(a, ITEM_RE.match(a) and (ITEM_RE.match(a).group(1), ITEM_RE.match(a).group(2))) + '\n' for a in appended)
        tail = tail.replace('</resources>', appended_text + '</resources>', 1)
    out.append(tail)
    result = ''.join(out)
    open(out_p, 'w', encoding='utf-8').write(result)
    # dup check
    _, ro = parse(result)
    dups = {k for k in ro if ro.count(k) > 1}
    for name, lst in report.items():
        if lst: print(f"  {name} ({len(lst)}): {', '.join(lst[:12])}{' …' if len(lst) > 12 else ''}", file=sys.stderr)
    if dups: print(f"  DUPLICATES: {dups}", file=sys.stderr); sys.exit(2)

if __name__ == '__main__':
    main()
