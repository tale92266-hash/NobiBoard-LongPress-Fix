import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import java.io.File;
import java.util.*;
public class CheckMethod {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        for (String spec : Arrays.copyOfRange(args, 1, args.length)) {
            String[] p = spec.split("->");
            String cls = p[0], mname = p[1];
            for (ClassDef cd : dex.getClasses()) {
                if (!cd.getType().equals(cls)) continue;
                for (Method m : cd.getMethods()) {
                    if (!m.getName().equals(mname)) continue;
                    MethodImplementation impl = m.getImplementation();
                    if (impl == null) { System.out.println(spec + ": no impl"); continue; }
                    List<String> payloads = new ArrayList<>();
                    int off = 0;
                    for (Instruction ins : impl.getInstructions()) {
                        String op = ins.getOpcode().name();
                        if (op.equals("PACKED_SWITCH_PAYLOAD") || op.equals("SPARSE_SWITCH_PAYLOAD")
                                || op.equals("ARRAY_PAYLOAD")) payloads.add(off + ":" + op);
                        off += ins.getCodeUnits();
                    }
                    System.out.println(spec + ": regs=" + impl.getRegisterCount()
                        + " insns=" + off + " codeUnits tryBlocks=" + impl.getTryBlocks().size()
                        + " payloads=" + payloads);
                    for (TryBlock tb : impl.getTryBlocks()) {
                        System.out.println("  try start=" + tb.getStartCodeAddress()
                            + " count=" + tb.getCodeUnitCount());
                    }
                }
            }
        }
    }
}
