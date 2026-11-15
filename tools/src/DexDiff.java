import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import java.io.File;
import java.util.*;

/** Diff two dex files method-by-method; print methods whose instruction list differs. */
public class DexDiff {
    static Map<String, List<String>> methods(DexFile dex) {
        Map<String, List<String>> m = new TreeMap<>();
        for (ClassDef c : dex.getClasses()) {
            for (Method meth : c.getMethods()) {
                String key = c.getType() + "->" + meth.getName() + meth.getParameters() + meth.getReturnType();
                MethodImplementation impl = meth.getImplementation();
                List<String> ins = new ArrayList<>();
                if (impl != null) for (Instruction i : impl.getInstructions()) ins.add(i.toString());
                m.put(key, ins);
            }
        }
        return m;
    }
    public static void main(String[] args) throws Exception {
        DexFile a = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        DexFile b = DexFileFactory.loadDexFile(new File(args[1]), Opcodes.getDefault());
        Map<String, List<String>> ma = methods(a), mb = methods(b);
        int diff = 0;
        for (String k : new TreeSet<>(ma.keySet())) {
            if (!mb.containsKey(k)) { System.out.println("REMOVED: " + k); diff++; continue; }
            if (!ma.get(k).equals(mb.get(k))) { System.out.println("CHANGED: " + k); diff++; }
        }
        for (String k : new TreeSet<>(mb.keySet())) {
            if (!ma.containsKey(k)) { System.out.println("ADDED: " + k); diff++; }
        }
        System.out.println("total methods: a=" + ma.size() + " b=" + mb.size() + " diff=" + diff);
    }
}
