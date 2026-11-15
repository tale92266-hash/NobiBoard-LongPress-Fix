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
 * Fix5 (code 176071841) — ONE dexlib2 pass over the shipped 176071840
 * classes2.dex: fix the Fix4 VerifyError regression (v11 merge conflict)
 * and make the centering null-safe.
 *   Lqmu.o(Lqmc;)V : Fix4 (37 insns, 8 regs) -> Fix5 null-safe centered
 *                    (41 insns, 8 regs)
 *   Lqmu.i(II)V    : Fix4 block (21 insns/38u) -> Fix5 block (24 insns/48u:
 *                    v8 for refs, v19 (I32 at entry, dead after 0x21a)
 *                    for ints, null/width/height fallbacks); 5 branch
 *                    offsets changed (+10). v19 (not v11) because v11 is
 *                    the high half of a live double at 0x21a (used at
 *                    0x294 mul-double) -- writing an int there both
 *                    corrupts the double AND conflicts at the FALLBACK
 *                    merge. (v23 was tried first and is also wrong: it
 *                    is the high half of the v22/v23 double, I64.)
 * Everything else: default (byte-identical). Zero new refs.
 * Usage: Fix5CombinedPatch <input840.dex> <output.dex>
 */
public class Fix5CombinedPatch {
    static int oCount = 0, iCount = 0;

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
                            MethodImplementation ni = Fix5CenterPatch.patchO(impl);
                            if (ni == null) throw new RuntimeException("patchO failed");
                            oCount++;
                            return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                method.getParameters(), method.getReturnType(),
                                method.getAccessFlags(), method.getAnnotations(), null, ni);
                        }
                        if (method.getName().equals("i")
                                && method.getParameters().size() == 2
                                && method.getParameters().get(0).getType().equals("I")
                                && method.getParameters().get(1).getType().equals("I")) {
                            MethodImplementation ni = Fix5CenterPatch.patchI(impl);
                            if (ni == null) throw new RuntimeException("patchI failed");
                            iCount++;
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
        DexBackedDexFile dex = DexFileFactory.loadDexFile(
            new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched: o=" + oCount + " i=" + iCount);
        if (oCount != 1 || iCount != 1) {
            System.out.println("ERROR: expected exactly 1 o and 1 i");
            System.exit(1);
        }
    }
}
