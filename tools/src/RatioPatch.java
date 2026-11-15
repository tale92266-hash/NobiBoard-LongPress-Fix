import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * NobiBoard 176071853 — keyboard height +20% (about one row).
 *
 * The keyboard height is baseHeight * ratio. The ratio lives in Lpfd
 * (KeyboardHeightRatio): its static initializer loads ONE shared default
 * string "1.0" into v2 (insn 6) and reuses it for all 7 flags (base
 * config_keyboard_height_ratio + portrait/landscape/normal/foldable
 * variants). This patch rewrites ONLY that one CONST_STRING in
 * Lpfd.<clinit> from "1.0" to "1.2". The new string is a fresh pool
 * entry; the shared "1.0" pool entry is NOT touched, so no other code is
 * affected (verified: exactly one CONST_STRING "1.0" exists in the whole
 * classes.dex, and it is this one). The instruction stays 21c (2 code
 * units), same register — code size, offsets, branches and registers are
 * all unchanged. This is the ONLY method changed in classes.dex.
 *
 * Usage: RatioPatch <classes.dex in> <classes.dex out>
 * Fails loudly unless exactly 1 replacement is made at insn index 6 of
 * a 35-insn Lpfd.<clinit>.
 */
public class RatioPatch {
    static final String OLD_RATIO = "1.0";
    static final String NEW_RATIO = "1.2";

    static MethodImplementation patchClinit(MethodImplementation impl) {
        List<Instruction> all = new ArrayList<>();
        for (Instruction insn : impl.getInstructions()) all.add(insn);
        if (all.size() != 35)
            throw new RuntimeException("Lpfd.<clinit> has " + all.size() + " insns (expected 35)");
        Instruction target = all.get(6);
        if (!(target instanceof ReferenceInstruction)
                || target.getOpcode() != Opcode.CONST_STRING
                || !(((ReferenceInstruction) target).getReference() instanceof StringReference)
                || !OLD_RATIO.equals(((StringReference) ((ReferenceInstruction) target).getReference()).getString()))
            throw new RuntimeException("insn 6 of Lpfd.<clinit> is not CONST_STRING \"1.0\": " + target);
        all.set(6, new ImmutableInstruction21c(Opcode.CONST_STRING,
                ((Instruction21c) target).getRegisterA(),
                new ImmutableStringReference(NEW_RATIO)));
        return new ImmutableMethodImplementation(impl.getRegisterCount(), all,
                impl.getTryBlocks(), impl.getDebugItems());
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null && method.getDefiningClass().equals("Lpfd;")
                            && method.getName().equals("<clinit>")
                            && method.getParameters().size() == 0
                            && method.getReturnType().equals("V")) {
                        MethodImplementation ni = patchClinit(impl);
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, ni);
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("RatioPatch: Lpfd.<clinit> insn 6 CONST_STRING \"1.0\" -> \"1.2\" (1 site)");
    }
}
