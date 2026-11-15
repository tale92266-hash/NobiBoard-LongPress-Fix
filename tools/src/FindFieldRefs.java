import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

public class FindFieldRefs {
    public static void main(String[] args) throws Exception {
        // args: dexFile definingClass fieldName
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        String defCls = args[1], field = args[2];
        for (ClassDef cls : dex.getClasses()) {
            for (Method m : cls.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                List<Instruction> list = new ArrayList<>();
                for (Instruction ins : impl.getInstructions()) list.add(ins);
                for (int i = 0; i < list.size(); i++) {
                    Instruction ins = list.get(i);
                    Reference ref = null;
                    if (ins instanceof Instruction21c) {
                        ref = ((Instruction21c) ins).getReference();
                    } else if (ins instanceof Instruction22c) {
                        ref = ((Instruction22c) ins).getReference();
                    }
                    if (ref != null) {
                        if (ref instanceof FieldReference) {
                            FieldReference fr = (FieldReference) ref;
                            if (fr.getDefiningClass().equals(defCls) && fr.getName().equals(field)) {
                                System.out.println("METHOD: " + cls.getType() + "->" + m.getName() + "(" + m.getParameterTypes() + ")" + m.getReturnType());
                                for (int j = Math.max(0, i - 4); j <= Math.min(list.size() - 1, i + 4); j++)
                                    System.out.println("   " + (j == i ? ">>" : "  ") + j + ": " + FindResRefs.fmt(list.get(j)));
                                System.out.println();
                            }
                        }
                    }
                }
            }
        }
    }
}
