import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
public class DumpBranches {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        String cls = args[1], mname = args[2];
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(cls)) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(mname)) continue;
                MethodImplementation impl = m.getImplementation();
                if (impl == null) { System.out.println("no impl"); return; }
                int addr = 0, i = 0;
                for (Instruction ins : impl.getInstructions()) {
                    String extra = "";
                    if (ins instanceof Instruction10t) extra = " -> " + (addr + ((Instruction10t)ins).getCodeOffset());
                    else if (ins instanceof Instruction20t) extra = " -> " + (addr + ((Instruction20t)ins).getCodeOffset());
                    else if (ins instanceof Instruction21t) extra = " reg=" + ((Instruction21t)ins).getRegisterA() + " -> " + (addr + ((Instruction21t)ins).getCodeOffset());
                    else if (ins instanceof Instruction22t) extra = " regs=" + ((Instruction22t)ins).getRegisterA() + "," + ((Instruction22t)ins).getRegisterB() + " -> " + (addr + ((Instruction22t)ins).getCodeOffset());
                    else if (ins instanceof Instruction30t) extra = " -> " + (addr + ((Instruction30t)ins).getCodeOffset());
                    else if (ins instanceof Instruction31t) extra = " reg=" + ((Instruction31t)ins).getRegisterA() + " -> " + (addr + ((Instruction31t)ins).getCodeOffset());
                    System.out.println(addr + " [" + i + "]: " + ins.getOpcode() + extra);
                    addr += ins.getCodeUnits(); i++;
                }
            }
        }
    }
}
