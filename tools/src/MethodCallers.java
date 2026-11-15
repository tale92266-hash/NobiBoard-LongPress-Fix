// MethodCallers: find all callers of methods on a given class.
// Usage: MethodCallers <dex-dir> <defining-class> [method-name]
// Prints each caller's class->method and insn index.
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import java.io.File;
import java.util.*;

public class MethodCallers {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        String cls = args[1];
        String mname = args.length > 2 ? args[2] : null;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".dex"));
        Arrays.sort(files);
        for (File f : files) {
            DexFile dex = DexFileFactory.loadDexFile(f, Opcodes.forApi(35));
            for (ClassDef cd : dex.getClasses()) {
                for (Method m : cd.getMethods()) {
                    MethodImplementation impl = m.getImplementation();
                    if (impl == null) continue;
                    List<Instruction> list = new ArrayList<>();
                    for (Instruction i : impl.getInstructions()) list.add(i);
                    for (int idx = 0; idx < list.size(); idx++) {
                        Instruction insn = list.get(idx);
                        if (!(insn instanceof ReferenceInstruction)) continue;
                        Object ref = ((ReferenceInstruction) insn).getReference();
                        if (ref instanceof MethodReference) {
                            MethodReference mr = (MethodReference) ref;
                            if (mr.getDefiningClass().equals(cls) && (mname == null || mr.getName().equals(mname)))
                                System.out.println(f.getName() + " " + cd.getType() + "->" + m.getName()
                                    + " insn " + idx + " calls " + mr.getName() + "(" + String.join(",", mr.getParameterTypes()) + ")" + mr.getReturnType());
                        }
                    }
                }
            }
        }
    }
}
