import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

/**
 * Fix3 (code 176071839) — ONE dexlib2 pass over the shipped 176071838
 * classes2.dex: Lqmu.o() reverted to the exact stock implementation from
 * the shipped 176071834 classes2.dex (removes the conflicting 120ms
 * ViewPropertyAnimator that fights the stock 300ms AnimatorSet in
 * Lqmu.i()). Everything else: default (byte-identical). Zero new refs.
 * Usage: RevertOCombinedPatch <input838.dex> <stock834.dex> <output.dex>
 */
public class RevertOCombinedPatch {
    static int oCount = 0;
    static MethodImplementation stockImpl;

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null && method.getDefiningClass().equals("Lqmu;")) {
                        if (method.getName().equals("o")
                                && method.getParameters().size() == 1
                                && method.getParameters().get(0).getType().equals("Lqmc;")) {
                            MethodImplementation ni = RevertOPatch.revertO(impl, stockImpl);
                            if (ni == null) throw new RuntimeException("revertO failed");
                            oCount++;
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
        DexBackedDexFile stockDex = DexFileFactory.loadDexFile(
            new java.io.File(args[1]), Opcodes.forApi(35));
        stockImpl = null;
        for (org.jf.dexlib2.iface.ClassDef cd : stockDex.getClasses()) {
            if (!cd.getType().equals("Lqmu;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("o") && m.getParameters().size() == 1
                        && m.getParameters().get(0).getType().equals("Lqmc;")) {
                    stockImpl = m.getImplementation();
                }
            }
        }
        if (stockImpl == null) {
            System.out.println("ERROR: stock Lqmu.o not found in " + args[1]);
            System.exit(1);
        }
        System.out.println("stock o(): regs=" + stockImpl.getRegisterCount());
        DexBackedDexFile dex = DexFileFactory.loadDexFile(
            new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[2], out);
        System.out.println("patched: o=" + oCount);
        if (oCount != 1) {
            System.out.println("ERROR: expected exactly 1 o");
            System.exit(1);
        }
    }
}
