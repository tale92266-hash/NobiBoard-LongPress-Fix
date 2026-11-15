import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import java.io.File;
import java.util.*;

public class CountRefs {
    public static void main(String[] args) throws Exception {
        for (String path : args) {
            DexFile dex = DexFileFactory.loadDexFile(new File(path), Opcodes.getDefault());
            Set<String> methods = new HashSet<>(), fields = new HashSet<>(), types = new HashSet<>(), strings = new HashSet<>();
            for (ClassDef c : dex.getClasses()) {
                types.add(c.getType());
                for (Field f : c.getFields()) { fields.add(f.getDefiningClass() + "->" + f.getName() + ":" + f.getType()); types.add(f.getType()); }
                for (Method m : c.getMethods()) {
                    methods.add(m.getDefiningClass() + "->" + m.getName() + m.getParameters() + m.getReturnType());
                    for (MethodParameter p : m.getParameters()) types.add(p.getType());
                    types.add(m.getReturnType());
                }
            }
            // also count refs used in code (method/field refs), not just defined
            Set<String> usedMethods = new HashSet<>(), usedFields = new HashSet<>();
            for (ClassDef c : dex.getClasses()) {
                for (Method m : c.getMethods()) {
                    MethodImplementation impl = m.getImplementation();
                    if (impl == null) continue;
                    for (org.jf.dexlib2.iface.instruction.Instruction ins : impl.getInstructions()) {
                        if (ins instanceof org.jf.dexlib2.iface.instruction.formats.Instruction35c) {
                            usedMethods.add(String.valueOf(((org.jf.dexlib2.iface.instruction.formats.Instruction35c) ins).getReference()));
                        } else if (ins instanceof org.jf.dexlib2.iface.instruction.formats.Instruction3rc) {
                            usedMethods.add(String.valueOf(((org.jf.dexlib2.iface.instruction.formats.Instruction3rc) ins).getReference()));
                        } else if (ins instanceof org.jf.dexlib2.iface.instruction.formats.Instruction22c) {
                            Object r = ((org.jf.dexlib2.iface.instruction.formats.Instruction22c) ins).getReference();
                            if (r instanceof org.jf.dexlib2.iface.reference.FieldReference) usedFields.add(String.valueOf(r));
                            else if (r instanceof org.jf.dexlib2.iface.reference.MethodReference) usedMethods.add(String.valueOf(r));
                        } else if (ins instanceof org.jf.dexlib2.iface.instruction.formats.Instruction21c) {
                            Object r = ((org.jf.dexlib2.iface.instruction.formats.Instruction21c) ins).getReference();
                            if (r instanceof org.jf.dexlib2.iface.reference.FieldReference) usedFields.add(String.valueOf(r));
                            else if (r instanceof org.jf.dexlib2.iface.reference.MethodReference) usedMethods.add(String.valueOf(r));
                        }
                    }
                }
            }
            System.out.println(path + " classes=" + dex.getClasses().size()
                + " definedMethods=" + methods.size() + " usedMethodRefs=" + usedMethods.size()
                + " definedFields=" + fields.size() + " usedFieldRefs=" + usedFields.size());
        }
    }
}
