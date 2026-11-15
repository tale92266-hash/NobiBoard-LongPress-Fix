# NobiBoard long-press emoji popup — how the code actually works

**Audience:** an AI (or engineer) with no prior context, who needs to trace
the popup + long-press logic from the bytecode. **Read this first, then the
smali in `smali/`, then the dexdump excerpts in `dumps/`.**

**What this is:** Gboard has no Java source. Its popup/long-press logic is
compiled Dalvik bytecode (dex). This document explains it, and `smali/`
contains the human-readable disassembly (baksmali, from the shipped 861
APK, code 176071861) of the classes that are directly in the
popup/long-press path and the def-parse/customization path. That is a
short, scoped excerpt for diagnosis — not the whole app.

**Naming:** Gboard's code is obfuscated (`Lqmu`, `Lqml`, `Lqmr`, … — all in
`defpackage`, i.e. classes2.dex). The one real name is the popup view:
`Lcom/google/android/libraries/inputmethod/popup/SilkRectangularPopupView;`.
Method/field names below are the obfuscated ones; the roles are what we
proved from the bytecode + device behaviour.

---

## 1. The big picture

Long-press a letter key → a circle popup (84×84dp, 3×3 grid of 28×28dp
cells) shows 5 themed emojis in a PLUS layout: **center + up / down / left /
right**. While the finger is held, dragging anywhere on screen moves the
selection step-by-step (tick-tick-tick); the green highlight circle
(indicator) **glides** to the new cell in 300 ms and the emoji **scales up**;
releasing inserts the highlighted emoji.

Three objects do the work:

| Object | Class | Job |
|---|---|---|
| Popup view | `SilkRectangularPopupView` | the visible circle; owns the controller |
| Controller | `Lqmu` (extends `Lqml`) | builds the popup, tracks selection, drives the indicator + animation |
| Spatial tracker | `Lqmr` (implements `Lqmd`) | maps a touch point to a grid cell; holds the cell registry |

The touch flow is **relative, not absolute**: `Lqml.k()` measures the drag
**delta from the touch-down anchor** and maps it to a direction (up/down/
left/right/center), then to full-cell-center pixels, then `Lqmu.i()` turns
those pixels into a cell via the tracker.

---

## 2. End-to-end flow

```
finger holds a letter key
  └─ long-press timer fires Lpiy.run()          [smali/piy.smali, case 0x2d]
       ├─ Lqck.k(Lpuc.b) → ActionDef (the long-press action)
       ├─ Lqck.n(ActionDef, …)                  (handle the action)
       └─ if Lqck.i != Lpuc.b: Lqck.D(0)        [smali/qck.smali]
            └─ Lqck.D(J)V — popup-creation entry, 27 regs
                 ├─ Trace.beginSection("TouchActionBundle.showPopup")
                 ├─ ONE catch-all try: start 9, len 614, handler 0x273
                 │    (validates via V()/I()/ActionDef.d()/M(), then drives
                 │    the popup through the Lqch helper +
                 │    AccessibilityFullScreenPopupView)
                 └─ handler: move-exception → Trace.endSection() → THROW
                      (it RETHROWS — see §8; it does NOT swallow)

popup build (inside D's try, via the view)
  └─ SilkRectangularPopupView.onFinishInflate() [smali/com/.../SilkRectangularPopupView.smali]
       ├─ Ltga.a(this)
       ├─ findViewById(0x7f0b0705) → LinearLayout  (field e)
       ├─ findViewById(0x7f0b0706) → View           (field f, or e as fallback)
       │    → read its MarginLayoutParams → Lqml.f(leftMargin, rightMargin)
       └─ findViewById(0x7f0b0709) → indicator View → Lqmu.h
            (Lqmu.h is assigned in EXACTLY this one place)

  └─ Lqmu.c(LinearLayout, SoftKeyboardView, View, F, F, ActionDef, [I)V
       [smali/qmu.smali — Fix11's exact 101-insn version since 856;
        the Phase 3 prefs hook (436 insns) was REMOVED in 856]
       ├─ super Lqml.c(...) — base build (sets Lqml.a = "indicator shown")
       ├─ reads Lqmn.f (initial col) and computes the initial row:
       │    (d()==false && c()==false) ? Lqmn.d - 1 : 0
       ├─ sets Lqmr.b AND Lqmr.c to that initial point
       ├─ clears Lqmr.d (the cell registry) and rebuilds it:
       │    for each row/col, for each child view with a real id:
       │      Lqmc(Point(col,row), id, cellView, labelView)
       │      labelView = findViewById(0x7f0b070b) or 0x7f0b070a
       │      (missing label → throws NPE "Null labelView";
       │       missing view  → throws NPE "Null view")
       ├─ EMOJI GUARD (stock, insn 70): if the items list has exactly 9
       │    entries and item[3] is an emoji (codePoint >= 0x10000) → the
       │    tracker-init block (insns 94–96) sets Lqmr.b AND Lqmr.c to
       │    (1,1) — CENTER init
       └─ p() — refresh selection + indicator visibility

  └─ Lqmu.p()V  [smali/qmu.smali]
       ├─ Lmgw.L(tracker, point) + Lmgw.I(tracker, point) → Lqmc
       └─ if non-null: m(Lqmc) (scale) then
            if Lqmu.a: o(Lqmc) + indicator VISIBLE (setVisibility 0)
            else:      indicator INVISIBLE (setVisibility 4)

touch moves (finger held, dragging anywhere)
  └─ Lqml.k(FF)Lpva — the touch handler  [smali/qml.smali]
       ├─ EMOJI GUARD: 9 items, item[3] is an emoji, cell list non-empty,
       │    instanceof Lqmu → CHECK_CAST to Lqmu
       │    (the CHECK_CAST is the 836 fix: INSTANCE_OF alone does NOT
       │    narrow the static type for ART's verifier)
       ├─ anchor = Lqml.i (float X) / Lqml.b (float Y); if both 0 → stock
       │    fallback path (cond_a8)
       ├─ content w/h = LinearLayout size minus padding;
       │    cellW = w/3, cellH = h/3
       ├─ deltaX = (int)touchX - anchorX, deltaY = (int)touchY - anchorY
       ├─ direction → FULL-CELL-CENTER pixels:
       │    |dx|<cellW && |dy|<cellH → CENTER (w/2, h/2)
       │    dy most negative         → UP    (w/2, h/2 - cellH)
       │    dy most positive         → DOWN  (w/2, h/2 + cellH)
       │    dx most negative         → LEFT  (w/2 - cellW, h/2)
       │    dx most positive         → RIGHT (w/2 + cellW, h/2)
       ├─ i(px, py) — virtual call, dispatches to Lqmu.i
       ├─ n() — scale + position indicator
       └─ a() → Lpva (the item that will be inserted on release)

  └─ Lqmu.i(II)V — the move handler  [smali/qmu.smali, 27 regs]
       ├─ clamps the pixel point (Lqmd cell dims, Math.round, Lsag.e clamp)
       ├─ Lqme.a(px, py, Point) → nearest-item delta
       ├─ Lmgw.J(tracker, col, row) → target Lqmc
       ├─ Lmgw.L(tracker, Point) — update the tracker
       ├─ Lqme.b() — ends any running AnimatorSet
       ├─ builds TWO AnimatorSets (outer + scale set) + 4 ObjectAnimators
       │    (translationX/Y old cell → new cell + scaleX/Y), setInterpolator,
       │    setDuration(300) (const-wide 0x12c), start()
       │    — THIS is the glide + the emoji scale-up (stock, 300 ms)
       ├─ Fix5 v19 block: getLayoutParams() of the indicator + cell dims
       │    from Lqmr → move-result v19 → sub/div/add to CENTER the
       │    AnimatorSet end values (see §6)
       └─ Lqms listener: onAnimationEnd → m(old) + m(new) + n() + Lqmt.a()

release
  └─ Lqml.a()Lpva — returns Lqml.e[Lqml.g] (the item at the stored index),
       or null if g == -1 → the emoji is inserted.
```

---

## 3. The classes

### `Lqmu` — Silk popup controller (`smali/qmu.smali`, public final, 12 methods)

Extends `Lqml`. Fields: `a:Z` (indicator shown), `f:Lqmo` (config),
`g:Lqmr` (tracker), `h:Landroid/view/View` (the green indicator,
40×40dp, drawable `0x7f080316` = `#ff4db6ac`).

| Method | What it does |
|---|---|
| `c(LinearLayout, SoftKeyboardView, View, F, F, ActionDef, [I)V` | popup build + tracker init (see §2). **101 insns in 861 (Fix11's exact version)** — NOT the 436-insn Phase 3 hook version (removed in 856) and NOT the 72-insn fully-stock V40 version. |
| `p()V` (private final) | refresh: resolve current item → `m()` + `o()` + indicator visibility |
| `o(Lqmc;)V` (private final, 8 regs, **25 insns in 861 — STOCK**) | position the indicator — see §6. The Fix5 41-insn centred version was removed with the 856 revert. |
| `i(II)V` (protected final, 27 regs) | move handler: pixel → cell → 300 ms AnimatorSet glide — see §6. **STOCK in 861** (no v19 centring block — that was Fix5, removed in 856). |
| `m(Lqmc;)V` (public final) | scale the selected cell's emoji: if `a` && item.Point == `Lqmr.c` → scale = `l(View)` (1.0–1.32); else 1.0f; setScaleX/Y on the item's `d` View |
| `n()V` (public final) | `Lmgw.K(tracker)` → `Lqmc`; if non-null: `m()` + `o()` |
| `l(View)F` (public static) | scale factor: for `AutoCenterScaleTextView`, `min(contentW/textW, contentH/textH)` clamped to **[1.0, 1.32]**; otherwise 1.32f |
| `a()Lpva` | the selected action (inherited behaviour) |
| `b()V`, `d()V`, `e()V` | lifecycle (close / dismiss / background) |

### `Lqml` — base popup controller / touch handler (`smali/qml.smali`, public)

Fields: `a:Z`, `b:F` (anchor Y), `c:LinearLayout`, `d:List` (cell views),
`e:List` (the 9 items), `f:Lqmo`, `g:I`/`h:I` (stored selected indices),
`i:F` (anchor X), `j:Lrfp`, `k:Ligp`.

| Method | What it does |
|---|---|
| `c(LinearLayout, SoftKeyboardView, View, F, F, ActionDef, [I)V` (33 regs) | base build: sets up `Lrfp`, reads `SoftKeyboardView.H`/`W`, sets `a` |
| `e()V` (public) | **background**: emoji guard (9 items, item[3] emoji) → `setBackgroundResource(0x7f080786)` (the circle drawable `emoji_popup_circle.xml`, #ff3c474c + 1dp #ff9aa0a6) on the container's parent; else stock `0x7f080319`. Then `m(View, a)` highlight + stores the index in `g`/`h` |
| `k(FF)Lpva` (public final) | **touch handler** — the relative-delta direction mapping (see §2) |
| `i(II)V` (protected) | base move handler — **overridden by `Lqmu.i`**; `k()` calls it virtually, so a `Lqmu` gets the override |
| `a()Lpva` (public) | returns `e[g]`, or null if `g == -1` — the release value |
| `f(II)V` (public final) | margins (called from `SilkRectangularPopupView.onFinishInflate` with the LinearLayout's left/right margins) |
| `g(OnClickListener)V`, `h(View)V`, `j()Z`, `d()V` | misc wiring / dismiss |

### `Lqmr` — spatial tracker (`smali/qmr.smali`, public final, implements `Lqmd`)

Fields: `b:Point` (current cell), `c:Point` (previous/target cell),
`d:List` (the `Lqmc` cell registry), `e:Lqmu` (synthetic back-ref).

- `a()` / `b()` / `c()` / `d()` read the cell dimensions through
  `Lqmu.f` (`Lqmo`) → `Lqmo.d` (`Lqmn`) → fields `b` (`a()` = cell
  height), `a` (`b()` = cell width), `e` (`c()`), `d` (`d()`).
- The tracker is the single source of truth for "which cell is selected":
  `Lqmu.c` initialises `b`/`c`, `Lqmu.i` updates them via `Lmgw.L`,
  `Lqmu.p`/`n` resolve them via `Lmgw.I`/`Lmgw.K`/`Lmgw.J`.

### `Lqck` — long-press dispatcher (`smali/qck.smali`, public final)

`D(J)V` (public final, 27 regs) is the popup-creation entry. It validates
the action (`V()`, `I()`, `ActionDef.d()`, `M()`) and drives the popup
through the `Lqch` helper + `AccessibilityFullScreenPopupView`
(`a(SoftKeyboardView)` / `b()`). Its one catch-all try (start 9, len 614,
handler 0x273) covers the whole build — see §8.

Callers of `D` (exactly 2): `Lpiy.run()` (case 0x2d, after `Lqck.n`) and
`Lqck.n(...)` itself (self-call when the ActionDef's `e` is not `Lpuc.b`).

### `Lpiy` — the long-press Runnable (`smali/piy.smali`, public final synthetic, implements `Runnable`)

A switch-dispatch Runnable: field `$cid:I` selects the case,
field `a:Object` is the target. Case `0x2d` is the long-press path:
`Lqck.k(Lpuc.b)` → `ActionDef` → `Lqck.n(ActionDef, …)` → if
`Lqck.i != Lpuc.b`, `Lqck.D(0)`. Other cases: `$cid 0` → `Lqck.s(x)` +
`Lqcl.j`; `0x1b` → clear pressed; `0x60` → `I()` check + `ActionDef.h`.
**`run()` has NO try block** (verified in the smali).

### `Lqme` — nearest-item search (`smali/qme.smali`, public final)

Static doubles `a = 0.41421356237309503` (√2−1) and `b = 2.414213562373095`
(1+√2) — the direction thresholds. Fields: `c:Lqmd` (cell-size interface),
`d:Lqmt` (animation context).

- `a(II, Point)Point` — given pixel X/Y + a cell Point, returns
  `Point(round(px − (col+0.5)*cellW), round(py − (row+0.5)*cellH))`:
  the touch's offset from the cell's centre, in pixels.
- `b()V` — ends a running AnimatorSet: if `Lqmt.b()` is true and
  `Lqmt.c` (AnimatorSet) `isRunning()` → `end()`, then `Lqmt.a()`.

### `Lqmc` — the selected-item descriptor (`smali/qmc.smali`, public final, extends `Ldrd`)

Fields: `a:Point` (cell), `b:int`, `c:View` (cell view), `d:View`
(content/label view). Constructor `(Point, int, View, View)`. This is
what `o()` positions the indicator on and `m()` scales.

### `Lqmf` — cell-size helper (`smali/qmf.smali`, public final)

Constructor reads styled attributes (a boolean + three dimension pixel
offsets `b`/`c`/`d` via TypedArray). `a(View)` computes a height-related
value (`b` + view height − paddingBottom). Static `b(View,View)` returns
paddingBottom or 0.

### `Lqms` / `Lqmt` — animation bookkeeping (`smali/qms.smali`, `smali/qmt.smali`)

- `Lqms` (final, extends `AnimatorListenerAdapter`): `onAnimationEnd` →
  `Lqmu.m(Lqmt.a)` + `Lqmu.m(Lqmt.b)` + `Lqmu.n()` + `Lqmt.a()`.
- `Lqmt` (final): field `c` = the `AnimatorSet`; `b()Z`; `a()V`;
  `g:Lqmu`; `a`/`b:Lqmc` (old/new items). Held by `Lqme` as field `d`.

### `Ltga` — view helper (`smali/tga.smali`, public final, source "PG")

Called by `SilkRectangularPopupView.onFinishInflate` (`Ltga.a(View)`);
also called from two other places in classes2.dex. Methods a–g.

### `SilkRectangularPopupView` — the popup view (`smali/com/google/android/libraries/inputmethod/popup/SilkRectangularPopupView.smali`, public, extends `FrameLayout`)

Fields: `a:Ltga`, `c:Lqmu` (the controller), `e:LinearLayout`
(id `0x7f0b0705`), `f:View` (id `0x7f0b0706`, or `e` as fallback).
`onFinishInflate()` wires it all up (see §2). `Lqmu.h` (the indicator)
is assigned here and nowhere else.

---

## 4. The tracker: how touch becomes a cell

1. **Registry.** `Lqmu.c()` builds `Lqmr.d`: one `Lqmc` per real cell,
   `Point(col, row)` + item id + cell View + label View. Cells whose id
   is −1 (the blank spacers) get a `null` entry instead — they exist in
   the list but are never selectable.
2. **State.** `Lqmr.b` = current cell Point, `Lqmr.c` = previous/target
   cell Point. Both start at the initial point from `Lqmu.c()` and are
   forced to **(1,1) = centre** by the emoji-guard block (the patch).
3. **Move.** `Lqml.k()` turns the drag delta into full-cell-centre
   pixels; `Lqmu.i()` clamps them, calls `Lqme.a` for the nearest-item
   delta, resolves the target `Lqmc` via `Lmgw.J(tracker, col, row)`,
   and updates the tracker via `Lmgw.L(tracker, Point)`.
4. **Refresh.** `Lqmu.n()` (and `p()`) resolve the current `Lqmc` via
   `Lmgw.K(tracker)` and call `m()` (scale) + `o()` (indicator).

The earlier Phase 2Fix3 bug was exactly here: the old `k()` hook
early-returned after setting only `g` + pressed state, **bypassing the
stock `k()` body — the only caller of `Lqmu.i()`** — so the tracker
stayed frozen at its build-time point (1,2) = bottom-middle = DOWN emoji
and swipe could never move anything. The fix requirement is: drive the
tracker — init to centre (1,1) at open + route touch through `Lqmu.i`,
never early-return out of `k()` without replacing the tracker update.

---

## 5. The grid: PLUS layout and the 3×3 mapping

The popup is a 3×3 grid, but the stock mapping **rotates the columns by
+1 and flips the rows**: data index `i` → visual cell
`(col, row) = ((i % 3 + 1) % 3, 2 - (i / 3))`.

The current list order in `0x7f170fd2.xml` (verified on the A key) is:

```
[DOWN, _, _, CENTER, RIGHT, LEFT, UP, _, _]
```

i.e. centre = index 3. The 4 corner cells are `U+200B` (zero-width
space). **Empty tokens must be `U+200B`, never empty**: an empty token
becomes `null` via the comma splitter and kills the whole LONG_PRESS
ActionDef (the Phase 1 bug). The 4 corner `U+200B` cells are never
selectable under the direction mapping, which only yields indices
3/6/0/5/4 — "zero blank area" holds in selection terms only; the 9-token
list is kept because a 5-item list would give a 2×3 grid, not a plus.

**Grid mapping DEPENDS ON KEY COLUMN (root cause of the side-key issue,
proven in Fix11):** centre keys use `(col,row) = ((i%3+1)%3, 2-(i/3))`
(rotated +1 → plus shape); **left-edge keys (Q/A/Z) use `(i%3, 2-(i/3))`**
(no rotation → UP/DOWN land in corners, '<' shape); **right-edge keys
(P/L/M) use `(2-(i%3), 2-(i/3))`** (mirrored → '>' shape). Fix11 is
**XML-only** — it permutes the `long_press_data` lists for the 12
edge-key entries in `0x7f170fd2.xml` so they land on plus cells under
their own column mapping: left edge `[_, DOWN, _, LEFT, CENTER, RIGHT, _, UP, _]`,
right edge `[_, DOWN, _, RIGHT, CENTER, LEFT, _, UP, _]`; same 5 emojis
per key, only reordered; blanks stay `U+200B`; centre keys untouched.
A key's fd2 entry (post-Fix11): `U+200B,U+1F495,U+200B,U+1F496,U+1F60D,U+1F618,U+200B,U+1F970,U+200B`
— token[0]=blank, token[1]=💕 DOWN, token[3]=💖 LEFT, token[4]=😍 CENTER,
token[5]=😘 RIGHT, token[7]=🥰 UP. **The customization hooks fingerprint
item[3] → token[3] = 0x1F496 (💖) for A — NOT 0x1F60D, which is token[4],
the CENTRE emoji.**

---

## 6. How `o()` and `i()` position the indicator (the centring problem)

The indicator view (`Lqmu.h`) is **40×40dp**; a cell is **28×28dp**.
Both positioners historically set the indicator's translation to the
cell's **top-left corner** (`col*cellW`, `row*cellH`), so the indicator's
centre sat `(indicator − cell)/2` ≈ **6dp right and 6dp down** of the
cell centre. Measured on Sam's Fix3 screenshot: ~5dp right, ~3dp down.
That is the open defect Fix4/Fix5 tried to fix.

**Stock `o()`** (25 insns, 6 regs: `this` v4, param v5): direct
`setTranslationX/Y` to the cell top-left, then a tail
`setScaleX/Y/Alpha(1.0f)`.

**Fix5 `o()`** (41 insns, 8 regs — in the OLD Fix5 smali, NOT in 861):
reads the indicator's
`LayoutParams` width/height; **null guards** (`if-eqz v4`,
`if-lez v5`, `if-lez v3`) fall back to the stock top-left position;
otherwise the centred position
`col*cellW + (cellW − indW)/2`, `row*cellH + (cellH − indH)/2`, computed
at runtime from the LayoutParams. Direct `setTranslationX/Y`, same tail.

**CURRENT STATE (861): `o()` and `i()` are STOCK (25 insns / 27 regs).**
The Fix5 centred `o()`/`i()` were removed by the 856 revert (which
restored Fix11's exact `Lqmu.c()` and left `o()`/`i()` stock), so the
~5dp right / ~3dp down circle offset from Fix3 is PRESENT in the
current working build (856) — it was never re-fixed after Fix5's
regression. Any future centring attempt must re-do the Fix5 work on
the 856 base AND prove it does not reintroduce the popup regression
(see §9: Fix4/Fix5 both regressed, and Fix5's dex had NO provable
defect location).

**Fix5 `i()`** (27 regs — in the OLD Fix5 smali, NOT in 861): the AnimatorSet's end values
get the same centring correction from a **24-insn block using scratch
register v19 (int-only)**. v19 is chosen because it is `I32` at entry
and DEAD after the insertion — the Fix4 defect was writing REFERENCE
values into v11, which is an INT on the 01f6 jump path, so ART had to
merge INT vs `Lqmr` ref at 027e → VerifyError → whole `Lqmu` failed at
load. The v19 block has **zero v11 writes**. The glide is still the
stock 300 ms AnimatorSet (`const-wide/16 0x12c`).

**Why the 835/836 builds had no popup at all** (three independent
VerifyError defects, all in the same arc):
1. `Lqml.k()` insn 88 called subclass `Lqmu->n()` on a `Lqml`-typed
   `this` — INSTANCE_OF does not narrow the static type for ART's
   verifier; only CHECK_CAST does (fixed in 836).
2. `Lqmu.o()` both `setDuration(J)` invokes listed register count 2
   instead of 3 (a long needs 3) (fixed in 836).
3. `Lqmu.o()` was grown 6→9 registers by the 835 glide patch, moving
   `this` v4→v7 and the param v5→v8 — but the body was kept verbatim
   and still read v4/v5, which are never-written locals with 9 registers
   → ART rejects `o()` for uninitialized-register use (fixed in 838 by
   remapping the body to v7/v8).

**The glide-jump bug (Fix3):** two competing animators drove the same
indicator on the same properties with different durations — the stock
`Lqmu.i()` 300 ms AnimatorSet vs the 838 patch's `Lqmu.o()` 120 ms
ViewPropertyAnimator, run on every touch move via `k()`'s unconditional
`n()` call. The 120 ms VPA finished first; the 300 ms set kept writing
mid-flight values and pulled the indicator BACKWARD → jump, not glide.
Fix3 reverted `o()` to the **exact stock implementation** (25 insns) so
the stock 300 ms set is the SOLE driver of the indicator's translation.
`k()`'s `n()` call is kept (with stock `o()` its direct set is one-time
and overwritten by the running set's next frame — same as stock).

---

## 7. The emoji guard (appears in `c()`, `e()`, `k()`)

Three methods repeat the same guard: the items list must have exactly 9
entries **and** item[3] must be an emoji (`Lpva`, `e` a non-empty
`CharSequence`, `codePointAt(0) >= 0x10000` — a supplementary-plane
character). Only then does the emoji-popup behaviour apply (centre-init,
circle background, relative-swipe `k()` hook). Otherwise the stock
behaviour runs. This is how the patch is scoped to emoji popups only:
the popup view is REUSED for other popups (e.g. the key-preview bubble),
and those must keep the stock background/behaviour.

---

## 8. The catch-all try in `Lqck.D` — and why failures are silent

`Lqck.D(J)V` has **one catch-all try: start 9, len 614, handler 0x273**
(`.catchall`, handler type null). It covers the entire popup build.

**Correction to earlier diagnosis wording:** the handler does **not**
swallow the exception. It does:

```
:catchall_273
    move-exception v0
    invoke-static {}, Landroid/os/Trace;->endSection()V
    throw v0
```

i.e. it ends the Trace section and **rethrows**. Neither `Lqck.n` nor
`Lpiy.run` has a try block, so the exception propagates out of the
long-press Runnable. The observed device behaviour is still "no popup,
no crash" — so the exception is contained by the Runnable's dispatch
(the scheduler/Handler that runs `Lpiy`), which captures the task
exception instead of crashing the app. The net effect is identical to a
swallow (silent no-popup), which is why earlier reports worded it that
way — but an AI instrumenting `D` must know the rethrow is there: the
Fix5-Diag ERR log sits in this handler (after `move-exception`), which
is the correct place to catch the Throwable before the rethrow.

Two consequences that matter for diagnosis:

- **A VerifyError in any class `D` touches kills the popup silently.**
  If `Lqmu` (or `Lqml`/`Lqmr`/`Lqck`) fails ART verification, the class
  is erroneous for the **process lifetime** — every subsequent long-press
  fails identically until the process is restarted (force-stop / reboot).
  This is why "force-stop + retest" is a real discriminator, not a
  ritual.
- **A failure inside `D`'s try never reaches the crash reporter.**
  There is no crash, no log, no Toast — only the absence of the popup.
  That is why the Fix5-Diag instrumented `D` itself (entry Toast/Log
  `"D-enter 176071842"` + catch-handler Toast/Log with the Throwable):
  no Toast = `D` never entered; D-enter only = build completed but the
  popup was not shown; D-enter + ERR = the exception names the cause.

---

## 9. Current state (2026-11-15 ~11:45 IST) and what is still unknown

| Build | Code | Result |
|---|---|---|
| Fix11 | 176071849 | **OWNER'S FAVOURITE — WORKS.** XML-only (classes2.dex byte-identical to Fix8). Side-key UP/DOWN straight up/down (+ shape), popup opens and holds while pressed, glide smooth. No customization. |
| Phase 3 | 176071851 | Editor WORKS, **popup NEVER appears**. Cause PROVEN: 7 field reads from `Object`-typed registers in the `Lqmu.c()` hook → VerifyError at class load (`diagnoses/diagnosis-hook-851.md`). |
| Phase 3 Fix | 176071852 | **Popup opens** (7 narrowings). Flickers at open; Q–P popups contain a DIGIT cell (cae.xml merge); number row ON after fresh install (Auto Backup restore). |
| 853 | 176071853 | Number-row default already false (fix = manual toggle) + height ratio 1.0 → 1.2. |
| 854 | 176071854 | Digit fix (10 D01–D10 merges removed) + `show_animation` removed. **Flicker WORSE: popup comes and goes while HOLDING.** |
| 855 | 176071855 | `show_animation` restored. **During HOLD no popup; on RELEASE it appears and goes away.** |
| **856** | **176071856** | **`Lqmu.c()` reverted to Fix11's exact 101-insn version. POPUP WORKING — CURRENT ROLLBACK BUILD.** Editor saves, but no code reads the prefs (no customization). |
| 857 | 176071857 | Hook at the LIST LOAD in `Lqml.c()` (insn 25). **REGRESSION — popup gone** (5th). ONE defect PROVEN: v13 colId clobber (explains WRONG slots, NOT the missing popup — `diagnoses/diagnosis-857.md`). |
| 858 | 176071858 | **Def-parse hook** in `Lpud.m` (insn 0; long-press codes UNTOUCHED, classes2 = Fix11). **Customization STILL does NOT reflect**, even after force-stop. |
| 859 | 176071859 | **Toast diagnostic** (only `Lpud.m`, no behaviour change). Owner: **NO Toast at all**, popup showed defaults. |
| 860 | 176071860 | Context fix (`Lesb.onCreate()` → `Lpud->appCtx`) + `nobi_diag` persistent diagnostic (editor Diagnostics section). **Customization STILL does not show. Diagnostics "No record yet" even after force-stop + keyboard open.** |
| **861** | **176071861** | **CURRENT — BROKEN. Keyboard does NOT open AT ALL after installing over 860.** Owner wants it fixed PERMANENTLY. **DO NOT INSTALL. Rollback is 856.** Cause TBD — `diagnoses/diagnosis-861.md` pending; prime suspects in `PROBLEM_PROMPT.md` §4. |

**What is still unknown:**

1. **Why does 861 stop the keyboard from opening?** The two prime
   suspects: (a) the `Lmxa.attachBaseContext` hook breaks Application
   startup (`SPUT_OBJECT` into `Lpud` triggers `Lpud.<clinit>` at
   attach time, before `onCreate()`); (b) the substitution now fires
   for the first time and throws during def-parse, killing the def
   load. The `diagnosis-861` (pending) settles this.
2. **Why was the Context null on the first `Lpud.m` call in 860?**
   Diagnosis-860 proved the write was gated by `diagFirst` and deduced
   the Context was null on the first call — but the exact trigger
   (early-init race vs a ContentProvider-triggered def-parse) is NOT
   provable statically. The 861 fix addresses it regardless (write on
   every call + Context set at `attachBaseContext`), but if 861's
   keyboard-not-opening is caused by the `attachBaseContext` hook
   itself, the "earliest Context" idea is the wrong trade.
3. **The centring itself is unproven on-device.** Fix5's centred
   `o()`/`i()` has never been seen working — every build carrying it
   had no popup at all. The current working build (856) has the stock
   ~5dp/~3dp circle offset.
4. **The digit verdict is still unconfirmed on-device** (Sam never
   explicitly confirmed the digit/superscript are gone in 854+; his
   reports focused on flicker/hold/customization). If the digit
   persists, the remaining candidates are the `D06_dup` merge (kept in
   854+), a stale IME def cache, or the ActionDef-builder insertion
   (now located: `Lpud.m`).

**The open problem:** make the keyboard ALWAYS open (even if the hook
fails) AND make the customization from Settings show in the popup,
with the popup exactly Fix11's. See `PROBLEM_PROMPT.md` for the
complete standalone problem statement.

---

## 11. The def-parse path (how customization reaches the popup)

The customization does NOT touch the popup at build time. It is applied
at **keyboard-def load time**, when the keyboard definition is parsed —
long before any popup is built. The def cache is **in-memory only**
(`Levj`/IMEManager, invalidated only by the 13-int reload list in
`Levj.a()`); force-stop clears it, so the def IS re-parsed after a
force-stop. There is NO file-based def cache in the traced code.

**The real path (verified in full by diagnosis-859 — every link dumped
from the shipped 858 dex):**

```
fd2.xml <softkey_list splitter="," template_id="@id/0x7f0b2665">
  └─ Lpxa.a (softkey branch, insn 140)
       └─ Lpwz.q / Lpwz.h — template binding
            ├─ Lpub.e → Lptq.c → Lptw.a → Lpvb.h
            │    splits the 9-token string by "," and parses each U+XXXX
            │    via Lpvb.f → 9-element [I (item[3] = 0x1F496 for A)
            └─ Lpwz.d → Lpwz.f
                 ├─ insn 110: INVOKE_VIRTUAL Lpud.m([I,[Lpuz;,[Object;)V
                 │    Lpud.m builds the Lpva list (stock body: null/empty →
                 │    empty list Lwmm.a; else loops, NEW_INSTANCE Lpva with
                 │    g = 0x7fffffff, Lwgz.h(Lpva), Lwgz.g() → Lwhe →
                 │    IPUT Lpud.b)
                 │    THE 858/859/860/861 HOOK SITS HERE (insn 0, before the
                 │    original body): fingerprint [I length==9 + item[3]
                 │    code point → 26-way chain → pref key + column class →
                 │    prefs read → split(",") must give exactly 5 parts AND
                 │    every part non-empty → clone both arrays → 5
                 │    substitutions at per-column slots → MOVE_OBJECT v13/v15
                 │    = clones. Any failure → GUARD (arrays untouched).
                 ├─ insn 198: Lpud.c() → ActionDef (NEW_INSTANCE ActionDef;
                 │    <init>(Lpud))
                 └─ insn 201: Lpwt.o(ActionDef)
                      └─ Lqml.c insns 24–25: ActionDef->f → Lqml->e
                           (the list the popup uses)
```

**CORRECTION to the 858 framing:** `long_press_data` is NOT consumed
by a hashCode SPARSE_SWITCH in `Lpwt.e` (`Lpwt.e` handles 15 OTHER
attributes). The string `"long_press_data"` exists in NO dex and its
hashCode (484777929) is in NO switch payload. The real path is the
**template-placeholder binding** above: the template `0x7f0b2665`
(`0x7f170ef3.xml` line 43) has TWO actions (PRESS with
`data="$press_data$"`, LONG_PRESS with `data="$long_press_data$"`), so
each letter key makes **two** `Lpud.m` calls — the LONG_PRESS one
carries the 9-item `[I`. Other `Lpud.m` callers: `Lpud.e` insn 198
(wrapper) and `Lpht.a` insns 28/60 (classes2, synthetic special keys
with EMPTY arrays — correctly skipped by the size-9 fingerprint).
Rejected alternatives: `Lpud.n` (general funnel), `Lpud.c`
(validates only), `Lpgs.<init>` (builds "default"/"recent_softkeys_"
lists, not fd2).

**Why the hook needs a Context at def-parse time:** the hook reads
SharedPreferences (`nobi_longpress_emoji`) to get the custom emojis.
Def-parse runs on a **background executor thread** (proven: `Lcwj.call`
insn 35 calls `Levj.a()` via `ModuleManager.newCreateModuleFutureTask`,
i.e. a FutureTask) during early process startup. See §12 for why the
Context was null.

---

## 12. The Context-timing problem (diagnosis-859/860/861)

**Diagnosis-859 (2026-11-15 ~10:09 IST):** the 858 chain is VERIFIED
correct in full, the fingerprint is VALID (A item[3] = 0x1F496), and
"`Lpud.m` never runs" is REFUTED. The ranked explanation for the 859
no-Toast verdict:
1. **LEADING — `ActivityThread.currentApplication()` is NULL at
   def-parse time** (null-guarded Toast skipped; in 858 the same call
   failing makes the hook go to GUARD → defaults). NOT proven
   statically — a hypothesis to test.
2. Toast fired but Sam didn't see it (P0 fires once at parse time;
   IME-path Toasts have NEVER been seen by Sam in this arc — 842 also
   "KUCH NHI aaya").
3. Sam didn't actually run 859 — RULED OUT by Sam ("SAB KIYA SB LATEST THA").
4. "Def not re-parsed" — REFUTED (cache is in-memory only; force-stop
   clears it).
5. "Fingerprint/prefs never match" — REFUTED as a no-Toast explanation
   (P0 fires before the fingerprint and before the prefs read).

**Diagnosis-860 (2026-11-15 ~11:30 IST):** ROOT CAUSE = two defects
combine into one single point of failure.
1. **Block L (the `last_run` write) only runs on the FIRST `Lpud.m`
   call per process — PROVEN.** BranchDump on shipped 860 `Lpud.m`
   (498 insns): insn 16 `IF_NEZ -> idx 70` skips insns 17–69 (Block L
   20–42 AND the P0 Toast 48–52) on every call after the first. The 860
   change report's claim that Block L writes `last_run` "on every
   def-parse" was WRONG — it is inside the `diagFirst` gate. `Lpud.m`
   is called dozens of times per def-parse (each letter key makes TWO
   calls), but Block L executes at most once.
2. **The Context is null on the first `Lpud.m` call (DEDUCED from proven
   premises).** On the first call, Block L's null-guard (insn 19
   `IF_EQZ -> idx 43`) silently skips the write when the Context (v5)
   is null; `diagFirst` is then set to true and Block L is never
   retried. `Lpud.m` IS called after force-stop + keyboard open; if v5
   were non-null on the first call, insns 20–42 would execute and
   `last_run` would be written; `last_run` is NOT in `nobi_diag`.
   Therefore v5 was null on the first call. The same null Context
   makes the substitution hook go to GUARD — one cause, both symptoms
   (no record + no customization).
**Structural flaw in the 860 diagnostic itself:** it **cannot record a
null Context** — writing SharedPreferences requires a Context, so if
the Context is null there is no way to report "ctx=null". "No record
yet" is exactly what a null Context produces. The diagnostic is blind
to its own failure mode.

**Checks already answered (do not re-investigate):**
- `Lesb.onCreate()` DOES run (manifest Application = `HiltLatinApp`
  → `Lesb` → `Lesf` → `Lmxa` → `Lpdy` → `android.app.Application`;
  `HiltLatinApp` declares only `<init>()V`; `Lesb.onCreate()` has the
  hook at idx 0 and calls `INVOKE_SUPER Lesf.onCreate()` at idx 8).
- IME and editor are in the SAME process (no `android:process` on
  either; `:train` is only for other services) — multi-process theory
  REFUTED.
- Def IS re-parsed after force-stop (in-memory cache only).
- The SharedPreferences write is correct IF the Context is non-null.
- Editor `refreshDiag()` is correct (called in `onCreate` + `onResume`).

**The 861 fix (and why it may have broken the keyboard):** 861 moved
Block L OUTSIDE the `diagFirst` gate (write on EVERY call — BranchDump
proved: insn 15 `IF_EQZ -> idx 39`, insn 32 `IF_EQZ -> idx 39`, insn 40
`IF_NEZ -> idx 66`) AND set `appCtx` EARLIER via
`Lmxa.attachBaseContext(Context)V` (5 insns: `SPUT_OBJECT v1,
Lpud->appCtx` + original 4 insns; the framework calls it BEFORE
`onCreate()`). The `attachBaseContext` hook is prime suspect #1 for
the 861 keyboard-not-opening: a `SPUT_OBJECT` into `Lpud` triggers
`Lpud.<clinit>` at Application attach time — before `onCreate()` and
before the app is fully loaded. If `<clinit>` throws, the Application
fails to attach → no IME process → keyboard never opens. In 860 the
same store ran inside `Lesb.onCreate()` (AFTER attach), so 860's hook
could not break attach.

---

## 13. The editor + prefs contract

- **Editor:** `com.nobi.nobiboard.NobiEmojiEditorActivity` (new Activity,
  in classes6.dex; source in `editor/src/`). Rebuilt with JDK 17 default
  (same as Phase 3, nest-based accessors — `-source 8 -target 8`
  produces `access$` accessors and CanonDiff 55 instead of 6).
  Features: header, 3-row QWERTY preview (customised keys show a green
  ● dot), popup preview (84dp circle, 3×3 grid, green circle behind
  centre emoji), 5 slot rows, picker dialog (search, category tabs,
  6-column grid, Clear → stores literal `U+200B`), Reset-this-key/all
  with confirm dialogs, auto-save. **Diagnostics section (since 860):**
  header + hint + record view, refreshed by `refreshDiag()` in
  `onCreate` and a new `onResume()` override; reads SharedPreferences
  `nobi_diag` (`last_run`, `last_a`).
- **Settings entry:** `res/xml/setting_preferences.xml`, **Appearance**
  category (category key `setting_appearance_category_key`, title string
  0x7f140b30 = "Appearance"), after EmojiScaleSliderPreference:
  `<androidx.preference.Preference title="Long-press emojis"
  summary="Choose which emojis appear when you long-press a letter key"
  key="nobi_longpress_emoji_editor">` with `<intent
  targetPackage="com.nobi.nobiboard"
  targetClass="com.nobi.nobiboard.NobiEmojiEditorActivity"/>`.
  **Navigation path: Settings → "Preferences" (tap it in the main list)
  → "Appearance" section → "Long-press emojis".**
  `AndroidManifest.xml`: `<activity exported="false"
  label="Long-press emojis"
  name="com.nobi.nobiboard.NobiEmojiEditorActivity"/>`.
- **Prefs contract:** SharedPreferences file **`nobi_longpress_emoji`**,
  keys **`emoji_A` … `emoji_Z`**, value = 5 parts joined by `,` in
  canonical order **`center,up,down,left,right`**; cleared slot =
  `U+200B`, NOT `""`. Verified from the editor source
  (`NobiEmojiEditorActivity.java`: `PREFS_NAME = "nobi_longpress_emoji"`;
  `prefKey(letterIdx)` returns `"emoji_" + letter`; `saveSlot` joins 5
  parts with `","` and saves via `prefs.edit().putString(prefKey,
  sb).apply()`; `BLANK = "\u200B"`).
- **Diag prefs:** SharedPreferences file **`nobi_diag`**, keys
  `last_run` = `"run ts=<currentTimeMillis>"` (every def-parse, since
  861 on every call) and `last_a` = `"A cp=1f496 ctx=<static|fallback|null>
  prefs=<value|null> outcome=SUB OK|GUARD"` (A-key only). **Caveat:**
  writing these requires a Context — if the Context is null, no record
  can be written, and the editor shows "No diagnostic record yet".

---

## 10. Where to look

- `smali/qmu.smali` — `Lqmu` (controller; `c`, `p`, `o`, `i`, `m`, `n`, `l`)
- `smali/qml.smali` — `Lqml` (base; `c`, `e`, `k`, `i`, `a`, `f`)
- `smali/qmr.smali` — `Lqmr` (tracker)
- `smali/qck.smali` — `Lqck` (`D(J)V` + the catch-all try)
- `smali/piy.smali` — `Lpiy` (long-press Runnable, case 0x2d)
- `smali/qme.smali` — `Lqme` (nearest-item search + AnimatorSet end)
- `smali/qmc.smali` — `Lqmc` (selected-item descriptor)
- `smali/qmf.smali` — `Lqmf` (cell-size helper)
- `smali/qms.smali` / `smali/qmt.smali` — animation listener / context
- `smali/tga.smali` — view helper
- `smali/pud.smali` — `Lpud` (def-parse hook; `m`, `c`, `e`, static fields `appCtx`/`diagCtxSrc`/`diagPrefs`/`diagFirst`)
- `smali/mxa.smali` — `Lmxa` (`attachBaseContext` hook)
- `smali/esb.smali` — `Lesb` (`onCreate` hook)
- `smali/com/google/android/libraries/inputmethod/popup/SilkRectangularPopupView.smali` — the popup view (`onFinishInflate` wires `Lqmu.h`)
- `dumps/` — dexdump excerpts of the same methods (positions stripped),
  including `Lqmu-c-861.txt`, `Lpud-m-861.txt`,
  `Lmxa-attachBaseContext-861.txt`, `Lesb-onCreate-861.txt`, plus the
  Fix5-era excerpts and `Lqck-D-fix5diag.txt` (the diag version of `D`,
  with the Toast/Log instrumentation)
- `change-reports/` — one change report per build from 851 to 861
- `diagnoses/` — `diagnosis-857.md`, `diagnosis-859.md`,
  `diagnosis-860.md`, `diagnosis-hook-851.md` (and `diagnosis-861.md`
  when it lands)
- `editor/src/` — the customization editor source
- `tools/src/` — the patch + verification tools (`TOOLS.md` for
  compile/run commands)

**Smali provenance:** disassembled with apktool 3.0.3 from classes2.dex
(classes.dex for `pud`/`mxa`/`esb`) of the shipped 861 APK (code
176071861, 105,632,334 B). The smali therefore contains the 861 patches:
`Lqmu.c()` is Fix11's exact 101-insn version (tracker-init to centre,
NO prefs hook); `Lpud.m` is the 494-insn def-parse hook version (Block
L' writes `last_run` on every call); `Lmxa.attachBaseContext` is the
5-insn hooked version; `Lesb.onCreate` is the 10-insn hooked version.
`Lqmu.o()`/`i()` are STOCK in this smali (the Fix5 centred versions
were removed in 856). The previous commit's smali was from the Fix5 APK
(176071841) and is replaced by this 861 extraction — see
`smali/README.md`.
