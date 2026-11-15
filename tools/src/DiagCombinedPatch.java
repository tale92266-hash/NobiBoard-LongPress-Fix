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
 * NobiBoard DIAG (code 176071842) — ONE dexlib2 pass over the shipped
 * 176071841 classes2.dex: instruments ONLY Lqck.D(J)V (entry marker +
 * catch-handler log). Everything else: default (byte-identical).
 * Zero new refs. Usage: DiagCombinedPatch <input.dex> <output.dex>
 */
public class DiagCombinedPatch {
    static int dCount = 0;

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null && method.getDefiningClass().equals("Lqck;")
                            && method.getName().equals("D")
                            && method.getParameters().size() == 1
                            && method.getParameters().get(0).getType().equals("J")
                            && method.getReturnType().equals("V")) {
                        MethodImplementation ni = DiagPatch.patchD(impl);
                        if (ni == null) throw new RuntimeException("patchD failed");
                        dCount++;
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, ni);
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
        System.out.println("patched: D=" + dCount);
        if (dCount != 1) {
            System.out.println("ERROR: expected exactly 1 D");
            System.exit(1);
        }
    }
}
