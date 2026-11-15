import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction10x;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.ImmutableTryBlock;
import org.jf.dexlib2.immutable.ImmutableExceptionHandler;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import java.util.ArrayList;
import java.util.List;

/**
 * NobiBoard CapsSugg (code 176071823): extend the v1.17 caps uppercase
 * coverage to the TWO suggestion funnels that bypass ouy.a.
 *
 * Investigation (dex-proven, 2026-11-11):
 * - C-funnel (main strip, LatinFixedCountCandidatesHolderView): single
 *   entry Levt.b(List, oqn, Z) -- the only Lpel.b impl for Latin.
 *   Fed by pej.c from: ouy.a (PATCHED v1.17), Lijm.dY (search ext),
 *   Litb.m (ext key handler), Limn.accept (async callback). The three
 *   non-ouy.a feeders deliver lowercase lists to the strip.
 * - K-funnel (ReadingText strip): single entry Lovc.G(List) -- the only
 *   caller of pej.k(List). Fed by Lpbk.ay (key dispatcher), Louy.N
 *   (via Lord.N/Lora.N <- AbstractAsyncIme.y, OutputProcessor.el,
 *   Lfxy.q, Lnms.run), ovc.q. OutputProcessor.el -> Lora.N is the
 *   after-space next-word trigger (Sam's 2026-11-10 14:44 repro:
 *   caps ON, reopen, suggestions AFTER A SPACE stay lowercase).
 *
 * Two new hooks, ONE dexlib2 pass on Square2 classes.dex (the 5 existing
 * caps hooks are NOT touched):
 * 6. Lovc.G(List): head = uppercaseCandidatesLive(list, this)
 *    (4 code units, even; 'this' is the LIVE ovc, so the live peq
 *    chain is read -- no stale-ouy-ovc question).
 * 7. Levt.b(List, oqn, Z): head = uppercaseCandidatesLive(list, null)
 *    + uppercaseCandidateLive(oqn, null) + nop
 *    (10 code units, even; null ovc -> CapsLockHelper.isCapsOnLive
 *    falls back to the saved pref, which fG/ovx.l keep current).
 * Both helpers are null-safe (null list/oqn -> original) and return the
 * original list unchanged when caps is OFF (no allocation, no behaviour
 * change). Head inserts only: no branch crosses the insert point, so
 * relative branch offsets stay valid. Try blocks ARE shifted by the
 * insert delta (V26 lesson) -- Levt.b has 1 try block at the tail
 * (start 636, count 3); ovc.G has none. The v1.17 head inserts kept
 * try blocks as-is only because those methods have 0 try blocks.
 */
public class CapsSuggPatch {
    static final String HELPER = "Lcom/nobiboard/helper/CapsLockHelper;";

    static final ImmutableMethodReference UPPERCASE_LIVE =
        new ImmutableMethodReference(HELPER, "uppercaseCandidatesLive",
            java.util.Arrays.asList("Ljava/util/List;", "Ldefpackage/ovc;"),
            "Ljava/util/List;");
    static final ImmutableMethodReference UPPERCASE_ONE_LIVE =
        new ImmutableMethodReference(HELPER, "uppercaseCandidateLive",
            java.util.Arrays.asList("Ldefpackage/oqn;", "Ldefpackage/ovc;"),
            "Ldefpackage/oqn;");

    static int patchedG = 0, patchedLevtB = 0;

    /**
     * Shift every try block (start + each handler address) by the head-insert
     * delta. For a head insert every address in the method is >= the insert
     * point, so all of them move by +delta; counts are unchanged.
     */
    static List<ImmutableTryBlock> shiftTryBlocks(MethodImplementation impl, int delta) {
        List<ImmutableTryBlock> out = new ArrayList<>();
        for (TryBlock<? extends ExceptionHandler> tb : impl.getTryBlocks()) {
            List<ImmutableExceptionHandler> handlers = new ArrayList<>();
            for (ExceptionHandler h : tb.getExceptionHandlers()) {
                handlers.add(new ImmutableExceptionHandler(
                    h.getExceptionType(), h.getHandlerCodeAddress() + delta));
            }
            out.add(new ImmutableTryBlock(
                tb.getStartCodeAddress() + delta, tb.getCodeUnitCount(), handlers));
        }
        return out;
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    String cls = method.getDefiningClass();
                    String name = method.getName();
                    MethodImplementation impl = method.getImplementation();
                    if (impl == null) return def.rewrite(method);

                    if (cls.equals("Lovc;") && name.equals("G")
                            && method.getParameters().size() == 1) {
                        MethodImplementation ni = patchOvcG(impl);
                        if (ni != null) {
                            patchedG++;
                            return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                method.getParameters(), method.getReturnType(),
                                method.getAccessFlags(), method.getAnnotations(), null, ni);
                        }
                    } else if (cls.equals("Levt;") && name.equals("b")
                            && method.getParameters().size() == 3) {
                        MethodImplementation ni = patchLevtB(impl);
                        if (ni != null) {
                            patchedLevtB++;
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

    /** Lovc.G(List): uppercase the list via the LIVE ovc (this = v3, list = v4). */
    static MethodImplementation patchOvcG(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (impl.getRegisterCount() != 5) {
            System.out.println("ovc.G: unexpected register count " + impl.getRegisterCount());
            return null;
        }
        if (insns.isEmpty() || insns.get(0).getOpcode() != Opcode.IGET_OBJECT) {
            System.out.println("ovc.G: first instruction is not IGET_OBJECT");
            return null;
        }
        List<Instruction> head = new ArrayList<>();
        head.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC,
            2, 4, 3, 0, 0, 0, UPPERCASE_LIVE));
        head.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        List<Instruction> all = new ArrayList<>();
        all.addAll(head);
        all.addAll(insns);
        System.out.println("ovc.G: uppercaseCandidatesLive(list, this) inserted at index 0 "
            + "(4 code units, even)");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, shiftTryBlocks(impl, 4), impl.getDebugItems());
    }

    /**
     * Levt.b(List, oqn, Z): uppercase list + single oqn via pref fallback
     * (null ovc). this=v8, list=v9, oqn=v10, bool=v11. v0 is a local that
     * the original body overwrites at its first instruction (IGET_BOOLEAN
     * v0, v8, h), so const/4 v0,0 is safe at head.
     */
    static MethodImplementation patchLevtB(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (impl.getRegisterCount() != 12) {
            System.out.println("Levt.b: unexpected register count " + impl.getRegisterCount());
            return null;
        }
        if (insns.isEmpty() || insns.get(0).getOpcode() != Opcode.IGET_BOOLEAN) {
            System.out.println("Levt.b: first instruction is not IGET_BOOLEAN");
            return null;
        }
        List<Instruction> head = new ArrayList<>();
        head.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        head.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC,
            2, 9, 0, 0, 0, 0, UPPERCASE_LIVE));
        head.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 9));
        head.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC,
            2, 10, 0, 0, 0, 0, UPPERCASE_ONE_LIVE));
        head.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 10));
        head.add(new ImmutableInstruction10x(Opcode.NOP));
        List<Instruction> all = new ArrayList<>();
        all.addAll(head);
        all.addAll(insns);
        System.out.println("Levt.b: uppercaseCandidatesLive(list, null) + "
            + "uppercaseCandidateLive(oqn, null) + nop inserted at index 0 "
            + "(10 code units, even)");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, shiftTryBlocks(impl, 10), impl.getDebugItems());
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(args[0], Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched: ovcG=" + patchedG + " levtB=" + patchedLevtB);
        if (patchedG != 1 || patchedLevtB != 1) {
            throw new RuntimeException("expected exactly 1 ovc.G and 1 Levt.b patch");
        }
    }
}
