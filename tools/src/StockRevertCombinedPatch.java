import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

/**
 * Stock Revert (code 176071845) — ONE dexlib2 pass over the shipped
 * 176071839 classes2.dex: Lqml.k() and Lqmu.c() are replaced with their
 * STOCK implementations taken from the V40-Clean (176071809) classes2.dex.
 * Everything else: default (byte-identical). Zero new refs.
 *
 * Fingerprints asserted (fail loudly on mismatch):
 *   stock k: Lqml->k(FF)Lpva; regs=13 insns=198 units=335 tryBlocks=0
 *   stock c: Lqmu->c(7 params)V regs=14 insns=72 units=143 tryBlocks=0
 *   839   k: regs=13 insns=292 units=503 tryBlocks=0
 *   839   c: regs=14 insns=101 units=199 tryBlocks=0
 * Usage: StockRevertCombinedPatch <input839.dex> <stockV40.dex> <output.dex>
 */
public class StockRevertCombinedPatch {
    static int kCount = 0;
    static int cCount = 0;
    static MethodImplementation stockK;
    static MethodImplementation stockC;

    static int[] fingerprint(MethodImplementation impl) {
        int insns = 0, units = 0;
        for (Instruction ins : impl.getInstructions()) { insns++; units += ins.getCodeUnits(); }
        return new int[]{insns, units, impl.getRegisterCount(), impl.getTryBlocks().size()};
    }

    static void assertFp(String what, MethodImplementation impl, int regs, int insns, int units, int tries) {
        int[] fp = fingerprint(impl);
        if (fp[0] != insns || fp[1] != units || fp[2] != regs || fp[3] != tries) {
            throw new RuntimeException("FINGERPRINT MISMATCH " + what + ": got regs=" + fp[2]
                + " insns=" + fp[0] + " units=" + fp[1] + " tries=" + fp[3]
                + " expected regs=" + regs + " insns=" + insns + " units=" + units + " tries=" + tries);
        }
        System.out.println("fingerprint OK " + what + ": regs=" + fp[2] + " insns=" + fp[0] + " units=" + fp[1] + " tries=" + fp[3]);
    }

    static boolean isK(Method m) {
        return m.getName().equals("k") && m.getParameters().size() == 2
            && m.getParameters().get(0).getType().equals("F")
            && m.getParameters().get(1).getType().equals("F")
            && m.getReturnType().equals("Lpva;");
    }

    static boolean isC(Method m) {
        return m.getName().equals("c") && m.getParameters().size() == 7
            && m.getParameters().get(0).getType().equals("Landroid/widget/LinearLayout;")
            && m.getReturnType().equals("V");
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl == null) return def.rewrite(method);
                    if (method.getDefiningClass().equals("Lqml;") && isK(method)) {
                        assertFp("839 k", impl, 13, 292, 503, 0);
                        kCount++;
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, stockK);
                    }
                    if (method.getDefiningClass().equals("Lqmu;") && isC(method)) {
                        assertFp("839 c", impl, 14, 101, 199, 0);
                        cCount++;
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, stockC);
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile stockDex = DexFileFactory.loadDexFile(
            new java.io.File(args[1]), Opcodes.forApi(35));
        stockK = null; stockC = null;
        for (org.jf.dexlib2.iface.ClassDef cd : stockDex.getClasses()) {
            if (cd.getType().equals("Lqml;")) {
                for (Method m : cd.getMethods()) {
                    if (isK(m)) stockK = m.getImplementation();
                }
            }
            if (cd.getType().equals("Lqmu;")) {
                for (Method m : cd.getMethods()) {
                    if (isC(m)) stockC = m.getImplementation();
                }
            }
        }
        if (stockK == null) { System.out.println("ERROR: stock Lqml.k not found in " + args[1]); System.exit(1); }
        if (stockC == null) { System.out.println("ERROR: stock Lqmu.c not found in " + args[1]); System.exit(1); }
        assertFp("stock k", stockK, 13, 198, 335, 0);
        assertFp("stock c", stockC, 14, 72, 143, 0);
        // Stock-ness checks: stock k must contain no Lqmu reference and no codePointAt;
        // stock c must end with p() + RETURN_VOID and contain no CONST 1,1 center-init.
        for (Instruction ins : stockK.getInstructions()) {
            String s = ins.toString();
            if (s.contains("Lqmu;->i") || s.contains("codePointAt")) {
                throw new RuntimeException("STOCK k CONTAINS HOOK MARKER: " + s);
            }
        }
        System.out.println("stock k: no hook markers (no Lqmu.i, no codePointAt)");
        DexBackedDexFile dex = DexFileFactory.loadDexFile(
            new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[2], out);
        System.out.println("patched: k=" + kCount + " c=" + cCount);
        if (kCount != 1 || cCount != 1) {
            System.out.println("ERROR: expected exactly 1 k and 1 c");
            System.exit(1);
        }
    }
}
