# NobiBoard — Simple Animated Fix5 — change report

**Code 176071841** (from 176071840) · package `com.nobi.nobiboard` · built 2026-11-12 ~09:50 UTC

## What Sam reported (176071840 REGRESSED)

After installing Fix4, long-press popups are **gone entirely** — "After this
again long press are gone". Fix3 (176071839) WORKED (popup opens, glide
smooth, scale-up works) — only the green circle was offset ~5dp right /
~3dp down. Sam was told to roll back to Fix3 meanwhile.

## Root cause — PROVEN from the shipped 176071840 dex (NOT the null-LayoutParams guess)

The parent's first hypothesis — `View.getLayoutParams()` returning NULL at
open time, NPE in `Lqmu.o()` swallowed by `Lqck.D`'s catch-all — was
**RULED OUT by the trace**: `Lqmu.h` (the indicator) is assigned in exactly
ONE place, `SilkRectangularPopupView.onFinishInflate()` insn 003c, from
`findViewById(0x7f0b0709)` — i.e. the indicator IS an inflated child of
popup layout `0x7f0e0607.xml` (style `0x7f15063d`, layout_width/height =
`?0x7f04029e`), so `getLayoutParams()` is non-null. Stock `onFinishInflate`
itself reads `MarginLayoutParams.leftMargin/rightMargin` off
`getLayoutParams()` with NO null check. `o()` IS on the open path
(`c()` → tail `p()` → gated `o()` at insn 0014; `p()` does NOT call `i()`),
but the null check in Fix5's `o()` is hardening only.

**TRUE ROOT CAUSE — a register-type merge conflict in `Lqmu.i()`,
invisible to every older gate (RegInitCheck 0, VerifyPatch 0,
VerifyBranches 0, payload 0 odd — Fix4 passed them all):**

Fix4's `i()` insertion wrote REFERENCE values into **v11** — insn 021f
`move-result-object v11` (LayoutParams) and insn 0222 `iget-object v11,
v5, Lqmu.g` (the `Lqmr` tracker) — but v11 is an **INT on the 01f6 jump
path** (last write 012b `add-int/2addr v11, v5`; zero v11 writes/reads
between abs 012c and 01f6, verified by grep). At the merge point 027e
(predecessors: fall-through from 027d, and the 01f6 `if-eqz v6, 027e`
jump taken when indicator h is null) ART's verifier must merge v11 =
INT (jump path) with v11 = `Lqmr` reference (fall-through path) →
**VerifyError → whole `Lqmu` class fails verification at class load →
popup never builds**, swallowed by `Lqck.D`'s catch-all (no popup, no
crash — exactly Sam's report). The verifier is static: it rejects the
class even though the 01f6 jump is (almost certainly) never taken at
runtime (h is non-null). Same failure mode as the 835 `k()` defect.

The new gate **TypeMergeCheck** catches it mechanically: Fix4 `Lqmu.i`
flags `CONFLICT at join insn idx 350 (abs 27e) reg v11: I64 vs REF`
(the I64 is the checker's view of v11's int type on the jump path, which
runs through the wide-value region). Stock 839 `Lqmu.i` has NO conflict
at 027e.

## The fix — and why v11 could NOT simply be kept for ints

Fix5's centering block needs an INT scratch register for
cellW/corrX/cellH/corrY. The obvious candidate v11 is **unusable for
ints**: at the insertion point (abs 0x21a) v11 is the HIGH half of a
**live double** (`move-wide/from16 v10, v16` at 0x157; read again at
0x294 `mul-double/2addr v10, v2` → `double-to-long` → duration
computation). Writing an int to v11 would (a) corrupt that double at
runtime, and (b) create a NEW merge conflict at the block's FALLBACK
merge (fallback paths carry the entry I64, the fall-through carries the
written I32) — proven by TypeMergeCheck flagging exactly that on the
first Fix5 attempt (`CONFLICT at join insn idx 321 (abs 246) reg v11:
I64 vs I32`).

**v23 was tried next and is also wrong**: it is the HIGH half of the
v22/v23 double (`sget-wide v22` at 0xaa/0xcc), I64 at entry — same
conflict at the FALLBACK (`reg v23: I32 vs I64`). (A plain grep for
"v23" finds 0 occurrences because wide values are named by their LOW
half only — the register-state dump is the reliable source.)

**The correct scratch is v19**: from the register-state dump at the
insertion point (TypeMergeCheck `--state` mode, 839 `i()` idx 297):
v19 = **I32** at entry (`const/16 v19, 3`) and **DEAD after 0x21a**
(zero uses in the stock code after the insertion), so the FALLBACK
merge is I32-vs-I32 = clean, and no live value is touched. Full
entry state at 0x21a (for the record): I64 = v10, v11, v22, v23 (two
doubles); I32 = v3, v6, v7, v8, v9, v12, v13, v16, v18, v19, v20, v21,
v25, v26; REF = v0, v1, v2, v4, v5, v14, v15, v17, v24; NO register is
UNINIT. (v13 is I32 at entry, not REF — that is why the checker never
flagged it; v8 is I32 at entry but is written as REF at block insn 2,
before any fallback, so it merges clean.)

Because v19 > 15, the four `ADD_INT_2ADDR` (12x, 4-bit regs) became
`ADD_INT` (23x, 2 units each): block 24 insns, **48 units** (even),
FALLBACK = 0x24a. The 5 crossing branches retarget by **+10**.

## Fix — null-safe centered, register-type-clean

`Lqmu.o(Lqmc;)V` (41 insns, 8 regs) and `Lqmu.i(II)V` (24-insn /
48-unit block at abs 0x21a) both compute the centering correction
`(cell − indicator)/2` per axis at runtime from LayoutParams
width/height and `Lqmr.b()/a()`, with **null-safety**: if
`getLayoutParams()` is null OR width/height ≤ 0, the block falls back
to the stock top-left translation — the popup still opens, the circle
is just offset like Fix3 (never a throw). In `i()` the scratch is
**v19 (int only)**; v8 holds only references (LayoutParams, tracker);
v13 holds only ints (width/height); the v10/v11 double is never
written. In `o()` the params reference stays in v4 (never overwritten
with an int); width in v5, height in v3.

NOT touched: the glide (stock 300ms AnimatorSet), the scale-up, the
tracker, the PLUS layout, the circle background, caps, icons, emoji
row, CrashFix, Q–P numbers flag, the 26-key emoji sets, all three
VerifyError fixes. New refs: **ZERO**.

Tools (new, `tools/src/`):
- `Fix5CenterPatch.java` — `newO()` (41 insns, 4 fallback branches),
  `patchO()` (asserts Fix4 o() fingerprint), `centerBlockI()` (24
  insns, 48 units, 3 fallback branches to FALLBACK=0x24a),
  `patchI()` (asserts Fix4 i() 404 insns / 27 regs / abs 0x21a, no
  try blocks, no switch payloads; generic branch fixup, expects
  exactly 5 crossing branches, +10).
- `Fix5CombinedPatch.java` — driver (ONE dexlib2 pass over the shipped
  176071840 classes2.dex); asserts exactly 1 `o` and 1 `i` replaced.
- `DexVersionBumpFix5.java` — classes5 version string
  176071840 → 176071841 (3 use-sites). NOTE: the manifest
  versionCode lives in `decoded/apktool.yml`, NOT in classes5 — it had
  to be bumped separately (first signed build came out 176071840 and
  was rebuilt; see Verification).
- `TypeMergeCheck.java` — NEW GATE: forward abstract-interpretation
  type checker for one method (kinds UNINIT / I32 / I64 / REF / NULL /
  CONFLICT; NULL merges with anything — this is what makes the stock
  `o()` return clean, where v0 is null on the if-eqz path and 1.0f on
  the fall-through; `IF_EQZ`/`IF_NEZ` null refinement on the branch
  target / fall-through). Switch payloads NOT supported (loud warning;
  patched methods contain none — `patchI` asserts this). Usage:
  `TypeMergeCheck <dex> <class> <method> [paramTypes...]`, plus
  `--state:<idx>` to dump the register types at an instruction (used
  to pick v19). **Caveat: the checker has false positives on stock
  code with wide-value register reuse** (it does not model ART's
  wide-pair invalidation — e.g. stock 839 `Lqmu.i` shows 25).
  It is therefore used **DIFFERENTIALLY**: the patched method's
  conflict set must equal the stock method's set (same count, same
  registers, modulo the insertion shift). An EXTRA conflict vs stock
  is the signal. Fix4 `i()` = 26 vs stock 25 (the extra one is the
  real v11 defect at 027e); Fix5 `i()` = 25 = stock 25.

## Verification (all on the FINAL signed APK)

| Check | Result |
|---|---|
| File | `outputs/NobiBoard-LongPress-SimpleAnimatedFix5.apk`, 105,597,041 B |
| SHA-256 | `e56adab89395951ce6284784945a841a2418e75f193b7a49eb4d2b6364bfacc3` |
| versionCode | **176071841** (aapt2 badging), package `com.nobi.nobiboard`, label `NobiBoard` |
| Cert SHA-256 | `80:A9:47:FF:8B:5D:A7:4F:96:4A:F4:2C:A0:B9:6A:6C:EB:D5:48:68:AD:61:86:9D:6F:4F:88:05:86:B6:DA:7E` — v2 MATCH |
| Entry count | 9,465 |
| .so 16 KB alignment | 14 .so, 0 misaligned (`zipalign -c -P 16 4` exit 0; direct data-offset check 0/14) |
| Unsigned-APK check (before signing) | classes2.dex md5 `e1ea37d3…`, classes5.dex md5 `786d166f…` — matches the patched dexes, no stale-unsigned trap |
| CanonDiff vs 176071840 | **diff=2 methods: `Lqmu;->o[Lqmc;]V` and `Lqmu;->i[I, I]V` only** (47,276 methods each) |
| RegInitCheck (fixed) | `Lqml.k` 0, `Lqmu.c` 0, `Lqmu.o` 0, `Lqml.e` 0, `Lqmu.i` 0 violations |
| VerifyPatch (`Lqml`/`Lqmu`/`Lqck`/`Lqmr`) | 94 methods, 746 invokes, 0 violations |
| VerifyBranches | classes.dex 106,283 / classes2.dex 120,976 / classes3 37,110 / classes4 8,190 / classes5 34 / classes6 53 — **0 bad everywhere** |
| Payload alignment | classes2.dex 1,105 checked, 0 odd (0 odd in all six dex) |
| TypeMergeCheck (differential vs stock 839) | `Lqmu.i` Fix5 = **25** = stock 839 = 25, **no conflict at FALLBACK 0x24a, no v19 conflict anywhere**; `Lqmu.o` = **0**; `Lqml.k` Fix5 = 5 = stock 5; `Lqml.e` Fix5 = 4 = stock 4 — **zero NEW conflicts on any patched method** |
| Res diff vs 176071840 | `res/` identical (9,297 entries, 0 changed), `resources.arsc` md5 identical (`3c764058…`) — no res change |
| Other dex | classes.dex / classes3 / classes4 / classes6 byte-identical to 840; classes5 differs only by the version bump |

## Dump proof (final dex)

`Lqmu.o()` (8 regs, 41 insns, 80 units) — X: insn 0013
`if-eqz v4, 001f` (null LayoutParams → stock setTranslationX), insn
0017 `if-lez v5, 001f` (width ≤ 0 → stock), insn 0019–001d centering
math, insn 001f `setTranslationX`. Y: insn 002f `if-eqz v4, 003b`,
insn 0033 `if-lez v3, 003b`, insn 0035–0039 math, insn 003b
`setTranslationY`. Tail setScaleX/Y/Alpha 1.0f. v4 is REF-only
(`move-result-object`), v5/v3 int-only.

`Lqmu.i()` block (abs 0x21a–0x24a) — insn 0220 `if-eqz v8, 024a`
(null params → FALLBACK), insn 0224 `if-lez v13, 024a` (width ≤ 0),
insn 022b `move-result v19` (cellW), 022c `sub-int v19, v19, v13`,
022e `div-int/lit8 v19, v19, 2` (corrX), 0230/0232 `add-int v9/v12,
v19`; insn 023c `if-lez v13, 024a` (height ≤ 0), 0241
`move-result v19` (cellH), 0242/0244 sub/div (corrY), 0246/0248
`add-int v6/v7, v19`. FALLBACK 0x24a = `new-instance v8,
AnimatorSet` (first stock insn). v19 is int-only; v8 ref-only; v11/v10
double untouched (no v11 write in the block). Retargeted branches:
idx 204/205/207 → 0x2c2, idx 219 → 0x29c, idx 277 → 0x27e (all +10).

Glide intact: `i()` still has `CONST_WIDE_16 #300` +
`AnimatorSet.setDuration` + `start()`; `o()` contains 0
`animate()`/`ViewPropertyAnimator` calls; `n() = m() + o()` unchanged.

## What did NOT change

Everything else is byte-identical to 176071840: PLUS layout, circle
background, tracker init to center, full-cell direction pixels, stock
300ms glide, emoji scale-up, caps suggestions, icons, caps toggle,
emoji row, CrashFix, Q–P numbers flag, the 26-key themed emoji sets,
all three VerifyError fixes.

## Test for Sam

Long-press a letter → popup opens → the green circle should now sit
EXACTLY centered on the middle emoji at open, and stay exactly centered
on whichever emoji is selected as you hold-drag (up / down / left /
right) — the glide should still be smooth, and the emoji scale-up
unchanged. Check all five positions. (If the popup does NOT open at
all, that would mean a new VerifyError — report it immediately; the
diag build 176071837 is kept as fallback, and Fix3 176071839 is the
current working build.)
