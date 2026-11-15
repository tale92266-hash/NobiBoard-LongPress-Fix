# smali/

Smali (disassembled dex) for the classes that matter to the long-press popup and
the customization hook. Extracted with apktool from the shipped APK — NOT hand-written.

## Source version

All files in this directory are from **NobiBoard 176071861** (2026-11-15), the latest
build. The previous commit's smali was from Fix5 (176071841); the 861 versions
replace them because the popup code changed in between (Fix11's `Lqmu.c()` replaced
the Phase 3 hook version, and `Lpud`/`Lmxa`/`Lesb` are new hook points).

- `qmu.smali`, `qml.smali`, `qck.smali`, `qmr.smali`, `qme.smali`, `qmc.smali`,
  `qmf.smali`, `piy.smali`, `qms.smali`, `qmt.smali`, `tga.smali` — classes2.dex.
  `Lqmu.c()` here is Fix11's exact 101-instruction version (tracker-init to centre
  (1,1), NO prefs hook). `Lqml.c()` is stock except it is the base popup build.
- `com/google/android/libraries/inputmethod/popup/SilkRectangularPopupView.smali` —
  classes2.dex. The popup view (`onFinishInflate()` assigns `Lqmu.h`).
- `pud.smali` — classes.dex. `Lpud.m([I,[Lpuz;,[Object;)V` is the def-parse hook
  (861: 494 insns — Block L' writes `last_run` on EVERY call; substitution hook
  before the original body). Also `Lpud.c()`, `Lpud.e()`, and the new static fields
  `appCtx`, `diagCtxSrc`, `diagPrefs`, `diagFirst`.
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
