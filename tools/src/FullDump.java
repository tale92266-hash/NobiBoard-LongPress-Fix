import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

public class FullDump {
    static String fmt(Instruction ins, int addr, Map<Integer,Integer> addrToIdx) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%4d @%04x ", addrToIdx.getOrDefault(addr, -1), addr));
        sb.append(ins.getOpcode().name());
        try {
            if (ins instanceof Instruction35c) {
                Instruction35c i = (Instruction35c) ins;
                int n = i.getRegisterCount();
                int[] r = {i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG()};
                sb.append(" {"); for (int k = 0; k < n; k++) sb.append("v").append(r[k]).append(",");
                sb.append("} ").append(i.getReference());
            } else if (ins instanceof Instruction3rc) {
                Instruction3rc i = (Instruction3rc) ins;
                sb.append(" {v").append(i.getStartRegister()).append("..v").append(i.getStartRegister()+i.getRegisterCount()-1).append("} ").append(i.getReference());
            } else if (ins instanceof Instruction22c) {
                Instruction22c i = (Instruction22c) ins;
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(", ").append(i.getReference());
            } else if (ins instanceof Instruction21c) {
                Instruction21c i = (Instruction21c) ins;
                sb.append(" v").append(i.getRegisterA()).append(", ").append(i.getReference());
                if (i.getReference() instanceof StringReference) sb.append(" \"").append(((StringReference)i.getReference()).getString()).append("\"");
            } else if (ins instanceof Instruction22x) {
                Instruction22x i = (Instruction22x) ins;
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB());
            } else if (ins instanceof Instruction23x) {
                Instruction23x i = (Instruction23x) ins;
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(", v").append(i.getRegisterC());
            } else if (ins instanceof Instruction12x) {
                Instruction12x i = (Instruction12x) ins;
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB());
            } else if (ins instanceof Instruction11x) {
                sb.append(" v").append(((Instruction11x) ins).getRegisterA());
            } else if (ins instanceof Instruction11n) {
                Instruction11n i = (Instruction11n) ins;
                sb.append(" v").append(i.getRegisterA()).append(", ").append(i.getNarrowLiteral());
            } else if (ins instanceof Instruction21s) {
                Instruction21s i = (Instruction21s) ins;
                sb.append(" v").append(i.getRegisterA()).append(", ").append(i.getNarrowLiteral());
            } else if (ins instanceof Instruction22s) {
                Instruction22s i = (Instruction22s) ins;
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(", ").append(i.getNarrowLiteral());
            } else if (ins instanceof Instruction22b) {
                Instruction22b i = (Instruction22b) ins;
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(", ").append(i.getNarrowLiteral());
            } else if (ins instanceof Instruction31i) {
                Instruction31i i = (Instruction31i) ins;
                sb.append(" v").append(i.getRegisterA()).append(", ").append(String.format("0x%08x (%d)", i.getNarrowLiteral(), i.getNarrowLiteral()));
            } else if (ins instanceof Instruction51l) {
                Instruction51l i = (Instruction51l) ins;
                sb.append(" v").append(i.getRegisterA()).append(", ").append(i.getWideLiteral());
            } else if (ins instanceof Instruction21ih) {
                Instruction21ih i = (Instruction21ih) ins;
                sb.append(" v").append(i.getRegisterA()).append(", ").append(String.format("0x%04x", i.getNarrowLiteral()));
            } else if (ins instanceof Instruction22t) {
                Instruction22t i = (Instruction22t) ins;
                int t = addr + i.getCodeOffset();
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(", -> @").append(String.format("%04x", t)).append(" (idx ").append(addrToIdx.getOrDefault(t, -1)).append(")");
            } else if (ins instanceof Instruction21t) {
                Instruction21t i = (Instruction21t) ins;
                int t = addr + i.getCodeOffset();
                sb.append(" v").append(i.getRegisterA()).append(", -> @").append(String.format("%04x", t)).append(" (idx ").append(addrToIdx.getOrDefault(t, -1)).append(")");
            } else if (ins instanceof Instruction10t) {
                Instruction10t i = (Instruction10t) ins;
                int t = addr + i.getCodeOffset();
                sb.append(" -> @").append(String.format("%04x", t)).append(" (idx ").append(addrToIdx.getOrDefault(t, -1)).append(")");
            } else if (ins instanceof Instruction20t) {
                Instruction20t i = (Instruction20t) ins;
                int t = addr + i.getCodeOffset();
                sb.append(" -> @").append(String.format("%04x", t)).append(" (idx ").append(addrToIdx.getOrDefault(t, -1)).append(")");
            } else if (ins instanceof Instruction30t) {
                Instruction30t i = (Instruction30t) ins;
                int t = addr + i.getCodeOffset();
                sb.append(" -> @").append(String.format("%04x", t)).append(" (idx ").append(addrToIdx.getOrDefault(t, -1)).append(")");
            } else if (ins instanceof Instruction31t) {
                Instruction31t i = (Instruction31t) ins;
                int t = addr + i.getCodeOffset();
                sb.append(" v").append(i.getRegisterA()).append(", -> @").append(String.format("%04x", t)).append(" (idx ").append(addrToIdx.getOrDefault(t, -1)).append(")");
            } else if (ins instanceof Instruction32x) {
                Instruction32x i = (Instruction32x) ins;
                sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB());
            }
        } catch (Exception e) { sb.append(" <fmt-err ").append(e.getMessage()).append(">"); }
        return sb.toString();
    }
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
                List<Instruction> list = new ArrayList<>();
                for (Instruction ins : impl.getInstructions()) list.add(ins);
                Map<Integer,Integer> addrToIdx = new HashMap<>();
                int addr = 0;
                for (int i = 0; i < list.size(); i++) { addrToIdx.put(addr, i); addr += list.get(i).getCodeUnits(); }
                System.out.println("METHOD " + cls + "->" + mname + " regs=" + impl.getRegisterCount() + " ins=" + list.size());
                for (TryBlock tb : impl.getTryBlocks()) System.out.println("TRY start=" + tb.getStartCodeAddress() + " count=" + tb.getCodeUnitCount());
                addr = 0;
                for (int i = 0; i < list.size(); i++) {
                    if (i >= from && i <= to) System.out.println(fmt(list.get(i), addr, addrToIdx));
                    addr += list.get(i).getCodeUnits();
                }
            }
        }
    }
}
