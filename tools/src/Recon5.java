import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;
public class Recon5 {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        for (ClassDef c : dex.getClasses()) {
            for (Method m : c.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                int i = 0;
                for (Instruction ins : impl.getInstructions()) {
                    Reference r = null;
                    if (ins instanceof Instruction22c) r = ((Instruction22c) ins).getReference();
                    else if (ins instanceof Instruction21c) r = ((Instruction21c) ins).getReference();
                    if (r instanceof FieldReference) {
                        FieldReference fr = (FieldReference) r;
                        if (fr.getDefiningClass().equals("Lpva;") && fr.getName().equals("g")) {
                            System.out.println(c.getType() + "->" + m.getName() + " insn " + i + " " + ins.getOpcode().name());
                        }
                    }
                    i++;
                }
            }
        }
        // also dump Lpva methods that read g: equals/hashCode/toString/writeToParcel bodies are in classes.dex; do here for the given dex
    }
}
