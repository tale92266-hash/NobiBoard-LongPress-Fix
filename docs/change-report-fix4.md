# NobiBoard — Simple Animated Fix4 — change report

**Code 176071840** (from 176071839) · package `com.nobi.nobiboard` · built 2026-11-12 ~09:07 UTC

## What Sam reported (176071839 works, one issue left)

Glide is smooth now ("WORKED"). Only the green highlight circle (BG) is
**not accurately aligned** on the emoji: from Sam's screenshot the circle
sits ~5dp RIGHT and ~3dp DOWN from the emoji it should be centered on
(circle center (610, 727.5) vs flower emoji (595, 724); emojis themselves
are well aligned — UP/CENTER/DOWN share one column, LEFT/RIGHT symmetric).

## Root cause — PROVEN from the shipped 176071839 dex + decoded res

Both places that position the indicator set its translation to
`(col * cellW, row * cellH)` — the **top-left corner** of the target cell:
`Lqmu.o()` (direct set) and `Lqmu.i()`'s AnimatorSet end values.
But the indicator view is LARGER than the cell:

- Cell: 28×28dp (`Lqmf.c/d` via `Lqmr.b()/a()` — the same values the
  translation already uses as its step).
- Indicator (`Lqmu.h`, style `0x7f15063d`/`63e`): 40×40dp
  (`?0x7f04029e`), or 34–46dp by theme/layout variant. Its background
  (`@drawable/0x7f080315` / `0x7f080316`, both solid `#ff4db6ac`) fills
  the view, so the green circle's diameter = the view's size.

Top-left anchoring therefore puts the indicator's CENTER
`(indicator − cell)/2` to the RIGHT and DOWN of the cell's center —
exactly the direction Sam measured. (X and Y need not match: the
indicator/cell need not be square in every theme/variant. The fix reads
each axis separately at runtime, so the X-vs-Y difference needs no
static resolution — the runtime sizes encode the answer.)

## Fix — center the indicator on the cell, by construction

Add the centering correction `(cell − indicator)/2` to BOTH axes,
computed at RUNTIME from the real sizes (correct for every
theme/variant and every position — NOT a hardcoded nudge):

```
translationX = col*cellW + (cellW - indicatorW)/2
translationY = row*cellH + (cellH - indicatorH)/2
```

`indicatorW/H` come from the indicator view's **LayoutParams**
(available immediately after inflation — unlike `getWidth()`/
`getHeight()`, which are 0 before layout at open); `cellW/H` from
`Lqmr.b()/a()` (the same values the translation uses). Proof of
centering: indicator center = translation + indicator/2
= col*cellW + (cellW−indicatorW)/2 + indicatorW/2 = col*cellW + cellW/2
= the cell's center. Same for Y. The emoji glyph is centered in its
cell (`gravity=center`), so centering on the cell centers on the glyph.

Applied in BOTH places that position the indicator (fixing only one
would leave the resting position wrong):

1. **`Lqmu.o(Lqmc;)V`** — the direct set (at-open placement via
   `c()->p()`, and the per-move refresh via `k()->n()`). Replaced
   wholesale: stock (25 insns, 6 regs) → centered (37 insns, 72 units,
   **8 regs**: this v4→v6, param v5→v7, scratch v4/v5). The stock
   input was verified identical to the 834 stock `o()` before replacing.
2. **`Lqmu.i(II)V`** — the stock 300ms AnimatorSet's end values (the
   resting position after every swipe). 21 insns / 38 units inserted
   before insn 297 (abs `0x21a`, the translation AnimatorSet's
   `new-instance`); the correction is added to BOTH the old and the
   new X/Y so the glide starts and ends centered. The **5 branches**
   crossing the insertion point were retargeted (+38): idx 204/205/207
   (`if-eqz`/`if-lez` → `0x29c`), idx 219 (`goto/16` → `0x276`),
   idx 277 (`if-eqz` → `0x258`). 38 is EVEN, so no payload shift.
   Scratch is low regs v11/v13/v14 only (field access 22c and
   arithmetic 12x/23x/22b are 4-bit — high regs v16+ cannot be used
   as operands; v11 is unused after `0x21a`, v13/v14 are overwritten
   before being read).

NOT touched: the glide (stock 300ms AnimatorSet), the scale-up, the
tracker, the PLUS layout, the circle background, caps, icons, emoji
row, CrashFix, Q–P numbers flag, the 26-key emoji sets, all three
VerifyError fixes.

New refs: **ZERO**. `View.getLayoutParams()` already exists
(method@1e2b); `ViewGroup$LayoutParams.width/height` were already in
the field table (just never referenced by an instruction in 839).
method_ids stays 65536, field_ids stays 45404. `getWidth()`/
`getHeight()` are NOT used (not referenced, and 0 before layout).

Tools (new, `tools/src/`):
- `Fix4CenterPatch.java` — `newO()` (37 insns, IF_EQZ offset computed
  from the built list = 69), `patchO()` (asserts stock input),
  `centerBlockI()` (21 insns, 38 units), `patchI()` (asserts 383
  insns / 27 regs / boundary insns 296–298 / abs 0x21a, no try blocks,
  no switch payloads; generic branch fixup, expects exactly 5
  crossing branches).
- `Fix4CombinedPatch.java` — driver (ONE dexlib2 pass over the 839
  classes2.dex); asserts exactly 1 `o` and 1 `i` replaced.
- `DexVersionBumpFix4.java` — classes5 version string
  176071839 → 176071840 (3 use-sites).

## Verification (all on the FINAL signed APK)

| Check | Result |
|---|---|
| File | `outputs/NobiBoard-LongPress-SimpleAnimatedFix4.apk`, 105,597,077 B |
| SHA-256 | `06d359e72ac1b6eec3ba1f2477dc4eae0a7213bee2fd376981ae1c6f0a44a28b` |
| versionCode | 176071840 (aapt2 badging), package `com.nobi.nobiboard` |
| Cert SHA-256 | `80:A9:47:FF:8B:5D:A7:4F:96:4A:F4:2C:A0:B9:6A:6C:EB:D5:48:68:AD:61:86:9D:6F:4F:88:05:86:B6:DA:7E` — v2 MATCH |
| Entry count | 9,465 |
| .so 16 KB alignment | 14 .so, 0 misaligned (zipalign `-c -P 16 -v 4`: verification successful; direct offset check) |
| Unsigned-APK check (before signing) | classes2.dex md5 `abeb996f…`, classes5.dex md5 `c06b6feb…` — matches the patched dexes, no stale-unsigned trap |
| CanonDiff vs 176071839 | **diff=2 methods: `Lqmu;->o[Lqmc;]V` and `Lqmu;->i[I, I]V` only** (47,276 methods each) |
| RegInitCheck (fixed) | `Lqmu.o` 0, `Lqmu.i` 0, `Lqmu.c` 0, `Lqml.k` 0, `Lqml.e` 0 violations |
| VerifyPatch (`Lqml`/`Lqmu`/`Lqck`/`Lqmr`) | 94 methods, 746 invokes, 0 violations |
| VerifyBranches | 120,969 checked, 0 bad |
| Payload alignment | 1,105 checked, 0 odd |
| Res diff vs 176071839 | `res/` identical (9,296 entries, 0 changed), `resources.arsc` md5 identical (`3c764058…`) — no res change |
| Other dex | classes.dex / classes3 / classes4 / classes6 byte-identical to 839; classes5 differs only by the version bump |

## Dump proof (final dex, from the signed APK)

`Lqmu.o()` (8 regs, 37 insns) — X: insn 7 `v2 = col*cellW`,
insn 10 `v4 = LayoutParams.width`, insn 11 `v5 = v3−v4`
(cellW−indicatorW), insn 12 `v5 /= 2` (corrX), insn 13 `v2 += v5`,
insn 15 `setTranslationX(v2)`. Y: insn 20 `v5 = row*cellH`,
insn 23 `v4 = LayoutParams.height`, insn 24 `v3 = v1−v4`
(cellH−indicatorH), insn 25 `v3 /= 2` (corrY), insn 26 `v5 += v3`,
insn 28 `setTranslationY(v5)`. IF_EQZ offset 69 (return at abs 71).

`Lqmu.i()` (27 regs, 404 insns) — insertion at abs `0x21a`:
insn 300 `v13 = LayoutParams.width`, insn 302–303 `v14 = Lqmr.b()`
(cellW), insn 304–305 `v14 = (cellW−indicatorW)/2`, insn 306–307
`oldX += corrX`, `newX += corrX`; insn 311 `v13 = LayoutParams.height`,
insn 312–313 `v14 = Lqmr.a()` (cellH), insn 314–315
`v14 = (cellH−indicatorH)/2`, insn 316–317 `oldY += corrY`,
`newY += corrY`. The float arrays / `ObjectAnimator`s at insn 323–341
then animate between the CORRECTED old/new values. Retargeted
branches land correctly (e.g. idx 204 `IF_EQZ` off 354 → `0x2c2`
= `0x29c`+38; idx 219 `GOTO_16` off 286 → `0x29c` = `0x276`+38).

Glide intact: `i()` still has `CONST_WIDE_16 #300` +
`AnimatorSet.setDuration` + `start()`; `o()` contains 0
`animate()`/`ViewPropertyAnimator` calls; `n() = m() + o()` unchanged.

## What did NOT change

Everything else is byte-identical to 176071839: PLUS layout, circle
background, tracker init to center, full-cell direction pixels, stock
300ms glide, emoji scale-up, caps suggestions, icons, caps toggle,
emoji row, CrashFix, Q–P numbers flag, the 26-key themed emoji sets,
all three VerifyError fixes.

## Test for Sam

Long-press a letter → popup opens → the green circle should now sit
EXACTLY centered on the middle emoji at open, and stay exactly centered
on whichever emoji is selected as you hold-drag (up / down / left /
right) — the glide should still be smooth, and the emoji scale-up
unchanged. Check all five positions.
