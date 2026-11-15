import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Fix4 (code 176071840) — center the green highlight circle (Lqmu.h) exactly
 * on the selected emoji cell, for ALL FIVE positions.
 *
 * ROOT CAUSE (proven from the shipped 176071839 dex + decoded res):
 * Lqmu.o() and Lqmu.i()'s AnimatorSet both set the indicator's translation
 * to (col * cellW, row * cellH) — the TOP-LEFT corner of the target cell.
 * But the indicator view is LARGER than the cell: the cell is 28x28dp
 * (Lqmf.c/d via Lqmr.b()/a()), while the indicator (style 0x7f15063d/63e)
 * is 40x40dp (or 34-46dp by theme/variant). Top-left anchoring therefore
 * puts the indicator's CENTER (indicator-cell)/2 to the RIGHT and DOWN of
 * the cell's center. Sam's screenshot measures exactly that: the circle
 * sits ~5dp right / ~3dp down from the emoji. (X and Y differ because the
 * indicator/cell need not be square in every theme/variant — the runtime
 * correction below reads each axis separately, so the discrepancy needs no
 * static resolution.)
 *
 * FIX: add the centering correction (cell - indicator)/2 to BOTH axes,
 * computed at RUNTIME from the real sizes (so it is correct for every
 * theme/variant and every position by construction — NOT a hardcoded nudge):
 *   translationX = col*cellW + (cellW - indicatorW)/2
 *   translationY = row*cellH + (cellH - indicatorH)/2
 * where indicatorW/H come from the indicator view's LayoutParams
 * (available immediately after inflation, unlike getWidth()/getHeight()
 * which are 0 before layout at open) and cellW/H from Lqmr.b()/a()
 * (the same values the translation already uses).
 *
 * Applied in BOTH places that position the indicator, or the resting
 * position would stay wrong:
 *   (a) Lqmu.o(Lqmc;)V — the direct set (at-open placement via c()->p()
 *       and the per-move refresh via k()->n()). Replaced wholesale:
 *       6 -> 8 regs (this v4->v6, param v5->v7, scratch v4/v5).
 *   (b) Lqmu.i(II)V — the stock 300ms AnimatorSet's end values (the
 *       resting position after every swipe). 21 insns / 38 units inserted
 *       before insn 297 (abs 0x21a, the translation AnimatorSet's
 *       new-instance); the 5 branches crossing the insertion point are
 *       retargeted (+38). 38 is EVEN, so no payload misalignment.
 * The glide (stock 300ms AnimatorSet), the scale-up, the tracker, and
 * everything else are untouched.
 *
 * NEW refs: exactly 2 FIELD refs (ViewGroup$LayoutParams.width/height —
 * field_ids has room, 45404 < 65536). ZERO new method refs (method_ids is
 * full at 65536): View.getLayoutParams() already exists (method@1e2b).
 * View.getWidth()/getHeight() are NOT used (not referenced, and 0 before
 * layout at open anyway).
 */
public class Fix4CenterPatch {
    static final ImmutableFieldReference QMU_H =
        new ImmutableFieldReference("Lqmu;", "h", "Landroid/view/View;");
    static final ImmutableFieldReference QMC_A =
        new ImmutableFieldReference("Lqmc;", "a", "Landroid/graphics/Point;");
    static final ImmutableFieldReference QMU_G =
        new ImmutableFieldReference("Lqmu;", "g", "Lqmr;");
    static final ImmutableFieldReference POINT_X =
        new ImmutableFieldReference("Landroid/graphics/Point;", "x", "I");
    static final ImmutableFieldReference POINT_Y =
        new ImmutableFieldReference("Landroid/graphics/Point;", "y", "I");
    static final ImmutableMethodReference QMR_B =
        new ImmutableMethodReference("Lqmr;", "b",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference QMR_A =
        new ImmutableMethodReference("Lqmr;", "a",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_SET_TX =
        new ImmutableMethodReference("Landroid/view/View;", "setTranslationX",
            java.util.Arrays.asList("F"), "V");
    static final ImmutableMethodReference VIEW_SET_TY =
        new ImmutableMethodReference("Landroid/view/View;", "setTranslationY",
            java.util.Arrays.asList("F"), "V");
    static final ImmutableMethodReference VIEW_SET_SX =
        new ImmutableMethodReference("Landroid/view/View;", "setScaleX",
            java.util.Arrays.asList("F"), "V");
    static final ImmutableMethodReference VIEW_SET_SY =
        new ImmutableMethodReference("Landroid/view/View;", "setScaleY",
            java.util.Arrays.asList("F"), "V");
    static final ImmutableMethodReference VIEW_SET_ALPHA =
        new ImmutableMethodReference("Landroid/view/View;", "setAlpha",
            java.util.Arrays.asList("F"), "V");
    static final ImmutableMethodReference VIEW_GET_LAYOUT_PARAMS =
        new ImmutableMethodReference("Landroid/view/View;", "getLayoutParams",
            java.util.Collections.emptyList(), "Landroid/view/ViewGroup$LayoutParams;");
    static final ImmutableFieldReference LP_WIDTH =
        new ImmutableFieldReference("Landroid/view/ViewGroup$LayoutParams;", "width", "I");
    static final ImmutableFieldReference LP_HEIGHT =
        new ImmutableFieldReference("Landroid/view/ViewGroup$LayoutParams;", "height", "I");

    // ---------- new Lqmu.o() (8 regs: this=v6, param=v7, scratch v4/v5) ----------
    // 37 insns, 72 units. IF_EQZ offset is computed from the built list.
    static List<Instruction> newO() {
        List<Instruction> all = new ArrayList<>();
        // (0) v0 = this.h (indicator)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 6, QMU_H));
        // (1) if (v0 == null) return — offset patched below
        all.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, 0));
        // (2) v7 = param.a (Point)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 7, 7, QMC_A));
        // (3) v1 = this.g (tracker)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 6, QMU_G));
        // (4) v2 = point.x
        all.add(new ImmutableInstruction22c(Opcode.IGET, 2, 7, POINT_X));
        // (5) v3 = tracker.b() (cellW)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, QMR_B));
        // (6) v3 = move-result
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // (7) v2 *= v3  (X = col*cellW)
        all.add(new ImmutableInstruction12x(Opcode.MUL_INT_2ADDR, 2, 3));
        // (8) v4 = indicator.getLayoutParams()
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        // (9) v4 = move-result-object
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        // (10) v4 = params.width (indicatorW)
        all.add(new ImmutableInstruction22c(Opcode.IGET, 4, 4, LP_WIDTH));
        // (11) v5 = cellW - indicatorW
        all.add(new ImmutableInstruction23x(Opcode.SUB_INT, 5, 3, 4));
        // (12) v5 /= 2 (corrX)
        all.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 5, 5, 2));
        // (13) v2 += v5 (X += corrX)
        all.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 2, 5));
        // (14) v2 = (float) v2
        all.add(new ImmutableInstruction12x(Opcode.INT_TO_FLOAT, 2, 2));
        // (15) view.setTranslationX(v2)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 2, 0, 0, 0, VIEW_SET_TX));
        // (16) v0 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 6, QMU_H));
        // (17) v5 = point.y
        all.add(new ImmutableInstruction22c(Opcode.IGET, 5, 7, POINT_Y));
        // (18) v1 = tracker.a() (cellH)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, QMR_A));
        // (19) v1 = move-result
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // (20) v5 *= v1 (Y = row*cellH)
        all.add(new ImmutableInstruction12x(Opcode.MUL_INT_2ADDR, 5, 1));
        // (21) v4 = indicator.getLayoutParams()
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        // (22) v4 = move-result-object
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        // (23) v4 = params.height (indicatorH)
        all.add(new ImmutableInstruction22c(Opcode.IGET, 4, 4, LP_HEIGHT));
        // (24) v3 = cellH - indicatorH
        all.add(new ImmutableInstruction23x(Opcode.SUB_INT, 3, 1, 4));
        // (25) v3 /= 2 (corrY)
        all.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 3, 3, 2));
        // (26) v5 += v3 (Y += corrY)
        all.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 5, 3));
        // (27) v5 = (float) v5
        all.add(new ImmutableInstruction12x(Opcode.INT_TO_FLOAT, 5, 5));
        // (28) view.setTranslationY(v5)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 5, 0, 0, 0, VIEW_SET_TY));
        // (29) v5 = this.h (tail: scale/alpha 1.0f)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 5, 6, QMU_H));
        // (30) v0 = 1.0f
        all.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 0, 0x3F800000));
        // (31) view.setScaleX(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 0, 0, 0, 0, VIEW_SET_SX));
        // (32) v5 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 5, 6, QMU_H));
        // (33) view.setScaleY(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 0, 0, 0, 0, VIEW_SET_SY));
        // (34) v4 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 6, QMU_H));
        // (35) view.setAlpha(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 4, 0, 0, 0, 0, VIEW_SET_ALPHA));
        // (36) return
        all.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
        // fix the IF_EQZ offset: return addr - ifeqz addr
        int total = 0;
        List<Integer> addr = new ArrayList<>();
        for (Instruction i : all) { addr.add(total); total += i.getCodeUnits(); }
        if (all.size() != 37 || total != 72) {
            throw new RuntimeException("newO shape wrong: ins=" + all.size()
                + " units=" + total + " (expected 37/72)");
        }
        int returnAddr = addr.get(36);
        int ifeqzAddr = addr.get(1);
        int off = returnAddr - ifeqzAddr;
        all.set(1, new ImmutableInstruction21t(Opcode.IF_EQZ, 0, off));
        System.out.println("newO: 37 insns, 72 units, IF_EQZ offset=" + off
            + " (return at " + returnAddr + ")");
        return all;
    }

    /** Replace the stock Lqmu.o() (25 insns, 6 regs) with the centered
     *  version (37 insns, 8 regs). Verifies the input IS the stock o()
     *  via RevertOPatch.checkStock(). */
    static MethodImplementation patchO(MethodImplementation impl,
            MethodImplementation stockImpl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 25 || impl.getRegisterCount() != 6) {
            System.out.println("Lqmu.o: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount() + " (expected stock 25/6)");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqmu.o: unexpected try blocks");
            return null;
        }
        if (!RevertOPatch.checkStock(impl)) {
            System.out.println("Lqmu.o: input is NOT the stock o() — refusing");
            return null;
        }
        System.out.println("Lqmu.o: stock (25 insns, 6 regs) -> centered "
            + "(37 insns, 8 regs, LayoutParams centering)");
        return new ImmutableMethodImplementation(
            8, newO(), impl.getTryBlocks(), impl.getDebugItems());
    }

    // ---------- Lqmu.i() insertion (before insn idx 297, abs 0x21a) ----------
    // 20 insns + NOP = 38 units (EVEN). Scratch v11/v13/v14 ONLY (low regs:
    // field access 22c and arithmetic 12x/23x/22b are 4-bit, so high regs
    // v16+ cannot be used as operands). v11 is UNUSED after 0x21a; v13/v14
    // are overwritten (0x21f/0x221) before being read, so their values at
    // 0x21a are dead. v5 = Lqmu (live, read-only here); v9/v12 = old/new X,
    // v6/v7 = old/new Y (the values being corrected).
    // LayoutParams is fetched twice (once per axis) to stay within 3 scratch
    // regs; getLayoutParams() is cheap and the object is the same.
    static final int I_INSERT_IDX = 297;
    static final int I_INSERT_ADDR = 0x21a;

    static List<Instruction> centerBlockI() {
        List<Instruction> b = new ArrayList<>();
        // ---- X axis ----
        // v14 = indicator (v5.h)
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 14, 5, QMU_H));
        // v11 = indicator.getLayoutParams()
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 14, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 11));
        // v13 = params.width (indicatorW)
        b.add(new ImmutableInstruction22c(Opcode.IGET, 13, 11, LP_WIDTH));
        // v11 = tracker (v5.g)
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 11, 5, QMU_G));
        // v14 = tracker.b() (cellW)
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 11, 0, 0, 0, 0, QMR_B));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 14));
        // v14 = cellW - indicatorW
        b.add(new ImmutableInstruction23x(Opcode.SUB_INT, 14, 14, 13));
        // v14 /= 2 (corrX)
        b.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 14, 14, 2));
        // oldX += corrX ; newX += corrX
        b.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 9, 14));
        b.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 12, 14));
        // ---- Y axis ----
        // v13 = indicator (v5.h)
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 13, 5, QMU_H));
        // v13 = indicator.getLayoutParams()
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 13, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 13));
        // v13 = params.height (indicatorH)
        b.add(new ImmutableInstruction22c(Opcode.IGET, 13, 13, LP_HEIGHT));
        // v14 = tracker.a() (cellH) — v11 still holds the tracker
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 11, 0, 0, 0, 0, QMR_A));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 14));
        // v14 = cellH - indicatorH
        b.add(new ImmutableInstruction23x(Opcode.SUB_INT, 14, 14, 13));
        // v14 /= 2 (corrY)
        b.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 14, 14, 2));
        // oldY += corrY ; newY += corrY
        b.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 6, 14));
        b.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 7, 14));
        int total = 0;
        for (Instruction i : b) total += i.getCodeUnits();
        // Pad with one NOP if the total is ODD so the insert stays EVEN
        // (payload alignment: an odd insert would shift later payloads odd).
        boolean padded = false;
        if ((total % 2) != 0) {
            b.add(new ImmutableInstruction10x(Opcode.NOP));
            total += 1;
            padded = true;
        }
        if (b.size() != 21 || total != 38) {
            throw new RuntimeException("centerBlockI shape wrong: ins=" + b.size()
                + " units=" + total + " (expected 21/38)");
        }
        System.out.println("centerBlockI: 21 insns" + (padded ? " (incl. NOP pad)" : "")
            + ", 38 units (even)");
        return b;
    }

    /** Rebuild a branch instruction with a new code offset. */
    static Instruction retarget(Instruction ins, int newOff) {
        if (ins instanceof Instruction21t) {
            Instruction21t o = (Instruction21t) ins;
            return new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), newOff);
        } else if (ins instanceof Instruction22t) {
            Instruction22t o = (Instruction22t) ins;
            return new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(),
                o.getRegisterB(), newOff);
        } else if (ins instanceof Instruction10t) {
            Instruction10t o = (Instruction10t) ins;
            return new ImmutableInstruction10t(o.getOpcode(), newOff);
        } else if (ins instanceof Instruction20t) {
            Instruction20t o = (Instruction20t) ins;
            return new ImmutableInstruction20t(o.getOpcode(), newOff);
        } else if (ins instanceof Instruction30t) {
            Instruction30t o = (Instruction30t) ins;
            return new ImmutableInstruction30t(o.getOpcode(), newOff);
        }
        throw new RuntimeException("retarget on non-branch " + ins.getOpcode());
    }

    /** Insert the centering block into Lqmu.i() before insn 297 (abs 0x21a),
     *  retargeting every branch that crosses the insertion point. */
    static MethodImplementation patchI(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 383 || impl.getRegisterCount() != 27) {
            System.out.println("Lqmu.i: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount() + " (expected 383/27)");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqmu.i: unexpected try blocks");
            return null;
        }
        for (Instruction i : insns) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                    || op == Opcode.FILL_ARRAY_DATA) {
                System.out.println("Lqmu.i: unexpected payload " + op);
                return null;
            }
        }
        // boundary: insn 296 = mul-int/2addr v7,v8 (newY); insn 297 =
        // new-instance v8, AnimatorSet; insn 298 = its <init>.
        if (insns.get(296).getOpcode() != Opcode.MUL_INT_2ADDR
                || insns.get(297).getOpcode() != Opcode.NEW_INSTANCE
                || insns.get(298).getOpcode() != Opcode.INVOKE_DIRECT) {
            System.out.println("Lqmu.i: boundary mismatch: "
                + insns.get(296).getOpcode() + " / " + insns.get(297).getOpcode()
                + " / " + insns.get(298).getOpcode());
            return null;
        }
        List<Integer> absOff = new ArrayList<>();
        int off = 0;
        for (Instruction i : insns) { absOff.add(off); off += i.getCodeUnits(); }
        if (absOff.get(297) != I_INSERT_ADDR) {
            System.out.println("Lqmu.i: insn 297 is at abs " + absOff.get(297)
                + ", expected 0x21a");
            return null;
        }
        List<Instruction> block = centerBlockI();
        int delta = 0;
        for (Instruction i : block) delta += i.getCodeUnits();
        int retargeted = 0;
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < insns.size(); i++) {
            if (i == I_INSERT_IDX) all.addAll(block); // insert BEFORE insn 297
            Instruction ins = insns.get(i);
            if (ins instanceof OffsetInstruction) {
                int a = absOff.get(i);
                int tgt = a + ((OffsetInstruction) ins).getCodeOffset();
                boolean crossFwd = a < I_INSERT_ADDR && tgt >= I_INSERT_ADDR;
                boolean crossBwd = a >= I_INSERT_ADDR && tgt < I_INSERT_ADDR;
                if (crossFwd || crossBwd) {
                    int newOff = ((OffsetInstruction) ins).getCodeOffset()
                        + (crossFwd ? delta : -delta);
                    all.add(retarget(ins, newOff));
                    retargeted++;
                    System.out.println("Lqmu.i: retargeted branch idx " + i
                        + " abs " + Integer.toHexString(a) + " -> tgt "
                        + Integer.toHexString(tgt) + " newOff " + newOff);
                    continue;
                }
            }
            all.add(ins);
        }
        if (retargeted != 5) {
            System.out.println("Lqmu.i: expected exactly 5 crossing branches, found "
                + retargeted);
            return null;
        }
        System.out.println("Lqmu.i: centering block inserted before insn 297 "
            + "(+" + delta + " units), " + retargeted + " branches retargeted");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }
}
