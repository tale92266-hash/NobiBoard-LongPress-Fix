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
 * Phase 2Fix2 (code 176071831): ONE dexlib2 pass applying BOTH patches.
 * V27 lesson: two separate dexlib2 read+write passes on the same dex break
 * the app at startup. So this single pass rewrites Lqml.e (center-force
 * post-block) AND Lqml.k (direction-based head) together.
 *
 * args: classes2.dex out.dex
 */
public class Phase2Fix2CombinedPatch {
    static int patchedE = 0;
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
                            if (method.getName().equals("e") && method.getParameters().size() == 0) {
                                MethodImplementation ni = EmojiPopupCenterForcePatch.patchE(impl);
                                if (ni != null) {
                                    patchedE++;
                                    return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                        method.getParameters(), method.getReturnType(),
                                        method.getAccessFlags(), method.getAnnotations(), null, ni);
                                }
                            }
                            if (method.getName().equals("k") && method.getParameters().size() == 2) {
                                MethodImplementation ni = PopupDirectionPatch.patchK(impl);
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
        System.out.println("patchedE=" + patchedE + " patchedK=" + patchedK);
        if (patchedE != 1) throw new RuntimeException("expected exactly 1 Lqml.e patch");
        if (patchedK != 1) throw new RuntimeException("expected exactly 1 Lqml.k patch");
    }
}
