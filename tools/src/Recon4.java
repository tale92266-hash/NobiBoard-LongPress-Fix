import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/** List every used FIELD ref on Lpva / ActionDef / Lqml / TextView in the dex. */
public class Recon4 {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        Set<String> refs = new TreeSet<>();
        for (ClassDef c : dex.getClasses()) {
            for (Method m : c.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    Reference r = null;
                    if (ins instanceof Instruction22c) r = ((Instruction22c) ins).getReference();
                    else if (ins instanceof Instruction21c) r = ((Instruction21c) ins).getReference();
                    if (r instanceof FieldReference) {
                        FieldReference fr = (FieldReference) r;
                        String dc = fr.getDefiningClass();
                        if (dc.equals("Lpva;") || dc.equals("Lcom/google/android/libraries/inputmethod/metadata/ActionDef;")
                            || dc.equals("Lqml;") || dc.equals("Lqmu;") || dc.equals("Lqmo;") || dc.equals("Lqmn;")) {
                            refs.add(ins.getOpcode().name() + " " + dc + "->" + fr.getName() + ":" + fr.getType());
                        }
                    }
                }
            }
        }
        for (String s : refs) System.out.println(s);
    }
}
