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
 * Simple Rewrite (code 176071834) — the whole long-press + swipe selection
 * in ONE small, clear implementation.
 *
 * THE SELECTION FLOW, in plain words (this is all of it):
 *   1. At open, the tracker starts on the CENTER cell (1,1).
 *      (Lqmu.c() hook — kept from 2Fix4; stock starts it at (1,2) = DOWN.)
 *   2. On touch, take the swipe DELTA from the finger's start point
 *      (stock anchor this.i/this.b): at rest the delta is ~0 -> CENTER;
 *      a swipe in a direction picks that direction's emoji, however far.
 *   3. Call this.i(px,py) with the PIXEL CENTER of the target cell.
 *      i() takes container-content-local PIXELS (proven: stock k() clamps
 *      the touch to content dims and passes those pixels; Lqmu.i clamps
 *      them again and feeds them to the pixel-based nearest-item search
 *      Lqme.a). Virtual dispatch sends it to Lqmu.i for the Silk popup
 *      (tracker moves, green circle moves, a() returns the swiped emoji)
 *      and to Lqml.i for Material. Then return a().
 *   4. Nothing else. The Lqml.e() post-block that forced g=3 is REMOVED:
 *      it only ever served Material popups (Lqml.a() = e.get(g)), and
 *      Sam's popup is Silk, where g is never read (Lqmu.a() ignores g).
 *      The e() head that sets the oval (circle) background is KEPT.
 *
 * What was removed vs 2Fix4:
 *   - Lqml.e() post-block (insns 59-92 in 2Fix4): deleted; insn 41 retargeted
 *     to the RETURN_VOID, insn 58 GOTO restored to RETURN_VOID.
 *   - Lqml.k() 200-unit head: replaced by a ~168-unit head with the same
 *     behavior but one geometry (contentW/H -> cellW/H, center, half-cells
 *     computed once) and one direction->pixel flow (no index dispatch).
 *   - Lqmu.c() init hook: UNCHANGED (already minimal and correct).
 *
 * Registers in k() (13 regs: v10=this, v11=x float, v12=y float):
 *   v0=dx, v1=anchorX then -dx then (dead), v2=dy then temp,
 *   v3=anchorY then -dy, v4=contentW then halfCellW, v5=contentH then
 *   halfCellH, v6=cellW then px, v7=cellH then py, v8=centerX, v9=container
 *   then centerY. v10/v11/v12 are only READ (stock k() overwrites them
 *   itself); v0-v9 are scratch the stock body writes before reading.
 * Zero new refs. One dexlib2 pass. Heads are EVEN (NOP-padded by Asm).
 */
public class SimplePopupPatch {
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
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");

    // ---------- tiny label-based assembler ----------
    /** Build a hook head with named labels; finish() resolves branch
     *  offsets and pads the total to an EVEN number of code units. */
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
        // ---- ANCHOR: finger's start point (stock anchor this.i=X, this.b=Y) ----
        a.add(new ImmutableInstruction22c(Opcode.IGET, 1, 10, QML_I));
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 1, 1));
        a.add(new ImmutableInstruction22c(Opcode.IGET, 3, 10, QML_B));
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 3, 3));
        a.br21t(Opcode.IF_NEZ, 1, "HAS_ANCHOR");
        a.br21t(Opcode.IF_EQZ, 3, "STOCK");  // anchor (0,0) = never set -> stock
        a.label("HAS_ANCHOR");
        // ---- GEOMETRY (once): contentW/H, cellW/H, centerX/Y, halfCellW/H ----
        // v9=container; v4=contentW->halfCellW; v5=contentH->halfCellH;
        // v6=cellW; v7=cellH; v8=centerX; v9=centerY (container dead after).
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
        a.add(new ImmutableInstruction23x(Opcode.DIV_INT, 4, 6, 2));  // halfCellW
        a.add(new ImmutableInstruction23x(Opcode.DIV_INT, 5, 7, 2));  // halfCellH
        // ---- DELTA from the anchor: v0=dx, v2=dy; v1=-dx, v3=-dy ----
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 0, 11));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 0, 0, 1));
        a.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 2, 12));
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 2, 2, 3));
        a.add(new ImmutableInstruction12x(Opcode.NEG_INT, 1, 0));
        a.add(new ImmutableInstruction12x(Opcode.NEG_INT, 3, 2));
        // ---- DIRECTION + PIXEL CENTER (one flow; px->v6, py->v7) ----
        // center iff |dx|<cellW and |dy|<cellH
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
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 7, 9, 5)); // py=centerY-halfCellH
        a.br10t(Opcode.GOTO, "SEL");
        a.label("CHECK_DOWN");
        a.br22t(Opcode.IF_LT, 2, 0, "CHECK_LEFT");  // dy<dx -> not DOWN
        a.br22t(Opcode.IF_LT, 2, 1, "CHECK_LEFT");  // dy<-dx -> not DOWN
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 6, 8));   // DOWN: px=centerX
        a.add(new ImmutableInstruction23x(Opcode.ADD_INT, 7, 9, 5)); // py=centerY+halfCellH
        a.br10t(Opcode.GOTO, "SEL");
        a.label("CHECK_LEFT");
        a.br22t(Opcode.IF_GT, 0, 2, "IS_RIGHT");    // dx>dy -> not LEFT
        a.br22t(Opcode.IF_GT, 0, 3, "IS_RIGHT");    // dx>-dy -> not LEFT
        a.add(new ImmutableInstruction23x(Opcode.SUB_INT, 6, 8, 4)); // LEFT: px=centerX-halfCellW
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 7, 9));   // py=centerY
        a.br10t(Opcode.GOTO, "SEL");
        a.label("IS_RIGHT");
        a.add(new ImmutableInstruction23x(Opcode.ADD_INT, 6, 8, 4)); // RIGHT: px=centerX+halfCellW
        a.add(new ImmutableInstruction12x(Opcode.MOVE, 7, 9));   // py=centerY
        // ---- SELECT: drive the tracker, return the selected item ----
        a.label("SEL");
        a.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 10, 6, 7, 0, 0, QML_I2));
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

    /** Replace the 2Fix4 k() head (200 units, insns 0-118, NOP pad at 118)
     *  with the new head; keep the stock body (insns 119+) verbatim. */
    static MethodImplementation patchK(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 317 || impl.getRegisterCount() != 13) {
            System.out.println("Lqml.k: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.k: unexpected try blocks");
            return null;
        }
        if (insns.get(0).getOpcode() != Opcode.IGET_OBJECT
                || insns.get(118).getOpcode() != Opcode.NOP
                || insns.get(119).getOpcode() != Opcode.IGET_OBJECT) {
            System.out.println("Lqml.k: boundary mismatch: " + insns.get(0).getOpcode()
                + " / " + insns.get(118).getOpcode() + " / " + insns.get(119).getOpcode());
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        all.addAll(insns.subList(119, insns.size())); // stock body verbatim
        System.out.println("Lqml.k: old 200-unit head replaced by the simple head; stock body kept");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    /** Remove the Lqml.e() post-block (2Fix2) that forced g=3.
     *  2Fix4 e(): insns 0-37 = oval-background head (KEPT), 38-58 = stock
     *  body, 59-92 = post-block, 93 = RETURN_VOID. Only insns 41 and 58
     *  branch into the post-block; both are fixed here. Result: the stock
     *  e() body (+ the circle-background head) and nothing else. */
    static MethodImplementation patchE(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 94 || impl.getRegisterCount() != 6) {
            System.out.println("Lqml.e: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        // absolute addresses
        List<Integer> absOff = new ArrayList<>();
        int off = 0;
        for (Instruction i : insns) { absOff.add(off); off += i.getCodeUnits(); }
        // shape checks: insn 41 = IF_NEZ -> 59; insn 58 = GOTO -> 59; insn 93 = RETURN_VOID
        if (!(insns.get(41) instanceof Instruction21t)
                || ((Instruction21t) insns.get(41)).getOpcode() != Opcode.IF_NEZ
                || absOff.get(41) + ((Instruction21t) insns.get(41)).getCodeOffset() != absOff.get(59)) {
            System.out.println("Lqml.e: insn 41 is not IF_NEZ -> 59: " + insns.get(41));
            return null;
        }
        if (!(insns.get(58) instanceof Instruction10t)
                || ((Instruction10t) insns.get(58)).getOpcode() != Opcode.GOTO
                || absOff.get(58) + ((Instruction10t) insns.get(58)).getCodeOffset() != absOff.get(59)) {
            System.out.println("Lqml.e: insn 58 is not GOTO -> 59: " + insns.get(58));
            return null;
        }
        if (insns.get(93).getOpcode() != Opcode.RETURN_VOID) {
            System.out.println("Lqml.e: insn 93 is not RETURN_VOID: " + insns.get(93));
            return null;
        }
        // no other branch in 0..58 may target the post-block (59..92)
        for (int i = 0; i < 59; i++) {
            Instruction in = insns.get(i);
            if (i == 41 || i == 58) continue;
            if (in instanceof OffsetInstruction) {
                int tgt = absOff.get(i) + ((OffsetInstruction) in).getCodeOffset();
                if (tgt >= absOff.get(59) && tgt < absOff.get(93)) {
                    System.out.println("Lqml.e: insn " + i + " also branches into the post-block");
                    return null;
                }
            }
        }
        // try blocks must not cover the deleted range
        for (org.jf.dexlib2.iface.TryBlock tb : impl.getTryBlocks()) {
            int s = tb.getStartCodeAddress(), e = s + tb.getCodeUnitCount();
            if (s < absOff.get(93) && e > absOff.get(59)) {
                System.out.println("Lqml.e: try block overlaps the post-block");
                return null;
            }
        }
        // rebuild: insns 0..57 verbatim, insn 41 retargeted to the new
        // RETURN_VOID (index 58), insn 58 = RETURN_VOID, tail deleted.
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < 58; i++) {
            Instruction in = insns.get(i);
            if (i == 41) {
                int newOff = absOff.get(58) - absOff.get(41);
                all.add(new ImmutableInstruction21t(Opcode.IF_NEZ, 1, newOff));
            } else {
                all.add(in);
            }
        }
        all.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
        System.out.println("Lqml.e: post-block (insns 59-92) removed; e() is stock body + circle head");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }
}
