import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

// Enumerates every MethodReference used by any instruction in the dex,
// plus every method DEFINED in the dex. Prints one line per distinct ref:
//   <definingClass>-><name>(<params>)<return>
public class MethodRefScan {
    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        TreeSet<String> refs = new TreeSet<>();
        for (ClassDef cd : dex.getClasses()) {
            for (Method m : cd.getMethods()) {
                refs.add(m.getDefiningClass() + "->" + m.getName() + "(" + join(m.getParameterTypes()) + ")" + m.getReturnType());
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    if (ins instanceof ReferenceInstruction) {
                        Reference r = ((ReferenceInstruction) ins).getReference();
                        if (r instanceof MethodReference) {
                            MethodReference mr = (MethodReference) r;
                            refs.add(mr.getDefiningClass() + "->" + mr.getName() + "(" + join(mr.getParameterTypes()) + ")" + mr.getReturnType());
                        }
                    }
                }
            }
        }
        for (String s : refs) System.out.println(s);
        System.out.println("total distinct method refs (defined + referenced): " + refs.size());
    }
    static String join(Iterable<? extends CharSequence> it) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (CharSequence c : it) { if (!first) sb.append(","); first = false; sb.append(c); }
        return sb.toString();
    }
}
