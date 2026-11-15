import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import java.io.File;
import java.util.*;
public class BranchDump {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(args[1])) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(args[2])) continue;
                MethodImplementation impl = m.getImplementation();
                List<Instruction> ins = new ArrayList<>();
                for (Instruction i : impl.getInstructions()) ins.add(i);
                List<Integer> addr = new ArrayList<>();
                Map<Integer,Integer> byAddr = new HashMap<>();
                int a = 0;
                for (int i = 0; i < ins.size(); i++) { addr.add(a); byAddr.put(a, i); a += ins.get(i).getCodeUnits(); }
                for (int i = 0; i < ins.size(); i++) {
                    Instruction in = ins.get(i);
                    if (in instanceof OffsetInstruction) {
                        int t = addr.get(i) + ((OffsetInstruction) in).getCodeOffset();
                        System.out.println(i + " addr=" + Integer.toHexString(addr.get(i)) + " " + in.getOpcode() + " -> idx " + byAddr.get(t) + " (addr " + Integer.toHexString(t) + ")");
                    }
                }
            }
        }
    }
}
