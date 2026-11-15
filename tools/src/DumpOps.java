import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
public class DumpOps {
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
                    StringBuilder sb = new StringBuilder();
                    sb.append(addr).append(" [").append(i).append("]: ").append(ins.getOpcode());
                    if (ins instanceof Instruction23x) { Instruction23x f=(Instruction23x)ins; sb.append(" val=v").append(f.getRegisterA()).append(" arr=v").append(f.getRegisterB()).append(" idx=v").append(f.getRegisterC()); }
                    else if (ins instanceof Instruction35c) { Instruction35c f=(Instruction35c)ins; sb.append(" regs=[").append(f.getRegisterC()).append(",").append(f.getRegisterD()).append(",").append(f.getRegisterE()).append(",").append(f.getRegisterF()).append(",").append(f.getRegisterG()).append("] ref=").append(f.getReference()); }
                    else if (ins instanceof Instruction3rc) { Instruction3rc f=(Instruction3rc)ins; sb.append(" startReg=v").append(f.getStartRegister()).append(" count=").append(f.getRegisterCount()).append(" ref=").append(f.getReference()); }
                    else if (ins instanceof Instruction21c) { Instruction21c f=(Instruction21c)ins; sb.append(" v").append(f.getRegisterA()).append(" ref=").append(f.getReference()); }
                    else if (ins instanceof Instruction11n) { Instruction11n f=(Instruction11n)ins; sb.append(" v").append(f.getRegisterA()).append(" lit=").append(f.getNarrowLiteral()); }
                    else if (ins instanceof Instruction21s) { Instruction21s f=(Instruction21s)ins; sb.append(" v").append(f.getRegisterA()).append(" lit=").append(f.getNarrowLiteral()); }
                    else if (ins instanceof Instruction11x) { Instruction11x f=(Instruction11x)ins; sb.append(" v").append(f.getRegisterA()); }
                    else if (ins instanceof Instruction12x) { Instruction12x f=(Instruction12x)ins; sb.append(" v").append(f.getRegisterA()).append(" v").append(f.getRegisterB()); }
                    System.out.println(sb.toString());
                    addr += ins.getCodeUnits(); i++;
                }
            }
        }
    }
}
