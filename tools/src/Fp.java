import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import java.io.File;
public class Fp {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(args[1])) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(args[2])) continue;
                MethodImplementation impl = m.getImplementation();
                int insns = 0, units = 0;
                for (Instruction ins : impl.getInstructions()) { insns++; units += ins.getCodeUnits(); }
                System.out.println("cls=" + c.getType() + " m=" + m.getName() + " params=" + m.getParameters().size()
                    + " regs=" + impl.getRegisterCount() + " insns=" + insns + " units=" + units
                    + " tries=" + impl.getTryBlocks().size());
            }
        }
    }
}
