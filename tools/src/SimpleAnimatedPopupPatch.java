import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Simple Animated Fix (code 176071836) — two VerifyError fixes on top of
 * 176071835 (CHECK_CAST after the INSTANCE_OF guard in k(); setDuration
 * register count 2 -> 3 in o()). See change-report.md.
 *
 * Simple Animated (code 176071835) — three PROVEN fixes on top of the
 * Simple Rewrite (176071834), all in ONE dexlib2 pass, zero new refs.
 *
 * BUG #1 (PROVEN from the shipped 176071834 dex): the Lqmu.c() tracker-init
 * block is DEAD CODE. The 2Fix4 patch retargeted the loop-exit IF_GE
 * (insn 26) to the NEW p() address (insn 99) instead of to the init-block
 * start (insn 70), so the block is reachable only by fall-through from
 * insn 69 — an unconditional GOTO. The tracker therefore never gets
 * (1,1); it stays at stock (1,2) = bottom-middle = DOWN emoji at open.
 * FIX (patchC): retarget insn 26 to insn 70 (one branch). Flow becomes
 * loop exit -> init block (tracker -> (1,1) for emoji popups) -> p()
 * (green indicator on the center at open).
 *
 * BUG #2 (PROVEN from tools/src/SimplePopupPatch.java): the k() head's
 * direction pixels sit on the CELL BOUNDARIES, not the cell centers.
 * It used HALF-cell offsets (centerY +/- halfCellH = the edge between
 * the center cell and the UP/DOWN cell). A boundary point is ambiguous
 * for the nearest-item search, so swipes do not reliably change the
 * selection. FIX (patchK): use FULL cell offsets — UP py = centerY-cellH,
 * DOWN py = centerY+cellH, LEFT px = centerX-cellW, RIGHT px = centerX
 * +cellW — the exact centers of the target cells. The half-cell DIVs
 * become dead and are removed.
 *
 * FIX #3 (guarantee + animation): after i() updates the tracker, the
 * k() head now calls n() — the stock "refresh the indicator to the
 * tracker's current item" (m() scale + o() move). i()'s own tail only
 * calls n() on one path, so this makes the indicator follow the finger
 * unconditionally. And Lqmu.o() — the ONE place the indicator moves —
 * now GLIDES instead of jumping: setTranslationX/Y become
 * animate().translationX/Y().setDuration(120).start(). Because the
 * glide lives in o(), it applies at open (p() -> o() glides the
 * indicator to the center) AND on every swipe move. A 2-instruction
 * INSTANCE_OF guard proves `this` is a Lqmu before calling its n().
 *
 * Registers in k() (13 regs: v10=this, v11=x float, v12=y float):
 * same as the Simple Rewrite head. v4=contentW, v5=contentH (dead after
 * geometry); v6=cellW then px; v7=cellH then py; v8=centerX; v9=centerY.
 * Lqmu.o() grows 6 -> 9 registers (v6=animator, v7+v8=long 120ms).
 * Lqml.e() is NOT touched (already correct in 176071834).
 */
public class SimpleAnimatedPopupPatch {
    // ---------- shared references (all already exist in classes2.dex) ----------
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QML_D =
        new ImmutableFieldReference("Lqml;", "d", "Ljava/util/List;");
    static final ImmutableFieldReference QML_C =
        new ImmutableFieldReference("Lqml;", "c", "Landroid/widget/LinearLayout;");
    static final ImmutableFieldReference QML_I =
        new ImmutableFieldReference("Lqml;", "i", "F");
    static final ImmutableFieldReference QML_B =
        new ImmutableFieldReference("Lqml;", "b", "F");
    static final ImmutableFieldReference PVA_E =
        new ImmutableFieldReference("Lpva;", "e", "Ljava/lang/Object;");
    static final ImmutableMethodReference LIST_SIZE =
        new ImmutableMethodReference("Ljava/util/List;", "size",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference LIST_GET =
        new ImmutableMethodReference("Ljava/util/List;", "get",
            java.util.Arrays.asList("I"), "Ljava/lang/Object;");
    static final ImmutableMethodReference LIST_ISEMPTY =
        new ImmutableMethodReference("Ljava/util/List;", "isEmpty",
            java.util.Collections.emptyList(), "Z");
    static final ImmutableMethodReference CHARSEQ_LENGTH =
        new ImmutableMethodReference("Ljava/lang/CharSequence;", "length",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference CHAR_CODEPOINTAT =
        new ImmutableMethodReference("Ljava/lang/Character;", "codePointAt",
            java.util.Arrays.asList("Ljava/lang/CharSequence;", "I"), "I");
    static final ImmutableMethodReference LL_GETWIDTH =
        new ImmutableMethodReference("Landroid/widget/LinearLayout;", "getWidth",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference LL_GETHEIGHT =
        new ImmutableMethodReference("Landroid/widget/LinearLayout;", "getHeight",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_GETPADLEFT =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingLeft",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_GETPADRIGHT =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingRight",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_GETPADTOP =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingTop",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_GETPADBOTTOM =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingBottom",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference QML_A =
        new ImmutableMethodReference("Lqml;", "a",
            java.util.Collections.emptyList(), "Lpva;");
    static final ImmutableMethodReference QML_I2 =
        new ImmutableMethodReference("Lqml;", "i",
            java.util.Arrays.asList("I", "I"), "V");
    static final ImmutableMethodReference QMU_N =
        new ImmutableMethodReference("Lqmu;", "n",
            java.util.Collections.emptyList(), "V");
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");
    static final ImmutableTypeReference QMU_TYPE = new ImmutableTypeReference("Lqmu;");

    // ---------- refs for the o() glide (all already exist in classes2.dex) ----------
    static final ImmutableMethodReference VIEW_ANIMATE =
        new ImmutableMethodReference("Landroid/view/View;", "animate",
            java.util.Collections.emptyList(), "Landroid/view/ViewPropertyAnimator;");
    static final ImmutableMethodReference VPA_TRANSLATION_X =
        new ImmutableMethodReference("Landroid/view/ViewPropertyAnimator;", "translationX",
            java.util.Arrays.asList("F"), "Landroid/view/ViewPropertyAnimator;");
    static final ImmutableMethodReference VPA_TRANSLATION_Y =
        new ImmutableMethodReference("Landroid/view/ViewPropertyAnimator;", "translationY",
            java.util.Arrays.asList("F"), "Landroid/view/ViewPropertyAnimator;");
    static final ImmutableMethodReference VPA_SET_DURATION =
        new ImmutableMethodReference("Landroid/view/ViewPropertyAnimator;", "setDuration",
            java.util.Arrays.asList("J"), "Landroid/view/ViewPropertyAnimator;");
    static final ImmutableMethodReference VPA_START =
        new ImmutableMethodReference("Landroid/view/ViewPropertyAnimator;", "start",
            java.util.Collections.emptyList(), "V");

    // ---------- tracker-init refs (Lqmu.c) ----------
    static final ImmutableFieldReference QMU_G =
        new ImmutableFieldReference("Lqmu;", "g", "Lqmr;");
    static final ImmutableFieldReference QMR_B =
        new ImmutableFieldReference("Lqmr;", "b", "Landroid/graphics/Point;");
    static final ImmutableFieldReference QMR_C =
        new ImmutableFieldReference("Lqmr;", "c", "Landroid/graphics/Point;");

    // ---------- tiny label-based assembler ----------
    static final class Asm {
        final ArrayList<Instruction> ins = new ArrayList<>();
        final HashMap<String, Integer> labels = new HashMap<>();
        static final class Fix {
            final int idx, at; final String label;
            Fix(int i, int a, String l) { idx = i; at = a; label = l; }
        }
        final ArrayList<Fix> fixups = new ArrayList<>();
        int addr = 0;

        void label(String n) { labels.put(n, addr); }
        void add(Instruction i) { ins.add(i); addr += i.getCodeUnits(); }
        void br21t(Opcode op, int r, String l) {
            fixups.add(new Fix(ins.size(), addr, l));
            add(new ImmutableInstruction21t(op, r, 0));
        }
        void br22t(Opcode op, int a, int b, String l) {
            fixups.add(new Fix(ins.size(), addr, l));
            add(new ImmutableInstruction22t(op, a, b, 0));
        }
        void br10t(Opcode op, String l) {
            fixups.add(new Fix(ins.size(), addr, l));
            add(new ImmutableInstruction10t(op, 0));
        }
        List<Instruction> finish() {
            if ((addr % 2) != 0) add(new ImmutableInstruction10x(Opcode.NOP));
            labels.put("STOCK", addr);
            for (Fix f : fixups) {
                Integer t = labels.get(f.label);
                if (t == null) throw new RuntimeException("unknown label " + f.label);
                int off = t - f.at;
                Instruction old = ins.get(f.idx);
                if (old instanceof ImmutableInstruction21t) {
                    ImmutableInstruction21t o = (ImmutableInstruction21t) old;
                    ins.set(f.idx, new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), off));
                } else if (old instanceof ImmutableInstruction22t) {
                    ImmutableInstruction22t o = (ImmutableInstruction22t) old;
                    ins.set(f.idx, new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(), o.getRegisterB(), off));
                } else if (old instanceof ImmutableInstruction10t) {
                    ImmutableInstruction10t o = (ImmutableInstruction10t) old;
                    ins.set(f.idx, new ImmutableInstruction10t(o.getOpcode(), off));
                } else throw new RuntimeException("fixup on non-branch " + old);
            }
            return ins;
        }
    }

    // ---------- the new Lqml.k() head ----------
    // k() regs: v10=this, v11=x(float), v12=y(float); scratch v0-v9.
    static List<Instruction> headBlock() {
        Asm a = new Asm();
        // ---- GUARD: emoji popup only (9 items, item[3] a non-BMP emoji) ----
        a.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 10, QML_E));
        a.br21t(Opcode.IF_EQZ, 0, "STOCK");
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_SIZE));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        a.add(new ImmutableInstruction21s(Opcode.CONST_16, 2, 9));
        a.br22t(Opcode.IF_NE, 1, 2, "STOCK");
        a.add(new ImmutableInstruction11n(Opcode.CONST_4, 1, 3));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0, LIST_GET));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        a.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 2, 1, PVA_TYPE));
        a.br21t(Opcode.IF_EQZ, 2, "STOCK");
        a.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, PVA_E));
        a.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, CHARSEQ_TYPE));
        a.br21t(Opcode.IF_EQZ, 3, "STOCK");
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CHARSEQ_LENGTH));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        a.br21t(Opcode.IF_EQZ, 3, "STOCK");
        a.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 3, 0, 0, 0, CHAR_CODEPOINTAT));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        a.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 2, 0x10000));
        a.br22t(Opcode.IF_LT, 3, 2, "STOCK");
        a.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 10, QML_D));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_ISEMPTY));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 0));
        a.br21t(Opcode.IF_NEZ, 0, "STOCK");
        // ---- TYPE GUARD: n() lives on Lqmu (Silk controller). A Material
        // popup (plain Lqml) must never reach the SEL below. ----
        a.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 0, 10, QMU_TYPE));
        a.br21t(Opcode.IF_EQZ, 0, "STOCK");
        // ---- VERIFIER FIX (176071836): INSTANCE_OF does NOT narrow the
        // static type for ART's verifier — only CHECK_CAST does. Without
        // this, the Lqmu->n() call in SEL fails verification of the whole
        // Lqml class (VerifyError, popup never appears). The guard above
        // already proves `this` IS a Lqmu at runtime, so this can never
        // throw; it only narrows v10's static type to Lqmu. ----
        a.add(new ImmutableInstruction21c(Opcode.CHECK_CAST, 10, QMU_TYPE));
        // ---- ANCHOR: finger's start point (stock anchor this.i=X, this.b=Y) ----
        a.add(new ImmutableInstruction22c(Opcode.IGET, 1, 10, QML_I));
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 1, 1));
        a.add(new ImmutableInstruction22c(Opcode.IGET, 3, 10, QML_B));
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 3, 3));
        a.br21t(Opcode.IF_NEZ, 1, "HAS_ANCHOR");
        a.br21t(Opcode.IF_EQZ, 3, "STOCK");  // anchor (0,0) = never set -> stock
        a.label("HAS_ANCHOR");
        // ---- GEOMETRY (once): contentW/H, cellW/H, centerX/Y ----
        // v9=container; v4=contentW; v5=contentH; v6=cellW; v7=cellH;
        // v8=centerX; v9=centerY (container dead after).
        a.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 9, 10, QML_C));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, LL_GETWIDTH));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 4));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADLEFT));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 4, 4, 2));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADRIGHT));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 4, 4, 2));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, LL_GETHEIGHT));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 5));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADTOP));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 5, 5, 2));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADBOTTOM));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 5, 5, 2));
        a.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 3));
        a.add(new ImmutableInstruction23x(Opcode.DIV_INT, 6, 4, 2));  // cellW
        a.add(new ImmutableInstruction23x(Opcode.DIV_INT, 7, 5, 2));  // cellH
        a.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 2));
        a.add(new ImmutableInstruction23x(Opcode.DIV_INT, 8, 4, 2));  // centerX
        a.add(new ImmutableInstruction23x(Opcode.DIV_INT, 9, 5, 2));  // centerY
        // (half-cell DIVs REMOVED: full-cell offsets below make them dead.)
        // ---- DELTA from the anchor: v0=dx, v2=dy; v1=-dx, v3=-dy ----
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 0, 11));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 0, 0, 1));
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 2, 12));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 2, 2, 3));
        a.add(new ImmutableInstruction12x(Opcode.NEG_INT, 1, 0));
        a.add(new ImmutableInstruction12x(Opcode.NEG_INT, 3, 2));
        // ---- DIRECTION + PIXEL CENTER (one flow; px->v6, py->v7) ----
        // center iff |dx|<cellW and |dy|<cellH. Direction pixels are the
        // EXACT CENTERS of the target cells (FULL cell offsets, not half):
        // UP=(centerX, centerY-cellH), DOWN=(centerX, centerY+cellH),
        // LEFT=(centerX-cellW, centerY), RIGHT=(centerX+cellW, centerY).
        a.br22t(Opcode.IF_GE, 0, 6, "NOT_CENTER");
        a.br22t(Opcode.IF_GE, 1, 6, "NOT_CENTER");
        a.br22t(Opcode.IF_GE, 2, 7, "NOT_CENTER");
        a.br22t(Opcode.IF_GE, 3, 7, "NOT_CENTER");
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 6, 8));   // CENTER: px=centerX
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 7, 9));   // py=centerY
        a.br10t(Opcode.GOTO, "SEL");
        a.label("NOT_CENTER");
        a.br22t(Opcode.IF_GT, 2, 0, "CHECK_DOWN");  // dy>dx -> not UP
        a.br22t(Opcode.IF_GT, 2, 1, "CHECK_DOWN");  // dy>-dx -> not UP
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 6, 8));   // UP: px=centerX
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 7, 9, 7)); // py=centerY-cellH
        a.br10t(Opcode.GOTO, "SEL");
        a.label("CHECK_DOWN");
        a.br22t(Opcode.IF_LT, 2, 0, "CHECK_LEFT");  // dy<dx -> not DOWN
        a.br22t(Opcode.IF_LT, 2, 1, "CHECK_LEFT");  // dy<-dx -> not DOWN
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 6, 8));   // DOWN: px=centerX
        a.add(new ImmutableInstruction23x(Opcode.ADD_INT, 7, 9, 7)); // py=centerY+cellH
        a.br10t(Opcode.GOTO, "SEL");
        a.label("CHECK_LEFT");
        a.br22t(Opcode.IF_GT, 0, 2, "IS_RIGHT");    // dx>dy -> not LEFT
        a.br22t(Opcode.IF_GT, 0, 3, "IS_RIGHT");    // dx>-dy -> not LEFT
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 6, 8, 6)); // LEFT: px=centerX-cellW
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 7, 9));   // py=centerY
        a.br10t(Opcode.GOTO, "SEL");
        a.label("IS_RIGHT");
        a.add(new ImmutableInstruction23x(Opcode.ADD_INT, 6, 8, 6)); // RIGHT: px=centerX+cellW
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 7, 9));   // py=centerY
        // ---- SELECT: drive the tracker, refresh the indicator, return ----
        a.label("SEL");
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 10, 6, 7, 0, 0, QML_I2));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 10, 0, 0, 0, 0, QMU_N));
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 10, 0, 0, 0, 0, QML_A));
        a.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        a.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0));
        List<Instruction> h = a.finish();
        int total = 0;
        for (Instruction i : h) total += i.getCodeUnits();
        if ((total % 2) != 0) throw new RuntimeException("k() head is ODD (" + total + ")");
        System.out.println("k() new head: " + h.size() + " insns, " + total + " units (even)");
        return h;
    }

    /** Replace the Simple Rewrite k() head (162 units, insns 0-90) with the
     *  new head; keep the stock body (insns 91+) verbatim — proven identical
     *  to the stock Lqml.k() by CompareKBody. */
    static MethodImplementation patchK(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 289 || impl.getRegisterCount() != 13) {
            System.out.println("Lqml.k: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.k: unexpected try blocks");
            return null;
        }
        if (insns.get(0).getOpcode() != Opcode.IGET_OBJECT
                || insns.get(87).getOpcode() != Opcode.INVOKE_VIRTUAL
                || insns.get(88).getOpcode() != Opcode.INVOKE_VIRTUAL
                || insns.get(90).getOpcode() != Opcode.RETURN_OBJECT
                || insns.get(91).getOpcode() != Opcode.IGET_OBJECT) {
            System.out.println("Lqml.k: boundary mismatch: " + insns.get(0).getOpcode()
                + " / " + insns.get(87).getOpcode() + " / " + insns.get(88).getOpcode()
                + " / " + insns.get(90).getOpcode() + " / " + insns.get(91).getOpcode());
            return null;
        }
        // insn 91 must read Lqml.c (the stock body start, proven by CompareKBody)
        if (!(insns.get(91) instanceof Instruction22c)) {
            System.out.println("Lqml.k: insn 91 is not IGET_OBJECT: " + insns.get(91));
            return null;
        }
        org.jf.dexlib2.iface.reference.FieldReference f91 =
            (org.jf.dexlib2.iface.reference.FieldReference)
            ((Instruction22c) insns.get(91)).getReference();
        if (!f91.getDefiningClass().equals("Lqml;") || !f91.getName().equals("c")
                || !f91.getType().equals("Landroid/widget/LinearLayout;")) {
            System.out.println("Lqml.k: insn 91 is not IGET Lqml.c: " + f91.getDefiningClass()
                + "->" + f91.getName() + ":" + f91.getType());
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        all.addAll(insns.subList(91, insns.size())); // stock body verbatim
        System.out.println("Lqml.k: old 162-unit head replaced by the fixed head; stock body kept");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    /** BUG #1 fix: in the shipped 176071834 Lqmu.c() (101 insns), retarget
     *  the loop-exit IF_GE (insn 26) from the p() call (insn 99) to the
     *  init-block start (insn 70). One branch, nothing else changes. */
    static MethodImplementation patchC(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 101 || impl.getRegisterCount() != 14) {
            System.out.println("Lqmu.c: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqmu.c: unexpected try blocks");
            return null;
        }
        for (Instruction i : insns) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                    || op == Opcode.FILL_ARRAY_DATA) {
                System.out.println("Lqmu.c: unexpected payload " + op);
                return null;
            }
        }
        List<Integer> absOff = new ArrayList<>();
        int off = 0;
        for (Instruction i : insns) { absOff.add(off); off += i.getCodeUnits(); }
        // shape: insn 26 IF_GE -> 99; insn 69 GOTO -> 24; insn 70 block start
        // (IGET Lqml.e); insn 99 INVOKE_DIRECT p(); insn 100 RETURN_VOID.
        if (!(insns.get(26) instanceof Instruction22t)
                || ((Instruction22t) insns.get(26)).getOpcode() != Opcode.IF_GE
                || absOff.get(26) + ((Instruction22t) insns.get(26)).getCodeOffset() != absOff.get(99)) {
            System.out.println("Lqmu.c: insn 26 is not IF_GE -> 99: " + insns.get(26));
            return null;
        }
        if (insns.get(69).getOpcode() != Opcode.GOTO
                || insns.get(70).getOpcode() != Opcode.IGET_OBJECT
                || insns.get(99).getOpcode() != Opcode.INVOKE_DIRECT
                || insns.get(100).getOpcode() != Opcode.RETURN_VOID) {
            System.out.println("Lqmu.c: boundary mismatch: " + insns.get(69).getOpcode()
                + " / " + insns.get(70).getOpcode() + " / " + insns.get(99).getOpcode()
                + " / " + insns.get(100).getOpcode());
            return null;
        }
        Instruction22t br = (Instruction22t) insns.get(26);
        int newOff = absOff.get(70) - absOff.get(26);
        List<Instruction> all = new ArrayList<>(insns);
        all.set(26, new ImmutableInstruction22t(br.getOpcode(),
            br.getRegisterA(), br.getRegisterB(), newOff));
        System.out.println("Lqmu.c: loop-exit IF_GE retargeted 99 -> 70 (init block now reachable); "
            + "new offset=" + newOff);
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    /** FIX #3 (animation): Lqmu.o() moves the green indicator. Replace the
     *  two direct setTranslationX/Y with 120ms ViewPropertyAnimator glides.
     *  Registers 6 -> 9 (v6=animator, v7+v8=long 120). The scale/alpha tail
     *  stays direct. IF_EQZ (insn 1) retargeted to the new RETURN_VOID. */
    static MethodImplementation patchO(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 25 || impl.getRegisterCount() != 6) {
            System.out.println("Lqmu.o: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqmu.o: unexpected try blocks");
            return null;
        }
        for (Instruction i : insns) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                    || op == Opcode.FILL_ARRAY_DATA) {
                System.out.println("Lqmu.o: unexpected payload " + op);
                return null;
            }
        }
        // shape: 0 IGET h; 1 IF_EQZ -> 24; 9 setTranslationX; 16 setTranslationY; 24 RETURN.
        if (insns.get(0).getOpcode() != Opcode.IGET_OBJECT
                || !(insns.get(1) instanceof Instruction21t)
                || ((Instruction21t) insns.get(1)).getOpcode() != Opcode.IF_EQZ
                || insns.get(9).getOpcode() != Opcode.INVOKE_VIRTUAL
                || insns.get(16).getOpcode() != Opcode.INVOKE_VIRTUAL
                || insns.get(24).getOpcode() != Opcode.RETURN_VOID) {
            System.out.println("Lqmu.o: boundary mismatch: " + insns.get(0).getOpcode()
                + " / " + insns.get(1).getOpcode() + " / " + insns.get(9).getOpcode()
                + " / " + insns.get(16).getOpcode() + " / " + insns.get(24).getOpcode());
            return null;
        }
        List<Integer> absOff = new ArrayList<>();
        int off = 0;
        for (Instruction i : insns) { absOff.add(off); off += i.getCodeUnits(); }
        if (absOff.get(1) + ((Instruction21t) insns.get(1)).getCodeOffset() != absOff.get(24)) {
            System.out.println("Lqmu.o: insn 1 IF_EQZ does not target 24");
            return null;
        }
        // verify the two replaced invokes are the setTranslation calls
        if (!(insns.get(9) instanceof Instruction35c)
                || !(insns.get(16) instanceof Instruction35c)) {
            System.out.println("Lqmu.o: insns 9/16 are not INVOKE_VIRTUAL");
            return null;
        }
        org.jf.dexlib2.iface.reference.MethodReference m9 =
            (org.jf.dexlib2.iface.reference.MethodReference)
            ((Instruction35c) insns.get(9)).getReference();
        org.jf.dexlib2.iface.reference.MethodReference m16 =
            (org.jf.dexlib2.iface.reference.MethodReference)
            ((Instruction35c) insns.get(16)).getReference();
        if (!m9.getDefiningClass().equals("Landroid/view/View;")
                || !m9.getName().equals("setTranslationX")
                || !m16.getDefiningClass().equals("Landroid/view/View;")
                || !m16.getName().equals("setTranslationY")) {
            System.out.println("Lqmu.o: insns 9/16 are not setTranslationX/Y: "
                + m9.getDefiningClass() + "->" + m9.getName() + " / "
                + m16.getDefiningClass() + "->" + m16.getName());
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        // insns 0..8 verbatim (v0=indicator view, v2=float targetX ready at 8)
        for (int i = 0; i <= 8; i++) all.add(insns.get(i));
        // X glide (replaces insn 9): animate().translationX(v2).setDuration(120).start()
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, VIEW_ANIMATE));
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6));
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 6, 2, 0, 0, 0, VPA_TRANSLATION_X));
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6));
        all.add(new ImmutableInstruction21s(Opcode.CONST_WIDE_16, 7, 120));
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 6, 7, 8, 0, 0, VPA_SET_DURATION));
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6));
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 6, 0, 0, 0, 0, VPA_START));
        // insns 10..15 verbatim (v0=indicator view, v5=float targetY ready at 15)
        for (int i = 10; i <= 15; i++) all.add(insns.get(i));
        // Y glide (replaces insn 16): animate().translationY(v5).setDuration(120).start()
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, VIEW_ANIMATE));
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6));
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 6, 5, 0, 0, 0, VPA_TRANSLATION_Y));
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6));
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 6, 7, 8, 0, 0, VPA_SET_DURATION));
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 6));
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 6, 0, 0, 0, 0, VPA_START));
        // insns 17..24 verbatim (scale/alpha tail + return)
        for (int i = 17; i <= 24; i++) all.add(insns.get(i));
        // retarget IF_EQZ (now index 1) to the new RETURN_VOID (last insn)
        List<Integer> newOff = new ArrayList<>();
        off = 0;
        for (Instruction i : all) { newOff.add(off); off += i.getCodeUnits(); }
        int retIdx = all.size() - 1;
        if (all.get(retIdx).getOpcode() != Opcode.RETURN_VOID) {
            System.out.println("Lqmu.o: last insn is not RETURN_VOID");
            return null;
        }
        int ifOff = newOff.get(retIdx) - newOff.get(1);
        all.set(1, new ImmutableInstruction21t(Opcode.IF_EQZ, 0, ifOff));
        int total = 0;
        for (Instruction i : all) total += i.getCodeUnits();
        System.out.println("Lqmu.o: setTranslationX/Y -> 120ms glides; " + all.size()
            + " insns, " + total + " units, regs 6->9, IF_EQZ -> " + retIdx);
        return new ImmutableMethodImplementation(
            9, all, impl.getTryBlocks(), impl.getDebugItems());
    }
}
