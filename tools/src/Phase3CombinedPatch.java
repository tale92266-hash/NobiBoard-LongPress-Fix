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
import org.jf.dexlib2.writer.builder.BuilderClassDef;
import org.jf.dexlib2.writer.builder.BuilderField;
import org.jf.dexlib2.writer.builder.BuilderMethod;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import java.io.File;
import java.util.*;

/**
 * Phase 3 combined patch (code 176071851) — ONE run, each dex read+written once:
 *   classes2.dex: Lqmu.c() hook (reuses Phase3HookPatch.Module verbatim)
 *   classes5.dex: version string 176071849 -> 176071851 (CONST_STRING + EncodedValue)
 *   classes6.dex: merge editor.dex classes in (CapsLockHelper must stay canonical-identical)
 *
 * Args: <c2in> <c2out> <c5in> <c5out> <c6in> <editorDex> <c6out>
 */
public class Phase3CombinedPatch {
    static final String OLD_VER = "176071849";
    static final String NEW_VER = "176071851";
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

    static void internDex(DexPool pool, DexFile dex, Set<String> seen, String tag) {
        int n = 0;
        for (ClassDef cd : dex.getClasses()) {
            if (!seen.add(cd.getType()))
                throw new RuntimeException("duplicate class across dexes: " + cd.getType());
            // DexPool interns the whole ClassDef including method implementations,
            // converting DexBacked references to pool references at write time.
            // (DexBuilder.internMethod does NOT do this — it stores the
            // implementation as-is and writeTo dies with a ClassCastException.)
            pool.internClass(cd);
            n++;
        }
        System.out.println(tag + ": interned " + n + " classes");
    }

    public static void main(String[] args) throws Exception {
        // ---- classes2: hook ----
        DexBackedDexFile c2 = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        DexRewriter rw2 = new DexRewriter(new Phase3HookPatch.Module());
        DexFile c2out = rw2.getDexFileRewriter().rewrite(c2);
        DexFileFactory.writeDexFile(args[1], c2out);
        System.out.println("classes2 hooked -> " + args[1]);

        // ---- classes5: version bump ----
        DexBackedDexFile c5 = DexFileFactory.loadDexFile(new File(args[2]), Opcodes.forApi(35));
        DexRewriter rw5 = new DexRewriter(new VerModule());
        DexFile c5out = rw5.getDexFileRewriter().rewrite(c5);
        DexFileFactory.writeDexFile(args[3], c5out);
        System.out.println("classes5 version bump replacements: " + replaced + " -> " + args[3]);
        if (replaced == 0) throw new RuntimeException("version string NOT found in classes5!");

        // ---- classes6: merge editor.dex ----
        DexBackedDexFile c6 = DexFileFactory.loadDexFile(new File(args[4]), Opcodes.forApi(35));
        DexBackedDexFile ed = DexFileFactory.loadDexFile(new File(args[5]), Opcodes.forApi(35));
        DexPool pool = new DexPool(Opcodes.forApi(35));
        Set<String> seen = new HashSet<>();
        internDex(pool, c6, seen, "classes6");
        internDex(pool, ed, seen, "editor");
        FileDataStore store = new FileDataStore(new File(args[6]));
        pool.writeTo(store);
        store.close();
        System.out.println("classes6 merged -> " + args[6]);
    }
}
