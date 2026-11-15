# NobiBoard 176071857 — Long-Press Popup Diagnosis

**Status: ONE DEFECT PROVEN (value-flow bug), but it does NOT explain a missing popup.
No static defect is provable that explains "popup never appears for ALL keys."**
**Date:** 2026-11-14 IST
**Scope:** Diagnosis only. No build, no upload, no message sent.

## TL;DR

The `Lqml.c()` hook (857) contains **one proven defect: insn 214 `MOVE_RESULT v13`**
— the clone loop stores `List.add()`'s boolean result (always `1`) into **v13, the
colId register**, so after the loop v13 is ALWAYS `1` (LEFT) regardless of the real
column class. Every customised key gets LEFT-column slot mapping, so the custom
emojis land in the WRONG slots. **This is a real bug that must be fixed.**

**But it does NOT explain the reported symptom.** The substituted list is still 9
valid `Lpva` items, the `Lqmu.c()` guard passes (or, at worst, skips tracker
centering and still calls `p()`), so the popup **should still appear** — with the
wrong emojis in the wrong slots. I verified the guard's code-point threshold
(`CONST_HIGH16` = 65536, supplementary-plane) and that a `U+200B` in item[3] only
skips centering, never suppresses the popup.

**Everything else the gates check was re-verified by hand and is clean** (branch
retargeting, register liveness, field/invoke receiver types, fingerprint/chain,
`Lwhe.o` rewrap). The GUARD path (uncustomised keys) is a **proven no-op**: it does
not modify the list, does not clobber any live register, and cannot throw
(traced instruction by instruction). Therefore, for uncustomised keys, the popup
MUST appear. Sam reports it does not appear for ANY key.

**The honest conclusion:** either Sam's test involved only CUSTOMISED keys (so only
the SUCCESS path ran) and the SUCCESS path fails for a reason that needs device
evidence (ranked candidates in §4), or there is a defect class the gates do not
check that I have not identified. §5 designs the minimal Toast/Log instrumentation
that settles it. **Do NOT build another blind patch.**

---

## 1. The proven defect — v13 colId clobber (insn 214)

Method: **`Lqml.c()`** (33 regs, 898 insns in 857), hook block insns 26–321.
File: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-customize-safe/verify/diag857/full-857.txt`

### 1a. The defect — exact location

The clone loop (insns 206–216):

```
206: [0184] NEW_INSTANCE v5 Ljava/util/ArrayList;
207: [0186] INVOKE_DIRECT {v5} Ljava/util/ArrayList;-><init>()V
208: [0189] CONST_4 v8, #0
209: [018a] CONST_16 v11, #9
210: [018c] IF_GE v8, v11  // -> insn 217 (CL_DONE)
211: [018e] INVOKE_INTERFACE {v4,v8} Ljava/util/List;->get(I)Ljava/lang/Object;
212: [0191] MOVE_RESULT_OBJECT v10
213: [0192] INVOKE_INTERFACE {v5,v10} Ljava/util/List;->add(Ljava/lang/Object;)Z
214: [0195] MOVE_RESULT v13          <-- VIOLATION: add()'s boolean -> v13 (colId)
215: [0196] ADD_INT_LIT8 v8, v8, #1
216: [0198] GOTO  // -> insn 210
```

- `v13` is the **colId** register (set by the 26-way chain: 0=center, 1=left, 2=right;
  see patcher `R_COL = 13`).
- `List.add()` returns boolean `true` (`1`) for every `ArrayList.add()` call.
- `MOVE_RESULT v13` stores that `1` into v13 on **every** loop iteration.
- After 9 iterations, **v13 = 1 always**, regardless of the chain's colId.

The setSlot blocks then read v13 as the colId (e.g. insn 219 `IF_NE v13, v8` for
the down slot, insn 257/259/281/300 for the others), so **every customised key is
treated as a LEFT-edge key** and gets LEFT-column slots `{1,4,5,3,7}` instead of
its real column's slots (centre `{0,3,4,5,6}`, right `{1,4,3,5,7}`). Concretely,
for a CENTRE key the custom CENTRE emoji lands in slot 4, the custom LEFT emoji
lands in slot 3 (the popup's centre), etc. The popup shows the wrong emoji in
every slot.

### 1b. Why it does NOT kill the popup

The substituted list is still 9 valid `Lpva` items (the clone has 9 items; the 5
`List.set` calls use in-range slot indices 0–7 on a 9-item list). `Lqmu.c()`'s
guard (Fix11's, insns 70–98 in `lqmu-c-857.txt`) requires only: list non-null,
size == 9, item[3] is `Lpva` with a non-empty `CharSequence` `e`, and
`codePointAt(e,0) >= 65536` (the `CONST_HIGH16` at insn 90 is **1**, i.e.
`0x00010000`; confirmed by re-dump). **Every guard failure jumps to insn 99
(`p()`) — the popup is still built and shown; only the tracker centering is
skipped.** So even if the wrong emoji (or a `U+200B`) lands in item[3], the
popup appears. The v13 clobber explains WRONG EMOJI PLACEMENT, not a missing
popup.

### 1c. Why the gates missed it (the gate gap)

This is a **value-flow bug**, not a type/branch/init defect, and **no gate checks
register VALUES or semantic correctness**:
- **RegInitCheck** only checks a register is *initialised* — v13 IS initialised
  (to 0, then to the chain's colId, then to 1). Pass.
- **TypeMergeCheck** only tracks *kind* (REF/INT/I64) — the boolean `1` and the
  colId `0/1/2` are all INT. Pass (34 = 34).
- **VerifyPatch** checks invoke receivers/arg counts, not `MOVE_RESULT`
  destinations. Pass.
- **FieldCheck** checks field-access receiver types. Pass.
- **VerifyBranches / RefResolve / CanonDiff** do not look at values at all.
**Lesson:** a `MOVE_RESULT` (or any result write) into a register that already
holds a live semantic value is a defect class the gate set cannot see. A future
gate (or a dump-review checklist item) must verify that **every `MOVE_RESULT` /
`MOVE_RESULT_OBJECT` destination is a dead scratch register, never a register
holding live hook state** (here: colId). The patcher author's comment
("`MOVE_RESULT v13` // boolean, colId dead now") was WRONG — colId is read by
every setSlot block after the loop.

---

## 2. Ruled out (with evidence)

All re-verified independently on the shipped 857 dex (not trusting the build
agent's runs). Full dumps: `verify/diag857/full-857.txt` (857, 898 insns),
`verify/diag857/full-856.txt` (856, 602 insns); liveness script output in §8.

| # | Candidate | Verdict | Evidence |
|---|---|---|---|
| 1 | Branch retargeting (stock branches shifted +296) | **CLEARED** | Prefix insns 0–25 identical (0 diffs). Shifted stock (856 insns 26–601 → 857 insns 322–897): 576 insns, **0 opcode/core diffs, 0 branch-target mismatches**. No stock branch targets insn 26 (the `t == INS` boundary case in the patcher's `t > INS` condition does not trigger). 857 insn 322 = 856 insn 26 (`IGET_OBJECT v5, v0, Lqml->f`). GUARD (insn 322) is targeted by exactly the 15 hook fail-skips. |
| 2 | Register liveness (hook clobbers a live stock value) | **CLEARED** | Backward fixpoint liveness on 856 stock code: live-in at insn 26 = `{0,1,2,3,4,6,7,18,20,21,22,23,27,28,32}`. Hook clobbers v5, v8–v15 — **NONE live**. Each is first WRITTEN by the stock code after insn 26 before any read (v5@26, v8@27, v9@29, v10@35, v11@36, v12@37, v13@40, v14@51, v15@64). Live doubles v20/v21, v22/v23 (and v18) are never touched by the hook. |
| 3 | Field-access receiver types (Phase 3 defect class) | **CLEARED** | FieldCheck (all-dex load, re-run): `Lqml.c` 898 insns, 126 accesses, **0 violations**; `Lqmu.c` 101 insns, 13 accesses, **0 violations**. Control: Phase 3 (851) `Lqmu.c` gives 7 violations with the same tool, so the gate works. Every hook field read from a `List.get()` result is preceded by `INSTANCE_OF` + `IF_EQZ → GUARD` (fingerprint idx 48–50, setSlot idx 228–230 / 247–249 / 271–273 / 290–292 / 309–311). |
| 4 | Invoke receiver types / arg counts | **CLEARED** | VerifyPatch (re-run): `Lqml` 15 methods, 238 invokes, **0 violations**. All 12 hook invokes hand-verified (receivers, arg counts incl. wide args): `List.size/get/add/set` on Lwhe/ArrayList, `CharSequence.length`, `Character.codePointAt`, `Context.getSharedPreferences`, `SharedPreferences.getString`, `String.split`, `ArrayList.<init>`, `Lwhe.o`, `Lpva.<init>(I,Lpuz,Object,I)` (5 regs: this + 4 params, correct). |
| 5 | Register initialisation | **CLEARED (pre-existing)** | RegInitCheck (re-run): 857 `Lqml.c` = **1 violation** (insn 668 reads v20, `SUB_DOUBLE`); control 856 `Lqml.c` = **1 violation** (insn 372, same site, shifted +296). Identical — the hook adds none; a RegInitCheck false positive on stock code, not a regression. |
| 6 | Type merges | **CLEARED** | TypeMergeCheck (re-run, set comparison): **34 = 34**, identical profile by location after the +296-insn shift (the `tmc-patched.txt` file with 38 is the FIRST patcher version, superseded by `tmc-patched2.txt` = 34). Zero new conflicts. |
| 7 | Unresolved refs | **CLEARED** | RefResolve (filtered, re-run): **unresolved = 17, identical set to 856** (the 3 `Lqml.c` lines differ only by the +296 insn shift: 33→329, 61→357, 179→475). No new unresolved refs. `Lqml` class structure identical to 856 (superclass, interfaces, fields, all other methods) — only `c()` insn count differs (602→898). |
| 8 | Fingerprint / 26-way chain | **CLEARED** | All 26 `LOOKUP_CP` values match fd2.xml token[3] exactly (0 mismatches, upper + lower). All 52 fd2 entries have exactly 9 tokens, all `U+XXXX`, **0 empty tokens**. Column classes correct (A/Q/Z=1, L/M/P=2, rest 0). |
| 9 | `Lwhe.o` rewrap throwing (NPE) | **CLEARED for A–Z** | Decompiled `Lwhe.o` (classes.dex): for a non-`Lwgu` input (our ArrayList) it does `Collection.toArray()` → `Lwhe.j(Object[])` → `Lvmg.R(array,len)` → `Lwhe.i(array,len)` → `new Lwmm(array,len)`. `Lvmg.R` loops and calls `Lvmg.Q(Object,int)`, which **throws `NullPointerException("at index N")` iff an element is null** (dumped, `Lvmg;->Q`). fd2 lists have **no nulls** (see #8), so no throw for A–Z keys. `Lpva.<init>` dumped: safe (handles null `Lpuz`, normalises `CharSequence`→`String`, no throw for our args). The stock code never calls `Lwhe.o`, so this validation is new — but it cannot fire on A–Z lists. |
| 10 | `Lqmu.c()` guard suppressing the popup | **CLEARED** | Guard (insns 70–98) is fully defensive: every failure (`null` list, size≠9, item[3] not `Lpva`, `e` not `CharSequence`, length 0, code point < 65536) jumps to insn 99 (`p()`), and the popup is still built/shown. A `U+200B` in item[3] only skips tracker centering. The popup cannot be suppressed by the guard. |
| 11 | CHECK_CAST in the hook | **CLEARED** | **Zero** `CHECK_CAST` instructions in the hook block (insns 26–321). The 15 in `Lqml.c()` are all stock code. No `ClassCastException` from the hook. |
| 12 | GUARD path throwing (uncustomised keys) | **CLEARED** | Traced instruction by instruction (insns 26–199 for the no-custom-pref path): every potentially-throwing operation is guarded — `List.size/get` on a non-null v4 (null → GUARD at insn 40), size==9 before `get(3)`, `INSTANCE_OF`+`IF_EQZ` before every field read, `length()>0` before `codePointAt`, `Lqmo->b` read on a non-null `Lqml->f` (the stock code at insn 322 reads the same field and uses it, so it is non-null when `c()` runs), `getSharedPreferences`/`getString` null-guarded after each call. **No throw is reachable on the GUARD path.** The hook is a pure no-op there (list and all live registers untouched). |

---

## 3. What the symptom requires (and why it is a paradox)

For the popup to **never appear at all**, `Lqmu.c()` must fail before `p()` runs.
`Lqmu.c()` (Fix11's, 101 insns) is:
- insn 0: `INVOKE_SUPER_RANGE {v6..v13} Lqml;->c(...)` — the hook runs here, FIRST.
- insns 1–69: tracker Point setup + registry loop (stock, works in 856).
- insns 70–98: guard (defensive — see #10 above; cannot suppress the popup).
- insn 99: `p()` (at-open placement). insn 100: return.

So the popup fails to appear **only if** `Lqml.c()` throws (contained by
`Lqck.D`'s catch-all → silent no-popup) **or** the `Lqml` class fails
verification at load (whole class erroneous → `Lqmu` cannot link either, since
`Lqmu` extends `Lqml`).

But §2 proves the GUARD path (uncustomised keys) cannot throw and cannot corrupt
state, and the gates prove no verification defect in the classes they check.
Hence the paradox: **for uncustomised keys the popup MUST appear, yet Sam reports
it appears for NO key.** The most likely resolution is that **Sam's test involved
only customised keys** (he had customised keys in 851–856, and the 857 test's
first step was long-pressing Y — if Y was customised, only the SUCCESS path ran).
The SUCCESS path is the only place the hook does real work, so its failure
candidates (§4) are where the missing-popup cause lives — if it is in the dex at
all.

---

## 4. Ranked candidates for the missing-popup cause

### Candidate 1 (most likely, IF Sam tested a customised key) — `codePointAt` on an empty custom-string part → `StringIndexOutOfBoundsException`
- **Location:** hook insns 224 / 243 / 267 / 286 / 305
  (`INVOKE_STATIC {v12,v8} Character.codePointAt(CharSequence,I)`), one per setSlot.
- **Mechanism:** the hook checks `split(",")` gives exactly 5 parts (insn 205)
  but does **NOT** check that each part is non-empty. If a custom pref string has
  an empty middle part (e.g. `"🚀,,,,"` → parts `["🚀","","","",""]`), then
  `AGET_OBJECT v12, v9, v8` loads `""` and `codePointAt("", 0)` throws
  `StringIndexOutOfBoundsException` (index 0, length 0). The exception propagates
  through `Lqmu.c()` → `Lqck.D` → contained → **no popup**.
- **Evidence for:** the missing guard is real and visible in the dump (no
  length check on the parts before `codePointAt`; contrast the fingerprint,
  which DOES check `length()>0` before `codePointAt` at insn 55). `String.split`
  DOES produce empty strings for consecutive commas (unlike trailing empties,
  which it drops).
- **Evidence against / unproven:** I cannot read Sam's actual saved prefs
  (they are on his phone). The current editor stores `U+200B` for cleared slots,
  not `""`, so a well-formed editor-written pref has no empty parts. This only
  fires if Sam's prefs contain an empty part (e.g. written by an earlier editor
  version, a Reset path, or manual editing).
- **Distinguishing test:** the instrumentation in §5 (log each part's length
  before `codePointAt`).

### Candidate 2 — `Lwhe.o` NPE via `Lvmg.Q` (a null in the original list)
- **Location:** hook insn 317 (`INVOKE_STATIC {v5} Lwhe;->o(Collection)Lwhe;`)
  → `Lwhe.j` → `Lvmg.R` → `Lvmg.Q` (throws `NullPointerException("at index N")`
  iff an element is null; dumped and confirmed).
- **Mechanism:** if the ORIGINAL 9-item list contains a NULL element, the clone
  copies it, `Collection.toArray()` puts it in the array, and `Lwhe.j` throws
  NPE. The fingerprint only checks item[3]; it never checks the other 8 items.
  This fires ONLY on the SUCCESS path (custom pref exists), because `Lwhe.o` is
  only called after successful substitution.
- **Evidence for:** `Lvmg.Q`'s NPE is PROVEN (dumped); the hook is the ONLY caller
  of `Lwhe.o` in the popup path (stock never calls it); the fingerprint's
  single-item check leaves the other 8 items unchecked.
- **Evidence against:** fd2.xml (A–Z) has **0 empty tokens** and all 52 entries
  are 9 valid `U+XXXX` tokens — so A–Z lists have no nulls. This can only fire
  for a NON-fd2 list (a number/symbol key, or a key whose ActionDef is built from
  a different source) that has 9 items, item[3] an `Lpva`, and another item null.
  Sam's test was Y (a letter, fd2) — so this is unlikely for Y, unless Y's list
  is not the fd2 list on his device (e.g. a different keyboard layout/variant).
- **Distinguishing test:** the instrumentation in §5 (log whether any clone item
  is null before calling `Lwhe.o`).

### Candidate 3 — a VerifyError class the gates do not check
- **Mechanism:** the gates (FieldCheck, VerifyPatch, TypeMergeCheck, RegInitCheck,
  VerifyBranches, RefResolve) do not model ART's verifier exactly. A residual
  possibility is a verification rule about the hook's specific structure (e.g. a
  merge-type issue at GUARD involving the SPECIFIC class — not just REF kind — of
  a register, or an `INSTANCE_OF` refinement interaction) that none of the gates
  sees. I searched for a specific instance and found none (all merges at GUARD
  are kind-consistent and the stock code rewrites the hook's registers before
  reading them; all hook-internal merges are kind-consistent).
- **Evidence for:** Fix5 and Fix10 in this same arc also had "no defect provable
  in the dex" yet failed on-device — so this arc has precedent for a
  statically-invisible failure mode.
- **Evidence against:** every verifier rule I can name and check by hand
  (field receivers, invoke receivers, initialisation, branches, payload
  alignment, ref resolution, kind merges) passes; the `Lqml` class structure is
  byte-identical to 856 except `c()`.
- **Distinguishing test:** the instrumentation in §5 (an entry Toast at the very
  start of `Lqml.c()` — if it never appears, the class failed verification; if
  it appears, the class loaded fine and the failure is a runtime throw).

### Candidate 4 (least likely) — install / process state
- 856 works on the same install and 857 is a clean install-over with the same
  key, so a stale install is unlikely. A force-stop + clean reinstall of 857
  would rule it out. Do this before any further patching.

---

## 5. What device evidence would settle it (minimal instrumentation)

Do NOT build another blind patch. Build **one** Toast/Log-diagnosed variant of 857
(code 176071858, same hook + instrumentation ONLY, no behaviour change) and have
Sam long-press Y (uncustomised if possible) and then A (customised if he set one):

| Toast / Log (tag `NobiDiag`) | Meaning if it appears | Meaning if it does NOT appear |
|---|---|---|
| `Lqml.c-enter` (first insn of `Lqml.c()`, before the hook) | The `Lqml` class loaded and verified fine → **Candidate 3 (VerifyError) is REFUTED**; the failure is a runtime throw inside the hook or later. | The class failed verification at load → **Candidate 3 CONFIRMED**; the defect is a VerifyError the gates missed. Stop and re-audit the hook against ART's verifier rules (not the gates). |
| `hook-GUARD` (at GUARD, insn 322, i.e. the hook fell through to stock) | The hook ran and fell through to the stock code → the hook did not throw on this key; if the popup STILL does not appear, the failure is downstream of `Lqml.c()` (in `Lqmu.c()`'s registry/guard/`p()`, or in `Lqck.D`'s caller) — re-diagnose there. | The hook did not reach GUARD → it threw (or branched wrongly) inside the hook. |
| `hook-OK parts=5` + per-part lengths (right before the first setSlot's `codePointAt`, i.e. after the split) | If a part length is `0`, **Candidate 1 is CONFIRMED** — the custom string has an empty part and `codePointAt` throws. | If all parts are non-empty, Candidate 1 is refuted for this key. |
| `clone-null i=<idx>` (in the clone loop, if `List.get(i)` returns null) | **Candidate 2 is CONFIRMED** — the original list has a null item and `Lwhe.o` will throw NPE at `Lvmg.Q`. | No null in the clone → Candidate 2 refuted for this key. |
| `Lqml.c-exit` (last insn of `Lqml.c()`, i.e. the method returned normally) | `Lqml.c()` completed → the hook and the stock tail both ran; the failure is in `Lqmu.c()` after `super.c()` returns, or in the caller. | `Lqml.c()` threw before returning (and no earlier Toast fired) → the throw is in the stock tail after the hook (not the hook itself). |

**The decisive discriminator is the first two Toasts.** If `Lqml.c-enter` never
appears on ANY long-press, the class never loads (VerifyError). If it appears
but `hook-GUARD` does not, the hook throws — and the per-part / clone-null
Toasts say which candidate. If BOTH appear and the popup still does not show,
the defect is NOT in `Lqml.c()` at all — it is downstream, and the next
instrumentation belongs in `Lqmu.c()` / `Lqck.D`, not in another `Lqml` patch.

**Also ask Sam (one question, bundled with the diag build):** which key did he
long-press when the popup did not appear — and had he customised that key in the
editor? If it was an UNcustomised key, Candidates 1–2 are refuted outright (the
GUARD path cannot throw) and only Candidate 3 (VerifyError) or 4 (install state)
remains. If it was a CUSTOMISED key, Candidates 1–2 are live.

---

## 6. What the fix would need to be (design only, no code)

1. **Fix the proven v13 clobber (mandatory, regardless of the popup cause):**
   move the clone loop's `List.add()` result into a DEAD scratch register (e.g.
   `v11`, which already holds the loop bound but is dead after the loop — or a
   dedicated temp), never into v13. Preserve v13 (colId) across the clone loop.
   Verify by dump: after the loop, v13 must equal the chain's colId (0/1/2) on
   every path. Add a gate/checklist item: **every `MOVE_RESULT` /
   `MOVE_RESULT_OBJECT` destination must be a dead scratch register; never a
   register holding live hook state.**
2. **Guard Candidate 1:** after `split(",")` gives 5 parts, check each part's
   `length() > 0` (or `codePointAt` only if non-empty) — any empty part → GUARD
   (leave the list as loaded; popup shows defaults). This makes the hook robust
   to malformed prefs.
3. **Guard Candidate 2:** in the clone loop, if `List.get(i)` returns null → GUARD
   immediately (do not clone a null; do not call `Lwhe.o`). This makes the hook
   robust to a null in the original list.
4. **Do NOT re-insert the Phase 3 hook into `Lqmu.c()`.** Do NOT change
   `Lqmu.c()` (byte-identical to Fix11's). Any fix must stay in the `Lqml.c()`
   list-load hook (or in a place that is proven safe by the diag build's Toasts).
5. **Do NOT build the fix until the diag build's Toasts say which candidate
   fired.** If `Lqml.c-enter` never appears (VerifyError), the fix is a
   verifier-level re-audit of the hook, not a value guard. If the Toasts show
   the hook reaches GUARD and returns, the fix is NOT in `Lqml.c()` at all.

---

## 7. Gate gap (for the gate set)

The v13 clobber is invisible to the entire gate set because **no gate checks
register VALUES or semantic correctness** — only types, initialisation, branches,
alignment, and ref resolution. A `MOVE_RESULT` into a live register is a
value-flow bug. Add to the dump-review checklist (mandatory on every future dex
patch): *"For every `MOVE_RESULT` / `MOVE_RESULT_OBJECT`, name the register it
writes and prove that register holds no live value at that point (trace its
last write and next read)."* The patcher author's inline comment ("colId dead
now") was the exact wrong claim — the comment asserted deadness that the code
contradicted.

---

## 8. Files & commands (reproducibility)

- Shipped 857 dex: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-customize-safe/outputs/NobiBoard-CustomizeSafe-176071857.apk` (classes2 md5 `dce6bec4…`); 856 control: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-fix11-popup/outputs/NobiBoard-Fix11Popup-176071856.apk` (classes2 md5 `87aeab3d…` = Fix11's).
- Scratch: `verify/diag857/` (dex856/dex857 extracted dexes; `full-857.txt` 898 insns, `full-856.txt` 602 insns, `lqmu-c-857.txt` 101 insns; `FullDump.java` — prints registers, refs, and branch targets as insn indices; `ClassInfo.java`).
- Key dumps: `Lwhe.o` / `Lwhe.j` / `Lwhe.h` / `Lwhe.i` (classes.dex), `Lvmg.R` / `Lvmg.Q` (classes.dex), `Lpva.<init>` (classes.dex), `Lqmu.c` guard (insns 70–98, `CONST_HIGH16` = 65536).
- Independent re-runs on the shipped 857 dex: FieldCheck `Lqml.c` = 0 (126 accesses), `Lqmu.c` = 0; VerifyPatch `Lqml` = 0 (238 invokes); RegInitCheck `Lqml.c` = 1 (insn 668, pre-existing stock v20; control 856 = 1 at insn 372); VerifyBranches classes2 = 0 bad (121,028); TypeMergeCheck 34 = 34 (set-identical after +296 shift; `tmc-patched.txt` = 38 is the superseded FIRST patcher version); RefResolve 17 = identical set (insn indices shifted +296).
- Liveness: backward fixpoint over the 856 `Lqml.c()` CFG (from `full-856.txt`); live-in at insn 26 = `{0,1,2,3,4,6,7,18,20,21,22,23,27,28,32}`; hook regs v5, v8–v15 all dead at insn 26.
- Fingerprint check: `Phase3Tables.LOOKUP_CP/COL` vs re-decoded `verify/redecode/res/xml/APKTOOL_DUMMYVAL_0x7f170fd2.xml` — 26/26 match (upper + lower), 52 entries, 9 tokens each, 0 empty tokens.
