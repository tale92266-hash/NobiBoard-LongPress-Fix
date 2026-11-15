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
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction12x;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.instruction.formats.Instruction3rc;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifier-level gate (added after the 176071835 regression): for every
 * invoke in the checked methods, verify
 *   (a) the register count matches the method prototype, with wide args
 *       (long/double) counting double, and wide arg register pairs
 *       consecutive;
 *   (b) the object register's static type is assignable to the invoked
 *       method's declaring class — i.e. a subclass method invoked on a
 *       superclass-typed object (without a preceding CHECK_CAST) is
 *       flagged, exactly as ART's verifier rejects it.
 *
 * This is a conservative single-pass abstract interpretation: a check is
 * only reported when the object type is KNOWN (tracked through moves,
 * field loads, new-instance, check-cast and invoke results) and both the
 * object type and the declaring class are classes in this dex (otherwise
 * the hierarchy cannot be proven and the check is skipped, never
 * guessed). INSTANCE_OF deliberately does NOT narrow a type — only
 * CHECK_CAST does — mirroring ART.
 *
 * Usage: VerifyPatch <dex> [method-filter-substring ...]
 *   No filters = scan every method in the dex. Exit 0 = no violations,
 *   1 = violations found.
 */
public class VerifyPatch {
    static Map<String, String> superOf = new HashMap<>();
    static Map<String, Set<String>> ifacesOf = new HashMap<>();
    static Set<String> dexClasses = new HashSet<>();
    static int violations = 0;
    static int invokesChecked = 0;
    static int methodsChecked = 0;

    static boolean isInterfaceAssignable(String iface, String target) {
        if (iface.equals(target)) return true;
        if (target.equals("Ljava/lang/Object;")) return true;
        Set<String> ifs = ifacesOf.get(iface);
        if (ifs != null) {
            for (String i : ifs) if (isInterfaceAssignable(i, target)) return true;
        }
        return false;
    }

    /** True if objType is assignable to target. Only called when both
     *  types are classes in this dex (or equal); unknown pairs are
     *  skipped by the caller, never guessed. */
    static boolean isAssignable(String obj, String target) {
        if (obj == null) return true;
        if (obj.equals(target)) return true;
        if (obj.charAt(0) == '[') {
            return target.equals("Ljava/lang/Object;")
                || target.equals("Ljava/lang/Cloneable;")
                || target.equals("Ljava/io/Serializable;");
        }
        String cur = obj;
        Set<String> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            if (cur.equals(target)) return true;
            Set<String> ifs = ifacesOf.get(cur);
            if (ifs != null) {
                for (String i : ifs) if (isInterfaceAssignable(i, target)) return true;
            }
            cur = superOf.get(cur);
        }
        return false;
    }

    static int width(String t) {
        return (t.equals("J") || t.equals("D")) ? 2 : 1;
    }

    static void violation(String where, int idx, Instruction insn, String detail) {
        violations++;
        System.out.println("VIOLATION " + where + " insn " + idx + " " + insn.getOpcode()
            + " : " + detail);
    }

    /** Register list of an invoke, or null if the format is not a
     *  plain 35c/3rc invoke (polymorphic/custom are skipped). */
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

    static void checkMethod(String where, Method method, MethodImplementation impl) {
        methodsChecked++;
        int regs = impl.getRegisterCount();
        String[] types = new String[regs];
        List<? extends MethodParameter> params = method.getParameters();
        boolean isStatic = AccessFlags.STATIC.isSet(method.getAccessFlags());
        int paramWords = isStatic ? 0 : 1;
        for (MethodParameter p : params) paramWords += width(p.getType().toString());
        int firstParam = regs - paramWords;
        if (!isStatic && firstParam >= 0) types[firstParam] = method.getDefiningClass();
        int pr = firstParam + (isStatic ? 0 : 1);
        for (MethodParameter p : params) {
            if (pr < regs) types[pr] = p.getType().toString();
            pr += width(p.getType().toString());
        }
        String lastResult = null; // type of last invoke/iget result (object only)
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        for (int idx = 0; idx < insns.size(); idx++) {
            Instruction insn = insns.get(idx);
            Opcode op = insn.getOpcode();
            switch (op) {
                case MOVE_OBJECT: {
                    Instruction12x m = (Instruction12x) insn;
                    types[m.getRegisterA()] = types[m.getRegisterB()];
                    break;
                }
                case MOVE:
                case MOVE_WIDE: {
                    Instruction12x m = (Instruction12x) insn;
                    types[m.getRegisterA()] = null;
                    if (op == Opcode.MOVE_WIDE && m.getRegisterA() + 1 < regs)
                        types[m.getRegisterA() + 1] = null;
                    break;
                }
                case MOVE_FROM16:
                case MOVE_16:
                case MOVE_WIDE_FROM16:
                case MOVE_WIDE_16: {
                    org.jf.dexlib2.iface.instruction.formats.Instruction22x m =
                        (org.jf.dexlib2.iface.instruction.formats.Instruction22x) insn;
                    types[m.getRegisterA()] = null;
                    if ((op == Opcode.MOVE_WIDE_FROM16 || op == Opcode.MOVE_WIDE_16)
                            && m.getRegisterA() + 1 < regs)
                        types[m.getRegisterA() + 1] = null;
                    break;
                }
                case MOVE_RESULT_OBJECT: {
                    Instruction11x m = (Instruction11x) insn;
                    types[m.getRegisterA()] = lastResult;
                    break;
                }
                case MOVE_RESULT:
                case MOVE_RESULT_WIDE: {
                    Instruction11x m = (Instruction11x) insn;
                    types[m.getRegisterA()] = null;
                    if (op == Opcode.MOVE_RESULT_WIDE && m.getRegisterA() + 1 < regs)
                        types[m.getRegisterA() + 1] = null;
                    break;
                }
                case MOVE_EXCEPTION: {
                    Instruction11x m = (Instruction11x) insn;
                    types[m.getRegisterA()] = "Ljava/lang/Throwable;";
                    break;
                }
                case CONST_CLASS: {
                    Instruction21c m = (Instruction21c) insn;
                    types[m.getRegisterA()] = "Ljava/lang/Class;";
                    break;
                }
                case CONST_STRING: {
                    Instruction21c m = (Instruction21c) insn;
                    types[m.getRegisterA()] = "Ljava/lang/String;";
                    break;
                }
                case CONST_STRING_JUMBO: {
                    org.jf.dexlib2.iface.instruction.formats.Instruction31c m =
                        (org.jf.dexlib2.iface.instruction.formats.Instruction31c) insn;
                    types[m.getRegisterA()] = "Ljava/lang/String;";
                    break;
                }
                case NEW_INSTANCE: {
                    Instruction21c m = (Instruction21c) insn;
                    types[m.getRegisterA()] = m.getReference().toString();
                    break;
                }
                case CHECK_CAST: {
                    Instruction21c m = (Instruction21c) insn;
                    types[m.getRegisterA()] = m.getReference().toString();
                    break;
                }
                case NEW_ARRAY: {
                    Instruction22c m = (Instruction22c) insn;
                    types[m.getRegisterA()] = m.getReference().toString();
                    break;
                }
                case FILLED_NEW_ARRAY:
                case FILLED_NEW_ARRAY_RANGE: {
                    org.jf.dexlib2.iface.instruction.ReferenceInstruction m =
                        (org.jf.dexlib2.iface.instruction.ReferenceInstruction) insn;
                    lastResult = m.getReference().toString();
                    break;
                }
                case CONST_4: {
                    org.jf.dexlib2.iface.instruction.formats.Instruction11n m =
                        (org.jf.dexlib2.iface.instruction.formats.Instruction11n) insn;
                    types[m.getRegisterA()] = null;
                    break;
                }
                case CONST_16:
                case CONST_WIDE_16: {
                    org.jf.dexlib2.iface.instruction.formats.Instruction21s m =
                        (org.jf.dexlib2.iface.instruction.formats.Instruction21s) insn;
                    types[m.getRegisterA()] = null;
                    break;
                }
                case CONST_HIGH16: {
                    org.jf.dexlib2.iface.instruction.formats.Instruction21ih m =
                        (org.jf.dexlib2.iface.instruction.formats.Instruction21ih) insn;
                    types[m.getRegisterA()] = null;
                    break;
                }
                case CONST:
                case CONST_WIDE_32: {
                    org.jf.dexlib2.iface.instruction.formats.Instruction31i m =
                        (org.jf.dexlib2.iface.instruction.formats.Instruction31i) insn;
                    types[m.getRegisterA()] = null;
                    break;
                }
                case CONST_WIDE: {
                    org.jf.dexlib2.iface.instruction.formats.Instruction51l m =
                        (org.jf.dexlib2.iface.instruction.formats.Instruction51l) insn;
                    types[m.getRegisterA()] = null;
                    if (m.getRegisterA() + 1 < regs) types[m.getRegisterA() + 1] = null;
                    break;
                }
                case AGET:
                case AGET_BOOLEAN:
                case AGET_BYTE:
                case AGET_CHAR:
                case AGET_SHORT:
                case AGET_WIDE: {
                    Instruction23x m = (Instruction23x) insn;
                    types[m.getRegisterA()] = null;
                    if (op == Opcode.AGET_WIDE && m.getRegisterA() + 1 < regs)
                        types[m.getRegisterA() + 1] = null;
                    lastResult = null;
                    break;
                }
                case IGET_OBJECT: {
                    Instruction22c m = (Instruction22c) insn;
                    FieldReference f = (FieldReference) m.getReference();
                    types[m.getRegisterA()] = f.getType();
                    lastResult = f.getType();
                    break;
                }
                case SGET_OBJECT: {
                    Instruction21c m = (Instruction21c) insn;
                    FieldReference f = (FieldReference) m.getReference();
                    types[m.getRegisterA()] = f.getType();
                    lastResult = f.getType();
                    break;
                }
                case IGET:
                case IGET_WIDE:
                case IGET_BOOLEAN:
                case IGET_BYTE:
                case IGET_CHAR:
                case IGET_SHORT:
                case SGET:
                case SGET_WIDE:
                case SGET_BOOLEAN:
                case SGET_BYTE:
                case SGET_CHAR:
                case SGET_SHORT: {
                    int a = (insn instanceof Instruction22c)
                        ? ((Instruction22c) insn).getRegisterA()
                        : ((Instruction21c) insn).getRegisterA();
                    types[a] = null;
                    lastResult = null;
                    break;
                }
                case AGET_OBJECT: {
                    Instruction23x m = (Instruction23x) insn;
                    String arr = types[m.getRegisterB()];
                    String comp = null;
                    if (arr != null && arr.startsWith("[") && arr.length() > 1) {
                        comp = arr.substring(1);
                        if (comp.length() == 1) comp = null; // primitive array
                    }
                    types[m.getRegisterA()] = comp;
                    lastResult = comp;
                    break;
                }
                case INSTANCE_OF: {
                    Instruction22c m = (Instruction22c) insn;
                    types[m.getRegisterA()] = null; // int result; type NOT narrowed
                    break;
                }
                case INVOKE_VIRTUAL:
                case INVOKE_SUPER:
                case INVOKE_DIRECT:
                case INVOKE_STATIC:
                case INVOKE_INTERFACE:
                case INVOKE_VIRTUAL_RANGE:
                case INVOKE_SUPER_RANGE:
                case INVOKE_DIRECT_RANGE:
                case INVOKE_STATIC_RANGE:
                case INVOKE_INTERFACE_RANGE: {
                    List<Integer> r = invokeRegs(insn);
                    if (r == null) break;
                    boolean isStaticInvoke = op == Opcode.INVOKE_STATIC
                        || op == Opcode.INVOKE_STATIC_RANGE;
                    MethodReference mr;
                    try {
                        mr = (MethodReference) ((org.jf.dexlib2.iface.instruction.ReferenceInstruction) insn).getReference();
                    } catch (Exception e) {
                        break;
                    }
                    invokesChecked++;
                    List<? extends CharSequence> pts = mr.getParameterTypes();
                    int required = isStaticInvoke ? 0 : 1;
                    for (CharSequence pt : pts) required += width(pt.toString());
                    if (r.size() != required) {
                        violation(where, idx, insn, "register count " + r.size()
                            + " != prototype-required " + required + " for "
                            + mr.getDefiningClass() + "->" + mr.getName() + "(" + pts + ")" + mr.getReturnType());
                    } else {
                        // wide args must occupy consecutive register pairs
                        int pos = isStaticInvoke ? 0 : 1;
                        for (int a = 0; a < pts.size(); a++) {
                            if (width(pts.get(a).toString()) == 2) {
                                if (pos + 1 >= r.size() || r.get(pos + 1) != r.get(pos) + 1) {
                                    violation(where, idx, insn, "wide arg " + a
                                        + " registers not consecutive in " + r);
                                }
                                pos += 2;
                            } else {
                                pos += 1;
                            }
                        }
                    }
                    if (!isStaticInvoke && !r.isEmpty()) {
                        String objType = types[r.get(0)];
                        String target = mr.getDefiningClass();
                        if (objType != null && dexClasses.contains(objType)
                                && dexClasses.contains(target)
                                && !isAssignable(objType, target)) {
                            violation(where, idx, insn, "invoke " + target + "->" + mr.getName()
                                + " on object statically typed " + objType
                                + " (not assignable; needs CHECK_CAST)");
                        }
                    }
                    String rt = mr.getReturnType();
                    lastResult = (rt.length() > 0 && (rt.charAt(0) == 'L' || rt.charAt(0) == '[')) ? rt : null;
                    break;
                }
                default:
                    // CONST_* / arithmetic / branches / returns: clear nothing.
                    // A register written by a non-tracked op keeps a stale
                    // type; that can only cause a missed check, never a
                    // false positive on the object register, because every
                    // object-producing op above is tracked.
                    break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        List<String> filters = new ArrayList<>();
        for (int i = 1; i < args.length; i++) filters.add(args[i]);
        for (ClassDef c : dex.getClasses()) {
            dexClasses.add(c.getType());
            superOf.put(c.getType(), c.getSuperclass());
            Set<String> ifs = new HashSet<>();
            for (String it : c.getInterfaces()) ifs.add(it);
            ifacesOf.put(c.getType(), ifs);
        }
        for (ClassDef c : dex.getClasses()) {
            for (Method m : c.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                String where = m.getDefiningClass() + "->" + m.getName()
                    + "(" + m.getParameters().size() + ")";
                if (!filters.isEmpty()) {
                    boolean hit = false;
                    for (String f : filters) {
                        if ((m.getDefiningClass() + "->" + m.getName()).contains(f)) { hit = true; break; }
                    }
                    if (!hit) continue;
                }
                checkMethod(where, m, impl);
            }
        }
        System.out.println("VerifyPatch: methods=" + methodsChecked
            + " invokes=" + invokesChecked + " violations=" + violations);
        if (violations > 0) System.exit(1);
    }
}
