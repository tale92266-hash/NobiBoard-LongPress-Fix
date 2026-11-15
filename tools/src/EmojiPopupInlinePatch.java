import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.immutable.ImmutableMethod;
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
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 (code 176071827): square long-press emoji popup, center = default.
 *
 * WHY INLINE (no helper class, no helper method call):
 * classes2.dex method_ids is EXACTLY 65536 (the 16-bit maximum). Adding ANY
 * new method reference (a helper call, or a new helper method) overflows it
 * and dexlib2 fails to write ("Unsigned short value out of range: 65537",
 * proven on the first attempt which merged LEmojiPopup;). So the center
 * check is inlined into Lqml.e() using ONLY method/field/type references
 * that ALREADY exist in classes2.dex (verified: List.size, List.get,
 * CharSequence.length, Character.codePointAt, Lqml.e, Lpva.e, Lpva,
 * CharSequence). Zero new refs of any kind; method_ids stays 65536.
 *
 * The check (head insert, 44 code units, EVEN; e() has no payloads/try):
 *   v4 = 0;
 *   items = this.e; if (items == null) done;
 *   if (items.size() != 9) done;
 *   item = items.get(4); if (!(item instanceof Lpva)) done;
 *   label = item.e; if (!(label instanceof CharSequence)) done;
 *   if (label.length() == 0) done;
 *   if (Character.codePointAt(label, 0) < 0x10000) done;
 *   v4 = 4;
 * done: (stock body follows)
 * All branches target L_done (end of the head block). Head insert => all
 * stock branch offsets unchanged (branch and target shift equally).
 * The stock `CONST_4 v4, 0` at insn 7 is replaced by NOP (same 1 unit),
 * so v4 keeps the head-computed value. The stock balanced /
 * first_key_as_default logic after it is untouched (for non-emoji popups
 * v4 is 0 = stock; for our popups balanced=true so v4=4 survives).
 * Registers: head block uses v0-v3 (free at head) + v4 (result) + v5 (this).
 */
public class EmojiPopupInlinePatch {
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
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
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");

    static int patchedE = 0;

    /** Build the 24-instruction head block. L_done is at code-unit 44. */
    static List<Instruction> headBlock() {
        final int DONE = 44;
        List<Instruction> h = new ArrayList<>();
        // addr 0
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 0));                 // v4 = 0
        // addr 1
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 5, QML_E));     // v0 = this.e
        // addr 3
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, DONE - 3));          // if v0==null -> done
        // addr 5
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_SIZE));
        // addr 8
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));               // v1 = size
        // addr 9
        h.add(new ImmutableInstruction21s(Opcode.CONST_16, 2, 9));               // v2 = 9
        // addr 11
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 1, 2, DONE - 11));       // if size != 9 -> done
        // addr 13
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 1, 4));                 // v1 = 4
        // addr 14
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0, LIST_GET));
        // addr 17
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));        // v1 = item
        // addr 18
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 2, 1, PVA_TYPE));  // v2 = item instanceof Lpva
        // addr 20
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 2, DONE - 20));         // if not -> done
        // addr 22
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, PVA_E));     // v2 = item.e
        // addr 24
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, CHARSEQ_TYPE)); // v3 = label instanceof CharSequence
        // addr 26
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, DONE - 26));         // if not -> done
        // addr 28
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CHARSEQ_LENGTH));
        // addr 31
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));               // v3 = length
        // addr 32
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, DONE - 32));         // if length == 0 -> done
        // addr 34
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));                 // v3 = 0
        // addr 35
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 3, 0, 0, 0, CHAR_CODEPOINTAT));
        // addr 38
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));               // v3 = codePointAt(label, 0)
        // addr 39
        h.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 2, 0x10000));          // v2 = 0x10000
        // addr 41
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 3, 2, DONE - 41));       // if cp < 0x10000 -> done
        // addr 43
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 4));                 // v4 = 4
        // addr 44 = L_done (stock insn 0 follows)
        int total = 0;
        for (Instruction i : h) total += i.getCodeUnits();
        if (total != DONE) throw new RuntimeException("head block is " + total + " units, expected " + DONE);
        return h;
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
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        for (int i = 0; i < insns.size(); i++) {
            if (i == 7) {
                all.add(new ImmutableInstruction10x(Opcode.NOP)); // was CONST_4 v4, 0
            } else {
                all.add(insns.get(i));
            }
        }
        System.out.println("Lqml.e: head block (44 units, even) + insn 7 CONST_4 -> NOP");
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
