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
 * Fix11-Popup (code 176071856) — ONE dexlib2 pass over the shipped
 * 176071855 classes2.dex: Lqmu.c() is replaced with Fix11's EXACT
 * implementation (101 insns, tracker-init to centre (1,1), stock guard).
 * Everything else: default (byte-identical). Zero new refs.
 *
 * Fingerprints asserted (fail loudly on mismatch):
 *   Fix11 c: Lqmu->c(7 params)V regs=14 insns=101 units=199 tryBlocks=0
 *   855   c: Lqmu->c(7 params)V regs=14 insns=436 units=811 tryBlocks=0
 * Fix11-ness checks: Fix11 c must contain NO prefs-hook markers
 * (no "nobi_longpress_emoji", no getSharedPreferences, no getString).
 * It DOES contain exactly one stock codePointAt (the stock guard's
 * fingerprint, idx 88) — that is expected and allowed.
 * Usage: ReplaceCWithFix11 <input855.dex> <fix11.dex> <output.dex>
 */
public class ReplaceCWithFix11 {
    static int cCount = 0;
    static MethodImplementation fix11C;

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
                    if (method.getDefiningClass().equals("Lqmu;") && isC(method)) {
                        assertFp("855 c", impl, 14, 436, 811, 0);
                        cCount++;
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, fix11C);
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile fix11Dex = DexFileFactory.loadDexFile(
            new java.io.File(args[1]), Opcodes.forApi(35));
        fix11C = null;
        for (org.jf.dexlib2.iface.ClassDef cd : fix11Dex.getClasses()) {
            if (cd.getType().equals("Lqmu;")) {
                for (Method m : cd.getMethods()) {
                    if (isC(m)) fix11C = m.getImplementation();
                }
            }
        }
        if (fix11C == null) { System.out.println("ERROR: Fix11 Lqmu.c not found in " + args[1]); System.exit(1); }
        assertFp("Fix11 c", fix11C, 14, 101, 199, 0);
        // Fix11-ness checks: no prefs-hook markers allowed.
        // NOTE: Instruction.toString() does NOT include the referenced
        // item in this dexlib2 version (the StockRevert pattern was
        // vacuous), so check the actual ReferenceInstruction refs.
        int codePointAt = 0;
        for (Instruction ins : fix11C.getInstructions()) {
            if (!(ins instanceof org.jf.dexlib2.iface.instruction.ReferenceInstruction)) continue;
            org.jf.dexlib2.iface.reference.Reference ref =
                ((org.jf.dexlib2.iface.instruction.ReferenceInstruction) ins).getReference();
            if (ref instanceof org.jf.dexlib2.iface.reference.MethodReference) {
                org.jf.dexlib2.iface.reference.MethodReference mr =
                    (org.jf.dexlib2.iface.reference.MethodReference) ref;
                String n = mr.getName();
                if (n.equals("getSharedPreferences") || n.equals("getString")) {
                    throw new RuntimeException("FIX11 c CONTAINS HOOK MARKER: " + mr.getDefiningClass() + "->" + n);
                }
                if (n.equals("codePointAt")) codePointAt++;
            } else if (ref instanceof org.jf.dexlib2.iface.reference.StringReference) {
                String sv = ((org.jf.dexlib2.iface.reference.StringReference) ref).getString();
                if (sv.equals("nobi_longpress_emoji")) {
                    throw new RuntimeException("FIX11 c CONTAINS HOOK MARKER: const-string nobi_longpress_emoji");
                }
            }
        }
        if (codePointAt != 1) {
            throw new RuntimeException("FIX11 c codePointAt count=" + codePointAt + " expected 1");
        }
        System.out.println("Fix11 c: no hook markers (nobi_longpress_emoji/getSharedPreferences/getString absent); codePointAt=1 (stock guard, expected)");
        DexBackedDexFile dex = DexFileFactory.loadDexFile(
            new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[2], out);
        System.out.println("patched: c=" + cCount);
        if (cCount != 1) {
            System.out.println("ERROR: expected exactly 1 c");
            System.exit(1);
        }
    }
}
