# NobiBoard — LongPress Fix5 Diag (code 176071842)

**Output:** `outputs/NobiBoard-LongPress-Fix5Diag.apk`
**Size:** 105,596,825 B
**SHA-256:** `2e10a797250221132c791c20201e5107bd80a3fddefab49e7ed64718804c52a5`
**versionCode:** 176071842 (aapt2 badging)
**Cert SHA-256:** `80:A9:47:FF:8B:5D:A7:4F:96:4A:F4:2C:A0:B9:6A:6C:EB:D5:48:68:AD:61:86:9D:6F:4F:88:05:86:B6:DA:7E` — **v2 MATCH**
**Entries:** 9,465 · **.so:** 14, **0 misaligned at 16 KB**

## What this build is

Diagnostic build, NOT a fix attempt. Identical to 176071841 (Fix5) except
for two logging points added to exactly one method, `Lqck.D(J)V` (the
long-press popup-creation entry). It surfaces on Sam's phone what happens
at long-press, so the next step is evidence, not a third blind patch.
Base = the shipped 176071841 classes2.dex, patched in ONE dexlib2 pass.

## Why a diag, not a Fix6 — the location-level comparison

Fix5 regressed exactly like Fix4 ("FIRSE POPUP AANE BAND HO GYE"), but
every gate says the Fix5 dex is clean. The parent's prime suspect — a
new merge conflict at the 01f6 jump → 0x288 merge in `Lqmu.i()` (v11 INT
on the jump path vs I64 double-high-half on the fall-through) — was
**RULED OUT by a location-level TypeMergeCheck comparison** (not counts):

- **Fix5 `Lqmu.i` conflict set == stock 839 `Lqmu.i` conflict set,
  location-for-location** (25 conflicts each; same join addresses modulo
  the +48 insertion shift, same registers, same type pairs). The 0x288
  merge has exactly v3/v7/v9 — the same three as stock's 0x258 merge.
  **No v11 conflict at 0x288, and no conflict anywhere that stock does
  not have.** The count-based check (25 = 25) was actually correct here;
  the location comparison confirms it.
- **Fix5 `Lqmu.o` = 0 conflicts = stock 0.**
- **Branch retarget verified correct against stock directly:** all 5
  crossing branches in `i()` are stock target + 48 (0160/0162/0166 →
  0x2cc, 017e → 0x2a6, 01f6 → 0x288). Not the defect.
- **Manual dump of the `i()` fall-through** (01f8 → 0x288): zero v11
  writes anywhere between 01f6 and 0x288; the block uses v19 (int-only),
  v8 (ref-only), v13 (int at every merge point); the FALLBACK merge at
  0x24a is type-clean on all four incoming paths.
- **Full 41-insn `Lqmu.o()` trace:** register remap, null/size fallbacks
  and tail all correct.

So no defect location is provable in the dex by any available gate
(RegInitCheck 0, VerifyPatch 0, VerifyBranches 0 bad, payload 0 odd,
TypeMergeCheck location-identical to stock, CanonDiff = only o() + i()
vs 839). A Fix6 would be a third blind patch. The diag is the right
next step: its `ERR` Toast will name the failure on the device, or its
absence will prove D is never entered (wrong/stale install).

## The instrumentation (exactly two places, in `Lqck.D` only)

Same design as the 176071837 diag (see that change report), re-based on
the Fix5 dex, with the marker string updated to the new code:

1. **Entry marker** (inserted after insn 1, the parameter moves):
   `Log.e("NobiDiag", "D-enter 176071842")` + a Toast with the same
   text. The Toast is null-guarded (SoftKeyView `this.m` null → skip;
   Context null → skip); Log.e always runs. Proves WHICH build is
   running and whether D is entered at all.
2. **Catch-handler log** (inserted after insn 322, `MOVE_EXCEPTION v0`,
   before the original `Trace.endSection` + `THROW v0`):
   `Log.e("NobiDiag", "ERR", throwable)` (3-arg form — full stack trace)
   + a Toast `"ERR " + throwable.toString()` (class + message). The
   caught Throwable stays in v0 for the original THROW.

Design constraints (same as 837, re-verified on the Fix5 dex):
- **No register-count bump** (registers stay 27). Scratch is v3/v4/v5,
  written before being read in both inserted blocks. `this` in the
  handler is read from the parameter register v24 (never written by D's
  body — the patch asserts this with a dest-register scan).
- **Both insertions are even** (entry 28 units, handler 52 units incl.
  a NOP pad) and neither changes any existing branch offset (every
  branch source and target sits after the entry insertion point, so both
  shift by the same +28). The patch recomputes every branch offset and
  ASSERTS each original offset is unchanged. Only the try block moves:
  start 9 → 37, handler 627 → 655, count stays 614.
- **Zero new refs.** All invoked methods already exist in classes2.dex
  (method_ids is full at 65536).
- **D runs on the UI thread**, so `Toast.show()` is legal here.

Nothing else changed: Fix5's centered `Lqmu.o()` + `Lqmu.i()` (v19
scratch, null-safe fallbacks), PLUS layout, circle background, tracker
init to center, full-cell direction pixels, stock 300ms glide, emoji
scale-up, caps suggestions, icons, caps toggle, emoji row, CrashFix,
Q–P numbers flag, the 26-key themed emoji sets, all three VerifyError
fixes.

## Verification (all on the FINAL signed APK)

| Check | Result |
|---|---|
| File size / SHA-256 | 105,596,825 B / `2e10a797…04c52a5` |
| Cert | v2 MATCH |
| versionCode | **176071842** (aapt2 badging), package `com.nobi.nobiboard` |
| Entries | 9,465 |
| .so 16 KB alignment | 14 .so, 0 misaligned (`zipalign -c -P 16 4` exit 0; direct data-offset check 0/14) |
| Unsigned-APK check (before signing) | classes2.dex md5 `836416c4…`, classes5.dex md5 `a78f9afe…` — matches the patched dexes, no stale-unsigned trap |
| CanonDiff vs 176071841 (classes2) | **exactly 1 method changed: `Lqck->D`**; 47,276 methods each |
| RegInitCheck (fixed) | `Lqck.D` 0, `Lqml.k` 0, `Lqmu.c` 0, `Lqmu.o` 0, `Lqml.e` 0, `Lqmu.i` 0 violations |
| VerifyPatch (`Lqck`, final dex) | 62 methods, 392 invokes, 0 violations |
| VerifyBranches | classes.dex 106,283 / classes2.dex 120,980 / classes3 37,110 / classes4 8,190 / classes5 34 / classes6 53 — **0 bad everywhere** |
| Payload alignment | classes2.dex 1,105 checked, 0 odd (0 odd in all six dex) |
| Res diff vs 176071841 | `res/` identical (9,297 entries), `resources.arsc` md5 identical (`3c764058…`) — no res change |
| Other dex | classes.dex / classes3 / classes4 / classes6 byte-identical to 841; classes5 differs only by the version bump (841 → 842, 3 use-sites) |
| TypeMergeCheck `Lqck.D` | NOT runnable — the checker crashes with `ArrayIndexOutOfBoundsException: Index 27` on methods with a wide (J) parameter (entry-state setup counts params, not register width). It crashes identically on the Fix5 dex and the diag dex, so it is a tool limitation, not a diag defect. The other gates above cover D. |

## Dump proof (final dex)

`Lqck.D(J)V` (27 regs, 712 units = 632 + 28 + 52): entry marker at
insns 2–15 (`const-string "D-enter 176071842"` + `Log.e` 2-arg +
null-guarded `Toast.makeText`/`show`); handler log after
`MOVE_EXCEPTION v0` at 0x28f (`const-string "ERR"` + `Log.e` 3-arg +
StringBuilder `"ERR " + throwable.toString()` + Toast); original
`Trace.endSection` at 0x2c4 + `THROW v0` at 0x2c7 (Throwable preserved
in v0); try 37/614/655.

## How to read Sam's result (interpretation table)

- **No Toast at all on long-press** → D is never entered (long-press
  never reaches the popup build, or the wrong build is installed —
  check the versionCode shown in Settings is 176071842).
- **`D-enter 176071842` Toast only** → D entered, no exception caught
  (the build completed but the popup is not shown = visibility problem,
  not a build failure). Also proves the phone IS running 842.
- **`D-enter` + `ERR <ExceptionClass>: <message>` Toasts** → the
  exception names the cause. A `VerifyError` naming a method = a static
  cause still exists in that method; any other exception → the throw
  site names the fix.

## Test asked of Sam

Install over (same v2 key), long-press any letter, and report (or
screenshot) EXACTLY which Toast(s) appear: nothing / `D-enter
176071842` only / `D-enter` + `ERR ...`. That one line settles it.
Fix3 (176071839) remains the current working build meanwhile.
