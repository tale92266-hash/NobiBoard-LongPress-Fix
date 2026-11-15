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
 * Fix4 (code 176071840) — ONE dexlib2 pass over the shipped 176071839
 * classes2.dex: center the green highlight circle on the selected emoji.
 *   Lqmu.o(Lqmc;)V : stock (25 insns, 6 regs) -> centered (37 insns, 8 regs)
 *   Lqmu.i(II)V    : centering block inserted before insn 297 (+38 units,
 *                    5 crossing branches retargeted)
 * Everything else: default (byte-identical). Zero new method refs; exactly
 * 2 new field refs (ViewGroup$LayoutParams.width/height).
 * Usage: Fix4CombinedPatch <input839.dex> <stock834.dex> <output.dex>
 * (the stock834 dex supplies the reference stock o() for checkStock).
 */
public class Fix4CombinedPatch {
    static int oCount = 0, iCount = 0;
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
                            MethodImplementation ni = Fix4CenterPatch.patchO(impl, stockImpl);
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
                            MethodImplementation ni = Fix4CenterPatch.patchI(impl);
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
        System.out.println("patched: o=" + oCount + " i=" + iCount);
        if (oCount != 1 || iCount != 1) {
            System.out.println("ERROR: expected exactly 1 o and 1 i");
            System.exit(1);
        }
    }
}
