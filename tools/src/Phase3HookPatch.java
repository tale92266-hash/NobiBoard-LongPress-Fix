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
 * Phase 3 (code 176071851) — long-press emoji override hook.
 *
 * Inserts ONE block into Lqmu.c() of Fix11's classes2.dex, right before the
 * emoji guard (original insn idx 70, the `iget-object v0, p0, Lqml->e`).
 * The block:
 *   1. Reads the default 9-item list (Lqml.e, set by super.c()).
 *   2. Fingerprints the key: item[3] -> Lpva.e (CharSequence) -> codePointAt(0).
 *      Token[3] is UNIQUE across all 26 keys (verified from fd2.xml).
 *   3. 26-way chain -> pref key "emoji_X" + column class (0=center,1=left,2=right).
 *   4. Reads SharedPreferences "nobi_longpress_emoji" -> getString("emoji_X", null).
 *      Absent/malformed -> SKIP with ZERO side effects (fall through to the guard).
 *   5. split(",") -> 5 parts (center,up,down,left,right); builds a new
 *      ImmutableList (ArrayList clone + 5 List.set + Lwhe.o copyOf) and
 *      iputs it into Lqml.e (public, non-final).
 *   6. Updates the label TextViews via the tracker registry (Lqmr.d, Lqmc.b
 *      == data index) so the DISPLAYED emoji matches the INSERTED one.
 *   7. Falls through to the stock emoji guard, which re-reads Lqml.e (now the
 *      custom list) and centre-inits the tracker.
 *
 * Register discipline (the Fix4 lesson): every scratch register holds ONE
 * KIND for the whole block, and all 13 scratch regs are zeroed (const/4 0)
 * at block entry, so every skip-branch merges kind-consistently at the
 * guard (int/int, ref/ref, Zero/anything). Registers:
 *   v0 int temp, v1 ref (default list / built list / registry),
 *   v2 ref (SharedPreferences / new Lpva / Lqmc),
 *   v3 int temp, v4 ref (pref string / emoji / stashed ArrayList),
 *   v5 int (colId, then 0x7fffffff g, then label-loop counter),
 *   v6 this (never written), v7 ref (pref key / default Lpva / Lpuz / set sink),
 *   v8..v12 int slot indices (down,center,right,left,up),
 *   v13 ref (split parts array).
 * The REL-kind scratch regs (v2/v4/v7/v13) are REF-seeded with the default
 * list right after it is loaded: without this they merge INT-0 (from the
 * zeroing) vs REF at GUARD and TypeMergeCheck flags a spurious conflict.
 * The seeds are overwritten before each reg's first real read, so they are
 * pure type seeding with zero semantic effect.
 * No new method refs: every invoked method already exists in classes2.dex
 * (verified by Recon/Recon3); new strings/types/fields have room.
 * New Lpva args mirror the JSON parser (Lpfs.b): c=codePointAt(emoji,0),
 * d=default item's Lpuz (read back), e=emoji String, g=0x7fffffff (the
 * cached-hash sentinel the parser stores).
 */
public class Phase3HookPatch {
    // ---------- field refs (all already used in classes2.dex) ----------
    static final ImmutableFieldReference LQML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QMU_F =
        new ImmutableFieldReference("Lqmu;", "f", "Lqmo;");
    static final ImmutableFieldReference QMO_B =
        new ImmutableFieldReference("Lqmo;", "b", "Landroid/content/Context;");
    static final ImmutableFieldReference QMU_G =
        new ImmutableFieldReference("Lqmu;", "g", "Lqmr;");
    static final ImmutableFieldReference QMR_D =
        new ImmutableFieldReference("Lqmr;", "d", "Ljava/util/List;");
    static final ImmutableFieldReference QMC_B =
        new ImmutableFieldReference("Lqmc;", "b", "I");
    static final ImmutableFieldReference QMC_D =
        new ImmutableFieldReference("Lqmc;", "d", "Landroid/view/View;");
    static final ImmutableFieldReference LPVA_D =
        new ImmutableFieldReference("Lpva;", "d", "Lpuz;");
    static final ImmutableFieldReference LPVA_E =
        new ImmutableFieldReference("Lpva;", "e", "Ljava/lang/Object;");

    // ---------- method refs (all already used in classes2.dex) ----------
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
    static final ImmutableMethodReference LIST_SIZE =
        new ImmutableMethodReference("Ljava/util/List;", "size",
            Collections.<String>emptyList(), "I");
    static final ImmutableMethodReference LIST_GET =
        new ImmutableMethodReference("Ljava/util/List;", "get",
            Arrays.asList("I"), "Ljava/lang/Object;");
    static final ImmutableMethodReference LIST_ADD =
        new ImmutableMethodReference("Ljava/util/List;", "add",
            Arrays.asList("Ljava/lang/Object;"), "Z");
    static final ImmutableMethodReference LIST_SET =
        new ImmutableMethodReference("Ljava/util/List;", "set",
            Arrays.asList("I", "Ljava/lang/Object;"), "Ljava/lang/Object;");
    static final ImmutableMethodReference ARRAYLIST_INIT =
        new ImmutableMethodReference("Ljava/util/ArrayList;", "<init>",
            Collections.<String>emptyList(), "V");
    static final ImmutableMethodReference LWHE_COPYOF =
        new ImmutableMethodReference("Lwhe;", "o",
            Arrays.asList("Ljava/util/Collection;"), "Lwhe;");
    static final ImmutableMethodReference LPVA_INIT =
        new ImmutableMethodReference("Lpva;", "<init>",
            Arrays.asList("I", "Lpuz;", "Ljava/lang/Object;", "I"), "V");
    static final ImmutableMethodReference TEXTVIEW_SETTEXT =
        new ImmutableMethodReference("Landroid/widget/TextView;", "setText",
            Arrays.asList("Ljava/lang/CharSequence;"), "V");
    static final ImmutableMethodReference CS_LENGTH =
        new ImmutableMethodReference("Ljava/lang/CharSequence;", "length",
            Collections.<String>emptyList(), "I");

    // ---------- type refs ----------
    static final ImmutableTypeReference T_LPVA = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference T_CHARSEQ = new ImmutableTypeReference("Ljava/lang/CharSequence;");
    static final ImmutableTypeReference T_TEXTVIEW = new ImmutableTypeReference("Landroid/widget/TextView;");
    static final ImmutableTypeReference T_ARRAYLIST = new ImmutableTypeReference("Ljava/util/ArrayList;");

    static final String PREFS_NAME = "nobi_longpress_emoji";
    static final int G_SENTINEL = 0x7fffffff;

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
        /** GOTO/16 (20t, +/-32767): use for long jumps — plain GOTO (10t) only holds +/-127. */
        void br20t(Opcode op, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction20t(op, 0)); }
        int totalUnits() { int t = 0; for (Instruction i : code) t += i.getCodeUnits(); return t; }
        /** Patch all branch offsets; base = address of the block start; GUARD = base + total. */
        void fixup(int base) {
            if (totalUnits() % 2 != 0) code.add(new ImmutableInstruction10x(Opcode.NOP));
            List<Integer> addr = new ArrayList<>();
            int a = base;
            for (Instruction i : code) { addr.add(a); a += i.getCodeUnits(); }
            int total = a - base;
            for (Fix f : fixups) {
                int targetAddr;
                if (f.target.equals("GUARD")) targetAddr = base + total;
                else {
                    Integer ti = labels.get(f.target);
                    if (ti == null) throw new RuntimeException("unknown label " + f.target);
                    // a label at code.size() (block end, e.g. LB_DONE) sits at base+total
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
    /** Build the hook block. Labels: GUARD = fall-through to the stock guard.
     *  Returns the builder itself (labels + fixups must survive with the code). */
    static B buildBlock() {
        B b = new B();
        // ---- 1. normalize all scratch registers (kind-consistent merges) ----
        for (int r : new int[]{0, 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13})
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, r, 0));
        // ---- 2. fingerprint: default list, item[3] code point -> v3 ----
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 6, LQML_E));
        // REF-seed the ref-kind scratch regs with the list (see header note):
        // every GUARD skip path then carries REF, never INT-0.
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, 2, 1));
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, 4, 1));
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, 7, 1));
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, 13, 1));
        b.br21t(Opcode.IF_EQZ, 1, "GUARD");
        b.add(inv35(Opcode.INVOKE_INTERFACE, 1, 1, 0, 0, 0, 0, LIST_SIZE));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, 0, 9));
        b.br22t(Opcode.IF_NE, 3, 0, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 3));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, 1, 0, 0, 0, 0, LIST_GET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 2));
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 0, 2, T_LPVA));
        b.br21t(Opcode.IF_EQZ, 0, "GUARD");
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 2, LPVA_E));
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 0, 2, T_CHARSEQ));
        b.br21t(Opcode.IF_EQZ, 0, "GUARD");
        b.add(inv35(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CS_LENGTH));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        b.br21t(Opcode.IF_EQZ, 3, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, 2, 0, 0, 0, 0, CHAR_CODEPOINTAT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // ---- 3. 26-way chain on v3 -> v7 = "emoji_X", v5 = colId ----
        for (int i = 0; i < 26; i++) {
            b.label("N" + i);
            b.add(new ImmutableInstruction31i(Opcode.CONST, 0, Phase3Tables.LOOKUP_CP[i]));
            b.br22t(Opcode.IF_NE, 3, 0, (i == 25) ? "GUARD" : ("N" + (i + 1)));
            b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 7,
                new ImmutableStringReference("emoji_" + Character.toUpperCase(Phase3Tables.LETTERS.charAt(i)))));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, 5, Phase3Tables.LOOKUP_COL[i]));
            b.br20t(Opcode.GOTO_16, "COLSET"); // long jump: chain spans >127 units, plain GOTO cannot reach
        }
        // ---- 4. COLSET: v8..v12 = slot indices (down,center,right,left,up) ----
        b.label("COLSET");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        b.br22t(Opcode.IF_EQ, 5, 0, "CENTER");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 1));
        b.br22t(Opcode.IF_EQ, 5, 0, "LEFT");
        setIdx(b, 2); // right edge
        b.br10t(Opcode.GOTO, "PREFS");
        b.label("CENTER");
        setIdx(b, 0);
        b.br10t(Opcode.GOTO, "PREFS");
        b.label("LEFT");
        setIdx(b, 1);
        b.label("PREFS");
        // ---- 5. prefs read: v4 = custom string or skip ----
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 6, QMU_F));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 2, QMO_B));
        b.br21t(Opcode.IF_EQZ, 2, "GUARD");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 4, new ImmutableStringReference(PREFS_NAME)));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, 2, 4, 0, 0, 0, CTX_GET_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 2));
        b.br21t(Opcode.IF_EQZ, 2, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 0));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, 2, 7, 4, 0, 0, PREFS_GET_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        b.br21t(Opcode.IF_EQZ, 4, "GUARD");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 2, new ImmutableStringReference(",")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, 4, 2, 0, 0, 0, STRING_SPLIT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 13));
        b.add(new ImmutableInstruction12x(Opcode.ARRAY_LENGTH, 0, 13));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 5));
        b.br22t(Opcode.IF_NE, 0, 3, "GUARD");
        // ---- 6. clone defaults into ArrayList (v4), stash in this.e ----
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, 4, T_ARRAYLIST));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, 4, 0, 0, 0, 0, ARRAYLIST_INIT));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, 3, 9));
        b.label("CL_LOOP");
        b.br22t(Opcode.IF_GE, 0, 3, "CL_DONE");
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, 1, 0, 0, 0, 0, LIST_GET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 2));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, 4, 2, 0, 0, 0, LIST_ADD));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 5));
        b.add(new ImmutableInstruction22b(Opcode.ADD_INT_LIT8, 0, 0, 1));
        b.br10t(Opcode.GOTO, "CL_LOOP");
        b.label("CL_DONE");
        b.add(new ImmutableInstruction22c(Opcode.IPUT_OBJECT, 4, 6, LQML_E));
        b.add(new ImmutableInstruction31i(Opcode.CONST, 5, G_SENTINEL));
        // ---- 7. five List.set replacements: (slotReg, partsIdx) ----
        // down (v8, parts[2]), center (v9, parts[0]), right (v10, parts[3]),
        // left (v11, parts[4]), up (v12, parts[1])
        setSlot(b, 8, 2);
        setSlot(b, 9, 0);
        setSlot(b, 10, 3);
        setSlot(b, 11, 4);
        setSlot(b, 12, 1);
        // ---- 8. finalize: Lwhe.o(ArrayList) -> this.e ----
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 6, LQML_E));
        b.add(inv35(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0, LWHE_COPYOF));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        b.add(new ImmutableInstruction22c(Opcode.IPUT_OBJECT, 1, 6, LQML_E));
        // ---- 9. label update via registry (Lqmr.d) ----
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 6, QMU_G));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 1, QMR_D));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 1, 1, 0, 0, 0, 0, LIST_SIZE));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 5, 0));
        b.label("LB_LOOP");
        b.br22t(Opcode.IF_GE, 5, 0, "LB_DONE");
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, 1, 5, 0, 0, 0, LIST_GET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 2));
        b.br21t(Opcode.IF_EQZ, 2, "LB_NEXT");
        b.add(new ImmutableInstruction22c(Opcode.IGET, 3, 2, QMC_B));
        b.br22t(Opcode.IF_EQ, 3, 8, "LB_DOWN");
        b.br22t(Opcode.IF_EQ, 3, 9, "LB_CENTER");
        b.br22t(Opcode.IF_EQ, 3, 10, "LB_RIGHT");
        b.br22t(Opcode.IF_EQ, 3, 11, "LB_LEFT");
        b.br22t(Opcode.IF_EQ, 3, 12, "LB_UP");
        b.br10t(Opcode.GOTO, "LB_NEXT");
        b.label("LB_DOWN");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 2));
        b.br10t(Opcode.GOTO, "LB_SET");
        b.label("LB_CENTER");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));
        b.br10t(Opcode.GOTO, "LB_SET");
        b.label("LB_RIGHT");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 3));
        b.br10t(Opcode.GOTO, "LB_SET");
        b.label("LB_LEFT");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 4));
        b.br10t(Opcode.GOTO, "LB_SET");
        b.label("LB_UP");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 1));
        b.label("LB_SET");
        b.add(new ImmutableInstruction23x(Opcode.AGET_OBJECT, 4, 13, 3));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 2, QMC_D));
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, T_TEXTVIEW));
        b.br21t(Opcode.IF_EQZ, 3, "LB_NEXT");
        b.add(new ImmutableInstruction21c(Opcode.CHECK_CAST, 2, T_TEXTVIEW));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, 2, 4, 0, 0, 0, TEXTVIEW_SETTEXT));
        b.label("LB_NEXT");
        b.add(new ImmutableInstruction22b(Opcode.ADD_INT_LIT8, 5, 5, 1));
        b.br10t(Opcode.GOTO, "LB_LOOP");
        b.label("LB_DONE");
        return b;
    }

    /** Emit v8..v12 = SLOT_IDX[col] (down,center,right,left,up). */
    static void setIdx(B b, int col) {
        int[] s = Phase3Tables.SLOT_IDX[col];
        for (int i = 0; i < 5; i++)
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, 8 + i, s[i]));
    }

    /** One List.set replacement: slot register vX, parts[k]. */
    static void setSlot(B b, int slotReg, int partsIdx) {
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, partsIdx));
        b.add(new ImmutableInstruction23x(Opcode.AGET_OBJECT, 4, 13, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, 4, 0, 0, 0, 0, CHAR_CODEPOINTAT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, 1, slotReg, 0, 0, 0, LIST_GET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 7));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 7, 7, LPVA_D));
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, 2, T_LPVA));
        b.add(inv35(Opcode.INVOKE_DIRECT, 5, 2, 3, 7, 4, 5, LPVA_INIT));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 6, LQML_E));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 3, 4, slotReg, 2, 0, 0, LIST_SET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 7));
    }

    /** Patch Lqmu.c() (101 insns, 14 regs, no tries) of Fix11 classes2.dex. */
    static MethodImplementation patchC(MethodImplementation impl) {
        List<Instruction> orig = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) orig.add(i);
        if (orig.size() != 101 || impl.getRegisterCount() != 14) {
            System.out.println("Lqmu.c: unexpected shape ins=" + orig.size() + " regs=" + impl.getRegisterCount() + " (expected Fix11 101/14)");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) { System.out.println("Lqmu.c: unexpected try blocks"); return null; }
        for (Instruction i : orig) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH || op == Opcode.FILL_ARRAY_DATA) {
                System.out.println("Lqmu.c: unexpected payload " + op); return null;
            }
        }
        List<Integer> origAbs = new ArrayList<>();
        int off = 0;
        for (Instruction i : orig) { origAbs.add(off); off += i.getCodeUnits(); }
        int INS = origAbs.get(70);
        // fingerprint: insn 0 super, 26 IF_GE -> INS, 70 IGET_OBJECT Lqml->e, 99 INVOKE_DIRECT p(), 100 RETURN_VOID
        if (orig.get(0).getOpcode() != Opcode.INVOKE_SUPER_RANGE) { System.out.println("c: insn0 not super"); return null; }
        if (orig.get(70).getOpcode() != Opcode.IGET_OBJECT) { System.out.println("c: insn70 not iget-object"); return null; }
        Instruction i70 = orig.get(70);
        if (!(i70 instanceof Instruction22c) || !((Instruction22c) i70).getReference().equals(LQML_E)) {
            System.out.println("c: insn70 is not iget Lqml->e"); return null;
        }
        if (orig.get(99).getOpcode() != Opcode.INVOKE_DIRECT || orig.get(100).getOpcode() != Opcode.RETURN_VOID) {
            System.out.println("c: tail mismatch"); return null;
        }
        if (!(orig.get(26) instanceof OffsetInstruction)) { System.out.println("c: insn26 not a branch"); return null; }
        int t26 = origAbs.get(26) + ((OffsetInstruction) orig.get(26)).getCodeOffset();
        if (t26 != INS) { System.out.println("c: insn26 targets " + Integer.toHexString(t26) + " != INS " + Integer.toHexString(INS)); return null; }
        int targetsIns = 0;
        for (int n = 0; n < orig.size(); n++) {
            Instruction ins = orig.get(n);
            if (!(ins instanceof OffsetInstruction)) continue;
            int t = origAbs.get(n) + ((OffsetInstruction) ins).getCodeOffset();
            if (t == INS) targetsIns++;
        }
        if (targetsIns != 1) { System.out.println("c: " + targetsIns + " branches target INS (expected exactly 1)"); return null; }
        // build + fixup block (buildBlock returns its own B: labels+fixups
        // must stay attached to the code — copying only the list drops them)
        B b = buildBlock();
        b.fixup(INS);
        List<Instruction> block = b.code;
        int grow = b.totalUnits();
        System.out.println("Lqmu.c: INS=" + Integer.toHexString(INS) + " grow=+" + grow + " units");
        // splice: orig[0..69] + block + orig[70..100]
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < 70; i++) all.add(orig.get(i));
        int blockStart = all.size();
        all.addAll(block);
        int blockEnd = all.size();
        for (int i = 70; i < orig.size(); i++) all.add(orig.get(i));
        // retarget ORIGINAL branches only (skip block range).
        // Only branches SPANNING the insertion point (a < INS, t > INS) change
        // offset: branches entirely after INS shift source AND target equally,
        // so the six guard skips to p() keep their offsets (my earlier "6"
        // expectation was wrong — the dump proved it: 0 changed is correct
        // unless a pre-INS branch crosses INS).
        int expectedSpanning = 0;
        for (int n = 0; n < orig.size(); n++) {
            Instruction ins = orig.get(n);
            if (!(ins instanceof OffsetInstruction)) continue;
            int a = origAbs.get(n);
            int t = a + ((OffsetInstruction) ins).getCodeOffset();
            if (a < INS && t > INS) expectedSpanning++;
        }
        System.out.println("c: " + expectedSpanning + " original branches span the insertion point");
        int changed = 0;
        for (int n = 0; n < all.size(); n++) {
            if (n >= blockStart && n < blockEnd) continue;
            Instruction ins = all.get(n);
            if (!(ins instanceof OffsetInstruction)) continue;
            int origIdx = (n < blockStart) ? n : n - block.size();
            int a = origAbs.get(origIdx);
            int t = a + ((OffsetInstruction) ins).getCodeOffset();
            int ap = (a >= INS) ? a + grow : a;
            int tp = (t > INS) ? t + grow : t;
            int newOff = tp - ap;
            int oldOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                all.set(n, retarget(ins, newOff));
                changed++;
                System.out.println("c: retargeted origIdx " + origIdx + " abs " + Integer.toHexString(a) + " -> tgt " + Integer.toHexString(t) + " newOff " + newOff);
            }
        }
        System.out.println("c: " + changed + " original branch offsets changed (expected " + expectedSpanning + " spanning branches)");
        if (changed != expectedSpanning) { System.out.println("c: unexpected changed count"); return null; }
        return new ImmutableMethodImplementation(impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    static Instruction retarget(Instruction ins, int newOff) {
        if (ins instanceof Instruction21t) { Instruction21t o = (Instruction21t) ins; return new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), newOff); }
        else if (ins instanceof Instruction22t) { Instruction22t o = (Instruction22t) ins; return new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(), o.getRegisterB(), newOff); }
        else if (ins instanceof Instruction10t) { Instruction10t o = (Instruction10t) ins; return new ImmutableInstruction10t(o.getOpcode(), newOff); }
        else if (ins instanceof Instruction20t) { Instruction20t o = (Instruction20t) ins; return new ImmutableInstruction20t(o.getOpcode(), newOff); }
        else if (ins instanceof Instruction30t) { Instruction30t o = (Instruction30t) ins; return new ImmutableInstruction30t(o.getOpcode(), newOff); }
        throw new RuntimeException("retarget on non-branch " + ins.getOpcode());
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null && method.getDefiningClass().equals("Lqmu;")
                            && method.getName().equals("c") && method.getParameters().size() == 7) {
                        MethodImplementation ni = patchC(impl);
                        if (ni == null) throw new RuntimeException("patchC failed");
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, ni);
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes2 written to " + args[1]);
    }
}
