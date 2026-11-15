import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import java.io.File;
public class TryCheck {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(args[1])) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(args[2])) continue;
                MethodImplementation impl = m.getImplementation();
                int n = countInsns(impl); System.out.println(m.getName() + " regs=" + impl.getRegisterCount() + " insns=" + countInsns(impl) + " tries=" + impl.getTryBlocks().size());
                for (TryBlock tb : impl.getTryBlocks()) {
                    System.out.println("  try start=" + tb.getStartCodeAddress() + " count=" + tb.getCodeUnitCount() + " handlers=" + tb.getExceptionHandlers().size());
                }
                // check for switch/array payloads
                for (org.jf.dexlib2.iface.instruction.Instruction ins : impl.getInstructions()) {
                    String op = ins.getOpcode().name();
                    if (op.contains("SWITCH") || op.contains("FILL_ARRAY")) System.out.println("  payload-op: " + op + " at " + ins.getCodeUnits());
                }
            }
        }
    }
    static int countInsns(MethodImplementation impl) { int n=0; for (Object o : impl.getInstructions()) n++; return n; }
}
