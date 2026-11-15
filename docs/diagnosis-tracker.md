# Diagnosis — why swipe does nothing on long-press (Phase 2Fix3, 176071832)

**Date:** 2026-11-12 IST. **Status:** root cause PROVEN from the shipped dex + resources + Sam's screenshot. No build was made, nothing was patched.

## ROOT CAUSE (one sentence)

Sam's letter long-press popup is the **Silk popup** (`SilkRectangularPopupView`, controller `Lqmu`), whose visible selection (the green circle) and whose inserted value are **both driven by the `Lqmr` spatial tracker — not by `Lqml.g` or the pressed state** — and all three `Lqml.k()` hook heads (Phase 2, 2Fix2, 2Fix3) early-return after setting only `g` + the pressed state, **bypassing the stock `k()` body, which is the ONLY caller of `Lqmu.i()` — the only method that ever updates the tracker**. The tracker is therefore frozen at its build-time initial point (bottom-middle) for the whole lifetime of the popup: the green circle can never move, and the inserted item can never change, no matter what the hook computes.

**Phase 0 is the control experiment that proves it:** Phase 0 (176071826) had NO `k()` hook at all, so the stock `k()` body ran, `Lqmu.i()` updated the tracker, and Sam's verdict was "Working perfect". Every build since Phase 1 added a `k()` head that early-returns — and every one of them has had a dead swipe.

---

## Evidence chain

### 1. The popup on Sam's phone is the Silk popup, not the Material popup

`Lqmu` EXTENDS `Lqml` (`super=Lqml;`). `SilkRectangularPopupView.<init>` does `NEW Lqmu;`; `MaterialRectangularPopupView.<init>` and `RectangularPopupView.<init>` do `NEW Lqml;`. Both Silk layouts exist in the APK:

- `0x7f0e061e.xml` — root `SilkRectangularPopupView`, `balanced_placement="false" first_key_as_default="false"`, text item `0x7f0e0646`.
- `0x7f0e0624.xml` — root `SilkRectangularPopupView`, `balanced_placement="true"` (no `first_key_as_default`), text item `0x7f0e0640`.
- `0x7f0e0620.xml` — root `MaterialRectangularPopupView`, `balanced_placement="true"`, text item `0x7f0e063b`.

Three independent on-device observations are all **predicted by Silk and all contradicted by Material**:

| Observation (Sam) | Silk prediction | Material prediction |
|---|---|---|
| Green highlight is on the **BOTTOM** emoji at open | Tracker init = bottom row (see §3) | `e()` post-block sets `g=3` + presses the center item → center would be green |
| Highlight **never moves** on swipe | `k()` head never touches the tracker/indicator | `k()` head's `h(view)` sets the pressed state, which IS Material's visible green |
| Swipe **inserts nothing different** | `Lqmu.a()` returns the tracker item, unchanged | `Lqml.a()` = `e.get(g)` — the head's `h()` sets `g`, so the insert would follow the swipe |

The screenshot `1000331611_19_k1vt.jpg` also shows the green as a **separate circular badge** sitting on the bottom emoji (😆), not a cell-filling background — the shape of the Silk indicator view (§2), not a pressed item background.

### 2. The green circle is the Silk indicator view `Lqmu.h`, not a pressed state

- `Lqmu.h` (indicator `View`) = `findViewById(0x7f0b0709)` in `SilkRectangularPopupView.onFinishInflate`. That view's style `0x7f15063e` gives it background drawable `0x7f080316`: a rounded rectangle, solid color `@color/0x7f0605ef` = **`#ff4db6ac`** (mint-green-teal) — the exact color of the green circle in Sam's screenshot, size `?0x7f0402a6`.
- `Lqmu.p()` (20 insns) does `Lmgw.I(Lqmd, Lqmr.b)` → current `Lqmc` → `m(Lqmc)` (scales the item view) + `o(Lqmc)` (moves indicator `h` to the item's position via `setTranslationX/Y`, makes it visible if `this.a` else invisible). **Callers of `p()`: only `Lqmu.c` and `Lqmu.e`. `k()` never calls `p()` or `o()`.**
- The Silk text items (`0x7f0e0640`, `0x7f0e0646`) have **no `android:background` at all**; their only foreground is drawable `0x7f08030f`, a `FocusRingDrawable` with a **transparent** solid. So `Lqml.m(view, true)` (`view.setPressed(true)`) is **invisible** on a Silk item. (For contrast, the Material item `0x7f0e063b` has background selector `0x7f080317` → pressed → `0x7f080318`, also `#ff4db6ac` — same color, which is why color alone does not discriminate; behavior does, §1.)
- The inserted value is equally tracker-driven: `Lqmu.a()` (17 insns) returns `e.get(Lqmc.b)` via `Lmgw.K(Lqmr.g)` — it **ignores `Lqml.g` entirely**. `Lqml.a()` (= `e.get(g)`) is the Material behavior; it is overridden for Silk.

### 3. At open, the tracker starts on the BOTTOM row — that is why the green is on DOWN

- `Lqmu.c()` (72 insns) sets `Lqmr.b`/`Lqmr.c` Points to (`Lqmn.f`, `0` or `Lqmn.d - 1`), then iterates the popup child views building `Lqmc` entries into `Lqmr.d`; ends calling `p()`.
- Grid for G's popup (9 items, `max_item_per_row` = 3): `Lqml.c()` insns 75–82 compute `Lqmn.d = (9-1)/3 + 1 = 3` (rows), `Lqmn.e = (9-1)/3 + 1 = 3` (cols).
- `Lqmn.f` is set in `Lqml.c()` insns 330–339: `Lqmm.a() ? 0 : (e-1)/2`. `Lqmm.a()` = `balanced_placement && first_key_as_default` — false for both Silk layouts (061e: both false; 0624: balanced=true but `first_key_as_default` absent/false). So `f = (3-1)/2 = 1`.
- `Lqmn.l` defaults to 2, so `Lqmn.d()` (`l==1`) and `Lqmn.c()` (`l==3`) are both false, and the tracker's row = `Lqmn.d - 1 = 2`.
- **Tracker initial point = (col 1, row 2) = bottom-middle cell = the DOWN emoji** (data index 0 in the PLUS layout). `p()` then puts the green indicator exactly there. This is the whole explanation for "highlight lands on the BOTTOM at open" — no finger position is involved.

### 4. The tracker is updated ONLY by `Lqmu.i()`, which is called ONLY from the stock `k()` body — and every hook head bypasses it

- `Lqmu.i(I,I)` (383 insns) is the spatial selection update: it reads the tracker, runs a nearest-item search (`Lqme`/`Lqmt`), and animates the change (`ObjectAnimator` SCALE_X/SCALE_Y on the old/new item views, insns 220–258). It has **zero direct callers in any dex**; it is reached only virtually, from the stock `Lqml.k()` body insn 260: `INVOKE_VIRTUAL {v10,v12,v11,} Lqml;->i(II)V` (virtual dispatch → `Lqmu.i` for Silk instances).
- The stock `k()` body (insns 76–274) computes row/col via `Lqml.n(FFFFFFI)` from `Lqmn` geometry and `this.i`/`this.b`, calls `this.i(row, col)`, then `this.a()`.
- All three hook heads (Phase 2 freeze-on-center, Phase 2Fix2 absolute-position direction, Phase 2Fix3 relative-delta) take the same shape: compute an index → `h(view)` (sets `Lqml.g` + pressed state) → `return a()`. **They return before insn 76, so `Lqmu.i()` never runs for an emoji popup.** The tracker's point, the indicator's position, and `a()`'s return value are all frozen at the build-time bottom-middle point.

### 5. `this.i` / `this.b` ARE the finger's start point — the Phase 2Fix3 anchor model was correct, and irrelevant

- `Lqml` fields: `a:Z, b:F, c:LinearLayout, d:List, e:List, f:Lqmo, g:I, h:I, i:F, j:Lrfp, k:Ligp`.
- `Lqml.c()` insns 20–23: `this.i` = first float param, `this.b` = second float param. `Lqck.D` (325 insns) passes `Lqck.d` (current touch X) and `Lqck.e` (current touch Y) as those floats to `Ltdt.b` (insns 111–112, 125, invoke at insn 228). So `this.i`/`this.b` **are the touch point at long-press fire time**. The relative-delta head's anchor was real.
- It does not matter, because the head's output (`g` + pressed state) is not what the Silk popup displays or inserts. A perfectly computed delta still writes to a variable nobody reads on this popup.

### 6. The touch path DOES reach `k()` — that was never the break

- `BasicMotionEventHandler.g` / `Lqch.h` (handleActionMove) → `Lqmi.c(FFZ)` → `Ltdt.a` → popupView.`a()` → `Lqml.k`. `Lqmi.c` (12 insns) forwards (x,y) only if `Lqmi.b()` is true (popup view non-null AND `isShown()` AND `Ltdt.g()` true; Material `g()` returns constant 1).
- `Lqch.h` forwards to `Lqmi.c` only when the key view under the finger `m` is UNCHANGED (`m == original m`); `Lqck.t()` freezes `m` when the popup is shown AND (`SoftKeyView.g` OR `Ltdt.h()`). `SoftKeyView.t()` sets `g=true` (called by `Lks.E`, `Lphe.a`, `Ltee.b` — the key-press paths). `Ltdt.h()` = `Lqml.j()` = (`g == -1` AND `!Lrfp.d()`) — false once `e()` sets `g=3`, so during a swipe the freeze relies on the `SoftKeyView.g` gate, not on `h()`.
- So the touch reaches `k()` during the swipe; the hook fires, computes the right direction, and writes it into state that the Silk popup never displays. The break is downstream of `k()`, not upstream.

### 7. Hooks verified present in the SHIPPED 176071832 dex (not from project sources)

- `Lqml` exists in **exactly one dex** (`classes2.dex`) — no duplicate class, no stale copy anywhere.
- `Lqml.e()` post-block present in the final dex (insns 64–92): guards `e.size()==9`, item[3] is an `Lpva` with non-empty `CharSequence`, `codePointAt >= 0x10000`, `this.d` non-empty → sets `v4=3`, `m(d.get(3), this.a)`, `g=3, h=3`. For G's popup (9 items, item[3] = 😂 U+1F602, non-BMP, `d` non-empty) **the guard PASSES and `g=3` IS set — yet the screenshot shows the green on the bottom**. This is the cleanest single proof that `g` is not the visible selection.
- `Lqml.k()` 132-unit delta head present in the final dex with correct branch targets (verified by dump from the APK's `classes2.dex`).
- `Lqml.c()` ends at insn 600 with `INVOKE_VIRTUAL e()` — so `e()` runs at popup build for all popups. (Note: `SilkRectangularPopupView.c(I)` is EMPTY — `e()` is never re-called via the animation-end path for Silk; `MaterialRectangularPopupView.c(I)` calls `e()` when arg != 0. Either way, `e()` runs once at build, sets `g=3`, and then `Lqmu.e()`'s trailing `p()` re-positions the indicator from the tracker — bottom — overwriting any visual effect of the `g=3` press.)

### 8. Guard conditions for G's popup: they PASS — the failure is downstream of the guards

9 items, item[3] = 😂 (U+1F602, non-BMP, so the `>= 0x10000` check passes), `this.d` non-empty. Both the `e()` and `k()` guards pass at runtime. Nothing about the merged digit or the item count defeats them (the 10-item top-row problem from Phase 2 was already fixed by removing the digit merge). The hooks fire; their output goes nowhere visible.

### 9. Installed-app / stale-build theory: RULED OUT

- Sam confirmed at 08:59 IST 2026-11-12 that the problem is in the LATEST build too.
- The APK's `versionCode` is 176071832; `Lqml` is in exactly one dex; the hooks are present in that dex. There is no second, unpatched `classes2.dex` in the APK and no split-APK structure that could load one. The phone is running the patched code — the patched code just patches the wrong variable for this popup.

---

## Which earlier theories were wrong, and why

- **Theory (A) — Phase 2Fix3: "absolute-position mapping makes DOWN dominate because the finger rests ~51dp below the popup"**: WRONG as the mechanism. The finger's absolute position never enters the selection on Silk: the visible selection is the tracker, and the tracker is never updated by touch (the head bypasses the stock body). The resting-zone measurement (localY 146.5dp vs zone [95.1,126.8)) was real geometry but irrelevant — even a perfect resting zone would not move the indicator, because the indicator is not driven by `k()`'s output. The DOWN-at-open is explained by the tracker init Point(1,2), not by dy=+99dp.
- **Theory (B) — "hooks not firing"**: WRONG. The hooks are in the final dex, the touch reaches `k()`, and the `e()` guard passes and sets `g=3`. They fire and compute correctly; their output is written to state the Silk popup does not display.
- **Phase 2Fix2 theory — "stock `e()` body overwrites v4 with `Lqmn.f` because balanced+first_key_as_default are false"**: the overwrite is real, and the post-block does fix `g` — but `g` is the wrong variable on Silk. The post-block fixed a value nobody reads.
- **Phase 1's "empty-token null" diagnosis** (Phase 1Fix): that one was CORRECT for its own symptom (no popup at all) and is untouched by this finding.

## What a fix would have to change (requirement, not a patch)

The fix must drive the **`Lqmr` tracker** — it is the only state the Silk popup both displays and inserts from. Concretely, in `Lqmu` (not `Lqml`):

1. **At open — initialize the tracker to the CENTER cell (1,1), not (1,2).** Hook where `Lqmu.c()` sets `Lqmr.b`/`Lqmr.c` (or the `Lqmn.f` / row inputs it derives them from), so the tracker's initial point is the center; `p()` will then put the green indicator on the center at open.
2. **On touch — route the direction through `Lqmu.i(col, row)`** (or equivalently update `Lqmr.b`/`Lqmr.c` and call `p()`), so the indicator moves AND `a()` returns the swiped item. The direction computation itself (relative delta from the initial touch) is already correct and can be kept — only its *destination* must change from `h(view)`/`g` to the tracker.
3. **Do NOT early-return out of `k()` for emoji popups without replacing the `Lqmu.i` call** — the stock `k()` body is the only existing caller of the tracker update; bypassing it is what froze the selection. Any head that keeps the early return must itself perform the tracker update.

Patching `Lqml.k()`'s head to set `g` / the pressed state can never work on this popup, in any form, because on Silk neither `g` nor the pressed state is visible or returned.

## Runtime evidence that would settle any remaining doubt (not required to proceed)

The static case is complete, but a single diag log would confirm it on-device in one run: log (a) which popup class is instantiated for a letter long-press (`SilkRectangularPopupView` vs `MaterialRectangularPopupView`), (b) `Lqmr.b`/`Lqmr.c` at open, and (c) whether `Lqmu.i()` is entered during a swipe. If (a) is Silk, (b) is (1,2), and (c) is never — the diagnosis is confirmed at runtime. No new build is needed to obtain this: the existing crash-handler/diag path could log it, or it can be read from a single instrumented run.
