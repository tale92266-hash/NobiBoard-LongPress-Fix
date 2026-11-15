import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

public class RefBatch {
    static int countIds(File f, int off) throws Exception {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
        try {
            raf.seek(off);
            int b0 = raf.read(); int b1 = raf.read(); int b2 = raf.read(); int b3 = raf.read();
            return (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        } finally { raf.close(); }
    }
    static String keyM(MethodReference r) {
        StringBuilder sb = new StringBuilder(r.getDefiningClass()).append("->").append(r.getName()).append("(");
        for (CharSequence p : r.getParameterTypes()) sb.append(p);
        return sb.append(")").append(r.getReturnType()).toString();
    }
    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(f, Opcodes.forApi(35));
        Set<String> methods = new HashSet<>();
        Set<String> fields = new HashSet<>();
        Set<String> types = new HashSet<>();
        Set<String> strings = new HashSet<>();
        for (ClassDef cd : dex.getClasses()) {
            types.add(cd.getType());
            for (Field fld : cd.getFields()) fields.add(fld.getDefiningClass() + "->" + fld.getName() + ":" + fld.getType());
            for (Method m : cd.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    if (ins instanceof ReferenceInstruction) {
                        Reference ref = ((ReferenceInstruction) ins).getReference();
                        if (ref instanceof MethodReference) methods.add(keyM((MethodReference) ref));
                        else if (ref instanceof FieldReference) { FieldReference fr = (FieldReference) ref; fields.add(fr.getDefiningClass() + "->" + fr.getName() + ":" + fr.getType()); }
                        else if (ref instanceof TypeReference) types.add(((TypeReference) ref).getType());
                        else if (ref instanceof StringReference) strings.add(((StringReference) ref).getString());
                    }
                }
            }
        }
        System.out.println("method_ids_size=" + countIds(f, 88) + " field_ids_size=" + countIds(f, 80)
            + " usedMethodRefs=" + methods.size() + " usedFieldRefs=" + fields.size());
        for (int i = 1; i < args.length; i++) {
            String q = args[i];
            boolean hit = q.contains(":") ? fields.contains(q) : methods.contains(q);
            System.out.println((hit ? "USED     " : "NOT-USED ") + q);
        }
        for (String s : new String[]{"nobi_longpress_emoji", "nobi_diag", "last_run", "last_a", "NobiDiag", "static", "fallback"}) {
            System.out.println((strings.contains(s) ? "STR-YES  " : "STR-NO   ") + s);
        }
    }
}
