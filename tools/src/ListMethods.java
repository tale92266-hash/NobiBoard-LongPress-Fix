import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import java.io.File;

public class ListMethods {
    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        String want = args[1];
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals(want)) continue;
            System.out.println("CLASS " + cd.getType() + " super=" + cd.getSuperclass() + " flags=" + cd.getAccessFlags());
            System.out.println("  fields:");
            for (Field f : cd.getFields()) System.out.println("    " + f.getType() + " " + f.getName() + " flags=" + f.getAccessFlags());
            System.out.println("  methods:");
            for (Method m : cd.getMethods()) {
                StringBuilder sb = new StringBuilder("    ").append(m.getName()).append("(");
                for (CharSequence p : m.getParameterTypes()) sb.append(p);
                sb.append(")").append(m.getReturnType()).append(" flags=").append(m.getAccessFlags());
                MethodImplementation impl = m.getImplementation();
                if (impl == null) sb.append(" [abstract/native]");
                else { int n = 0; for (Instruction i : impl.getInstructions()) n++; sb.append(" regs=").append(impl.getRegisterCount()).append(" insns=").append(n).append(" tries=").append(impl.getTryBlocks().size()); }
                System.out.println(sb);
            }
        }
    }
}
