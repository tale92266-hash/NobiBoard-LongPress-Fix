import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction30t;
import org.jf.dexlib2.iface.instruction.formats.Instruction31t;
import java.util.*;

/** Checks every branch target in every method lands on an instruction start. */
public class VerifyBranches {
    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(args[0], Opcodes.forApi(35));
        int bad = 0, checked = 0;
        Set<String> only = new HashSet<>();
        if (args.length > 1) for (int i = 1; i < args.length; i++) only.add(args[i]);
        for (ClassDef c : dex.getClasses()) {
            for (Method m : c.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                String key = c.getType() + "." + m.getName();
                if (!only.isEmpty() && !only.contains(key)) continue;
                List<Instruction> insns = new ArrayList<>();
                for (Instruction i : impl.getInstructions()) insns.add(i);
                // pc of each instruction
                List<Integer> pcs = new ArrayList<>();
                int pc = 0;
                for (Instruction i : insns) { pcs.add(pc); pc += i.getCodeUnits(); }
                Set<Integer> starts = new HashSet<>(pcs);
                for (int k = 0; k < insns.size(); k++) {
                    Instruction ins = insns.get(k);
                    Integer off = null;
                    if (ins instanceof Instruction10t) off = ((Instruction10t) ins).getCodeOffset();
                    else if (ins instanceof Instruction21t) off = ((Instruction21t) ins).getCodeOffset();
                    else if (ins instanceof Instruction22t) off = ((Instruction22t) ins).getCodeOffset();
                    else if (ins instanceof Instruction30t) off = ((Instruction30t) ins).getCodeOffset();
                    else if (ins instanceof Instruction31t) off = ((Instruction31t) ins).getCodeOffset();
                    if (off == null) continue;
                    checked++;
                    int target = pcs.get(k) + off;
                    if (!starts.contains(target)) {
                        bad++;
                        System.out.println("BAD " + key + " [" + k + "] " + ins.getOpcode()
                            + " pc=0x" + Integer.toHexString(pcs.get(k))
                            + " off=" + off + " target=0x" + Integer.toHexString(target));
                    }
                }
            }
        }
        System.out.println("branches checked: " + checked + ", bad: " + bad);
        if (bad > 0) System.exit(1);
    }
}
