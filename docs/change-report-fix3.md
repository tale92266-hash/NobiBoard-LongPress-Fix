# NobiBoard — Simple Animated Fix3 — change report

**Code 176071839** (from 176071838) · package `com.nobi.nobiboard` · built 2026-11-12 ~08:30 UTC

## What Sam reported (176071838 works, one issue left)

Popup opens, center default, swipe, insert — all correct. Only the switch
animation is wrong: the green highlight circle **jumps** from emoji to emoji
instead of gliding smoothly ("Jump karta hai, glide nahi hota").

**Scope correction from Sam (13:50 IST): the emoji scale-up ALREADY works**
(swiping makes the emoji a little bigger; that is how he knows the selection
moved). Part 2 of the original brief (add scale-up) is CANCELLED — no scale
work was done in this build.

## Root cause — PROVEN from the shipped 176071838 `classes2.dex`

Two competing animators were driving the SAME indicator (`Lqmu.h`) on the
SAME properties (`translationX`/`translationY`) with DIFFERENT durations:

1. **Stock `Lqmu.i(II)V`** already glides the indicator with a **300 ms
   `AnimatorSet`** — 2 `ObjectAnimator`s (indicator `TRANSLATION_X`/`Y`,
   old cell → new cell) + 4 scale animators, `setDuration(300)`, started
   together. The scale animators in this same set are what make the
   emoji scale-up work — Sam confirms scale works, so this set provably runs.
2. **The 838 patch's `Lqmu.o()`** added a SECOND, competing **120 ms
   `ViewPropertyAnimator`** (`animate().translationX/Y().setDuration(120)`)
   on the same indicator, run on EVERY touch move via the `k()` head's
   unconditional `n()` call (`n()` = `m()` + `o()`).

The 120 ms VPA reaches the target first and finishes; the 300 ms stock set
keeps writing mid-flight values afterwards and pulls the indicator
BACKWARD → the green circle jumps instead of gliding.

Evidence (all from the shipped 838 dex, dexdump):
- NO direct `setTranslationX/Y` anywhere in `Lqmu`/`Lqml` except the two VPA
  `translationX/Y` calls inside `Lqmu.o()` itself. All other direct
  `setTranslationX/Y` sites in classes2.dex are unrelated classes
  (`axq`, `bfc`/`bfd`/`bfr`/`bfs`, `bge`/`bgf`, `buj`/`buk`,
  `AccessPointDragPopupView`, `WidgetSoftKeyboardView`) — none on the swipe path.
- `Lqmu.n()V` (15 units) = `m()` + `o()`; `Lqmu.p()V` (41 units) = `m()` +
  `o()` + visibility; neither sets translation directly.
- `Lqmu.m(Lqmc)V` scales the selected cell's view directly — the scale-up
  Sam confirms works.
- `Lqmu.i(II)V` (713 units, 27 regs): insns 0168–017e skip rebuilding if the
  selection is unchanged; insns 0180–0273 build and `start()` the outer
  `AnimatorSet` (v2): scale set (v13, 4 ObjectAnimators) + translation set
  (v8, 2 ObjectAnimators on `Lqmu.h` `TRANSLATION_X`/`Y`), `setDuration(300)`.

## Fix (ONE method, nothing else)

**Revert `Lqmu.o(Lqmc;)V` to the EXACT stock implementation** from the
shipped 176071834 dex (25 insns, 50 units, 6 regs: `this` v4, param v5;
insn 9 direct `setTranslationX`, insn 16 direct `setTranslationY`, tail
`setScaleX/Y/Alpha` 1.0f). The stock 300 ms AnimatorSet in `Lqmu.i()` is
then the SOLE driver of the indicator's translation → smooth glide.
The `k()` head's unconditional `n()` call is kept — with stock `o()` it is a
harmless one-time direct set, overwritten by the running set's next frame
(same as stock behaviour on the `Lqmt.b()` path).

Tools (new, `tools/src/`):
- `RevertOPatch.java` — `stockO()` reconstructs the stock `o()` body
  instruction-by-instruction; `checkStock()` asserts the reconstruction is
  identical to the actual 834 implementation (opcode + registers + refs,
  per instruction) and FAILS the build if not; `revertO()` asserts the
  input is the 838 glide version (39 insns, 9 regs, contains a VPA
  translationX) and the stock check passes, then returns the new
  implementation (6 regs, stock insns, no try blocks).
- `RevertOCombinedPatch.java` — driver (DexRewriter + Module), usage
  `RevertOCombinedPatch <input838.dex> <stock834.dex> <output.dex>`;
  asserts exactly 1 `o` replaced.
- `DexVersionBumpFix3.java` — classes5 version string 176071838 → 176071839
  (3 use-sites).

Patch run output:
```
stock o(): regs=6
checkStock: reconstruction == actual 834 o() (25 insns)
Lqmu.o: glide (39 insns, 9 regs, VPA) -> stock (25 insns, 6 regs, direct setTranslationX/Y)
patched: o=1
```

## Verification (all on the FINAL signed APK)

| Check | Result |
|---|---|
| File | `outputs/NobiBoard-LongPress-SimpleAnimatedFix3.apk`, 105,597,197 B |
| SHA-256 | `c50ee77fc01efd7e93584dc51aaf6be492b9fbb3b22c8736e5ef654d568efdf2` |
| versionCode | 176071839 (aapt2 badging), package `com.nobi.nobiboard` |
| Cert SHA-256 | `80:A9:47:FF:8B:5D:A7:4F:96:4A:F4:2C:A0:B9:6A:6C:EB:D5:48:68:AD:61:86:9D:6F:4F:88:05:86:B6:DA:7E` — v2 MATCH |
| Entry count | 9,465 |
| .so 16 KB alignment | 14 .so, 0 misaligned (zipalign `-c -P 16 -v 4`: 9,462 OK; direct offset check) |
| Unsigned-APK check (before signing) | classes2.dex md5 `5f3985fd…`, classes5.dex md5 `52ed413e…` — no stale-unsigned trap |
| CanonDiff vs 176071838 | **diff=1 method: `Lqmu;->o[Lqmc;]V` only** (47,276 methods each) |
| RegInitCheck (fixed) | `Lqml.k` 0, `Lqmu.c` 0, `Lqmu.o` 0, `Lqml.e` 0 violations |
| VerifyPatch (`Lqml`/`Lqmu`/`Lqck`/`Lqmr`) | 94 methods, 740 invokes, 0 violations |
| VerifyBranches | 120,969 checked, 0 bad |
| Payload alignment | 1,105 checked, 0 odd |
| Res diff vs 176071838 | `res/` identical, `resources.arsc` md5 identical (`3c764058…`) — no res change |

## Dump proof (final dex, from the signed APK)

- `Lqmu.o()` = the stock body exactly (25 insns): insn 0 `IGET v0,v4,Lqmu.h`,
  insn 1 `IF_EQZ → 0031`, insn 9 (abs 0010) direct `setTranslationX`,
  insn 16 (abs 001d) direct `setTranslationY`, tail `setScaleX/Y/Alpha` 1.0f,
  insn 24 `RETURN_VOID`. **animate()/VPA calls in `o()`: 0.**
- `Lqmu.i()` stock AnimatorSet intact: `TRANSLATION_X` + `TRANSLATION_Y`
  `ObjectAnimator`s on `Lqmu.h`, `AnimatorSet.setDuration`, `start()` present.
- Remaining `ViewPropertyAnimator.translationX/Y` calls in classes2.dex live
  only in unrelated classes (`Lcbv;`, `Ls;`/`Lt;` support-library
  RecyclerView Runnables) — **none in `Lqmu`/`Lqml`, none on the swipe path**.
- No `View.animate()` call anywhere in the `Lqmu` range.
- Conclusion: the swipe path's ONLY remaining indicator-translation driver is
  the stock 300 ms AnimatorSet in `Lqmu.i()`.

## What did NOT change

Everything else is byte-identical to 176071838: PLUS layout, circle
background, tracker init to center, full-cell direction pixels, caps
suggestions, icons, caps toggle, emoji row, CrashFix, Q–P numbers flag, the
26-key themed emoji sets, all three VerifyError fixes.

## Test for Sam

Long-press a letter → popup opens → hold and drag to another emoji: the
green circle should now GLIDE smoothly to the new emoji (300 ms), and the
emoji should scale up as before. Release inserts the selected emoji.
