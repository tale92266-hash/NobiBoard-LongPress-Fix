# NobiBoard — long-press emoji popup: source, tools, and diagnosis

NobiBoard is a personal-use rebrand of Google's Gboard 18.6.3 (package
`com.nobi.nobiboard`), built by binary patching: the caps-lock fix, a
custom emoji row, and a long-press emoji popup (5 emojis per letter key:
1 center + 4 around it, swipe to select, green highlight circle, smooth
glide, emoji scale-up). The keyboard engine is still Google's Gboard.
**Personal use only.**

This repo is the complete working source for the long-press popup arc
(2026-11-11 → 2026-11-15): the dex-patching Java tools, the verification
gates, the change reports, the diagnosis documents, the customization
editor source, and plain-text dexdump excerpts of the key methods. It
contains **no Google binaries**: no APK, no dex, no decoded resources,
no keystore. See
[What is NOT in this repo](#what-is-not-in-this-repo) below.

## Status (2026-11-15 ~11:45 IST)

| Build | Code | Result |
|---|---|---|
| Fix11 | 176071849 | **OWNER'S FAVOURITE — WORKS.** XML-only (classes2.dex byte-identical to Fix8). Side-key UP/DOWN straight up/down (+ shape), popup opens and holds while pressed, glide smooth. No customization. |
| Phase 3 | 176071851 | Editor WORKS, **popup NEVER appears**. Cause PROVEN: 7 field reads from `Object`-typed registers in the `Lqmu.c()` hook → VerifyError at class load. |
| Phase 3 Fix | 176071852 | **Popup opens** (7 narrowings). Flickers at open; Q–P popups contain a DIGIT cell; number row ON after fresh install (Auto Backup restore). |
| 853 | 176071853 | Number-row default already false (fix = manual toggle) + keyboard height ratio 1.0 → 1.2. |
| 854 | 176071854 | Digit fix (10 D01–D10 merges removed) + `show_animation` removed. **Flicker WORSE: popup comes and goes while HOLDING.** |
| 855 | 176071855 | `show_animation` restored. **During HOLD no popup; on RELEASE it appears and goes away.** |
| **856** | **176071856** | **`Lqmu.c()` reverted to Fix11's exact 101-insn version. POPUP WORKING — CURRENT ROLLBACK BUILD.** Editor saves, but no code reads the prefs (no customization). |
| 857 | 176071857 | Hook at the LIST LOAD in `Lqml.c()` (insn 25). **REGRESSION — popup gone** (5th). ONE defect PROVEN: v13 colId clobber. |
| 858 | 176071858 | **Def-parse hook** in `Lpud.m` (insn 0; long-press codes UNTOUCHED, classes2 = Fix11). **Customization STILL does NOT reflect**, even after force-stop. |
| 859 | 176071859 | **Toast diagnostic** (only `Lpud.m`). Owner: **NO Toast at all**, popup showed defaults. |
| 860 | 176071860 | Context fix (`Lesb.onCreate()` → `Lpud->appCtx`) + `nobi_diag` diagnostic (editor Diagnostics section). **Customization STILL does not show. Diagnostics "No record yet" even after force-stop + keyboard open.** |
| **861** | **176071861** | **CURRENT — BROKEN. Keyboard does NOT open AT ALL after installing over 860.** Owner wants it fixed **permanently**. **DO NOT INSTALL. Rollback is 856.** Cause TBD — `diagnoses/diagnosis-861.md` pending; prime suspects in `PROBLEM_PROMPT.md` §4. |

**The open problem:** make the keyboard ALWAYS open (even if the hook
fails) AND make the customization from Settings show in the popup, with
the popup exactly Fix11's. See `PROBLEM_PROMPT.md` for the complete
standalone problem statement (written so another AI with no prior
context can pick it up).

## Base (NOT included — you must supply it yourself)

- Stock base: Gboard 18.6.3 XAPK, `arm64-v8a`, package
  `com.google.android.inputmethod.latin`, original versionCode 176071766.
  Base APK SHA-256 `2d1d060c…ab875e`.
- The base is Google's proprietary binary. It is NOT in this repo and
  must never be uploaded to a public repo. Supply your own copy, decode
  it with `apktool d -s`, and apply the tools to the decoded tree.
- Signing: each app has ONE fixed signing key. The NobiBoard v2 key is
  NOT in this repo (never commit a keystore). Sign with your own key, or
  with the owner's fixed v2 key if you have it (cert SHA-256
  `80:A9:47:FF:8B:5D:A7:4F:96:4A:F4:2C:A0:B9:6A:6C:EB:D5:48:68:AD:61:86:9D:6F:4F:88:05:86:B6:DA:7E`).

## Toolchain

- Java 17 (JDK 17.0.21 used here; `javac`/`java` from the JDK).
- apktool 3.0.3 (`apktool-3.0.3.jar`).
- dexlib2 **2.5.2** + Guava (both Apache-licensed). In the accompanying
  ZIP they are in `tools/lib/`; in the GitHub repo they are NOT included
  (GitHub push accepts UTF-8 text only) — download them from Maven
  Central, see `TOOLS.md` for the exact URLs. dexlib2 3.0.3 is CORRUPT —
  do not use it.
- uber-apk-signer 1.3.0 (for signing; not included — obtain it yourself).
- Android build-tools 35.0.0 (`aapt2`, `dexdump`, `zipalign`) for
  verification.
- Device: the builds were tested on a vivo iQOO Z10, Android 16, not
  rooted. The phone test is the only verdict — never call a build done
  from static checks alone.

## Build flow (the traps are real — read this before touching anything)

1. **Decode ONLY with `apktool d -s`.** Plain `apktool d` round-trips ALL
   dex through smali and silently SHRINKS them (classes.dex 12.3 MB →
   9.4 MB).
2. **ALWAYS delete `decoded/build/` before rebuilding.** `apktool b`
   reuses intermediates and ignores an edited `apktool.yml` (stamps the
   old versionCode).
3. **Dex patches in ONE dexlib2 pass** (one driver, `*CombinedPatch`).
   Two serializations of the same dex break the app at startup, even when
   every gate passes.
4. **Hook inserts must total an EVEN number of code units** (pad with
   `nop`). Odd deltas shift switch/array payloads to odd offsets and ART
   rejects the class at load (V44 crash).
5. **FIELD refs use the real dex type** (`Lpeq;`); **METHOD refs use the
   helper declaration** (`Ldefpackage/…`). A field ref whose type ≠ its
   `.field` declaration = VerifyError at class load.
6. **A failed `apktool b` leaves a STALE unsigned APK** (old dex, old
   versionCode, plausible size/date). ALWAYS md5-check the dex inside the
   unsigned APK before signing.
7. **Manifest versionCode lives in `decoded/apktool.yml`, NOT in
   classes5.** Bump BOTH (the `DexVersionBump*` tools only bump the
   in-code string), and confirm the final APK with `aapt2 dump badging`.
8. **Page-align 16 KB `.so` BEFORE signing** (Android 15+/16 needs 16 KB
   alignment, not 4 KB). Verify with `zipalign -c -P 16`.
9. **Decode NEVER in /tmp** (512 MB tmpfs — a full decode fills it).
   Decode under the workspace, and use `-Djava.io.tmpdir=<workspace>`
   for apktool if needed.

A full build: decode → apply ONE `*CombinedPatch` driver to classes2.dex
(+ `DexVersionBump*` on classes5) → rebuild → page-align → sign with the
fixed v2 key → verify (see below) → deliver as ONE Drive link.

## Verification gates (ALL mandatory on every patched method)

Run on the FINAL signed APK. Each gate caught a defect the others missed.
None of them runs ART's real verifier — a `VerifyError` is only caught by
the combination + a dump review.

| Gate | Tool | Must be |
|---|---|---|
| Uninitialized registers | `RegInitCheck` (fixed version) | 0 violations on every patched method |
| Field-access receiver types | `FieldCheckGen` (NOT stock `FieldCheck` — its 7-param filter silently skips non-7-param methods) | 0 violations on every patched method |
| Invoke counts + assignability | `VerifyPatch` | 0 violations |
| Branch targets | `VerifyBranches` | 0 bad |
| Payload alignment | `CheckPayloadAlignment` | 0 odd |
| Register-type merges | `TypeMergeCheck` | conflict set **location-identical** to stock (counts alone are NOT enough) |
| Dex diff | `CanonDiff` | minimal — only the intended methods |
| Ref resolution | `RefResolve` (WITH class filters — a no-filter run checks NOTHING) | 0 NEW unresolved refs (43 pre-existing framework false positives are identical in working builds) |
| `MOVE_RESULT` destinations | **by hand, on the dump** | every `MOVE_RESULT` into a proven-dead scratch register (no gate checks register VALUES — the 857 v13 colId clobber was invisible to all of them) |
| Cert / version / alignment | `keytool`, `aapt2`, `zipalign` | cert v2 MATCH, versionCode bumped, 14 `.so` 0 misaligned at 16 KB |

See `TOOLS.md` for the full tool list, compile command, and usage.

## Repository layout

```
README.md            this file
PROBLEM_PROMPT.md    standalone problem statement for another AI (no prior context needed)
POPUP_MECHANISM.md   how the popup + long-press code actually works (READ FIRST, then the smali)
TOOLS.md             every tool, what it does, compile/run commands
apktool.yml          the apktool config from the 861 build (text; versionCode must be bumped per build)
manifest-summary.txt package / versionCode / SDKs / launchable activity (summary only, NOT the full manifest)
smali/
  qmu.smali qml.smali qmr.smali qck.smali qme.smali qmc.smali qmf.smali
  piy.smali qms.smali qmt.smali tga.smali
  pud.smali mxa.smali esb.smali
                     baksmali disassembly (from the shipped 861 APK, code 176071861)
                     of the popup/long-press classes + the def-parse/Context hook classes
  com/google/android/libraries/inputmethod/popup/
    SilkRectangularPopupView.smali
                     the popup view itself
  README.md          per-file source-version notes
tools/
  src/*.java         all 114 patch + verification tools (default package; dexlib2 2.5.2 + Guava)
  lib/               dexlib2-2.5.2.jar, guava.jar (Apache-licensed; in the ZIP only, NOT in the git repo)
docs/
  change-report-fix3.md     Fix3 (176071839, WORKING) — glide fix
  change-report-fix4.md     Fix4 (176071840, REGRESSED) — centering attempt, v11 defect
  change-report-fix5.md     Fix5 (176071841, REGRESSED) — v19 scratch + null-safe fallbacks
  change-report-fix5diag.md Fix5-Diag (176071842) — Lqck.D instrumentation, location-level proof
  diagnosis.md              835 regression diagnosis (two VerifyError defects)
  diagnosis-regression.md   (same diagnosis, full version)
  diagnosis-regression-2.md 836 diagnosis — no static cause remains; device hypotheses
  diagnosis-tracker.md      Phase 2Fix3 diagnosis — tracker frozen at (1,2), swipe dead
  phase3-spec.md            Phase 3 editor spec (owner-approved)
change-reports/
  176071851.md ... 176071861.md   one change report per build from Phase 3 (851) to 861
diagnoses/
  diagnosis-857.md          v13 colId clobber + the missing-popup paradox
  diagnosis-859.md          Lpud.m IS the right method; Context-null leading cause
  diagnosis-860.md          Block L gated by diagFirst + Context null on first call
  diagnosis-hook-851.md     the 7 field-access violations that killed 851's popup
  diagnosis-861.md          PENDING — the current keyboard-not-opening issue
editor/
  src/com/nobi/nobiboard/
    NobiEmojiEditorActivity.java   the customization editor (incl. the 860 Diagnostics section)
    Defaults.java  EmojiData.java  default emojis + the 246-emoji picker data
dumps/
  Lpud-m-861.txt  Lmxa-attachBaseContext-861.txt  Lesb-onCreate-861.txt  Lqmu-c-861.txt
                     (dexdump excerpts from 861, positions stripped)
  Lqmu-o-fix5.txt  Lqmu.i-fix5.txt  Lqml-k-fix5.txt  Lqmu-c-fix5.txt
  Lqml-e-fix5.txt  Lqmu-n-fix5.txt  Lqmu-p-fix5.txt  Lqmu-m-fix5.txt
  Lqmu-e-fix5.txt  Lqck-D-fix5diag.txt   (dexdump excerpts from Fix5, positions stripped)
```

## What is NOT in this repo (and why)

- **No .apk / .dex / decoded res/ / binary AndroidManifest.** These are
  Google's proprietary Gboard binaries. Redistributing them publicly is
  an IP problem. The repo contains only original patch/verification tools,
  written documentation, short dexdump text excerpts, and the `smali/`
  disassembly of ONLY the 12 popup/long-press classes (the minimum
  needed to reason about the bytecode) — not the whole app.
- **No signing keystore (*.p12) and no password.** Never commit a key or
  a credential. The cert fingerprint is documented so a build can be
  verified, but the key itself stays with the owner.
- **No APKs in the ZIP either.** Same reason.
- **No personal data** beyond what is already in the change reports.

## Hard boundaries

- **No signature / integrity bypass.** The work is binary patching of a
  personal-use rebrand (own key, own package, own features). Never add a
  bypass for Gboard's signature or integrity checks.
- **No blind patches.** Every fix must be proven from the dex and/or
  device evidence first (the 835/836/Fix4/Fix5 arc is a catalogue of what
  happens otherwise). If the cause cannot be proven, build a diagnostic
  build instead and read the device result.
- **The phone test is the only verdict.** Static gates passing does not
  mean the build works.

## The feature (for orientation)

Long-press a letter key → a circle popup (84×84dp, 28×28dp cells) shows
5 themed emojis in a PLUS layout: center + up / down / left / right.
List order in `0x7f170fd2.xml`: `[DOWN, _, _, CENTER, RIGHT, LEFT, UP, _, _]`
(center = index 3). The green highlight circle (indicator `Lqmu.h`,
40×40dp, `#ff4db6ac`) sits on the selected emoji; the stock 300 ms
AnimatorSet in `Lqmu.i()` glides it between cells; the emoji scales up
on selection; releasing inserts the highlighted emoji. The popup is the
SILK popup (`SilkRectangularPopupView`, controller `Lqmu`, spatial
tracker `Lqmr`). `Lqck.D(J)V` is the popup-creation entry and has one
catch-all try block covering the whole build; the handler ends the Trace
section and rethrows, and the failure is contained in the long-press
dispatch — which is why every popup-build failure in this arc presented
as "no popup, no crash". See `POPUP_MECHANISM.md` §8 for the exact
handler and the correction to the earlier "swallows" wording.

**Customization:** Settings → Preferences → Appearance → "Long-press
emojis" opens the editor (`editor/src/`). It saves to SharedPreferences
file `nobi_longpress_emoji`, keys `emoji_A` … `emoji_Z`, value = 5 parts
(`center,up,down,left,right`) joined by `,`; a cleared slot is `U+200B`.
The def-parse hook in `Lpud.m` (see `POPUP_MECHANISM.md` §11) reads
those prefs at keyboard-load time and substitutes the custom emojis
into the popup lists — the long-press codes themselves are untouched
(classes2 byte-identical to Fix11). The editor's Diagnostics section
shows what the def-parse hook recorded, via SharedPreferences file
`nobi_diag`.
