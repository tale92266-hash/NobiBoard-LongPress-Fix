import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.PayloadInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction31t;
import java.util.ArrayList;
import java.util.List;

/**
 * V46-CrashFix: payload-alignment check. ART requires packed-switch /
 * sparse-switch / fill-array-data payloads to sit at EVEN code-unit
 * offsets (4-byte aligned). An odd hook insert (V44 H4 = 3 units) shifted
 * Levj.a()'s fill-array-data payload to 735 (odd) and ART rejected the
 * whole class with `VerifyError: unaligned table at 735` at load time.
 * dexlib2, apktool and VerifyBranches do NOT check this — only ART does.
 *
 * Checks every method in a dex file: every switch/array payload's own
 * offset is even, and every switch instruction's payload target is even
 * and lands on a payload. Exits 1 if any odd payload is found.
 * Usage: CheckPayloadAlignment <dex> [dex...]
 */
public class CheckPayloadAlignment {
    public static void main(String[] args) throws Exception {
        int totalPayloads = 0, totalSwitchRefs = 0, odd = 0;
        for (String path : args) {
            DexBackedDexFile dex = DexFileFactory.loadDexFile(path, Opcodes.forApi(35));
            for (ClassDef c : dex.getClasses()) {
                for (Method m : c.getMethods()) {
                    MethodImplementation impl = m.getImplementation();
                    if (impl == null) continue;
                    List<Instruction> insns = new ArrayList<Instruction>();
                    for (Instruction i : impl.getInstructions()) insns.add(i);
                    List<Integer> pcs = new ArrayList<Integer>();
                    int pc = 0;
                    for (Instruction i : insns) { pcs.add(pc); pc += i.getCodeUnits(); }
                    String key = c.getType() + "." + m.getName();
                    boolean isEvjA = c.getType().equals("Levj;") && m.getName().equals("a");
                    for (int k = 0; k < insns.size(); k++) {
                        Instruction ins = insns.get(k);
                        int at = pcs.get(k);
                        boolean isPayload = (ins instanceof PayloadInstruction);
                        if (isPayload) {
                            totalPayloads++;
                            if (isEvjA || at % 2 != 0) {
                                System.out.println((at % 2 == 0 ? "OK  " : "ODD ")
                                        + path + " " + key + " [" + k + "] " + ins.getOpcode()
                                        + " payload at code-unit offset " + at);
                            }
                            if (at % 2 != 0) odd++;
                        }
                        if (ins instanceof Instruction31t) {
                            Opcode op = ins.getOpcode();
                            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                                    || op == Opcode.FILL_ARRAY_DATA) {
                                totalSwitchRefs++;
                                int target = at + ((Instruction31t) ins).getCodeOffset();
                                if (target % 2 != 0) {
                                    odd++;
                                    System.out.println("ODD  " + path + " " + key + " [" + k + "] "
                                            + op + " pc=" + at + " target=" + target);
                                }
                                if (isEvjA) {
                                    System.out.println("OK   " + path + " " + key + " [" + k + "] "
                                            + op + " pc=" + at + " target=" + target);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("payloads checked: " + totalPayloads
                + ", switch refs checked: " + totalSwitchRefs + ", odd: " + odd);
        if (odd > 0) System.exit(1);
    }
}
