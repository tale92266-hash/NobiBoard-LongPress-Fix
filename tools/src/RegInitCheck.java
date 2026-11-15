import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import java.io.File;
import java.util.*;

/**
 * Register-initialization check (what VerifyPatch does NOT do): for one
 * method, compute by fixpoint (must-analysis) which registers are
 * definitely written on every path reaching each instruction, and report
 * every read of a register that is not definitely written. A read of an
 * undefined register is a VerifyError under ART's verifier (the register
 * has no type). Usage: RegInitCheck <dex> <class> <method>
 * Prints the parameter registers, then one line per violation, then a
 * summary. Exit 0 always (it reports; the caller judges).
 */
public class RegInitCheck {
    static void fail(String msg) { throw new RuntimeException("RegInitCheck: " + msg); }

    static int destReg(Instruction ins) {
        Opcode op = ins.getOpcode();
        if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                || op == Opcode.FILL_ARRAY_DATA) fail("payload opcode: " + op);
        if (ins instanceof Instruction11x) {
            if (op == Opcode.MOVE_RESULT || op == Opcode.MOVE_RESULT_OBJECT
                    || op == Opcode.MOVE_RESULT_WIDE || op == Opcode.MOVE_EXCEPTION)
                return ((Instruction11x) ins).getRegisterA();
            return -1;
        }
        if (ins instanceof Instruction11n) return ((Instruction11n) ins).getRegisterA();
        if (ins instanceof Instruction12x) return ((Instruction12x) ins).getRegisterA();
        if (ins instanceof Instruction20t) return -1;
        if (ins instanceof Instruction21c) return ((Instruction21c) ins).getRegisterA();
        if (ins instanceof Instruction21s) return ((Instruction21s) ins).getRegisterA();
        if (ins instanceof Instruction21ih) return ((Instruction21ih) ins).getRegisterA();
        if (ins instanceof Instruction21lh) return ((Instruction21lh) ins).getRegisterA();
        if (ins instanceof Instruction21t) return -1;
        if (ins instanceof Instruction22c) {
            if (op.name().startsWith("IPUT")) return -1;
            return ((Instruction22c) ins).getRegisterA();
        }
        if (ins instanceof Instruction22x) return ((Instruction22x) ins).getRegisterA();
        if (ins instanceof Instruction22b) return ((Instruction22b) ins).getRegisterA();
        if (ins instanceof Instruction22s) return ((Instruction22s) ins).getRegisterA();
        if (ins instanceof Instruction22t) return -1;
        if (ins instanceof Instruction23x) return ((Instruction23x) ins).getRegisterA();
        if (ins instanceof Instruction31c) return ((Instruction31c) ins).getRegisterA();
        if (ins instanceof Instruction31i) return ((Instruction31i) ins).getRegisterA();
        if (ins instanceof Instruction32x) return ((Instruction32x) ins).getRegisterA();
        if (ins instanceof Instruction35c) return -1;
        if (ins instanceof Instruction3rc) return -1;
        if (ins instanceof Instruction10x) return -1;
        if (ins instanceof Instruction10t) return -1;
        if (ins instanceof Instruction30t) return -1;
        if (ins instanceof Instruction51l) return ((Instruction51l) ins).getRegisterA();
        fail("unhandled format for dest: " + ins.getClass().getSimpleName() + " " + op);
        return -1;
    }

    /** Registers READ by ins (as a set). Wide reads count both halves. */
    static Set<Integer> readRegs(Instruction ins) {
        Set<Integer> r = new HashSet<>();
        Opcode op = ins.getOpcode();
        if (ins instanceof Instruction11x) {
            if (op == Opcode.RETURN || op == Opcode.RETURN_WIDE || op == Opcode.RETURN_OBJECT
                    || op == Opcode.THROW || op == Opcode.MONITOR_ENTER || op == Opcode.MONITOR_EXIT)
                r.add(((Instruction11x) ins).getRegisterA());
            return r;
        }
        if (ins instanceof Instruction12x) {
            Instruction12x t = (Instruction12x) ins;
            if (op.name().endsWith("_2ADDR")) { r.add(t.getRegisterA()); r.add(t.getRegisterB()); }
            else r.add(t.getRegisterB());
            return r;
        }
        if (ins instanceof Instruction21c) {
            if (op == Opcode.CHECK_CAST) r.add(((Instruction21c) ins).getRegisterA());
            return r;
        }
        if (ins instanceof Instruction21s || ins instanceof Instruction21ih
                || ins instanceof Instruction21lh || ins instanceof Instruction11n) return r;
        if (ins instanceof Instruction21t) { r.add(((Instruction21t) ins).getRegisterA()); return r; }
        if (ins instanceof Instruction22c) {
            Instruction22c t = (Instruction22c) ins;
            if (op.name().startsWith("IPUT")) { r.add(t.getRegisterA()); r.add(t.getRegisterB()); }
            else r.add(t.getRegisterB());
            return r;
        }
        if (ins instanceof Instruction22x) { r.add(((Instruction22x) ins).getRegisterB()); return r; }
        if (ins instanceof Instruction22b) { r.add(((Instruction22b) ins).getRegisterB()); return r; }
        if (ins instanceof Instruction22s) { r.add(((Instruction22s) ins).getRegisterB()); return r; }
        if (ins instanceof Instruction22t) {
            Instruction22t t = (Instruction22t) ins;
            r.add(t.getRegisterA()); r.add(t.getRegisterB()); return r;
        }
        if (ins instanceof Instruction23x) {
            Instruction23x t = (Instruction23x) ins;
            r.add(t.getRegisterB()); r.add(t.getRegisterC()); return r;
        }
        if (ins instanceof Instruction31c || ins instanceof Instruction31i) return r;
        if (ins instanceof Instruction32x) { r.add(((Instruction32x) ins).getRegisterB()); return r; }
        if (ins instanceof Instruction35c) {
            Instruction35c t = (Instruction35c) ins;
            int n = t.getRegisterCount();
            int[] regs = {t.getRegisterC(), t.getRegisterD(), t.getRegisterE(), t.getRegisterF(), t.getRegisterG()};
            for (int i = 0; i < n; i++) r.add(regs[i]);
            return r;
        }
        if (ins instanceof Instruction3rc) {
            Instruction3rc t = (Instruction3rc) ins;
            for (int i = 0; i < t.getRegisterCount(); i++) r.add(t.getStartRegister() + i);
            return r;
        }
        if (ins instanceof Instruction10x || ins instanceof Instruction10t
                || ins instanceof Instruction20t || ins instanceof Instruction30t) return r;
        if (ins instanceof Instruction51l) return r;
        fail("unhandled format for reads: " + ins.getClass().getSimpleName() + " " + op);
        return r;
    }

    static boolean isReturnOrThrow(Instruction ins) {
        Opcode op = ins.getOpcode();
        return op == Opcode.RETURN_VOID || op == Opcode.RETURN || op == Opcode.RETURN_WIDE
            || op == Opcode.RETURN_OBJECT || op == Opcode.THROW;
    }

    static boolean isGoto(Instruction ins) {
        Opcode op = ins.getOpcode();
        return op == Opcode.GOTO || op == Opcode.GOTO_16 || op == Opcode.GOTO_32;
    }

    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        String cls = args[1], mname = args[2];
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals(cls)) continue;
            for (Method m : cd.getMethods()) {
                if (!m.getName().equals(mname)) continue;
                MethodImplementation impl = m.getImplementation();
                if (impl == null) { System.out.println("no impl"); return; }
                List<Instruction> list = new ArrayList<>();
                for (Instruction i : impl.getInstructions()) list.add(i);
                int n = list.size();
                int regs = impl.getRegisterCount();
                // parameter words: 1 for this (non-static) + param widths
                int insWords = m.getAccessFlags() == 0 || (m.getAccessFlags() & 0x8) == 0 ? 1 : 0;
                for (CharSequence p : m.getParameterTypes())
                    insWords += (p.toString().equals("J") || p.toString().equals("D")) ? 2 : 1;
                int firstParam = regs - insWords;
                BitSet params = new BitSet(regs);
                for (int i = firstParam; i < regs; i++) params.set(i);
                System.out.println("METHOD " + cls + "->" + mname + " registers=" + regs
                    + " insWords=" + insWords + " params=v" + firstParam + "..v" + (regs - 1)
                    + " insns=" + n);
                // absolute offsets + successors
                int[] abs = new int[n];
                int off = 0;
                for (int i = 0; i < n; i++) { abs[i] = off; off += list.get(i).getCodeUnits(); }
                int total = off;
                Map<Integer, Integer> absToIdx = new HashMap<>();
                for (int i = 0; i < n; i++) absToIdx.put(abs[i], i);
                List<List<Integer>> succ = new ArrayList<>();
                for (int i = 0; i < n; i++) succ.add(new ArrayList<>());
                for (int i = 0; i < n; i++) {
                    Instruction ins = list.get(i);
                    if (isReturnOrThrow(ins)) continue;
                    if (ins instanceof OffsetInstruction) {
                        int tgt = abs[i] + ((OffsetInstruction) ins).getCodeOffset();
                        if (tgt == total) continue; // branch to end = exit
                        Integer j = absToIdx.get(tgt);
                        if (j == null) fail("branch at " + i + " targets unknown abs " + tgt);
                        succ.get(i).add(j);
                        if (!isGoto(ins)) succ.get(i).add(i + 1);
                    } else {
                        if (i + 1 < n) succ.get(i).add(i + 1);
                    }
                }
                // must-analysis fixpoint: in[i] = intersection of out[preds]
                BitSet[] in = new BitSet[n], out = new BitSet[n];
                for (int i = 0; i < n; i++) {
                    in[i] = new BitSet(regs); in[i].set(0, regs); // top (all written)
                    // FIX (Fix2): out[] MUST also start at TOP. Starting at EMPTY
                    // poisons any join fed by a not-yet-computed predecessor
                    // (backward branches / loops): the intersection with an
                    // empty out-set drops the parameters, and the cycle never
                    // re-acquires them — 35 false violations in Lqml.k and 13
                    // in Lqmu.c (identical 13 present in the WORKING 834 build).
                    // Must-analysis needs the GREATEST fixpoint (descend from TOP).
                    out[i] = new BitSet(regs); out[i].set(0, regs); // top
                }
                in[0] = (BitSet) params.clone();
                boolean changed = true;
                while (changed) {
                    changed = false;
                    for (int i = 0; i < n; i++) {
                        // in[i]: entry gets params; others get intersection of preds' out
                        BitSet ni;
                        if (i == 0) { ni = (BitSet) params.clone(); }
                        else {
                            ni = null;
                            for (int p = 0; p < n; p++) {
                                if (succ.get(p).contains(i)) {
                                    if (ni == null) ni = (BitSet) out[p].clone();
                                    else ni.and(out[p]);
                                }
                            }
                            if (ni == null) {
                                // Unreachable instruction (no predecessors, not the
                                // entry): TOP, not empty. ART's verifier only
                                // verifies reachable code (worklist from the
                                // entry), so unreachable code is never checked
                                // and must neither be flagged nor poison a join.
                                // (The 836 Lqml.k NOP pad at insn 93 is exactly
                                // this: unreachable, fall-through to STOCK at 94;
                                // empty here poisoned all 35 stock-body flags.)
                                ni = new BitSet(regs); ni.set(0, regs);
                            }
                        }
                        BitSet no = (BitSet) ni.clone();
                        int d = destReg(list.get(i));
                        if (d >= 0) {
                            no.set(d);
                            // wide dest writes the pair
                            if (list.get(i).getOpcode() == Opcode.MOVE_WIDE
                                    || list.get(i).getOpcode() == Opcode.MOVE_WIDE_FROM16
                                    || list.get(i).getOpcode() == Opcode.CONST_WIDE
                                    || list.get(i).getOpcode() == Opcode.CONST_WIDE_16
                                    || list.get(i).getOpcode() == Opcode.CONST_WIDE_32
                                    || list.get(i).getOpcode() == Opcode.CONST_WIDE_HIGH16
                                    || list.get(i).getOpcode() == Opcode.MOVE_RESULT_WIDE)
                                no.set(d + 1);
                        }
                        if (!ni.equals(in[i]) || !no.equals(out[i])) {
                            in[i] = ni; out[i] = no; changed = true;
                        }
                    }
                }
                int violations = 0;
                for (int i = 0; i < n; i++) {
                    for (int r : readRegs(list.get(i))) {
                        if (!in[i].get(r)) {
                            System.out.println("VIOLATION insn " + i + ": reads v" + r
                                + " (not definitely written) : " + list.get(i).getOpcode());
                            violations++;
                        }
                    }
                }
                System.out.println("RegInitCheck: violations=" + violations);
            }
        }
    }
}
