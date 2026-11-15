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
 * Simple Rewrite (code 176071834) — ONE dexlib2 pass:
 *   Lqml.k  -> new simple head (patchK); stock body kept.
 *   Lqml.e  -> post-block removed (patchE); circle head + stock body kept.
 *   Lqmu.c  -> UNTOUCHED (tracker init to center, from 2Fix4).
 * Everything else: default (byte-identical).
 * Usage: SimpleRewriteCombinedPatch <input.dex> <output.dex>
 */
public class SimpleRewriteCombinedPatch {
    static int kCount = 0, eCount = 0;

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null && method.getDefiningClass().equals("Lqml;")) {
                        if (method.getName().equals("k")
                                && method.getParameters().size() == 2
                                && method.getParameters().get(0).getType().equals("F")
                                && method.getParameters().get(1).getType().equals("F")) {
                            MethodImplementation ni = SimplePopupPatch.patchK(impl);
                            if (ni == null) throw new RuntimeException("patchK failed");
                            kCount++;
                            return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                method.getParameters(), method.getReturnType(),
                                method.getAccessFlags(), method.getAnnotations(), null, ni);
                        }
                        if (method.getName().equals("e") && method.getParameters().isEmpty()) {
                            MethodImplementation ni = SimplePopupPatch.patchE(impl);
                            if (ni == null) throw new RuntimeException("patchE failed");
                            eCount++;
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
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched: k=" + kCount + " e=" + eCount);
        if (kCount != 1 || eCount != 1) {
            System.out.println("ERROR: expected exactly 1 k and 1 e");
            System.exit(1);
        }
    }
}
