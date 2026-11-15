# PROBLEM PROMPT — NobiBoard: 176071863 will not install ("package appears to be invalid"); the 861 keyboard-not-opening bug is FIXED in 862/863

> **How to use this file:** paste this entire prompt to another AI (along
> with the repository it came from) and ask it to fix the problem. It is
> written to be self-contained: the other AI needs NO prior conversation,
> NO access to the original agent, and NO Google binaries. The repo
> contains the patch/verification tools (`tools/src/`), the full written
> history (`change-reports/`, `diagnoses/`), plain-text dexdump excerpts of
> the key methods (`dumps/`), and the smali of the popup/long-press classes
> (`smali/`). The base Gboard XAPK is NOT included and must be supplied
> separately (see §1). The 861 keyboard-not-opening issue is RESOLVED —
> `diagnoses/diagnosis-861.md` PROVED the cause (the P0 Toast in `Lpud.m`
> throws on the no-Looper def-parse thread) — and builds 862/863 already
> contain the fix. The CURRENT open issue is the 863 INSTALL failure
> ("package appears to be invalid", §3) — the APK was verified good on
> the VM, so the leading explanation is a corrupted download, NOT a build
> defect. Start from §3/§4, not from the old prime suspects.

---

## 1. What NobiBoard is

NobiBoard is a **personal-use rebrand of Google's Gboard 18.6.3**
(package `com.nobi.nobiboard`, original package
`com.google.android.inputmethod.latin`, original versionCode 176071766,
arm64-v8a). There is **no Gboard Java source** — the work is **binary
patching**: decode the stock XAPK with apktool, patch the dex bytecode
with dexlib2-based Java tools, rebuild, sign with one fixed key, and
verify. The keyboard engine is still Google's Gboard. Personal use only.

The owner added, by patching: a caps-lock fix (confirmed working), a
custom emoji row, a **long-press emoji popup**, and a **customization
editor** (Settings → Preferences → Appearance → "Long-press emojis")
that lets the owner pick which emojis appear in each letter key's popup.
This prompt is about the popup AND the customization — and, right now,
about the keyboard not opening at all.

**Base you need (not in the repo):** Gboard 18.6.3 XAPK, arm64-v8a,
`com.google.android.inputmethod.latin`, code 176071766, base APK SHA-256
`2d1d060c…ab875e`. Decode it with `apktool d -s` (see §8 for the traps).

**Signing:** one fixed key per app. NobiBoard's v2 key cert SHA-256 is
`80:A9:47:FF:8B:5D:A7:4F:96:4A:F4:2C:A0:B9:6A:6C:EB:D5:48:68:AD:61:86:9D:6F:4F:88:05:86:B6:DA:7E`
(the keystore itself is NOT in the repo — use your own key for local
testing, or the owner's fixed v2 key if you have it; a build signed with
a different key will NOT install over the owner's builds). Device for
testing: vivo iQOO Z10, Android 16, not rooted. The phone test is the
only verdict — never call a build done from static checks alone.

## 2. The feature (how the popup + customization are supposed to work)

Long-press a letter key (A–Z) → a **circle popup** (84×84dp, 3×3 grid of
28×28dp cells) shows **5 themed emojis** in a **PLUS layout**: center +
up / down / left / right. The other 4 grid corners are invisible
`U+200B` spacer tokens (a 5-item list would give a 2×3 grid, not a plus;
empty tokens become null via the splitter and kill the whole LONG_PRESS
action, so spacers MUST be `U+200B`).

- **List order** in `0x7f170fd2.xml`: `[DOWN, _, _, CENTER, RIGHT, LEFT, UP, _, _]`
  (center = index 3). Stock grid maps data index `i` to visual cell
  `(col,row) = ((i%3+1)%3, 2-(i/3))` — columns rotated +1. Example (A key,
  after the Fix11 permutation): `U+200B,U+1F495,U+200B,U+1F496,U+1F60D,U+1F618,U+200B,U+1F970,U+200B`
  = [_, 💕 DOWN, _, 💖 LEFT, 😍 CENTER, 😘 RIGHT, _, 🥰 UP, _].
  **Edge keys use different mappings** (this was the side-key alignment
  bug, fixed XML-only in Fix11): left-edge keys (Q/A/Z) use
  `(i%3, 2-(i/3))`, right-edge keys (P/L/M) use `(2-(i%3), 2-(i/3))` —
  so their `long_press_data` lists are permuted differently to land on
  the same plus shape. See `POPUP_MECHANISM.md` §5.
- **Mechanism (found, not invented):** the popup is the **SILK popup**
  (`SilkRectangularPopupView`, controller **`Lqmu`**, subclass of `Lqml`).
  The green highlight circle (indicator **`Lqmu.h`**, 40×40dp,
  drawable `0x7f080316` = `#ff4db6ac`) AND the inserted value
  (`Lqmu.a()`) are BOTH driven by the **`Lqmr` spatial tracker** — NOT
  by `Lqml.g` or the pressed state.
  - `Lqmu.c(...)V` (Fix11's exact 101-insn version in every build since
    856) builds the popup and inits the tracker to CENTER (1,1).
  - `Lqmu.o(Lqmc;)V` positions the indicator (direct `setTranslationX/Y`).
  - `Lqmu.i(II)V` takes PIXELS, feeds them to `Lqme.a(II, Point)` nearest-item
    search, and builds/starts the **stock 300 ms AnimatorSet** (translation
    ObjectAnimators old cell → new cell + 4 scale animators = the emoji
    scale-up effect).
  - `Lqmu.n()V` = `m()` (scale selected cell) + `o()`. `Lqml.k(F,F)` is
    the touch handler; `Lqml.e()` sets the popup background (circle
    drawable `emoji_popup_circle.xml`, id `0x7f080786`, oval, solid
    `#ff3c474c` + 1dp `#ff9aa0a6` stroke, scoped to emoji popups only
    because the popup view is REUSED).
  - **Popup-creation entry: `Lqck.D(J)V`.** It has ONE catch-all try
    block (start=9, len=614, handler type=null). **CORRECTION to earlier
    wording: the handler does NOT swallow the exception** — it does
    `move-exception → Trace.endSection() → THROW` (it RETHROWS). Neither
    `Lqck.n` nor `Lpiy.run` has a try block, so the exception propagates
    out of the long-press Runnable and is contained by the Runnable's
    dispatch (scheduler/Handler) — which is why every popup-build
    failure in this arc presented as "no popup, no crash".
- **Interaction spec (from the owner):** long-press → popup opens fully
  and STAYS shown as long as the key is held; selection stays in the
  MIDDLE by default; while HOLDING the touch ANYWHERE on screen, drag
  down → bottom emoji, drag up → top, drag anywhere → selection follows
  step-by-step; popup opens with animation, highlight GLIDES smoothly
  (300 ms); the selected emoji scales up a little; releasing inserts the
  highlighted emoji.
- **Customization (the editor + prefs contract):** Settings → Preferences
  → Appearance → "Long-press emojis" opens
  `com.nobi.nobiboard.NobiEmojiEditorActivity` (in classes6.dex; source
  in `editor/src/`). It saves to SharedPreferences file
  **`nobi_longpress_emoji`**, keys **`emoji_A` … `emoji_Z`**, value = 5
  parts joined by `,` in canonical order **`center,up,down,left,right`**
  (e.g. A = `😂,🥰,💕,💖,😘`); a cleared slot is stored as `U+200B`,
  NOT as an empty string. The editor also has a **Diagnostics** section
  (since 860) that shows what the keyboard's def-parse hook recorded, by
  reading SharedPreferences file `nobi_diag` (`last_run`, `last_a`).
- **Other confirmed-working context:** caps suggestions (incl. after
  space), caps OFF/ON toggle reliable, icons in all layouts, emoji row,
  CrashFix build. Voice dictation + Tenor GIFs are broken in all versions
  (feature splits excluded — do not try to fix them). `classes2.dex`
  method_ids is EXACTLY 65536 (the 16-bit max) — **no new method refs
  possible anywhere in classes2.dex**.
- **Def-parse path (the customization route — see `POPUP_MECHANISM.md`
  §§11–12 for the full chain):** the keyboard definition is parsed at
  keyboard-load time, long before any popup builds. The real path is
  **template-placeholder binding** (NOT a hashCode switch — the string
  `"long_press_data"` exists in NO dex and its hashCode 484777929 is in
  NO switch payload): fd2.xml `<softkey_list splitter="," template_id="@id/0x7f0b2665">`
  → `Lpxa.a` → `Lpwz` template binding (`Lpub.e` → `Lptq.c` → `Lptw.a` →
  `Lpvb.h`, which splits the 9-token string and parses each `U+XXXX` via
  `Lpvb.f` → 9-element `[I`, item[3] = 0x1F496 for A) → `Lpwz.f` insn 110
  = `INVOKE_VIRTUAL Lpud.m([I,[Lpuz;,[Object;)V` → `Lpud.m` builds the
  `Lpva` list → `Lpwz.f` insn 198 `Lpud.c()` → ActionDef → `Lqml.c`
  insns 24–25 → `Lqml->e` (the list the popup uses). The def cache is
  **in-memory only** (Levj/IMEManager); force-stop clears it, so the def
  IS re-parsed after a force-stop. Def-parse runs on a **background
  executor thread** during early process startup.

## 3. The exact current problem

| Build | Code | Result |
|---|---|---|
| Fix11 | 176071849 | **OWNER'S FAVOURITE — WORKS.** XML-only (classes2.dex byte-identical to Fix8). Side-key UP/DOWN straight up/down (+ shape), popup opens and holds while pressed, glide smooth. No customization (no hook at all). |
| Phase 3 | 176071851 | Editor WORKS (owner could customize), **but the long-press popup NEVER appears** (4th occurrence). Cause PROVEN: 7 field reads from `Object`-typed registers (`List.get()` return) with no narrowing in the `Lqmu.c()` hook → VerifyError at class load (`diagnoses/diagnosis-hook-851.md`). |
| Phase 3 Fix | 176071852 | **Popup opens** (7 `INSTANCE_OF` + `IF_EQZ -> GUARD` narrowings). Swipe works, emoji inserts. New issue: popup FLICKERS at open; also the Q–P popups contain a DIGIT cell, and the number row shows ON by default after a fresh install. |
| 853 | 176071853 | Number row OFF by default (APK default was ALREADY false — the fresh-install ON was Android Auto Backup restoring the old pref; fix = manual toggle) + keyboard height ratio 1.0 → 1.2. |
| 854 | 176071854 | Digit fix (removed the 10 D01–D10 `<merge>` lines from cae.xml's `<else>` block) + `show_animation` removed (flicker attempt). **Flicker got WORSE: popup comes and goes repeatedly while HOLDING.** |
| 855 | 176071855 | `show_animation` restored. **During HOLD the popup does NOT come at all; on RELEASE it appears and goes away.** Owner's directive: make the animation EXACTLY like Fix11 (his top favourite); customization he will do himself from Settings. |
| 856 | 176071856 | **`Lqmu.c()` reverted to Fix11's exact 101-insn version. POPUP WORKING — ROLLBACK BUILD.** Editor still opens and saves, but no code reads the prefs into the popup (custom emojis do NOT appear). **This is the current working build — go back to it.** |
| 857 | 176071857 | Customization via a hook at the LIST LOAD in `Lqml.c()` (insn 25). **REGRESSION — popup does NOT appear at all** (5th occurrence). ONE defect PROVEN: v13 colId clobber (the clone loop stores `List.add()`'s boolean into v13, the column class, so it is ALWAYS 1 = LEFT after the loop) — explains WRONG slots, NOT the missing popup (`diagnoses/diagnosis-857.md`). |
| 858 | 176071858 | **Def-parse hook** in `Lpud.m` (insn 0, keyboard-def load time; long-press codes UNTOUCHED — classes2 byte-identical to Fix11). **Customization STILL does NOT reflect**, even after force-stop (owner's words: "I UNINSTALLED PREVIOUS VERSION THN INSTALLED THIS. STILL JO CUSTOMIZE KRRA HUN WO YHAN WORK NHI KRRA H"). |
| 859 | 176071859 | **Toast diagnostic** (instrumentation ONLY in `Lpud.m`, no behaviour change). Owner: **NO Toast appeared at all** (not even "DEF 859: Lpud.m runs"), popup showed defaults. |
| 860 | 176071860 | Context fix (`Lesb.onCreate()` stores `this` into `Lpud->appCtx`) + persistent diagnostic (SharedPreferences `nobi_diag`, shown in the editor's Diagnostics section). **Customization STILL does not show. Diagnostics shows "No record yet" for both fields, even after force-stop + keyboard open.** |
| 861 | 176071861 | **BROKEN — SUPERSEDED by 862.** After installing 861 over 860, the keyboard does NOT open AT ALL. Owner: "FR wahi dikkt keyboard hi na open hona..." — he wants this fixed **permanently**. **Cause PROVEN** (`diagnoses/diagnosis-861.md`): the P0 Toast in `Lpud.m` throws on the no-Looper def-parse thread. DO NOT INSTALL 861. |
| 862 | 176071862 | **Toast fix** — all 4 Toast call sites in `Lpud.m` NOP'd in place (Log.e + `nobi_diag` writes kept) + ONE catch-all try block around the hook region → GUARD. Delivered (Drive `1Wet60ytz-D3GLPjUDRXFs7uYiG2iBzTy`). Superseded by 863 (same fix, richer hook). Verdict: never installed by the owner (863 was delivered right after). |
| **863** | **176071863** | **CURRENT — TryBlock rewrite of `Lpud.m`** (481 insns / 908 units): catch-all try block, `diagCalls` / `diagNullCtxCount` counters, `split(",", -1)`, `last_run` on EVERY call, zero Toasts. Change A (delete the `attachBaseContext` hook) was REJECTED and NOT done — the hook is PROVEN safe. Delivered (Drive `1DQ6SryLn5OOwrW-TRW9fhOYfIU9cTW5E`). **CURRENT ISSUE — the phone reports "App not installed as package appears to be invalid" when installing 863** (screenshot 2026-11-15 12:39 IST, Downloads shows both 862 and 863 at 106 MB). The APK was verified good on the VM (unzip -t clean; apksigner "Verifies", v3=true, 1 signer — the SAME v1=false/v2=false/v3=true pattern as 856 and 861, which installed fine; aapt2 parses; keytool cert v2 MATCH; Drive size == local 105,632,361). **Leading explanation: truncated/corrupted download on the phone — advised fix is delete + re-download.** If a fresh download still fails, re-upload a fresh Drive copy and re-verify; do NOT rebuild blind. |

**What 861 changed (vs 860 — see `change-reports/176071861.md`):**
1. **Block L (the `last_run` write) moved OUTSIDE the `diagFirst` gate** so it
   runs on EVERY `Lpud.m` call (diagnosis-860 proved the 860 version only
   ran it on the FIRST call per process, and the Context was null on that
   first call — so the record was never written and never retried).
2. **`appCtx` set EARLIER via `Lmxa.attachBaseContext(Context)V`**
   (5 insns: `SPUT_OBJECT v1, Lpud->appCtx` + the original 4 insns) —
   the framework calls it on the Application instance BEFORE `onCreate()`.
   `Lesb.onCreate()`'s hook is kept as a second setter;
   `ActivityThread.currentApplication()` stays the final fallback.
   CanonDiff vs 860: classes.dex diff=2 (ONLY `Lpud.m` + `Lmxa.attachBaseContext`);
   **classes2 diff=0** (popup code byte-identical to Fix11). All gates
   passed. **And yet the keyboard does not open at all on the phone.**

## 4. The 861 keyboard-not-opening — RESOLVED (`diagnoses/diagnosis-861.md` PROVED the cause)

**`diagnoses/diagnosis-861.md` has landed and PROVED the root cause: the P0 Toast
in `Lpud.m` (`"DEF 859: Lpud.m runs"`, insns 44–48) fires on the FIRST `Lpud.m`
call of the process and `Toast.makeText()` throws `NullPointerException:
"Can't toast on a thread that has not called Looper.prepare()"` on the
no-Looper def-parse thread (proven from AOSP `Toast.java`). `Lpud.m` has
0 tries, so the throw propagates and the module-load FutureTask completes
exceptionally; `ModuleInfo.loadModule()` catches the `ExecutionException`
and returns null — no keyboard definition → the keyboard cannot open. The
failed Future is cached and reused, so it never opens again in that
process. This is the ONLY candidate in the 860→861 diff that can throw,
and it fits the device evidence exactly (861 fails, 860 works). The two
suspects below are kept as history — BOTH WERE REFUTED:**

1. **The `attachBaseContext` hook breaks Application startup.** The hook
   inserts `SPUT_OBJECT v1, Lpud->appCtx` as the FIRST instruction of
   `Lmxa.attachBaseContext`. That `SPUT_OBJECT` **triggers `Lpud`'s class
   initialization (`<clinit>`)** — which runs at Application attach time,
   before `onCreate()`, before MultiDex/install completes, and possibly
   before the classes the `<clinit>` touches are loadable. If
   `Lpud.<clinit>` throws (it creates an empty-byte-array helper `Lpux`;
   verify exactly what it does and what it depends on), the Application
   itself fails to attach → **the whole app dies at startup → the
   keyboard never opens, and no IME process ever exists to parse a def**.
   Note: in 860 the same `SPUT_OBJECT` ran inside `Lesb.onCreate()`, i.e.
   AFTER attach — so 860's hook could not break attach, and 861's is the
   first build to touch `Lpud` that early. **Test:** remove ONLY the
   `attachBaseContext` hook (keep everything else from 861) and see
   whether the keyboard opens. If it does, the hook point (not the
   `Lpud.m` change) is the cause.
   **VERDICT (diagnosis-861): REFUTED.** The hook is SAFE — original 4 insns
   are super `attachBaseContext` + `Lvqt.b(Context,Z)` (NO `MultiDex.install`,
   Application-only); the `SPUT_OBJECT` triggers a harmless `<clinit>`
   already triggered in 860; and the process stays alive (Settings/editor
   work on 861), so the Application did NOT die at startup. **Do NOT remove
   this hook — removing it restores the 860 failure (Context null on first
   call → GUARD → customization never applies).**
2. **The substitution now fires for the first time and throws during
   def-parse, killing the def load.** 861 is the first build in which the
   `Lpud.m` hook can actually READ the prefs (Context available on a
   later call, Block L' outside the gate). If the substitution path
   throws on the owner's real prefs value — e.g. a prefs string whose
   split/chain check passes but whose substitution hits an unexpected
   state (a `codePointAt` on an empty part despite the non-empty guard,
   a `List.set` index out of range on a non-9 list, a null array element
   from the clone) — the exception propagates out of `Lpud.m` during
   def-parse. If def-parse has no catch around `Lpud.m`, the def load
   fails → **no keyboard def → the keyboard never opens**. **Test:**
   inspect the GUARD coverage of the 861 `Lpud.m` hook instruction by
   instruction (every throwing op must be guarded); and check whether
   the owner's actual prefs value (`emoji_A` = `😂,🥰,💕,💖,😘`) passes
   the fingerprint/chain/split checks (it should — but verify against
   the shipped 861 dex, not the patcher source).
   **VERDICT (diagnosis-861): REFUTED as the throw source.** The substitution
   was traced throw-free for the owner's exact value
   (`😂,🥰,💕,💖,😘`): split = exactly 5 parts, all non-empty; slots
   in-bounds; `arraycopy` length-mismatch and `[I` null both REFUTED. **The
   actual throw was the P0 Toast** (the only newly-executing code in the
   860→861 diff that can throw).

**Ruled out already (do not re-investigate):** the popup code is
byte-identical to Fix11/856 (classes2 diff=0, md5 `87aeab3d…`), so the
cause is NOT in `Lqmu.c`/`Lqml.c`/`Lqmu.o`/`Lqmu.i`/`Lqml.k`. The IME
and the editor run in the SAME process (no `android:process` on either),
so SharedPreferences multi-process caching is NOT the cause. The def IS
re-parsed after force-stop (in-memory cache only — diagnosis-859/860).
`Lesb.onCreate()` DOES run (manifest Application = `HiltLatinApp` →
`Lesb` → `Lesf` → `Lmxa` → `Lpdy` → `android.app.Application`).

## 5. What a fix MUST do

1. **The keyboard ALWAYS opens, even if the hook fails.** This is the
   owner's standing order ("fix it permanently"). The hook must be
   **fail-safe**: any error in the hook path — null Context, bad prefs,
   bad split, unexpected list size, ANY exception — must land on GUARD
   and leave the original arrays untouched, so the keyboard opens with
   Fix11 defaults. A hook that can throw its way out of def-parse (or
   out of Application attach) is a defect by construction. If the
   fail-safe cannot be proven statically on the shipped dex, wrap the
   hook body so it cannot throw (e.g. a try/catch around the
   substitution that falls through to GUARD), and PROVE from the dump
   that every throwing instruction is covered.
2. **The customization works.** Long-press A with `emoji_A` =
   `😂,🥰,💕,💖,😘` saved must show 😂 in the CENTER slot (and 🥰 up,
   💕 down, 💖 left, 😘 right). The editor's Diagnostics section must
   show `run ts=<number>` and `A cp=1f496 ctx=static prefs=😂,🥰,💕,💖,😘 outcome=SUB OK`.
3. **The popup stays exactly Fix11's.** `classes2.dex` byte-identical to
   856 (md5 `87aeab3d…`); `Lqmu.c()` and `Lqml.c()` untouched. The owner
   explicitly directed: do NOT touch the long-press codes.
4. **Verified with the full gate set (§6) — never a blind build.** If the
   cause cannot be proven, build a diagnostic build instead and report
   what the device says (a diagnostic that CANNOT record its own failure
   mode — like 860's, which needed a Context to report a null Context —
   is a design defect; design the diag so its failure modes are
   observable, e.g. a record the editor can read even when the IME
   never runs).
5. **Do not reintroduce the popup regressions.** The popup has broken 5
   times (Fix4/Fix5/Fix10/Phase 3-851/857) from hooks in the popup path.
   The def-parse route exists precisely to avoid that. Any proposal to
   read the prefs at popup-build time instead (the owner's own fallback
   idea) needs the owner's EXPLICIT agreement AND all five safeguards:
   FieldCheckGen 0 with 852-style narrowing, the v13 colId fix, the
   empty-part guard, the full gate set with `Lqmu.c()` diff = 0, and a
   visible diag confirming it ran.

## 6. Every proven root cause so far (do not re-derive these — use them)

1. **835 `Lqml.k()` insn 88 — subclass method on superclass-typed `this`**
   (VerifyError). `k()` called `Lqmu->n()` on `this` statically typed
   `Lqml`. ART's verifier checks the invoke against the STATIC type;
   `INSTANCE_OF` does NOT narrow it — only `CHECK_CAST` does. Fix:
   `CHECK_CAST v10, Lqmu;` after the `INSTANCE_OF` guard (proven in 836).
2. **835/836 `Lqmu.o()` register growth — uninitialized-register use**
   (VerifyError). The 835 glide patch grew `o()` 6→9 registers (moving
   `this` v4→v7, param v5→v8) but kept the body verbatim, so it read
   never-written locals v4/v5. Fix: remap the body to the new registers
   (proven in 838). **Lesson: `RegInitCheck` proves initialization, NOT
   value correctness — a dump review of any remapped method body is still
   mandatory.**
3. **Fix4 `Lqmu.i()` v11 INT-vs-REF merge conflict at 027e** (VerifyError,
   PROVEN). Fix4's `i()` insertion wrote REFERENCE values into v11
   (`move-result-object` LayoutParams + `iget-object` tracker), but v11 is
   an INT on the 01f6 jump path (last write 012b `add-int/2addr v11, v5`).
   At merge 027e ART must merge INT vs `Lqmr` reference → whole `Lqmu`
   class fails verification at class load → popup never builds, contained
   by the long-press dispatch. The verifier is static: it rejects the
   class even though the 01f6 jump is (almost certainly) never taken at
   runtime.
4. **Fix5's v19 fix (correct, but the regression persisted).** Fix5 uses
   int-only scratch **v19** (I32 at entry, DEAD after the insertion at
   abs 0x21a; v11 and v23 are unusable — both are the HIGH half of a live
   double: v10/v11 `move-wide` at 0x157 read again at 0x294 `mul-double`;
   v22/v23 `sget-wide` at 0xaa/0xcc; a plain grep for "v23" finds 0
   occurrences because wide values are named by their LOW half only).
5. **Fix5's dex has NO provable defect location (location-level proof).**
   TypeMergeCheck compared **conflict LOCATIONS, not counts**: Fix5
   `Lqmu.i` conflict set == stock 839, location-for-location (25 each;
   same join addresses modulo the +48 shift, same registers, same type
   pairs). **Conclusion: do NOT ship a Fix6 that re-patches `o()`/`i()` on
   a guess. The next step is device evidence (the diag), not another
   blind patch.**
6. **The null-LayoutParams hypothesis was RULED OUT.** `Lqmu.h` is
   assigned in exactly one place — `SilkRectangularPopupView.onFinishInflate()`
   insn 003c from `findViewById(0x7f0b0709)` — an inflated child of layout
   `0x7f0e0607.xml`, so `getLayoutParams()` is non-null. Stock
   `onFinishInflate` itself reads `MarginLayoutParams` with no null check.
7. **Phase 3 (851) — 7 field reads from `Object`-typed registers**
   (VerifyError, PROVEN by FieldCheck). The `Lqmu.c()` hook read fields
   (`Lpva->d`, `Lqmc->b`, `Lqmc->d`) from registers straight out of
   `List.get()` (declared return `Object`) with NO narrowing. Fix:
   `INSTANCE_OF <S>, <obj>, <Type>;` + `IF_EQZ <S> -> GUARD` before
   EVERY such field read (proven in 852 — FieldCheck 7 → 0).
8. **857 — v13 colId clobber** (PROVEN value-flow bug). The `Lqml.c()`
   clone loop stored `List.add()`'s boolean result into v13 — the
   column class (0=center, 1=left, 2=right) — so after the 9-iteration
   loop v13 is ALWAYS 1 (LEFT). **No gate checks register VALUES** (only
   types, init, branches, alignment, refs) — this defect is invisible
   to the whole gate set. **Every `MOVE_RESULT` destination must be
   proven a dead scratch register, verified by hand on the dump.**
9. **858/859/860 — the Context is null on the first `Lpud.m` call**
   (diagnosis-859/860). Def-parse runs on a background executor thread
   very early in process startup; `ActivityThread.currentApplication()`
   is null at that point, and the 860 Block L write was gated by the
   `diagFirst` flag, so it ran at most once — on the call where the
   Context was null — and was never retried. Same null Context makes the
   substitution hook go to GUARD → defaults. **Structural flaw in the
   860 diagnostic: it cannot record a null Context** (writing
   SharedPreferences requires a Context), so "No record yet" is exactly
   what a null Context produces.
10. **Earlier mechanism facts (do not re-litigate):** the tracker MUST be
    driven — init to center (1,1) at open + route touch through `Lqmu.i`;
    early-returning out of `k()` without replacing the tracker update
    freezes the tracker at its build-time point (1,2) = DOWN emoji
    (Phase 2Fix3 diagnosis). `Lqmu.o()` both sets the indicator AND
    starts nothing — the 300 ms AnimatorSet in `Lqmu.i()` is the SOLE
    glide driver (Fix3 proved two competing animators on the same
    properties cause the jump).

## 7. The gates that MUST pass on any fix

Run on the FINAL signed APK. Each gate caught a defect the others missed.
None runs ART's real verifier — a `VerifyError` is only caught by the
combination + a dump review.

- **FieldCheckGen = 0** violations on EVERY patched method (ALL-dex load:
  a class referenced by the method may live in another dex). **Use
  FieldCheckGen, NOT stock FieldCheck** — stock FieldCheck has a
  hardcoded `if (m.getParameters().size() != 7) continue;` that SILENTLY
  SKIPS any method without exactly 7 parameters, checking NOTHING
  (vacuous pass — proven in the 858 build). **Always verify a gate
  actually ran** (it must report the method's insn count) — silence is
  not a pass.
- **RegInitCheck = 0** violations on EVERY patched method. Use the FIXED
  version in `tools/src/` (its `destReg()` handles `MOVE_RESULT_WIDE` —
  the stock one silently missed wide-result reads). Must-analysis from
  TOP (greatest fixpoint); unreachable code gets TOP.
- **VerifyPatch = 0** violations (invoke register counts vs prototypes —
  wide args count double — + object-type assignability; a subclass method
  invoked on a superclass-typed object needs a preceding `CHECK_CAST`;
  `INSTANCE_OF` does NOT narrow the static type).
- **VerifyBranches = 0 bad** (all six dex).
- **Payload alignment = 0 odd** (all six dex).
- **TypeMergeCheck location-identical to stock** — compare the conflict
  SET (addresses, registers, type pairs), not just the count. Zero NEW
  conflicts on any patched method. Caveat: it has false positives on
  stock wide-value reuse (no wide-pair invalidation model — stock 839
  `Lqmu.i` shows 25), and it CRASHES on methods with a wide (J)
  parameter (e.g. `Lqck.D`) — a tool limitation, not a defect; use
  RegInitCheck + VerifyPatch + manual dump review for those.
- **CanonDiff minimal** — only the intended methods change vs the base
  build. Never use raw DexDiff on re-serialized dex (tens of thousands
  of false diffs from object-identity hashes in `Instruction.toString()`).
- **RefResolve = 0 NEW unresolved refs** (43 pre-existing
  framework-inherited false positives are identical in working builds;
  run it WITH class-filter arguments — a no-filter run checks NOTHING
  and is vacuous).
- **MOVE_RESULT dead-scratch check (by hand, on the dump):** every
  `MOVE_RESULT` / `MOVE_RESULT_OBJECT` / `MOVE_RESULT_WIDE` destination
  must be a proven-dead scratch register (the 857 v13 clobber is
  invisible to every gate above).
- **Check `ReferenceInstruction` refs, NEVER `Instruction.toString()`** —
  `toString()` does NOT include the referenced item (method name /
  string value), so a `contains("codePointAt")` check on it is VACUOUS.
- **Cert v2 MATCH** (SHA-256 `80:A9:47:FF:…:DA:7E`), **versionCode bumped**
  (manifest versionCode lives in `apktool.yml`, NOT in classes5 — bump
  BOTH), **14 `.so` 0 misaligned at 16 KB** (`zipalign -c -P 16`),
  entry count 9,465.
- **Dump-verify the fix on the final dex** (the hook's branch targets,
  the GUARD coverage of every throwing instruction, the `Lpud.m` head
  showing Block L' before the `diagFirst` gate if that design is kept).

## 8. What to deliver

A build (next code 176071864 — builds 862 and 863 are already built and delivered; see §3) in which, on the owner's phone:
**the keyboard ALWAYS opens** (even if the owner has customized emojis,
even if the prefs file is missing or malformed, even on a fresh install);
long-press A shows 😂 in the CENTER slot (customization from Settings
applies); the popup is exactly Fix11's (opens, holds while pressed,
center default, swipe, glide, insert); the editor's Diagnostics shows
`run ts=<number>` + `A cp=1f496 ctx=static prefs=😂,🥰,💕,💖,😘 outcome=SUB OK`.
Deliver with: file size + SHA-256, versionCode, cert check, entry count,
gate results (all of §7), CanonDiff vs the base build (classes2 MUST be
0), and a short change report stating the root cause with dex evidence
(method, instruction, register, merging types). **If the cause cannot be
proven, build a diagnostic build instead** and report what the device
says — do NOT ship a blind patch. The diagnosis of 861
(`diagnoses/diagnosis-861.md`) has landed and PROVED the cause (P0 Toast
— see §4); start from §3/§4. The current blocker is the 863 INSTALL
failure, not the keyboard-opening bug — verify a fresh download first
before changing any code.

## 9. Build traps (every one of these has cost a build)

1. Decode ONLY with `apktool d -s` — plain `apktool d` round-trips ALL dex
   through smali and silently SHRINKS them (classes.dex 12.3 MB → 9.4 MB).
2. ALWAYS delete `decoded/build/` before rebuilding — `apktool b` reuses
   intermediates and ignores an edited `apktool.yml` (stamps the old
   versionCode).
3. Dex patches in ONE dexlib2 pass (one `*CombinedPatch` driver). Two
   serializations of the same dex break the app at startup, even when
   every gate passes.
4. Hook inserts must total an EVEN number of code units (pad with `nop`) —
   odd deltas shift switch/array payloads to odd offsets and ART rejects
   the class at load (V44 crash). Verify payload alignment explicitly.
5. FIELD refs use the real dex type (`Lpeq;` / `Lovc;`); METHOD refs use
   the helper declaration (`Ldefpackage/…`). A field ref whose type ≠ its
   `.field` declaration = VerifyError at class load, app dies at startup
   before the crash reporter initializes → NO log.
6. A failed `apktool b` leaves a STALE unsigned APK (old dex, old
   versionCode, plausible size/date). ALWAYS md5-check the dex inside the
   unsigned APK before signing.
7. dexdump output can be silently TRUNCATED — confirm dump completeness
   (line count / class present) before drawing conclusions.
8. Decode NEVER in /tmp (512 MB tmpfs — a full decode fills it). Decode
   under the workspace.
9. dexlib2 3.0.3 is CORRUPT (554 B) — use dexlib2 2.5.2 + `guava.jar`
   (without Guava, every ref search silently returns 0).
10. uber-apk-signer needs `--ksAlias nobiboard` and `--ksKeyPass` (same
    value as `--ksPass`, else it stalls silently waiting for the password).
11. When a method grows registers, the body must be remapped to the new
    registers — keeping the body verbatim reads never-written locals
    (836 `o()` defect).
12. `CONST_4` literal range is −8..7 — a value of 9 in `CONST_4` is
    invalid; use `CONST_16` (858 build).
13. dexlib2's 4-bit register limit: 4-bit-format instructions can only use
    registers v0–v15 — a patcher using v16+ fails with `Invalid register:
    v16` (857 build).
14. A `SPUT_OBJECT` into a class triggers that class's `<clinit>` — if the
    hook runs at Application attach time, the `<clinit>` runs before
    `onCreate()` and before the app is fully loaded. Verify what the
    `<clinit>` does before hooking `attachBaseContext` (861 suspect #1 — REFUTED by diagnosis-861; the hook is safe, do NOT remove it).

## 10. Hard boundaries (do not cross these for any reason)

- **NO signature / integrity bypass.** The work is binary patching of a
  personal-use rebrand (own key, own package, own features). Never add a
  bypass for Gboard's signature or integrity checks, and never propose one.
- **Personal use only.** Gboard's engine is Google's. Do not redistribute
  Google's binaries (no APK/dex/decoded-res in any public repo or share).
- **No blind patches.** Diagnose from dex and/or device evidence first.
  If the cause cannot be proven, build a diagnostic build and read the
  device result.
- **Do not rewrite the app for a small feature, do not fake functionality,
  verify before declaring done.** (Owner's standing Android master prompt:
  professional-engineering standard — inspect before modifying, reuse
  before creating, verify before declaring done.)
- **Do not touch the long-press codes** (`Lqmu.c`, `Lqml.c`, `Lqmu.o`,
  `Lqmu.i`, `Lqml.k`, `Lqmu.e`) — the owner's standing directive since
  2026-11-15 09:15 IST, reaffirmed by the 861 regression. A popup-path
  hook needs the owner's EXPLICIT agreement (see §5.5).

## 11. Repo map (what to read first)

**Read order: `POPUP_MECHANISM.md` first, then the smali, then the dumps/,
then the change reports, then the diagnoses.** The mechanism doc is the
map; the smali is the actual popup/long-press code; the dumps are short
excerpts of the same methods.

- `POPUP_MECHANISM.md` — standalone explanation of the popup +
  long-press code (end-to-end flow, each class's role + key methods,
  tracker mapping, `o()`/`i()` centring, PLUS grid mapping, the `Lqck.D`
  catch-all try, the def-parse path, the Context-timing problem, the
  editor/prefs contract, current state + what is still unknown).
- `smali/` — baksmali disassembly (`pud.smali` from the shipped 863 APK,
  code 176071863; the rest from the shipped 861 APK) of the
  popup/long-press classes: `qmu` (controller),
  `qml` (base/touch handler), `qmr` (tracker), `qck` (`D(J)V`),
  `qme` (nearest-item search), `qmc` (selected-item descriptor),
  `qmf` (cell-size helper), `piy` (long-press Runnable), `qms`/`qmt`
  (animation), `tga` (view helper), `pud` (def-parse hook), `mxa`
  (`attachBaseContext` hook), `esb` (`onCreate` hook), and
  `com/google/android/libraries/inputmethod/popup/SilkRectangularPopupView`
  (the popup view). A short, scoped excerpt for diagnosis — not the
  whole app.
- `change-reports/176071851.md` … `176071863.md` — one change report per
  build from Phase 3 (851) to the current build (863).
- `diagnoses/` — `diagnosis-857.md` (v13 colId clobber + the missing-popup
  paradox), `diagnosis-859.md` (`Lpud.m` IS the right method; Context-null
  leading cause), `diagnosis-860.md` (Block L gated by `diagFirst` +
  Context null on first call), `diagnosis-hook-851.md` (the 7
  field-access violations that killed 851's popup). `diagnosis-861.md`
  (the 861 keyboard-not-opening issue — ROOT CAUSE PROVEN: P0 Toast
  throws on the no-Looper def-parse thread; `attachBaseContext` and the
  substitution both REFUTED).
- `dumps/` — dexdump excerpts: `Lqmu.c-861`, `Lpud.m-861`, `Lpud.m-862`,
  `Lpud.m-863`, `Lmxa-attachBaseContext-861`, `Lesb-onCreate-861`
  (positions stripped),
  plus the Fix5-era excerpts (`Lqmu.o`, `Lqmu.i`, `Lqml.k`, `Lqmu.c`,
  `Lqml.e`, `Lqmu.n/p/m`, `Lqck.D` diag version).
- `editor/src/` — the customization editor source
  (`NobiEmojiEditorActivity.java` incl. the 860 Diagnostics section,
  `Defaults.java`, `EmojiData.java`).
- `docs/phase3-spec.md` — the Phase 3 editor spec (approved by the owner).
- `tools/src/` — all 119 patch + verification tools (see `TOOLS.md`).
- `TOOLS.md` — compile/run commands and per-tool reference.
- `README.md` — project status, build flow, repo layout.
