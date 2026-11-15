import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import java.io.File;

public class FindSubclasses {
    public static void main(String[] args) throws Exception {
        // args: dexFile superType
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        for (ClassDef c : dex.getClasses()) {
            if (args[1].equals(c.getSuperclass())) {
                System.out.println("SUB " + c.getType() + " ifaces=" + c.getInterfaces());
                for (Method m : c.getMethods()) {
                    System.out.println("  M " + m.getName() + "(" + m.getParameterTypes() + ")" + m.getReturnType()
                        + " impl=" + (m.getImplementation() != null));
                }
            }
        }
    }
}
