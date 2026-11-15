# NobiBoard Phase 3 (176071851) — Long-Press Popup Diagnosis

**Status: DEFECT PROVEN (machine-checked differential, not a hypothesis)**
**Date:** 2026-11-14 IST
**Scope:** Diagnosis only. No build, no upload, no message sent.

## TL;DR

The long-press popup never appears on Phase 3 because **`Lqmu.c()` fails ART's
class verification** and the whole `Lqmu` class becomes unusable for the
session. The cause is **7 field reads from `Object`-typed registers with no
narrowing** — the hook reads `Lpva->d` (5×) and `Lqmc->b` / `Lqmc->d` (2×)
from `List.get()` results whose declared return type is `java.lang.Object`,
with **no `instance-of` and no `check-cast` in between**. ART's verifier
rejects an `iget`/`iget-object` whose object register is not assignable to the
field's declaring class — the same rule Fix2 already proved for invoke
receivers (`Lqmu->n()` on an `Lqml`-typed `this` → VerifyError). The
VerifyError is contained by `Lqck.D`'s catch-all → silent no-popup, no crash.
The editor (a separate Activity in classes6.dex) is untouched, which is why
Sam can customize but the popup never appears.

* **Working Fix11 `Lqmu.c`:** 13 field accesses, **0 violations**
* **Phase 3 `Lqmu.c`:** 34 field accesses, **7 violations** — all in the hook
* The failure is a **class-load VerifyError**, so the popup fails on the very
  first long-press — **even before any customizing** (before/after-prefs is
  moot: the class never loads). If Sam's on-device test shows the popup worked
  *before* he customized and only broke *after*, this hypothesis is REFUTED
  (see §7 for the distinguishing test).

---

## 1. The defect — exact locations

Method: **`Lqmu.c()`** (the popup-builder, 7 params, 14 regs), shipped Phase 3
classes2.dex, `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-phase3/verify/classes2.dex`.

### 1a. Five `Lpva->d` reads (setSlot ×5)

Each `setSlot` block does:

```
294: INVOKE_INTERFACE {v1,v8,...} Ljava/util/List;->get(I)Ljava/lang/Object;
295: MOVE_RESULT_OBJECT v7            ; v7 is now Object-typed (declared return type)
296: IGET_OBJECT v7, v7, Lpva;->d:Lpuz;      <-- VIOLATION
297: NEW_INSTANCE v2, Lpva;
```

Indices: **296, 309, 322, 335, 348** (one per slot: down/center/right/left/up).
`v7` is `Object`-typed (the declared return type of `List.get`) and is never
narrowed before the field read. `Lpva` is a dex class (defined in
**classes.dex**), `Object` is not assignable to it.

### 1b. Two `Lqmc` reads (label walk)

```
364: INVOKE_INTERFACE {v1,v5,...} Ljava/util/List;->get(I)Ljava/lang/Object;
365: MOVE_RESULT_OBJECT v2            ; v2 is Object-typed
366: IF_EQZ                            ; null check only — does NOT narrow the type
367: IGET v3, v2, Lqmc;->b:I           <-- VIOLATION (b = data index, int field)
...
384: IGET_OBJECT v2, v2, Lqmc;->d:Landroid/view/View;   <-- VIOLATION (d = label View)
```

`Lqmc` is defined in classes2.dex. `Lqmc.b` = the data index (int),
`Lqmc.d` = the label `View` (confirmed from `smali/qmc.smali`:
`.field public final b:I`, `.field public final d:Landroid/view/View;`).
The only guard between the `List.get` and the field reads is a null check —
a null check does not change the static type.

### 1c. The tell: the narrowing convention is used everywhere EXCEPT these 7 sites

The stock guard and the hook's own fingerprint both narrow before their
`Lpva->e` reads — the exact same pattern (`List.get` → `instance-of` →
`IF_EQZ` → field read):

```
# hook fingerprint (idx 94–98) — CORRECT narrowing
94:  INVOKE_INTERFACE {v1,v0,...} List.get(I) Object
95:  MOVE_RESULT_OBJECT v2
96:  INSTANCE_OF v0, v2, Lpva;
97:  IF_EQZ
98:  IGET_OBJECT v2, v2, Lpva;->e:Ljava/lang/Object;

# stock guard (Phase 3 idx 398–402) — CORRECT narrowing
398: INVOKE_INTERFACE {v0,v1,...} List.get(I) Object
399: MOVE_RESULT_OBJECT v1
400: INSTANCE_OF v2, v1, Lpva;
401: IF_EQZ
402: IGET_OBJECT v2, v1, Lpva;->e:Ljava/lang/Object;
```

And the stock registry loop narrows with `CHECK_CAST v13, ViewGroup` (Fix11
idx 29) before using a `getChildAt` result. So the build agent clearly knew
the narrowing convention (it used it in the fingerprint and the stock guard
was preserved) and simply omitted it at the 7 hook sites. The hook contains
exactly ONE `check-cast` in total (idx 387, `TextView`, for `setText`) — and
it comes *after* the two `Lqmc` field reads, so it protects nothing.

---

## 2. Failure mechanism (why the popup never appears for ANY key)

1. First long-press → the popup controller class `Lqmu` is loaded.
2. ART verifies `Lqmu` — **all** its methods, including `c()`.
3. `c()` fails verification at the 7 sites (§1): an `iget`/`iget-object` on a
   register whose static type (`Object`) is not assignable to the field's
   declaring class (`Lpva` / `Lqmc`).
4. The **whole `Lqmu` class is marked erroneous** — no method of it can ever
   run, including the stock tail, `o()`, `i()`, `p()`, `n()`.
5. The `VerifyError` propagates up through `c()` → `Lqck.D` (catch-all,
   handler type=null, rethrows into the Runnable dispatch) → **contained
   silently → no popup, no crash**.
6. The editor (`NobiEmojiEditorActivity`, classes6.dex) is a different class
   in a different dex — unaffected. Sam can customize; the popup never
   appears. **Exactly the reported symptom.**

This is the 4th occurrence of this failure mode (Fix4, Fix5, Fix10, Phase 3),
but the **first one whose cause is a field-access receiver type** — the
earlier ones were invoke-receiver / register-merge defects.

---

## 3. Proof — the new gate: FieldCheck (differential)

The existing gates cannot see this defect class (see §4), so I wrote a
focused checker modelled on VerifyPatch's type tracking (params, moves, field
loads, new-instance, check-cast, invoke return types, `AGET_OBJECT`
component types), that adds **field-access receiver checks** (`IGET` /
`IGET_OBJECT` / `IPUT` / `IPUT_OBJECT` + primitives): it flags an access when
the object register's known static type is not assignable to the field's
declaring class. It also models ART's `instance-of` refinement (an
`INSTANCE_OF` immediately followed by `IF_EQZ` narrows the checked register
on the fall-through path) — the same refinement the stock guard relies on.

Tool: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-phase3/verify/diag/FieldCheck.java`
(compiled to `verify/gate-classes/`). A key detail: it loads **ALL dexes** in
a directory for the class hierarchy — `Lpva` lives in **classes.dex**, not
classes2.dex, so a single-dex load silently skips the 5 `Lpva->d` sites
(this is exactly the blind spot that hid them).

```
java FieldCheck <dex-dir> <class> <method>
```

Result (all-dex load):

```
Fix11  Lqmu;->c (101 insns): fieldAccesses=13 violations=0
Phase3 Lqmu;->c (422 insns): fieldAccesses=34 violations=7
  FIELD VIOLATION idx 296 IGET_OBJECT v7 Object -> Lpva;->d:Lpuz;
  FIELD VIOLATION idx 309 IGET_OBJECT v7 Object -> Lpva;->d:Lpuz;
  FIELD VIOLATION idx 322 IGET_OBJECT v7 Object -> Lpva;->d:Lpuz;
  FIELD VIOLATION idx 335 IGET_OBJECT v7 Object -> Lpva;->d:Lpuz;
  FIELD VIOLATION idx 348 IGET_OBJECT v7 Object -> Lpva;->d:Lpuz;
  FIELD VIOLATION idx 367 IGET v2 Object -> Lqmc;->b:I
  FIELD VIOLATION idx 384 IGET_OBJECT v2 Object -> Lqmc;->d:Landroid/view/View;
```

The clean Fix11 result is the control: the checker correctly passes the stock
guard (its `instance-of` refinement narrows `v1`/`v2` to `Lpva` before the
`Lpva->e` reads), so the 7 Phase 3 violations are not checker noise — they
are the only field reads in the method that lack narrowing.

---

## 4. Why every gate passed (the blind spot, now named)

| Gate | Result on shipped Phase 3 | Why it missed the defect |
|---|---|---|
| **RegInitCheck** | 0 (re-run independently, 422 insns) | Checks register *initialization* only, never types. |
| **VerifyPatch** | 0 (12 methods, 189 invokes, re-run) | Checks **INVOKE receivers only** — it never looks at field accesses. It also loads **one dex at a time**, so cross-dex types (`Lpva` in classes.dex) can't be resolved, and it skips `Object`-typed receivers as "cannot prove". |
| **VerifyBranches** | 0 bad (121,033 branches, re-run) | Checks branch *targets* only. All 99 hook branches are correct (see §5). |
| **TypeMergeCheck** | 4 = Fix11 profile (location-level) | Tracks only **REF vs INT kind**, not the specific class — `Object` and `Lpva` are both REF. |
| **CheckPayloadAlignment** | 0 odd | Checks payload alignment only. |
| **RefResolve** | No new real unresolved refs | Checks that refs *resolve* (`Lpva->d` exists). It does not check receiver types. |
| **CanonDiff** | classes2 = 1 method only (`Lqmu.c`) | Confirms the change is confined to `c()` — it does not check the new code's type discipline. |

**Lesson for the gate set:** field-access receiver types are a whole defect
class the gates never checked. `FieldCheck` (all-dex load) closes it. Any
future dex patch that reads a field from a `List.get` / `Map.get` / interface
result **must** narrow with `instance-of` + `IF_EQZ` (or `check-cast`)
before the field read — exactly as the stock guard and the fingerprint do.

---

## 5. Ruled out (with evidence)

1. **Control flow / branch targets — CLEARED.** Full 422-insn dump traced
   (idx 70–390); `BranchDump` on the shipped dex: **all 99 branches verified
   correct** against the patcher design. Every fingerprint/PREFS fail-skip →
   GUARD (idx 391); every chain `IF_NE` → next entry; COLSET dispatches
   correct; clone loop `IF_GE` → CL_DONE (287); label walk `IF_GE` → GUARD
   (391); slot dispatches → their `LB_*` targets; stock guard skips → `p()`
   (idx 420). **No wrong jump, no path that skips the guard, no early
   return, no fall off the end.** The stock tail (insns 391–421) uses only
   v0–v3 and v6 (`this`) — it never reads v7–v13, the params the hook
   clobbers — so hook register reuse cannot break it. `this` (v6) is never
   written by the hook.
2. **Fingerprint (item[3] → codePointAt) — CLEARED.** Verified against the
   re-decoded `0x7f170fd2.xml` (52 entries, 26 letters × upper/lower, via
   `press_data`): **every token[3] equals `Phase3Tables.LOOKUP_CP` exactly**,
   and **no token[3] is blank (`U+200B`) or null**. The 26-way chain cannot
   misbehave for any key.
3. **SharedPreferences / Context read — not the cause.** The Context comes
   from `this.f.b` (`Lqmo->b`, idx 260) and is null-guarded (skip to GUARD);
   a missing prefs file makes `getString` return `null` → skip to GUARD.
   But this is moot: the class fails verification **before any of these
   instructions execute**.
4. **Substitution path throwing (`List.set`, `Lpva` ctor, `Lwhe.o`) — not the
   cause.** The clone has 9 items (copied from the 9-item default list), all
   slot indices (0,3,4,5,6 / 1,4,5,3,7 / 1,4,3,5,7) are within 0–8;
   `Lpva` ctor args are guarded (code point from a real emoji string,
   `Lpuz` obtained from the same slot's default item — though the field
   read that obtains it is itself one of the 7 violations). These never
   execute because the class never loads.
5. **Label-walk throwing (null view, bad cast, index) — not the cause.**
   Same reason: the walk never executes. Its `Lqmc.b`/`d` reads are
   violations §1b, not runtime throws.
6. **Register initialization — CLEARED.** RegInitCheck re-run on the shipped
   dex = 0 on `c()` (and Fix11 = 0). The hook's 13-reg zeroing (idx 70–82)
   uses `CONST_4` (int) for registers later used as refs, but every such
   register is REF-seeded (object load / `move-result-object` / `new-instance`)
   before its first ref use on every path.
7. **`Lqmu.o()` / `i()` centring, glide, insert — UNTOUCHED.** CanonDiff
   classes2 = 1 method only (`Lqmu.c`); `o()`/`i()` are byte-identical to
   Fix11. The Fix5 `v11` merge defect is not present.
8. **Stale install — RULED OUT.** Sam can customize: the editor activity is
   new in Phase 3 (176071851) and renders, so 176071851 is what is installed
   and running. The popup failing while a new activity in the same APK works
   is the signature of a **single-class** verification failure, not a stale
   install.

---

## 6. Hypothesis mapping (before vs after customizing)

The defect is a **class-load VerifyError**, not a runtime exception in a
prefs-dependent path. That fixes the mapping:

| Sam's answer | Meaning |
|---|---|
| **Popup failed even BEFORE he customized** (fresh install, no prefs) | **Confirms this diagnosis.** A VerifyError kills `Lqmu` at class load, so the popup cannot appear on the very first long-press, prefs or no prefs. |
| **Popup worked BEFORE customizing, broke only AFTER** | **Refutes this diagnosis.** If the class verified, the defect would be a *runtime* exception in the substitution/label-walk path (which only runs when a custom pref exists) — e.g. inside `Lwhe.o()` or the `Lqml->e` stash. The 7 sites would then be a red herring (their field reads are fine *at runtime*, since the objects really are `Lpva`/`Lqmc`). |

Note the asymmetry: a **runtime** exception in the hook can only break the
popup *after* customizing (the substitution/label paths only run with a
custom pref); a **verification** failure breaks it *always*. Sam's report so
far (popup never appears; editor works) is consistent with the verification
failure, and the editor working already proves the install is Phase 3.

---

## 7. What device evidence would confirm / refute

* **Confirming (expected):** Install Phase 3 on a *fresh* profile (or clear
  the app's SharedPreferences `nobi_longpress_emoji` so no custom pref
  exists, force-stop, then long-press). If the popup still never appears,
  the class-load VerifyError is confirmed — the hook's substitution path
  never ran, yet the popup is dead.
* **Refuting:** If the popup *opens* with no custom prefs set (stock lists,
  Fix11 behaviour) and only breaks *after* a key is customized, the class
  verified fine and the defect is a runtime exception in the
  substitution/label-walk path — re-diagnose from that path (`Lwhe.o()`,
  `Lqml->e` stash, `List.set`).
* **Strongest confirmation (if available):** a logcat `VerifyError` naming
  `Lqmu` / `Lqmu.c` at first long-press. (The in-app DiagLogger cannot
  capture it — the class fails before any of its code runs; `Lqck.D`'s
  catch-all contains the error silently.)

---

## 8. Files & commands (reproducibility)

* Shipped Phase 3 dex: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-phase3/verify/classes2.dex` (all dexes in `verify/final-dex/`)
* Working Fix11 dex: `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-phase3/verify/fix11/classes2.dex` (all dexes in `verify/fix11-dex/`)
* Full dumps: `verify/diag/phase3-c.txt` (422 insns), `verify/diag/fix11-c.txt` (101 insns); branch table `verify/diag/phase3-branches.txt` (99 branches)
* FieldCheck source: `verify/diag/FieldCheck.java` (compiled into `verify/gate-classes/`; run: `java -cp gate-classes:dexlib2-2.5.2.jar:guava.jar FieldCheck <dex-dir> <class> <method>`)
* Change report: `outputs/change-report.md`; patcher: `tools/src/Phase3HookPatch.java`
* Fingerprint check: re-decoded `verify/redecode/res/xml/APKTOOL_DUMMYVAL_0x7f170fd2.xml` vs `tools/src/Phase3Tables.java` (52/52 token[3] match, 0 blank)
* Independent re-runs on the shipped dex: RegInitCheck `Lqmu.c` = 0;
  VerifyPatch `Lqmu` = 0 (12 methods / 189 invokes);
  VerifyBranches classes2 = 0 bad (121,033 branches)

## 9. Caveat (honest)

The defect is proven by a **machine differential** (Fix11 0 vs Phase 3 7
violations, all in the new hook, with the stock guard's identical pattern
passing) plus the **established verification model** (Fix2 proved ART rejects
a non-assignable invoke receiver; field-access receivers are checked by the
same verifier pass, and Google's own stock code narrows before every such
read). What I cannot do from here is run ART: the exact on-device error
string was not captured (no logcat, and the DiagLogger cannot see a
class-load failure). The distinguishing device test in §7 settles it.
