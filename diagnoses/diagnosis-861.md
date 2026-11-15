# DIAGNOSIS-861 — Why 176071861 breaks the keyboard so it does not open at all

**Date:** 2026-11-15 ~12:00 IST (written after Sam's 11:44 IST report)
**Builds:** 861 (broken, keyboard never opens) vs 860 (control, keyboard opens, customization does not reflect)
**Report type:** diagnosis ONLY — no APK built, no APK modified, no upload, no message to Sam, no GitHub repo.
**Project dir:** `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-defparse-diag4/`
**Scratch / dumps:** `verify/` (`new/` = 861 dexes, `base/` = 860 dexes, `lpud-m-861.txt`, `lpud-m-861-full.txt`, `lpud-m-860.txt`, `lpwz-f-861.txt`, `lcwj-call-full.txt`, `lcwj-call.txt`, `levj-a.txt`, `lpxa-a.txt`, `lpvb-h-861.txt`, `lsah-t.txt`, `llgy-ce.txt`, `lptz-a.txt`, `lpwz-h-861.txt`, `mm-b.txt`, `mm-loadmodule.txt`, `lxiv-clinit.txt`, `dexdump-classes.txt`, `dexdump-d-classes.txt` (classes.dex only), `FullDump.java`).

---

## 1. Executive summary — ROOT CAUSE

**The P0 Toast in `Lpud.m` throws on the def-parse thread and kills the keyboard-definition load.**

861 is the first build in which the `Lpud.m` diagnostic Toasts actually FIRE on the phone (in 858/859/860 they were always skipped because the Context was null on the first def-parse call). The P0 Toast (`"DEF 859: Lpud.m runs"`, insns 44–48) fires on the **first** `Lpud.m` call of the process. `Toast.makeText()` on a thread that has no `Looper` throws `NullPointerException: "Can't toast on a thread that has not called Looper.prepare()"` (proven from AOSP `Toast.java`, §4 below). `Lpud.m` has **0 tries**, so the throw propagates out of the def-parse (`Lpud.m` → `Lpwz.f` (0 tries) → `Lpxa.a` → `Levj.a` (0 tries) → `Lcwj.call`, which rethrows) and the module-load `FutureTask` completes exceptionally. `ModuleInfo.loadModule()` catches the resulting `ExecutionException`, logs `"Failed to get module from moduleFuture"`, and **returns null** — the IME has no keyboard definition, so **the keyboard cannot open at all**. The `FutureTask` is created once and cached, so the failed Future is reused on every later attempt: the keyboard never opens again for the whole process lifetime, and every fresh process fails the same way on its first def-parse. There is no crash dialog and no visible log, because the failure is contained by the Future — exactly matching Sam's symptom ("keyboard hi na open hona", no crash, Settings and the editor still work).

**The two 861 changes, judged:**

| 861 change | Verdict |
|---|---|
| (a) Block L' (`last_run` write) moved OUTSIDE the `diagFirst` gate | **SAFE — not the cause.** `getSharedPreferences` / `edit()` / `putString` / `apply()` on a valid Application Context cannot throw. Verified throw-free (§5). |
| (b) `appCtx` set earlier via `Lmxa.attachBaseContext` | **SAFE in itself — but it is what UNMASKS the Toast.** It makes the Context non-null on the first def-parse call, which is exactly what lets the P0 Toast fire for the first time. The hook itself cannot throw (§2, §3). **Do NOT remove it** — it is correct and needed for customization to work at all. |

**The permanent fix (detailed in §7):** remove ALL four Toast call sites from `Lpud.m` (P0, P1, P2, P3) — they are diagnostic-only and lethal on a no-Looper thread; keep `Log.e` (never throws) and the `nobi_diag` SharedPreferences writes (safe, and they are the reliable diagnostic). Add a try/catch around the hook region so ANY future hook exception falls through to GUARD/defaults. Keep the `attachBaseContext` hook, keep `Lesb.onCreate` as second setter, keep Block L' outside the gate, keep the whole substitution logic. `Lqmu.c()` / `Lqml.c()` diff must stay 0. **Do NOT revert to the popup-time read (option c)** — the def-parse path is proven sound; the only defect is the Toasts.

---

## 2. Check 1 — `Lmxa.attachBaseContext`: SAFE, REFUTED as a cause

### 2.1 Original (from shipped 860 classes.dex) — 4 insns, 2 regs, 0 tries

```
0: INVOKE_SUPER {v0, v1} Lpdy;->attachBaseContext(Landroid/content/Context;)V
1: CONST_4 v1, 0
2: INVOKE_STATIC {v0, v1} Lvqt;->b(Landroid/content/Context;Z)Z
3: RETURN_VOID
```

It calls the **super** `attachBaseContext` (`Lpdy`, which is in the Application chain) and a static helper `Lvqt.b(Context, Z)`. It does **NOT** call `MultiDex.install` — there is no MultiDex call anywhere in this method, and the app's primary dex contains all the classes this path needs (see §3). No try block, no branch, no field assignment except through the two calls.

### 2.2 Patched (from shipped 861 classes.dex) — 5 insns, 2 regs, 0 tries

```
0: SPUT_OBJECT v1, Lpud->appCtx
1: INVOKE_SUPER {v0, v1} Lpdy;->attachBaseContext(Landroid/content/Context;)V
2: CONST_4 v1, 0
3: INVOKE_STATIC {v0, v1} Lvqt;->b(Landroid/content/Context;Z)Z
4: RETURN_VOID
```

The only change is the `SPUT_OBJECT v1, Lpud->appCtx` inserted at idx 0; the original 4 insns follow verbatim (no branches to retarget).

### 2.3 Is `Lmxa` Application-only?

`Lmxa` sits in the chain `HiltLatinApp` → `Lesb` → `Lesf` → `Lmxa` → `Lpdy` → `android.app.Application` → `Context` (verified in diagnosis-860). `Lmxa` is the most-derived override of `attachBaseContext` in the Application hierarchy (`HiltLatinApp`, `Lesb`, `Lesf` declare no `attachBaseContext`). It is **not** a base for Activities or Services — Activities/Services extend their own framework bases, not `Lmxa`. The framework calls it on the Application instance during process startup, **before** `onCreate()`, with the base Context in v1 (guaranteed non-null by the framework).

### 2.4 Does the `SPUT_OBJECT` trigger a dangerous class init?

The `SPUT_OBJECT` to `Lpud->appCtx` triggers `Lpud` class initialization (`<clinit>`) — see §3 for the full `<clinit>` dump. It is harmless, and it was **already** triggered by the `Lesb.onCreate()` hook in 860 (which worked — the keyboard opened on 860). So the init point is not new.

### 2.5 Device evidence also refutes it

If `attachBaseContext` threw, Application creation would fail and the **whole process would die** — Settings, the editor, everything would be gone. Sam's report is the opposite: the editor works (he customized A in 861), Settings works, only the **keyboard** never opens. The process is alive. Therefore `attachBaseContext` did not throw. Check 1 is closed: **REFUTED**.

---

## 3. Check 2 — `Lpud.<clinit>`: HARMLESS, REFUTED

Dumped from the shipped 861 dex (5 insns, 0 tries):

```
0: NEW_INSTANCE v0, Lpux
1: CONST_4 v1, 0
2: INVOKE_DIRECT {v0, v1} Lpux;-><init>([B)V
3: SPUT_OBJECT v0, Lpud->q:Lpux
4: RETURN_VOID
```

### 3.1 What it references, and where those classes live

| Class | Dex (verified via dexdump) | Notes |
|---|---|---|
| `Lpud` | classes.dex (primary) | the class itself |
| `Lpux` | classes.dex (primary) | super `Ljava/lang/Object;`, no interfaces, no fields; `<init>([B)` just calls `Object.<init>()` and returns — the null array argument is ignored |
| `Lpuz`, `Lpva`, `Lwhe`, `Lwgz` | classes.dex (primary) | referenced by `Lpud.m` / the hook, all primary-dex |
| `Lmxa`, `Lesb`, `Lesf`, `Lpdy` | classes.dex (primary) | Application hierarchy |
| `Lcwj`, `Levj`, `Lpwz`, `Lpxa` | classes.dex (primary) | def-parse chain |
| `Lvqt` (called by `attachBaseContext` as `Lvqt.b(Context,Z)`) | **classes3.dex (secondary)** | not referenced by `<clinit>`; it is called by the ORIGINAL `attachBaseContext` body, which ran fine on every build |

**No class referenced by `Lpud.<clinit>` lives in a secondary dex.** `<clinit>` cannot throw `NoClassDefFoundError` / `ClassNotFoundException` (there is nothing to load that isn't already in the primary dex), and `Lpux.<init>` cannot throw anything. **Check 2 is REFUTED** — triggering `<clinit>` from `attachBaseContext` is safe, and the classloader/dex-readiness concern does not apply (there is no `MultiDex.install` in this path at all; the relevant classes are all in the primary dex, which is loaded before `attachBaseContext` runs).

---

## 4. Check 3 — the `Lpud.m` substitution path: THROW-FREE for Sam's value; the Toasts are the throw

### 4.1 The full 861 `Lpud.m` (494 insns, 16 regs, **0 tries**)

Dumped from the shipped 861 dex (`verify/lpud-m-861.txt` / `lpud-m-861-full.txt`). Structure:

| Region | Insns | What it does |
|---|---|---|
| Block E | 0–14 | Context resolution: `SGET appCtx` → fallback `ActivityThread.currentApplication()` → record `diagCtxSrc` = `static` / `fallback` / `null` |
| Block L' | 15–38 | `last_run` write to SharedPreferences `nobi_diag` (null-guarded at insn 15 and insn 32) |
| diagFirst gate | 39–40 | `SGET_BOOLEAN diagFirst; IF_NEZ -> Seg D` |
| Flag set | 41–42 | `diagFirst = true` |
| **P0 Toast** | **43–48** | `IF_EQZ v5 -> skip; Toast.makeText(v5, "DEF 859: Lpud.m runs", 1).show()` |
| Fingerprint | 49–65 | `ARRAY_LENGTH v13; == 9?` → `AGET v2, v13, 3` (token[3] code point) |
| 26-way chain | 66–201 | token[3] cp → pref key + column class (A: cp 0x1F496 → `emoji_A`, colId v4 = 1 = LEFT) |
| Prefs read | 202–214 | Context null-check → `getSharedPreferences("nobi_longpress_emoji", 0)` → `getString(key, null)`; null → GUARD |
| P1 Toast | 215–232 | A-key only: `"A: prefs=<value>"` |
| `SPUT diagPrefs` | 233 | records the prefs string |
| Split + length check | 234–239 | `split(",")` → must be exactly 5 parts |
| Empty-part guard | 240–264 | each part `length() > 0`, else GUARD |
| Clone | 265–270 | `NEW_ARRAY [I` (len 9) + `arraycopy(v13,0,v9,0,9)`; `NEW_ARRAY [Object;` (len 9) + `arraycopy(v15,0,v10,0,9)` |
| 5 substitutions | 271–330 | per-part slot computed on the fly from colId v4; `codePointAt(part,0)` → `APUT` into cloneI; part string → `APUT_OBJECT` into cloneO |
| `MOVE_OBJECT` | 331–332 | `v13 = v9; v15 = v10` (clones replace the inputs) |
| P2 Toast | 333–342 | A-key SUCCESS path only: `"A: SUB OK"` |
| Block W_SUB (`last_a`) | 343–375 | `last_a` write, `outcome=SUB OK` |
| Clean-up | 377–381 | reset v3/v6/v7/v8/v11 to `CONST_4 0` |
| Stock body | 382–443 | null/empty → `Lwmm.a`; else loop (`Math.max(lenI, lenObj)`, bounds-guarded `AGET`/`AGET_OBJECT`, `NEW_INSTANCE Lpva(cp, Lpuz, label, 0x7fffffff)`, `Lwgz.h`, `Lwgz.g()` → `Lwhe` → `IPUT Lpud.b`) |
| GUARD path | 444–493 | P3 Toast (`"A: GUARD"`, insns 453/455) + `last_a` write with `outcome=GUARD`, then `GOTO_16` to clean-up |

### 4.2 The four Toast call sites (shipped 861 dex)

| Toast | Insns | When it fires |
|---|---|---|
| **P0** `"DEF 859: Lpud.m runs"` | 46 `Toast.makeText` / 48 `Toast.show` | **First `Lpud.m` call of the process** (inside the `diagFirst` gate, before the fingerprint and before the prefs read) |
| P1 `"A: prefs=<value>"` | 227 / 229 | A-key only, after prefs read |
| P2 `"A: SUB OK"` | 337 / 339 | A-key only, SUCCESS path |
| P3 `"A: GUARD"` | 453 / 455 | A-key only, GUARD path |

The same four sites exist in the 860 control (insns 50/52, 231/233, 341/343, 457/459) — but in 860 they **never fired**, because v5 was always null on the first call (insn 43's `IF_EQZ` skipped them) and the prefs read also required non-null v5. **861 is the first build in which any of these Toasts executes on the phone.** P0 is the first to fire, and it fires before any fingerprint/prefs/substitution code runs.

### 4.3 `Toast.makeText` throws on a thread without a Looper — PROVEN from AOSP source

Fetched 2026-11-15 from `android.googlesource.com/platform/frameworks/base/+/main/.../Toast.java`:

```java
public Toast(@NonNull Context context, @Nullable Looper looper) {
    ...
    looper = getLooper(looper);
    mHandler = new Handler(looper);
    ...
}

private Looper getLooper(@Nullable Looper looper) {
    if (looper != null) {
        return looper;
    }
    return checkNotNull(Looper.myLooper(),
            "Can't toast on a thread that has not called Looper.prepare()");
}
```

`Toast.makeText(context, text, duration)` calls `new Toast(context)` → `new Toast(context, null)` → `getLooper(null)` → `Looper.myLooper()`. On a thread with no Looper, `checkNotNull` throws **`NullPointerException: "Can't toast on a thread that has not called Looper.prepare()"`**. This is not a log warning and not a silent skip — it is a hard throw from inside `makeText` itself (before `show()` is even reached). The same mechanism holds on API 30+ (when the `Toast(Context, Looper)` constructor was added) and, in older form (`new Handler()` throwing inside `TN`), on every Android version. **A Toast from a no-Looper thread always throws.**

### 4.4 The def-parse thread: a FutureTask on an executor — and the async path is a background pool with no Looper

- `Lcwj.call` insn 35 calls `Levj.a()` inside a `FutureTask` (`Lxkc`) created by `ModuleManager$ModuleInfo.newCreateModuleFutureTask()` (`new-instance Lcwj` + `new-instance Lxkc`, `Lxkc.<init>(Callable)`).
- The FutureTask is executed by an `Executor` via `ModuleInfo.loadModuleAsync(Executor)` → `Executor.execute(Runnable)`.
- Two execution paths exist:
  - **Synchronous** `ModuleInfo.loadModule()`: passes `Lxiv.a`. `Lxiv.execute(Runnable)` is exactly `invoke-interface Runnable.run(); return-void` — a **direct executor** that runs the task inline on the **calling thread**. (Dumped from the shipped 861 dex; `Lxiv` is an enum singleton in classes.dex.)
  - **Asynchronous** `ModuleInitListener.maybeLoadModule()`: passes `this.executor`, a field set in `ModuleInitListener.<init>` from the module's `Lqay` enum ordinal: ordinal 3 → `Lxiv.a` (direct); ordinal 2 → `Lnhg.a().f()`; ordinal 1 → `Lnhg.a().c()`; default → `Lnho.a`. `Lnhg.a().f()` / `.c()` return `Lxkf`, which is a **public interface** whose methods are `a(Runnable,J,TimeUnit)`, `b(Callable,J,TimeUnit)`, `c/d(Runnable,J,J,TimeUnit)` returning `Lxkd` — i.e. a **`ScheduledExecutorService`** (a standard background thread-pool executor). Plain pool threads do **not** call `Looper.prepare()`, so they have **no Looper**.
- The module system is designed to load modules asynchronously at startup on those background executors (this is the "def-parse runs on a background executor thread very early in process startup" finding of diagnosis-859, now refined: the FutureTask exists in both paths, and the async path is a pool thread).

**What is NOT statically provable:** the exact `Lqay` ordinal of the IMEManager (`Levj`) module, and therefore which of the two paths the very first def-parse takes on Sam's phone. The `getModuleDef(Class)` switch in the dex has no `const-class Levj` entry (the `Levj` def is registered through a route not located in the time available). So the Looper status of the def-parse thread is **not proven statically** — it is deduced from the device evidence below. The report does not hide this gap (see §8).

### 4.5 The device evidence that closes the gap — by elimination, the Toast is the ONLY candidate

The 860 → 861 diff is exactly two changes (both verified on the shipped dexes). Trace every code path that executes for the first time in 861 on the first def-parse call:

| Newly-executing code in 861 | Can it throw? | Verdict |
|---|---|---|
| Block L' (insns 15–38): `getSharedPreferences("nobi_diag", 0)` → `edit()` → `putString("last_run", …)` → `apply()` | No — on a valid Application Context none of these throws. `apply()` is async and never throws for a valid Context. Both null-guards (insns 15, 32) skip cleanly if the Context/prefs are null. | SAFE |
| `attachBaseContext` SPUT + `<clinit>` | No — §2, §3. | SAFE |
| Block E (Context resolution) | No — `SGET`, `currentApplication()` (returns null or an Application, never throws), `StringBuilder`, `CONST_STRING`. | SAFE |
| **P0 Toast (insns 44–48)** | **YES — `Toast.makeText` throws NPE on a no-Looper thread (§4.3).** It is the FIRST newly-executing code that can throw, and it runs before the fingerprint, before the prefs read, before any substitution. | **THE CAUSE** |
| Substitution path (insns 66–381) | No for Sam's value — see §4.6. (It also runs only AFTER P0.) | SAFE |

There is no other candidate in the diff. **If the def-parse thread had a Looper, the Toast would be harmless and 861 would work — but 861 does not work.** The only mechanism in the diff that can kill the def load is the Toast on a no-Looper thread. This is a deduction from elimination + the proven AOSP throw mechanism, not a static proof of the thread's Looper status — but it is the only explanation that fits all of the device evidence (861 fails, 860 works, the process stays alive, no crash dialog).

### 4.6 Sam's exact value `😂,🥰,💕,💖,😘` traced through the substitution — NO THROW

(Sam's saved customization for key A: Centre 😂, Up 🥰, Down 💕, Left 💖, Right 😘 → prefs value `😂,🥰,💕,💖,😘`, canonical order centre,up,down,left,right.)

- **Fingerprint:** `[I` length 9 (fd2 A list has 9 tokens); `AGET v2, v13, 3` = 0x1F496 (💖) → chain → pref key `emoji_A`, colId v4 = 1 (LEFT). Verified against fd2.
- **Prefs read:** `getSharedPreferences("nobi_longpress_emoji", 0)` on the valid Application Context → non-null; `getString("emoji_A", null)` → `"😂,🥰,💕,💖,😘"` (non-null — the editor saved it). → does not go to GUARD.
- **Split:** `split(",")` on `"😂,🥰,💕,💖,😘"` → exactly 5 parts. Length check passes.
- **Empty-part guard:** every part has `length() > 0` (each is a single emoji, length 2 in UTF-16 as a surrogate pair). No empty part → does not go to GUARD. (This is the diagnosis-857 Candidate 1 — the empty-part throw — and it is guarded; Sam's value has no empty part.)
- **Clone:** `NEW_ARRAY [I` length 9 + `arraycopy(v13, 0, v9, 0, 9)`; `NEW_ARRAY [Object;` length 9 + `arraycopy(v15, 0, v10, 0, 9)`. Both arrays are length 9 (see §4.7) — no `ArrayIndexOutOfBoundsException`.
- **Substitution (A is LEFT column, v4=1):** slots, recomputed with branch-offset semantics calibrated against BranchDump: centre {down 0, centre 3, right 4, left 5, up 6}; left {down 1, centre 4, right 5, left 3, up 7}; right {down 1, centre 4, right 3, left 5, up 7}. For A: part0 centre 😂 → slot 4; part1 up 🥰 → slot 7; part2 down 💕 → slot 1; part3 left 💖 → slot 3; part4 right 😘 → slot 5. All slots are within the length-9 clones. Each `codePointAt(part, 0)` is called on a non-empty part (length ≥ 1) → no `StringIndexOutOfBoundsException`. Each `APUT` / `APUT_OBJECT` is in-bounds.
- **Clean-up:** resets v3/v6/v7/v8/v11 to `CONST_4 0` — these are INT locals in the stock body (verified safe in 858/859/860); v5/v9/v10 not reset (REF in the stock body too). Verified by the 861 gates (TypeMergeCheck `Lpud.m` 1 = 1 stock).
- **Stock body:** identical to 860's (which worked). Bounds-guarded `AGET`/`AGET_OBJECT` via `Math.max(lenI, lenObj)`.

**Conclusion:** the substitution path is throw-free for Sam's exact value. (It also never gets to run in 861, because P0 throws first.)

### 4.7 The `arraycopy` length-mismatch hypothesis — REFUTED

Candidate: `arraycopy(v15, 0, v10, 0, 9)` at insn 270 throws `ArrayIndexOutOfBoundsException` if the `[Object;` (from `Lptz`) is shorter than the `[I` (from `Lptw`). **Refuted.** The three arrays are parsed from the SAME raw attribute string by `Lptw.a` (`Lpvb.h` → `[I`), `Lptz.a` (`Lsah.t` → `[String;`, then `Lsah.l` applies `Lsah.i` in place to decode each `U+XXXX` token), and `Lpts.a` (`Lsah.r` → `[Enum;`). `Lpvb.h` (45 insns) and `Lsah.t` (39 insns) both split via `Lway.j`, add one element per token, then call `Llgy.cE(list, x)`. `Llgy.cE` (12 insns, verified three ways — dexlib2 FullDump, BranchDump, and dexdump `-d` raw bytes `3900 1200` = opcode 0x39 `if-nez`): `isEmpty() -> v0; if-nez v0, -> return (insn 11); size()-1; get(last); …`. For a NON-empty list, `if-nez` branches immediately to return — the remove logic never runs for non-empty lists (it is dead code there; it would crash at `get(-1)` only for an empty list). Therefore for the 9-token fd2 list, BOTH the `[I` and the `[String;` are length 9, and `Lsah.t`'s empty tokens become null entries (length preserved, one entry per token). **The `arraycopy` cannot throw for a 9-token list.** (`Lpvb.f` token parser, 49 insns, and `Lsah.i` decoder, 48 insns, were also dumped; empty-token handling is consistent with one element per token.)

### 4.8 `[I` null check

Insn 49 `ARRAY_LENGTH v1, v13` has no null check on v13. But 858/859/860 ran the identical `ARRAY_LENGTH` on every call and the keyboard opened, so `[I` is never null in practice. Refuted by device evidence.

### 4.9 The propagation chain — PROVEN, and it explains the symptom exactly

Dumped from the shipped 861 dex:

1. **`Lpud.m`: 0 tries.** A throw propagates straight out.
2. **`Lpwz.f` (276 insns): tries=0.** Insns 101–110 fetch the three arrays from `Lpue->d:Lptw` (`[I`), `e:Lpts` (`[Lpuz;`), `f:Lptz` (`[Object;`) via `Lptp->a` + `CHECK_CAST`, then insn 110 `INVOKE_VIRTUAL Lpud.m([I,[Lpuz;,[Object;)`. A `Lpud.m` throw propagates straight through `Lpwz.f`.
3. **`Levj.a` (323 insns): tries=0.** Propagates.
4. **`Lcwj.call` (889 insns, 33 tries):** insn 35 invokes `Levj.a()` at code address 65, INSIDE TRY start=63 len=6 (catch-all handler at addr 73 = insn 39 `move-exception`) — a try-with-resources: on exception it closes the `Lrmr` trace section (insn 41) and **RETHROWS the original exception** (insn 42 `goto -> insn 45 throw v15`; insns 43–44 handle a `close()` failure via `addSupressed`). **So a `Levj.a()` throw propagates out of `Lcwj.call` (the Callable) → the FutureTask completes exceptionally.** The TRY at start=93 len=13 (ExecutionException + IllegalStateException → addr 106) is a DIFFERENT `$cid` switch branch, not the `Levj.a()` path.
5. **`ModuleInfo.loadModule()` (40 insns, 1 try):** insn 0 `sget Lxiv->a` (the executor); insn 1 `loadModuleAsync(executor)`; insn 6 `Lxkc.get(50, SECONDS)` — blocks up to 50 s waiting for the module. TRY start=8 len=10, handlers: `ExecutionException` → addr 49, `TimeoutException` → addr 47, `InterruptedException` → addr 19. The `ExecutionException` handler (insns 27–39) logs `"Failed to get module from moduleFuture: %s"` and **returns null (v0 = 0)**. So if the def-parse Future fails, `loadModule()` logs the failure and returns **null** — the IMEManager module is null → the IME has no keyboard definition → **the keyboard cannot open**.
6. **No crash dialog, no visible log:** the exception is contained by the Future (`ExecutionException` caught, null returned). Gboard logs it via `La.ck` to logcat only — invisible to Sam. This is why Sam sees "keyboard never opens" with no crash and no error message.
7. **The failure is permanent per process:** `loadModuleAsync` caches the `FutureTask` in `createModuleFutureTask` (insns 1–12: only created if null, otherwise reused). A FutureTask that completed exceptionally stays exceptionally completed — every later `loadModule()` call gets the same `ExecutionException` and returns null again. So the keyboard can **never** open for the whole process lifetime, and every fresh process fails the same way on its first def-parse (the P0 Toast fires on the first `Lpud.m` call of every process). This matches Sam's report that the keyboard "never opens" (not "sometimes").

### 4.10 Why 860 worked and 861 does not — the complete picture

| | 860 | 861 |
|---|---|---|
| Context on first `Lpud.m` call | null (only `Lesb.onCreate` sets it, and the first def-parse lands before that) | non-null (set by `attachBaseContext`, which runs before `onCreate`) |
| P0 Toast (insns 44–48) | skipped by the null-guard (insn 43 `IF_EQZ`) | **FIRES** |
| Def-parse | completes (GUARD → defaults) | **throws NPE at P0 → Future fails → `loadModule()` returns null** |
| Keyboard | opens (defaults) | **never opens** |

This is why Sam's 861 report is worse than 860's: 860's defect was silent (customization never applied); 861's defect is fatal to the keyboard (the def load dies on the first call).

---

## 5. Check 4 — does the def-parse run BEFORE `attachBaseContext` / `onCreate`?

**No.** The def-parse cannot run before the Application is created, because the module system (`ModuleManager.getInstance(Context)`) requires a Context, and the module loading is triggered by the IME service / keyboard creation, which happens after Application creation. `attachBaseContext` runs during Application creation (before `onCreate`), so by the time any def-parse runs, `appCtx` is set. That is exactly why 861's P0 Toast fires: the Context IS available at def-parse time in 861 (unlike 860, where the only setter was `Lesb.onCreate` and the first def-parse apparently landed before `onCreate` completed — the exact trigger for that early def-parse is not provable statically, as noted in diagnosis-860).

Two consequences:

1. **There is no path where the def-parse throws BEFORE the Application is fully created** in a way that kills the process. The throw is confined to the def-parse FutureTask (contained, §4.9). The Application itself is fine — which is why Settings and the editor keep working on 861.
2. **The "cached def" question:** the def is cached in-memory only (diagnosis-859: `Levj`/IMEManager, invalidated only by the 13-int reload list; no file-based def cache). A FAILED def-parse does not cache a "failed def" — it caches a failed Future, which is worse: every later attempt reuses the failed Future and returns null (see §4.9 step 7). Force-stop starts a new process, but the new process hits the same P0 Toast throw on its first def-parse, so the keyboard never opens there either. (This is why Sam's force-stop did not help on 861.)

---

## 6. What was ruled out (complete list)

| Candidate | Verdict | Evidence |
|---|---|---|
| `Lmxa.attachBaseContext` hook throws / classloader not ready / MultiDex | **REFUTED** | §2: original 4 insns quoted; no MultiDex call; all classes in primary dex; process stays alive (Settings/editor work) |
| `Lpud.<clinit>` throws (`NoClassDefFoundError` / `ClassNotFoundException`) | **REFUTED** | §3: 5-insn `<clinit>` quoted; `Lpux` is trivial and primary-dex; no secondary-dex class referenced |
| Substitution throws for Sam's value (`codePointAt` on empty part) | **REFUTED** | §4.6: all 5 parts non-empty; empty-part guard verified; slots all in-bounds |
| `arraycopy` length mismatch (`[Object;` shorter than `[I`) | **REFUTED** | §4.7: `Llgy.cE` dead-code analysis (3 independent verifications); both arrays length 9 |
| `[I` null | **REFUTED** | §4.8: identical `ARRAY_LENGTH` ran fine on 858/859/860 |
| Block L' (`last_run` write) throws | **REFUTED** | §4.5: `getSharedPreferences`/`edit`/`putString`/`apply()` cannot throw on a valid Application Context |
| Block E (Context resolution) throws | **REFUTED** | §4.5: `SGET`, `currentApplication()` (null-safe), `StringBuilder` only |
| Stale install / wrong version | **REFUTED** | Sam customized A in 861 (the editor with the Diagnostics section is new in 860/861) and force-stopped; the 861 change report's SHA/version were verified by the parent |
| Stock body throws | **REFUTED** | Identical to 860's stock body, which worked; bounds-guarded |
| P1/P2/P3 Toasts as the FIRST throw | **REFUTED as first cause** | They fire only for A-key (P1/P2/P3) and only AFTER P0; P0 fires first on the very first `Lpud.m` call of the process |
| Popup-path code (`Lqmu.c` / `Lqml.c`) | **NOT INVOLVED** | classes2.dex is byte-identical to Fix11 (md5 `87aeab3d…`) in 858/859/860/861 — the popup code was never touched in this line |

---

## 7. Check 5 — the PERMANENT fix (ranked)

Sam's demand: "isko fix kr permanent yrr" — the keyboard must ALWAYS open even if the hook fails, AND the customization must work.

### Option 1 (PRIMARY — the root-cause fix, must do): remove ALL Toasts from `Lpud.m`

- Delete/disable the four `Toast.makeText(...).show()` pairs (P0 insns 46/48, P1 insns 227/229, P2 insns 337/339, P3 insns 453/455). Replace each pair with NOPs (or remove the insns and re-verify branches). Keep the `Log.e` calls (they never throw and are the safe diagnostic). Keep the `nobi_diag` SharedPreferences writes (Block L', Block W_SUB, Block W_GUARD — safe, and they are the reliable on-device diagnostic).
- **Why this is the permanent fix for the regression:** the Toasts are diagnostic-only — they serve no user-facing purpose. They are the ONLY code in the hook that can throw on the def-parse thread. Removing them removes the entire throw class. The keyboard then opens even if every other part of the hook misbehaves (because everything else goes to GUARD/defaults).
- **Do NOT replace the Toast with a `Toast` on the main thread** (e.g. posting to a Handler) — the def-parse thread has no Looper and there is no reason to add a cross-thread diagnostic. `Log.e` + `nobi_diag` are sufficient.

### Option 2 (ALSO must do — belt and braces): make the substitution fail-safe with a try/catch → GUARD

- Add ONE try block to `Lpud.m` covering the hook region (fingerprint → chain → prefs → split → clone → substitution, i.e. insns 49–381 in the 861 layout), with the handler jumping to the existing GUARD path (which already does the `last_a` write with `outcome=GUARD` and then `GOTO_16` to clean-up). The handler is `move-exception` into a dead scratch register, then jump to GUARD. `Lpud.m` currently has 0 tries; adding a try is feasible in dexlib2 (the `Tries` are part of the method implementation), but it must be verified with the full gate set (VerifyBranches, RegInitCheck, FieldCheckGen, TypeMergeCheck, payload alignment) because a try region changes the verification profile. The handler must not clobber a live register and must not allocate a new ref.
- **Why:** this is what makes the fix PERMANENT in the sense Sam means — even if some future hook change throws (a prefs edge case, a malformed saved value, a new Android behaviour), the keyboard still opens with defaults. The Toast removal fixes today's regression; the try/catch guarantees no future hook throw can ever kill the keyboard again.
- **Sequencing:** if the try/catch proves too risky to add in the same build, ship the Toast removal FIRST (it is the root-cause fix and it is low-risk), and add the try/catch as a second hardening step. Do NOT ship a build that keeps the Toasts.

### Keep (do NOT change)

- **`Lmxa.attachBaseContext` hook** — correct, safe (§2, §3), and it is what makes the Context available so the substitution can actually run. Removing it would restore the 860 failure (Context null → GUARD → customization never applies).
- **`Lesb.onCreate()` hook** — keep as the second setter.
- **Block L' outside the `diagFirst` gate** — safe and it is the diagnostic that proves the hook ran.
- **The whole substitution logic** (fingerprint, 26-way chain, split=5 + every-part-non-empty, clone, 5 per-column substitutions, clean-up) — proven throw-free for Sam's value and for every valid fd2 list.
- **`Lqmu.c()` / `Lqml.c()` untouched** (classes2 diff = 0) — Sam's directive stands.

### Option (a) — remove `attachBaseContext`, rely on `Lesb.onCreate()` + retry: NOT recommended

This would restore the 860 behaviour: the Context is null on the first def-parse call, the hook goes to GUARD, and customization never applies. It treats the wrong cause (the hook is safe; the Toasts are the defect). Reject.

### Option (c) — Sam's popup-time read (FIXED 857-style hook at the LIST LOAD in `Lqml.c()` insn 25): NOT recommended for THIS regression

The def-parse path is now proven sound — the only defect is the diagnostic Toasts. The popup-time read would reintroduce the approach that broke the popup 5 times (Fix4/Fix5/Fix10/Phase 3-851/857) and would violate Sam's 09:15 IST directive ("do NOT touch the long-press codes AT ALL"). Only revisit it if the def-parse path fails again AFTER the Toast removal, and then only with the 5 safeguards from diagnosis-860 §6 (FieldCheckGen 0 with 852-style `INSTANCE_OF` + `IF_EQZ -> GUARD` narrowing; v13 colId fix; empty-part guard; full gates with `Lqmu.c()` diff = 0; visible diag) and Sam's explicit agreement.

### What the 862 build must contain (exact)

1. Base = shipped 861.
2. Remove the 4 Toast call sites in `Lpud.m` (P0/P1/P2/P3). Keep `Log.e` and the `nobi_diag` writes.
3. Add the try/catch around the hook region → GUARD (if the gates pass; otherwise ship the Toast removal alone and harden next).
4. Keep `attachBaseContext` + `Lesb.onCreate` hooks, Block L', the substitution logic, the editor.
5. `Lqmu.c()` / `Lqml.c()` diff = 0; classes2 md5 must stay `87aeab3d…` (= Fix11).
6. Full gate set on the final signed APK: FieldCheckGen `Lpud.m` 0, RegInitCheck 0, VerifyPatch 0, VerifyBranches 0 bad, payload 0 odd, CanonDiff vs 861 = 1 method only (`Lpud.m`), RefResolve filtered = 17 (identical set), versionCode 176071862 in BOTH apktool.yml and classes5 dex.
7. On-device confirmation (Sam's test): force-stop, install over 861, customize A centre = 😂 (already saved), force-stop once, open the keyboard → it MUST open; long-press A → centre 😂; editor → Diagnostics must show `run ts=<number>` and `A cp=1f496 ctx=static prefs=😂,🥰,💕,💖,😘 outcome=SUB OK`.

### Immediate safe rollback for Sam (until 862 lands)

**856** (Drive `1HqdoTzwQsuSLaYcEbeZ-rmUvtpabVdOo`) — popup works, keyboard opens, no customization. Or Fix11 (176071849, Drive `1V4_QwuFEl1lGndpSMXOEi3RDHxke2s_j`). Tell him to go back to 856 now; 861 must not stay on his phone.

---

## 8. What is NOT proven (honest gaps)

1. **The exact Looper status of the def-parse thread is deduced, not statically proven.** The chain is: (a) `Toast.makeText` throws NPE on a no-Looper thread — PROVEN from AOSP source; (b) the P0 Toast is the ONLY newly-executing code in the 860→861 diff that can throw — PROVEN by elimination on the shipped dexes; (c) the def-parse runs in a FutureTask on an executor, and the async path is a background `ScheduledExecutorService` (pool thread, no Looper) — PROVEN for the async path; the IMEManager module's exact `Lqay` ordinal (which selects the executor) could not be located statically (the `getModuleDef` switch has no `const-class Levj` entry). If the def-parse thread had a Looper, the Toast would be harmless — but then 861 would work, and it does not. The deduction is strong, but the report does not claim a static proof of the thread's Looper status.
2. **Which UI path consumes the null IMEManager module** (the exact null-check downstream of `loadModule()` returning null) was not traced to a specific instruction. The symptom match (keyboard never opens, no crash, process alive) is exact, and the `loadModule()` → null return is proven; the downstream consumption is the standard Gboard module-access pattern and is not needed to fix the bug.
3. **The exact trigger that makes the first def-parse land before `Lesb.onCreate()` in 860** (early-init race vs a ContentProvider-triggered def-parse) was already noted as not provable statically in diagnosis-860 and is not needed here — in 861 the Context IS set (via `attachBaseContext`), which is what matters.

**What device evidence would confirm the diagnosis:** install 862 (Toast-free, same substitution logic). If the keyboard opens, the Diagnostics show `run ts=…` + `A cp=1f496 ctx=static prefs=😂,🥰,💕,💖,😘 outcome=SUB OK`, and A-centre shows 😂 — the diagnosis is confirmed (the Toasts were the killer). If 862's keyboard opens but Diagnostics still show "No record yet", the Toast hypothesis is wrong and the Context is still null on every call — then fall back to re-examining the Context source (option (a) analysis). The strong prediction is that 862 works.

---

## 9. Summary table — the 5 checks

| # | Check | Answer |
|---|---|---|
| 1 | `Lmxa.attachBaseContext` — safe? | **YES, safe. REFUTED as cause.** Original 4 insns quoted (§2.1); no `MultiDex.install`; super `attachBaseContext` + `Lvqt.b`; Application-only; the SPUT triggers a harmless `<clinit>` already triggered in 860. Process stays alive → it did not throw. |
| 2 | `Lpud.<clinit>` — can it throw? | **NO. REFUTED.** 5 insns quoted (§3); `Lpux` trivial, all classes primary-dex; no secondary-dex class referenced; cannot throw `NoClassDefFoundError`/any other. |
| 3 | `Lpud.m` substitution — every throw path | **Throw-free for Sam's exact value** (§4.6). `arraycopy` length mismatch REFUTED (§4.7, `Llgy.cE` dead code). `[I` null REFUTED (§4.8). **The throw is the P0 Toast** (§4.3, §4.5) — `Toast.makeText` NPE on a no-Looper thread, first firing in 861. Propagation chain PROVEN (§4.9): 0 tries through `Lpud.m`/`Lpwz.f`/`Levj.a` → `Lcwj.call` rethrows → Future fails → `loadModule()` catches `ExecutionException` → returns null → no keyboard def → keyboard never opens. |
| 4 | Def-parse before `attachBaseContext`/`onCreate`? | **NO.** The Application is created first; `attachBaseContext` runs before `onCreate`; the def-parse runs after. The throw is confined to the def-parse Future (contained, no process death). A failed Future is cached and reused, so the keyboard never opens for the whole process (§4.9 step 7, §5). |
| 5 | Permanent fix | **Remove ALL 4 Toasts from `Lpud.m`** (keep `Log.e` + `nobi_diag` writes) **+ add a try/catch around the hook region → GUARD**. Keep `attachBaseContext` + `Lesb.onCreate` + Block L' + substitution logic. `Lqmu.c`/`Lqml.c` diff = 0. Reject option (a) (restores the 860 failure) and option (c) (reintroduces the 5-times-broken popup-path approach; violates Sam's directive). Roll Sam back to 856 now; build 862. |

---

## 10. Files referenced

- Shipped 861 APK: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-defparse-ctxfix2/outputs/NobiBoard-CtxFix2-176071861.apk` (SHA `13b10ce4…`, cert v2 MATCH, versionCode 176071861)
- Shipped 860 APK: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-defparse-ctxfix/outputs/NobiBoard-CtxFix-176071860.apk`
- Change reports: `.../ctxfix2/outputs/change-report.md`, `.../ctxfix/outputs/change-report.md`, `.../defparse-custom/outputs/change-report.md` (858)
- Prior diagnoses: `.../defparse-diag3/outputs/diagnosis-860.md`, `.../defparse-diag2/outputs/diagnosis-859.md`, `.../customize-safe/outputs/diagnosis-857.md`
- AOSP `Toast.java` (main branch): `https://android.googlesource.com/platform/frameworks/base/+/main/core/java/android/widget/Toast.java` — constructor `Toast(Context, Looper)` + `getLooper(null)` = `checkNotNull(Looper.myLooper(), "Can't toast on a thread that has not called Looper.prepare()")` (fetched 2026-11-15)
- Working rollback: 856 (Drive `1HqdoTzwQsuSLaYcEbeZ-rmUvtpabVdOo`); Fix11 (Drive `1V4_QwuFEl1lGndpSMXOEi3RDHxke2s_j`)

*End of diagnosis-861.*
