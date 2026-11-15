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
 * Customize-safe (code 176071857) — bring back Phase 3 customization WITHOUT
 * touching Lqmu.c().
 *
 * DESIGN (why this is safe where the Phase 3 hook was not):
 *   The Phase 3 hook (321 insns) was inserted into Lqmu.c(), the popup
 *   BUILDER, AFTER the registry/views were already built from the default
 *   list. It substituted the list post-hoc and then repaired the already-
 *   bound labels with a setText registry walk. The hook is PROVEN to be the
 *   cause of the popup breakage in 852-855 (removing it in 856 fixed the
 *   popup; classes2 was byte-identical to Fix11). The exact micro-mechanism
 *   is not proven statically (all gates pass), but the symptom progression
 *   (852 flicker-at-open -> 854 come-and-go -> 855 appears-on-release, while
 *   the hook stayed byte-identical) proves a TIMING/STATE interaction in the
 *   popup-build critical path, not a deterministic logic bug: the
 *   substitute-after-build + label-repair window, plus the 321-insn
 *   insertion changing Lqmu.c()'s structure, interact with the
 *   time-critical build/show sequence.
 *
 *   This build hooks the LIST LOAD instead: Lqml.c() (the base popup build,
 *   called by Lqmu.c() via super) right after the ActionDef list is stored
 *   into Lqml->e (original insn 25) and BEFORE any view inflation, registry
 *   build, or label binding. The hook substitutes the custom list into BOTH
 *   the local list register (v4, which the view-inflation loop reads via
 *   Lwhe->size()/get()) AND Lqml->e, so Lqmu.c() runs its EXACT Fix11 code
 *   on an already-custom list and never knows customization exists. There
 *   is NO label update (labels are correct from the start, because the views
 *   are inflated from the custom list), NO post-hoc substitution, and NO
 *   change to Lqmu.c()/o()/i()/k()/e() (byte-identical to Fix11).
 *
 * INSERTION POINT (Lqml.c, 602 insns, 33 regs, 0 try blocks, 0 payloads):
 *   After original insn 25 (IPUT_OBJECT v4, v0, Lqml->e), i.e. before
 *   original insn 26. BranchDump proves ZERO branches cross insn 25/26,
 *   so NO original branch needs retargeting (verified: spanning=0,
 *   changed=0). The method has 0 try blocks and 0 switch/array payloads,
 *   so no try/payload fixup is needed.
 *
 * REGISTER DISCIPLINE (the Fix4 / Phase 3 lessons):
 *   LIVE (never write): v0=this, v1, v2, v3, v4=list (written ONLY at
 *   finalize, on substitution success), v6=1, v7=0.
 *   FREE scratch (all first-written AND first-read AFTER insn 25, verified
 *   from the smali + DumpMethod; every one is REWRITTEN by the original code
 *   before any later read, or never used at all):
 *     v5, v8-v24, v25-v32.
 *   Each scratch register holds ONE KIND for the whole block (INT or REF),
 *   and all scratch regs are zeroed (CONST_4 0 for v<=15, CONST_16 0 for
 *   v>15) at entry; REF-kind regs are then REF-seeded with the default list
 *   (MOVE_OBJECT from v4), so every skip-branch merges kind-consistently
 *   at GUARD (int/int, ref/ref, Zero/anything) — the Phase 3 discipline.
 *   4-BIT LIMIT (dexlib2 enforces it): CONST_4 (11n), 35c invokes,
 *   22t/22c/12x field/branch/move formats only address v0-v15. So EVERY
 *   register used in a 35c invoke, 22t branch, 22c field access, 12x move,
 *   or 11n const MUST be <= v15. The 5 slot indices live in HIGH registers
 *   (v16-v20), set via CONST_16 (21s, 8-bit) and read via MOVE_FROM16
 *   (22x, 16-bit src) into the low temp v15 right before each use.
 *     INT-kind (<=15): v8 (tmp), v11 (tmp2), v13 (colId), v15 (slot temp)
 *     REF-kind (<=15): v5 (cloned ArrayList), v9 (split parts array),
 *                      v10 (item/Lpva/CharSequence -> Context ->
 *                      SharedPreferences -> new Lpva), v12 (pref key ->
 *                      custom string -> per-slot custom part), v14
 *                      (per-slot default Lpva -> Lpuz; prefs-name temp)
 *   SLOT INDICES: NOT stored in registers (v16-v20 are wide-value/double
 *   registers in the stock code — writing INTs into them creates new
 *   TypeMergeCheck I32-vs-I64 conflicts at the stock joins; and the param
 *   registers v25-v32 are re-used as temps by the original code after
 *   insn 25, so they are NOT free). Instead each setSlot computes its slot
 *   index on the fly from colId (v13) with a short branch sequence into
 *   v15. This keeps the hook to low registers only and adds ZERO new
 *   TypeMergeCheck conflicts (verified by differential).
 *   FIELD-ACCESS NARROWING (the 851 lesson — FieldCheck): every field read
 *   from a List.get() result is preceded by INSTANCE_OF + IF_EQZ -> GUARD
 *   (the stock-guard/fingerprint pattern). The hook's 7 field reads are:
 *     1x Lpva->e (fingerprint), 5x Lpva->d (setSlot), 1x Lqmo->b is read
 *     from Lqml->f (a Lqmo-typed field, NOT from a List.get result, so it
 *     is already correctly typed), 1x Lqml->f (from `this`, correctly
 *     typed). FieldCheck must report 0 violations.
 *   No new method refs (classes2 method_ids is exactly 65536): every
 *   invoked method already exists in classes2.dex (verified by the
 *   pre-check in main()).
 */
public class CustomizeSafePatch {
    // ---------- register map ----------
    static final int R_THIS = 0;        // this (Lqml), read-only
    static final int R_LIST_LOCAL = 4;  // v4 = the list; fingerprint/clone source; written ONLY at finalize
    static final int R_CLONE = 5;       // REF: cloned ArrayList
    static final int R_TMP = 8;         // INT: temp int
    static final int R_PARTS = 9;       // REF: split parts array
    static final int R_ITEM = 10;       // REF: item/Lpva/CharSequence -> Context -> SharedPreferences -> new Lpva
    static final int R_TMP2 = 11;       // INT: temp int (size/length/const literal/code point)
    static final int R_PREFSTR = 12;    // REF: pref key -> custom string -> per-slot custom part
    static final int R_COL = 13;        // INT: colId (0=center,1=left,2=right)
    static final int R_SLOT_TMP = 14;   // REF: per-slot default Lpva -> Lpuz; prefs-name temp
    static final int R_SLOT = 15;       // INT: slot index temp (computed on the fly per setSlot)

    // ---------- field refs (all already in classes2.dex field_ids) ----------
    static final ImmutableFieldReference LQML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference LQML_F =
        new ImmutableFieldReference("Lqml;", "f", "Lqmo;");
    static final ImmutableFieldReference QMO_B =
        new ImmutableFieldReference("Lqmo;", "b", "Landroid/content/Context;");
    static final ImmutableFieldReference LPVA_D =
        new ImmutableFieldReference("Lpva;", "d", "Lpuz;");
    static final ImmutableFieldReference LPVA_E =
        new ImmutableFieldReference("Lpva;", "e", "Ljava/lang/Object;");

    // ---------- method refs (all already in classes2.dex method_ids) ----------
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
    static final ImmutableMethodReference CS_LENGTH =
        new ImmutableMethodReference("Ljava/lang/CharSequence;", "length",
            Collections.<String>emptyList(), "I");

    // ---------- type refs ----------
    static final ImmutableTypeReference T_LPVA = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference T_CHARSEQ = new ImmutableTypeReference("Ljava/lang/CharSequence;");
    static final ImmutableTypeReference T_ARRAYLIST = new ImmutableTypeReference("Ljava/util/ArrayList;");

    static final String PREFS_NAME = "nobi_longpress_emoji";
    static final int G_SENTINEL = 0x7fffffff;

    static final List<MethodReference> ALL_METHOD_REFS = Arrays.asList(
        CTX_GET_PREFS, PREFS_GET_STRING, STRING_SPLIT, CHAR_CODEPOINTAT,
        LIST_SIZE, LIST_GET, LIST_ADD, LIST_SET, ARRAYLIST_INIT,
        LWHE_COPYOF, LPVA_INIT, CS_LENGTH);
    static final List<FieldReference> ALL_FIELD_REFS = Arrays.asList(
        LQML_E, LQML_F, QMO_B, LPVA_D, LPVA_E);

    // ---------- tiny label-based block builder (copied from Phase3HookPatch) ----------
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

    /** Build the hook block. GUARD = fall-through to original insn 26. */
    static B buildBlock() {
        B b = new B();
        // ---- 1. normalize all scratch registers (kind-consistent merges) ----
        for (int r : new int[]{R_CLONE, R_TMP, R_PARTS, R_ITEM, R_TMP2, R_PREFSTR, R_COL, R_SLOT_TMP, R_SLOT})
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, r, 0));
        // REF-seed the ref-kind scratch regs with the default list (v4):
        // every GUARD skip path then carries REF, never INT-0.
        for (int r : new int[]{R_CLONE, R_PARTS, R_ITEM, R_PREFSTR, R_SLOT_TMP})
            b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, r, R_LIST_LOCAL));

        // ---- 2. fingerprint: list (v4), item[3] code point -> v8 ----
        b.br21t(Opcode.IF_EQZ, R_LIST_LOCAL, "GUARD");
        b.add(inv35(Opcode.INVOKE_INTERFACE, 1, R_LIST_LOCAL, 0, 0, 0, 0, LIST_SIZE));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP2));
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_TMP, 9));
        b.br22t(Opcode.IF_NE, R_TMP2, R_TMP, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 3));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, R_LIST_LOCAL, R_TMP, 0, 0, 0, LIST_GET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_ITEM));
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, R_TMP, R_ITEM, T_LPVA));
        b.br21t(Opcode.IF_EQZ, R_TMP, "GUARD");
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, R_ITEM, R_ITEM, LPVA_E));
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, R_TMP, R_ITEM, T_CHARSEQ));
        b.br21t(Opcode.IF_EQZ, R_TMP, "GUARD");
        b.add(inv35(Opcode.INVOKE_INTERFACE, 1, R_ITEM, 0, 0, 0, 0, CS_LENGTH));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP2));
        b.br21t(Opcode.IF_EQZ, R_TMP2, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, R_ITEM, R_TMP, 0, 0, 0, CHAR_CODEPOINTAT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));

        // ---- 3. 26-way chain on v8 (code point) -> v12 = "emoji_X", v13 = colId ----
        for (int i = 0; i < 26; i++) {
            b.label("N" + i);
            b.add(new ImmutableInstruction31i(Opcode.CONST, R_TMP2, Phase3Tables.LOOKUP_CP[i]));
            b.br22t(Opcode.IF_NE, R_TMP, R_TMP2, (i == 25) ? "GUARD" : ("N" + (i + 1)));
            b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PREFSTR,
                new ImmutableStringReference("emoji_" + Character.toUpperCase(Phase3Tables.LETTERS.charAt(i)))));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_COL, Phase3Tables.LOOKUP_COL[i]));
            if (i < 25) b.br20t(Opcode.GOTO_16, "PREFS");
        }
        b.label("PREFS");

        // ---- 4. prefs read: v12 = custom string or skip to GUARD ----
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, R_ITEM, R_THIS, LQML_F));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, R_ITEM, R_ITEM, QMO_B));
        b.br21t(Opcode.IF_EQZ, R_ITEM, "GUARD");
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_SLOT_TMP, new ImmutableStringReference(PREFS_NAME)));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_ITEM, R_SLOT_TMP, R_TMP, 0, 0, CTX_GET_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_ITEM));
        b.br21t(Opcode.IF_EQZ, R_ITEM, "GUARD");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP2, 0)); // null default (INT-kind reg)
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_ITEM, R_PREFSTR, R_TMP2, 0, 0, PREFS_GET_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFSTR));
        b.br21t(Opcode.IF_EQZ, R_PREFSTR, "GUARD");

        // ---- 5. split(",") -> 5 parts (center,up,down,left,right) in v9 ----
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_SLOT_TMP, new ImmutableStringReference(",")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFSTR, R_SLOT_TMP, 0, 0, 0, STRING_SPLIT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction12x(Opcode.ARRAY_LENGTH, R_TMP2, R_PARTS));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 5));
        b.br22t(Opcode.IF_NE, R_TMP2, R_TMP, "GUARD");

        // ---- 6. clone defaults into ArrayList (v5) ----
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_CLONE, T_ARRAYLIST));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_CLONE, 0, 0, 0, 0, ARRAYLIST_INIT));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_TMP2, 9));
        b.label("CL_LOOP");
        b.br22t(Opcode.IF_GE, R_TMP, R_TMP2, "CL_DONE");
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, R_LIST_LOCAL, R_TMP, 0, 0, 0, LIST_GET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_ITEM));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, R_CLONE, R_ITEM, 0, 0, 0, LIST_ADD));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_COL)); // boolean, colId dead now
        b.add(new ImmutableInstruction22b(Opcode.ADD_INT_LIT8, R_TMP, R_TMP, 1));
        b.br10t(Opcode.GOTO, "CL_LOOP");
        b.label("CL_DONE");

        // ---- 7. five List.set replacements: (partsIdx, slotForCol0, slotForCol1, slotForCol2) ----
        // SLOT_IDX[col] = {down,center,right,left,up}; col 0=center, 1=left, 2=right.
        // down   (parts[2]): col 0->0, col 1->1, col 2->1
        // center (parts[0]): col 0->3, col 1->4, col 2->4
        // right  (parts[3]): col 0->4, col 1->5, col 2->3
        // left   (parts[4]): col 0->5, col 1->3, col 2->5
        // up     (parts[1]): col 0->6, col 1->7, col 2->7
        setSlot(b, 2, 0, 1, 1);
        setSlot(b, 0, 3, 4, 4);
        setSlot(b, 3, 4, 5, 3);
        setSlot(b, 4, 5, 3, 5);
        setSlot(b, 1, 6, 7, 7);

        // ---- 8. finalize: Lwhe.o(ArrayList) -> new Lwhe; store into v4 AND Lqml->e ----
        // (R_PARTS v9 is dead after setSlot; reuse it for the new Lwhe.)
        b.add(inv35(Opcode.INVOKE_STATIC, 1, R_CLONE, 0, 0, 0, 0, LWHE_COPYOF));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction12x(Opcode.MOVE_OBJECT, R_LIST_LOCAL, R_PARTS));
        b.add(new ImmutableInstruction22c(Opcode.IPUT_OBJECT, R_PARTS, R_THIS, LQML_E));
        return b;
    }

    /**
     * One List.set replacement: parts[k], slot index = f(colId) with
     * (c0, c1, c2) = slot for column 0 (center), 1 (left), 2 (right).
     * The slot is computed on the fly into v15 (R_SLOT) — slot indices are
     * NOT kept in registers, because the only free high registers (v18-v20)
     * are wide-value/double registers in the stock code (writing INTs into
     * them creates new TypeMergeCheck I32-vs-I64 conflicts).
     */
    static void setSlot(B b, int partsIdx, int c0, int c1, int c2) {
        // ---- compute slot index into v15 from colId (v13) ----
        if (c0 == c1 && c1 == c2) {
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
        } else if (c1 == c2) {
            // default c1; if col==0 -> c0
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c1));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
            b.br22t(Opcode.IF_NE, R_COL, R_TMP, "S" + partsIdx + "done");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.label("S" + partsIdx + "done");
        } else if (c0 == c2) {
            // default c0; if col==1 -> c1
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
            b.br22t(Opcode.IF_NE, R_COL, R_TMP, "S" + partsIdx + "done");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c1));
            b.label("S" + partsIdx + "done");
        } else if (c0 == c1) {
            // default c0; if col==2 -> c2
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 2));
            b.br22t(Opcode.IF_NE, R_COL, R_TMP, "S" + partsIdx + "done");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c2));
            b.label("S" + partsIdx + "done");
        } else {
            // all three differ: default c2; col==0 -> c0; col==1 -> c1
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c2));
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
            b.br22t(Opcode.IF_EQ, R_COL, R_TMP, "S" + partsIdx + "c0");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
            b.br22t(Opcode.IF_EQ, R_COL, R_TMP, "S" + partsIdx + "c1");
            b.br10t(Opcode.GOTO, "S" + partsIdx + "done");
            b.label("S" + partsIdx + "c0");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c0));
            b.br10t(Opcode.GOTO, "S" + partsIdx + "done");
            b.label("S" + partsIdx + "c1");
            b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, c1));
            b.label("S" + partsIdx + "done");
        }
        // ---- the substitution ----
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, partsIdx));
        b.add(new ImmutableInstruction23x(Opcode.AGET_OBJECT, R_PREFSTR, R_PARTS, R_TMP));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, R_PREFSTR, R_TMP, 0, 0, 0, CHAR_CODEPOINTAT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP2));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 2, R_CLONE, R_SLOT, 0, 0, 0, LIST_GET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_SLOT_TMP));
        // NARROWING (851 lesson): INSTANCE_OF + IF_EQZ before the Lpva->d read.
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, R_TMP, R_SLOT_TMP, T_LPVA));
        b.br21t(Opcode.IF_EQZ, R_TMP, "GUARD");
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, R_SLOT_TMP, R_SLOT_TMP, LPVA_D));
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_ITEM, T_LPVA));
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_TMP, G_SENTINEL));
        b.add(inv35(Opcode.INVOKE_DIRECT, 5, R_ITEM, R_TMP2, R_SLOT_TMP, R_PREFSTR, R_TMP, LPVA_INIT));
        b.add(inv35(Opcode.INVOKE_INTERFACE, 3, R_CLONE, R_SLOT, R_ITEM, 0, 0, LIST_SET));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_SLOT_TMP));
    }

    /** Patch Lqml.c() (602 insns, 33 regs, no tries, no payloads) of 856 classes2.dex. */
    static MethodImplementation patchC(MethodImplementation impl) {
        List<Instruction> orig = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) orig.add(i);
        if (orig.size() != 602 || impl.getRegisterCount() != 33) {
            System.out.println("Lqml.c: unexpected shape ins=" + orig.size() + " regs=" + impl.getRegisterCount() + " (expected 602/33)");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) { System.out.println("Lqml.c: unexpected try blocks"); return null; }
        for (Instruction i : orig) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH || op == Opcode.FILL_ARRAY_DATA) {
                System.out.println("Lqml.c: unexpected payload " + op); return null;
            }
        }
        List<Integer> origAbs = new ArrayList<>();
        int off = 0;
        for (Instruction i : orig) { origAbs.add(off); off += i.getCodeUnits(); }
        int INS = origAbs.get(26); // address of original insn 26 (block inserts BEFORE it)
        // Fingerprint: insn 24 IGET_OBJECT ActionDef->f, insn 25 IPUT_OBJECT Lqml->e
        Instruction i24 = orig.get(24);
        Instruction i25 = orig.get(25);
        if (i24.getOpcode() != Opcode.IGET_OBJECT || !(i24 instanceof Instruction22c)) {
            System.out.println("c: insn24 not iget-object (got " + i24.getOpcode() + ")"); return null;
        }
        {   // insn24 must read ActionDef->f (the list source)
            Reference r24 = ((Instruction22c) i24).getReference();
            if (!(r24 instanceof FieldReference)
                    || !((FieldReference) r24).getDefiningClass().endsWith("ActionDef;")
                    || !((FieldReference) r24).getName().equals("f")) {
                System.out.println("c: insn24 not iget ActionDef->f (got " + r24 + ")"); return null;
            }
        }
        if (i25.getOpcode() != Opcode.IPUT_OBJECT || !(i25 instanceof Instruction22c)
                || !((Instruction22c) i25).getReference().equals(LQML_E)) {
            System.out.println("c: insn25 not iput Lqml->e"); return null;
        }
        // Count branches that SPAN the insertion point (either direction) — expect 0.
        int spanning = 0;
        for (int n = 0; n < orig.size(); n++) {
            Instruction ins = orig.get(n);
            if (!(ins instanceof OffsetInstruction)) continue;
            int a = origAbs.get(n);
            int t = a + ((OffsetInstruction) ins).getCodeOffset();
            if ((a < INS && t > INS) || (a >= INS && t < INS)) spanning++;
        }
        System.out.println("c: INS=" + Integer.toHexString(INS) + ", " + spanning + " original branches span the insertion point (expected 0)");
        if (spanning != 0) { System.out.println("c: spanning branches exist — abort"); return null; }
        // build + fixup block (buildBlock returns its own B: labels+fixups
        // must stay attached to the code — copying only the list drops them)
        B b = buildBlock();
        b.fixup(INS);
        List<Instruction> block = b.code;
        int grow = b.totalUnits();
        System.out.println("Lqml.c: INS=" + Integer.toHexString(INS) + " grow=+" + grow + " units");
        // splice: orig[0..25] + block + orig[26..601]
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < 26; i++) all.add(orig.get(i));
        int blockStart = all.size();
        all.addAll(block);
        int blockEnd = all.size();
        for (int i = 26; i < orig.size(); i++) all.add(orig.get(i));
        // Retarget ORIGINAL branches only (skip block range). None should change
        // (spanning=0; branches entirely after INS shift source AND target equally).
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
        System.out.println("c: " + changed + " original branch offsets changed (expected " + spanning + ")");
        if (changed != spanning) { System.out.println("c: unexpected changed count"); return null; }
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
                    if (impl != null && method.getDefiningClass().equals("Lqml;")
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

    static String methodKey(MethodReference r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getDefiningClass()).append("->").append(r.getName()).append("(");
        for (CharSequence p : r.getParameterTypes()) sb.append(p);
        sb.append(")").append(r.getReturnType());
        return sb.toString();
    }
    static String fieldKey(FieldReference r) {
        return r.getDefiningClass() + "->" + r.getName() + ":" + r.getType();
    }
    static int methodIdsSize(File f) throws Exception {
        // dex header: method_ids_size is a 4-byte little-endian int at offset 88.
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
        // ---- pre-check: every method/field ref I use must ALREADY be used ----
        // (classes2 method_ids is exactly 65536 — no new method refs allowed).
        // Collect all refs actually referenced by code in the dex.
        Set<String> mrefs = new HashSet<>();
        Set<String> frefs = new HashSet<>();
        for (ClassDef cd : dex.getClasses()) {
            for (Method m : cd.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    if (ins instanceof ReferenceInstruction) {
                        Reference r = ((ReferenceInstruction) ins).getReference();
                        if (r instanceof MethodReference) mrefs.add(methodKey((MethodReference) r));
                        if (r instanceof FieldReference) frefs.add(fieldKey((FieldReference) r));
                    }
                }
            }
            for (Field fld : cd.getFields()) {
                // static field initial values may reference strings, not methods; skip
            }
        }
        int inMethodIds = methodIdsSize(inFile);
        for (MethodReference r : ALL_METHOD_REFS) {
            if (!mrefs.contains(methodKey(r))) throw new RuntimeException("MISSING method ref (would exceed 65536): " + methodKey(r));
        }
        for (FieldReference r : ALL_FIELD_REFS) {
            if (!frefs.contains(fieldKey(r))) throw new RuntimeException("MISSING field ref: " + fieldKey(r));
        }
        System.out.println("pre-check: all " + ALL_METHOD_REFS.size() + " method refs + " + ALL_FIELD_REFS.size() + " field refs already present; method_ids_size=" + inMethodIds);
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes2 written to " + args[1]);
        // ---- post-check: method_ids count must not have grown ----
        int outMethodIds = methodIdsSize(new File(args[1]));
        System.out.println("post-check: output method_ids_size=" + outMethodIds + " (input was " + inMethodIds + ")");
        if (outMethodIds != inMethodIds) throw new RuntimeException("method_ids count CHANGED — a new method ref was added!");
    }
}
