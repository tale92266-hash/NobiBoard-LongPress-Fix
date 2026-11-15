import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
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
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10t;
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
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2Fix2 (code 176071831): FORCE the center selection in Lqml.e().
 *
 * DIAGNOSIS (proven from the dex, see change-report):
 * The e() head hook sets v4=3 (center) BEFORE the stock body, but the stock
 * body OVERWRITES v4 with Lqmn.f (insn 50: IGET v4, Lqmn.f) whenever
 * Lqmm.d (balanced_placement) AND Lqmm.g (first_key_as_default) are BOTH
 * false. The Silk popup layout 0x7f0e061e sets BOTH to false, so for a Silk
 * popup the center is silently replaced by Lqmn.f (a bottom-row item) and
 * the k() touch-guard then faithfully preserves that WRONG selection.
 * That is why the center never stuck in three builds: the head hook was
 * correct, the stock body undid it afterwards.
 *
 * FIX: append a POST-block to e() that runs AFTER the stock body and forces
 * the selection to the center (index 3) for our emoji popups, no matter
 * what the stock body computed. The stock body's final RETURN_VOID is
 * replaced by a GOTO to the post-block, and the early-return branch
 * (IF_NEZ -> RETURN, when this.d is empty) is retargeted to the post-block
 * too (the post-block itself checks this.d is non-empty before touching it).
 * The post-block re-checks the emoji guard (9 items, item[3] a non-BMP
 * CharSequence) so non-emoji popups are untouched.
 *
 * Post-block layout (relative to P = post-block start, 67 units total,
 * RETURN at P+66). All refs already exist in classes2.dex (zero new refs).
 * e() has no try blocks and no payloads, so appending at the end is safe.
 */
public class EmojiPopupCenterForcePatch {
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QML_D =
        new ImmutableFieldReference("Lqml;", "d", "Ljava/util/List;");
    static final ImmutableFieldReference QML_A =
        new ImmutableFieldReference("Lqml;", "a", "Z");
    static final ImmutableFieldReference QML_G =
        new ImmutableFieldReference("Lqml;", "g", "I");
    static final ImmutableFieldReference QML_H =
        new ImmutableFieldReference("Lqml;", "h", "I");
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
    static final ImmutableMethodReference QML_M =
        new ImmutableMethodReference("Lqml;", "m",
            java.util.Arrays.asList("Landroid/view/View;", "Z"), "V");
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");
    static final ImmutableTypeReference VIEW_TYPE = new ImmutableTypeReference("Landroid/view/View;");

    static final int POST_SIZE = 67;   // post-block total code units
    static final int POST_RETURN = 66;  // RETURN_VOID offset within post-block

    /** Build the post-block. RET = P+66. Branch offsets are relative to each branch. */
    static List<Instruction> postBlock() {
        List<Instruction> b = new ArrayList<>();
        // P+0
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 5, QML_E));
        // P+2: if e == null -> RETURN
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, POST_RETURN - 2));
        // P+4
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_SIZE));
        // P+7
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // P+8
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, 2, 9));
        // P+10: if size != 9 -> RETURN
        b.add(new ImmutableInstruction22t(Opcode.IF_NE, 1, 2, POST_RETURN - 10));
        // P+12
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 1, 3));
        // P+13
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0, LIST_GET));
        // P+16
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        // P+17
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 2, 1, PVA_TYPE));
        // P+19: if not Lpva -> RETURN
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 2, POST_RETURN - 19));
        // P+21
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, PVA_E));
        // P+23
        b.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, CHARSEQ_TYPE));
        // P+25: if label not CharSequence -> RETURN
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, POST_RETURN - 25));
        // P+27
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CHARSEQ_LENGTH));
        // P+30
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // P+31: if length == 0 -> RETURN
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, POST_RETURN - 31));
        // P+33
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));
        // P+34
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 3, 0, 0, 0, CHAR_CODEPOINTAT));
        // P+37
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // P+38
        b.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 2, 0x10000));
        // P+40: if cp < 0x10000 -> RETURN
        b.add(new ImmutableInstruction22t(Opcode.IF_LT, 3, 2, POST_RETURN - 40));
        // P+42: force center
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 5, QML_D));
        // P+44
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_ISEMPTY));
        // P+47
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // P+48: if d empty -> RETURN
        b.add(new ImmutableInstruction21t(Opcode.IF_NEZ, 1, POST_RETURN - 48));
        // P+50
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 3));
        // P+51
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 4, 0, 0, 0, LIST_GET));
        // P+54
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        // P+55
        b.add(new ImmutableInstruction21c(Opcode.CHECK_CAST, 0, VIEW_TYPE));
        // P+57
        b.add(new ImmutableInstruction22c(Opcode.IGET_BOOLEAN, 1, 5, QML_A));
        // P+59
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_DIRECT, 3, 5, 0, 1, 0, 0, QML_M));
        // P+62
        b.add(new ImmutableInstruction22c(Opcode.IPUT, 4, 5, QML_G));
        // P+64
        b.add(new ImmutableInstruction22c(Opcode.IPUT, 4, 5, QML_H));
        // P+66
        b.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));
        int total = 0;
        for (Instruction i : b) total += i.getCodeUnits();
        if (total != POST_SIZE) throw new RuntimeException("post-block is " + total + " units, expected " + POST_SIZE);
        return b;
    }

    static int patchedE = 0;

    static MethodImplementation patchE(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        // Phase 2Fix e(): 59 insns (38 head + 21 stock body), regs 6, no try blocks
        if (insns.size() != 59 || impl.getRegisterCount() != 6) {
            System.out.println("Lqml.e: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.e: unexpected try blocks");
            return null;
        }
        if (insns.get(0).getOpcode() != Opcode.CONST_4
                || insns.get(58).getOpcode() != Opcode.RETURN_VOID) {
            System.out.println("Lqml.e: head/tail mismatch: "
                + insns.get(0).getOpcode() + " / " + insns.get(58).getOpcode());
            return null;
        }
        // compute absolute addresses
        List<Integer> absOff = new ArrayList<>();
        int off = 0;
        for (Instruction i : insns) { absOff.add(off); off += i.getCodeUnits(); }
        int totalUnits = off;
        int postStart = totalUnits; // post-block starts here
        // find the IF_NEZ that branches to the final RETURN (insn 58)
        int earlyIdx = -1;
        for (int i = 0; i < insns.size(); i++) {
            Instruction ins = insns.get(i);
            if (ins instanceof Instruction21t) {
                int tgt = absOff.get(i) + ((Instruction21t) ins).getCodeOffset();
                if (tgt == absOff.get(58)) { earlyIdx = i; break; }
            }
        }
        if (earlyIdx < 0) {
            System.out.println("Lqml.e: early-return branch to insn 58 not found");
            return null;
        }
        System.out.println("Lqml.e: totalUnits=" + totalUnits + " postStart=" + postStart
            + " earlyIdx=" + earlyIdx + " earlyAddr=" + absOff.get(earlyIdx));
        if (totalUnits != 115) {
            System.out.println("Lqml.e: WARNING totalUnits=" + totalUnits + " (expected 115)");
        }
        List<Instruction> all = new ArrayList<>();
        for (int i = 0; i < insns.size(); i++) {
            if (i == earlyIdx) {
                // retarget early return -> post-block (preserve opcode + register)
                Instruction21t orig = (Instruction21t) insns.get(i);
                int newOff = postStart - absOff.get(i);
                all.add(new ImmutableInstruction21t(orig.getOpcode(), orig.getRegisterA(), newOff));
            } else if (i == 58) {
                // final RETURN_VOID -> GOTO post-block
                int newOff = postStart - absOff.get(i);
                all.add(new ImmutableInstruction10t(Opcode.GOTO, newOff));
            } else {
                all.add(insns.get(i));
            }
        }
        all.addAll(postBlock());
        System.out.println("Lqml.e: post-block appended (force center=3 after stock body)");
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
        // args: classes2.dex out.dex
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patchedE=" + patchedE);
        if (patchedE != 1) throw new RuntimeException("expected exactly 1 Lqml.e patch");
    }
}
