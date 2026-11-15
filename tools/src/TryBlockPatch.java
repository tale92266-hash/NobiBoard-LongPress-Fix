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
 * TRYBLOCK REWRITE (code 176071863) for NobiBoard def-parse customization.
 * Base = shipped 176071861. Change B ONLY (Sam's prompt) — the Lmxa.attachBaseContext
 * hook is KEPT (Change A was REFUTED by diagnosis-861; removing it restores the
 * 860 failure). Lmxa.attachBaseContext (5 insns) and Lesb.onCreate (10 insns) are
 * asserted 861-shaped and NOT modified.
 *
 * WHAT CHANGES (Lpud.m only, 16 regs kept — strategy (b), SAFE allocation):
 *   - diagCalls counter (new static int) incremented on EVERY call.
 *   - diagNullCtxCount counter (new static int) incremented when the Context
 *     is null; null-ctx goes straight to GUARD (NO one-shot lockout).
 *   - diagLastErr (new static String) set by the catch handler from the
 *     thrown exception's toString().
 *   - P0 Toast + P0 Log + diagFirst flag-check REMOVED (no Toasts at all in
 *     the new hook; last_run is the per-call diagnostic). diagFirst field
 *     stays in the class (dead, harmless).
 *   - last_run now "run ts=<ms> calls=<diagCalls> nullCtx=<diagNullCtxCount>".
 *   - split(raw, ",", -1) — 2-arg, trailing empties kept (was 1-arg).
 *   - Empty-part GUARDs replaced by per-slot validation: empty (or cp<=0)
 *     -> 0x200B in BOTH the [I and the [Object; (blank "\u200B" string).
 *   - P1/P2/P3 Toasts removed; their Log.e calls kept (A-key only).
 *   - One catch-all try block covers the ENTIRE hook [0, CLEANUP) -> GUARD.
 *   - The stock body (861 insns 382..443) is copied VERBATIM; clean-up
 *     (v3/v6/v7/v8/v11 = 0) and GUARD (last_a GUARD write) are re-emitted
 *     with the same semantics as 861. Substitution semantics (fingerprint,
 *     26-way chain, per-column slots, prefs format) are UNCHANGED.
 *
 * REGISTER DISCIPLINE (same as 858/861 — 16 regs, params v12..v15):
 *   v0 INT temp, v1 INT temp, v2 INT token cp (sentinel -1; persists to
 *   GUARD), v3 REF pref key, v4 INT colId, v5 REF Context (persists to
 *   GUARD), v6 REF prefs/builder/editor, v7 REF raw string, v8 REF
 *   parts/record string, v9 REF cloned [I, v10 REF cloned [Object;,
 *   v11 REF part string. v12 this, v13 [I, v14 [Lpuz;, v15 [Object;.
 *   NEVER write p0..p3 in the hook. Every MOVE_RESULT destination is a
 *   proven-dead scratch. Clean-up resets v3/v6/v7/v8/v11 to INT(0) exactly
 *   as 861 does, so the stock-body entry types match 861.
 */
public class TryBlockPatch {
    // ---------- registers (Lpud.m: 16 regs, params v12..v15) ----------
    static final int R_SLOT = 0;
    static final int R_TMP = 1;
    static final int R_CP = 2;
    static final int R_PREFKEY = 3;
    static final int R_COL = 4;
    static final int R_CTX = 5;
    static final int R_PREFS = 6;
    static final int R_PREFSTR = 7;
    static final int R_PARTS = 8;
    static final int R_CLONE_I = 9;
    static final int R_CLONE_O = 10;
    static final int R_PART = 11;
    static final int R_THIS = 12;
    static final int R_ARR_I = 13;
    static final int R_ARR_U = 14;
    static final int R_ARR_O = 15;

    // ---------- method refs (ALL already present in 861's classes.dex) ----------
    static final ImmutableMethodReference ACTIVITY_THREAD_APP =
        new ImmutableMethodReference("Landroid/app/ActivityThread;", "currentApplication",
            Collections.<String>emptyList(), "Landroid/app/Application;");
    static final ImmutableMethodReference CTX_GET_PREFS =
        new ImmutableMethodReference("Landroid/content/Context;", "getSharedPreferences",
            Arrays.asList("Ljava/lang/String;", "I"), "Landroid/content/SharedPreferences;");
    static final ImmutableMethodReference PREFS_GET_STRING =
        new ImmutableMethodReference("Landroid/content/SharedPreferences;", "getString",
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"), "Ljava/lang/String;");
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
    static final ImmutableMethodReference CURRENT_TIME_MILLIS =
        new ImmutableMethodReference("Ljava/lang/System;", "currentTimeMillis",
            Collections.<String>emptyList(), "J");
    static final ImmutableMethodReference SB_INIT =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "<init>",
            Collections.<String>emptyList(), "V");
    static final ImmutableMethodReference SB_APP_STR =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("Ljava/lang/String;"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_APP_LONG =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("J"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_APP_INT =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("I"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_TOSTRING =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "toString",
            Collections.<String>emptyList(), "Ljava/lang/String;");
    static final ImmutableMethodReference STRING_SPLIT2 =
        new ImmutableMethodReference("Ljava/lang/String;", "split",
            Arrays.asList("Ljava/lang/String;", "I"), "[Ljava/lang/String;");
    static final ImmutableMethodReference CS_LENGTH =
        new ImmutableMethodReference("Ljava/lang/CharSequence;", "length",
            Collections.<String>emptyList(), "I");
    static final ImmutableMethodReference CHAR_CODEPOINTAT =
        new ImmutableMethodReference("Ljava/lang/Character;", "codePointAt",
            Arrays.asList("Ljava/lang/CharSequence;", "I"), "I");
    static final ImmutableMethodReference ARRAYCOPY =
        new ImmutableMethodReference("Ljava/lang/System;", "arraycopy",
            Arrays.asList("Ljava/lang/Object;", "I", "Ljava/lang/Object;", "I", "I"), "V");
    static final ImmutableMethodReference LOG_E =
        new ImmutableMethodReference("Landroid/util/Log;", "e",
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"), "I");
    static final ImmutableMethodReference OBJECT_TOSTRING =
        new ImmutableMethodReference("Ljava/lang/Object;", "toString",
            Collections.<String>emptyList(), "Ljava/lang/String;");

    static final List<MethodReference> ALL_METHOD_REFS = Arrays.asList(
        ACTIVITY_THREAD_APP, CTX_GET_PREFS, PREFS_GET_STRING, SP_EDIT, SP_PUT_STRING,
        SP_APPLY, CURRENT_TIME_MILLIS, SB_INIT, SB_APP_STR, SB_APP_LONG, SB_APP_INT,
        SB_TOSTRING, STRING_SPLIT2, CS_LENGTH, CHAR_CODEPOINTAT, ARRAYCOPY, LOG_E,
        OBJECT_TOSTRING);

    // ---------- field refs ----------
    static final ImmutableFieldReference APP_CTX =
        new ImmutableFieldReference("Lpud;", "appCtx", "Landroid/content/Context;");
    static final ImmutableFieldReference DIAG_CTX_SRC =
        new ImmutableFieldReference("Lpud;", "diagCtxSrc", "Ljava/lang/String;");
    static final ImmutableFieldReference DIAG_PREFS =
        new ImmutableFieldReference("Lpud;", "diagPrefs", "Ljava/lang/String;");
    static final ImmutableFieldReference DIAG_CALLS =
        new ImmutableFieldReference("Lpud;", "diagCalls", "I");
    static final ImmutableFieldReference DIAG_NULLCTX =
        new ImmutableFieldReference("Lpud;", "diagNullCtxCount", "I");
    static final ImmutableFieldReference DIAG_LASTERR =
        new ImmutableFieldReference("Lpud;", "diagLastErr", "Ljava/lang/String;");

    static final ImmutableTypeReference T_INT_ARR = new ImmutableTypeReference("[I");
    static final ImmutableTypeReference T_OBJ_ARR = new ImmutableTypeReference("[Ljava/lang/Object;");
    static final ImmutableTypeReference T_SB = new ImmutableTypeReference("Ljava/lang/StringBuilder;");

    static final String TAG = "NobiDiag";
    static final String PREFS_NAME = "nobi_longpress_emoji";
    static final String DIAG_PREFS_NAME = "nobi_diag";
    static final String BLANK_STR = "\u200B";

    // ---------- label-based block builder (from DefParsePatch) ----------
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
        void fixup(int base) {
            if (totalUnits() % 2 != 0) code.add(new ImmutableInstruction10x(Opcode.NOP));
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
                    throw new RuntimeException("10t branch out of range at block idx " + f.idx + " -> " + f.target + " off=" + off + " (use GOTO_16)");
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

    static String methodKey(MethodReference r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getDefiningClass()).append("->").append(r.getName()).append("(");
        for (CharSequence p : r.getParameterTypes()) sb.append(p);
        sb.append(")").append(r.getReturnType());
        return sb.toString();
    }

    static int countIds(File f, int off) throws Exception {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
        try {
            raf.seek(off);
            int b0 = raf.read(); int b1 = raf.read(); int b2 = raf.read(); int b3 = raf.read();
            return (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        } finally { raf.close(); }
    }

    static int[] computeAddrs(List<Instruction> ins) {
        int[] a = new int[ins.size()];
        int x = 0;
        for (int i = 0; i < ins.size(); i++) { a[i] = x; x += ins.get(i).getCodeUnits(); }
        return a;
    }

    /** One substitution with per-slot validation (empty or cp<=0 -> 0x200B). */
    static void subSlotValid(B b, int partsIdx, int c0, int c1, int c2) {
        String tag = "S" + partsIdx;
        if (c0 == c1 && c1 == c2) {
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
        } else if (c1 == c2) {
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c1));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
            b.br22t(Opcode.IF_NE, R_COL, R_TMP, tag + "done");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.label(tag + "done");
        } else if (c0 == c2) {
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
            b.br22t(Opcode.IF_NE, R_COL, R_TMP, tag + "done");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c1));
            b.label(tag + "done");
        } else if (c0 == c1) {
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 2));
            b.br22t(Opcode.IF_NE, R_COL, R_TMP, tag + "done");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c2));
            b.label(tag + "done");
        } else {
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c2));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
            b.br22t(Opcode.IF_EQ, R_COL, R_TMP, tag + "c0");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
            b.br22t(Opcode.IF_EQ, R_COL, R_TMP, tag + "c1");
            b.br20t(Opcode.GOTO_16, tag + "done");
            b.label(tag + "c0");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.br20t(Opcode.GOTO_16, tag + "done");
            b.label(tag + "c1");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c1));
            b.label(tag + "done");
        }
        // part string -> v11; validated code point -> v1; APUT into clones.
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, partsIdx));
        b.add(new ImmutableInstruction23x(Opcode.AGET_OBJECT, R_PART, R_PARTS, R_TMP));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 1, R_PART, 0, 0, 0, 0, CS_LENGTH));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
        b.br21t(Opcode.IF_EQZ, R_TMP, tag + "empty");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, R_PART, R_TMP, 0, 0, 0, CHAR_CODEPOINTAT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
        b.br21t(Opcode.IF_LEZ, R_TMP, tag + "empty");
        b.br20t(Opcode.GOTO_16, tag + "aput");
        b.label(tag + "empty");
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_TMP, 0x200B));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(BLANK_STR)));
        b.label(tag + "aput");
        b.add(new ImmutableInstruction23x(Opcode.APUT, R_TMP, R_CLONE_I, R_SLOT));
        b.add(new ImmutableInstruction23x(Opcode.APUT_OBJECT, R_PART, R_CLONE_O, R_SLOT));
    }

    /** Emit the last_a write (outcomeLabel = "SUB OK" or "GUARD") for A keys. */
    static void emitLastA(B b, String outcome) {
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS, T_SB));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("A cp=1f496 ctx=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SGET_OBJECT, R_PARTS, DIAG_CTX_SRC));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PARTS, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(" prefs=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SGET_OBJECT, R_PARTS, DIAG_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PARTS, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(" outcome=" + outcome)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(DIAG_PREFS_NAME)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_CTX, R_PART, R_SLOT, 0, 0, CTX_GET_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.br21t(Opcode.IF_EQZ, R_PREFS, "GUARD_END");
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_EDIT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("last_a")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_PART, R_PARTS, 0, 0, SP_PUT_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_APPLY));
    }

    public static void main(String[] args) throws Exception {
        File inFile = new File(args[0]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(inFile, Opcodes.forApi(35));
        int inMethodIds = countIds(inFile, 88);
        int inFieldIds = countIds(inFile, 80);
        System.out.println("input method_ids_size=" + inMethodIds + " field_ids_size=" + inFieldIds);

        // ============ Load + verify Lpud.m (861: 494 insns, 16 regs, 0 tries) ============
        List<Instruction> orig = null;
        MethodImplementation origImpl = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lpud;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("m") && m.getParameters().size() == 3) {
                    origImpl = m.getImplementation();
                    orig = new ArrayList<>();
                    for (Instruction i : origImpl.getInstructions()) orig.add(i);
                    if (orig.size() != 494 || origImpl.getRegisterCount() != 16) {
                        System.out.println("Lpud.m: unexpected shape ins=" + orig.size() + " regs=" + origImpl.getRegisterCount() + " (expected 494/16)"); return;
                    }
                    if (!origImpl.getTryBlocks().isEmpty()) { System.out.println("Lpud.m: unexpected try blocks"); return; }
                }
            }
        }
        if (orig == null) { System.out.println("Lpud.m not found"); return; }
        int n = orig.size();
        int[] origAddr = computeAddrs(orig);
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
        // Structural anchors (ToastFixPatch-verified against shipped 861).
        if (orig.get(49).getOpcode() != Opcode.ARRAY_LENGTH) { System.out.println("anchor: insn 49 not ARRAY_LENGTH"); return; }
        if (orig.get(67).getOpcode() != Opcode.ARRAY_LENGTH) { System.out.println("anchor: insn 67 not ARRAY_LENGTH"); return; }
        if (orig.get(377).getOpcode() != Opcode.CONST_4) { System.out.println("anchor: insn 377 not CONST_4 (clean-up start)"); return; }
        if (orig.get(382).getOpcode() != Opcode.CONST_4) { System.out.println("anchor: insn 382 not CONST_4 (stock body start)"); return; }
        if (orig.get(443).getOpcode() != Opcode.RETURN_VOID) { System.out.println("anchor: insn 443 not RETURN_VOID (stock body end)"); return; }
        if (orig.get(444).getOpcode() != Opcode.CONST || !(orig.get(444) instanceof Instruction31i) || ((Instruction31i) orig.get(444)).getNarrowLiteral() != 0x1F496) {
            System.out.println("anchor: insn 444 not CONST 0x1F496 (GUARD start)"); return;
        }
        if (origTarget[493] != 377) { System.out.println("anchor: insn 493 GOTO_16 does not target clean-up 377 (got " + origTarget[493] + ")"); return; }
        List<Integer> guardSrc = new ArrayList<>();
        for (int i = 0; i < n; i++) if (origTarget[i] == 444) guardSrc.add(i);
        System.out.println("GUARD branches (target 444): " + guardSrc.size() + " " + guardSrc);
        if (guardSrc.size() != 11) { System.out.println("anchor: expected 11 GUARD branches, got " + guardSrc.size()); return; }
        int logE = 0, toastInv = 0, nobiDiag = 0, lastRun = 0, lastA = 0;
        for (Instruction ins : orig) {
            if (ins instanceof ReferenceInstruction) {
                Reference r = ((ReferenceInstruction) ins).getReference();
                if (r instanceof MethodReference) {
                    MethodReference mr = (MethodReference) r;
                    if (mr.getDefiningClass().equals("Landroid/util/Log;") && mr.getName().equals("e")) logE++;
                    if (mr.getDefiningClass().equals("Landroid/widget/Toast;")) toastInv++;
                }
                if (r instanceof StringReference) {
                    String s = ((StringReference) r).getString();
                    if (s.equals("nobi_diag")) nobiDiag++;
                    if (s.equals("last_run")) lastRun++;
                    if (s.equals("last_a")) lastA++;
                }
            }
        }
        System.out.println("Log.e=" + logE + " (expect 4); Toast=" + toastInv + " (expect 8); nobi_diag=" + nobiDiag + " (3); last_run=" + lastRun + " (1); last_a=" + lastA + " (2)");
        if (logE != 4 || toastInv != 8 || nobiDiag != 3 || lastRun != 1 || lastA != 2) { System.out.println("anchor: counts wrong"); return; }
        // STOCK range [382..443] must be branch-self-contained (no branch targets outside it).
        for (int i = 382; i <= 443; i++) {
            if (origTarget[i] >= 0 && (origTarget[i] < 382 || origTarget[i] > 443)) {
                System.out.println("STOCK branch at " + i + " targets outside [382..443] (" + origTarget[i] + ") — abort"); return;
            }
        }
        System.out.println("Lpud.m: shape + anchors + STOCK self-containment OK (494 insns, 16 regs)");

        // ============ Verify Lmxa.attachBaseContext + Lesb.onCreate are 861-shaped (NOT modified) ============
        for (ClassDef cd : dex.getClasses()) {
            if (cd.getType().equals("Lmxa;")) {
                for (Method m : cd.getMethods()) {
                    if (m.getName().equals("attachBaseContext") && m.getParameters().size() == 1) {
                        List<Instruction> li = new ArrayList<>();
                        for (Instruction i : m.getImplementation().getInstructions()) li.add(i);
                        if (li.size() != 5 || li.get(0).getOpcode() != Opcode.SPUT_OBJECT || li.get(4).getOpcode() != Opcode.RETURN_VOID) {
                            System.out.println("Lmxa.attachBaseContext: unexpected shape (expected 861's 5 insns, SPUT at 0)"); return;
                        }
                        if (!m.getImplementation().getTryBlocks().isEmpty()) { System.out.println("Lmxa.attachBaseContext: unexpected try blocks"); return; }
                        System.out.println("Lmxa.attachBaseContext: OK (5 insns, hook at idx 0) — KEPT, NOT modified");
                    }
                }
            }
            if (cd.getType().equals("Lesb;")) {
                for (Method m : cd.getMethods()) {
                    if (m.getName().equals("onCreate") && m.getParameters().size() == 0) {
                        List<Instruction> li = new ArrayList<>();
                        for (Instruction i : m.getImplementation().getInstructions()) li.add(i);
                        if (li.size() != 10 || li.get(0).getOpcode() != Opcode.SPUT_OBJECT || li.get(9).getOpcode() != Opcode.RETURN_VOID) {
                            System.out.println("Lesb.onCreate: unexpected shape (expected 861's 10 insns, SPUT at 0)"); return;
                        }
                        System.out.println("Lesb.onCreate: OK (10 insns, hook at idx 0) — KEPT, NOT modified");
                    }
                }
            }
        }

        // ============ Pre-check: every method ref the new hook uses is already in the dex ============
        Set<String> mrefs = new HashSet<>();
        for (ClassDef cd : dex.getClasses()) {
            for (Method m : cd.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    if (ins instanceof ReferenceInstruction) {
                        Reference r = ((ReferenceInstruction) ins).getReference();
                        if (r instanceof MethodReference) mrefs.add(methodKey((MethodReference) r));
                    }
                }
            }
        }
        for (MethodReference r : ALL_METHOD_REFS) {
            if (!mrefs.contains(methodKey(r))) { System.out.println("MISSING method ref: " + methodKey(r) + " — abort"); return; }
        }
        System.out.println("pre-check: all " + ALL_METHOD_REFS.size() + " method refs already present (no new method refs)");

        // ============ Build the new Lpud.m (ONE B block, labels resolved by fixup) ============
        B b = new B();
        // ---- HEAD: reset diagPrefs; diagCalls++; Block E; const v2,-1; null-ctx -> NULLCTX ----
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, R_SLOT, DIAG_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SGET, R_SLOT, DIAG_CALLS));
        b.add(new ImmutableInstruction22b(Opcode.ADD_INT_LIT8, R_SLOT, R_SLOT, 1));
        b.add(new ImmutableInstruction21c(Opcode.SPUT, R_SLOT, DIAG_CALLS));
        b.add(new ImmutableInstruction21c(Opcode.SGET_OBJECT, R_CTX, APP_CTX));
        b.br21t(Opcode.IF_NEZ, R_CTX, "L_FALLBACK");
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, ACTIVITY_THREAD_APP));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_CTX));
        b.br21t(Opcode.IF_EQZ, R_CTX, "L_NULLSRC");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("fallback")));
        b.br20t(Opcode.GOTO_16, "L_SETSRC");
        b.label("L_FALLBACK");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("static")));
        b.br20t(Opcode.GOTO_16, "L_SETSRC");
        b.label("L_NULLSRC");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("null")));
        b.label("L_SETSRC");
        b.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, R_PART, DIAG_CTX_SRC));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_CP, -1));
        b.br21t(Opcode.IF_EQZ, R_CTX, "L_NULLCTX");
        // ---- BLOCK_L: last_run = "run ts=<ms> calls=<diagCalls> nullCtx=<diagNullCtxCount>" ----
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS, T_SB));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("run ts=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, CURRENT_TIME_MILLIS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_WIDE, R_SLOT));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_SLOT, R_TMP, 0, 0, SB_APP_LONG));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(" calls=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SGET, R_SLOT, DIAG_CALLS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_SLOT, 0, 0, 0, SB_APP_INT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(" nullCtx=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SGET, R_SLOT, DIAG_NULLCTX));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_SLOT, 0, 0, 0, SB_APP_INT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(DIAG_PREFS_NAME)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_CTX, R_PART, R_SLOT, 0, 0, CTX_GET_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.br21t(Opcode.IF_EQZ, R_PREFS, "L_FP");
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_EDIT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("last_run")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_PART, R_PARTS, 0, 0, SP_PUT_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_APPLY));
        // ---- FINGERPRINT + 26-way chain ----
        b.label("L_FP");
        b.add(new ImmutableInstruction12x(Opcode.ARRAY_LENGTH, R_TMP, R_ARR_I));
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_SLOT, 9));
        b.br22t(Opcode.IF_NE, R_TMP, R_SLOT, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 3));
        b.add(new ImmutableInstruction23x(Opcode.AGET, R_CP, R_ARR_I, R_SLOT));
        for (int i = 0; i < 26; i++) {
            b.label("N" + i);
            b.add(new ImmutableInstruction31i(Opcode.CONST, R_TMP, Phase3Tables.LOOKUP_CP[i]));
            b.br22t(Opcode.IF_NE, R_CP, R_TMP, (i == 25) ? "GUARD" : ("N" + (i + 1)));
            b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PREFKEY,
                new ImmutableStringReference("emoji_" + Character.toUpperCase(Phase3Tables.LETTERS.charAt(i)))));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_COL, Phase3Tables.LOOKUP_COL[i]));
            if (i < 25) b.br20t(Opcode.GOTO_16, "PREFS");
        }
        // ---- PREFS read ----
        b.label("PREFS");
        b.br21t(Opcode.IF_EQZ, R_CTX, "GUARD");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(PREFS_NAME)));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_CTX, R_PART, R_TMP, 0, 0, CTX_GET_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.br21t(Opcode.IF_EQZ, R_PREFS, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_PREFKEY, R_TMP, 0, 0, PREFS_GET_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFSTR));
        b.br21t(Opcode.IF_EQZ, R_PREFSTR, "GUARD");
        b.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, R_PREFSTR, DIAG_PREFS));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 1, R_PREFSTR, 0, 0, 0, 0, CS_LENGTH));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
        b.br21t(Opcode.IF_EQZ, R_TMP, "GUARD");
        // ---- P1: A-only Log.e "A: prefs=<raw>" (NO Toast) ----
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, 0x1F496));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "SPLIT");
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS, T_SB));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("A: prefs=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PREFSTR, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(TAG)));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, R_PART, R_PARTS, 0, 0, 0, LOG_E));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
        // ---- SPLIT with -1 ----
        b.label("SPLIT");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(",")));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, -1));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFSTR, R_PART, R_TMP, 0, 0, STRING_SPLIT2));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction12x(Opcode.ARRAY_LENGTH, R_TMP, R_PARTS));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 5));
        b.br22t(Opcode.IF_NE, R_TMP, R_SLOT, "GUARD");
        // ---- CLONE arrays ----
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_TMP, 9));
        b.add(new ImmutableInstruction22c(Opcode.NEW_ARRAY, R_CLONE_I, R_TMP, T_INT_ARR));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 5, R_ARR_I, R_SLOT, R_CLONE_I, R_SLOT, R_TMP, ARRAYCOPY));
        b.add(new ImmutableInstruction22c(Opcode.NEW_ARRAY, R_CLONE_O, R_TMP, T_OBJ_ARR));
        b.add(inv35(Opcode.INVOKE_STATIC, 5, R_ARR_O, R_SLOT, R_CLONE_O, R_SLOT, R_TMP, ARRAYCOPY));
        // ---- 5 substitutions with per-slot validation ----
        subSlotValid(b, 0, 3, 4, 4);
        subSlotValid(b, 1, 6, 7, 7);
        subSlotValid(b, 2, 0, 1, 1);
        subSlotValid(b, 3, 5, 3, 5);
        subSlotValid(b, 4, 4, 5, 3);
        // ---- COMMIT ----
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, R_ARR_I, R_CLONE_I));
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, R_ARR_O, R_CLONE_O));
        // ---- P2: A-only Log.e "A: SUB OK" (NO Toast) ----
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, 0x1F496));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "WSUB");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("A: SUB OK")));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PARTS, new ImmutableStringReference(TAG)));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, R_PARTS, R_PART, 0, 0, 0, LOG_E));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
        // ---- WSUB: last_a "SUB OK" ----
        b.label("WSUB");
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, 0x1F496));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "CLEANUP");
        b.br21t(Opcode.IF_EQZ, R_CTX, "CLEANUP");
        emitLastA(b, "SUB OK");
        // ---- CLEANUP (same 5 resets as 861) ----
        b.label("CLEANUP");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PREFKEY, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PREFS, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PREFSTR, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PARTS, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PART, 0));
        // ---- STOCK body (861 insns 382..443, verbatim; internal branches preserved) ----
        for (int i = 382; i <= 443; i++) b.add(orig.get(i));
        // ---- NULLCTX: nullCtx++ then GUARD ----
        b.label("L_NULLCTX");
        b.add(new ImmutableInstruction21c(Opcode.SGET, R_SLOT, DIAG_NULLCTX));
        b.add(new ImmutableInstruction22b(Opcode.ADD_INT_LIT8, R_SLOT, R_SLOT, 1));
        b.add(new ImmutableInstruction21c(Opcode.SPUT, R_SLOT, DIAG_NULLCTX));
        b.br20t(Opcode.GOTO_16, "GUARD");
        // ---- GUARD: A-only Log.e "A: GUARD" + last_a "GUARD" ----
        b.label("GUARD");
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, 0x1F496));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "GUARD_END");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference("A: GUARD")));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PARTS, new ImmutableStringReference(TAG)));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, R_PARTS, R_PART, 0, 0, 0, LOG_E));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
        b.br21t(Opcode.IF_EQZ, R_CTX, "GUARD_END");
        emitLastA(b, "GUARD");
        b.label("GUARD_END");
        b.br20t(Opcode.GOTO_16, "CLEANUP");
        // ---- HANDLER: catch-all -> GUARD ----
        b.label("HANDLER");
        b.add(new ImmutableInstruction11x(Opcode.MOVE_EXCEPTION, R_SLOT));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_SLOT, 0, 0, 0, 0, OBJECT_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, R_PREFS, DIAG_LASTERR));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PREFS, 0));
        b.br20t(Opcode.GOTO_16, "GUARD");

        // ============ Fixup + try block ============
        b.fixup(0);
        int[] newAddr = computeAddrs(b.code);
        int newTotal = newAddr[b.code.size() - 1] + b.code.get(b.code.size() - 1).getCodeUnits();
        int cleanupIdx = b.labels.get("CLEANUP");
        int handlerIdx = b.labels.get("HANDLER");
        int guardIdx = b.labels.get("GUARD");
        int stockStartIdx = cleanupIdx + 5; // clean-up is 5 insns
        System.out.println("new Lpud.m: " + b.code.size() + " insns, " + newTotal + " units");
        System.out.println("labels: CLEANUP idx=" + cleanupIdx + " addr=" + newAddr[cleanupIdx]
            + "; GUARD idx=" + guardIdx + " addr=" + newAddr[guardIdx]
            + "; HANDLER idx=" + handlerIdx + " addr=" + newAddr[handlerIdx]
            + "; STOCK idx=" + stockStartIdx + " addr=" + newAddr[stockStartIdx]);
        if (newTotal % 2 != 0) { System.out.println("new total units ODD: " + newTotal + " — abort"); return; }
        // Verify the STOCK copy's internal branch offsets are unchanged.
        for (int i = 382; i <= 443; i++) {
            if (!(orig.get(i) instanceof OffsetInstruction)) continue;
            int ni = stockStartIdx + (i - 382);
            Instruction ins = b.code.get(ni);
            int oldOff = ((OffsetInstruction) orig.get(i)).getCodeOffset();
            int newOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                System.out.println("STOCK branch offset CHANGED at orig " + i + " old=" + oldOff + " new=" + newOff + " — abort");
                return;
            }
        }
        System.out.println("STOCK internal branch offsets unchanged (verified)");
        // Verify NO Toast invoke remains and counts are as expected.
        int toastAfter = 0, logAfter = 0, diagAfter = 0, lastRunAfter = 0, lastAAfter = 0;
        for (Instruction ins : b.code) {
            if (ins instanceof ReferenceInstruction) {
                Reference r = ((ReferenceInstruction) ins).getReference();
                if (r instanceof MethodReference) {
                    MethodReference mr = (MethodReference) r;
                    if (mr.getDefiningClass().equals("Landroid/widget/Toast;")) toastAfter++;
                    if (mr.getDefiningClass().equals("Landroid/util/Log;") && mr.getName().equals("e")) logAfter++;
                }
                if (r instanceof StringReference) {
                    String s = ((StringReference) r).getString();
                    if (s.equals("nobi_diag")) diagAfter++;
                    if (s.equals("last_run")) lastRunAfter++;
                    if (s.equals("last_a")) lastAAfter++;
                }
            }
        }
        System.out.println("new Lpud.m: Toast=" + toastAfter + " (expect 0); Log.e=" + logAfter + " (expect 3); nobi_diag=" + diagAfter + " (expect 3); last_run=" + lastRunAfter + " (expect 1); last_a=" + lastAAfter + " (expect 2)");
        if (toastAfter != 0 || logAfter != 3 || diagAfter != 3 || lastRunAfter != 1 || lastAAfter != 2) {
            System.out.println("new-method counts wrong — abort"); return;
        }
        // Try block: catch-all covering [0, CLEANUP) -> HANDLER.
        List<ImmutableTryBlock> tryBlocks = new ArrayList<>();
        int tryStartAddr = 0;
        int tryEndAddr = newAddr[cleanupIdx];
        int handlerAddr = newAddr[handlerIdx];
        tryBlocks.add(new ImmutableTryBlock(tryStartAddr, tryEndAddr - tryStartAddr,
            Collections.singletonList(new ImmutableExceptionHandler(null, handlerAddr))));
        System.out.println("try/catch: region [" + tryStartAddr + ".." + tryEndAddr + "), handler at addr " + handlerAddr + " -> GUARD (idx " + guardIdx + ")");
        if (tryEndAddr <= tryStartAddr) { System.out.println("try region empty — abort"); return; }

        // ============ Build new implementation + rewrite Lpud.m + add 3 fields ============
        MethodImplementation newImpl = new ImmutableMethodImplementation(
            origImpl.getRegisterCount(), b.code, tryBlocks, origImpl.getDebugItems());
        final MethodImplementation finImpl = newImpl;
        Module mod = new Module(finImpl);
        DexRewriter rw = new DexRewriter(mod);
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes.dex written to " + args[1]);
        int outMethodIds = countIds(new File(args[1]), 88);
        int outFieldIds = countIds(new File(args[1]), 80);
        System.out.println("post-check: method_ids_size=" + outMethodIds + " (input " + inMethodIds + ", expect -2 = Toast.makeText + Toast.show dropped)");
        System.out.println("post-check: field_ids_size=" + outFieldIds + " (input " + inFieldIds + ", expect +3 = diagCalls + diagNullCtxCount + diagLastErr)");
        if (outMethodIds != inMethodIds - 2) throw new RuntimeException("method_ids changed unexpectedly (got " + outMethodIds + ", expected " + (inMethodIds - 2) + ")");
        if (outFieldIds != inFieldIds + 3) throw new RuntimeException("field_ids did not grow by exactly 3 (got " + outFieldIds + ")");
    }

    static class Module extends RewriterModule {
        final MethodImplementation finImpl;
        Module(MethodImplementation f) { finImpl = f; }
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
                    return def.rewrite(method);
                }
            };
        }
        @Override
        public Rewriter<ClassDef> getClassDefRewriter(final Rewriters rewriters) {
            final Rewriter<ClassDef> def = super.getClassDefRewriter(rewriters);
            return new Rewriter<ClassDef>() {
                public ClassDef rewrite(ClassDef classDef) {
                    ClassDef rw = def.rewrite(classDef);
                    if (!rw.getType().equals("Lpud;")) return rw;
                    List<Field> fields = new ArrayList<>();
                    for (Field f : rw.getFields()) fields.add(f);
                    fields.add(new ImmutableField("Lpud;", "diagCalls", "I",
                        0x2 | 0x8, null, null, null));
                    fields.add(new ImmutableField("Lpud;", "diagNullCtxCount", "I",
                        0x2 | 0x8, null, null, null));
                    fields.add(new ImmutableField("Lpud;", "diagLastErr", "Ljava/lang/String;",
                        0x2 | 0x8, null, null, null));
                    return new ImmutableClassDef(rw.getType(), rw.getAccessFlags(),
                        rw.getSuperclass(), rw.getInterfaces(), rw.getSourceFile(),
                        rw.getAnnotations(), fields, rw.getMethods());
                }
            };
        }
    }
}
