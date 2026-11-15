import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/**
 * Recon for Phase 3: (a) list all methods/fields of Lpva and Lwhe in classes2.dex;
 * (b) check whether given method refs already exist in classes2.dex (used anywhere);
 * (c) list which methods reference Lpva.<init> / Lwhe.<init>.
 * Usage: Recon <dex> <Lpva|...> ...
 */
public class Recon {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        Set<String> usedMethods = new HashSet<>();
        Map<String, List<String>> ctorCallers = new HashMap<>();
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
                    if (r instanceof MethodReference) {
                        MethodReference mr = (MethodReference) r;
                        String s = mr.getDefiningClass() + "->" + mr.getName() + "(" + params(mr) + ")" + mr.getReturnType();
                        usedMethods.add(s);
                        if (mr.getName().equals("<init>") && (mr.getDefiningClass().equals("Lpva;") || mr.getDefiningClass().equals("Lwhe;"))) {
                            ctorCallers.computeIfAbsent(s, k -> new ArrayList<>()).add(c.getType() + "->" + m.getName() + m.getParameters() + m.getReturnType());
                        }
                    }
                }
            }
        }
        // (a) dump Lpva / Lwhe class defs
        for (String t : new String[]{"Lpva;", "Lwhe;"}) {
            System.out.println("=== class " + t + " ===");
            boolean found = false;
            for (ClassDef c : dex.getClasses()) {
                if (!c.getType().equals(t)) continue;
                found = true;
                System.out.println("super=" + c.getSuperclass() + " ifaces=" + c.getInterfaces() + " flags=" + c.getAccessFlags());
                for (Field f : c.getFields()) System.out.println("  field " + f.getName() + ":" + f.getType() + " flags=" + f.getAccessFlags());
                for (Method m : c.getMethods()) System.out.println("  method " + m.getName() + "(" + params(m) + ")" + m.getReturnType() + " flags=" + m.getAccessFlags());
            }
            if (!found) System.out.println("  NOT FOUND in this dex");
        }
        // (c) ctor callers
        System.out.println("=== <init> callers ===");
        for (Map.Entry<String, List<String>> e : ctorCallers.entrySet()) {
            System.out.println(e.getKey() + " called by " + e.getValue().size() + " methods:");
            for (String s : e.getValue()) System.out.println("    " + s);
        }
        // (b) check requested refs
        System.out.println("=== ref presence ===");
        for (int i = 1; i < args.length; i++) {
            System.out.println((usedMethods.contains(args[i]) ? "PRESENT  " : "ABSENT   ") + args[i]);
        }
    }
    static String params(MethodReference m) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence p : m.getParameterTypes()) { if (sb.length() > 0) sb.append(","); sb.append(p); }
        return sb.toString();
    }
    static String params(Method m) {
        StringBuilder sb = new StringBuilder();
        for (MethodParameter p : m.getParameters()) { if (sb.length() > 0) sb.append(","); sb.append(p.getType()); }
        return sb.toString();
    }
}
