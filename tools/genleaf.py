# Generates res/drawable/ic_mint.xml (and build/leaf.svg for previewing). Run from the repo root:
#   python3 tools/genleaf.py
# SPDX-License-Identifier: GPL-3.0-or-later
import math
# Leaf in a 48x48 viewport, drawn vertically, then rotated 40deg about centre.
tip, base = 8.5, 36.5            # y of tip and base
W = 9.6                         # max half-width
N = 7                            # serrations per side
def halfwidth(t):                # t: 0 at tip -> 1 at base
    return W * (math.sin(math.pi * (t ** 1.35)) ** 0.9) * (1 - 0.10 * t)
def side(sign):
    pts = []
    steps = N * 2
    for i in range(steps + 1):
        t = i / steps
        y = tip + (base - tip) * t
        w = halfwidth(t)
        # serration: every odd point juts outward and slightly toward tip
        if 0 < i < steps and i % 2 == 1:
            w += 1.25 * math.sin(math.pi * t) ** 0.5
            y -= 0.9
        pts.append((24 + sign * w, y))
    return pts
r = side(+1); l = side(-1)[::-1]
d = "M24,%.2f " % tip + " ".join("L%.2f,%.2f" % p for p in r[1:-1])
d += " Q25.2,%.2f 24,%.2f Q22.8,%.2f %.2f,%.2f " % (base - 0.4, base + 0.3, base - 0.4, l[1][0], l[1][1])
d += " ".join("L%.2f,%.2f" % p for p in l[2:-1]) + " Z"
leaf = d
midrib = "M24,11.5 L24,38.8"
veins = []
for t in (0.28, 0.46, 0.64):
    y0 = tip + (base - tip) * t
    w = halfwidth(t) * 0.72
    for s in (+1, -1):
        veins.append("M24,%.2f Q%.2f,%.2f %.2f,%.2f" % (y0 + 2.2, 24 + s*w*0.5, y0 + 0.6, 24 + s*w, y0 - 1.6))
stem = "M24,36 L24,41.2"
BG, LEAF, LINE = "#1E6B4E", "#D9F7E7", "#1E6B4E"
vec = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="48dp"
    android:viewportWidth="48" android:viewportHeight="48">
  <path android:fillColor="{BG}" android:pathData="M24,0 A24,24 0 1,1 23.99,0 Z"/>
  <group android:translateX="-1.6" android:translateY="1.2"><group android:rotation="40" android:pivotX="24" android:pivotY="24">
    <path android:strokeColor="{LEAF}" android:strokeWidth="1.8" android:strokeLineCap="round" android:pathData="{stem}"/>
    <path android:fillColor="{LEAF}" android:pathData="{leaf}"/>
    <path android:strokeColor="{LINE}" android:strokeWidth="1.3" android:strokeLineCap="round" android:pathData="{midrib}"/>
    <path android:strokeColor="{LINE}" android:strokeWidth="1.0" android:strokeLineCap="round" android:pathData="{' '.join(veins)}"/>
  </group></group>
</vector>
'''
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="384" height="384">
  <circle cx="24" cy="24" r="24" fill="{BG}"/>
  <g transform="translate(-1.6 1.2)"><g transform="rotate(40 24 24)">
    <path d="{stem}" stroke="{LEAF}" stroke-width="1.8" stroke-linecap="round" fill="none"/>
    <path d="{leaf}" fill="{LEAF}"/>
    <path d="{midrib}" stroke="{LINE}" stroke-width="1.3" stroke-linecap="round" fill="none"/>
    <path d="{' '.join(veins)}" stroke="{LINE}" stroke-width="1.0" stroke-linecap="round" fill="none"/>
  </g></g>
</svg>'''
import os; os.makedirs("res/drawable", exist_ok=True)
open("res/drawable/ic_mint.xml","w").write(vec); os.makedirs("build", exist_ok=True); open("build/leaf.svg","w").write(svg)
