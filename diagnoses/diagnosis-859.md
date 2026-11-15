# Diagnosis 859 — the REAL def-parse builder, and why no Toast appeared

**Date:** 2026-11-15 · **Project:** `~/workspace/apk-edits/nobiboard-2026-11-11-longpress-defparse-diag2/`
**Scope:** DIAGNOSIS ONLY. No APK built, signed, uploaded, or modified. Read-only analysis on the shipped 858 dex (md5-verified: classes.dex `119b975b…`, classes2.dex `87aeab3d…`).
**Question:** 859's P0 Toast (`"DEF 859: Lpud.m runs"`, at `Lpud.m` idx 0) never appeared on Sam's phone, and the popup showed defaults. Was the 858 def-parse hook in the WRONG method?

## VERDICT — `Lpud.m` IS the real builder. The 858 hook was placed correctly.

The 858 agent's chain claim is **VERIFIED correct, in full**. `Lpud.m` is NOT the wrong method. The failure is in the hook's **runtime conditions** (the prefs-read Context at def-parse time), not in the hook location. **Do NOT move the hook** — and do NOT re-insert it into `Lqmu.c()` / `Lqml.c()` (Sam's directive + proven to break the popup).

---

## 1. The full verified chain (fd2 letter key → popup list)

All insn indices below are from the shipped 858 dex (classes.dex unless noted).

```
fd2.xml  <softkey_list splitter="," template_id="@id/0x7f0b2665">
           <softkey long_press_data="U+200B,U+1F495,U+200B,U+1F496,U+1F60D,..."
                    press_data="a" />
  ↓
Lpxa.a  (keyboard-def XML node parser, "softkey" branch, insn 140)
  insn 147: getAttributeResourceValue("template_id") → 0 (individual softkeys
            have no template_id; the list does)
  insn 150-153: v2 = Lpxa.g — the Lpwz template set by the softkey_list
            handler (insn 136). Lpxa.j returns the template by id.
  insn 161-162: unicode range == null → fall through to insn 163 (plain key)
  insn 169: Lpwu.q(Ltjb)          → dispatches to Lpwz.q (Lpwu→Lpur→Lpuq chain,
            §2) → Lpwz.h (binds the softkey's attributes)
  insn 170: Lpuq.d()              → dispatches to Lpwz.d → Lpwz.f
  ↓
Lpwz.h  (softkey node handler on the template)
  insn 5:  getIdAttributeResourceValue(0) → key id → Lpwt.a
  insn 9-13: loop over ALL attributes of the <softkey> (getAttributeCount,
            getAttributeName(i)); skips only "id" / "template_id"
  insn 24-46: for each other attribute, Lpub.e(Context, name, value, Lway)
            → dispatches to the Lpue's registered Lptq handler for that name
  insn 47-60: attributes that match NO Lpue handler are deferred into
            Lpwz.y (a list of (attrs, index) pairs) — handled later by
            Lpwz.f insns 252-272 via Lpwt.e(attrs, index)
  ↓  (binding: Lpub.e → Lptq.c → Lptp.c → Lptw.a → Lpvb.h, §4)
Lpwz.f  (softkey template expansion — builds the SoftKeyDef)
  insn 82-94:  iterate Lpwz.b (ArrayList<Lpue> — the template's actions)
  insn 95-110: per Lpue: v4=Lpue.b (the Lpud), v5=[I from Lpue.d (Lptw),
            v6=[Lpuz; from Lpue.e (Lpts), v7=[Object; from Lpue.f (Lptz)
  insn 110:    INVOKE_VIRTUAL Lpud.m([I, [Lpuz;, [Object;)V        ← THE HOOK POINT
  insn 198:    Lpud.c() → ActionDef   (Lpud.b → ActionDef.f = the Lwhe list)
  insn 201:    Lpwt.o(ActionDef)      (stores the ActionDef in the builder)
  insn 273:    Lpwt.b() → SoftKeyDef  (returned to the caller)
  ↓
Lpud.m  ([I, [Lpuz;, [Object;)V — THE REAL BUILDER (62 stock insns)
  insn 255-258: null/empty arrays → stores the empty list Lwmm.a into Lpud.b
  insn 260-270: lengths → Math.max → loop bound
  insn 271-273: NEW_INSTANCE Lwgz; <init>(4)   (the list builder)
  insn 280-302: loop: AGET code point (v7), AGET_OBJECT Lpuz (v9) + label (v8)
            → NEW_INSTANCE Lpva; CONST 0x7fffffff;
            Lpva.<init>(I, Lpuz, Object, I)      ← each long-press item
  insn 303:    Lwgz.h(Lpva)
  insn 306-310: Lwgz.g() → Lwhe → IPUT Lpud.b      ← the finished Lpva list
  ↓
Lpud.c() → NEW_INSTANCE ActionDef; ActionDef.<init>(Lpud)
  (ActionDef.f = the Lwhe built by Lpud.m)
  ↓
Popup (at long-press time):
  Lqml.c (classes2.dex) insn 24: IGET_OBJECT v4, v3, ActionDef->f:Lwhe
  Lqml.c insn 25: IPUT_OBJECT v4, v0, Lqml->e     ← the list the popup uses
  Lqmu.c builds the popup cells from Lqml->e
```

### 1a. `Lpwz.f` insn 110 → `Lpud.m` — VERIFIED
Dump of `Lpwz.f` (classes.dex): insn 110 is exactly
`INVOKE_VIRTUAL {v4,v5,v6,v7} Lpud;->m([I[Lpuz;[Ljava/lang/Object;)V`,
with v5/v6/v7 loaded from `Lpue->d:Lptw` / `Lpue->e:Lpts` / `Lpue->f:Lptz`
(insns 101-109). The 858 claim is correct.

### 1b. All callers of `Lpud.m` (all dexes, via ReferenceInstruction refs)
| Call site | What it passes | Role |
|---|---|---|
| `Lpwz.f` insn 110 (classes.dex) | the Lpue's bound arrays (see §4) | **the fd2 def-parse path** |
| `Lpud.e` insn 198 (classes.dex) | forwards its own `Ltjb` parse | wrapper; called by `Lpud.g` insn 0 and `Lpwt.a` insn 58 |
| `Lpht.a` insn 28 (classes2.dex) | **EMPTY** `[I` (FILLED_NEW_ARRAY, 0 elements), null `[Lpuz;`/`[Object;` | synthetic special-key builder (hardcoded res ids `0x7f0b1040`, `0x7f040179`, `0x7f1407a5`) — NOT letter keys |
| `Lpht.a` insn 60 (classes2.dex) | same, empty | same (second synthetic key: `0x7f0b103f`, `0x7f040178`, `0x7f140482`) |

`Lpht.a` is called by `Lphc.l` insn 32 and `Lphe.a` insn 25 (SoftKeyView code). It builds synthetic keys with EMPTY long-press arrays — the 858 hook's size-9 fingerprint correctly skips these (length 0 ≠ 9 → GUARD). Note: if `Lpht.a` ever ran, `Lpud.m` would be called and the P0 Toast would fire — so the no-Toast also means `Lpht.a` never ran in Sam's 859 session (or the Context was null — §5).

### 1c. How `Lpwz.f` is reached for a plain fd2 softkey (branch targets verified)
`Lpxa.a` branch dump (targets verified with BranchDump):
- insn 149 `IF_NEZ → 155` (template_id != 0 → look up by id); template_id == 0
  falls through to insn 150.
- insn 151 `IF_NEZ → 153`: `Lpxa.g` (the softkey_list's template) != null → v2 = g.
- insn 162 `IF_NEZ → 187`: unicode range == null → fall through to insn 163.
- insn 163-182: `Lpwu.q(Ltjb)` (→ `Lpwz.q` → `Lpwz.h`, binds the attributes),
  then `Lpuq.d()` → `Lpwz.d()` → `Lpwz.f()` (builds the SoftKeyDef).
- The unicode-range path (insn 187-228) instead calls `Lpwz.h` directly and
  then `Lpxa.e` (insn 222), which calls `Lpwz.f` at its insn 18.

Interface dispatch is real, not assumed:
`Lpwu` extends `Lpur` extends `Lpuq` (verified via ClassInfo), and both
`Lpwz` and `Lpwt` implement `Lpwu` — so `Lpuq.d()` on a `Lpwz` template
dispatches to `Lpwz.d()`, whose entire body is just `Lpwz.f()` + return.

### 1d. The template 0x7f0b2665 (res/xml/0x7f170ef3.xml, line 43)
```xml
<softkey_template id="@id/0x7f0b2665" ...>
    <action data="$press_data$"        intention="$press_intent=DECODE$"
            keycode="ALPHABET" popup_label="$press_data$" type="PRESS" />
    <action data="$long_press_data$"    intention="$long_press_intention=DECODE$"
            keycode="ALPHABET" merge_insertion_index="$merge_index=0$"
            popup_label="$long_press_data$" type="LONG_PRESS" />
    <label location="..." value="$press_data$" />
</softkey_template>
```
So each fd2 softkey produces **two** `Lpue` entries and therefore **two**
`Lpud.m` calls: PRESS (data = the single letter, `[I` length 1) and
LONG_PRESS (data = the 9-token list, `[I` length 9). The hook's size-9
fingerprint matches only the LONG_PRESS call — exactly as designed.

---

## 2. Where `long_press_data` is actually consumed (correction to a framing in the 858 report)

- The string `"long_press_data"` exists in **NO dex** (StringSearch, all dexes: 0 hits).
- Its hashCode, **484777929 (0x1CE51FC9)**, appears as a CONST in **NO** method
  and in **NO** `SPARSE_SWITCH` payload in any dex (ConstSearch over all 6 dexes).
- `Lpwt.e(AttributeSet, I)` — the attribute handler with the hashCode switch —
  handles exactly 15 attributes (`long_press_delay`, `popup_timing`,
  `enable_slide_actions_in_a11y_mode`, `touch_action_repeat_interval`,
  `additional_content_description`, `tooltip_text`,
  `touch_action_repeat_start_delay`, `alpha`, `multi_touch`, `span`,
  `disable_lift_to_tap`, `layout`, `a11y_click_action_label`,
  `content_description`, `slide_sensitivity`). **`long_press_data` is NOT one of them.**
- So `long_press_data` is NOT consumed by any name/hashCode dispatch. It is
  consumed by the **template-placeholder mechanism**: the template's
  `<action data="$long_press_data$">` registers an `Lptq` handler for the
  attribute name; when a `<softkey>` supplies that attribute, `Lpwz.h` →
  `Lpub.e(Context, name, value, Lway)` → `Lptq.c(Context, value, Lway)` →
  `Lptp.c` → `Lptw.a` parses the value into the `[I` holder.
  (`Lpub` holds a `Map<String, List<Lptq>>`; `Lptp` is the only `Lptq`
  implementor and the base of all holders `Lptr/Lpts/Lptt/Lptu/Lptv/Lptw/
  Lptx/Lpty/Lptz/Lpua`.)

## 3. The fingerprint is VALID (the `[I` really is the 9-token list)

- `Lptw.a(Context, String, Lway)` → `Lpvb.h(String, Lway)` → `[I`.
- `Lpvb.h`: splits the string by the `Lway` splitter (`Lway.j(CharSequence)`,
  "," for fd2) and converts each token with `Lpvb.f(String)` (parses
  `"U+XXXX"` → code point int); `Lwta.I(Collection)` → `[I`.
- `Lptz.a` → `Lsah.t(String, Lway)` → `[String;` (the labels).
- For A: fd2 token list is
  `U+200B,U+1F495,U+200B,U+1F496,U+1F60D,U+1F618,U+200B,U+1F970,U+200B`
  → `[I` length 9, item[3] = **0x1F496** (💖 — the LEFT emoji, not the centre;
  token[4] = 0x1F60D 😍 is the displayed centre). `LOOKUP_CP[0] = 0x1F496`
  (confirmed in the shipped 858 dex: `Lpud.m` insn 5 = `CONST v1, 0x0001f496`).
- So the 858 hook's fingerprint (length == 9, item[3] → 26-way chain) **does
  match** the LONG_PRESS call for every A–Z key. The fingerprint is NOT the
  reason customization failed.

---

## 4. The def is parsed on a BACKGROUND thread (proven)

- The ImeListDef (the parsed def) is produced by `Levj.a()`, which is invoked
  from `Lcwj.call` insn 35 — a `Callable` (`Lcwj` is used via
  `ModuleManager$ModuleInfo.newCreateModuleFutureTask()`, i.e. a FutureTask).
  The `Lnwa` code in the same callable deals with `Future.isDone()` / result
  maps. So the def-parse runs on a **background executor thread**, not the
  main thread.
- The parsed def is cached **in memory** by the IMEManager/`Levj` (a `Lwhl`
  map of locale → def supplier, built by `Levj.c()`). It is invalidated only
  when a preference in the reload list changes. The reload list is the
  13-int `FILL_ARRAY_DATA` in `Levj.a()` — it contains `0x7f140944`
  (`enable_number_row`) but **NOT** `0x7f14099b` (`enable_emoji_row`) and
  **NOT** any `nobi_longpress_emoji` key (it's a custom file, so it can never
  be in the list).
- **No disk cache of the parsed def was found.** `KeyboardDef` and `Lpva`
  are `Parcelable` (so a def *could* be marshalled), but no file-based
  persistence of the parsed def (no cache file, no serialize-to-disk path)
  exists in the code I traced, and `SoftKeyDef`/`ActionDef`/`ImeListDef`
  are not `Serializable`/`Parcelable`. The in-memory cache is cleared by a
  force-stop (the process dies), so **after a force-stop the def IS re-parsed
  from XML and `Lpud.m` DOES run**. The "def cached on disk" explanation is
  therefore **unproven and unlikely**.
- The IME service (`LatinIME`) has **no `android:process`** in the manifest,
  so it runs in the **main process** with the Application
  (`HiltLatinApp`).

## 5. Why no Toast appeared — ranked explanations

The 859 P0 Toast is at `Lpud.m` idx 0 and fires on the FIRST `Lpud.m` call
per process **provided** (a) `Lpud.m` is called at all, (b)
`ActivityThread.currentApplication()` is non-null, and (c) the Toast is
actually displayed. The 859 dump (insns 0-12) confirms the instrumentation
is sound: flag check → set flag → get Application → null-guard → makeText →
show. So the no-Toast result has exactly these possible causes:

| # | Explanation | Evidence / status |
|---|---|---|
| 1 | **`ActivityThread.currentApplication()` is null at def-parse time → the null-guarded Toast is skipped** (and, in 858, the hook's prefs read fails the same way → GUARD → defaults) | **LEADING HYPOTHESIS — fits BOTH 858 and 859 with one cause.** Mechanism: the def-parse runs on a background executor thread during early process startup (ModuleManager FutureTask, §4). If the parse fires before the Application is bound (or the accessor is unusable from that thread), the Context is null. **NOT proven statically** — in AOSP `currentApplication()` is a static field set when the main thread binds the Application, so it *should* be non-null after startup; I could not prove a null from the dex alone. It is the only single-cause explanation for both failures, so it is the first thing to test. |
| 2 | **The Toast fired but Sam didn't see it** (transient + background IME) | PLAUSIBLE. P0 fires exactly once, at parse time — which is when the keyboard service starts, possibly *before* Sam is looking at the screen. A Toast from the IME (a background process from the system's point of view — the user is in another app) is also unreliable on Android. Note the pattern: Toasts from the IME path have NEVER been seen by Sam in this arc (842's `Lqck.D` Toasts also reported as "KUCH NHI aaya"). A Toast is a weak diagnostic for a background-thread parse. |
| 3 | **Sam didn't actually run 859** (stale install/process) | POSSIBLE, cannot verify from the dex. If the old 858 process was still alive when he tested (install-over does not always kill the IME process immediately), the 859 Toast code never executed. Must be confirmed with Sam (did he force-stop AFTER installing 859? does Settings show 176071859?). |
| 4 | **`Lpud.m` never runs (def not re-parsed / wrong method)** | **REFUTED as "wrong method"** (§1 — it IS the builder, and the chain reaches it for fd2 letter keys). "Not re-parsed" would need a disk cache — **no evidence** (§4). So this is the *least* likely explanation. |
| 5 | **The fingerprint / prefs never match, so the hook does nothing** | **REFUTED as an explanation for the no-Toast.** P0 fires on ANY `Lpud.m` call, before the fingerprint and before the prefs read. A fingerprint or prefs failure explains 858's missing customization but CANNOT explain 859's missing Toast. (The fingerprint itself is verified valid, §3.) |

**Conclusion on the no-Toast:** it does NOT prove `Lpud.m` never runs, and it does NOT refute the hook location. The most probable cause is #1 (Context null at parse time — which would also be the whole explanation for 858's missing customization) or #2/#3 (Toast/invisible or stale install). The next diag must therefore **not rely on a Toast** and **not rely on `ActivityThread.currentApplication()`**.

## 6. What the 858 failure most likely was

Putting §1–§5 together: the hook was in the right method, the fingerprint
is valid, and the def IS re-parsed after a force-stop. The remaining single
point of failure that explains BOTH 858 (no customization after fresh install
+ force-stop) and 859 (no Toast) is the **Context source**: everything the
hook and the diag do — prefs read AND Toast — goes through
`ActivityThread.currentApplication()` at def-parse time, on a background
thread, during early startup. If that accessor returns null there, the hook
goes to GUARD (defaults) and the diag Toast is skipped (silence) — exactly
what Sam observed on both builds. This is a hypothesis, not a proof; the
file-based diag in §7 is designed to settle it.

## 7. Recommended hook point and next step (design, NOT code)

### Keep the hook in `Lpud.m` — do not move it
- `Lpud.m` is the only method that builds the `Lpva` list from the parsed
  arrays, and it runs at def-parse time — long before any popup is built.
  It is the correct hook point. The hook design (fingerprint → 26-way chain
  → prefs → split/validate → clone → 5 substitutions → fall through to the
  stock body; GUARD leaves the arrays untouched) is sound and its gates all
  passed. **Do not move it to `Lqmu.c()` / `Lqml.c()`** (Sam's directive;
  both hooks already proved to break the popup).

### Fix the Context source (the actual defect to address)
- Replace `ActivityThread.currentApplication()` in the hook with a Context
  obtained from a **static field populated by a tiny hook in a method that
  is guaranteed to run on the main thread after the Application exists and
  before any def-parse** — e.g. the IME service `LatinIME.onCreate()` or the
  Application `HiltLatinApp.onCreate()`, storing `getApplicationContext()`
  into a new static field (e.g. `Lpud->appCtx:Landroid/content/Context;` in
  classes.dex, which has free field_ids). The def-parse (background thread)
  then reads that static field. Rationale: `onCreate` runs on the main
  thread after the Application is created, and it always runs before the
  keyboard def is needed (the def is built when the keyboard view is
  created, after `onCreate`). The static field survives for the process
  lifetime and is re-populated on every process start (so it survives
  force-stop correctly).
- Keep `ActivityThread.currentApplication()` only as a fallback if the static
  field is null (defensive; if both are null → GUARD, as now).
- Do NOT read the prefs file by raw path as the primary route (XML parsing
  in dex is heavier and needs new refs); the static-field Context is the
  minimal change.

### Next diag build (for the parent to commission — not built here)
- A **file-based** diag (new versionCode), instrumentation ONLY in `Lpud.m`
  (same one-pass discipline, same gates), that writes a small persistent
  record instead of relying on a transient Toast — e.g. append one line per
  `Lpud.m` call to a file in the app's external files/Download area
  (a location Sam can open without root), recording: timestamp, centre code
  point, fingerprint match (y/n), Context null? (y/n), prefs string (or
  NULL), substitution (SUB OK / GUARD + reason). Keep the P0/A Toasts too
  (they cost nothing), but the **file is the ground truth**.
- Sam's test for it: install over 859 → force-stop → open keyboard → long-press
  A (customized) → report the file's contents (or screenshot it) + whether
  any Toast appeared. The file settles, in one run, whether `Lpud.m` runs,
  whether the Context is null, whether the prefs read returns his value,
  and whether substitution happens.
- Before building it, ask Sam two things: (a) did he force-stop AFTER
  installing 859 (not only before)? (b) does Settings show 176071859?
  If he was not actually on 859, the no-Toast result is void and no new
  build is needed to explain it.

### If the file-based diag shows the Context is NOT null and prefs ARE read
- Then the failure is downstream: either the substitution happens but the
  substituted list is not the one the popup uses (a second list build —
  search for another producer of `ActionDef.f` / another `Lpud.m`-like
  builder), or the def is genuinely not re-parsed (re-open the disk-cache
  question with the file as evidence). Do not rebuild blind.

## 8. What was ruled out (with evidence)
- "Long-press code is the problem" — no; classes2 is byte-identical to
  Fix11 in 858/859 (md5 `87aeab3d…`), and the popup works (defaults show).
- "The 858 chain claim (`Lpwz.f` insn 110 → `Lpud.m`) is wrong" — no;
  verified in the dump, and the reachability for plain fd2 softkeys is
  proven by the branch dump + the `Lpwu→Lpur→Lpuq` interface chain.
- "The fingerprint doesn't match (wrong `[I` / wrong item[3])" — no;
  `Lpvb.h` provably yields the 9 code points, item[3] = 0x1F496 for A.
- "`long_press_data` is consumed by a hashCode switch in `Lpwt.e`" — no;
  the string and its hashCode occur nowhere; `Lpwt.e` handles 15 other
  attributes. The real path is the template-placeholder binding (§2).
- "A disk cache of the parsed def prevents re-parse" — no evidence found;
  the cache is in-memory (`Levj`/IMEManager) and dies on force-stop.
- "No gate catches this" — correct; no gate checks the *runtime* Context
  or Toast visibility. The gap is diagnosable only from device evidence,
  which is why the file-based diag is the next step.

## Appendix — methods dumped (shipped 858 dex)
`Lpwz.f` (0-275), `Lpxa.a` (0-267) + BranchDump, `Lpxa.e`, `Lpxa.j`,
`Lpwz.d`, `Lpwz.h`, `Lpwz.q`, `Lpwz.g` (ref), `Lpwt.e`, `Lpub.e`,
`Lptp.c`, `Lptw.a`, `Lptz.a`, `Lpvb.h`, `Lpud.m` (250-311 stock body),
`Lpud.c`, `Lqml.c` (0-30, classes2), `Lpht.a` (classes2), `Levj.a`,
`Levj.c`, `Lcwj.call`, `Lpxa`/`Lpwz`/`Lpud`/`Lpue`/`Lpwt`/`Lpht` ClassInfo,
`Lpwu`/`Lpur`/`Lpuq`/`Lpuo`/`Ltja`/`Lpub` Ifaces, `Lptq`/`Lptp` Subclasses,
`Lpuq`/`Lpxa` StringSearch (type), `Lpud.m`/`Lpwz.f`/`Lpxa.a`/`Lpxa.e`/
`Lpud.e`/`Lpht.a`/`Lpwz.d`/`Levj.a` MethodCallers, ConstSearch
484777929 (0 hits), StringSearch `long_press_data` (0 hits),
`long_press_delay` (Lpwt.e insn 11, Lpwz.g insn 17), `softkey_list`
(Lpxa.a insn 121). Tools in `verify/tools/`, compiled classes in
`verify/classes/`, dexes in `verify/dex-858/` and `verify/dex-859/`.
