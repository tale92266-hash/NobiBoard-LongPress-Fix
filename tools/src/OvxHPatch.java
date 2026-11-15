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
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.ImmutableTryBlock;
import org.jf.dexlib2.immutable.ImmutableExceptionHandler;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction22c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import java.util.ArrayList;
import java.util.List;

/**
 * NobiBoard IconsCapsFix (code 176071824), Issue 2: caps lock gets STUCK ON
 * after an OFF->ON cycle (Sam, 2026-11-11 19:39 IST).
 *
 * Root cause (dex-proven):
 * - The caps state lives in TWO places: the LIVE peq CAPS field
 *   (0x60000000000L, written by the shift/caps toggle in Lpbk.ay) and the
 *   SAVED pref (nobi_caps/caps_lock_on, the only thing the restore in
 *   Lovx.d reads: peq.ev(CAPS, isCapsOn())).
 * - The pref is saved ONLY by two indirect hooks: Lovx.l head
 *   (saveCapsStateFromPeq, v1.14 fix for "OFF never being saved") and
 *   LatinIme.fG head (saveCapsState). Both depend on guard conditions /
 *   broadcast paths (ovx.h's guards, Keyboard.ey -> Loqz.fG) that do not
 *   fire reliably on every shift tap. If a caps OFF toggle is never
 *   followed by a save before the next Lovx.d rebuild, the stale saved ON
 *   is restored and caps comes back ON at reopen -- and the user reads
 *   that as "caps won't turn OFF / stays ON".
 * - The toggle itself (Lpbk.ay :cond_20, peq.ev(CAPS, v5)) DOES run on
 *   screen; the defect is the missing save, not the toggle. (Lovx.f, the
 *   stale snapshot that gates :cond_20, stays true exactly because the
 *   pref stays ON -- same root cause.)
 *
 * Fix: ONE new hook, head of Lovx.h(Loak;)V -- the choke point every key
 * dispatch reaches at its end (only callers are Lpbk.ay:7422/7464, AFTER
 * the toggle in :cond_20 has written the peq). It saves the CURRENT live
 * peq caps state, so every shift tap (ON or OFF) is persisted at the
 * moment the toggle lands, whether or not Lovx.l / fG fire.
 *   head = iget-object v0, p0(Lovx.this=v6), Lovx;->b:Lpeq;
 *          invoke-static {v0}, CapsLockHelper.saveCapsStateFromPeq(peq)
 *          nop
 * (6 code units, even; v0 is free at head -- the original body overwrites
 * it at its first MOVE_RESULT. Lovx.h has 0 try blocks. Head insert only:
 * no branch crosses the insert point, so relative branch offsets stay
 * valid. The helper is null-safe (null peq / null Context -> no-op) and
 * uses apply(), so no sync disk I/O on the keypress path.)
 * Does NOT touch the 5 v1.17 caps hooks or the 2 CapsSugg hooks.
 */
public class OvxHPatch {
    static final String HELPER = "Lcom/nobiboard/helper/CapsLockHelper;";

    static final ImmutableMethodReference SAVE_FROM_PEQ =
        new ImmutableMethodReference(HELPER, "saveCapsStateFromPeq",
            java.util.Arrays.asList("Ldefpackage/peq;"), "V");

    static final ImmutableFieldReference OVX_B =
        new ImmutableFieldReference("Lovx;", "b", "Lpeq;");
    // CRASHFIX 176071825: the field type MUST be the real dex type Lpeq;
    // (the field is declared ".field public b:Lpeq;"). The 176071824 build
    // used "Ldefpackage/peq;" here (the METHOD-ref convention, correct for
    // helper method signatures but WRONG for a field ref) -- the field ref
    // then does not resolve, ART's verifier rejects Lovx.h with VerifyError
    // at class load, and the app dies at startup with no crash log.

    static int patchedH = 0;

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

                    if (cls.equals("Lovx;") && name.equals("h")
                            && method.getParameters().size() == 1) {
                        MethodImplementation ni = patchOvxH(impl);
                        if (ni != null) {
                            patchedH++;
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

    /**
     * Lovx.h(Loak;): save the live peq caps state at every key-dispatch end.
     * Registers: 8 total (locals v0-v5, this=v6, oak=v7). v0 is free at head.
     */
    static MethodImplementation patchOvxH(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (impl.getRegisterCount() != 8) {
            System.out.println("ovx.h: unexpected register count " + impl.getRegisterCount());
            return null;
        }
        if (insns.isEmpty() || insns.get(0).getOpcode() != Opcode.INVOKE_VIRTUAL) {
            System.out.println("ovx.h: first instruction is not INVOKE_VIRTUAL");
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("ovx.h: unexpected try blocks: " + impl.getTryBlocks().size());
            return null;
        }
        List<Instruction> head = new ArrayList<>();
        head.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 6, OVX_B));
        head.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC,
            1, 0, 0, 0, 0, 0, SAVE_FROM_PEQ));
        head.add(new ImmutableInstruction10x(Opcode.NOP));
        List<Instruction> all = new ArrayList<>();
        all.addAll(head);
        all.addAll(insns);
        System.out.println("ovx.h: saveCapsStateFromPeq(this.b) + nop inserted at index 0 "
            + "(6 code units, even)");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, shiftTryBlocks(impl, 6), impl.getDebugItems());
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(args[0], Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched: ovxH=" + patchedH);
        if (patchedH != 1) {
            throw new RuntimeException("expected exactly 1 ovx.h patch");
        }
    }
}
