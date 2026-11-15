import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import java.io.File;
import java.util.*;

/** Debug for RegInitCheck: for one method, run the same must-analysis
 * (TOP init) and print, for each instruction, whether each parameter
 * register is in the in-set; plus the predecessors of the first
 * instruction where a parameter is lost. Usage: DebugReg <dex> <class> <method> */
public class DebugReg {
    static int destReg(Instruction ins) {
        Opcode op = ins.getOpcode();
        if (ins instanceof Instruction11x) {
            if (op == Opcode.MOVE_RESULT || op == Opcode.MOVE_RESULT_OBJECT
                    || op == Opcode.MOVE_EXCEPTION)
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
        return -1;
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
                List<Instruction> list = new ArrayList<>();
                for (Instruction i : impl.getInstructions()) list.add(i);
                int n = list.size();
                int regs = impl.getRegisterCount();
                int insWords = (m.getAccessFlags() & 0x8) == 0 ? 1 : 0;
                for (CharSequence p : m.getParameterTypes())
                    insWords += (p.toString().equals("J") || p.toString().equals("D")) ? 2 : 1;
                int firstParam = regs - insWords;
                BitSet params = new BitSet(regs);
                for (int i = firstParam; i < regs; i++) params.set(i);
                System.out.println("METHOD " + cls + "->" + mname + " registers=" + regs
                    + " params=v" + firstParam + "..v" + (regs - 1) + " insns=" + n
                    + " tryblocks=" + impl.getTryBlocks().size());
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
                        if (tgt == total) continue;
                        Integer j = absToIdx.get(tgt);
                        if (j == null) { System.out.println("BAD TARGET at " + i + " -> abs " + tgt); continue; }
                        succ.get(i).add(j);
                        if (!isGoto(ins)) succ.get(i).add(i + 1);
                    } else {
                        if (i + 1 < n) succ.get(i).add(i + 1);
                    }
                }
                // predecessors
                List<List<Integer>> pred = new ArrayList<>();
                for (int i = 0; i < n; i++) pred.add(new ArrayList<>());
                for (int i = 0; i < n; i++) for (int s : succ.get(i)) pred.get(s).add(i);
                // fixpoint from TOP
                BitSet[] in = new BitSet[n], out = new BitSet[n];
                for (int i = 0; i < n; i++) {
                    in[i] = new BitSet(regs); in[i].set(0, regs);
                    out[i] = new BitSet(regs); out[i].set(0, regs);
                }
                in[0] = (BitSet) params.clone();
                boolean changed = true;
                while (changed) {
                    changed = false;
                    for (int i = 0; i < n; i++) {
                        BitSet ni;
                        if (i == 0) ni = (BitSet) params.clone();
                        else {
                            ni = null;
                            for (int p : pred.get(i)) {
                                if (ni == null) ni = (BitSet) out[p].clone();
                                else ni.and(out[p]);
                            }
                            if (ni == null) ni = new BitSet(regs);
                        }
                        BitSet no = (BitSet) ni.clone();
                        int d = destReg(list.get(i));
                        if (d >= 0) {
                            no.set(d);
                            if (list.get(i).getOpcode() == Opcode.MOVE_WIDE
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
                // first insn where a param is lost from in-set
                for (int i = 0; i < n; i++) {
                    boolean lost = false;
                    for (int r = firstParam; r < regs; r++) if (!in[i].get(r)) lost = true;
                    if (lost) {
                        System.out.println("FIRST LOSS at insn " + i + " (" + list.get(i).getOpcode()
                            + "): in-set has params? " + in[i]);
                        System.out.println("  predecessors of " + i + ": " + pred.get(i));
                        for (int p : pred.get(i)) {
                            System.out.println("    pred " + p + " (" + list.get(p).getOpcode()
                                + ") out has params? " + out[p] + " succ=" + succ.get(p));
                        }
                        return;
                    }
                }
                System.out.println("NO LOSS: params present at every insn");
            }
        }
    }
}
