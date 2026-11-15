# TOOLS.md — NobiBoard dex-patching and verification tools

All tools are plain Java (default package, no `package` declaration). They
use **dexlib2 2.5.2** and **Guava**. Java 17 is required.

**Obtaining dexlib2 + Guava:** the two jars live in `tools/lib/` in the
accompanying ZIP, but they are **NOT in the GitHub repo** (the GitHub
push tool accepts UTF-8 text only — binaries are rejected). Download them
from Maven Central instead:
- dexlib2 2.5.2: `https://repo1.maven.org/maven2/org/smali/dexlib2/2.5.2/dexlib2-2.5.2.jar`
- Guava (any recent 32.x JRE build, e.g. 32.1.2-jre):
  `https://repo1.maven.org/maven2/com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar`
  Save both as `tools/lib/dexlib2-2.5.2.jar` and `tools/lib/guava.jar`.
  Both are Apache-licensed.

## Build (verified 2026-11-15 — all 114 sources compile clean)

```bash
cd <repo>
mkdir -p tools/classes
javac -encoding UTF-8 -cp tools/lib/dexlib2-2.5.2.jar:tools/lib/guava.jar \
  -d tools/classes tools/src/*.java
```

(The `-encoding UTF-8` flag is REQUIRED: several sources contain UTF-8
em-dashes in comments and plain `javac` defaults to US-ASCII and fails.)

## Run

```bash
CP=tools/classes:tools/lib/dexlib2-2.5.2.jar:tools/lib/guava.jar
java -cp "$CP" <ToolName> <args...>
```

dexlib2 2.5.2 is the pinned version. **dexlib2 3.0.3 is CORRUPT (554 B) —
do not use it.** Without `guava.jar` on the classpath every ref search
silently returns 0 (`NoClassDefFoundError: RangeMap`).

---

## Patch drivers (ONE dexlib2 read+write pass per build — never two)

Each `*CombinedPatch` is the driver for one build: it reads the input dex
once, applies its patch(es), and writes once. Dex patches MUST be done in
a single pass — two serializations of the same dex break the app at
startup (V27 lesson), even when every gate passes.

| Tool | Usage | What it does |
|---|---|---|
| `Fix5CombinedPatch` | `Fix5CombinedPatch <input840.dex> <output.dex>` | Latest fix. Applies `Fix5CenterPatch` (centered `Lqmu.o()` + `Lqmu.i()`) to the shipped 176071840 classes2.dex. Asserts exactly 1 `o` and 1 `i` replaced. |
| `Fix4CombinedPatch` | `Fix4CombinedPatch <input.dex> <output.dex>` | Fix4 centering (superseded — kept for history; it is the build that introduced the v11 merge defect). |
| `DiagCombinedPatch` | `DiagCombinedPatch <input.dex> <output.dex>` | Diag build. Applies `DiagPatch` (`Lqck.D` Toast/Log instrumentation) in one pass over the shipped Fix5 classes2.dex. |
| `RevertOCombinedPatch` | `RevertOCombinedPatch <input838.dex> <stock834.dex> <output.dex>` | Fix3. Reverts `Lqmu.o()` to the EXACT stock implementation from the 176071834 dex. |
| `SimpleAnimatedCombinedPatch` | (see source header) | Simple Animated build (176071835) — the `k()` head + `o()` glide patch. |
| `SimpleRewriteCombinedPatch` | (see source header) | Simple Rewrite build (176071834). |
| `Phase2Fix2CombinedPatch`, `Phase2Fix3CombinedPatch`, `Phase2Fix4CombinedPatch` | (see source headers) | Phase 2Fix2/2Fix3/2Fix4 builds. |

## Patch implementations (the actual method rewrites)

| Tool | What it does |
|---|---|
| `Fix5CenterPatch` | `newO()` (41 insns, 4 fallback branches) — centered `Lqmu.o()` with null/size fallbacks; `centerBlockI()` (24 insns, 48 units, 3 fallback branches to FALLBACK=0x24a) — centered `Lqmu.i()` block using int-only scratch **v19**; `patchO()` asserts the Fix4 `o()` fingerprint; `patchI()` asserts Fix4 `i()` (404 insns / 27 regs / abs 0x21a, no try blocks, no switch payloads), generic branch fixup, expects exactly 5 crossing branches (+10). |
| `Fix4CenterPatch` | Fix4 centering (superseded). Its `i()` block wrote REFERENCE values into **v11** — the proven cause of the Fix4 regression (see `docs/change-report-fix5.md`). Kept so the defect can be reproduced and studied. |
| `DiagPatch` | Instruments `Lqck.D(J)V`: entry marker `Log.e` + Toast `"D-enter <code>"` after insn 1; catch-handler `Log.e` (3-arg, full stack) + Toast `"ERR " + throwable.toString()` after `MOVE_EXCEPTION v0`. No register bump (27 regs), scratch v3/v4/v5, `this` from v24, both insertions even (28 + 52 units), try 9/614/627 → 37/614/655, zero new refs. |
| `DiagHooks` | Shared helpers for the diag instrumentation. |
| `RevertOPatch` | Reconstructs the stock `Lqmu.o()` body instruction-by-instruction; `checkStock()` asserts the reconstruction is identical to the actual 834 implementation (opcode + registers + refs, per instruction) and FAILS the build if not. |
| `CapsSuggPatch` | The v1.17 caps-lock patch (5 hooks in classes.dex + `CapsLockHelper` in classes6.dex; saves caps both directions to pref `nobi_caps` / `caps_lock_on`). |
| `EmojiPopupPatch`, `EmojiPopupCirclePatch`, `EmojiPopupInlinePatch`, `EmojiPopupCenterForcePatch` | Emoji-row / circle-popup resource + patch helpers from the Phase 1–2 arc (PLUS layout, circle background). |
| `PopupTouchGuardPatch`, `PopupTouchGuardPatch3`, `PopupDeltaPatch`, `PopupDirectionPatch`, `PopupTrackerPatch`, `TrackerInitPatch` | Touch-guard / direction-mapping / tracker-init patches from the Phase 2 arc (tracker init to center (1,1), full-cell direction pixels). |
| `SimpleAnimatedPopupPatch`, `SimplePopupPatch` | Simple Animated / Simple Rewrite popup patches. |
| `OvxHPatch` | The `Lovx.h` hook (field-ref lesson: FIELD refs use the real dex type). |

## Version bumps (classes5 version string only)

`DexVersionBumpFix5`, `DexVersionBumpFix5Diag`, `DexVersionBumpFix4`,
`DexVersionBumpFix3`, `DexVersionBumpFix2`, `DexVersionBumpAnimatedFix`,
`DexVersionBumpAnimated`, `DexVersionBump2Fix`, `DexVersionBump2Fix2`,
`DexVersionBump2Fix3`, `DexVersionBump2Fix4`, `DexVersionBumpSimple`,
`DexVersionBump` — each bumps the version string in classes5 (3 use-sites)
for one build. **WARNING: the manifest versionCode lives in
`decoded/apktool.yml`, NOT in classes5 — bump BOTH, and confirm the final
APK with `aapt2 dump badging`.** (Fix5's first signed build came out
176071840 because `apktool.yml` was missed.)

## Verification gates (ALL mandatory on every patched method)

Each gate caught a defect the others missed. None of them runs ART's real
verifier — a `VerifyError` class of defect is only caught by the
combination below + a dump review.

| Tool | Usage | What it checks |
|---|---|---|
| `TypeMergeCheck` | `TypeMergeCheck <dex> <class> <method> [params...]` (+ `--state:<idx>` to dump register types at an instruction) | NEW GATE (Fix5). Forward abstract-interpretation type checker for one method (kinds UNINIT / I32 / I64 / REF / NULL / CONFLICT; NULL merges with anything; `IF_EQZ`/`IF_NEZ` null refinement). Catches register-type merge conflicts (the Fix4 v11 defect: `CONFLICT at join insn idx 350 (abs 27e) reg v11: I64 vs REF`). **Caveat: false positives on stock code with wide-value register reuse** (no wide-pair invalidation model — stock 839 `Lqmu.i` shows 25). Use DIFFERENTIALLY: the patched method's conflict set must equal the stock method's set (same count AND same locations/registers, modulo the insertion shift). An EXTRA conflict vs stock is the signal. **Crashes with `ArrayIndexOutOfBoundsException: Index 27` on methods with a wide (J) parameter** (e.g. `Lqck.D`) — a tool limitation, not a defect; do not rely on it for those methods. Switch payloads NOT supported (loud warning). |
| `RegInitCheck` | `RegInitCheck <dex> <class> <method>` | Must-analysis fixpoint: flags reads of registers not definitely written on all paths. **Must start from TOP (greatest fixpoint), and UNREACHABLE instructions must get TOP (not EMPTY)** — the fixed version in this repo. Validated both ways: working 834 = 0, broken 836 `o()` = 7. Proves initialization, NOT value correctness — a dump review of any remapped method body is still mandatory. |
| `VerifyPatch` | `VerifyPatch <dex> [method-filter-substring ...]` | Invoke register counts vs prototypes (wide args count double) + object-type assignability (a subclass method invoked on a superclass-typed object needs a preceding `CHECK_CAST`; `INSTANCE_OF` does NOT narrow the static type). Catches the 835 `k()` defect (control run on 835 catches exactly the 3 known defects). |
| `VerifyBranches` | (see source header) | Every branch target in the dex is a valid instruction boundary. 0 bad required. |
| `CheckPayloadAlignment` | `CheckPayloadAlignment <dex> [dex...]` | Every switch/array payload sits at an even (4-byte-aligned) code-unit offset. 0 odd required. (V44 crash: an odd insert delta shifted a payload to an odd offset and ART rejected the class at load.) |
| `CanonDiff` | (see source header) | Canonical dex differ — compares instruction opcodes/operands only. **Raw DexDiff is USELESS on re-serialized dex** (`Instruction.toString()` embeds object-identity hashes → tens of thousands of false diffs). Use this, never `DexDiff`. |
| `RefResolve` | (see source header) | Checks that every field/method/type reference used by the given classes resolves to a real declaration across ALL six dex files. 43 pre-existing framework-inherited false positives are identical in working builds — only NEW unresolved refs matter. |
| `RefCheck`, `RefCheck2` | (see source headers) | Earlier ref-resolution checkers (superseded by `RefResolve` for full coverage). |
| `TryBlocks` | (see source header) | Dumps try-block ranges/handlers for a method (used to verify try-block rebuilds after mid-method insertions). |
| `CheckMethod` | (see source header) | Per-method structural checks (registers, insns, try blocks). |
| `CountRefs` | (see source header) | Reference counts (used by CanonDiff/CountRefs comparisons). |

## Dump / exploration helpers

`DumpMethod`, `DumpMethod2`, `DumpFull`, `DumpFull2`, `DumpRaw`, `DumpRegs`,
`DebugReg`, `FindClass`, `FindClasses`, `FindMethodRefs`, `FindFieldRefs`,
`FindResRefs`, `FindCallers`, `FindSubclasses`, `FindBySig`, `ListMethods`,
`ListFields`, `MethodInfo`, `StringSearch` — read-only dex explorers used
to locate classes/methods/refs and dump method bodies. `DumpMethod2` prints
instruction indices (used for the dump proofs in the change reports).
`DexDiff` is kept for reference only — **do not use it** (see `CanonDiff`).

## Tools added after Fix5 (851–861) — the customization arc

| Tool | What it does |
|---|---|
| `FieldCheck` | VerifyPatch-style type tracking PLUS field-access receiver checks (`IGET`/`IGET_OBJECT`/`IPUT`): flags a field read/write when the object register's static type is not assignable to the field's declaring class. Caught the 7 Phase 3 (851) defects. **Must be loaded with ALL dexes of the APK** (a class referenced by the method may live in another dex). |
| `FieldCheckGen` | Same as `FieldCheck` WITHOUT the hardcoded `if (m.getParameters().size() != 7) continue;` filter — the stock `FieldCheck` SILENTLY SKIPS any method without exactly 7 parameters (vacuous pass, proven in the 858 build). **Use this one for non-7-param methods, and always verify a gate actually ran** (it must report the method's insn count). |
| `BranchDump` | Dumps every branch in a method with source idx, address, and target idx/address (used for the Block L' proof in 861: insn 15 `IF_EQZ -> idx 39`, 32 `IF_EQZ -> idx 39`, 40 `IF_NEZ -> idx 66`). |
| `Phase3Tables` | The shared 26-way lookup tables (`LOOKUP_CP` = token[3] code point per key, `LOOKUP_COL` = column class 0=center/1=left/2=right) used by the Phase 3, 857, and def-parse hooks. |
| `Phase3HookPatch`, `Phase3CombinedPatch` | Phase 3 (851) — the `Lqmu.c()` prefs hook (321 insns / 584 units, fingerprint → 26-way chain → prefs read → clone + 5 `List.set` → label update). Superseded by the 852 fix (see `FixPatch`); kept for history and for reproducing the 7 field-access defects. |
| `FixPatch` | Phase 3 Fix (852) — the 7 narrowing insertions ONLY (`INSTANCE_OF` + `IF_EQZ -> GUARD` before each un-narrowed field read). |
| `ReplaceCWithFix11`, `StockRevertCombinedPatch` | 856 — replaces `Lqmu.c()` with Fix11's exact 101-insn version (DexRewriter, one dexlib2 pass). The stock-revert driver is the 845-era version of the same idea (V40 stock `c()`, 72 insns). |
| `CustomizeSafePatch` | 857 — the hook at the LIST LOAD in `Lqml.c()` (insn 25, after `IPUT_OBJECT v4, v0, Lqml->e`): fingerprint → chain → prefs → clone + 5 `List.set` → re-wrap via `Lwhe.o(Collection)` into BOTH v4 and `Lqml->e`. Contains the v13 colId-clobber defect (see `diagnoses/diagnosis-857.md`) — do NOT reuse as-is. |
| `DefParsePatch` | 858 — the def-parse hook in `Lpud.m` (insn 0, 250 insns / 454 units): fingerprint → chain → `ActivityThread.currentApplication()` → prefs → split=5 parts AND every part non-empty → clone arrays → 5 substitutions → clean-up (resets v3/v6/v7/v8/v11 to `CONST_4 0`). |
| `DiagPatch859` | 859 — Toast/Log instrumentation ONLY in `Lpud.m` (312 → 387 insns, no behaviour change): P0 first-call Toast, P1 A-key prefs value, P2 SUB OK, P3 GUARD. Renamed from `DiagPatch.java` (the 837 `Lqck.D` diagnostic) to avoid clobbering it. |
| `CtxFixPatch` | 860 — hooks `Lesb.onCreate()` (`SPUT_OBJECT v1, Lpud->appCtx`) + the Block E Context resolution and Block L / Block W `nobi_diag` writes in `Lpud.m` (387 → 498 insns). |
| `CtxFix2Patch` | 861 — moves Block L OUTSIDE the `diagFirst` gate (write on every call) + hooks `Lmxa.attachBaseContext` (`SPUT_OBJECT v1, Lpud->appCtx`). |
| `MergeEditor860` | 860 — rebuilds the editor (JDK 17 default) and merges `editor.dex` into classes6.dex, preserving `CapsLockHelper`. |
| `RatioPatch` | 853 — keyboard height ratio default `"1.0"` → `"1.2"` in `Lpfd.<clinit>` (ONE instruction) + the six `config_keyboard_height_ratio_*` restriction defaults. |
| `Recon`, `Recon2`–`Recon5`, `Sig` | Phase 3 recon tools (method/field/ref searches used to locate the hook points and the fingerprint). |
| `Fp` | 856 — fingerprint helper (asserts Fix11 `c()` = 101 insns / 199 units and 855 `c()` = 436 insns / 811 units before replacing). |
| `MethodCallers`, `RefExists`, `RefBatch` | Ref-graph helpers (who calls a method; whether a ref exists; batch RefResolve runs). |
| `TryCheck` | Dumps try-block ranges/handlers for a method (used to verify try blocks after mid-method insertions). |
| `DumpBranches`, `DumpOps` | Dump helpers (branch targets; instruction opcodes). |
| `DexVersionBumpFix6`, `DexVersionBumpFix11` | Version-string bumps for builds 846 (Fix6) and 849 (Fix11). |
| `VerBump`, `VerBump853`, `VerBump855`, `VerBump856`, `VerBump857`, `VerBump858`, `VerBump859`, `VerBump860`, `VerBump861` | Version-string bumps for builds 852–861 (one tool per build; each bumps the version string in classes5, 3 use-sites). |

## Notes

- The tools accumulated over the whole arc (Phase 1 → 861). Older
  patch tools are kept for history and for reproducing earlier builds;
  only the 858/860/861 def-parse tools and the gates are current.
- `ListMethods.java.tmp` was a scratch file and is NOT included.
- No tool in this repo touches the signing key, reads a password, or
  performs any network access. Signing is a separate manual step (see
  `README.md`).
