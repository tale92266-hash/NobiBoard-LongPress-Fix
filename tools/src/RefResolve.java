import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.TypeReference;

import java.io.File;
import java.util.*;

/**
 * RefResolve: check that every field/method/type reference used by the
 * given classes resolves to a real declaration (walking superclasses),
 * across ALL dex files of an APK. Usage:
 *   RefResolve <dex-dir> <class-filter-substring ...>
 * dex-dir contains classes*.dex files. Exit 0 = all resolvable, 1 = unresolved refs.
 * References whose declaring class is NOT in any dex (framework classes)
 * are reported as EXTERNAL and not counted as failures.
 */
public class RefResolve {
    static Map<String, ClassDef> classMap = new HashMap<>();
    static Set<String> allTypes = new HashSet<>();
    static int unresolved = 0;
    static int external = 0;
    static int checked = 0;

    static ClassDef findClass(String type) { return classMap.get(type); }

    /** Walk class + superclasses (dex classes only) for a field with exact name+type. */
    static boolean resolveField(String decl, String name, String type) {
        Set<String> seen = new HashSet<>();
        String cur = decl;
        while (cur != null && seen.add(cur)) {
            ClassDef cd = classMap.get(cur);
            if (cd == null) return false; // hit a framework class: cannot prove from dex
            for (Field f : cd.getFields()) {
                if (f.getName().equals(name) && f.getType().equals(type)) return true;
            }
            cur = cd.getSuperclass();
        }
        return false;
    }

    /** Walk class + superclasses + interfaces for a method with exact name+proto. */
    static boolean resolveMethod(String decl, String name, List<? extends CharSequence> params, String ret) {
        Deque<String> q = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        q.add(decl);
        while (!q.isEmpty()) {
            String cur = q.poll();
            if (cur == null || !seen.add(cur)) continue;
            ClassDef cd = classMap.get(cur);
            if (cd == null) continue; // framework class: skip (cannot prove)
            for (Method m : cd.getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (!m.getReturnType().equals(ret)) continue;
                List<? extends CharSequence> mp = m.getParameters();
                if (mp.size() != params.size()) continue;
                boolean ok = true;
                for (int i = 0; i < mp.size(); i++) {
                    if (!mp.get(i).toString().equals(params.get(i).toString())) { ok = false; break; }
                }
                if (ok) return true;
            }
            if (cd.getSuperclass() != null) q.add(cd.getSuperclass());
            for (String it : cd.getInterfaces()) q.add(it);
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        List<String> filters = new ArrayList<>();
        for (int i = 1; i < args.length; i++) filters.add(args[i]);
        File[] dexes = dir.listFiles((d, n) -> n.endsWith(".dex"));
        Arrays.sort(dexes);
        for (File f : dexes) {
            DexBackedDexFile dex = DexFileFactory.loadDexFile(f, Opcodes.forApi(35));
            for (String t : dex.getTypeSection()) allTypes.add(t);
            for (ClassDef c : dex.getClasses()) classMap.putIfAbsent(c.getType(), c);
        }
        System.out.println("dex files=" + dexes.length + " classes=" + classMap.size() + " types=" + allTypes.size());
        for (ClassDef c : new ArrayList<>(classMap.values())) {
            String ct = c.getType();
            boolean hit = false;
            for (String f : filters) if (ct.contains(f)) { hit = true; break; }
            if (!hit) continue;
            for (Method m : c.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                String where = ct + "->" + m.getName();
                int idx = 0;
                for (Instruction insn : impl.getInstructions()) {
                    if (insn instanceof ReferenceInstruction) {
                        Reference ref = ((ReferenceInstruction) insn).getReference();
                        checked++;
                        if (ref instanceof FieldReference) {
                            FieldReference fr = (FieldReference) ref;
                            if (classMap.get(fr.getDefiningClass()) == null) { external++; }
                            else if (!resolveField(fr.getDefiningClass(), fr.getName(), fr.getType())) {
                                System.out.println("UNRESOLVED FIELD " + where + " insn " + idx + ": " + fr.getDefiningClass() + "->" + fr.getName() + ":" + fr.getType());
                                unresolved++;
                            }
                        } else if (ref instanceof MethodReference) {
                            MethodReference mr = (MethodReference) ref;
                            if (classMap.get(mr.getDefiningClass()) == null) { external++; }
                            else if (!resolveMethod(mr.getDefiningClass(), mr.getName(), mr.getParameterTypes(), mr.getReturnType())) {
                                System.out.println("UNRESOLVED METHOD " + where + " insn " + idx + ": " + mr.getDefiningClass() + "->" + mr.getName() + "(" + mr.getParameterTypes() + ")" + mr.getReturnType());
                                unresolved++;
                            }
                        } else if (ref instanceof TypeReference) {
                            String t = ((TypeReference) ref).getType();
                            String base = t;
                            while (base.startsWith("[")) base = base.substring(1);
                            if (base.length() > 1 && !allTypes.contains(base) && !base.startsWith("L")) {
                                // primitive array base etc.
                            } else if (!allTypes.contains(base)) {
                                System.out.println("UNRESOLVED TYPE " + where + " insn " + idx + ": " + t);
                                unresolved++;
                            }
                        }
                    }
                    idx++;
                }
            }
        }
        System.out.println("RefResolve: checked=" + checked + " external(framework)=" + external + " unresolved=" + unresolved);
        if (unresolved > 0) System.exit(1);
    }
}
