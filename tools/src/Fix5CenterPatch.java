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
 * Fix5 (code 176071841) — FIX THE FIX4 REGRESSION (popup gone on long-press).
 *
 * VERIFIED ROOT CAUSE (proven from the shipped 176071840 dex by register-type
 * tracing on BOTH control-flow paths — see change-report.md §1):
 * Fix4's Lqmu.i() insertion wrote REFERENCE values into v11:
 *   021f: move-result-object v11 (LayoutParams)
 *   0222: iget-object v11, v5, Lqmu.g (Lqmr)
 * But v11 is an INT on the 01f6 jump path (last write 012b: add-int/2addr
 * v11, v5; no write to v11 between 012c and 01f6). At the merge point 027e
 * (predecessors: fall-through from 027d, and the 01f6 jump taken when the
 * indicator view h is null) the ART verifier must merge v11 = INT (jump
 * path) with v11 = Lqmr (reference, fall-through path). Int-vs-reference
 * at a join is a VerifyError -> the whole Lqmu class fails verification
 * at load -> the popup can never be built -> "long press are gone", no
 * crash (swallowed by Lqck.D's catch-all). 839 has no such conflict (v11
 * stays INT on both paths). RegInitCheck (init only), VerifyPatch (invokes
 * only) and VerifyBranches (targets only) CANNOT see a merge-type
 * conflict — that is why Fix4 passed every gate and still regressed.
 * The null-LayoutParams theory is RULED OUT as the cause: Lqmu.h is an
 * inflated child (onFinishInflate findViewById 0x7f0b0709, layout
 * 0x7f0e0607) with layout_width/height in style 0x7f15063d, so
 * getLayoutParams() is non-null (stock onFinishInflate itself reads
 * MarginLayoutParams.leftMargin/rightMargin off getLayoutParams() with
 * NO null check). The null checks below are hardening only.
 *
 * FIX (register-type discipline — the defect class, not just the instance):
 * In Lqmu.i(), reference values go ONLY into registers that are
 * REFERENCE-typed on the 01f6 jump path, and int values ONLY into registers
 * that are INT-typed there:
 *   v8  = reference holder (LayoutParams, tracker). v8 is a REFERENCE on
 *         the jump path (015f: move-object v8, v13) and dead after 0218
 *         (cellH consumed at 0219; rewritten as AnimatorSet at 0240).
 *   v11 = int holder (cellW/corrX/cellH/corrY). v11 is INT on the jump
 *         path (012b add-int) and dead after 012c — writing INTs into it
 *         merges int-vs-int at 027e. THE FIX4 BUG WAS WRITING REFERENCES
 *         INTO v11.
 *   v13/v14 keep their Fix4 roles (transient int width/height in v13;
 *         View ref in v14 at 021a) — both are REFERENCE-typed on the jump
 *         path and are rewritten as references (new-array / iget-object)
 *         before the 027e merge, so they are safe.
 * The correction is also made NULL-SAFE: after the first getLayoutParams()
 * an IF_EQZ jumps to the stock animator setup (FALLBACK) when params are
 * null, and IF_LEZ guards skip the correction when width/height <= 0
 * (MATCH_PARENT/WRAP_CONTENT). All three fallback jumps target the first
 * stock insn after the block; every register merges cleanly there
 * (v8 ref/ref, v11 int/int, v13 int/int, v14 View/View — proven in the
 * change report). Fallback = stock top-left translation (popup still
 * opens, circle offset like Fix3) — never a throw, never a regression.
 *
 * Lqmu.o() is rewritten the same way (41 insns, 8 regs): the params
 * reference stays in v4 (never overwritten with an int), width goes in v5
 * and height in v3, with IF_EQZ/IF_LEZ fallbacks jumping to the existing
 * setTranslationX/Y calls (stock top-left on fallback). All merges
 * proven clean (v4 ref/ref, v5/v3 int/int/uninit, return merges
 * null-vs-1.0f exactly like the stock o()).
 */
public class Fix5CenterPatch {
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

    // ---------- new Lqmu.o() (8 regs: this=v6, param=v7) ----------
    // 41 insns. Branch offsets computed from the built list.
    // v4 = params (REFERENCE throughout, never overwritten with an int).
    // v5 = width (X) / v3 = height (Y) — transient ints.
    static List<Instruction> newO() {
        List<Instruction> all = new ArrayList<>();
        // (0) v0 = this.h
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
        // (7) v2 *= v3 (X = col*cellW)
        all.add(new ImmutableInstruction12x(Opcode.MUL_INT_2ADDR, 2, 3));
        // (8) v4 = indicator.getLayoutParams()
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        // (9) v4 = move-result-object
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        // (10) if (v4 == null) -> SET_TX (stock X) — offset patched below
        all.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 4, 0));
        // (11) v5 = params.width (indicatorW)
        all.add(new ImmutableInstruction22c(Opcode.IGET, 5, 4, LP_WIDTH));
        // (12) if (v5 <= 0) -> SET_TX — offset patched below
        all.add(new ImmutableInstruction21t(Opcode.IF_LEZ, 5, 0));
        // (13) v5 = cellW - indicatorW
        all.add(new ImmutableInstruction23x(Opcode.SUB_INT, 5, 3, 5));
        // (14) v5 /= 2 (corrX)
        all.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 5, 5, 2));
        // (15) v2 += v5 (X += corrX)
        all.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 2, 5));
        // (16) v2 = (float) v2
        all.add(new ImmutableInstruction12x(Opcode.INT_TO_FLOAT, 2, 2));
        // (17) [SET_TX] view.setTranslationX(v2)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 2, 0, 0, 0, VIEW_SET_TX));
        // (18) v0 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 6, QMU_H));
        // (19) v5 = point.y
        all.add(new ImmutableInstruction22c(Opcode.IGET, 5, 7, POINT_Y));
        // (20) v1 = tracker.a() (cellH)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, QMR_A));
        // (21) v1 = move-result
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // (22) v5 *= v1 (Y = row*cellH)
        all.add(new ImmutableInstruction12x(Opcode.MUL_INT_2ADDR, 5, 1));
        // (23) v4 = indicator.getLayoutParams()
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        // (24) v4 = move-result-object
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        // (25) if (v4 == null) -> SET_TY (stock Y) — offset patched below
        all.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 4, 0));
        // (26) v3 = params.height (indicatorH)
        all.add(new ImmutableInstruction22c(Opcode.IGET, 3, 4, LP_HEIGHT));
        // (27) if (v3 <= 0) -> SET_TY — offset patched below
        all.add(new ImmutableInstruction21t(Opcode.IF_LEZ, 3, 0));
        // (28) v3 = cellH - indicatorH
        all.add(new ImmutableInstruction23x(Opcode.SUB_INT, 3, 1, 3));
        // (29) v3 /= 2 (corrY)
        all.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 3, 3, 2));
        // (30) v5 += v3 (Y += corrY)
        all.add(new ImmutableInstruction12x(Opcode.ADD_INT_2ADDR, 5, 3));
        // (31) v5 = (float) v5
        all.add(new ImmutableInstruction12x(Opcode.INT_TO_FLOAT, 5, 5));
        // (32) [SET_TY] view.setTranslationY(v5)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 5, 0, 0, 0, VIEW_SET_TY));
        // (33) v5 = this.h (tail: scale/alpha 1.0f)
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 5, 6, QMU_H));
        // (34) v0 = 1.0f
        all.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 0, 0x3F800000));
        // (35) view.setScaleX(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 0, 0, 0, 0, VIEW_SET_SX));
        // (36) v5 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 5, 6, QMU_H));
        // (37) view.setScaleY(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 5, 0, 0, 0, 0, VIEW_SET_SY));
        // (38) v4 = this.h
        all.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 6, QMU_H));
        // (39) view.setAlpha(1.0f)
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 4, 0, 0, 0, 0, VIEW_SET_ALPHA));
        // (40) return
        all.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
        // compute addresses and patch all branch offsets
        int total = 0;
        List<Integer> addr = new ArrayList<>();
        for (Instruction i : all) { addr.add(total); total += i.getCodeUnits(); }
        if (all.size() != 41) {
            throw new RuntimeException("newO shape wrong: ins=" + all.size()
                + " (expected 41)");
        }
        // (1) IF_EQZ v0 -> return (40); (10)/(12) -> SET_TX (17); (25)/(27) -> SET_TY (32)
        all.set(1, new ImmutableInstruction21t(Opcode.IF_EQZ, 0, addr.get(40) - addr.get(1)));
        all.set(10, new ImmutableInstruction21t(Opcode.IF_EQZ, 4, addr.get(17) - addr.get(10)));
        all.set(12, new ImmutableInstruction21t(Opcode.IF_LEZ, 5, addr.get(17) - addr.get(12)));
        all.set(25, new ImmutableInstruction21t(Opcode.IF_EQZ, 4, addr.get(32) - addr.get(25)));
        all.set(27, new ImmutableInstruction21t(Opcode.IF_LEZ, 3, addr.get(32) - addr.get(27)));
        System.out.println("newO: 41 insns, " + total + " units, IF_EQZ(ret)="
            + (addr.get(40) - addr.get(1)) + " SET_TX=" + addr.get(17)
            + " SET_TY=" + addr.get(32));
        return all;
    }

    /** Fingerprint check: the input must be the Fix4 Lqmu.o() (37 insns,
     *  8 regs, no try blocks). Spot-checks the opcode at every structural
     *  position of the known Fix4 body. */
    static boolean checkFix4O(List<Instruction> insns) {
        Opcode[] want = {
            Opcode.IGET_OBJECT, Opcode.IF_EQZ, Opcode.IGET_OBJECT,
            Opcode.IGET_OBJECT, Opcode.IGET, Opcode.INVOKE_VIRTUAL,
            Opcode.MOVE_RESULT, Opcode.MUL_INT_2ADDR, Opcode.INVOKE_VIRTUAL,
            Opcode.MOVE_RESULT_OBJECT, Opcode.IGET, Opcode.SUB_INT,
            Opcode.DIV_INT_LIT8, Opcode.ADD_INT_2ADDR, Opcode.INT_TO_FLOAT,
            Opcode.INVOKE_VIRTUAL, Opcode.IGET_OBJECT, Opcode.IGET,
            Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT, Opcode.MUL_INT_2ADDR,
            Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT_OBJECT, Opcode.IGET,
            Opcode.SUB_INT, Opcode.DIV_INT_LIT8, Opcode.ADD_INT_2ADDR,
            Opcode.INT_TO_FLOAT, Opcode.INVOKE_VIRTUAL, Opcode.IGET_OBJECT,
            Opcode.CONST_HIGH16, Opcode.INVOKE_VIRTUAL, Opcode.IGET_OBJECT,
            Opcode.INVOKE_VIRTUAL, Opcode.IGET_OBJECT, Opcode.INVOKE_VIRTUAL,
            Opcode.RETURN_VOID
        };
        if (insns.size() != want.length) return false;
        for (int i = 0; i < want.length; i++) {
            if (insns.get(i).getOpcode() != want[i]) {
                System.out.println("checkFix4O: insn " + i + " is "
                    + insns.get(i).getOpcode() + ", expected " + want[i]);
                return false;
            }
        }
        return true;
    }

    /** Replace the Fix4 Lqmu.o() (37 insns, 8 regs) with the null-safe
     *  centered version (41 insns, 8 regs). */
    static MethodImplementation patchO(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 37 || impl.getRegisterCount() != 8) {
            System.out.println("Lqmu.o: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount() + " (expected Fix4 37/8)");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqmu.o: unexpected try blocks");
            return null;
        }
        if (!checkFix4O(insns)) {
            System.out.println("Lqmu.o: input is NOT the Fix4 o() — refusing");
            return null;
        }
        System.out.println("Lqmu.o: Fix4 (37 insns, 8 regs) -> Fix5 null-safe "
            + "centered (41 insns, 8 regs)");
        return new ImmutableMethodImplementation(
            8, newO(), impl.getTryBlocks(), impl.getDebugItems());
    }

    // ---------- Lqmu.i() insertion (before insn idx 297, abs 0x21a) ----------
    // 24 insns, 44 units (EVEN). Register-type discipline (the Fix4 lesson):
    //   v8  = reference holder (LayoutParams, tracker) — v8 is a REFERENCE
    //         on the 01f6 jump path (015f move-object) and dead after 0218.
    //   v11 = int holder (cellW/corrX/cellH/corrY) — v11 is INT on the
    //         jump path (012b add-int) and dead after 012c. NEVER a ref.
    //   v13 = transient int (width/height); v14 = indicator View (ref).
    //   v5 = Lqmu (live, read-only); v9/v12 = old/new X, v6/v7 = old/new Y.
    // Three fallback branches (null params, width<=0, height<=0) all jump
    // to the first stock insn after the block (FALLBACK = 0x21a + 44);
    // every register merges cleanly there (see class javadoc).
    static final int I_INSERT_IDX = 297;
    static final int I_INSERT_ADDR = 0x21a;

    static List<Instruction> centerBlockI() {
        List<Instruction> b = new ArrayList<>();
        // ---- X axis ----
        // (0) v14 = indicator (v5.h)
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 14, 5, QMU_H));
        // (1) v14 = indicator.getLayoutParams()
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 14, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        // (2) v8 = move-result-object (LayoutParams — REFERENCE into v8)
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 8));
        // (3) if (v8 == null) -> FALLBACK — offset patched below
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 8, 0));
        // (4) v13 = params.width (indicatorW)
        b.add(new ImmutableInstruction22c(Opcode.IGET, 13, 8, LP_WIDTH));
        // (5) if (v13 <= 0) -> FALLBACK — offset patched below
        b.add(new ImmutableInstruction21t(Opcode.IF_LEZ, 13, 0));
        // (6) v8 = tracker (v5.g)
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 8, 5, QMU_G));
        // (7) v19 = tracker.b() (cellW) -- v19 is I32 at 0x21a entry
        //     (const/16 v19, 3) and DEAD after 0x21a (no use in the
        //     stock code after the insertion), so the FALLBACK merge is
        //     I32-vs-I32 = clean, and the v10/v11 double (live at 0x294
        //     mul-double) is never touched. (v23 was tried first: it is
        //     the HIGH half of the v22/v23 double, I64 at entry -- wrong.)
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 8, 0, 0, 0, 0, QMR_B));
        // (8) v19 = move-result
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 19));
        // (9) v19 = cellW - indicatorW
        b.add(new ImmutableInstruction23x(Opcode.SUB_INT, 19, 19, 13));
        // (10) v19 /= 2 (corrX)
        b.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 19, 19, 2));
        // (11) oldX += corrX ; (12) newX += corrX
        //     ADD_INT (23x, 2u) not ADD_INT_2ADDR (12x): 12x cannot
        //     encode v19 (>15).
        b.add(new ImmutableInstruction23x(Opcode.ADD_INT, 9, 9, 19));
        b.add(new ImmutableInstruction23x(Opcode.ADD_INT, 12, 12, 19));
        // ---- Y axis ----
        // (13) v13 = indicator (v5.h)
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 13, 5, QMU_H));
        // (14) v13 = indicator.getLayoutParams()
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 13, 0, 0, 0, 0, VIEW_GET_LAYOUT_PARAMS));
        // (15) v13 = move-result-object
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 13));
        // (16) v13 = params.height (indicatorH)
        b.add(new ImmutableInstruction22c(Opcode.IGET, 13, 13, LP_HEIGHT));
        // (17) if (v13 <= 0) -> FALLBACK — offset patched below
        b.add(new ImmutableInstruction21t(Opcode.IF_LEZ, 13, 0));
        // (18) v19 = tracker.a() (cellH) — v8 still holds the tracker
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 8, 0, 0, 0, 0, QMR_A));
        // (19) v19 = move-result
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 19));
        // (20) v19 = cellH - indicatorH
        b.add(new ImmutableInstruction23x(Opcode.SUB_INT, 19, 19, 13));
        // (21) v19 /= 2 (corrY)
        b.add(new ImmutableInstruction22b(Opcode.DIV_INT_LIT8, 19, 19, 2));
        // (22) oldY += corrY ; (23) newY += corrY
        b.add(new ImmutableInstruction23x(Opcode.ADD_INT, 6, 6, 19));
        b.add(new ImmutableInstruction23x(Opcode.ADD_INT, 7, 7, 19));
        // compute addresses (block starts at I_INSERT_ADDR) and patch the
        // three fallback branches to the first stock insn after the block.
        int total = 0;
        List<Integer> addr = new ArrayList<>();
        for (Instruction i : b) { addr.add(I_INSERT_ADDR + total); total += i.getCodeUnits(); }
        if (b.size() != 24 || total != 48) {
            throw new RuntimeException("centerBlockI shape wrong: ins=" + b.size()
                + " units=" + total + " (expected 24/48)");
        }
        int fallback = I_INSERT_ADDR + total;
        b.set(3, new ImmutableInstruction21t(Opcode.IF_EQZ, 8, fallback - addr.get(3)));
        b.set(5, new ImmutableInstruction21t(Opcode.IF_LEZ, 13, fallback - addr.get(5)));
        b.set(17, new ImmutableInstruction21t(Opcode.IF_LEZ, 13, fallback - addr.get(17)));
        System.out.println("centerBlockI: 24 insns, 48 units (even), FALLBACK="
            + Integer.toHexString(fallback) + " (null/width/height checks)");
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

    /** Fingerprint check: idx 296-318 of the Fix4 Lqmu.i() must be the Fix4
     *  centering block (21 insns) followed by the stock new-instance. */
    static boolean checkFix4I(List<Instruction> insns) {
        if (insns.size() != 404) return false;
        if (insns.get(296).getOpcode() != Opcode.MUL_INT_2ADDR
                || insns.get(318).getOpcode() != Opcode.NEW_INSTANCE
                || insns.get(319).getOpcode() != Opcode.INVOKE_DIRECT) {
            System.out.println("checkFix4I: boundary mismatch: "
                + insns.get(296).getOpcode() + " / " + insns.get(318).getOpcode()
                + " / " + insns.get(319).getOpcode());
            return false;
        }
        Opcode[] block = {
            Opcode.IGET_OBJECT, Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT_OBJECT,
            Opcode.IGET, Opcode.IGET_OBJECT, Opcode.INVOKE_VIRTUAL,
            Opcode.MOVE_RESULT, Opcode.SUB_INT, Opcode.DIV_INT_LIT8,
            Opcode.ADD_INT_2ADDR, Opcode.ADD_INT_2ADDR, Opcode.IGET_OBJECT,
            Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT_OBJECT, Opcode.IGET,
            Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT, Opcode.SUB_INT,
            Opcode.DIV_INT_LIT8, Opcode.ADD_INT_2ADDR, Opcode.ADD_INT_2ADDR
        };
        for (int i = 0; i < block.length; i++) {
            if (insns.get(297 + i).getOpcode() != block[i]) {
                System.out.println("checkFix4I: block insn " + i + " is "
                    + insns.get(297 + i).getOpcode() + ", expected " + block[i]);
                return false;
            }
        }
        return true;
    }

    /** Replace the Fix4 centering block in Lqmu.i() (idx 297-317, 38 units)
     *  with the Fix5 block (24 insns, 48 units). The block grows by 10 units,
     *  so every original branch crossing the block shifts by +10; all other
     *  original branch offsets are unchanged (both ends shift together or
     *  neither moves). Asserts exactly 5 offsets change (the forward
     *  crossing branches at 0160/0162/0166/017e/01f6 — the 017e one is a
     *  goto/32, which an earlier branch-count missed because its text has
     *  no comma). */
    static MethodImplementation patchI(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 404 || impl.getRegisterCount() != 27) {
            System.out.println("Lqmu.i: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount() + " (expected Fix4 404/27)");
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
        if (!checkFix4I(insns)) {
            System.out.println("Lqmu.i: input is NOT the Fix4 i() — refusing");
            return null;
        }
        // Fix4 absolute offsets
        List<Integer> origAbs = new ArrayList<>();
        int off = 0;
        for (Instruction i : insns) { origAbs.add(off); off += i.getCodeUnits(); }
        if (origAbs.get(297) != I_INSERT_ADDR) {
            System.out.println("Lqmu.i: Fix4 block is at abs " + origAbs.get(297)
                + ", expected 0x21a");
            return null;
        }
        List<Instruction> block = centerBlockI();
        int delta = 0;
        for (Instruction i : block) delta += i.getCodeUnits();
        int grow = delta - 38; // 48 - 38 = +10 units
        if (grow != 10) {
            System.out.println("Lqmu.i: unexpected block growth " + grow);
            return null;
        }
        // new list: orig[0..296] + Fix5 block + orig[318..403]
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < 297; i++) all.add(insns.get(i));
        int blockStart = all.size(); // 297
        all.addAll(block);
        int blockEnd = all.size(); // 321
        for (int i = 318; i < insns.size(); i++) all.add(insns.get(i));
        if (all.size() != 407) {
            System.out.println("Lqmu.i: new size " + all.size() + " (expected 407)");
            return null;
        }
        // retarget original branches: Fix4 (a,t) -> Fix5 (a',t').
        // a' = a+6 iff a >= 0x240 (after the old block); t' = t+6 iff
        // t >= 0x21a (no original branch targets INTO the old block).
        int changed = 0;
        for (int n = 0; n < all.size(); n++) {
            if (n >= blockStart && n < blockEnd) continue; // new block: already correct
            Instruction ins = all.get(n);
            if (!(ins instanceof OffsetInstruction)) continue;
            int origIdx = (n < blockStart) ? n : n - 3; // net +3 insns
            int a = origAbs.get(origIdx);
            int t = a + ((OffsetInstruction) ins).getCodeOffset();
            if (t >= I_INSERT_ADDR && t < I_INSERT_ADDR + 38) {
                System.out.println("Lqmu.i: branch targets INTO the old block: idx "
                    + origIdx + " abs " + Integer.toHexString(a) + " -> "
                    + Integer.toHexString(t));
                return null;
            }
            int ap = (a >= I_INSERT_ADDR + 38) ? a + grow : a;
            int tp = (t >= I_INSERT_ADDR) ? t + grow : t;
            int newOff = tp - ap;
            int oldOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                all.set(n, retarget(ins, newOff));
                changed++;
                System.out.println("Lqmu.i: retargeted branch newIdx " + n
                    + " abs " + Integer.toHexString(a) + " -> tgt "
                    + Integer.toHexString(t) + " newOff " + newOff);
            }
        }
        if (changed != 5) {
            System.out.println("Lqmu.i: expected exactly 5 changed branch offsets, found "
                + changed);
            return null;
        }
        System.out.println("Lqmu.i: Fix4 block (21 insns/38u) -> Fix5 block "
            + "(24 insns/48u), " + changed + " branch offsets changed (+10)");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }
}
