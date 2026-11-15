import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.rewriter.*;
import java.io.File;
import java.util.*;

/**
 * CONTEXT FIX (code 176071860) for NobiBoard def-parse customization.
 * Base = 176071859 (858 hook + Toast instrumentation).
 *
 * PROBLEM (diagnosis-859): ActivityThread.currentApplication() is NULL at
 * def-parse time, so the 858 hook goes to GUARD and the customization never
 * applies — even though Lpud.m IS the right method and Sam force-stopped.
 *
 * FIX (Change 1): a static field Lpud->appCtx is populated by a tiny hook in
 * Lesb.onCreate() (the Application base class's onCreate — runs on the main
 * thread, this IS the Application Context, runs before any Service/Activity
 * and hence before any def-parse). Lpud.m's hook uses appCtx FIRST and falls
 * back to currentApplication() only if appCtx is null. The three OLD
 * currentApplication() fetches inside Lpud.m (P0 idx 5, prefs idx 165, P3
 * idx 375) are NOP'd out so they cannot overwrite v5 with a null.
 *
 * DIAGNOSTIC (Change 2): a reliable record is written to SharedPreferences
 * "nobi_diag" (NO Toast as primary) and displayed in the editor activity's
 * new "Diagnostics" section (editor/src/.../NobiEmojiEditorActivity.java):
 *   last_run — "run ts=<currentTimeMillis>" on every def-parse.
 *   last_a   — "A cp=1f496 ctx=<static|fallback|null> prefs=<value|null>
 *               outcome=SUB OK|GUARD" on every A-key def-parse.
 * The record includes the Context source actually used and the prefs string
 * actually read, so it distinguishes "hook never ran" / "context null" /
 * "prefs null" / "GUARD reason" without a logcat.
 *
 * INSERTIONS into Lpud.m (859: 387 insns, 16 regs; all even-unit totals):
 *   E  — entry Context resolution (before orig idx 0): resolve appCtx first,
 *        else currentApplication(), record the source in diagCtxSrc, reset
 *        diagPrefs to null. 14 insns, 24 units.
 *   L  — last_run write (between orig idx 4 and orig idx 5, inside P0's
 *        first-call branch): "run ts=<currentTimeMillis>" -> nobi_diag.
 *        Wide J is reset to INT(0) after use so it cannot leak into P0's
 *        merge. 24 insns, 48 units.
 *   S  — stash diagPrefs = v7 (between orig idx 194 and idx 195, end of P1,
 *        A-key only). 1 insn, 2 units.
 *   W_SUB  — last_a SUB write (between orig idx 303 and idx 304, after P2).
 *        A-key only (explicit A-check). 33 insns, 68 units.
 *   W_GUARD — last_a GUARD write (between orig idx 384 and idx 385, after
 *        P3's Log). A-key only (explicit A-check). 33 insns, 68 units.
 * NOP REPLACEMENTS (same units, no shift, no retarget):
 *   orig idx 5-6 (P0 fetch), 165-166 (prefs fetch), 375-376 (P3 fetch).
 *   Each INVOKE_STATIC (3 units) -> 3 NOPs; each MOVE_RESULT_OBJECT (1 unit) -> 1 NOP.
 *
 * BRANCH RETARGETING (16, computed from final addresses; verified by
 * construction — a branch is retargeted iff its source and target are in
 * different insertion regions):
 *   idx 2 IF_NEZ -> P0_SKIP (30)         [L]
 *   idx 178 IF_NE -> P1_SKIP (195)       [S]
 *   idx 295 IF_NE -> P2_SKIP (304)       [W_SUB]
 *   idx 373 IF_NE -> P3_SKIP (385)       [W_GUARD]  (385, the NOP — NOT 386)
 *   idx 386 GOTO_16 -> clean-up (305)    [W_GUARD, offset decreases]
 *   the 11 GUARD branches -> P3 (372)    [S and/or W_SUB]
 * All other original branches keep their offsets (verified below).
 * The 25 chain GOTO_16s still target idx 165 (now a NOP — same address).
 *
 * LESB.onCreate HOOK: insert SPUT_OBJECT v1, Lpud->appCtx at idx 0 (1 insn,
 * 2 units, even). Insert-at-0 preserves the IF_NEZ (idx 1 -> idx 7) offset.
 * Lesb extends Lesf -> Lmxa -> Lpdy -> android.app.Application, so v1 (this,
 * type Lesb) IS assignable to Context. Field is PUBLIC STATIC (0x9) because
 * it is accessed cross-class. Use of appCtx also requires the class to be
 * verified — Lesb.onCreate runs before any def-parse, so appCtx is set.
 *
 * NEW FIELDS on Lpud (3):
 *   appCtx     :Landroid/content/Context;  public static  (0x9)
 *   diagCtxSrc :Ljava/lang/String;         private static (0xA)
 *   diagPrefs  :Ljava/lang/String;         private static (0xA)
 * NEW METHOD REFS: NONE — currentTimeMillis, append(J), getSharedPreferences,
 * edit/putString/apply, Toast.makeText/show, Log.e, StringBuilder are all
 * already in the 859 pool (verified by RefBatch). method_ids stays 57,519.
 * field_ids 38,007 -> 38,010.
 *
 * REGISTER DISCIPLINE (Lpud.m, 16 regs — same as 858/859):
 *   v0 INT temp (A-check const, then 0 = MODE_PRIVATE; wide low half in L)
 *   v1 INT temp (Toast duration; wide high half in L)
 *   v2 INT centre CP (fingerprint; A-check operand — never overwritten here)
 *   v3 REF pref key — NOT used by the new blocks (clean-up resets it)
 *   v4 INT colId — NEVER a MOVE_RESULT destination (v13-clobber lesson)
 *   v5 REF Context (the resolved Context from E; never overwritten again)
 *   v6 REF (StringBuilder / SharedPreferences / Editor / Toast)
 *   v7 REF (prefs string — read only in S; cleaned up after)
 *   v8 REF (record / diag field values / Log tag)
 *   v9/v10 REF (clones — untouched)
 *   v11 REF const strings
 *   v12 this, v13 [I, v14 [Lpuz;, v15 [Object;
 * The clean-up (orig idx 305-310) resets v3/v6/v7/v8/v11 to INT(0) before
 * the original code — W_SUB/W_GUARD leave v6/v8/v11 as REF, which is exactly
 * what the clean-up handles. v0 is left as INT(0) by L and W blocks.
 * MOVE_RESULT destinations: only v6 (REF, from Toast/getSharedPreferences/
 * edit/putString/toString) and v8 (REF, from toString) — both dead scratch
 * at those points. v0/v1 in Block L are reset to INT(0) after the wide use.
 */
public class CtxFixPatch {
    // ---------- registers (Lpud.m: 16 regs, params v12..v15) ----------
    static final int R_SLOT = 0;    // INT temp
    static final int R_TMP = 1;     // INT temp
    static final int R_CP = 2;      // INT centre code point
    static final int R_PREFKEY = 3; // (hook) REF pref key — NOT used by new blocks
    static final int R_COL = 4;     // (hook) INT colId — NOT used by new blocks
    static final int R_CTX = 5;     // REF Context (resolved at entry, Block E)
    static final int R_PREFS = 6;   // REF (StringBuilder / SharedPreferences / Editor / Toast)
    static final int R_PREFSTR = 7; // REF (prefs string — read by S, never written here)
    static final int R_PARTS = 8;   // REF (record string / diag field value / Log tag)
    static final int R_CLONE_I = 9; // (hook) — NOT used by new blocks
    static final int R_CLONE_O = 10;// (hook) — NOT used by new blocks
    static final int R_PART = 11;   // REF const strings
    static final int R_THIS = 12;
    static final int R_ARR_I = 13;
    static final int R_ARR_U = 14;
    static final int R_ARR_O = 15;

    static final int CP_A = 0x1F496; // A-key token[3] (Fix11 fd2.xml)
    static final String TAG = "NobiDiag";
    static final String DIAG_PREFS_NAME = "nobi_diag";

    // ---------- method refs (all already in the 859 pool) ----------
    static final ImmutableMethodReference TOAST_MAKE =
        new ImmutableMethodReference("Landroid/widget/Toast;", "makeText",
            Arrays.asList("Landroid/content/Context;", "Ljava/lang/CharSequence;", "I"),
            "Landroid/widget/Toast;");
    static final ImmutableMethodReference TOAST_SHOW =
        new ImmutableMethodReference("Landroid/widget/Toast;", "show",
            Collections.<String>emptyList(), "V");
    static final ImmutableMethodReference LOG_E =
        new ImmutableMethodReference("Landroid/util/Log;", "e",
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"), "I");
    static final ImmutableMethodReference SB_INIT =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "<init>",
            Collections.<String>emptyList(), "V");
    static final ImmutableMethodReference SB_APP_STR =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("Ljava/lang/String;"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_APP_INT =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("I"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_APP_LONG =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("J"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_TOSTRING =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "toString",
            Collections.<String>emptyList(), "Ljava/lang/String;");
    static final ImmutableMethodReference ACTIVITY_THREAD_APP =
        new ImmutableMethodReference("Landroid/app/ActivityThread;", "currentApplication",
            Collections.<String>emptyList(), "Landroid/app/Application;");
    static final ImmutableMethodReference CURRENT_TIME_MILLIS =
        new ImmutableMethodReference("Ljava/lang/System;", "currentTimeMillis",
            Collections.<String>emptyList(), "J");
    static final ImmutableMethodReference GET_SHARED_PREFS =
        new ImmutableMethodReference("Landroid/content/Context;", "getSharedPreferences",
            Arrays.asList("Ljava/lang/String;", "I"), "Landroid/content/SharedPreferences;");
    static final ImmutableMethodReference SP_EDIT =
        new ImmutableMethodReference("Landroid/content/SharedPreferences;", "edit",
            Collections.<String>emptyList(), "Landroid/content/SharedPreferences$Editor;");
    static final ImmutableMethodReference SP_PUT_STRING =
        new ImmutableMethodReference("Landroid/content/SharedPreferences$Editor;", "putString",
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"),
            "Landroid/content/SharedPreferences$Editor;");
    static final ImmutableMethodReference SP_APPLY =
        new ImmutableMethodReference("Landroid/content/SharedPreferences$Editor;", "apply",
            Collections.<String>emptyList(), "V");

    // ---------- field refs (3 NEW — added by the Module) ----------
    static final ImmutableFieldReference APP_CTX =
        new ImmutableFieldReference("Lpud;", "appCtx", "Landroid/content/Context;");
    static final ImmutableFieldReference DIAG_CTX_SRC =
        new ImmutableFieldReference("Lpud;", "diagCtxSrc", "Ljava/lang/String;");
    static final ImmutableFieldReference DIAG_PREFS =
        new ImmutableFieldReference("Lpud;", "diagPrefs", "Ljava/lang/String;");
    static final ImmutableFieldReference DIAG_FIRST =
        new ImmutableFieldReference("Lpud;", "diagFirst", "Z");

    // ---------- tiny label-based block builder (from DefParsePatch/DiagPatch) ----------
    static class B {
        List<Instruction> code = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();
        static class Fix { int idx; String target; Fix(int i, String t) { idx = i; target = t; } }
        List<Fix> fixups = new ArrayList<>();
        void label(String n) { if (labels.put(n, code.size()) != null) throw new RuntimeException("dup label " + n); }
        void add(Instruction i) { code.add(i); }
        void br21t(Opcode op, int reg, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction21t(op, reg, 0)); }
        void br22t(Opcode op, int ra, int rb, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction22t(op, ra, rb, 0)); }
        void br10t(Opcode op, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction10t(op, 0)); }
        void br20t(Opcode op, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction20t(op, 0)); }
        int totalUnits() { int t = 0; for (Instruction i : code) t += i.getCodeUnits(); return t; }
        void padEven() { if (totalUnits() % 2 != 0) code.add(new ImmutableInstruction10x(Opcode.NOP)); }
        void fixup(int base) {
            padEven();
            List<Integer> addr = new ArrayList<>();
            int a = base;
            for (Instruction i : code) { addr.add(a); a += i.getCodeUnits(); }
            int total = a - base;
            for (Fix f : fixups) {
                int targetAddr;
                Integer ti = labels.get(f.target);
                if (ti == null) throw new RuntimeException("unknown label " + f.target);
                targetAddr = (ti == code.size()) ? base + total : addr.get(ti);
                int insnAddr = addr.get(f.idx);
                int off = targetAddr - insnAddr;
                if (off == 0) throw new RuntimeException("zero branch offset at block idx " + f.idx + " -> " + f.target + " (self-loop!)");
                Instruction old = code.get(f.idx);
                if (old instanceof Instruction10t && (off < -128 || off > 127))
                    throw new RuntimeException("10t branch out of range at block idx " + f.idx + " -> " + f.target + " off=" + off);
                if ((old instanceof Instruction21t || old instanceof Instruction22t || old instanceof Instruction20t) && (off < -32768 || off > 32767))
                    throw new RuntimeException("16-bit branch out of range at block idx " + f.idx + " -> " + f.target + " off=" + off);
                if (old instanceof Instruction21t) { Instruction21t o = (Instruction21t) old; code.set(f.idx, new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), off)); }
                else if (old instanceof Instruction22t) { Instruction22t o = (Instruction22t) old; code.set(f.idx, new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(), o.getRegisterB(), off)); }
                else if (old instanceof Instruction10t) { Instruction10t o = (Instruction10t) old; code.set(f.idx, new ImmutableInstruction10t(o.getOpcode(), off)); }
                else if (old instanceof Instruction20t) { Instruction20t o = (Instruction20t) old; code.set(f.idx, new ImmutableInstruction20t(o.getOpcode(), off)); }
                else throw new RuntimeException("bad fixup insn " + old.getOpcode());
            }
            if (totalUnits() % 2 != 0) throw new RuntimeException("block units still odd: " + totalUnits());
        }
    }

    static ImmutableInstruction35c inv35(Opcode op, int count, int a, int b, int c, int d, int e, MethodReference ref) {
        return new ImmutableInstruction35c(op, count, a, b, c, d, e, ref);
    }

    /** Toast.show() on the Toast object in v6. */
    static void emitShow(B b) {
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, TOAST_SHOW));
    }

    /** Toast.makeText(v5 ctx, v11 msg, v1 duration) -> v6, then show(). */
    static void emitToast(B b) {
        b.add(inv35(Opcode.INVOKE_STATIC, 3, R_CTX, R_PART, R_TMP, 0, 0, TOAST_MAKE));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        emitShow(b);
    }

    /** Log.e(tag, msgReg). tagReg holds the tag string; msgReg holds the message. */
    static void emitLog(B b, int msgReg, int tagReg) {
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, tagReg,
            new ImmutableStringReference(TAG)));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, tagReg, msgReg, 0, 0, 0, LOG_E));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
    }

    /** Build StringBuilder message into v7 (R_PREFSTR) from a constant prefix + an int (v2). */
    static void emitSbMsgInt(B b, String prefix) {
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS,
            new ImmutableTypeReference("Ljava/lang/StringBuilder;")));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(prefix)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_CP, 0, 0, 0, SB_APP_INT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFSTR));
    }

    /** Build StringBuilder message into v8 (R_PARTS) from a constant prefix + a String (v7). */
    static void emitSbMsgStr(B b, String prefix) {
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS,
            new ImmutableTypeReference("Ljava/lang/StringBuilder;")));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(prefix)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PREFSTR, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
    }

    /**
     * Write (keyName, v8) into SharedPreferences "nobi_diag" using the Context
     * in v5 and mode v0 (= 0, MODE_PRIVATE). Null-guarded: if getSharedPreferences
     * returns null, jump to skipLabel. Uses v6 (prefs/Editor) and v11 (key strings).
     * On return, v8 still holds the value string.
     */
    static void emitPrefsWrite(B b, String keyName, String skipLabel) {
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(DIAG_PREFS_NAME)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_CTX, R_PART, R_SLOT, 0, 0, GET_SHARED_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.br21t(Opcode.IF_EQZ, R_PREFS, skipLabel);
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_EDIT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(keyName)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_PART, R_PARTS, 0, 0, SP_PUT_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_APPLY));
    }

    /** Block E — entry Context resolution (before orig idx 0).
     *  diagPrefs = null; v5 = appCtx (or currentApplication() fallback);
     *  diagCtxSrc = "static" / "fallback" / "null" (into v11, then stored).
     *  After this block, v5 = resolved Context (or null).
     */
    static B buildE() {
        B b = new B();
        // diagPrefs = null (reset for this call; P1/S sets it per A run)
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, R_SLOT, DIAG_PREFS));
        // v5 = appCtx
        b.add(new ImmutableInstruction21c(Opcode.SGET_OBJECT, R_CTX, APP_CTX));
        b.br21t(Opcode.IF_NEZ, R_CTX, "E_STATIC");
        // fallback: currentApplication()
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, ACTIVITY_THREAD_APP));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_CTX));
        b.br21t(Opcode.IF_EQZ, R_CTX, "E_NULL");
        // v11 = "fallback"
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("fallback")));
        b.br10t(Opcode.GOTO, "E_STORE");
        b.label("E_STATIC");
        // v11 = "static"
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("static")));
        b.br10t(Opcode.GOTO, "E_STORE");
        b.label("E_NULL");
        // v11 = "null"
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("null")));
        b.label("E_STORE");
        b.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, R_PART, DIAG_CTX_SRC));
        return b;
    }

    /** Block L — last_run write (between orig idx 4 and idx 5, inside P0's
     *  first-call branch). "run ts=<currentTimeMillis>" -> nobi_diag/last_run.
     *  Wide J is reset to INT(0) after use so it cannot leak into P0's merge.
     */
    static B buildL() {
        B b = new B();
        b.br21t(Opcode.IF_EQZ, R_CTX, "L_SKIP");
        // StringBuilder "run ts=" + currentTimeMillis() into v8
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS,
            new ImmutableTypeReference("Ljava/lang/StringBuilder;")));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("run ts=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, CURRENT_TIME_MILLIS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_WIDE, R_SLOT));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_SLOT, R_TMP, 0, 0, SB_APP_LONG));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        // reset the wide regs to INT(0) — the J must not reach P0's merge
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        // write ("last_run", v8)
        emitPrefsWrite(b, "last_run", "L_SKIP");
        b.label("L_SKIP");
        return b;
    }

    /** Block S — stash diagPrefs = v7 (between orig idx 194 and idx 195).
     *  Runs only on the A-key path (P1's body). 1 insn.
     */
    static B buildS() {
        B b = new B();
        b.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, R_PREFSTR, DIAG_PREFS));
        return b;
    }

    /**
     * Block W — "last_a" record write (inserted after P2 for SUB, after P3's
     * Log for GUARD). A-key only (explicit A-check). Record:
     *   "A cp=1f496 ctx=<diagCtxSrc> prefs=<diagPrefs> outcome=<outcome>"
     * Appending a null String appends the literal "null", so an unset
     * diagPrefs reads as "prefs=null" (early GUARD) and a set one reads as
     * the actual prefs value (late GUARD / SUB).
     */
    static B buildW(String outcome) {
        B b = new B();
        // A-check: only key A writes "last_a"
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, CP_A));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "W_SKIP");
        // ctx null? skip
        b.br21t(Opcode.IF_EQZ, R_CTX, "W_SKIP");
        // StringBuilder record into v8
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS,
            new ImmutableTypeReference("Ljava/lang/StringBuilder;")));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("A cp=1f496 ctx=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SGET_OBJECT, R_PARTS, DIAG_CTX_SRC));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PARTS, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(" prefs=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SGET_OBJECT, R_PARTS, DIAG_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PARTS, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(" outcome=" + outcome)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        // v0 = 0 (MODE_PRIVATE) for getSharedPreferences
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        // write ("last_a", v8)
        emitPrefsWrite(b, "last_a", "W_SKIP");
        b.label("W_SKIP");
        return b;
    }

    static int countIds(File f, int off) throws Exception {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
        try {
            raf.seek(off);
            int b0 = raf.read(); int b1 = raf.read(); int b2 = raf.read(); int b3 = raf.read();
            return (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        } finally { raf.close(); }
    }

    public static void main(String[] args) throws Exception {
        File inFile = new File(args[0]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(inFile, Opcodes.forApi(35));
        int inMethodIds = countIds(inFile, 88);
        int inFieldIds = countIds(inFile, 80);
        System.out.println("input method_ids_size=" + inMethodIds + " field_ids_size=" + inFieldIds);
        // ============ Load + verify Lpud.m (859: 387 insns, 16 regs) ============
        List<Instruction> orig = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lpud;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("m") && m.getParameters().size() == 3) {
                    MethodImplementation impl = m.getImplementation();
                    orig = new ArrayList<>();
                    for (Instruction i : impl.getInstructions()) orig.add(i);
                    if (orig.size() != 387 || impl.getRegisterCount() != 16) {
                        System.out.println("Lpud.m: unexpected shape ins=" + orig.size() + " regs=" + impl.getRegisterCount() + " (expected 387/16)");
                        return;
                    }
                    if (!impl.getTryBlocks().isEmpty()) { System.out.println("Lpud.m: unexpected try blocks"); return; }
                    // fingerprints
                    Instruction i0 = orig.get(0);
                    if (i0.getOpcode() != Opcode.CONST_4 || !(i0 instanceof Instruction11n)
                            || ((Instruction11n) i0).getRegisterA() != 2 || ((Instruction11n) i0).getNarrowLiteral() != -1) {
                        System.out.println("Lpud.m: insn0 not CONST_4 v2,-1 (got " + i0 + ")"); return;
                    }
                    if (orig.get(5).getOpcode() != Opcode.INVOKE_STATIC) { System.out.println("Lpud.m: insn5 not INVOKE_STATIC"); return; }
                    if (orig.get(31).getOpcode() != Opcode.ARRAY_LENGTH) { System.out.println("Lpud.m: insn31 not ARRAY_LENGTH"); return; }
                    if (orig.get(165).getOpcode() != Opcode.INVOKE_STATIC) { System.out.println("Lpud.m: insn165 not INVOKE_STATIC"); return; }
                    Instruction i177 = orig.get(177);
                    if (i177.getOpcode() != Opcode.CONST || !(i177 instanceof Instruction31i)
                            || ((Instruction31i) i177).getNarrowLiteral() != CP_A) {
                        System.out.println("Lpud.m: insn177 not CONST 0x1F496 (got " + i177 + ")"); return;
                    }
                    Instruction i294 = orig.get(294);
                    if (i294.getOpcode() != Opcode.CONST || !(i294 instanceof Instruction31i)
                            || ((Instruction31i) i294).getNarrowLiteral() != CP_A) {
                        System.out.println("Lpud.m: insn294 not CONST 0x1F496 (got " + i294 + ")"); return;
                    }
                    Instruction i305 = orig.get(305);
                    if (i305.getOpcode() != Opcode.CONST_4 || !(i305 instanceof Instruction11n)
                            || ((Instruction11n) i305).getRegisterA() != 3 || ((Instruction11n) i305).getNarrowLiteral() != 0) {
                        System.out.println("Lpud.m: insn305 not CONST_4 v3,0 (got " + i305 + ")"); return;
                    }
                    if (orig.get(371).getOpcode() != Opcode.RETURN_VOID) { System.out.println("Lpud.m: insn371 not RETURN_VOID"); return; }
                    Instruction i372 = orig.get(372);
                    if (i372.getOpcode() != Opcode.CONST || !(i372 instanceof Instruction31i)
                            || ((Instruction31i) i372).getNarrowLiteral() != CP_A) {
                        System.out.println("Lpud.m: insn372 not CONST 0x1F496 (got " + i372 + ")"); return;
                    }
                    if (orig.get(375).getOpcode() != Opcode.INVOKE_STATIC) { System.out.println("Lpud.m: insn375 not INVOKE_STATIC"); return; }
                    if (orig.get(386).getOpcode() != Opcode.GOTO_16) { System.out.println("Lpud.m: insn386 not GOTO_16"); return; }
                    System.out.println("Lpud.m: shape + fingerprints OK (387 insns, 16 regs, A CP=0x1F496)");
                }
            }
        }
        if (orig == null) { System.out.println("Lpud.m not found"); return; }
        // ============ Load + verify Lesb.onCreate (9 insns, 2 regs) ============
        List<Instruction> lesbOrig = null;
        String lesbSuper = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lesb;")) continue;
            lesbSuper = cd.getSuperclass();
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("onCreate") && m.getParameters().size() == 0) {
                    MethodImplementation impl = m.getImplementation();
                    lesbOrig = new ArrayList<>();
                    for (Instruction i : impl.getInstructions()) lesbOrig.add(i);
                    if (lesbOrig.size() != 9 || impl.getRegisterCount() != 2) {
                        System.out.println("Lesb.onCreate: unexpected shape ins=" + lesbOrig.size() + " regs=" + impl.getRegisterCount() + " (expected 9/2)");
                        return;
                    }
                    if (!impl.getTryBlocks().isEmpty()) { System.out.println("Lesb.onCreate: unexpected try blocks"); return; }
                    if (lesbOrig.get(0).getOpcode() != Opcode.IGET_BOOLEAN) { System.out.println("Lesb.onCreate: insn0 not IGET_BOOLEAN"); return; }
                    Instruction li1 = lesbOrig.get(1);
                    if (li1.getOpcode() != Opcode.IF_NEZ || !(li1 instanceof Instruction21t)
                            || ((Instruction21t) li1).getCodeOffset() != 11) {
                        System.out.println("Lesb.onCreate: insn1 not IF_NEZ +11 (got " + li1 + ")"); return;
                    }
                    if (lesbOrig.get(7).getOpcode() != Opcode.INVOKE_SUPER) { System.out.println("Lesb.onCreate: insn7 not INVOKE_SUPER"); return; }
                    if (lesbOrig.get(8).getOpcode() != Opcode.RETURN_VOID) { System.out.println("Lesb.onCreate: insn8 not RETURN_VOID"); return; }
                    System.out.println("Lesb.onCreate: shape + fingerprints OK (9 insns, 2 regs, super=" + lesbSuper + ", IF_NEZ +11 -> idx 7)");
                }
            }
        }
        if (lesbOrig == null) { System.out.println("Lesb.onCreate not found"); return; }
        // ============ Original addresses + branch targets ============
        int n = orig.size();
        int[] origAddr = new int[n];
        { int a = 0; for (int i = 0; i < n; i++) { origAddr[i] = a; a += orig.get(i).getCodeUnits(); } }
        int origTotal = origAddr[n - 1] + orig.get(n - 1).getCodeUnits();
        Map<Integer, Integer> addrToIdx = new HashMap<>();
        for (int i = 0; i < n; i++) addrToIdx.put(origAddr[i], i);
        int[] origTarget = new int[n];
        Arrays.fill(origTarget, -1);
        for (int i = 0; i < n; i++) {
            Instruction ins = orig.get(i);
            if (ins instanceof OffsetInstruction) {
                int tgt = origAddr[i] + ((OffsetInstruction) ins).getCodeOffset();
                Integer ti = addrToIdx.get(tgt);
                if (ti == null) { System.out.println("Lpud.m: branch at orig " + i + " targets invalid addr " + tgt); return; }
                origTarget[i] = ti;
            }
        }
        // GUARD branches = branches targeting orig 372 (P3 start). Expect exactly 11.
        List<Integer> guardSrc = new ArrayList<>();
        for (int i = 0; i < n; i++) if (origTarget[i] == 372) guardSrc.add(i);
        System.out.println("Lpud.m: GUARD branches (target orig 372): " + guardSrc);
        if (guardSrc.size() != 11) { System.out.println("Lpud.m: expected 11 GUARD branches, got " + guardSrc.size()); return; }
        int[] guardArr = {33, 162, 167, 172, 176, 200, 205, 210, 215, 220, 225};
        for (int g : guardArr) if (!guardSrc.contains(g)) { System.out.println("Lpud.m: missing expected GUARD branch " + g); return; }
        // Verify the other retarget targets.
        if (origTarget[2] != 30) { System.out.println("Lpud.m: idx 2 does not target 30 (got " + origTarget[2] + ")"); return; }
        if (origTarget[178] != 195) { System.out.println("Lpud.m: idx 178 does not target 195 (got " + origTarget[178] + ")"); return; }
        if (origTarget[295] != 304) { System.out.println("Lpud.m: idx 295 does not target 304 (got " + origTarget[295] + ")"); return; }
        if (origTarget[373] != 385) { System.out.println("Lpud.m: idx 373 does not target 385 (got " + origTarget[373] + ")"); return; }
        if (origTarget[386] != 305) { System.out.println("Lpud.m: idx 386 does not target 305 (got " + origTarget[386] + ")"); return; }
        System.out.println("Lpud.m: retarget targets verified (2->30, 178->195, 295->304, 373->385, 386->305, 11 GUARD->372)");
        // ============ Build blocks (fixup not yet called — need bases first) ============
        B e = buildE(); B l = buildL(); B s = buildS();
        B wSub = buildW("SUB OK"); B wGuard = buildW("GUARD");
        e.padEven(); l.padEven(); s.padEven(); wSub.padEven(); wGuard.padEven();
        int sE = e.totalUnits(), sL = l.totalUnits(), sS = s.totalUnits();
        int sWSub = wSub.totalUnits(), sWGuard = wGuard.totalUnits();
        System.out.println("block sizes (units): E=" + sE + " L=" + sL + " S=" + sS + " W_SUB=" + sWSub + " W_GUARD=" + sWGuard
            + " (insns E=" + e.code.size() + " L=" + l.code.size() + " S=" + s.code.size()
            + " W_SUB=" + wSub.code.size() + " W_GUARD=" + wGuard.code.size() + ")");
        // Bases (code-unit address where each block starts in the final method).
        int baseE = 0;
        int baseL = origAddr[5] + sE;
        int baseS = origAddr[195] + sE + sL;
        int baseWSub = origAddr[304] + sE + sL + sS;
        int baseWGuard = origAddr[385] + sE + sL + sS + sWSub;
        System.out.println("bases: E=" + baseE + " L=" + baseL + " S=" + baseS + " W_SUB=" + baseWSub + " W_GUARD=" + baseWGuard + " origTotal=" + origTotal);
        e.fixup(baseE); l.fixup(baseL); s.fixup(baseS); wSub.fixup(baseWSub); wGuard.fixup(baseWGuard);
        // ============ Assemble the final instruction list ============
        List<Instruction> all = new ArrayList<>();
        int[] origToFinal = new int[n];
        Arrays.fill(origToFinal, -1);
        // Indices replaced by same-unit NOPs (old currentApplication() fetches).
        Set<Integer> nopSite = new HashSet<>(Arrays.asList(5, 6, 165, 166, 375, 376));
        // E + orig 0..4
        all.addAll(e.code);
        for (int i = 0; i <= 4; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // L
        all.addAll(l.code);
        // orig 5..6 -> NOPs (INVOKE_STATIC 3 units -> 3 NOPs; MOVE_RESULT_OBJECT 1 unit -> 1 NOP)
        origToFinal[5] = all.size();
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        origToFinal[6] = all.size();
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        // orig 7..164
        for (int i = 7; i <= 164; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // orig 165..166 -> NOPs
        origToFinal[165] = all.size();
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        origToFinal[166] = all.size();
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        // orig 167..194
        for (int i = 167; i <= 194; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // S
        all.addAll(s.code);
        // orig 195..303
        for (int i = 195; i <= 303; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // W_SUB
        all.addAll(wSub.code);
        // orig 304..374
        for (int i = 304; i <= 374; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // orig 375..376 -> NOPs
        origToFinal[375] = all.size();
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        origToFinal[376] = all.size();
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        // orig 377..384
        for (int i = 377; i <= 384; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // W_GUARD
        all.addAll(wGuard.code);
        // orig 385..386
        for (int i = 385; i <= 386; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        for (int i = 0; i < n; i++) if (origToFinal[i] < 0) { System.out.println("origToFinal missing for orig " + i); return; }
        int m = all.size();
        int[] finAddr = new int[m];
        { int a = 0; for (int i = 0; i < m; i++) { finAddr[i] = a; a += all.get(i).getCodeUnits(); } }
        int finTotal = finAddr[m - 1] + all.get(m - 1).getCodeUnits();
        System.out.println("final Lpud.m: " + m + " insns, " + finTotal + " units (orig 387 insns, " + origTotal + " units)");
        // Sanity: block start addresses must equal the computed bases.
        if (finAddr[origToFinal[0]] != baseE + sE) { System.out.println("E end addr mismatch: " + finAddr[origToFinal[0]] + " != " + (baseE + sE)); return; }
        if (finAddr[origToFinal[5]] != baseL + sL) { System.out.println("L end addr mismatch: " + finAddr[origToFinal[5]] + " != " + (baseL + sL)); return; }
        if (finAddr[origToFinal[195]] != baseS + sS) { System.out.println("S end addr mismatch: " + finAddr[origToFinal[195]] + " != " + (baseS + sS)); return; }
        if (finAddr[origToFinal[304]] != baseWSub + sWSub) { System.out.println("W_SUB end addr mismatch: " + finAddr[origToFinal[304]] + " != " + (baseWSub + sWSub)); return; }
        if (finAddr[origToFinal[385]] != baseWGuard + sWGuard) { System.out.println("W_GUARD end addr mismatch: " + finAddr[origToFinal[385]] + " != " + (baseWGuard + sWGuard)); return; }
        // ============ Retarget the 16 branches ============
        Map<Integer, Integer> retarget = new HashMap<>();
        retarget.put(2, 30); retarget.put(178, 195); retarget.put(295, 304);
        retarget.put(373, 385); retarget.put(386, 305);
        for (int g : guardArr) retarget.put(g, 372);
        for (Map.Entry<Integer, Integer> rt : retarget.entrySet()) {
            int src = rt.getKey(), tgt = rt.getValue();
            int fi = origToFinal[src];
            Instruction ins = all.get(fi);
            int newOff = finAddr[origToFinal[tgt]] - finAddr[fi];
            if (newOff < -32768 || newOff > 32767) { System.out.println("retarget branch at orig " + src + " out of range: " + newOff); return; }
            if (ins instanceof Instruction21t) { Instruction21t o = (Instruction21t) ins; all.set(fi, new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), newOff)); }
            else if (ins instanceof Instruction22t) { Instruction22t o = (Instruction22t) ins; all.set(fi, new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(), o.getRegisterB(), newOff)); }
            else if (ins instanceof Instruction20t) { Instruction20t o = (Instruction20t) ins; all.set(fi, new ImmutableInstruction20t(o.getOpcode(), newOff)); }
            else { System.out.println("retarget branch at orig " + src + " is not 21t/22t/20t: " + ins.getOpcode()); return; }
        }
        System.out.println("retargeted " + retarget.size() + " original branches (2,178,295,373,386 + 11 GUARD)");
        // Verify every other original branch's offset is UNCHANGED.
        for (int i = 0; i < n; i++) {
            if (!(orig.get(i) instanceof OffsetInstruction)) continue;
            if (retarget.containsKey(i)) continue; // retargeted above (intentional)
            int fi = origToFinal[i];
            Instruction ins = all.get(fi);
            int oldOff = ((OffsetInstruction) orig.get(i)).getCodeOffset();
            int newOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                System.out.println("ORIGINAL branch offset CHANGED at orig " + i + " old=" + oldOff + " new=" + newOff + " — abort");
                return;
            }
        }
        System.out.println("all other original branch offsets unchanged (verified)");
        // ============ Build the new Lpud.m implementation ============
        MethodImplementation newImpl = null;
        MethodImplementation lesbOrigImpl = null;
        for (ClassDef cd : dex.getClasses()) {
            if (cd.getType().equals("Lpud;")) {
                for (Method mm : cd.getMethods()) {
                    if (mm.getName().equals("m") && mm.getParameters().size() == 3) {
                        newImpl = new ImmutableMethodImplementation(mm.getImplementation().getRegisterCount(),
                            all, mm.getImplementation().getTryBlocks(), mm.getImplementation().getDebugItems());
                    }
                }
            }
            if (cd.getType().equals("Lesb;")) {
                for (Method mm : cd.getMethods()) {
                    if (mm.getName().equals("onCreate") && mm.getParameters().size() == 0) {
                        lesbOrigImpl = mm.getImplementation();
                    }
                }
            }
        }
        if (newImpl == null) { System.out.println("failed to build new Lpud.m impl"); return; }
        if (lesbOrigImpl == null) { System.out.println("failed to find Lesb.onCreate impl"); return; }
        // ============ Build the new Lesb.onCreate implementation ============
        // Insert SPUT_OBJECT v1, Lpud->appCtx at idx 0 (2 units, even).
        List<Instruction> lesbNew = new ArrayList<>();
        lesbNew.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, 1, APP_CTX));
        for (Instruction i : lesbOrig) lesbNew.add(i);
        MethodImplementation lesbNewImpl = new ImmutableMethodImplementation(
            lesbOrigImpl.getRegisterCount(), lesbNew,
            lesbOrigImpl.getTryBlocks(), lesbOrigImpl.getDebugItems());
        // Verify: the IF_NEZ (now final idx 2) still targets idx 8 (orig 7).
        int[] lesbAddr = new int[lesbNew.size()];
        { int a = 0; for (int i = 0; i < lesbNew.size(); i++) { lesbAddr[i] = a; a += lesbNew.get(i).getCodeUnits(); } }
        Instruction lesbIfNez = lesbNew.get(2);
        if (!(lesbIfNez instanceof Instruction21t) || ((Instruction21t) lesbIfNez).getCodeOffset() != 11) {
            System.out.println("Lesb.onCreate: IF_NEZ offset changed after insert (got " + lesbIfNez + ")"); return;
        }
        System.out.println("Lesb.onCreate: new impl " + lesbNew.size() + " insns, IF_NEZ still +11 (targets final idx 8)");
        // ============ Module: rewrite Lpud.m + Lesb.onCreate + add 3 fields ============
        final MethodImplementation finImpl = newImpl;
        final MethodImplementation finLesbImpl = lesbNewImpl;
        Module mod = new Module(finImpl, finLesbImpl);
        DexRewriter rw = new DexRewriter(mod);
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes.dex written to " + args[1]);
        int outMethodIds = countIds(new File(args[1]), 88);
        int outFieldIds = countIds(new File(args[1]), 80);
        System.out.println("post-check: method_ids_size=" + outMethodIds + " (input was " + inMethodIds + ", expect +0 — no new method refs)");
        System.out.println("post-check: field_ids_size=" + outFieldIds + " (input was " + inFieldIds + ", expect +3 = appCtx + diagCtxSrc + diagPrefs)");
        if (outMethodIds != inMethodIds) throw new RuntimeException("method_ids changed (got " + outMethodIds + ", expected " + inMethodIds + ")");
        if (outFieldIds != inFieldIds + 3) throw new RuntimeException("field_ids did not grow by exactly 3 (got " + outFieldIds + ")");
    }

    static class Module extends RewriterModule {
        final MethodImplementation finImpl;
        final MethodImplementation finLesbImpl;
        Module(MethodImplementation fi, MethodImplementation fl) { finImpl = fi; finLesbImpl = fl; }
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    if (method.getDefiningClass().equals("Lpud;")
                            && method.getName().equals("m") && method.getParameters().size() == 3) {
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, finImpl);
                    }
                    if (method.getDefiningClass().equals("Lesb;")
                            && method.getName().equals("onCreate") && method.getParameters().size() == 0) {
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, finLesbImpl);
                    }
                    return def.rewrite(method);
                }
            };
        }
        @Override
        public Rewriter<ClassDef> getClassDefRewriter(final Rewriters rewriters) {
            final Rewriter<ClassDef> def = super.getClassDefRewriter(rewriters);
            return new Rewriter<ClassDef>() {
                public ClassDef rewrite(ClassDef classDef) {
                    ClassDef rw = def.rewrite(classDef); // applies method rewriter first
                    if (!rw.getType().equals("Lpud;")) return rw;
                    // add 3 static fields: appCtx (public static), diagCtxSrc, diagPrefs
                    List<Field> fields = new ArrayList<>();
                    for (Field f : rw.getFields()) fields.add(f);
                    fields.add(new ImmutableField("Lpud;", "appCtx", "Landroid/content/Context;",
                        0x1 | 0x8, null, null, null));
                    fields.add(new ImmutableField("Lpud;", "diagCtxSrc", "Ljava/lang/String;",
                        0x2 | 0x8, null, null, null));
                    fields.add(new ImmutableField("Lpud;", "diagPrefs", "Ljava/lang/String;",
                        0x2 | 0x8, null, null, null));
                    return new ImmutableClassDef(rw.getType(), rw.getAccessFlags(),
                        rw.getSuperclass(), rw.getInterfaces(), rw.getSourceFile(),
                        rw.getAnnotations(), fields, rw.getMethods());
                }
            };
        }
    }
}
