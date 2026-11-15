import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction12x;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22x;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.jf.dexlib2.iface.instruction.formats.Instruction31i;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;
import org.jf.dexlib2.iface.instruction.formats.Instruction51l;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import java.io.File;
import java.util.*;

/**
 * FieldCheck: like VerifyPatch, but checks FIELD-access receiver types
 * (iget/iput), which VerifyPatch does not check at all.
 * For every instance field read/write, the object register's tracked
 * static type must be assignable to the field's declaring class.
 * Type tracking mirrors VerifyPatch (params, moves, field loads,
 * new-instance, check-cast, invoke results). INSTANCE_OF *does* refine
 * the tested register in the fall-through of an immediately-following
 * IF_EQZ on the instanceof result (the pattern the stock guard uses),
 * mirroring ART's instanceof refinement; CHECK_CAST refines outright.
 * A register whose type is null (unknown) is skipped, never guessed.
 * Usage: FieldCheck <dex> <class> <method>
 */
public class FieldCheckGen {
    static Map<String, String> superOf = new HashMap<>();
    static Map<String, Set<String>> ifacesOf = new HashMap<>();
    static Set<String> dexClasses = new HashSet<>();
    static int violations = 0;
    static int fieldAccesses = 0;

    static boolean isInterfaceAssignable(String iface, String target) {
        if (iface.equals(target)) return true;
        if (target.equals("Ljava/lang/Object;")) return true;
        Set<String> ifs = ifacesOf.get(iface);
        if (ifs != null) for (String i : ifs) if (isInterfaceAssignable(i, target)) return true;
        return false;
    }
    static boolean isAssignable(String obj, String target) {
        if (obj == null) return true;
        if (obj.equals(target)) return true;
        if (target.equals("Ljava/lang/Object;")) return true;
        if (obj.equals("Ljava/lang/Object;")) return false; // root: not a subclass of target
        if (obj.charAt(0) == '[') {
            return target.equals("Ljava/lang/Cloneable;") || target.equals("Ljava/io/Serializable;");
        }
        String cur = obj;
        Set<String> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            if (cur.equals(target)) return true;
            Set<String> ifs = ifacesOf.get(cur);
            if (ifs != null) for (String i : ifs) if (isInterfaceAssignable(i, target)) return true;
            cur = superOf.get(cur);
        }
        return false;
    }
    static int width(String t) { return (t.equals("J") || t.equals("D")) ? 2 : 1; }
    static List<Integer> invokeRegs(Instruction insn) {
        if (insn instanceof Instruction35c) {
            Instruction35c i = (Instruction35c) insn;
            List<Integer> r = new ArrayList<>();
            int n = i.getRegisterCount();
            if (n > 0) r.add(i.getRegisterC());
            if (n > 1) r.add(i.getRegisterD());
            if (n > 2) r.add(i.getRegisterE());
            if (n > 3) r.add(i.getRegisterF());
            if (n > 4) r.add(i.getRegisterG());
            return r;
        }
        if (insn instanceof Instruction3rc) {
            Instruction3rc i = (Instruction3rc) insn;
            List<Integer> r = new ArrayList<>();
            for (int k = 0; k < i.getRegisterCount(); k++) r.add(i.getStartRegister() + k);
            return r;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        // args: <dex-dir> <class> <method> — load ALL dexes in the dir for the
        // class hierarchy (a class referenced by the method may live in another dex)
        File dir = new File(args[0]);
        String cls = args[1], mname = args[2];
        File[] dexFiles = dir.listFiles((d, n) -> n.endsWith(".dex"));
        Arrays.sort(dexFiles);
        List<DexBackedDexFile> dexes = new ArrayList<>();
        for (File f : dexFiles) {
            DexBackedDexFile d = DexFileFactory.loadDexFile(f, Opcodes.forApi(35));
            dexes.add(d);
            for (ClassDef c : d.getClasses()) {
                dexClasses.add(c.getType());
                superOf.put(c.getType(), c.getSuperclass());
                Set<String> ifs = new HashSet<>();
                for (String it : c.getInterfaces()) ifs.add(it);
                ifacesOf.put(c.getType(), ifs);
            }
        }
        for (DexBackedDexFile dex : dexes) {
        for (ClassDef c : dex.getClasses()) {
            if (!c.getType().equals(cls)) continue;
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(mname)) continue;
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                int regs = impl.getRegisterCount();
                String[] types = new String[regs];
                List<? extends MethodParameter> params = m.getParameters();
                boolean isStatic = AccessFlags.STATIC.isSet(m.getAccessFlags());
                int paramWords = isStatic ? 0 : 1;
                for (MethodParameter p : params) paramWords += width(p.getType().toString());
                int firstParam = regs - paramWords;
                if (!isStatic && firstParam >= 0) types[firstParam] = m.getDefiningClass();
                int pr = firstParam + (isStatic ? 0 : 1);
                for (MethodParameter p : params) {
                    if (pr < regs) types[pr] = p.getType().toString();
                    pr += width(p.getType().toString());
                }
                String lastResult = null;
                // instanceof refinement pending: [resultReg, objReg, type]
                int pendRes = -1, pendObj = -1; String pendType = null;
                List<Instruction> insns = new ArrayList<>();
                for (Instruction i : impl.getInstructions()) insns.add(i);
                for (int idx = 0; idx < insns.size(); idx++) {
                    Instruction insn = insns.get(idx);
                    Opcode op = insn.getOpcode();
                    // instanceof refinement applies to an immediately-following
                    // IF_EQZ on the instanceof result (fall-through = is-instance)
                    if (op == Opcode.IF_EQZ && pendRes >= 0) {
                        org.jf.dexlib2.iface.instruction.formats.Instruction21t t =
                            (org.jf.dexlib2.iface.instruction.formats.Instruction21t) insn;
                        if (t.getRegisterA() == pendRes) {
                            types[pendObj] = pendType;
                        }
                    }
                    pendRes = -1; pendObj = -1; pendType = null;
                    switch (op) {
                        case MOVE_OBJECT: {
                            Instruction12x mm = (Instruction12x) insn;
                            types[mm.getRegisterA()] = types[mm.getRegisterB()];
                            break;
                        }
                        case MOVE: case MOVE_WIDE: {
                            Instruction12x mm = (Instruction12x) insn;
                            types[mm.getRegisterA()] = null;
                            break;
                        }
                        case MOVE_RESULT_OBJECT: {
                            Instruction11x mm = (Instruction11x) insn;
                            types[mm.getRegisterA()] = lastResult;
                            break;
                        }
                        case MOVE_RESULT: case MOVE_RESULT_WIDE: {
                            Instruction11x mm = (Instruction11x) insn;
                            types[mm.getRegisterA()] = null;
                            break;
                        }
                        case CONST_STRING: case CONST_STRING_JUMBO: {
                            Instruction21c mm = (Instruction21c) insn;
                            types[mm.getRegisterA()] = "Ljava/lang/String;";
                            break;
                        }
                        case CONST_CLASS: {
                            Instruction21c mm = (Instruction21c) insn;
                            types[mm.getRegisterA()] = "Ljava/lang/Class;";
                            break;
                        }
                        case NEW_INSTANCE: {
                            Instruction21c mm = (Instruction21c) insn;
                            types[mm.getRegisterA()] = mm.getReference().toString();
                            break;
                        }
                        case CHECK_CAST: {
                            Instruction21c mm = (Instruction21c) insn;
                            types[mm.getRegisterA()] = mm.getReference().toString();
                            break;
                        }
                        case NEW_ARRAY: {
                            Instruction22c mm = (Instruction22c) insn;
                            types[mm.getRegisterA()] = mm.getReference().toString();
                            break;
                        }
                        case CONST_4: case CONST_16: case CONST: case CONST_HIGH16:
                        case CONST_WIDE_16: case CONST_WIDE_32: case CONST_WIDE: {
                            int a = -1;
                            if (insn instanceof Instruction11n) a = ((Instruction11n) insn).getRegisterA();
                            else if (insn instanceof Instruction21s) a = ((Instruction21s) insn).getRegisterA();
                            else if (insn instanceof Instruction31i) a = ((Instruction31i) insn).getRegisterA();
                            else if (insn instanceof Instruction21ih) a = ((Instruction21ih) insn).getRegisterA();
                            else if (insn instanceof Instruction51l) a = ((Instruction51l) insn).getRegisterA();
                            if (a >= 0) types[a] = null;
                            break;
                        }
                        case AGET_OBJECT: {
                            Instruction23x mm = (Instruction23x) insn;
                            String arr = types[mm.getRegisterB()];
                            String comp = null;
                            if (arr != null && arr.startsWith("[") && arr.length() > 1) {
                                comp = arr.substring(1);
                                if (comp.length() == 1) comp = null;
                            }
                            types[mm.getRegisterA()] = comp;
                            lastResult = comp;
                            break;
                        }
                        case AGET: case AGET_BOOLEAN: case AGET_BYTE: case AGET_CHAR:
                        case AGET_SHORT: case AGET_WIDE: {
                            Instruction23x mm = (Instruction23x) insn;
                            types[mm.getRegisterA()] = null;
                            lastResult = null;
                            break;
                        }
                        case ARRAY_LENGTH: {
                            Instruction12x mm = (Instruction12x) insn;
                            types[mm.getRegisterA()] = null;
                            break;
                        }
                        case INSTANCE_OF: {
                            Instruction22c mm = (Instruction22c) insn;
                            types[mm.getRegisterA()] = null;
                            pendRes = mm.getRegisterA(); pendObj = mm.getRegisterB();
                            pendType = mm.getReference().toString();
                            break;
                        }
                        case IGET_OBJECT: case IGET: case IGET_WIDE: case IGET_BOOLEAN:
                        case IGET_BYTE: case IGET_CHAR: case IGET_SHORT: {
                            Instruction22c mm = (Instruction22c) insn;
                            FieldReference f = (FieldReference) mm.getReference();
                            int objReg = mm.getRegisterB();
                            String objType = types[objReg];
                            String decl = f.getDefiningClass();
                            fieldAccesses++;
                            if (System.getenv("FC_DEBUG") != null) System.out.println("DBG idx " + idx + " " + op + " objReg v" + objReg + " type=" + objType + " field=" + decl + "->" + f.getName());
                            if (objType != null && dexClasses.contains(decl)
                                && (dexClasses.contains(objType) || objType.equals("Ljava/lang/Object;"))
                                && !isAssignable(objType, decl)) {
                                violations++;
                                System.out.println("FIELD VIOLATION idx " + idx + " " + op
                                    + " v" + objReg + " statically typed " + objType
                                    + " not assignable to field " + decl + "->" + f.getName() + ":" + f.getType());
                            }
                            types[mm.getRegisterA()] = f.getType();
                            lastResult = f.getType();
                            break;
                        }
                        case IPUT_OBJECT: case IPUT: case IPUT_WIDE: case IPUT_BOOLEAN:
                        case IPUT_BYTE: case IPUT_CHAR: case IPUT_SHORT: {
                            Instruction22c mm = (Instruction22c) insn;
                            FieldReference f = (FieldReference) mm.getReference();
                            int objReg = mm.getRegisterB();
                            String objType = types[objReg];
                            String decl = f.getDefiningClass();
                            fieldAccesses++;
                            if (System.getenv("FC_DEBUG") != null) System.out.println("DBG idx " + idx + " " + op + " objReg v" + objReg + " type=" + objType + " field=" + decl + "->" + f.getName());
                            if (objType != null && dexClasses.contains(decl)
                                && (dexClasses.contains(objType) || objType.equals("Ljava/lang/Object;"))
                                && !isAssignable(objType, decl)) {
                                violations++;
                                System.out.println("FIELD VIOLATION idx " + idx + " " + op
                                    + " v" + objReg + " statically typed " + objType
                                    + " not assignable to field " + decl + "->" + f.getName() + ":" + f.getType());
                            }
                            break;
                        }
                        case SGET_OBJECT: {
                            Instruction21c mm = (Instruction21c) insn;
                            FieldReference f = (FieldReference) mm.getReference();
                            types[mm.getRegisterA()] = f.getType();
                            lastResult = f.getType();
                            break;
                        }
                        case INVOKE_VIRTUAL: case INVOKE_SUPER: case INVOKE_DIRECT:
                        case INVOKE_STATIC: case INVOKE_INTERFACE:
                        case INVOKE_VIRTUAL_RANGE: case INVOKE_SUPER_RANGE:
                        case INVOKE_DIRECT_RANGE: case INVOKE_STATIC_RANGE:
                        case INVOKE_INTERFACE_RANGE: {
                            MethodReference mr;
                            try { mr = (MethodReference) ((org.jf.dexlib2.iface.instruction.ReferenceInstruction) insn).getReference(); }
                            catch (Exception e) { break; }
                            String rt = mr.getReturnType();
                            lastResult = (rt.length() > 0 && (rt.charAt(0) == 'L' || rt.charAt(0) == '[')) ? rt : null;
                            break;
                        }
                        default:
                            break;
                    }
                }
                System.out.println("FieldCheck " + cls + "->" + mname + " (" + insns.size()
                    + " insns): fieldAccesses=" + fieldAccesses + " violations=" + violations);
            }
        }
        } // end dex loop
        if (violations > 0) System.exit(1);
    }
}
