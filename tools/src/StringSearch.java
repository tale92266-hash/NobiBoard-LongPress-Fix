// StringSearch: find methods referencing a string constant, method, or field.
// Usage: StringSearch <dex-dir> <kind> <value>
//   kind=string  value="the string"
//   kind=method  value="Lpkg/Class;->name(Proto)Ret"  (exact match on reference)
//   kind=type    value="Lpkg/Class;"  (any reference to the type)
// Prints: dex file, class, method, and the insn index of each hit.
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import java.io.File;
import java.util.*;

public class StringSearch {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        String kind = args[1], value = args[2];
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
                        boolean hit = false;
                        if (kind.equals("string") && ref instanceof StringReference)
                            hit = ((StringReference) ref).getString().equals(value);
                        else if (kind.equals("method") && ref instanceof MethodReference) {
                            MethodReference mr = (MethodReference) ref;
                            String sig = mr.getDefiningClass() + "->" + mr.getName()
                                + "(" + String.join("", mr.getParameterTypes()) + ")"
                                + mr.getReturnType();
                            hit = sig.equals(value);
                        } else if (kind.equals("type") && ref instanceof TypeReference)
                            hit = ((TypeReference) ref).getType().equals(value);
                        else if (kind.equals("field") && ref instanceof FieldReference) {
                            FieldReference fr = (FieldReference) ref;
                            String sig = fr.getDefiningClass() + "->" + fr.getName() + ":" + fr.getType();
                            hit = sig.equals(value);
                        }
                        if (hit)
                            System.out.println(f.getName() + " " + cd.getType() + "->" + m.getName()
                                + "(" + String.join(",", m.getParameters().stream().map(p -> p.getType()).toArray(String[]::new)) + ")" + m.getReturnType()
                                + " insn " + idx);
                    }
                }
            }
        }
    }
}
