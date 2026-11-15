import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import java.io.File;
public class Sig {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(args[1])) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(args[2])) continue;
                System.out.print(m.getDefiningClass() + "->" + m.getName() + "(");
                for (MethodParameter p : m.getParameters()) System.out.print(p.getType() + " ");
                System.out.println(")" + m.getReturnType() + " regs?");
            }
        }
    }
}
