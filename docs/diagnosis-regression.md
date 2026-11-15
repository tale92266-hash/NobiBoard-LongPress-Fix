# Diagnosis — why the long-press popup no longer appears at all (Simple Animated, 176071835)

**Date:** 2026-11-12 IST. **Status:** root cause PROVEN from the shipped 176071835 dex + stock counter-examples. No build was made, nothing was patched.

## ROOT CAUSE (one sentence)

176071835 introduced **two independent `VerifyError` defects** in the patched dex — one in `Lqml.k()` and one in `Lqmu.o()` — so the popup controller classes (`Lqml` / `Lqmu`) **fail ART bytecode verification the first time a long-press tries to build the popup**; the `VerifyError` is **swallowed by a catch-all try block in the long-press popup-creation entry `Lqck.D`**, so no popup appears and there is no crash — the keyboard otherwise keeps working.

## Why this is a regression vs 176071834

176071834's popup OPENED (circle + plus layout visible). Its `k()` head called only
`Lqml->i(II)V` and `Lqml->a()Lpva;` (method declared on `Lqml`, object statically
`Lqml` — verifiable), and its `Lqmu.o()` was stock. Neither VerifyError existed.
Both defects below are NEW in 176071835, and both are in methods that run (or load)
at popup build / first touch — which is why the symptom changed from "popup opens
but selection/swipe don't work" to "popup does not appear at all".

## Defect 1 — `Lqml.k()` insn 88: subclass method invoked on a superclass-typed `this` (fires FIRST)

Final dex, `Lqml->k(F,F)Lpva;` (registers=13, `this` = v10, statically typed `Lqml`):

```
87: INVOKE_VIRTUAL {v10,v6,v7} Lqml;->i(II)V        // OK: i() declared on Lqml
88: INVOKE_VIRTUAL {v10}       Lqmu;->n()V          // <-- DEFECT
89: INVOKE_VIRTUAL {v10}       Lqml;->a()Lpva;      // OK: a() declared on Lqml
```

- `Lqmu.n()V` is declared **only on `Lqmu`** (a subclass of `Lqml`). `Lqml` itself
  declares a *different* method named `n` — `Lqml.n(FFFFFFI)Z` (the point-in-rect
  test) — not `n()V`.
- ART's verifier checks an `invoke-virtual` against the **static** type of the
  object register: the method's declaring class must be assignable from the
  object's type. `Lqmu` is NOT assignable from `Lqml` (a `Lqml` is not
  necessarily a `Lqmu`), so verification of `Lqml.k()` fails.
- The preceding guard does NOT save it: insns 26–27 are
  `INSTANCE_OF v0, v10, Lqmu; IF_EQZ -> STOCK`. The verifier does **not**
  narrow a register's static type from an `instance-of` result — only an
  explicit `CHECK_CAST` refines the type. At runtime `this` really is a
  `Lqmu` here, but the verifier only sees the static type `Lqml`, so it
  rejects the call.
- Stock counter-example (proves the rule): the only stock callers of
  `Lqmu.n()` in the whole dex are `Lqmu.i` (object = `this`, statically
  `Lqmu`) and `Lqms.onAnimationEnd`, which calls it on v2 loaded from field
  `Lqmt->g:Lqmu;` — i.e. a reference **statically typed `Lqmu`**. Stock code
  NEVER calls `Lqmu.n()` on a `Lqml`-typed reference. The patch's call is
  the only such call in the dex.
- Consequence: the **whole `Lqml` class fails verification** at first load.
  `Lqmu` extends `Lqml`, and superclasses are verified first, so this defect
  fires before anything in `Lqmu` is even checked.

## Defect 2 — `Lqmu.o()` insns 14 & 27: `setDuration(J)` invoked with register count 2 instead of 3 (masked by Defect 1, still fatal)

Final dex, `Lqmu->o(Lqmc;)V` (registers=9):

```
13: CONST_WIDE_16 v7, 120                       // long 120 -> v7 (low) + v8 (high)
14: INVOKE_VIRTUAL {v6,v7} ViewPropertyAnimator;->setDuration(J)   // <-- DEFECT: count=2
27: INVOKE_VIRTUAL {v6,v7} ViewPropertyAnimator;->setDuration(J)   // <-- DEFECT: count=2
```

- `setDuration(J)` takes a **long**: prototype needs 3 argument registers
  (`this` + long-low + long-high). Every stock call in the dex lists 3
  registers, e.g. `{v0,v1,v2}`, `{v2,v4,v5}`, `{v8,v3,v4}` (31 stock calls
  checked, all `regCount=3`). The patch's two calls list only 2 (`{v6,v7}`) —
  the high half (v8) is missing from the count, even though `CONST_WIDE_16 v7`
  correctly initialized v7/v8.
- ART's verifier requires the invoke's register count to match the method
  prototype (wide args count double). Count 2 vs required 3 is a malformed
  invoke → `VerifyError` in `Lqmu.o()`.
- This defect is currently **masked** by Defect 1 (superclass `Lqml` is
  verified first and fails first), but it would fire as soon as Defect 1 is
  fixed. Both must be fixed.

## How the failure plays out on the device (why: no popup, no crash)

1. Sam long-presses a letter. Gboard builds the Silk popup:
   `SilkRectangularPopupView.<init>` does `NEW Lqmu`, which triggers loading
   and verification of `Lqmu` and its superclass `Lqml`.
2. Verification of `Lqml` fails at `k()` insn 88 (`Lqmu->n()` on a
   `Lqml`-typed `this`) → `VerifyError`. (If that were fixed, verification of
   `Lqmu` would then fail at `o()` insns 14/27.)
3. The exception propagates up the popup-build chain into `Lqck.D` — the
   long-press popup-creation entry (325 insns) — which has **one catch-all
   try block covering almost the whole method** (start=9, len=614, handler
   type=null, i.e. catch-all). The `VerifyError` is swallowed there.
4. Result: the long-press silently does nothing. No popup, no crash, and the
   keyboard keeps working, because `Lqml`/`Lqmu` are only loaded for the
   more-keys popup. The class stays erroneous for the process lifetime, so
   every subsequent long-press fails the same way.
   (`SilkRectangularPopupView.b`, `Lqml.c`, `Lqmu.c`, `Lqmu.o`, `Lqml.k`
   themselves have NO try blocks — the swallow happens in `Lqck.D`.)

## What was EXONERATED (and why the task's initial reasoning was half right)

- **`Lqmu.c()` branch retarget (insn 26: 99 → 70) — NOT the cause.** Dumped
  from the final dex and diffed against 176071834: it is the ONLY change in
  `c()`. The init block (insns 70–98) is structurally sound: all guard
  failures branch to insn 99 (`p()`), success falls through to 99; it uses
  v0–v3 scratch (dead after the loop) and v6 = `this` (consistent with stock
  usage at insns 1, 16, 92, 99); its invokes are `Point.set(II)V` on
  `Point`-typed registers, `Character.codePointAt` (static), and `List`
  interface calls — no subclass-invoke and no wide-arg issue. It cannot throw
  on a real emoji popup, and it cannot prevent `p()` from running.
- **`Lqml.e()` — NOT the cause.** Byte-identical between 176071834 and
  176071835 (verified by dump diff).
- **`Lqml.k()` head otherwise — NOT the cause.** Every other invoke in the
  new head is on an exact or superclass type (`Lqml->i/a`, `List`,
  `Character.codePointAt`, `LinearLayout`/`View` getters); all guard
  branches target insn 93 (STOCK); direction GOTOs target insn 87 (SEL).
- **The task's premise "k() cannot prevent the popup from appearing" is
  WRONG in one respect:** `k()` indeed only *runs* on touch after the popup
  exists — but a verification defect *inside* `k()` kills the whole `Lqml`
  class at load time, before any popup can be built. That is the mechanism.

## Why the build's own checks missed it

`VerifyBranches` checks only branch targets (0 bad). `CheckPayloadAlignment`
checks only payload alignment (0 odd). `CanonDiff`/`CountRefs` compare
instructions and ref counts only. **None of them run ART's bytecode
verifier** — none check register counts against method prototypes, and none
check invoke assignability (subclass method on superclass-typed object).
A `VerifyError` is invisible to every check the build runs. The dex is
structurally valid (correct versionCode 176071835, cert v2 MATCH, entries
9,465, 14 `.so` 0 misaligned) — it is *semantically* unverifiable.

## What a fix must change (exact, minimal — no new machinery)

1. **`Lqml.k()` — make the `Lqmu->n()` call verifiable.** Insert
   `CHECK_CAST v10, Lqmu;` immediately after the `INSTANCE_OF` guard passes
   (after insn 27's fall-through, before the anchor code at insn 28). The
   guard already guarantees `this` IS a `Lqmu` at runtime, so the cast can
   never throw; it narrows v10's static type to `Lqmu`, and the invoke at
   insn 88 then verifies. (Alternative: delete the `n()` call entirely —
   the stock body already refreshes via `Lqmu.i`'s tail on one path — but
   the check-cast preserves the intended guarantee with a 1-instruction
   change. Do NOT keep the bare `INSTANCE_OF` guard as the only protection:
   it does not narrow the type for the verifier.)
2. **`Lqmu.o()` — fix both `setDuration` invokes to register count 3.**
   Change insns 14 and 27 to `INVOKE_VIRTUAL, 3, 6, 7, 8,
   ViewPropertyAnimator->setDuration(J)` — the long 120 is already in v7/v8
   via `CONST_WIDE_16 v7, 120`, and v8 is inside the method's 9 registers.
3. **Add a verifier-level check to the build gate** (so this class of defect
   can never ship again): for every invoke in every patched method, check
   (a) the register count matches the method prototype with wide args
   counting double, and (b) the object register's static type is assignable
   to the invoked method's declaring class (a subclass method on a
   superclass-typed object needs a preceding `CHECK_CAST`). A real ART
   verification pass (`dex2oat --verify` or equivalent) would be better
   still, if a runner is available.

## Runtime evidence that would confirm (not required — the static case is conclusive)

A logcat taken at the first long-press after installing 176071835 would show
a `VerifyError` naming `Lqml.k` (or, once that is fixed, `Lqmu.o`), thrown
from the popup-build path and swallowed in `Lqck.D`. Either defect alone is
fatal under ART's verifier rules, and both are new in 176071835 — which is
why the popup vanished exactly in this build.
