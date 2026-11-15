// RefExists: check whether given method/field refs already exist in a dex.
// Usage: RefExists <dex-file>  (reads refs from stdin, one per line: M <sig> or F <sig>)
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.*;
import java.util.*;

public class RefExists {
    static String mkey(MethodReference r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getDefiningClass()).append("->").append(r.getName()).append("(");
        for (CharSequence p : r.getParameterTypes()) sb.append(p);
        sb.append(")").append(r.getReturnType());
        return sb.toString();
    }
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        Set<String> mrefs = new HashSet<>(), frefs = new HashSet<>();
        for (ClassDef cd : dex.getClasses()) {
            for (Method m : cd.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    if (ins instanceof ReferenceInstruction) {
                        Reference r = ((ReferenceInstruction) ins).getReference();
                        if (r instanceof MethodReference) mrefs.add(mkey((MethodReference) r));
                        if (r instanceof FieldReference) frefs.add(((FieldReference) r).getDefiningClass() + "->" + ((FieldReference) r).getName() + ":" + ((FieldReference) r).getType());
                    }
                }
            }
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            char kind = line.charAt(0);
            String sig = line.substring(2);
            boolean found = kind == 'M' ? mrefs.contains(sig) : frefs.contains(sig);
            System.out.println((found ? "PRESENT " : "MISSING  ") + line);
        }
    }
}
