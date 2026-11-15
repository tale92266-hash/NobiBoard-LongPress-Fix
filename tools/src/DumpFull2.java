import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import java.io.File;
import java.util.*;

public class DumpFull2 {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        String cls = args[1], mname = args[2];
        int from = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int to = args.length > 4 ? Integer.parseInt(args[4]) : Integer.MAX_VALUE;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals(cls)) continue;
            for (Method m : cd.getMethods()) {
                if (!m.getName().equals(mname)) continue;
                MethodImplementation impl = m.getImplementation();
                if (impl == null) { System.out.println("no impl"); return; }
                System.out.println("METHOD: " + cd.getType() + "->" + m.getName()
                    + " params=" + m.getParameters() + " ret=" + m.getReturnType()
                    + " regs=" + impl.getRegisterCount()
                    + " try=" + impl.getTryBlocks().size());
                List<Instruction> list = new ArrayList<>();
                List<Integer> absOff = new ArrayList<>();
                int off = 0;
                for (Instruction ins : impl.getInstructions()) { absOff.add(off); list.add(ins); off += ins.getCodeUnits(); }
                Map<Integer,Integer> off2idx = new HashMap<>();
                for (int i = 0; i < absOff.size(); i++) off2idx.put(absOff.get(i), i);
                for (int i = 0; i < list.size(); i++) {
                    if (i < from || i > to) continue;
                    Instruction ins = list.get(i);
                    String extra = "";
                    if (ins instanceof OffsetInstruction) {
                        int tgt = absOff.get(i) + ((OffsetInstruction) ins).getCodeOffset();
                        extra = "  -> insn " + off2idx.getOrDefault(tgt, -1) + " (abs " + tgt + ")";
                    }
                    System.out.println(i + " [abs " + absOff.get(i) + "]: " + ins + extra);
                }
            }
        }
    }
}
