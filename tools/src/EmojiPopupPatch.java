import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 (code 176071827): square long-press emoji popup, center = default.
 *
 * ONE dexlib2 pass on classes2.dex:
 *  (a) Hook Lqml.e(): replace `CONST_4 v4, 0` (insn 7) with
 *      INVOKE_STATIC {v5}, LEmojiPopup;->defaultIndex(Lqml;)I (3 units)
 *      MOVE_RESULT v4 (1 unit)
 *      NOP (1 unit)
 *      = 5 units replacing 1 -> +4 code units (EVEN, payload-safe).
 *      The stock balanced/first_key_as_default logic AFTER insn 7 is
 *      untouched: for non-emoji popups defaultIndex returns 0 (identical
 *      to stock); for our 9-item emoji popups it returns 4 (the center).
 *  (b) Branch fix: the IF_NEZ at insn 3 (target insn 20) straddles the
 *      insert point (branch before, target after) so its offset grows by
 *      +4 (34 -> 38). The two IF_NEZ at insn 8/10 (targets insn 13) sit
 *      entirely after the insert, so their offsets are unchanged.
 *      e() has 0 try blocks and no switch/array payloads.
 *  (c) Merge the LEmojiPopup; class (d8 output) into classes2.dex.
 *      The whole dex is written ONCE (single serialization).
 */
public class EmojiPopupPatch {
    static final ImmutableMethodReference DEFAULT_INDEX =
        new ImmutableMethodReference("LEmojiPopup;", "defaultIndex",
            java.util.Arrays.asList("Lqml;"), "I");

    static int patchedE = 0;

    /** Code-unit address of each instruction index in a list. */
    static int[] addresses(List<Instruction> insns) {
        int[] addr = new int[insns.size() + 1];
        int a = 0;
        for (int i = 0; i < insns.size(); i++) {
            addr[i] = a;
            a += insns.get(i).getCodeUnits();
        }
        addr[insns.size()] = a;
        return addr;
    }

    /** Index of the instruction at code-unit address `target` in `insns`. */
    static int indexAt(List<Instruction> insns, int[] addr, int target) {
        for (int i = 0; i < insns.size(); i++) {
            if (addr[i] == target) return i;
        }
        throw new RuntimeException("no instruction at address " + target);
    }

    static MethodImplementation patchE(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 21 || impl.getRegisterCount() != 6) {
            System.out.println("Lqml.e: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (insns.get(7).getOpcode() != Opcode.CONST_4) {
            System.out.println("Lqml.e: insn 7 is not CONST_4: " + insns.get(7).getOpcode());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.e: unexpected try blocks");
            return null;
        }
        // Original addresses + branch targets (for the fix).
        int[] oldAddr = addresses(insns);
        // Build new list: [0..6] + hook + [7..20] with branch offsets fixed.
        // New address of each ORIGINAL index: orig idx < 7 -> same; >= 7 -> +4.
        // (hook replaces idx7: 1 unit -> 5 units, delta +4; new idx7..9 are hook.)
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < 7; i++) all.add(insns.get(i));
        all.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 1, 5, 0, 0, 0, 0, DEFAULT_INDEX));
        all.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 4));
        all.add(new ImmutableInstruction10x(Opcode.NOP));
        // Remaining original insns (7..20), skipping the replaced CONST_4 at 7.
        for (int i = 8; i < insns.size(); i++) {
            Instruction ins = insns.get(i);
            if (ins instanceof Instruction21t) {
                Instruction21t b = (Instruction21t) ins;
                int branchAddr = oldAddr[i];
                int targetAddr = branchAddr + b.getCodeOffset();
                int targetIdx = indexAt(insns, oldAddr, targetAddr);
                // New addresses: orig idx < 7 -> old addr; orig idx >= 8 -> old addr + 4.
                // (orig idx 7 is gone; hook occupies its slot.)
                int newBranchAddr = branchAddr + 4; // i >= 8
                int newTargetAddr = (targetIdx < 7) ? oldAddr[targetIdx] : oldAddr[targetIdx] + 4;
                int newOffset = newTargetAddr - newBranchAddr;
                all.add(new ImmutableInstruction21t(b.getOpcode(), b.getRegisterA(), newOffset));
                System.out.println("Lqml.e: branch insn " + i + " offset "
                    + b.getCodeOffset() + " -> " + newOffset);
            } else {
                all.add(ins);
            }
        }
        // Fix the branch at original insn 3 (before the insert, target after it).
        // It is in `all` at index 3 (unchanged position). Rebuild with +4 offset.
        {
            Instruction ins = all.get(3);
            if (!(ins instanceof Instruction21t)) {
                System.out.println("Lqml.e: insn 3 is not a branch");
                return null;
            }
            Instruction21t b = (Instruction21t) ins;
            int branchAddr = oldAddr[3];
            int targetAddr = branchAddr + b.getCodeOffset();
            int targetIdx = indexAt(insns, oldAddr, targetAddr);
            int newTargetAddr = oldAddr[targetIdx] + 4; // target (idx 20) is after insert
            int newOffset = newTargetAddr - branchAddr;
            all.set(3, new ImmutableInstruction21t(b.getOpcode(), b.getRegisterA(), newOffset));
            System.out.println("Lqml.e: branch insn 3 offset "
                + b.getCodeOffset() + " -> " + newOffset);
        }
        System.out.println("Lqml.e: CONST_4 v4,0 replaced by defaultIndex() hook (+4 units, even)");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    if (method.getDefiningClass().equals("Lqml;")
                            && method.getName().equals("e")
                            && method.getParameters().size() == 0) {
                        MethodImplementation impl = method.getImplementation();
                        if (impl == null) return def.rewrite(method);
                        MethodImplementation ni = patchE(impl);
                        if (ni != null) {
                            patchedE++;
                            return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                method.getParameters(), method.getReturnType(),
                                method.getAccessFlags(), method.getAnnotations(), null, ni);
                        }
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        // args: classes2.dex helper.dex out.dex
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile rewritten = rw.getDexFileRewriter().rewrite(dex);
        DexBackedDexFile helper = DexFileFactory.loadDexFile(new java.io.File(args[1]), Opcodes.forApi(35));
        List<ClassDef> all = new ArrayList<>();
        for (ClassDef c : rewritten.getClasses()) all.add(c);
        int helperClasses = 0;
        for (ClassDef c : helper.getClasses()) {
            all.add(c);
            helperClasses++;
            System.out.println("merged class: " + c.getType());
        }
        ImmutableDexFile out = new ImmutableDexFile(Opcodes.forApi(35), all);
        DexFileFactory.writeDexFile(args[2], out);
        System.out.println("patchedE=" + patchedE + " helperClasses=" + helperClasses);
        if (patchedE != 1 || helperClasses != 1) {
            throw new RuntimeException("expected exactly 1 Lqml.e patch and 1 helper class");
        }
    }
}
