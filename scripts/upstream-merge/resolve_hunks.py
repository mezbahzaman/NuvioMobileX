#!/usr/bin/env python3
"""resolve_hunks.py FILE CHOICE... — resolve conflict hunks in order. CHOICE: ours|theirs|both|both-rev|@file(replacement text)"""
import sys, re
path = sys.argv[1]; choices = sys.argv[2:]
lines = open(path, encoding='utf-8').read().split('\n')
out = []; i = 0; h = 0
while i < len(lines):
    if lines[i].startswith('<<<<<<< '):
        j = i + 1; ours = []
        while not lines[j].startswith('======='): ours.append(lines[j]); j += 1
        j += 1; theirs = []
        while not lines[j].startswith('>>>>>>> '): theirs.append(lines[j]); j += 1
        c = choices[h] if h < len(choices) else 'ours'; h += 1
        if c == 'ours': out += ours
        elif c == 'theirs': out += theirs
        elif c == 'both': out += ours + theirs
        elif c == 'both-rev': out += theirs + ours
        elif c.startswith('@'): out += open(c[1:], encoding='utf-8').read().rstrip('\n').split('\n')
        else: raise SystemExit(f'bad choice {c}')
        i = j + 1
    else:
        out.append(lines[i]); i += 1
open(path, 'w', encoding='utf-8').write('\n'.join(out))
print(f'  {path}: resolved {h} hunks ({", ".join(choices[:h])})')
if h != len(choices): print(f'  WARN: {len(choices)} choices given, {h} hunks found', file=sys.stderr)
