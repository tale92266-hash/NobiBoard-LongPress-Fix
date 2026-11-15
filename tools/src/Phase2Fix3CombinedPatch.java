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
 * Phase 2Fix3 (code 176071832): ONE dexlib2 pass applying ONLY the
 * Lqml.k() delta-head patch (PopupDeltaPatch). Lqml.e() is UNTOUCHED
 * (its post-block already forces center at open and is proven correct
 * in the 176071831 dex). V27 lesson: one dexlib2 read+write pass total.
 *
 * args: classes2.dex out.dex
 */
public class Phase2Fix3CombinedPatch {
    static int patchedK = 0;

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    if (method.getDefiningClass().equals("Lqml;")) {
                        MethodImplementation impl = method.getImplementation();
                        if (impl != null) {
                            if (method.getName().equals("k") && method.getParameters().size() == 2) {
                                MethodImplementation ni = PopupDeltaPatch.patchK(impl);
                                if (ni != null) {
                                    patchedK++;
                                    return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                        method.getParameters(), method.getReturnType(),
                                        method.getAccessFlags(), method.getAnnotations(), null, ni);
                                }
                            }
                        }
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patchedK=" + patchedK);
        if (patchedK != 1) throw new RuntimeException("expected exactly 1 Lqml.k patch");
    }
}
