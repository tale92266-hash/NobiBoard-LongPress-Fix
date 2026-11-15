import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;

import java.io.File;

/** Print registers, code size and try blocks for one method. Usage: TryBlocks <dex> <class> <name> */
public class TryBlocks {
    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(args[1])) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(args[2])) continue;
                MethodImplementation impl = m.getImplementation();
                if (impl == null) { System.out.println(m + " no impl"); continue; }
                int n = 0;
                for (Object x : impl.getInstructions()) n++;
                System.out.println("METHOD " + c.getType() + "->" + m.getName() + m.getParameters() + m.getReturnType()
                    + " registers=" + impl.getRegisterCount() + " insns=" + n);
                System.out.println("  tryBlocks=" + impl.getTryBlocks().size());
                for (TryBlock tb : impl.getTryBlocks()) {
                    int hn = 0;
                    for (Object x : tb.getExceptionHandlers()) hn++;
                    System.out.println("    start=" + tb.getStartCodeAddress() + " count=" + tb.getCodeUnitCount()
                        + " handlers=" + hn);
                    java.util.Iterator<?> it = tb.getExceptionHandlers().iterator();
                    while (it.hasNext()) {
                        org.jf.dexlib2.iface.ExceptionHandler h = (org.jf.dexlib2.iface.ExceptionHandler) it.next();
                        System.out.println("      catch " + h.getExceptionType() + " -> " + h.getHandlerCodeAddress());
                    }
                }
            }
        }
    }
}
