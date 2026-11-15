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
 * Simple Animated (code 176071835) — ONE dexlib2 pass over the shipped
 * 176071834 classes2.dex:
 *   Lqml.k  -> fixed head (full-cell direction pixels + n() call) — patchK.
 *   Lqmu.c  -> loop-exit IF_GE retargeted 99 -> 70 (init block reachable) — patchC.
 *   Lqmu.o  -> setTranslationX/Y become 120ms ViewPropertyAnimator glides — patchO.
 *   Lqml.e  -> UNTOUCHED (already correct in 176071834).
 * Everything else: default (byte-identical). Zero new refs.
 * Usage: SimpleAnimatedCombinedPatch <input.dex> <output.dex>
 */
public class SimpleAnimatedCombinedPatch {
    static int kCount = 0, cCount = 0, oCount = 0;

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
                            MethodImplementation ni = SimpleAnimatedPopupPatch.patchK(impl);
                            if (ni == null) throw new RuntimeException("patchK failed");
                            kCount++;
                            return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                method.getParameters(), method.getReturnType(),
                                method.getAccessFlags(), method.getAnnotations(), null, ni);
                        }
                    }
                    if (impl != null && method.getDefiningClass().equals("Lqmu;")) {
                        if (method.getName().equals("c")
                                && method.getParameters().size() == 7) {
                            MethodImplementation ni = SimpleAnimatedPopupPatch.patchC(impl);
                            if (ni == null) throw new RuntimeException("patchC failed");
                            cCount++;
                            return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                method.getParameters(), method.getReturnType(),
                                method.getAccessFlags(), method.getAnnotations(), null, ni);
                        }
                        if (method.getName().equals("o")
                                && method.getParameters().size() == 1
                                && method.getParameters().get(0).getType().equals("Lqmc;")) {
                            MethodImplementation ni = SimpleAnimatedPopupPatch.patchO(impl);
                            if (ni == null) throw new RuntimeException("patchO failed");
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
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched: k=" + kCount + " c=" + cCount + " o=" + oCount);
        if (kCount != 1 || cCount != 1 || oCount != 1) {
            System.out.println("ERROR: expected exactly 1 k, 1 c, 1 o");
            System.exit(1);
        }
    }
}
