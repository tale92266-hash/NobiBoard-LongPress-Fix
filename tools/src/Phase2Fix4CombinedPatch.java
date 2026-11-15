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
 * Phase 2Fix4 (code 176071833): ONE dexlib2 pass applying BOTH tracker
 * patches (V27 lesson: one dexlib2 read+write pass total):
 *   - Lqml.k(): PopupTrackerPatch -- delta direction now routes through
 *     this.i(px,py) (virtual dispatch to Lqmu.i for Silk) instead of
 *     h(view) (which only writes Lqml.g, never read by Silk).
 *   - Lqmu.c(): TrackerInitPatch -- guarded pre-p() block sets the tracker
 *     Points b/c to (1,1) (center) for emoji popups.
 * Lqml.e() is UNTOUCHED (its post-block already forces g=3; harmless for
 * Silk, correct for Material).
 *
 * args: classes2.dex out.dex
 */
public class Phase2Fix4CombinedPatch {
    static int patchedK = 0;
    static int patchedC = 0;

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null) {
                        if (method.getDefiningClass().equals("Lqml;")
                                && method.getName().equals("k")
                                && method.getParameters().size() == 2) {
                            MethodImplementation ni = PopupTrackerPatch.patchK(impl);
                            if (ni != null) {
                                patchedK++;
                                return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                    method.getParameters(), method.getReturnType(),
                                    method.getAccessFlags(), method.getAnnotations(), null, ni);
                            }
                        }
                        if (method.getDefiningClass().equals("Lqmu;")
                                && method.getName().equals("c")
                                && method.getParameters().size() == 7) {
                            MethodImplementation ni = TrackerInitPatch.patchC(impl);
                            if (ni != null) {
                                patchedC++;
                                return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                    method.getParameters(), method.getReturnType(),
                                    method.getAccessFlags(), method.getAnnotations(), null, ni);
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
        System.out.println("patchedK=" + patchedK + " patchedC=" + patchedC);
        if (patchedK != 1 || patchedC != 1) throw new RuntimeException("expected exactly 1 k + 1 c patch");
    }
}
