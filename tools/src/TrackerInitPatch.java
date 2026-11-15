import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction10x;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21ih;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21s;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction22c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction22t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2Fix4 (code 176071833): initialize the Lqmr tracker to the CENTER
 * cell (1,1) in Lqmu.c().
 *
 * DIAGNOSIS (proven, see diagnosis.md): Lqmu.c() sets the tracker Points
 * Lqmr.b/c to (Lqmn.f, 0 or Lqmn.d-1). For Sam's 3x3 emoji popups that is
 * (1,2) = bottom-middle = DOWN emoji, so the green indicator starts on the
 * bottom emoji at open and (because the k() heads bypassed Lqmu.i) never
 * moves. The e() post-block fixes Lqml.g, which Silk never reads.
 *
 * FIX (this file): insert a guarded block right before Lqmu.c()'s trailing
 * p() call (insn 70). For emoji popups only (same guard as the e()/k()
 * hooks: 9 items, item[3] an Lpva with a non-BMP label), set BOTH tracker
 * Points to (1,1); p() then puts the green indicator on the center at open.
 * Non-emoji popups fall through unchanged. The block uses v0-v5 scratch
 * (dead after the build loop); v6 (this) is preserved. Zero new refs
 * (Point.set, Lqmu.g, Lqmr.b/c are all already referenced by Lqmu.c()).
 *
 * Block layout (relative code units, block starts at original absOff(70)):
 *   guard 0..41  (failures -> SKIP = p() call)
 *   set   42..55 (tracker b/c -> (1,1))
 *   BLOCK_SIZE = 56 (EVEN). SKIP target = blockStart + 56.
 * The one stock branch targeting insn 70 (the loop-exit IF_GE at insn 26)
 * is retargeted to the new p() address.
 */
public class TrackerInitPatch {
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QMU_G =
        new ImmutableFieldReference("Lqmu;", "g", "Lqmr;");
    static final ImmutableFieldReference QMR_B =
        new ImmutableFieldReference("Lqmr;", "b", "Landroid/graphics/Point;");
    static final ImmutableFieldReference QMR_C =
        new ImmutableFieldReference("Lqmr;", "c", "Landroid/graphics/Point;");
    static final ImmutableFieldReference PVA_E =
        new ImmutableFieldReference("Lpva;", "e", "Ljava/lang/Object;");
    static final ImmutableMethodReference LIST_SIZE =
        new ImmutableMethodReference("Ljava/util/List;", "size",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference LIST_GET =
        new ImmutableMethodReference("Ljava/util/List;", "get",
            java.util.Arrays.asList("I"), "Ljava/lang/Object;");
    static final ImmutableMethodReference CHARSEQ_LENGTH =
        new ImmutableMethodReference("Ljava/lang/CharSequence;", "length",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference CHAR_CODEPOINTAT =
        new ImmutableMethodReference("Ljava/lang/Character;", "codePointAt",
            java.util.Arrays.asList("Ljava/lang/CharSequence;", "I"), "I");
    static final ImmutableMethodReference POINT_SET =
        new ImmutableMethodReference("Landroid/graphics/Point;", "set",
            java.util.Arrays.asList("I", "I"), "V");
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");

    static final int BLOCK_SIZE = 56;  // init block total code units (EVEN)
    static final int THIS = 6;         // Lqmu.c() this register

    /** Build the 56-unit init block. Branch offsets are BLOCK_SIZE - addr. */
    static List<Instruction> initBlock() {
        List<Instruction> b = new ArrayList<>();
        // rel 0
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, THIS, QML_E));
        // rel 2: if e == null -> SKIP
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, BLOCK_SIZE - 2));
        // rel 4
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_SIZE));
        // rel 7
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // rel 8
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, 2, 9));
        // rel 10: if size != 9 -> SKIP
        b.add(new ImmutableInstruction22t(Opcode.IF_NE, 1, 2, BLOCK_SIZE - 10));
        // rel 12
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 1, 3));
        // rel 13
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0, LIST_GET));
        // rel 16
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        // rel 17
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 2, 1, PVA_TYPE));
        // rel 19: if not Lpva -> SKIP
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 2, BLOCK_SIZE - 19));
        // rel 21
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, PVA_E));
        // rel 23
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, CHARSEQ_TYPE));
        // rel 25: if label not CharSequence -> SKIP
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, BLOCK_SIZE - 25));
        // rel 27
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CHARSEQ_LENGTH));
        // rel 30
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // rel 31: if length == 0 -> SKIP
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, BLOCK_SIZE - 31));
        // rel 33
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));
        // rel 34
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 3, 0, 0, 0, CHAR_CODEPOINTAT));
        // rel 37
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // rel 38
        b.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 2, 0x10000));
        // rel 40: if cp < 0x10000 -> SKIP
        b.add(new ImmutableInstruction22t(Opcode.IF_LT, 3, 2, BLOCK_SIZE - 40));
        // rel 42: tracker = (1,1)
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, THIS, QMU_G));
        // rel 44
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 0, QMR_B));
        // rel 46
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 1));
        // rel 47
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 1));
        // rel 48
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 1, 2, 3, 0, 0, POINT_SET));
        // rel 51
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 0, QMR_C));
        // rel 53
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 1, 2, 3, 0, 0, POINT_SET));
        // rel 56 = SKIP (p() call follows)
        int total = 0;
        for (Instruction i : b) total += i.getCodeUnits();
        if (total != BLOCK_SIZE) throw new RuntimeException("init block is " + total + " units, expected " + BLOCK_SIZE);
        if ((total % 2) != 0) throw new RuntimeException("init block is ODD (" + total + "), must be EVEN");
        return b;
    }

    static MethodImplementation patchC(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        // Lqmu.c(): 72 insns, regs 14, no try blocks
        if (insns.size() != 72 || impl.getRegisterCount() != 14) {
            System.out.println("Lqmu.c: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqmu.c: unexpected try blocks");
            return null;
        }
        if (insns.get(0).getOpcode() != Opcode.INVOKE_SUPER_RANGE
                || insns.get(70).getOpcode() != Opcode.INVOKE_DIRECT
                || insns.get(71).getOpcode() != Opcode.RETURN_VOID) {
            System.out.println("Lqmu.c: boundary mismatch: " + insns.get(0).getOpcode()
                + " / " + insns.get(70).getOpcode() + " / " + insns.get(71).getOpcode());
            return null;
        }
        // no switch/array payloads in c()
        for (Instruction i : insns) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                    || op == Opcode.FILL_ARRAY_DATA) {
                System.out.println("Lqmu.c: unexpected payload " + op);
                return null;
            }
        }
        // absolute addresses
        List<Integer> absOff = new ArrayList<>();
        int off = 0;
        for (Instruction i : insns) { absOff.add(off); off += i.getCodeUnits(); }
        int pAddr = absOff.get(70);          // original p() call address
        int newPAddr = pAddr + BLOCK_SIZE;   // p() call address after insert
        // retarget every branch that targets the p() call (the loop-exit IF_GE)
        int retargeted = 0;
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < insns.size(); i++) {
            Instruction ins = insns.get(i);
            if (i < 70 && ins instanceof OffsetInstruction) {
                int tgt = absOff.get(i) + ((OffsetInstruction) ins).getCodeOffset();
                if (tgt == pAddr) {
                    if (ins instanceof org.jf.dexlib2.iface.instruction.formats.Instruction22t) {
                        org.jf.dexlib2.iface.instruction.formats.Instruction22t o =
                            (org.jf.dexlib2.iface.instruction.formats.Instruction22t) ins;
                        int newOff = newPAddr - absOff.get(i);
                        all.add(new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(),
                            o.getRegisterB(), newOff));
                        retargeted++;
                        continue;
                    } else {
                        System.out.println("Lqmu.c: branch to p() is not 22t: " + ins);
                        return null;
                    }
                }
            }
            if (i == 70) all.addAll(initBlock());
            all.add(ins);
        }
        if (retargeted != 1) {
            System.out.println("Lqmu.c: expected exactly 1 branch to p(), found " + retargeted);
            return null;
        }
        System.out.println("Lqmu.c: init block inserted before p() (tracker -> (1,1)), "
            + "p() now at abs " + newPAddr);
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }
}
