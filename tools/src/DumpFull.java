import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import java.io.File;

public class DumpFull {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        String cls = args[1], mname = args[2];
        int from = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int to = args.length > 4 ? Integer.parseInt(args[4]) : Integer.MAX_VALUE;
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(cls)) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(mname)) continue;
                MethodImplementation impl = m.getImplementation();
                if (impl == null) { System.out.println("no impl"); return; }
                System.out.println("METHOD " + cls + "->" + mname + " regs=" + impl.getRegisterCount());
                for (TryBlock tb : impl.getTryBlocks()) {
                    System.out.println("TRY start=" + tb.getStartCodeAddress() + " count=" + tb.getCodeUnitCount() + " handlers=" + tb.getExceptionHandlers().size());
                }
                int i = 0;
                for (Instruction ins : impl.getInstructions()) {
                    if (i >= from && i <= to) System.out.println(i + ": " + ins.toString());
                    i++;
                }
            }
        }
    }
}
