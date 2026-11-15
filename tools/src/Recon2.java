import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/** List every used method ref in the dex whose return type is SharedPreferences,
 *  and every used ref defined on SharedPreferences / PreferenceManager classes. */
public class Recon2 {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        Set<String> retPrefs = new TreeSet<>();
        Set<String> onPrefs = new TreeSet<>();
        Set<String> onPrefMgr = new TreeSet<>();
        for (ClassDef c : dex.getClasses()) {
            for (Method m : c.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    Reference r = null;
                    if (ins instanceof Instruction35c) r = ((Instruction35c) ins).getReference();
                    else if (ins instanceof Instruction3rc) r = ((Instruction3rc) ins).getReference();
                    else if (ins instanceof Instruction22c) r = ((Instruction22c) ins).getReference();
                    else if (ins instanceof Instruction21c) r = ((Instruction21c) ins).getReference();
                    if (!(r instanceof MethodReference)) continue;
                    MethodReference mr = (MethodReference) r;
                    StringBuilder sb = new StringBuilder();
                    for (CharSequence p : mr.getParameterTypes()) { if (sb.length() > 0) sb.append(","); sb.append(p); }
                    String s = mr.getDefiningClass() + "->" + mr.getName() + "(" + sb + ")" + mr.getReturnType();
                    if (mr.getReturnType().equals("Landroid/content/SharedPreferences;")) retPrefs.add(s);
                    if (mr.getDefiningClass().equals("Landroid/content/SharedPreferences;")) onPrefs.add(s);
                    if (mr.getDefiningClass().contains("PreferenceManager")) onPrefMgr.add(s);
                }
            }
        }
        System.out.println("=== methods RETURNING SharedPreferences (" + retPrefs.size() + ") ===");
        for (String s : retPrefs) System.out.println("  " + s);
        System.out.println("=== methods ON SharedPreferences (" + onPrefs.size() + ") ===");
        for (String s : onPrefs) System.out.println("  " + s);
        System.out.println("=== methods ON *PreferenceManager (" + onPrefMgr.size() + ") ===");
        for (String s : onPrefMgr) System.out.println("  " + s);
    }
}
