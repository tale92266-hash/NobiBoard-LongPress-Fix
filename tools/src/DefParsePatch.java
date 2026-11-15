import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.rewriter.*;
import java.io.File;
import java.util.*;

/**
 * Def-parse customization (code 176071858) — bring back Phase 3 customization
 * WITHOUT touching ANY long-press code.
 *
 * DESIGN: hook Lpud.m([I,[Lpuz;,[Object;)V (classes.dex), the method that
 * converts parsed token arrays into the Lpva list for an ActionDef. It is
 * called by Lpwz.f (softkey template expansion = the fd2 def-parse path),
 * Lpud.e (ActionDef XML parser) and Lpht.a. The hook runs at KEYBOARD-LOAD
 * time (def parse), long before any popup is built. It substitutes custom
 * emoji code points + labels into CLONED copies of the [I and [Object;
 * arrays; the stock loop then builds Lpva items from the substituted arrays.
 * The long-press popup code (Lqml.c, Lqmu.c/o/i/k/e) is NEVER touched and
 * runs its exact Fix11/856 code on already-custom ActionDefs.
 *
 * INSERTION POINT: Lpud.m, insn 0 (before the original first instruction).
 *   - 62 insns, 16 regs, 0 tries, 0 payloads (verified).
 *   - Params: v12=this(Lpud), v13=[I, v14=[Lpuz;, v15=[Object;.
 *   - Locals v0-v11 are DEAD at method entry (written before read on every
 *     path; RegInitCheck=0 on the control), so the hook uses them as scratch
 *     WITHOUT growing the register count and WITHOUT remapping.
 *   - Inserting at insn 0 shifts EVERY original insn by +grow, so every
 *     original branch offset is UNCHANGED (source and target shift equally).
 *     NO original branch retargeting is needed (verified by re-dump).
 *   - GUARD = fall-through to original insn 0 with v13/v15 UNCHANGED.
 *     SUCCESS = store clones into v13/v15, fall through to insn 0.
 *     Both paths converge on the stock loop, which builds Lpva items.
 *
 * REGISTER DISCIPLINE (v13 colId clobber lesson — diagnosis-857):
 *   - v0: INT temp (slot index / const temp)
 *   - v1: INT temp (length / code point / const temp)
 *   - v2: INT center code point (fingerprint)
 *   - v3: REF pref key string (set by chain)
 *   - v4: INT colId (set by chain)
 *   - v5: REF Context (Application)
 *   - v6: REF SharedPreferences
 *   - v7: REF prefs value string
 *   - v8: REF split String[] parts
 *   - v9: REF cloned int[]
 *   - v10: REF cloned Object[]
 *   - v11: REF custom part string
 *   Each scratch holds ONE KIND for the whole block. v12 (this) and v14
 *   ([Lpuz;) are NEVER written. v13/v15 are written ONLY at finalize
 *   (on substitution success) — never by MOVE_RESULT. Every MOVE_RESULT
 *   destination is a proven-dead scratch (v1, v5, v6, v7, v8).
 *
 * CONTEXT: ActivityThread.currentApplication() (ONE new method ref in
 * classes.dex, which has 8,020 free method_ids). Null-guarded -> GUARD.
 *
 * EMPTY-PART GUARD (diagnosis-857 Candidate 1): after split gives 5 parts,
 * every part's length() must be > 0, else GUARD. codePointAt on "" throws.
 */
public class DefParsePatch {
    // ---------- registers (Lpud.m: 16 regs, params v12..v15) ----------
    static final int R_SLOT = 0;    // INT: slot index temp
    static final int R_TMP = 1;     // INT: length / code point / const temp
    static final int R_CP = 2;      // INT: center code point (fingerprint)
    static final int R_PREFKEY = 3; // REF: pref key string ("emoji_X")
    static final int R_COL = 4;     // INT: colId (0=center,1=left,2=right)
    static final int R_CTX = 5;     // REF: Context (Application)
    static final int R_PREFS = 6;   // REF: SharedPreferences
    static final int R_PREFSTR = 7; // REF: prefs value string
    static final int R_PARTS = 8;   // REF: split String[] parts
    static final int R_CLONE_I = 9; // REF: cloned int[]
    static final int R_CLONE_O = 10;// REF: cloned Object[]
    static final int R_PART = 11;   // REF: custom part string
    static final int R_THIS = 12;   // Lpud (param, read-only)
    static final int R_ARR_I = 13;  // [I code points (param; replaced on success)
    static final int R_ARR_U = 14;  // [Lpuz; (param, read-only)
    static final int R_ARR_O = 15;  // [Object; labels (param; replaced on success)

    // ---------- method refs ----------
    static final ImmutableMethodReference ACTIVITY_THREAD_APP =
        new ImmutableMethodReference("Landroid/app/ActivityThread;", "currentApplication",
            Collections.<String>emptyList(), "Landroid/app/Application;");
    static final ImmutableMethodReference CTX_GET_PREFS =
        new ImmutableMethodReference("Landroid/content/Context;", "getSharedPreferences",
            Arrays.asList("Ljava/lang/String;", "I"), "Landroid/content/SharedPreferences;");
    static final ImmutableMethodReference PREFS_GET_STRING =
        new ImmutableMethodReference("Landroid/content/SharedPreferences;", "getString",
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"), "Ljava/lang/String;");
    static final ImmutableMethodReference STRING_SPLIT =
        new ImmutableMethodReference("Ljava/lang/String;", "split",
            Arrays.asList("Ljava/lang/String;"), "[Ljava/lang/String;");
    static final ImmutableMethodReference CHAR_CODEPOINTAT =
        new ImmutableMethodReference("Ljava/lang/Character;", "codePointAt",
            Arrays.asList("Ljava/lang/CharSequence;", "I"), "I");
    static final ImmutableMethodReference ARRAYCOPY =
        new ImmutableMethodReference("Ljava/lang/System;", "arraycopy",
            Arrays.asList("Ljava/lang/Object;", "I", "Ljava/lang/Object;", "I", "I"), "V");
    static final ImmutableMethodReference CS_LENGTH =
        new ImmutableMethodReference("Ljava/lang/CharSequence;", "length",
            Collections.<String>emptyList(), "I");

    static final ImmutableTypeReference T_INT_ARR = new ImmutableTypeReference("[I");
    static final ImmutableTypeReference T_OBJ_ARR = new ImmutableTypeReference("[Ljava/lang/Object;");

    static final String PREFS_NAME = "nobi_longpress_emoji";

    static final List<MethodReference> ALL_METHOD_REFS = Arrays.asList(
        ACTIVITY_THREAD_APP, CTX_GET_PREFS, PREFS_GET_STRING, STRING_SPLIT,
        CHAR_CODEPOINTAT, ARRAYCOPY, CS_LENGTH);

    // ---------- tiny label-based block builder ----------
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
                if (f.target.equals("GUARD")) {
                    // GUARD = the clean-up label if defined, else the block end
                    // (original insn 0). The clean-up is placed at the block end
                    // so both are the same fall-through; defining the label
                    // makes failure branches land on the clean-up first.
                    Integer ti = labels.get("GUARD");
                    targetAddr = (ti == null) ? base + total : ((ti == code.size()) ? base + total : addr.get(ti));
                } else {
                    Integer ti = labels.get(f.target);
                    if (ti == null) throw new RuntimeException("unknown label " + f.target);
                    targetAddr = (ti == code.size()) ? base + total : addr.get(ti);
                }
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
            System.out.println("hook block: " + code.size() + " insns, " + totalUnits() + " units (even)");
        }
    }

    static ImmutableInstruction35c inv35(Opcode op, int count, int a, int b, int c, int d, int e, MethodReference ref) {
        return new ImmutableInstruction35c(op, count, a, b, c, d, e, ref);
    }

    /** Build the hook block. GUARD = fall-through to original insn 0. */
    static B buildBlock() {
        B b = new B();
        // ---- 1. fingerprint: [I length == 9, item[3] code point -> v2 ----
        b.add(new ImmutableInstruction12x(Opcode.ARRAY_LENGTH, R_TMP, R_ARR_I));
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_SLOT, 9));
        b.br22t(Opcode.IF_NE, R_TMP, R_SLOT, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 3));
        b.add(new ImmutableInstruction23x(Opcode.AGET, R_CP, R_ARR_I, R_SLOT));
        // ---- 2. 26-way chain on v2 -> v3 = "emoji_X", v4 = colId ----
        for (int i = 0; i < 26; i++) {
            b.label("N" + i);
            b.add(new ImmutableInstruction31i(Opcode.CONST, R_TMP, Phase3Tables.LOOKUP_CP[i]));
            b.br22t(Opcode.IF_NE, R_CP, R_TMP, (i == 25) ? "GUARD" : ("N" + (i + 1)));
            b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PREFKEY,
                new ImmutableStringReference("emoji_" + Character.toUpperCase(Phase3Tables.LETTERS.charAt(i)))));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_COL, Phase3Tables.LOOKUP_COL[i]));
            if (i < 25) b.br20t(Opcode.GOTO_16, "PREFS");
        }
        b.label("PREFS");
        // ---- 3. prefs read: Context -> SharedPreferences -> custom string (v7) ----
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, ACTIVITY_THREAD_APP));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_CTX));
        b.br21t(Opcode.IF_EQZ, R_CTX, "GUARD");
        // prefs name + "," go in R_PART (REF-kind) — R_SLOT (v0) is INT-kind
        // and must stay INT at every GUARD merge (see register discipline).
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(PREFS_NAME)));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        // getSharedPreferences(String, int): args {v5=ctx, v11=name, v1=mode}
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_CTX, R_PART, R_TMP, 0, 0, CTX_GET_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.br21t(Opcode.IF_EQZ, R_PREFS, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0)); // null default
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_PREFKEY, R_TMP, 0, 0, PREFS_GET_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFSTR));
        b.br21t(Opcode.IF_EQZ, R_PREFSTR, "GUARD");
        // ---- 4. split(",") -> 5 non-empty parts in v8 ----
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART, new ImmutableStringReference(",")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFSTR, R_PART, 0, 0, 0, STRING_SPLIT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction12x(Opcode.ARRAY_LENGTH, R_TMP, R_PARTS));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 5));
        b.br22t(Opcode.IF_NE, R_TMP, R_SLOT, "GUARD");
        // empty-part guard (diagnosis-857 Candidate 1): every part length > 0
        for (int k = 0; k < 5; k++) {
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, k));
            b.add(new ImmutableInstruction23x(Opcode.AGET_OBJECT, R_PART, R_PARTS, R_SLOT));
            b.add(inv35(Opcode.INVOKE_INTERFACE, 1, R_PART, 0, 0, 0, 0, CS_LENGTH));
            b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
            b.br21t(Opcode.IF_EQZ, R_TMP, "GUARD");
        }
        // ---- 5. clone [I and [Object; ----
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_TMP, 9));
        b.add(new ImmutableInstruction22c(Opcode.NEW_ARRAY, R_CLONE_I, R_TMP, T_INT_ARR));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 5, R_ARR_I, R_SLOT, R_CLONE_I, R_SLOT, R_TMP, ARRAYCOPY));
        b.add(new ImmutableInstruction22c(Opcode.NEW_ARRAY, R_CLONE_O, R_TMP, T_OBJ_ARR));
        b.add(inv35(Opcode.INVOKE_STATIC, 5, R_ARR_O, R_SLOT, R_CLONE_O, R_SLOT, R_TMP, ARRAYCOPY));
        // ---- 6. five substitutions (partsIdx; slot per col 0/1/2) ----
        // canonical pref order: center(0), up(1), down(2), left(3), right(4)
        // SLOT_IDX[col] = {down,center,right,left,up}
        // center part0 -> col0:3 col1:4 col2:4
        // up     part1 -> col0:6 col1:7 col2:7
        // down   part2 -> col0:0 col1:1 col2:1
        // left   part3 -> col0:5 col1:3 col2:5
        // right  part4 -> col0:4 col1:5 col2:3
        subSlot(b, 0, 3, 4, 4);
        subSlot(b, 1, 6, 7, 7);
        subSlot(b, 2, 0, 1, 1);
        subSlot(b, 3, 5, 3, 5);
        subSlot(b, 4, 4, 5, 3);
        // ---- 7. finalize: store clones into the param registers ----
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, R_ARR_I, R_CLONE_I));
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, R_ARR_O, R_CLONE_O));
        // ---- 8. clean-up (GUARD target) ----
        // The original code uses v3/v6/v7/v8/v11 as INT locals but the hook
        // leaves REF values (pref key, prefs, prefs string, parts array, part
        // string) in them. On the empty-arrays path those registers are never
        // rewritten by the original code, so their REF types would flow to the
        // loop head / RETURN and merge INT-vs-REF (TypeMergeCheck conflicts).
        // Resetting them to INT(0) here makes every merge INT-vs-INT.
        // v5/v9/v10 are NOT reset: they are REF in the original code too
        // (Lwgz / Lpuz / Lpva), so resetting them to INT would CREATE a
        // conflict. v13/v15 (the arrays) are deliberately untouched.
        b.label("GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PREFKEY, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PREFS, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PREFSTR, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PARTS, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_PART, 0));
        return b;
    }

    /**
     * One substitution: parts[k] -> slot index f(colId) with (c0,c1,c2).
     * Slot computed on the fly into v0 (R_SLOT). v1 = code point.
     * v11 = part string (kept for APUT_OBJECT).
     */
    static void subSlot(B b, int partsIdx, int c0, int c1, int c2) {
        String tag = "P" + partsIdx;
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
            b.br10t(Opcode.GOTO, tag + "done");
            b.label(tag + "c0");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.br10t(Opcode.GOTO, tag + "done");
            b.label(tag + "c1");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c1));
            b.label(tag + "done");
        }
        // part string -> v11; codePointAt(part,0) -> v1; APUT into clones.
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, partsIdx));
        b.add(new ImmutableInstruction23x(Opcode.AGET_OBJECT, R_PART, R_PARTS, R_TMP));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, R_PART, R_TMP, 0, 0, 0, CHAR_CODEPOINTAT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
        b.add(new ImmutableInstruction23x(Opcode.APUT, R_TMP, R_CLONE_I, R_SLOT));
        b.add(new ImmutableInstruction23x(Opcode.APUT_OBJECT, R_PART, R_CLONE_O, R_SLOT));
    }

    /** Patch Lpud.m() (62 insns, 16 regs, no tries, no payloads) of 856 classes.dex. Insert hook BEFORE insn 0. */
    static MethodImplementation patchM(MethodImplementation impl) {
        List<Instruction> orig = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) orig.add(i);
        if (orig.size() != 62 || impl.getRegisterCount() != 16) {
            System.out.println("Lpud.m: unexpected shape ins=" + orig.size() + " regs=" + impl.getRegisterCount() + " (expected 62/16)");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) { System.out.println("Lpud.m: unexpected try blocks"); return null; }
        for (Instruction i : orig) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH || op == Opcode.FILL_ARRAY_DATA) {
                System.out.println("Lpud.m: unexpected payload " + op); return null;
            }
        }
        // Fingerprint: insn 0 CONST_4 v0,0 ; params v12=this v13=[I v14=[Lpuz; v15=[Object;
        Instruction i0 = orig.get(0);
        if (i0.getOpcode() != Opcode.CONST_4 || !(i0 instanceof Instruction11n)
                || ((Instruction11n) i0).getRegisterA() != 0 || ((Instruction11n) i0).getNarrowLiteral() != 0) {
            System.out.println("Lpud.m: insn0 not CONST_4 v0,0 (got " + i0 + ")"); return null;
        }
        List<Integer> origAbs = new ArrayList<>();
        int off = 0;
        for (Instruction i : orig) { origAbs.add(off); off += i.getCodeUnits(); }
        B b = buildBlock();
        b.fixup(0); // block starts at address 0; GUARD = total units
        List<Instruction> block = b.code;
        int grow = b.totalUnits();
        System.out.println("Lpud.m: insert at insn 0, grow=+" + grow + " units");
        List<Instruction> all = new ArrayList<>();
        all.addAll(block);
        all.addAll(orig);
        // Original branches: source and target BOTH shift by +grow, so offsets
        // are UNCHANGED. Verify (do not retarget): every original branch's
        // offset in the patched method must equal its original offset.
        int base = block.size();
        for (int n = base; n < all.size(); n++) {
            Instruction ins = all.get(n);
            if (!(ins instanceof OffsetInstruction)) continue;
            int origIdx = n - base;
            int oldOff = ((OffsetInstruction) orig.get(origIdx)).getCodeOffset();
            int newOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                System.out.println("Lpud.m: ORIGINAL branch offset CHANGED at origIdx " + origIdx + " old=" + oldOff + " new=" + newOff + " — abort");
                return null;
            }
        }
        System.out.println("Lpud.m: all original branch offsets unchanged (insert-at-0 property holds)");
        return new ImmutableMethodImplementation(impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null && method.getDefiningClass().equals("Lpud;")
                            && method.getName().equals("m") && method.getParameters().size() == 3) {
                        MethodImplementation ni = patchM(impl);
                        if (ni == null) throw new RuntimeException("patchM failed");
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, ni);
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    static String methodKey(MethodReference r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getDefiningClass()).append("->").append(r.getName()).append("(");
        for (CharSequence p : r.getParameterTypes()) sb.append(p);
        sb.append(")").append(r.getReturnType());
        return sb.toString();
    }
    static int methodIdsSize(File f) throws Exception {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
        try {
            raf.seek(88);
            int b0 = raf.read(); int b1 = raf.read(); int b2 = raf.read(); int b3 = raf.read();
            return (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        } finally { raf.close(); }
    }

    public static void main(String[] args) throws Exception {
        File inFile = new File(args[0]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(inFile, Opcodes.forApi(35));
        int inMethodIds = methodIdsSize(inFile);
        System.out.println("input method_ids_size=" + inMethodIds);
        // Pre-check: all refs except ActivityThread must already be present.
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
            if (r == ACTIVITY_THREAD_APP) continue; // the ONE new ref (room: 8020)
            if (!mrefs.contains(methodKey(r))) throw new RuntimeException("MISSING method ref: " + methodKey(r));
        }
        System.out.println("pre-check: all refs present except ActivityThread.currentApplication (new, room available)");
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes.dex written to " + args[1]);
        int outMethodIds = methodIdsSize(new File(args[1]));
        System.out.println("post-check: output method_ids_size=" + outMethodIds + " (input was " + inMethodIds + ", expect +1)");
        if (outMethodIds != inMethodIds + 1) throw new RuntimeException("method_ids count did not grow by exactly 1 (got " + outMethodIds + ")");
    }
}
