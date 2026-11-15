# Diagnosis-860: Why the 860 diagnostic record is NOT written

**Date:** 2026-11-15 ~11:30 IST
**APK examined:** `NobiBoard-CtxFix-176071860.apk` (versionCode 176071860), shipped, re-decoded + dex dumped.
**Sam's evidence:** screenshots `1000333153_3_e9ze.jpg` (11:24 IST) and `1000333154_4_qp89.jpg` (11:27 IST) — editor "Diagnostics" shows **"No record yet"** for both `Last keyboard load` and `Last A-key check`, even after force-stop + keyboard open. Editor DOES show his customization saved (Centre = 😂 HAHA, Up 🥰, Down 💕, Left 💖, Right 😘).

## TL;DR — root cause

**Two defects combine into a single point of failure:**

1. **Block L (the `last_run` write) only runs on the FIRST `Lpud.m` call per process — PROVEN.** BranchDump on the shipped 860 `Lpud.m`: insn 16 `IF_NEZ -> idx 70` jumps past insns 17–69 (Block L insns 20–42 AND the P0 Toast insns 48–52) on every call after the first. The 860 change report's claim that Block L writes `last_run` "on every def-parse" is **wrong** — Block L is inside the `diagFirst` gate. This is a real implementation defect in 860.

2. **The Context is null on the first `Lpud.m` call — DEDUCED (see §4).** On the first call, Block L's null-guard (insn 19 `IF_EQZ -> idx 43`) silently skips the write when the Context (v5) is null, `diagFirst` is set to true, and Block L is never retried on subsequent calls. The same null Context makes the substitution hook go to GUARD, so customization doesn't apply either. One cause, both symptoms.

**A third, structural flaw:** the diagnostic **cannot record a null Context** — writing SharedPreferences requires a Context, so if the Context is null there is no way for the diagnostic to report "ctx=null". "No record yet" is exactly what a null Context produces. The diagnostic as designed is blind to its own failure mode.

**Fix (see §6):**
- (a) Move Block L **outside** the `diagFirst` gate so the `last_run` write runs on **every** `Lpud.m` call (retried when the Context becomes available).
- (b) Set `Lpud->appCtx` **earlier** — hook `Lmxa.attachBaseContext(Context)V` (runs BEFORE `onCreate()`, receives the base Context in v1). Keep `Lesb.onCreate()` as a second hook and `currentApplication()` as the final fallback.
- (c) Do NOT touch `Lqmu.c` / `Lqml.c` (Sam's directive). The def-parse path is fixable; the popup-time read is the fallback (§7).

---

## 1. Does `Lesb.onCreate()` actually run? — YES (not the root cause)

**Manifest (re-decoded 860 APK, `AndroidManifest.xml` line 69):**
```
<application ... android:name="com.google.android.apps.inputmethod.latin.HiltLatinApp" ...>
```

**Inheritance chain (dexdump on shipped 860 classes.dex):**
```
HiltLatinApp  ->  Lesb  ->  Lesf  ->  Lmxa  ->  Lpdy  ->  android.app.Application
```
- `HiltLatinApp`: `Superclass: 'Lesb;'` (direct). Declares ONLY `<init>()V` — **no `onCreate()`**.
- `Lesb`: `Superclass: 'Lesf;'`. Declares `<init>`, `a()`, `b()`, `onCreate()`. No `attachBaseContext`.
- `Lesf`: `Superclass: 'Lmxa;'`.
- `Lmxa`: `Superclass: 'Lpdy;'`. Declares `attachBaseContext(Context)V` (see §6).
- `Lpdy`: `Superclass: 'Landroid/app/Application;'`.

**Shipped 860 `Lesb.onCreate()` (10 insns, DumpMethod):**
```
0: SPUT_OBJECT v1, Lpud;->appCtx:Landroid/content/Context;   <- the 860 hook (idx 0)
1: IGET_BOOLEAN v0, v1, Lesb;->g:Z
2: IF_NEZ                          (guard: if g already true -> idx 8)
3: CONST_4 v0, 1
4: IPUT_BOOLEAN v0, v1, Lesb;->g:Z
5: INVOKE_VIRTUAL {v1,...} Lesb;->b()Ljava/lang/Object;
6: MOVE_OBJECT v0, v1
7: CHECK_CAST v0, Lcom/google/android/apps/inputmethod/latin/HiltLatinApp;
8: INVOKE_SUPER {v1,...} Lesf;->onCreate()V
9: RETURN_VOID
```

**Verdict:** `Lesb.onCreate()` is the only `onCreate()` in the chain, `HiltLatinApp` extends `Lesb` directly, and the hook (`SPUT_OBJECT v1, Lpud->appCtx`) is present at idx 0. When the Application is created, `appCtx` IS set. **The hook is present and correct — this is NOT the root cause.** (Whether it runs *before* the first `Lpud.m` call is a timing question — see §4.)

## 2. Does the IME run in the same process as the editor? — YES (multi-process theory REFUTED)

**Manifest (re-decoded 860 APK):**
- LatinIME service (line 76): `<service android:directBootAware="true" android:exported="true" android:name="com.android.inputmethod.latin.LatinIME" android:permission="android.permission.BIND_INPUT_METHOD">` — **no `android:process`** → default process.
- NobiEmojiEditorActivity (line 132): `<activity android:exported="false" android:label="Long-press emojis" android:name="com.nobi.nobiboard.NobiEmojiEditorActivity"/>` — **no `android:process`** → default process.
- `android:process=":train"` — only `InAppTrainingService` (line 164) and `ExampleStoreServiceMultiplexer` (line 191) — background training services, not the IME or the editor.
- `android:process=""` (4×) — Phenotype services/receivers (empty = default process).
- `:primes_lifeboat` (LifeboatReceiver) and `:playcore_missing_splits_activity` (PlayCoreMissingSplitsActivity) — unrelated.

**Verdict:** the IME service and the editor Activity run in the **same default process**. A SharedPreferences write by the IME's def-parse IS visible to the editor (same process, same prefs file; `apply()` flushes to disk in seconds). **The multi-process / stale-cache theory is REFUTED — not the root cause.**

## 3. Does Block L in `Lpud.m` execute at def-parse time? — NO: first call ONLY (PROVEN DEFECT)

**Shipped 860 `Lpud.m` (498 insns, DumpMethod + BranchDump). Block E (Context) insns 0–13, Block L insns 14–52:**

```
 0: CONST_4 v0, 0
 1: SPUT_OBJECT v0, Lpud;->diagPrefs:Ljava/lang/String;
 2: SGET_OBJECT v5, Lpud;->appCtx:Landroid/content/Context;
 3: IF_NEZ -> idx 9                    (appCtx non-null -> "static")
 4: INVOKE_STATIC ActivityThread->currentApplication()
 5: MOVE_RESULT_OBJECT v5
 6: IF_EQZ -> idx 11                   (currentApplication null -> "null")
 7: CONST_STRING v11, "fallback"
 8: GOTO -> idx 12
 9: CONST_STRING v11, "static"
10: GOTO -> idx 12
11: CONST_STRING v11, "null"
12: SPUT_OBJECT v11, Lpud;->diagCtxSrc:Ljava/lang/String;
13: NOP
14: CONST_4 v2, -1                     (centre-CP sentinel)
15: SGET_BOOLEAN v0, Lpud;->diagFirst:Z
16: IF_NEZ -> idx 70                   *** THE DEFECT (see below) ***
17: CONST_4 v0, 1
18: SPUT_BOOLEAN v0, Lpud;->diagFirst:Z
19: IF_EQZ -> idx 43                   (v5 null -> skip write)
20: NEW_INSTANCE v6, StringBuilder     (build "run ts=<millis>" into v8)
...
30: MOVE_RESULT_OBJECT v8
31: CONST_4 v0, 0
32: CONST_4 v1, 0
33: CONST_STRING v11, "nobi_diag"
34: INVOKE_VIRTUAL {v5,v11,v0} Context->getSharedPreferences(String;I)
35: MOVE_RESULT_OBJECT v6
36: IF_EQZ -> idx 43                   (prefs null -> skip write)
37: INVOKE_VIRTUAL {v6} SharedPreferences->edit()
38: MOVE_RESULT_OBJECT v6
39: CONST_STRING v11, "last_run"
40: INVOKE_VIRTUAL {v6,v11,v8} Editor->putString(String,String)
41: MOVE_RESULT_OBJECT v6
42: INVOKE_VIRTUAL {v6} Editor->apply()
43: NOP
44: NOP
45: NOP
46: NOP
47: IF_EQZ -> idx 53                   (v5 null -> skip Toast)
48: CONST_STRING v11, "DEF 859: Lpud.m runs"
49: CONST_4 v1, 1
50: INVOKE_STATIC {v5,v11,v1} Toast->makeText(Context,CharSequence,I)
51: MOVE_RESULT_OBJECT v6
52: INVOKE_VIRTUAL {v6} Toast->show()
53: ARRAY_LENGTH v1, v13               (fingerprint check starts)
...
70: NOP                                 <- insn 16's target
71: ARRAY_LENGTH v1, v13
```

**BranchDump evidence (shipped 860 classes.dex, `Lpud.m`):**
```
16 addr=1b IF_NEZ -> idx 70 (addr 81)
19 addr=20 IF_EQZ -> idx 43 (addr 50)
36 addr=41 IF_EQZ -> idx 43 (addr 50)
47 addr=54 IF_EQZ -> idx 53 (addr 60)
```

**What this means:** on the **first** `Lpud.m` call per process, `diagFirst` is false, so insn 16 falls through, insns 17–18 set the flag, and Block L (insns 20–42) runs. On **every subsequent call**, `diagFirst` is true, so insn 16 jumps **directly to idx 70**, skipping insns 17–69 — Block L AND the P0 Toast. **`Lpud.m` is called dozens of times per def-parse (each letter key makes TWO calls — PRESS + LONG_PRESS), but Block L executes at most ONCE.**

**The 860 change report's claim is wrong:** it says Block L writes `last_run` "on every def-parse". It does not. It writes at most once per process — on the first call only. This is a real implementation defect in 860, and it is half of the root cause: if the first call doesn't write (Context null), there is no second chance.

## 4. Is the SharedPreferences write correct? — Correct IF Context non-null; null Context silently skips it

**Write path (Block L, insns 34–42):** `v5.getSharedPreferences("nobi_diag", 0)` → null-check (insn 36 → 43) → `edit().putString("last_run", v8).apply()`. File name `"nobi_diag"` and key `"last_run"` are correct and match the editor. `apply()` is async but flushes to disk within seconds — Sam opened the editor minutes after the keyboard, so timing is not the issue.

**The null-guard (insn 19):** `IF_EQZ -> idx 43` — if v5 (the Context) is null, the entire write is silently skipped. No exception, no log, no record. The same applies to the `last_a` write (insns 371–379, `W_SUB`/`W_GUARD`).

**The diagnostic cannot record a null Context.** Writing `nobi_diag` requires a Context (insn 34). If the Context is null, there is no mechanism to record "ctx=null" — the diagnostic is blind to its own failure mode. "No record yet" is exactly what a null Context produces.

**Deduction that the Context IS null on the first call** (not a guess — it follows from the proven premises):
- (a) `Lpud.m` IS called after force-stop + keyboard open (diagnosis-859 proved the chain reaches it; §5 confirms the def is re-parsed).
- (b) On the first call, Block L executes up to insn 19 (the Context null-check). If v5 were non-null, insns 20–42 would execute and `last_run` would be written.
- (c) `last_run` is NOT in `nobi_diag` (editor shows "No record yet").
- **Therefore v5 (the Context) was null on the first call.** The only way Block L skips the write is insn 19 (Context null) or insn 36 (prefs null — but `getSharedPreferences` returns an instance or throws, never null).

**Why is the Context null on the first call?** The def-parse runs on a **background executor thread** (`Lcwj.call` insn 35 → `Levj.a()` via `ModuleManager.newCreateModuleFutureTask`, per diagnosis-859) very early in process startup. On the first `Lpud.m` call, `appCtx` (set by `Lesb.onCreate()`) and `ActivityThread.currentApplication()` can both still be null — the background def-parse thread can reach `Lpud.m` before the main thread's `Application.onCreate()` has completed setting them up, or the first def-parse call is triggered by an early-init path (e.g. a ContentProvider, whose `onCreate()` runs **before** `Application.onCreate()`). The exact trigger is not provable statically; what is proven is that the Context IS null on that first call (by the deduction above), and that Block L's first-call-only gating makes it unrecoverable.

**The same null Context also explains the missing customization:** the substitution hook reads prefs through the same v5 Context. If v5 is null, the hook goes to GUARD (defaults) — which is exactly what Sam sees (popup shows defaults, centre = 😍 not 😂). One cause, both symptoms.

## 5. Is the def actually re-parsed after force-stop? — YES (in-memory cache only)

Diagnosis-859 established this and it stands: the keyboard-def cache is **in-memory only** (`Levj` / `IMEManager`, invalidated by the 13-int reload list in `Levj.a()`). Force-stop kills the process and clears it. `KeyboardDef` / `Lpva` are `Parcelable` but **no file-based def cache exists in the traced code** — no serialized def on disk, no prefs/file cache that survives a force-stop. `Levj.a()` (dumped from shipped 860): builds the reload list (`NEW_ARRAY [I`, `FILL_ARRAY_DATA`) and iterates `Lwhl.values()` — an in-memory registry. When Sam force-stops and opens the keyboard, the def IS re-parsed from the XML resources and `Lpud.m` IS called (for every key, twice). **Not the root cause.**

## 6. Recommended fix (def-parse path — keeps Sam's directive)

The fix has two parts. Both are in/around `Lpud.m`; **neither touches `Lqmu.c`, `Lqml.c`, or any popup-path method.**

### (a) Move Block L OUTSIDE the `diagFirst` gate (the critical fix)
Move the `last_run` write (insns 20–42) so it executes on **every** `Lpud.m` call, not just the first. Keep the P0 Toast gated by `diagFirst` (to avoid spamming Toasts), but the `last_run` write must not be gated — if the Context is null on the first call, a later call (after the Context becomes available) must be able to write it. Concretely: hoist the write out of the `IF_NEZ -> idx 70` region (e.g. place it after the fingerprint, guarded only by the v5 null-check, or restructure so insn 16 jumps past the Toast only, not the write). This alone makes the diagnostic observable again.

### (b) Set `appCtx` EARLIER — hook `Lmxa.attachBaseContext(Context)V`
`Lmxa.attachBaseContext(Context)V` (shipped 860, 4 insns, 2 regs) is in the chain (`Lesb` → `Lesf` → `Lmxa`) and runs **before** `onCreate()`. It receives the base Context as parameter v1:
```
0: INVOKE_SUPER {v0,v1} Lpdy;->attachBaseContext(Landroid/content/Context;)V
1: CONST_4 v1, 0
2: INVOKE_STATIC {v0,v1} Lvqt;->b(Landroid/content/Context;Z)Z
3: RETURN_VOID
```
Insert at idx 0: `SPUT_OBJECT v1, Lpud;->appCtx` (v1 = the base Context; or `SPUT_OBJECT v0` for `this`, the Application — either is a valid Context; prefer v1, the passed Context, as it is guaranteed non-null by the framework). Keep the `Lesb.onCreate()` hook as a second setter (belt and suspenders) and `ActivityThread.currentApplication()` as the final fallback in Block E. This makes a valid Context available at the earliest possible moment — before any `onCreate()`, before any def-parse, before any ContentProvider-triggered call.

**Verification for the fix build (all mandatory):** FieldCheckGen on `Lpud.m` AND `Lmxa.attachBaseContext` = 0 (use FieldCheckGen — stock FieldCheck silently skips non-7-param methods); RegInitCheck 0 new; VerifyPatch 0; VerifyBranches 0 bad; payload 0 odd; TypeMergeCheck differential vs 860 = same profile; RefResolve (filtered) = same 16-ref set; CanonDiff vs 860: classes.dex = ONLY `Lpud.m` + `Lmxa.attachBaseContext`, **classes2 = 0**; re-decode: versionCode bumped, digit fix kept, show_animation present, fd2 identical.

**Expected outcome for Sam:** after the fix, force-stop → open keyboard → editor Diagnostics shows `Last keyboard load: run ts=<number>` and `Last A-key check: A cp=1f496 ctx=static prefs=😂,🥰,💕,💖,😘 outcome=SUB OK`, and long-press A shows 😂 in the centre.

## 7. Sam's proposal — popup-time read (fallback, with its safeguards)

Sam proposed linking the popup directly to the customization (by default, the popup reads the prefs when it opens). **Assessment:** this has a real advantage — at popup-build time the Context is definitely available (the popup is built on the IME's UI thread, long after the Application exists, and `Lqmu.c()` has a Context via `this.f.b`). It does not depend on def-parse timing. **But it broke the popup 5 times** (Fix4 / Fix5 / Fix10 / Phase 3-851 / 857), so it must NOT be attempted without the specific safeguards below.

**If the def-parse fix (§6) fails, the popup-time read is the fallback — and it must be built as a FIXED 857-style hook:**
- **Hook point:** the LIST LOAD in `Lqml.c()` (insn 25, right after `IPUT_OBJECT v4, v0, Lqml->e`), NOT `Lqmu.c()`. `Lqmu.c()` then runs its exact stock/Fix11 code on the already-custom list — no label update, no view mutation during the build.
- **Safeguards (each one addresses a proven earlier regression):**
  1. **FieldCheckGen = 0** on `Lqml.c()` (all-dex load) — closes the Phase 3 defect class: every `IGET`/`IPUT` receiver must be narrowed by `INSTANCE_OF` + `IF_EQZ -> GUARD` (the 852 check-cast fix), because a field read from an `Object`-typed register is a VerifyError (the exact cause of 851's regression).
  2. **v13 colId fix** (diagnosis-857's proven defect): NEVER store a `MOVE_RESULT` into a live register — colId (v4/v13) must be preserved across the clone loop; every `MOVE_RESULT` destination must be a proven-dead scratch.
  3. **Empty-part guard** (diagnosis-857 Candidate 1): `split(",")` must give exactly 5 parts AND every part length > 0 before any `codePointAt` — otherwise GUARD.
  4. **Full gate set** (RegInitCheck, VerifyPatch, VerifyBranches, payload alignment, TypeMergeCheck differential, RefResolve filtered, CanonDiff) — all must pass on the final signed APK, with `Lqmu.c()` diff = 0.
  5. **A visible diag** (the editor Diagnostics section, or a Toast) confirming the hook ran and the outcome — so a failure is diagnosable, unlike 857's "no provable defect".

**Recommendation order:** try §6 first (smaller change, keeps the popup code untouched, keeps Sam's directive). Only if §6 still shows "No record yet" (or the customization still doesn't apply) should the popup-time read be built — with all five safeguards above, and with Sam's explicit agreement that it touches the popup list-load path.

## Appendix — evidence index
- Manifest application class: `AndroidManifest.xml` line 69 (`HiltLatinApp`).
- IME service: line 76 (no `android:process`). Editor activity: line 132 (no `android:process`).
- `HiltLatinApp` superclass `Lesb`, methods = `<init>` only (dexdump + ListMethods, classes.dex).
- `Lesb.onCreate()` 10 insns incl. hook at idx 0 (DumpMethod, shipped 860).
- `Lmxa.attachBaseContext` 4 insns (DumpMethod, shipped 860); `Lpdy.attachBaseContext` 4 insns.
- `Lpud.m` 498 insns (DumpMethod, shipped 860); BranchDump: insn 16 `IF_NEZ -> idx 70`; insn 19 `IF_EQZ -> idx 43`; insn 36 `IF_EQZ -> idx 43`; insn 47 `IF_EQZ -> idx 53`.
- Editor `refreshDiag()` (860 editor source, lines 397–413): reads `nobi_diag` `last_run`/`last_a`; called in `onCreate` (line 335) and `onResume` (line 341). Correct — the editor is not the problem.
- `Levj.a()` (DumpMethod, shipped 860): in-memory reload list (`NEW_ARRAY [I`, `FILL_ARRAY_DATA`), no disk cache.
- Sam's screenshots: `~/workspace/user/files/1000333153_3_e9ze.jpg`, `~/workspace/user/files/1000333154_4_qp89.jpg` (both "No record yet").
- Prior reports: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-defparse-diag2/outputs/diagnosis-859.md`; 860 change report at `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-defparse-ctxfix/outputs/change-report.md`.
