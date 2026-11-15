# smali/

Smali (disassembled dex) for the classes that matter to the long-press popup and
the customization hook. Extracted with apktool from the shipped APK — NOT hand-written.

## Source version

Most files in this directory are from **NobiBoard 176071861** (2026-11-15); `pud.smali`
is from **176071863** (2026-11-15), the latest build. `mxa.smali` / `esb.smali` are
unchanged since 861 (verified byte-identical against the 863 APK). The previous
commit's smali was from Fix5 (176071841); the 861 versions replace them because the
popup code changed in between (Fix11's `Lqmu.c()` replaced the Phase 3 hook version,
and `Lpud`/`Lmxa`/`Lesb` are new hook points).

- `qmu.smali`, `qml.smali`, `qck.smali`, `qmr.smali`, `qme.smali`, `qmc.smali`,
  `qmf.smali`, `piy.smali`, `qms.smali`, `qmt.smali`, `tga.smali` — classes2.dex.
  `Lqmu.c()` here is Fix11's exact 101-instruction version (tracker-init to centre
  (1,1), NO prefs hook). `Lqml.c()` is stock except it is the base popup build.
- `com/google/android/libraries/inputmethod/popup/SilkRectangularPopupView.smali` —
  classes2.dex. The popup view (`onFinishInflate()` assigns `Lqmu.h`).
- `pud.smali` — classes.dex. `Lpud.m([I,[Lpuz;,[Object;)V` is the def-parse hook
  (**863: 481 insns / 908 units** — the TryBlock rewrite: catch-all try block
  `[0, 697)` → handler (`MOVE_EXCEPTION` → `toString()` → `diagLastErr`) → GUARD;
  `diagCalls` / `diagNullCtxCount` counters; `split(",", -1)`; `last_run` =
  `run ts=<ms> calls=<n> nullCtx=<n>` written on EVERY call; **zero Toasts**).
  Also `Lpud.c()`, `Lpud.e()`, and the static fields `appCtx`, `diagCtxSrc`,
  `diagPrefs`, `diagCalls`, `diagNullCtxCount`, `diagLastErr` (the 861 `diagFirst`
  flag was deleted in the 863 rewrite).
- `mxa.smali` — classes.dex. `Lmxa.attachBaseContext(Context)V` (861: 5 insns —
  `SPUT_OBJECT v1, Lpud->appCtx` + original 4 insns). Runs before `onCreate()`.
- `esb.smali` — classes.dex. `Lesb.onCreate()` (10 insns — `SPUT_OBJECT v1,
  Lpud->appCtx` + `INVOKE_SUPER Lesf.onCreate()`). Second Context setter.

## Note

This is a short excerpt of Google's proprietary Gboard bytecode, reproduced for
debugging/interoperability analysis only. It is NOT a redistributable app: to build
anything from it you must supply your own base XAPK (see README.md). The `.line`
debug-info density differs from the Fix5 extraction because the dex was
re-serialized fewer times; method and instruction counts are identical.
