import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Fix3 (code 176071839) — revert Lqmu.o() to STOCK.
 *
 * ROOT CAUSE (proven from the shipped 176071838 dex): the stock
 * Lqmu.i(II)V already glides the indicator with a 300ms ObjectAnimatorSet
 * (translation X/Y from the OLD cell to the NEW cell, plus the scale
 * animators — the same set that makes the emoji scale-up work, which Sam
 * confirms works). The Simple Animated patch added a SECOND, competing
 * 120ms ViewPropertyAnimator on the SAME indicator's SAME properties
 * (translationX/Y) in o(), run on EVERY touch move via the k() head's
 * unconditional n() call. Two animators, same property, different
 * durations: the 120ms VPA reaches the target first and finishes; the
 * 300ms stock set keeps writing mid-flight values afterwards and pulls
 * the indicator BACKWARD -> the green circle jumps instead of gliding.
 *
 * FIX: remove the redundant/conflicting VPA — restore o() to the exact
 * stock implementation (25 insns, 6 regs, direct setTranslationX/Y),
 * taken from the shipped 176071834 dex. The stock 300ms AnimatorSet in
 * i() then becomes the SOLE driver of the indicator's translation and
 * glides it smoothly (the scale effect, driven by the same set, already
 * works on device). The k() head's n() call stays: with stock o() it is
 * a harmless one-time direct set that the running set overwrites on its
 * next frame (same as stock behaviour on the Lqmt.b() path).
 *
 * The stock body below is reconstructed instruction-by-instruction from
 * the 176071834 dexdump, and checkStock() asserts it is identical to the
 * actual 834 implementation (opcode + registers + refs) before anything
 * is written. If the assertion fails, the build fails loudly.
 */
public class RevertOPatch {
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

    /** The stock Lqmu.o() body, exactly as in the shipped 176071834 dex
     *  (25 insns, 50 code units, registers 6: v4=this, v5=param Lqmc).
     *  Insn indices in the comments are the 0-based instruction indices
     *  (not byte offsets). */
    static List<Instruction> stockO() {
        List<Instruction> all = new ArrayList<>();
        // (0) v0 = this.h (indicator view)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 4, QMU_H));
        // (1) if (v0 == null) return; offset = 0x31-0x02 = 47 code units
        all.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, 47));
        // (2) v5 = param.a (Point)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 5, 5, QMC_A));
        // (3) v1 = this.g (tracker)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 4, QMU_G));
        // (4) v2 = point.x
        all.add(new ImmutableInstruction22c(Opcode.IGET, 2, 5, POINT_X));
        // (5) v3 = tracker.b() (cellW)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, QMR_B));
        // (6) v3 = move-result
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // (7) v2 *= v3
        all.add(new ImmutableInstruction12x(Opcode.MUL_INT_2ADDR, 2, 3));
        // (8) v2 = (float) v2
        all.add(new ImmutableInstruction12x(Opcode.INT_TO_FLOAT, 2, 2));
        // (9) view.setTranslationX(v2) — DIRECT set (stock)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 2, 0, 0, 0, VIEW_SET_TX));
        // (10) v0 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 4, QMU_H));
        // (11) v5 = point.y
        all.add(new ImmutableInstruction22c(Opcode.IGET, 5, 5, POINT_Y));
        // (12) v1 = tracker.a() (cellH)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, QMR_A));
        // (13) v1 = move-result
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // (14) v5 *= v1
        all.add(new ImmutableInstruction12x(Opcode.MUL_INT_2ADDR, 5, 1));
        // (15) v5 = (float) v5
        all.add(new ImmutableInstruction12x(Opcode.INT_TO_FLOAT, 5, 5));
        // (16) view.setTranslationY(v5) — DIRECT set (stock)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 5, 0, 0, 0, VIEW_SET_TY));
        // (17) v5 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 5, 4, QMU_H));
        // (18) v0 = 1.0f (0x3F800000; 21ih takes the full 32-bit literal,
        // low 16 bits zeroed)
        all.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 0, 0x3F800000));
        // (19) view.setScaleX(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 0, 0, 0, 0, VIEW_SET_SX));
        // (20) v5 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 5, 4, QMU_H));
        // (21) view.setScaleY(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 0, 0, 0, 0, VIEW_SET_SY));
        // (22) v4 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 4, QMU_H));
        // (23) view.setAlpha(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 4, 0, 0, 0, 0, VIEW_SET_ALPHA));
        // (24) return
        all.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
        int total = 0;
        for (Instruction i : all) total += i.getCodeUnits();
        if (all.size() != 25 || total != 50) {
            throw new RuntimeException("stockO shape wrong: ins=" + all.size()
                + " units=" + total);
        }
        return all;
    }

    /** Canonical one-line form of an instruction (opcode + registers +
     *  reference/literal) for the stock-equality check. */
    static String canon(Instruction ins) {
        StringBuilder sb = new StringBuilder(ins.getOpcode().name());
        if (ins instanceof Instruction10x) {
            // no operands
        } else if (ins instanceof Instruction11x) {
            sb.append(" r").append(((Instruction11x) ins).getRegisterA());
        } else if (ins instanceof Instruction12x) {
            Instruction12x i = (Instruction12x) ins;
            sb.append(" r").append(i.getRegisterA()).append(",r").append(i.getRegisterB());
        } else if (ins instanceof Instruction21t) {
            Instruction21t i = (Instruction21t) ins;
            sb.append(" r").append(i.getRegisterA()).append(" off=").append(i.getCodeOffset());
        } else if (ins instanceof Instruction21ih) {
            Instruction21ih i = (Instruction21ih) ins;
            sb.append(" r").append(i.getRegisterA()).append(" lit=").append(i.getNarrowLiteral());
        } else if (ins instanceof Instruction22c) {
            Instruction22c i = (Instruction22c) ins;
            sb.append(" r").append(i.getRegisterA()).append(",r").append(i.getRegisterB())
                .append(" ").append(i.getReference());
        } else if (ins instanceof Instruction23x) {
            Instruction23x i = (Instruction23x) ins;
            sb.append(" r").append(i.getRegisterA()).append(",r").append(i.getRegisterB());
        } else if (ins instanceof Instruction35c) {
            Instruction35c i = (Instruction35c) ins;
            sb.append(" n=").append(i.getRegisterCount());
            int[] rr = {i.getRegisterC(), i.getRegisterD(), i.getRegisterE(),
                i.getRegisterF(), i.getRegisterG()};
            for (int r = 0; r < i.getRegisterCount(); r++) sb.append(",r").append(rr[r]);
            sb.append(" ").append(i.getReference());
        } else {
            sb.append(" ?").append(ins.getClass().getSimpleName());
        }
        return sb.toString();
    }

    /** Assert the reconstruction equals the actual 834 implementation,
     *  instruction by instruction. Returns true on exact match. */
    static boolean checkStock(MethodImplementation stockImpl) {
        List<Instruction> actual = new ArrayList<>();
        for (Instruction i : stockImpl.getInstructions()) actual.add(i);
        List<Instruction> mine = stockO();
        if (actual.size() != mine.size()) {
            System.out.println("checkStock: size mismatch actual=" + actual.size()
                + " mine=" + mine.size());
            return false;
        }
        for (int k = 0; k < actual.size(); k++) {
            String a = canon(actual.get(k)), m = canon(mine.get(k));
            if (!a.equals(m)) {
                System.out.println("checkStock: insn " + k + " mismatch:\n  actual=" + a
                    + "\n  mine  =" + m);
                return false;
            }
        }
        System.out.println("checkStock: reconstruction == actual 834 o() ("
            + actual.size() + " insns)");
        return true;
    }

    /** Replace the 838 glide o() with the stock o(). Asserts the input is
     *  the 838 glide version (39 insns, 9 regs, VPA translationX) and the
     *  stock impl passes checkStock(). */
    static MethodImplementation revertO(MethodImplementation impl,
            MethodImplementation stockImpl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 39 || impl.getRegisterCount() != 9) {
            System.out.println("Lqmu.o: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount() + " (expected 39/9, the 838 glide)");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqmu.o: unexpected try blocks");
            return null;
        }
        // confirm this is the glide version: it must invoke
        // ViewPropertyAnimator.translationX somewhere
        boolean hasVpa = false;
        for (Instruction i : insns) {
            if (i instanceof Instruction35c) {
                org.jf.dexlib2.iface.reference.Reference ref =
                    ((Instruction35c) i).getReference();
                if (ref instanceof MethodReference) {
                    MethodReference mr = (MethodReference) ref;
                    if (mr.getDefiningClass().equals("Landroid/view/ViewPropertyAnimator;")
                            && mr.getName().equals("translationX")) hasVpa = true;
                }
            }
        }
        if (!hasVpa) {
            System.out.println("Lqmu.o: no VPA translationX found — not the glide version");
            return null;
        }
        if (!checkStock(stockImpl)) {
            System.out.println("Lqmu.o: stock reconstruction check FAILED");
            return null;
        }
        System.out.println("Lqmu.o: glide (39 insns, 9 regs, VPA) -> stock "
            + "(25 insns, 6 regs, direct setTranslationX/Y)");
        return new ImmutableMethodImplementation(
            6, stockO(), impl.getTryBlocks(), impl.getDebugItems());
    }
}
