import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction31c;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction31c;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.immutable.value.ImmutableStringEncodedValue;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;
import java.io.File;

/**
 * Phase 3 FIX (code 176071861) — classes5 version string bump only
 * (176071861 -> 176071861), same VerModule pattern as Phase3CombinedPatch.
 * Usage: VerBump <c5in> <c5out>
 */
public class VerBump861 {
    static final String OLD_VER = "176071860";
    static final String NEW_VER = "176071861";
    static int replaced = 0;

    static String mapVer(String s) {
        if (s == null || !s.contains(OLD_VER)) return s;
        replaced++;
        return s.replace(OLD_VER, NEW_VER);
    }

    static class VerModule extends RewriterModule {
        @Override
        public Rewriter<Instruction> getInstructionRewriter(final Rewriters rewriters) {
            final Rewriter<Instruction> def = super.getInstructionRewriter(rewriters);
            return new Rewriter<Instruction>() {
                public Instruction rewrite(Instruction insn) {
                    if (insn instanceof ReferenceInstruction) {
                        Reference ref = ((ReferenceInstruction) insn).getReference();
                        if (ref instanceof StringReference) {
                            String s = ((StringReference) ref).getString();
                            String ns = mapVer(s);
                            if (!ns.equals(s)) {
                                if (insn.getOpcode() == Opcode.CONST_STRING) {
                                    return new ImmutableInstruction21c(Opcode.CONST_STRING,
                                            ((Instruction21c) insn).getRegisterA(),
                                            new ImmutableStringReference(ns));
                                } else if (insn.getOpcode() == Opcode.CONST_STRING_JUMBO) {
                                    return new ImmutableInstruction31c(Opcode.CONST_STRING_JUMBO,
                                            ((Instruction31c) insn).getRegisterA(),
                                            new ImmutableStringReference(ns));
                                }
                            }
                        }
                    }
                    return def.rewrite(insn);
                }
            };
        }

        @Override
        public Rewriter<EncodedValue> getEncodedValueRewriter(final Rewriters rewriters) {
            final Rewriter<EncodedValue> def = super.getEncodedValueRewriter(rewriters);
            return new Rewriter<EncodedValue>() {
                public EncodedValue rewrite(EncodedValue v) {
                    if (v instanceof StringEncodedValue) {
                        String s = ((StringEncodedValue) v).getValue();
                        String ns = mapVer(s);
                        if (!ns.equals(s)) return new ImmutableStringEncodedValue(ns);
                    }
                    return def.rewrite(v);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile c5 = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        DexRewriter rw5 = new DexRewriter(new VerModule());
        DexFile c5out = rw5.getDexFileRewriter().rewrite(c5);
        DexFileFactory.writeDexFile(args[1], c5out);
        System.out.println("classes5 version bump replacements: " + replaced + " -> " + args[1]);
        if (replaced == 0) throw new RuntimeException("version string NOT found in classes5!");
    }
}
