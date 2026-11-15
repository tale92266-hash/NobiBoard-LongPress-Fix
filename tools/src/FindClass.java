import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import java.io.File;

public class FindClass {
    public static void main(String[] args) throws Exception {
        // args: dexFile classType
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(args[1])) continue;
            System.out.println("CLASS " + c.getType() + " access=0x" + Integer.toHexString(c.getAccessFlags())
                + " super=" + c.getSuperclass() + " ifaces=" + c.getInterfaces());
            for (Method m : c.getMethods()) {
                System.out.println("  M " + m.getName() + "(" + m.getParameterTypes() + ")" + m.getReturnType()
                    + " impl=" + (m.getImplementation() != null));
            }
            for (Field f : c.getFields()) {
                System.out.println("  F " + f.getName() + " : " + f.getType());
            }
        }
    }
}
