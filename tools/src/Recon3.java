import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/** Dump ActionDef + Lqml fields; find iputs to ActionDef.f, new-instance ActionDef,
 *  used refs on Lpva/Lwhe, and presence of candidate refs (comma-joined format). */
public class Recon3 {
    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
        for (String t : new String[]{"Lcom/google/android/libraries/inputmethod/metadata/ActionDef;","Lqml;","Lqmu;"}) {
            System.out.println("=== class " + t + " ===");
            boolean found = false;
            for (ClassDef c : dex.getClasses()) {
                if (!c.getType().equals(t)) continue;
                found = true;
                System.out.println("super=" + c.getSuperclass());
                for (Field f : c.getFields()) System.out.println("  field " + f.getName() + ":" + f.getType() + " flags=" + f.getAccessFlags());
                for (Method m : c.getMethods()) {
                    StringBuilder sb = new StringBuilder();
                    for (MethodParameter p : m.getParameters()) { if (sb.length() > 0) sb.append(","); sb.append(p.getType()); }
                    System.out.println("  method " + m.getName() + "(" + sb + ")" + m.getReturnType() + " flags=" + m.getAccessFlags());
                }
            }
            if (!found) System.out.println("  NOT FOUND in this dex");
        }
        Set<String> usedMethods = new HashSet<>();
        Set<String> iputActionDefF = new TreeSet<>();
        Set<String> newActionDef = new TreeSet<>();
        Set<String> onLpva = new TreeSet<>();
        Set<String> onLwhe = new TreeSet<>();
        Set<String> onList = new TreeSet<>();
        for (ClassDef c : dex.getClasses()) {
            for (Method m : c.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                String where = c.getType() + "->" + m.getName();
                for (Instruction ins : impl.getInstructions()) {
                    Reference r = null;
                    if (ins instanceof Instruction35c) r = ((Instruction35c) ins).getReference();
                    else if (ins instanceof Instruction3rc) r = ((Instruction3rc) ins).getReference();
                    else if (ins instanceof Instruction22c) r = ((Instruction22c) ins).getReference();
                    else if (ins instanceof Instruction21c) r = ((Instruction21c) ins).getReference();
                    else if (ins instanceof Instruction21c) r = ((Instruction21c) ins).getReference();
                    if (r instanceof MethodReference) {
                        MethodReference mr = (MethodReference) r;
                        StringBuilder sb = new StringBuilder();
                        for (CharSequence p : mr.getParameterTypes()) { if (sb.length() > 0) sb.append(","); sb.append(p); }
                        String s = mr.getDefiningClass() + "->" + mr.getName() + "(" + sb + ")" + mr.getReturnType();
                        usedMethods.add(s);
                        if (mr.getDefiningClass().equals("Lpva;")) onLpva.add(s);
                        if (mr.getDefiningClass().equals("Lwhe;")) onLwhe.add(s);
                        if (mr.getDefiningClass().equals("Ljava/util/List;")) onList.add(s);
                    } else if (r instanceof FieldReference) {
                        FieldReference fr = (FieldReference) r;
                        if (fr.getDefiningClass().equals("Lcom/google/android/libraries/inputmethod/metadata/ActionDef;") && fr.getName().equals("f")) {
                            if (ins.getOpcode().name().startsWith("IPUT")) iputActionDefF.add(where);
                        }
                    }
                    if (ins instanceof Instruction21c) {
                        Object ref = ((Instruction21c) ins).getReference();
                        if (ref instanceof TypeReference && ((TypeReference) ref).getType().equals("Lcom/google/android/libraries/inputmethod/metadata/ActionDef;")
                            && ins.getOpcode().name().equals("NEW_INSTANCE")) newActionDef.add(where);
                    }
                }
            }
        }
        System.out.println("=== iput to ActionDef.f by ===");
        for (String s : iputActionDefF) System.out.println("  " + s);
        System.out.println("=== new-instance ActionDef by ===");
        for (String s : newActionDef) System.out.println("  " + s);
        System.out.println("=== used refs ON Lpva (" + onLpva.size() + ") ===");
        for (String s : onLpva) System.out.println("  " + s);
        System.out.println("=== used refs ON Lwhe (" + onLwhe.size() + ") ===");
        for (String s : onLwhe) System.out.println("  " + s);
        System.out.println("=== used refs ON List (" + onList.size() + ") ===");
        for (String s : onList) System.out.println("  " + s);
        System.out.println("=== ref presence ===");
        for (int i = 1; i < args.length; i++) System.out.println((usedMethods.contains(args[i]) ? "PRESENT  " : "ABSENT   ") + args[i]);
    }
}
