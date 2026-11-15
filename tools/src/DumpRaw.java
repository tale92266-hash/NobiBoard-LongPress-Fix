import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import java.io.File;
import java.util.*;
// Dump a method's instructions with full operands.
public class DumpRaw {
    static String fmt(Instruction ins) {
        StringBuilder sb = new StringBuilder(ins.getOpcode().name());
        if (ins instanceof Instruction10x) {
        } else if (ins instanceof Instruction10t) {
            sb.append(" off=").append(((Instruction10t) ins).getCodeOffset());
        } else if (ins instanceof Instruction11x) {
            sb.append(" v").append(((Instruction11x) ins).getRegisterA());
        } else if (ins instanceof Instruction11n) {
            Instruction11n i = (Instruction11n) ins;
            sb.append(" v").append(i.getRegisterA()).append(" #").append(i.getNarrowLiteral());
        } else if (ins instanceof Instruction12x) {
            Instruction12x i = (Instruction12x) ins;
            sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB());
        } else if (ins instanceof Instruction20t) {
            sb.append(" off=").append(((Instruction20t) ins).getCodeOffset());
        } else if (ins instanceof Instruction21t) {
            Instruction21t i = (Instruction21t) ins;
            sb.append(" v").append(i.getRegisterA()).append(" off=").append(i.getCodeOffset());
        } else if (ins instanceof Instruction21ih) {
            Instruction21ih i = (Instruction21ih) ins;
            sb.append(" v").append(i.getRegisterA()).append(" #0x").append(Integer.toHexString(i.getNarrowLiteral()));
        } else if (ins instanceof Instruction21s) {
            Instruction21s i = (Instruction21s) ins;
            sb.append(" v").append(i.getRegisterA()).append(" #").append(i.getNarrowLiteral());
        } else if (ins instanceof Instruction22b) {
            Instruction22b i = (Instruction22b) ins;
            sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(" #").append(i.getNarrowLiteral());
        } else if (ins instanceof Instruction22c) {
            Instruction22c i = (Instruction22c) ins;
            sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(" ").append(i.getReference());
        } else if (ins instanceof Instruction22t) {
            Instruction22t i = (Instruction22t) ins;
            sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(" off=").append(i.getCodeOffset());
        } else if (ins instanceof Instruction23x) {
            Instruction23x i = (Instruction23x) ins;
            sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(", v").append(i.getRegisterC());
        } else if (ins instanceof Instruction30t) {
            sb.append(" off=").append(((Instruction30t) ins).getCodeOffset());
        } else if (ins instanceof Instruction35c) {
            Instruction35c i = (Instruction35c) ins;
            sb.append(" {");
            int[] rr = {i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG()};
            for (int r = 0; r < i.getRegisterCount(); r++) { if (r > 0) sb.append(","); sb.append("v").append(rr[r]); }
            sb.append("} ").append(i.getReference());
        } else if (ins instanceof Instruction3rc) {
            Instruction3rc i = (Instruction3rc) ins;
            sb.append(" {v").append(i.getStartRegister()).append("..v").append(i.getStartRegister() + i.getRegisterCount() - 1).append("} ").append(i.getReference());
        } else {
            sb.append(" [").append(ins.getClass().getSimpleName()).append("]");
        }
        return sb.toString();
    }
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        String cls = args[1], mname = args[2];
        int from = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int to = args.length > 4 ? Integer.parseInt(args[4]) : Integer.MAX_VALUE;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals(cls)) continue;
            for (Method m : cd.getMethods()) {
                if (!m.getName().equals(mname)) continue;
                MethodImplementation impl = m.getImplementation();
                List<Instruction> list = new ArrayList<>();
                List<Integer> absOff = new ArrayList<>();
                int off = 0;
                for (Instruction ins : impl.getInstructions()) { absOff.add(off); list.add(ins); off += ins.getCodeUnits(); }
                System.out.println("METHOD: " + cd.getType() + "->" + m.getName() + " registers=" + impl.getRegisterCount());
                for (int i = 0; i < list.size(); i++) {
                    if (i < from || i > to) continue;
                    System.out.println(String.format("%3d abs %04x: %s", i, absOff.get(i), fmt(list.get(i))));
                }
            }
        }
    }
}
