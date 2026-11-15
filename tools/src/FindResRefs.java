import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

public class FindResRefs {
    static String fmt(Instruction ins) {
        StringBuilder sb = new StringBuilder(ins.getOpcode().name());
        if (ins instanceof Instruction31i) {
            Instruction31i i = (Instruction31i) ins;
            sb.append(" v").append(i.getRegisterA()).append(", ").append(String.format("0x%08x", i.getNarrowLiteral()));
        } else if (ins instanceof Instruction21c) {
            Instruction21c i = (Instruction21c) ins;
            sb.append(" v").append(i.getRegisterA()).append(", ").append(i.getReference());
            if (i.getReference() instanceof StringReference) sb.append(" \"").append(((StringReference)i.getReference()).getString()).append("\"");
        } else if (ins instanceof Instruction11n) {
            Instruction11n i = (Instruction11n) ins;
            sb.append(" v").append(i.getRegisterA()).append(", ").append(i.getNarrowLiteral());
        } else if (ins instanceof Instruction11x) {
            sb.append(" v").append(((Instruction11x) ins).getRegisterA());
        } else if (ins instanceof Instruction21s) {
            Instruction21s i = (Instruction21s) ins;
            sb.append(" v").append(i.getRegisterA()).append(", ").append(i.getNarrowLiteral());
        } else if (ins instanceof Instruction35c) {
            Instruction35c i = (Instruction35c) ins;
            sb.append(" {");
            for (int r : new int[]{i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG()}) sb.append("v").append(r).append(",");
            sb.append("} ").append(i.getReference());
        } else if (ins instanceof Instruction10x) {
        } else if (ins instanceof Instruction22c) {
            Instruction22c i = (Instruction22c) ins;
            sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB()).append(", ").append(i.getReference());
        } else if (ins instanceof Instruction12x) {
            Instruction12x i = (Instruction12x) ins;
            sb.append(" v").append(i.getRegisterA()).append(", v").append(i.getRegisterB());
        }
        return sb.toString();
    }
    public static void main(String[] args) throws Exception {
        Set<Integer> targets = new HashSet<>();
        for (int i = 1; i < args.length; i++) targets.add((int)Long.parseLong(args[i].substring(2), 16));
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        for (ClassDef cls : dex.getClasses()) {
            for (Method m : cls.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                List<Instruction> list = new ArrayList<>();
                for (Instruction ins : impl.getInstructions()) list.add(ins);
                List<Integer> hits = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Instruction ins = list.get(i);
                    if (ins instanceof Instruction31i && targets.contains(((Instruction31i) ins).getNarrowLiteral())) hits.add(i);
                }
                if (hits.isEmpty()) continue;
                System.out.println("METHOD: " + cls.getType() + "->" + m.getName() + "(" + m.getParameterTypes() + ")" + m.getReturnType() + "  [total insns " + list.size() + "]");
                for (int h : hits) {
                    System.out.println("  --- hit at insn " + h + " ---");
                    for (int i = Math.max(0, h - 6); i <= Math.min(list.size() - 1, h + 6); i++) {
                        System.out.println("   " + (i == h ? ">>" : "  ") + i + ": " + fmt(list.get(i)));
                    }
                }
                System.out.println();
            }
        }
    }
}
