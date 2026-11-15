import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.rewriter.*;
import java.io.File;
import java.util.*;

/**
 * Phase 3 FIX (code 176071852) — diagnosis-driven fix for the PROVEN
 * FieldCheck defect in shipped Phase 3 (176071851) Lqmu.c().
 *
 * Defect (diagnosis-hook.md): 7 field reads from Object-typed registers
 * (List.get() declared return) with NO narrowing -> ART class-load
 * VerifyError -> whole Lqmu erroneous -> popup never builds.
 *   idx 296,309,322,335,348: IGET_OBJECT v7, v7, Lpva;->d:Lpuz;
 *   idx 367: IGET v3, v2, Lqmc;->b:I
 *   idx 384: IGET_OBJECT v2, v2, Lqmc;->d:Landroid/view/View;
 *
 * Fix: narrow before each of the 7 reads with the EXACT pattern the stock
 * guard and the hook's fingerprint use:
 *   INSTANCE_OF <S>, <obj>, <Type>;  IF_EQZ <S> -> GUARD
 * on the fall-through path ART (and FieldCheck) refine <obj> to <Type>.
 * Scratch register S is chosen dead at each site (proven by register
 * liveness trace; see change report):
 *   - 5x setSlot sites: S = v0 (last read at codePointAt in the same block,
 *     next written by CONST_4 before any read; GUARD re-writes v0 anyway).
 *   - 2x label-walk sites: S = v3 (overwritten by the IGET at 367 / by the
 *     INSTANCE_OF TextView at 385 before any read; the slot index it held
 *     was consumed by the AGET_OBJECT at 383).
 * Skip target is ALWAYS the fail-safe GUARD (stock guard entry, orig idx 391).
 *
 * Each insertion = 2 insns / 4 code units (even); total +14 insns / +28 units.
 * All 7 insertions are before GUARD, so GUARD shifts by +28 (even) and every
 * subsequent offset keeps its parity. No new refs (INSTANCE_OF/IF_EQZ are
 * opcodes; Lpva/Lqmc types already referenced). No other change to the hook.
 */
public class FixPatch {
    static final ImmutableFieldReference LPVA_D =
        new ImmutableFieldReference("Lpva;", "d", "Lpuz;");
    static final ImmutableFieldReference QMC_B =
        new ImmutableFieldReference("Lqmc;", "b", "I");
    static final ImmutableFieldReference QMC_D =
        new ImmutableFieldReference("Lqmc;", "d", "Landroid/view/View;");
    static final ImmutableFieldReference LQML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableTypeReference T_LPVA = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference T_LQMC = new ImmutableTypeReference("Lqmc;");

    static final int GUARD_IDX = 391; // stock guard entry (Phase 3 coords)
    static final int[] SITES = {296, 309, 322, 335, 348, 367, 384};

    static RuntimeException die(String m) { return new RuntimeException("FixPatch: " + m); }

    static void check22c(Instruction ins, Opcode op, int regA, int regB,
                         ImmutableFieldReference ref, String where) {
        if (ins.getOpcode() != op) throw die(where + ": expected " + op + " got " + ins.getOpcode());
        if (!(ins instanceof Instruction22c)) throw die(where + ": not 22c");
        Instruction22c i = (Instruction22c) ins;
        if (i.getRegisterA() != regA || i.getRegisterB() != regB)
            throw die(where + ": regs v" + i.getRegisterA() + ",v" + i.getRegisterB()
                + " expected v" + regA + ",v" + regB);
        if (!i.getReference().equals(ref))
            throw die(where + ": ref " + i.getReference() + " expected " + ref);
    }
    static void check11x(Instruction ins, Opcode op, int regA, String where) {
        if (ins.getOpcode() != op) throw die(where + ": expected " + op + " got " + ins.getOpcode());
        if (!(ins instanceof Instruction11x)) throw die(where + ": not 11x");
        if (((Instruction11x) ins).getRegisterA() != regA)
            throw die(where + ": reg v" + ((Instruction11x) ins).getRegisterA() + " expected v" + regA);
    }
    static void check21t(Instruction ins, Opcode op, int regA, String where) {
        if (ins.getOpcode() != op) throw die(where + ": expected " + op + " got " + ins.getOpcode());
        if (!(ins instanceof Instruction21t)) throw die(where + ": not 21t");
        if (((Instruction21t) ins).getRegisterA() != regA)
            throw die(where + ": reg v" + ((Instruction21t) ins).getRegisterA() + " expected v" + regA);
    }
    static void check23x(Instruction ins, Opcode op, int regA, int regB, int regC, String where) {
        if (ins.getOpcode() != op) throw die(where + ": expected " + op + " got " + ins.getOpcode());
        if (!(ins instanceof Instruction23x)) throw die(where + ": not 23x");
        Instruction23x i = (Instruction23x) ins;
        if (i.getRegisterA() != regA || i.getRegisterB() != regB || i.getRegisterC() != regC)
            throw die(where + ": regs v" + i.getRegisterA() + ",v" + i.getRegisterB() + ",v" + i.getRegisterC()
                + " expected v" + regA + ",v" + regB + ",v" + regC);
    }

    static Instruction retarget(Instruction ins, int newOff) {
        if (ins instanceof Instruction21t) { Instruction21t o = (Instruction21t) ins;
            if (newOff < -32768 || newOff > 32767) throw die("21t out of range " + newOff + " at " + o.getOpcode());
            return new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), newOff); }
        if (ins instanceof Instruction22t) { Instruction22t o = (Instruction22t) ins;
            if (newOff < -32768 || newOff > 32767) throw die("22t out of range " + newOff);
            return new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(), o.getRegisterB(), newOff); }
        if (ins instanceof Instruction10t) { Instruction10t o = (Instruction10t) ins;
            if (newOff < -128 || newOff > 127) throw die("10t out of range " + newOff + " at " + o.getOpcode() + " (would need widening)");
            return new ImmutableInstruction10t(o.getOpcode(), newOff); }
        if (ins instanceof Instruction20t) { Instruction20t o = (Instruction20t) ins;
            if (newOff < -32768 || newOff > 32767) throw die("20t out of range " + newOff);
            return new ImmutableInstruction20t(o.getOpcode(), newOff); }
        if (ins instanceof Instruction30t) { Instruction30t o = (Instruction30t) ins;
            return new ImmutableInstruction30t(o.getOpcode(), newOff); }
        throw die("retarget on non-branch " + ins.getOpcode());
    }

    /** Patch Phase 3 Lqmu.c() (422 insns, 14 regs, no tries, no payloads). */
    static MethodImplementation patchC(MethodImplementation impl) {
        List<Instruction> orig = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) orig.add(i);
        if (orig.size() != 422 || impl.getRegisterCount() != 14)
            throw die("unexpected shape ins=" + orig.size() + " regs=" + impl.getRegisterCount() + " (expected Phase3 422/14)");
        if (!impl.getTryBlocks().isEmpty()) throw die("unexpected try blocks");
        for (Instruction i : orig) {
            Opcode op = i.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH || op == Opcode.FILL_ARRAY_DATA)
                throw die("unexpected payload " + op);
            if (op == Opcode.PACKED_SWITCH_PAYLOAD || op == Opcode.SPARSE_SWITCH_PAYLOAD || op == Opcode.ARRAY_PAYLOAD)
                throw die("unexpected payload insn " + op);
        }
        // ---- verify the 7 defect sites + their preceding instructions ----
        for (int s : new int[]{296, 309, 322, 335, 348}) {
            check22c(orig.get(s), Opcode.IGET_OBJECT, 7, 7, LPVA_D, "site " + s);
            check11x(orig.get(s - 1), Opcode.MOVE_RESULT_OBJECT, 7, "site " + s + "-1");
        }
        check22c(orig.get(367), Opcode.IGET, 3, 2, QMC_B, "site 367");
        check21t(orig.get(366), Opcode.IF_EQZ, 2, "site 366");
        check11x(orig.get(365), Opcode.MOVE_RESULT_OBJECT, 2, "site 365");
        check22c(orig.get(384), Opcode.IGET_OBJECT, 2, 2, QMC_D, "site 384");
        check23x(orig.get(383), Opcode.AGET_OBJECT, 4, 13, 3, "site 383");
        // ---- verify GUARD + tail fingerprints ----
        check22c(orig.get(GUARD_IDX), Opcode.IGET_OBJECT, 0, 6, LQML_E, "GUARD 391");
        if (orig.get(0).getOpcode() != Opcode.INVOKE_SUPER_RANGE) throw die("insn0 not super");
        if (orig.get(420).getOpcode() != Opcode.INVOKE_DIRECT || orig.get(421).getOpcode() != Opcode.RETURN_VOID)
            throw die("tail mismatch");
        // ---- absolute offsets of the original method ----
        List<Integer> origAbs = new ArrayList<>();
        int off = 0;
        for (Instruction i : orig) { origAbs.add(off); off += i.getCodeUnits(); }
        int origTotal = off;
        Map<Integer, Integer> absToIdx = new HashMap<>();
        for (int i = 0; i < orig.size(); i++) absToIdx.put(origAbs.get(i), i);
        // all original branch targets must be instruction boundaries (or method end)
        for (int n = 0; n < orig.size(); n++) {
            Instruction ins = orig.get(n);
            if (!(ins instanceof OffsetInstruction)) continue;
            int t = origAbs.get(n) + ((OffsetInstruction) ins).getCodeOffset();
            if (t != origTotal && !absToIdx.containsKey(t))
                throw die("branch at idx " + n + " targets non-boundary abs " + Integer.toHexString(t));
        }
        // ---- build new list: splice 7 snippets (INSTANCE_OF + IF_EQZ) ----
        Set<Integer> insAt = new HashSet<>();
        for (int s : SITES) insAt.add(s);
        List<Instruction> all = new ArrayList<>();
        int[] pos = new int[orig.size() + 1];
        List<Integer> newIfEqz = new ArrayList<>();
        for (int i = 0; i < orig.size(); i++) {
            if (insAt.contains(i)) {
                int resReg, objReg; ImmutableTypeReference type;
                if (i == 367 || i == 384) { resReg = 3; objReg = 2; type = T_LQMC; }
                else { resReg = 0; objReg = 7; type = T_LPVA; }
                all.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, resReg, objReg, type));
                newIfEqz.add(all.size());
                all.add(new ImmutableInstruction21t(Opcode.IF_EQZ, resReg, 0)); // placeholder
            }
            pos[i] = all.size();
            all.add(orig.get(i));
        }
        pos[orig.size()] = all.size();
        List<Integer> newAbs = new ArrayList<>();
        int a = 0;
        for (Instruction ins : all) { newAbs.add(a); a += ins.getCodeUnits(); }
        int newTotal = a;
        int grow = newTotal - origTotal;
        System.out.println("Lqmu.c: orig " + orig.size() + " insns / " + origTotal + " units -> "
            + all.size() + " insns / " + newTotal + " units (grow +" + grow + ")");
        if (grow != 28) throw die("unexpected grow " + grow + " (expected +28)");
        if (newTotal % 2 != 0 && origTotal % 2 == 0) throw die("parity flipped");
        // ---- fix the 7 new IF_EQZ -> GUARD ----
        int guardNewAbs = newAbs.get(pos[GUARD_IDX]);
        for (int p : newIfEqz) {
            int insnAbs = newAbs.get(p);
            int o = guardNewAbs - insnAbs;
            if (o == 0) throw die("new IF_EQZ self-loop at new idx " + p);
            if (o < -32768 || o > 32767) throw die("new IF_EQZ out of range " + o);
            Instruction21t old = (Instruction21t) all.get(p);
            all.set(p, new ImmutableInstruction21t(old.getOpcode(), old.getRegisterA(), o));
            System.out.println("new IF_EQZ at new idx " + p + " abs " + Integer.toHexString(insnAbs)
                + " -> GUARD abs " + Integer.toHexString(guardNewAbs) + " off +" + o);
        }
        // ---- retarget ORIGINAL branches ----
        int changed = 0;
        for (int i = 0; i < orig.size(); i++) {
            Instruction ins = orig.get(i);
            if (!(ins instanceof OffsetInstruction)) continue;
            int oldOff = ((OffsetInstruction) ins).getCodeOffset();
            int tgtAbs = origAbs.get(i) + oldOff;
            Integer tIdx = absToIdx.get(tgtAbs);
            int newTgtAbs;
            if (tIdx != null) newTgtAbs = newAbs.get(pos[tIdx]);
            else if (tgtAbs == origTotal) newTgtAbs = newTotal;
            else throw die("branch target lost at idx " + i);
            int newSrcAbs = newAbs.get(pos[i]);
            int newOff = newTgtAbs - newSrcAbs;
            if (newOff != oldOff) {
                all.set(pos[i], retarget(ins, newOff));
                changed++;
                System.out.println("retargeted origIdx " + i + " abs " + Integer.toHexString(origAbs.get(i))
                    + " -> tgt " + Integer.toHexString(tgtAbs) + " oldOff " + oldOff + " newOff " + newOff);
            }
        }
        System.out.println("retargeted " + changed + " original branches");
        return new ImmutableMethodImplementation(impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl != null && method.getDefiningClass().equals("Lqmu;")
                            && method.getName().equals("c") && method.getParameters().size() == 7) {
                        MethodImplementation ni = patchC(impl);
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
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes2 written to " + args[1]);
    }
}
