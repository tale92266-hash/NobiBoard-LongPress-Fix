# Diagnosis 2 — why the long-press popup STILL does not appear (Simple Animated Fix, 176071836)

**Date:** 2026-11-12 IST. **Status: NO static cause remains.** Trace-only, no build was made, nothing was patched.
**Verdict:** the 176071836 dex is clean by every gate available in this environment.
The remaining explanations are (1) the device is not actually running 836's code
(stale IME process / wrong build installed), or (2) a runtime failure in the
popup build path that only device evidence can reveal. See §5 for the exact
discriminator (no-build first step) and §6 for the minimal instrumentation.

## 0. Baseline (established, not re-proven)

- 176071834: popup OPENS. 176071835 and 176071836: popup does NOT open at all.
  Keyboard otherwise works; no crash.
- The two 835 VerifyError defects (`Lqml.k()` insn 88 `Lqmu->n()` on a
  `Lqml`-typed `this`; `Lqmu.o()` `setDuration(J)` count 2) are FIXED in 836
  and were independently dump-verified by the parent.
- A `VerifyError` during popup build is swallowed by a catch-all try in
  `Lqck.D` (try start=9, len=614, handler type=null) → no popup, no crash.

## 1. PRIME SUSPECT RE-EXAMINED — `Lqmu.c()` init block (insns 70–98): EXONERATED

The one behavioral difference vs 834 that survived the fix was the init block
running for the first time (834: loop-exit `IF_GE` → insn 99 `p()`; 835/836:
`IF_GE` → insn 70). I dumped `Lqmu.c()` from the FINAL 836 dex
(`DumpMethod2`, instruction indices) and diffed it against 834 — the ONLY
difference in the whole method is insn 26 (`IF_GE -> 99` vs `-> 70`) — and
walked the init block instruction-by-instruction with full operands
(dexdump, code offsets 0x8b–0xc2):

| Insn | Instruction | Why it cannot throw on a real emoji popup |
|---|---|---|
| 70 | `iget-object v0, v6, Lqml;->e:List` | `this` (v6) is never null; field `Lqml.e:List` EXISTS (verified in the field table). |
| 71 | `if-eqz v0, 00c3` | Null list → skip to `p()`. Guarded. |
| 72–75 | `v0.size()`; `if-ne v1, 9, 00c3` | `v0` non-null (guard 71); size != 9 → skip. |
| 76–78 | `v0.get(3)` | size == 9 proven by 75, so index 3 is in bounds. |
| 79–80 | `instance-of v2, v1, Lpva; if-eqz -> 00c3` | `null` item → `instance-of` false → skip. |
| 81 | `iget-object v2, v1, Lpva;->e:Object` | `v1` proven non-null `Lpva` (79–80); field `Lpva.e:Object` EXISTS (RefResolve, §2). |
| 82–83 | `instance-of v3, v2, CharSequence; if-eqz -> 00c3` | `null` value → false → skip. |
| 84–86 | `v2.length()`; `if-eqz v3, 00c3` | `v2` proven non-null `CharSequence`; empty → skip. |
| 87–89 | `Character.codePointAt(v2, 0)` | Length > 0 proven by 86, so index 0 is valid. |
| 90–91 | `const/high16 v2, 65536; if-lt v3, v2, 00c3` | Pure comparison, cannot throw; BMP char → skip. |
| 92–93 | `v0 = this.g:Lqmr; v1 = v0.b:Point` | `g` was already dereferenced at insn 17 (`v10.b`) — a null `g` would have thrown there, before the loop; `Lqmu.g:Lqmr` and `Lqmr.b:Point` EXIST. |
| 94–96 | `v1.set(1, 1)` | `b` was already dereferenced at insn 18 (`Point.set`) — proven non-null. |
| 97–98 | `v1 = v0.c:Point; v1.set(1, 1)` | `c` was already dereferenced at insn 20; `Lqmr.c:Point` EXISTS. |
| 99 | `invoke-direct {v6}, Lqmu;->p()V` | Every path (success and every guard failure) reaches here. |

Additional safety facts: the init block uses only scratch registers v0–v3
(dead after the loop) and `this` (v6); `Lqmu.c()` has **zero try blocks**
in both 834 and 836 (so the V26 try-block trap does not apply); the loop
body (insns 24–69) is stock and unchanged; `Lqml.e` is populated by the
`INVOKE_SUPER_RANGE` at insn 0 (the superclass `Lqml.c()` builds the popup
data list) before the init block runs — and even if it were stale or null,
every guard above still holds.

**Conclusion: the init block has no throw path. It cannot prevent `p()`
from running, and it cannot be the remaining cause.** The earlier claim
that it "cannot throw on a real emoji popup" stands, now proven with
operands instead of asserted.

## 2. THIRD VERIFICATION DEFECT? — None found (two independent gates, controlled vs 834)

### 2a. VerifyPatch full-class gate (register counts + invoke assignability)

Ran the project's `VerifyPatch` on **every method** of `Lqml`, `Lqmu`,
`Lqck`, `Lqmr` in the final 836 dex (filters `Lqml;` / `Lqmu;` / `Lqck;` /
`Lqmr;` — note: the filter must include the `;`, a bare `Lqml->` matches
nothing because the class string is `Lqml;->k`):

- **836: methods=94, invokes=746, violations=0.**
- Control run on 835 with the same filters catches exactly the 3 known
  defects, so the gate is proven sensitive to this defect class.

### 2b. RefResolve — NEW tool (`~/workspace/diag836-2/RefResolve.java`)

VerifyPatch only checks invokes. This tool checks that every **field /
method / type reference** used by those four classes resolves to a real
declaration, walking superclasses and interfaces, across **all six dex
files** of the APK (a class in classes2.dex may reference a class defined
in classes.dex). References whose declaring class is not in any dex
(framework classes) are reported as EXTERNAL, never guessed.

- **836: checked=1693 refs, external(framework)=384, unresolved=43.**
- **834 control: checked=1684, external=378, unresolved=43 — the SAME 43.**
  All 43 are the same pre-existing false positives of the resolver (it
  cannot prove methods inherited from a *framework* superclass, e.g.
  `View.isPressed/setPressed/isEnabled/getId`, `Future.isDone/cancel`,
  `Enum.ordinal`, `List.size/get` via interface). They exist in the
  working 834 build, so they cannot explain the regression.
- **Zero NEW unresolved refs in 836.** In particular, every reference in
  the patched methods resolves: `Lqml.e:List`, `Lqmu.g:Lqmr`,
  `Lqmr.b/c:Point`, `Lqmr.d:List`, `Lpva.e:Object`, `Lqmo.d:Lqmn`,
  `Lqmu.f:Lqmo`, `Lqmn.f/d:I`, `Lqmn.d()/c()Z`, `Lqmr.d()/c()I`,
  `Lqmc.<init>(Point, I, View, View)`, `Lqmu.p()/n()V`,
  `Lqml.i(II)V`, `Lqml.a()Lpva`.

**Conclusion: no third verification defect exists in `Lqml`, `Lqmu`,
`Lqck` or `Lqmr` by either gate. Any real defect in ANY method of `Lqml`
or `Lqmu` would fail the whole class at load — and none is present.**

## 3. CHECK_CAST INSERTION — re-verified clean

`Lqml.k()` in the final 836 dex (`DumpMethod2`, dexdump cross-check):

- Head is **168 code units (even)**; STOCK starts at insn 94 (abs off 168).
- Insn 26 `INSTANCE_OF v0, v10, Lqmu;` → 27 `IF_EQZ -> 94 (STOCK)` →
  **28 `CHECK_CAST v10, Lqmu;`** → 29 anchor `IGET v1, v10, Lqml;->i:F`.
- All eight guards (insns 1, 5, 10, 13, 16, 21, 25, 27) target insn 94;
  all four direction GOTOs (70, 75, 80, 85) target insn 88 (SEL);
  insn 88 `Lqml->i(II)`, 89 `Lqmu->n()` (now on a `Lqmu`-typed v10),
  90 `Lqml->a()`; insn 93 is a NOP pad (keeps the head even).
- Insns 33–34 (`if-nez v1` / `if-eqz v3`) test INT registers produced by
  `FLOAT_TO_INT` at 30/32 — no float-comparison verifier issue.
- No later use of v10 requires it to be exactly `Lqml`: the later
  `IGET`s are `Lqml` fields (legal through the subclass) and the invokes
  are `Lqml->i/a` (superclass methods) plus the now-legal `Lqmu->n()`.
- `registers=13` unchanged; **try blocks = 0** in both 834 and 836.
- `VerifyBranches` on the final dex: **120,969 checked, 0 bad.**
  `CheckPayloadAlignment`: **1,105 payloads, 0 odd.**

**Conclusion: the CHECK_CAST insertion broke nothing.**

## 4. ANY OTHER BEHAVIORAL DIFFERENCE vs 834? — No

`CanonDiff` 836 vs 834, **all dex files**:

| Dex | Result |
|---|---|
| classes.dex | 0 diff (46,053 methods) |
| classes2.dex | **exactly 3 changed methods: `Lqml->k`, `Lqmu->c`, `Lqmu->o`** (47,276 methods) |
| classes3.dex | 0 diff (17,672) |
| classes4.dex | 0 diff (4,713) |
| classes5.dex | 1 changed method — `CrashReporter$Handler.buildReport`: only the version string `176071834` → `176071836` (expected, irrelevant) |
| classes6.dex | 0 diff (11) |

APK content diff 834 vs 836 (excluding dex): `res/` identical,
`resources.arsc` identical, assets/lib identical; `AndroidManifest.xml`
XML tree identical (versionCode differs, as expected, verified by badging);
only `META-INF/*` differ (signatures, expected).

**Conclusion: the only behavioral differences vs the working 834 are the
three intended patch changes — and §1–§3 prove each of them safe.**

## 5. What remains — the two live hypotheses (both need the device, not the dex)

Since the dex is statically clean, the cause is on the device side:

### Hypothesis A (most likely, cheapest to test): the phone is not actually running 836's code

- **Stale IME process:** the diagnosis of 835 established that an
  erroneous class "stays erroneous for the process lifetime". If 836 was
  installed over 835 while the keyboard process was alive and the process
  was never killed, the running process still holds **835's erroneous
  `Lqml` class** — the symptom would be identical to 835 even though the
  APK on disk is fixed. (Android normally kills the app process on update,
  but IME processes bound by the system are a known edge case.)
- **Wrong build installed:** the 836 install-over could have failed
  silently, or the 835 link could have been re-tapped — Sam would still
  be on 835 with the identical symptom.

**No-build discriminator (do this FIRST, before any diag build):**
1. Force-stop NobiBoard (Settings → Apps → NobiBoard → Force stop).
2. Uninstall NobiBoard completely.
3. Install 176071836 fresh from the Drive link
   (`1MGr3vXjIFnQXCC8p_L5bQ-WWDNCpX4Kp`).
4. Optionally reboot the phone (guarantees no stale IME process survives).
5. Long-press `A`.
If the popup appears, Hypothesis A is confirmed and the saga is over.
If it still does not appear, Hypothesis A is ruled out and only B remains.

### Hypothesis B: a runtime failure in the popup build path, invisible statically

Something in the stock build chain throws at runtime on Sam's device
(a null in `Lqck.D`, a view-inflation failure, an exception in
`SilkRectangularPopupView`'s constructor, …). Static analysis cannot see
it because nothing in the build chain changed between 834 and 836
(`Lqck.D` is byte-identical) — yet the symptom changed exactly when the
patches landed, which is why A must be ruled out first.

## 6. Minimal instrumentation that would settle it (one diag build, if §5 fails)

A diag build otherwise **identical to 836** (same code, versionCode bump
only) with logging added in exactly two places:

1. **`Lqck.D` entry marker + catch-handler log.** `Lqck.D` is the
   long-press popup-creation entry (325 insns) with the catch-all try
   (start=9, len=614, handler type=null). Add:
   - at method entry: log `"D-enter <versionCode>"` — proves WHICH build
     is actually running on the device (settles Hypothesis A in one line)
     and whether `D` is entered at all;
   - in the catch handler: log the caught `Throwable`'s class + message +
     first ~10 stack frames (e.g. `Log.e` + write to the DiagLogger
     target, or a Toast with the exception class name).
2. **Interpretation of the log:**
   - No `D-enter` line at all → the long-press never reaches the popup
     build (wrong build installed, or the key's long-press action is not
     wired) — a different problem from any patch.
   - `D-enter` + a `VerifyError` naming a method → a static cause still
     exists in that method; re-examine it with the method name in hand.
   - `D-enter` + another exception (NPE, etc.) → the throw site names the
     fix.
   - `D-enter` + no exception caught → the build completed but the popup
     is not shown (visibility/positioning problem, not a build failure) —
     investigate the view layer, not the dex.

**Do NOT build another blind patch.** The next build must be this diag
build (or none at all until §5 is tried).

## 7. Evidence index (all under `~/workspace/diag836-2/`)

- `lqmu-c-836-idx.txt`, `lqmu-c-834-idx.txt` — `Lqmu.c()` with instruction
  indices; diff = insn 26 only (`IF_GE -> 99` vs `-> 70`).
- `lqml-k-836-idx.txt` — `Lqml.k()` head (CHECK_CAST at 28, guards → 94,
  GOTOs → 88, NOP at 93).
- `lqmu-o-836-idx.txt`, `lqmu-o-834-idx.txt` — `Lqmu.o()` (stock vs
  animate; both `setDuration {v6,v7,v8}` in 836).
- `verifypatch-836-4classes.txt` — VerifyPatch on Lqml/Lqmu/Lqck/Lqmr:
  94 methods, 746 invokes, 0 violations.
- `refresolve-836.txt`, `refresolve-834.txt` — RefResolve: identical 43
  (all pre-existing framework-inherited false positives); 0 new in 836.
- `RefResolve.java`, `TryBlocks.java` — the tools written for this check.
- `c2-836.dump`, `c2-834.dump` — full dexdump of classes2.dex (both).
- `manifest-834.txt`, `manifest-836.txt` — identical XML trees.
- `dex836/`, `dex834/`, `apk836/`, `apk834/` — extracted APK contents.
