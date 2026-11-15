import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
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

/** Rewrites the crash-reporter version string in classes5.dex: 176071839 -> 176071840. */
public class DexVersionBumpFix4 {
    static final String OLD = "176071839";
    static final String NEW = "176071840";
    static int replaced = 0;

    static String map(String s) {
        if (s == null || !s.contains(OLD)) return s;
        replaced++;
        return s.replace(OLD, NEW);
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Instruction> getInstructionRewriter(final Rewriters rewriters) {
            final Rewriter<Instruction> def = super.getInstructionRewriter(rewriters);
            return new Rewriter<Instruction>() {
                public Instruction rewrite(Instruction insn) {
                    if (insn instanceof ReferenceInstruction) {
                        Reference ref = ((ReferenceInstruction) insn).getReference();
                        if (ref instanceof StringReference) {
                            String s = ((StringReference) ref).getString();
                            String ns = map(s);
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
                        String ns = map(s);
                        if (!ns.equals(s)) return new ImmutableStringEncodedValue(ns);
                    }
                    return def.rewrite(v);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(args[0], Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("rewritten string use-sites: " + replaced);
    }
}
