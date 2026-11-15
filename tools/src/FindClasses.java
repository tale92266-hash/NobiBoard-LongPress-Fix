import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import java.io.File;

public class FindClasses {
    public static void main(String[] args) throws Exception {
        // args: dexFile needle [mode]  mode=impl => classes implementing interface needle
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        String needle = args[1];
        String mode = args.length > 2 ? args[2] : "name";
        for (ClassDef c : dex.getClasses()) {
            if (mode.equals("impl")) {
                if (c.getInterfaces().contains(needle)) {
                    System.out.println("IMPL " + c.getType() + " super=" + c.getSuperclass());
                }
            } else {
                if (c.getType().contains(needle)) {
                    System.out.println("CLASS " + c.getType() + " super=" + c.getSuperclass()
                        + " ifaces=" + c.getInterfaces());
                }
            }
        }
    }
}
