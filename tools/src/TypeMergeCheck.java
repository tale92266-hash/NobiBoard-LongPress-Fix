import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;

import java.util.*;

/**
 * TypeMergeCheck — the gate Fix4 was missing. A forward abstract-interpretation
 * type checker for ONE method: it tracks a coarse register type (UNINIT /
 * I32 / I64 / REF) per register, merges at every control-flow join, and
 * reports any register that merges two INCOMPATIBLE concrete kinds
 * (I32-vs-REF, I64-vs-REF, I32-vs-I64). That is exactly ART's VerifyError
 * for a merge-type conflict — the defect class that killed Fix4 (v11 =
 * INT on the 01f6 jump path, Lqmr on the fall-through path, merging at
 * 027e) and that RegInitCheck (init only), VerifyPatch (invokes only) and
 * VerifyBranches (targets only) cannot see.
 *
 * Merge lattice: UNINIT is bottom (merges to anything); NULL is REF;
 * INT and FLOAT are both I32 (stock code passes ints to float params and
 * verifies fine); LONG and DOUBLE are both I64. A conflict is a concrete
 * kind mismatch — it is reported with the join instruction and register.
 *
 * Usage: TypeMergeCheck <dex> <class> <method> [params...]
 *   e.g. TypeMergeCheck classes2.dex Lqmu; i I I
 * Exit 0 = no conflicts; exit 1 = conflicts found (listed).
 */
public class TypeMergeCheck {
    // kinds
    static final int UNINIT = 0, I32 = 1, I64 = 2, REF = 3, CONFLICT = 4, NULL = 5;

    static String kindName(int k) {
        switch (k) {
            case UNINIT: return "UNINIT";
            case I32: return "I32";
            case I64: return "I64";
            case REF: return "REF";
            case NULL: return "NULL";
            default: return "CONFLICT";
        }
    }

    static int mergeKind(int a, int b) {
        if (a == b) return a;
        // UNINIT and NULL are bottom: they merge to the other kind.
        // (NULL is what ART's verifier does: null is assignable to any
        // type, so a null-vs-float merge at a join is NOT a conflict —
        // this is exactly the stock Lqmu.o() return, where v0 is null on
        // the if-eqz path and 1.0f on the fall-through.)
        if (a == UNINIT || a == NULL) return b;
        if (b == UNINIT || b == NULL) return a;
        if (a == CONFLICT || b == CONFLICT) return CONFLICT;
        return CONFLICT; // I32/I64/REF pairwise incompatible
    }

    static int typeKind(String t) {
        if (t == null) return UNINIT;
        if (t.equals("I") || t.equals("Z") || t.equals("B") || t.equals("C")
                || t.equals("S") || t.equals("F")) return I32;
        if (t.equals("J") || t.equals("D")) return I64;
        return REF; // L...; [...
    }

    static int returnKind(MethodReference m) {
        return typeKind(m.getReturnType());
    }

    /** Successor states of instruction idx: each entry is
     *  (successor index, state to propagate there). The state is the
     *  transfer output, possibly refined for a branch: after IF_EQZ vA the
     *  branch target sees vA as NULL; after IF_NEZ vA the fall-through
     *  sees vA as NULL. (This is ART's path-sensitive null refinement —
     *  without it the stock o() return looks like a REF-vs-float
     *  conflict.) */
    static List<int[]> succStates(List<Instruction> insns, List<Integer> addr,
            Map<Integer, Integer> addrToIdx, int idx, int[] out) {
        List<int[]> res = new ArrayList<>();
        Instruction ins = insns.get(idx);
        Opcode op = ins.getOpcode();
        int myAddr = addr.get(idx);
        boolean fallsThrough = true;
        if (op == Opcode.RETURN || op == Opcode.RETURN_VOID
                || op == Opcode.RETURN_WIDE || op == Opcode.RETURN_OBJECT
                || op == Opcode.THROW) {
            fallsThrough = false;
        }
        if (op == Opcode.GOTO || op == Opcode.GOTO_16 || op == Opcode.GOTO_32) {
            fallsThrough = false;
        }
        Integer targetIdx = null;
        if (ins instanceof OffsetInstruction) {
            int tgt = myAddr + ((OffsetInstruction) ins).getCodeOffset();
            targetIdx = addrToIdx.get(tgt);
            if (targetIdx == null) {
                System.out.println("  !! branch target " + Integer.toHexString(tgt)
                    + " is not an instruction boundary (from idx " + idx + ")");
            }
        }
        if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH) {
            System.out.println("  !! WARNING: switch at idx " + idx
                + " — payload targets NOT followed (checker incomplete here)");
        }
        // branch refinement for IF_EQZ / IF_NEZ (21t)
        int refinedReg = -1;
        boolean refineTarget = false; // true: refine the TARGET; false: refine fall-through
        if (op == Opcode.IF_EQZ || op == Opcode.IF_NEZ) {
            refinedReg = ((Instruction21t) ins).getRegisterA();
            refineTarget = (op == Opcode.IF_EQZ);
        }
        if (targetIdx != null) {
            int[] st = out;
            if (refineTarget && refinedReg >= 0) {
                st = out.clone();
                st[refinedReg] = NULL;
            }
            res.add(new int[]{targetIdx, 0}); // placeholder, state attached below
            res.set(res.size() - 1, new int[]{targetIdx, -1});
            // store the state in a parallel list via a hack: encode in a map
            succStateMap.put(targetIdx + ":" + idx, st);
        }
        if (fallsThrough && idx + 1 < insns.size()) {
            int[] st = out;
            if (!refineTarget && refinedReg >= 0) {
                st = out.clone();
                st[refinedReg] = NULL;
            }
            succStateMap.put((idx + 1) + ":" + idx, st);
            res.add(new int[]{idx + 1, -1});
        }
        return res;
    }

    // (successor -> state) for the current instruction being processed;
    // keyed "succIdx:fromIdx". Cleared per instruction.
    static Map<String, int[]> succStateMap = new HashMap<>();

    /** Register A for the single-register-dest formats, or -1. */
    static int regA(Instruction ins) {
        if (ins instanceof Instruction11n) return ((Instruction11n) ins).getRegisterA();
        if (ins instanceof Instruction11x) return ((Instruction11x) ins).getRegisterA();
        if (ins instanceof Instruction21s) return ((Instruction21s) ins).getRegisterA();
        if (ins instanceof Instruction21c) return ((Instruction21c) ins).getRegisterA();
        if (ins instanceof Instruction21ih) return ((Instruction21ih) ins).getRegisterA();
        if (ins instanceof Instruction31i) return ((Instruction31i) ins).getRegisterA();
        if (ins instanceof Instruction51l) return ((Instruction51l) ins).getRegisterA();
        return -1;
    }

    /** Apply the instruction's transfer to the register state (in-place
     *  copy semantics: returns a NEW state array). For MOVE_RESULT the
     *  return kind comes from the immediately preceding invoke in the
     *  instruction list (dex guarantees move-result follows its invoke). */
    static int[] transfer(List<Instruction> insns, int idx, int[] in) {
        int[] out = in.clone();
        Instruction ins = insns.get(idx);
        Opcode op = ins.getOpcode();
        switch (op) {
            // ---- constants: dest = I32 (int/float) ----
            case CONST: case CONST_4: case CONST_16: case CONST_HIGH16: {
                int r = regA(ins);
                if (r >= 0) out[r] = I32;
                break;
            }
            case CONST_WIDE: case CONST_WIDE_16: case CONST_WIDE_32:
            case CONST_WIDE_HIGH16: {
                int r = regA(ins);
                if (r >= 0) { out[r] = I64; out[r + 1] = I64; }
                break;
            }
            case CONST_STRING: case CONST_STRING_JUMBO: case CONST_CLASS: {
                int r = regA(ins);
                if (r >= 0) out[r] = REF;
                break;
            }
            // ---- moves ----
            case MOVE: case MOVE_FROM16: case MOVE_16: {
                int a, b;
                if (ins instanceof Instruction12x) {
                    a = ((Instruction12x) ins).getRegisterA();
                    b = ((Instruction12x) ins).getRegisterB();
                } else if (ins instanceof Instruction22x) {
                    a = ((Instruction22x) ins).getRegisterA();
                    b = ((Instruction22x) ins).getRegisterB();
                } else {
                    a = ((Instruction32x) ins).getRegisterA();
                    b = ((Instruction32x) ins).getRegisterB();
                }
                out[a] = in[b]; break;
            }
            case MOVE_OBJECT: case MOVE_OBJECT_FROM16: case MOVE_OBJECT_16: {
                int a;
                if (ins instanceof Instruction12x) {
                    a = ((Instruction12x) ins).getRegisterA();
                } else if (ins instanceof Instruction22x) {
                    a = ((Instruction22x) ins).getRegisterA();
                } else {
                    a = ((Instruction32x) ins).getRegisterA();
                }
                out[a] = REF; break;
            }
            case MOVE_WIDE: case MOVE_WIDE_FROM16: case MOVE_WIDE_16: {
                int a;
                if (ins instanceof Instruction12x) {
                    a = ((Instruction12x) ins).getRegisterA();
                } else if (ins instanceof Instruction22x) {
                    a = ((Instruction22x) ins).getRegisterA();
                } else {
                    a = ((Instruction32x) ins).getRegisterA();
                }
                out[a] = I64; out[a + 1] = I64; break;
            }
            case MOVE_RESULT: {
                int r = ((Instruction11x) ins).getRegisterA();
                out[r] = I32;
                if (idx > 0 && insns.get(idx - 1) instanceof ReferenceInstruction) {
                    Reference ref = ((ReferenceInstruction) insns.get(idx - 1)).getReference();
                    if (ref instanceof MethodReference) {
                        out[r] = returnKind((MethodReference) ref);
                    }
                }
                break;
            }
            case MOVE_RESULT_WIDE: {
                int r = ((Instruction11x) ins).getRegisterA();
                out[r] = I64; out[r + 1] = I64; break;
            }
            case MOVE_RESULT_OBJECT: {
                int r = ((Instruction11x) ins).getRegisterA();
                out[r] = REF; break;
            }
            case MOVE_EXCEPTION: {
                int r = ((Instruction11x) ins).getRegisterA();
                out[r] = REF; break;
            }
            // ---- field access ----
            case IGET: case IGET_BOOLEAN: case IGET_BYTE: case IGET_CHAR:
            case IGET_SHORT: {
                Instruction22c f = (Instruction22c) ins;
                out[f.getRegisterA()] = I32; break;
            }
            case IGET_WIDE: {
                Instruction22c f = (Instruction22c) ins;
                out[f.getRegisterA()] = I64; out[f.getRegisterA() + 1] = I64; break;
            }
            case IGET_OBJECT: {
                Instruction22c f = (Instruction22c) ins;
                out[f.getRegisterA()] = REF; break;
            }
            case SGET: case SGET_BOOLEAN: case SGET_BYTE: case SGET_CHAR:
            case SGET_SHORT: {
                Instruction21c f = (Instruction21c) ins;
                out[f.getRegisterA()] = I32; break;
            }
            case SGET_WIDE: {
                Instruction21c f = (Instruction21c) ins;
                out[f.getRegisterA()] = I64; out[f.getRegisterA() + 1] = I64; break;
            }
            case SGET_OBJECT: {
                Instruction21c f = (Instruction21c) ins;
                out[f.getRegisterA()] = REF; break;
            }
            // ---- int/float arithmetic -> I32 ----
            case ADD_INT: case SUB_INT: case MUL_INT: case DIV_INT:
            case REM_INT: case AND_INT: case OR_INT: case XOR_INT:
            case SHL_INT: case SHR_INT: case USHR_INT:
            case ADD_INT_2ADDR: case SUB_INT_2ADDR: case MUL_INT_2ADDR:
            case DIV_INT_2ADDR: case REM_INT_2ADDR: case AND_INT_2ADDR:
            case OR_INT_2ADDR: case XOR_INT_2ADDR: case SHL_INT_2ADDR:
            case SHR_INT_2ADDR: case USHR_INT_2ADDR:
            case ADD_INT_LIT8: case RSUB_INT_LIT8: case MUL_INT_LIT8:
            case DIV_INT_LIT8: case REM_INT_LIT8: case AND_INT_LIT8:
            case OR_INT_LIT8: case XOR_INT_LIT8: case SHL_INT_LIT8:
            case SHR_INT_LIT8: case USHR_INT_LIT8:
            case ADD_FLOAT: case SUB_FLOAT: case MUL_FLOAT: case DIV_FLOAT:
            case REM_FLOAT: case ADD_FLOAT_2ADDR: case SUB_FLOAT_2ADDR:
            case MUL_FLOAT_2ADDR: case DIV_FLOAT_2ADDR: case REM_FLOAT_2ADDR:
            case NEG_INT: case NOT_INT: case NEG_FLOAT:
            case INT_TO_FLOAT: case INT_TO_BYTE: case INT_TO_CHAR:
            case INT_TO_SHORT:
            case ADD_INT_LIT16: case RSUB_INT: case MUL_INT_LIT16:
            case DIV_INT_LIT16: case REM_INT_LIT16: case AND_INT_LIT16:
            case OR_INT_LIT16: case XOR_INT_LIT16:
            case FLOAT_TO_INT: case LONG_TO_INT: case DOUBLE_TO_INT: {
                int r = destReg(ins);
                if (r >= 0) out[r] = I32;
                break;
            }
            // ---- long/double arithmetic -> I64 ----
            case ADD_LONG: case SUB_LONG: case MUL_LONG: case DIV_LONG:
            case REM_LONG: case AND_LONG: case OR_LONG: case XOR_LONG:
            case SHL_LONG: case SHR_LONG: case USHR_LONG:
            case ADD_LONG_2ADDR: case SUB_LONG_2ADDR: case MUL_LONG_2ADDR:
            case DIV_LONG_2ADDR: case REM_LONG_2ADDR: case AND_LONG_2ADDR:
            case OR_LONG_2ADDR: case XOR_LONG_2ADDR: case SHL_LONG_2ADDR:
            case SHR_LONG_2ADDR: case USHR_LONG_2ADDR:
            case ADD_DOUBLE: case SUB_DOUBLE: case MUL_DOUBLE: case DIV_DOUBLE:
            case REM_DOUBLE: case ADD_DOUBLE_2ADDR: case SUB_DOUBLE_2ADDR:
            case MUL_DOUBLE_2ADDR: case DIV_DOUBLE_2ADDR: case REM_DOUBLE_2ADDR:
            case NEG_LONG: case NEG_DOUBLE:
            case INT_TO_LONG: case INT_TO_DOUBLE: case LONG_TO_DOUBLE:
            case FLOAT_TO_LONG: case FLOAT_TO_DOUBLE: case DOUBLE_TO_LONG:
            case LONG_TO_FLOAT: case DOUBLE_TO_FLOAT: {
                int r = destReg(ins);
                if (r >= 0) { out[r] = I64; out[r + 1] = I64; }
                break;
            }
            // ---- object-creating ----
            case NEW_INSTANCE: {
                int r = ((Instruction21c) ins).getRegisterA();
                out[r] = REF; break;
            }
            case NEW_ARRAY: {
                int r = ((Instruction22c) ins).getRegisterA();
                out[r] = REF; break;
            }
            case CHECK_CAST: {
                int r = ((Instruction21c) ins).getRegisterA();
                out[r] = REF; break;
            }
            case INSTANCE_OF: {
                int r = ((Instruction22c) ins).getRegisterA();
                out[r] = I32; break;
            }
            case ARRAY_LENGTH: {
                int r = ((Instruction12x) ins).getRegisterA();
                out[r] = I32; break;
            }
            // ---- array get ----
            case AGET: case AGET_BOOLEAN: case AGET_BYTE: case AGET_CHAR:
            case AGET_SHORT: {
                int r = ((Instruction23x) ins).getRegisterA();
                out[r] = I32; break;
            }
            case AGET_WIDE: {
                int r = ((Instruction23x) ins).getRegisterA();
                out[r] = I64; out[r + 1] = I64; break;
            }
            case AGET_OBJECT: {
                int r = ((Instruction23x) ins).getRegisterA();
                out[r] = REF; break;
            }
            // ---- compares -> I32 ----
            case CMPL_FLOAT: case CMPG_FLOAT: case CMPL_DOUBLE:
            case CMPG_DOUBLE: case CMP_LONG: {
                int r = ((Instruction23x) ins).getRegisterA();
                out[r] = I32; break;
            }
            default:
                // invokes, iputs, branches, returns, etc.: no register
                // dest. Unknown opcodes are ignored (no dest assumed).
                break;
        }
        return out;
    }

    /** Destination register for 12x/23x/22b/22s/21t-style insns, or -1. */
    static int destReg(Instruction ins) {
        if (ins instanceof Instruction12x) return ((Instruction12x) ins).getRegisterA();
        if (ins instanceof Instruction23x) return ((Instruction23x) ins).getRegisterA();
        if (ins instanceof Instruction22b) return ((Instruction22b) ins).getRegisterA();
        if (ins instanceof Instruction22s) return ((Instruction22s) ins).getRegisterA();
        return -1;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("usage: TypeMergeCheck <dex> <class> <method> [paramTypes...]");
            System.exit(2);
        }
        String dexPath = args[0];
        String cls = args[1];
        String name = args[2];
        List<String> params = new ArrayList<>();
        int nArgs = args.length;
        // a trailing "state:<idx>" is a dump request, not a param type
        if (nArgs > 3 && args[nArgs - 1].startsWith("state:")) nArgs--;
        for (int i = 3; i < nArgs; i++) params.add(args[i]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(
            new java.io.File(dexPath), Opcodes.forApi(35));
        Method target = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals(cls)) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals(name)
                        && m.getParameters().size() == params.size()) {
                    boolean ok = true;
                    for (int i = 0; i < params.size(); i++) {
                        if (!m.getParameters().get(i).getType().equals(params.get(i))) ok = false;
                    }
                    if (ok) target = m;
                }
            }
        }
        if (target == null || target.getImplementation() == null) {
            System.out.println("method not found: " + cls + "->" + name);
            System.exit(2);
        }
        MethodImplementation impl = target.getImplementation();
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        int nRegs = impl.getRegisterCount();
        List<Integer> addr = new ArrayList<>();
        Map<Integer, Integer> addrToIdx = new HashMap<>();
        int a = 0;
        for (int i = 0; i < insns.size(); i++) {
            addr.add(a); addrToIdx.put(a, i); a += insns.get(i).getCodeUnits();
        }
        int n = insns.size();
        // entry state: params are typed, locals UNINIT
        int nParams = params.size() + 1; // +this
        int[] entry = new int[nRegs];
        Arrays.fill(entry, UNINIT);
        // dexlib2: params occupy the LAST registers
        int p = nRegs - nParams;
        entry[p] = REF; // this
        for (int i = 0; i < params.size(); i++) {
            int k = typeKind(params.get(i));
            entry[p + 1 + i] = k;
            if (k == I64) { entry[p + 2 + i] = I64; i++; }
        }
        // dataflow states (null = not yet reached)
        int[][] inState = new int[n][];
        inState[0] = entry;
        Deque<Integer> work = new ArrayDeque<>();
        work.add(0);
        List<String> conflicts = new ArrayList<>();
        Set<String> seenConflict = new HashSet<>();
        while (!work.isEmpty()) {
            int idx = work.poll();
            int[] in = inState[idx];
            if (in == null) continue;
            int[] out = transfer(insns, idx, in);
            succStateMap.clear();
            List<int[]> succs = succStates(insns, addr, addrToIdx, idx, out);
            for (int[] se : succs) {
                int sIdx = se[0];
                int[] st = succStateMap.get(sIdx + ":" + idx);
                if (st == null) st = out;
                if (inState[sIdx] == null) {
                    inState[sIdx] = st.clone();
                    work.add(sIdx);
                } else {
                    boolean changed = false;
                    int[] merged = inState[sIdx].clone();
                    for (int r = 0; r < nRegs; r++) {
                        int m = mergeKind(merged[r], st[r]);
                        if (m == CONFLICT && merged[r] != CONFLICT && st[r] != CONFLICT
                                && merged[r] != UNINIT && merged[r] != NULL
                                && st[r] != UNINIT && st[r] != NULL) {
                            String key = sIdx + ":" + r;
                            if (!seenConflict.contains(key)) {
                                seenConflict.add(key);
                                conflicts.add("CONFLICT at join insn idx " + sIdx
                                    + " (abs " + Integer.toHexString(addr.get(sIdx)) + ") reg v" + r
                                    + ": " + kindName(merged[r]) + " vs " + kindName(st[r])
                                    + " (from idx " + idx + " abs "
                                    + Integer.toHexString(addr.get(idx)) + ")");
                            }
                        }
                        if (m != merged[r]) { merged[r] = m; changed = true; }
                    }
                    if (changed) {
                        inState[sIdx] = merged;
                        work.add(sIdx);
                    }
                }
            }
        }
        // --state <idx> : dump the register types at the entry of insn idx
        // (after the fixpoint) and exit. Used to find a scratch register
        // that is UNINIT (or I32) at the insertion point.
        if (args.length > 0 && args[args.length - 1].startsWith("state:")) {
            int dumpIdx = Integer.parseInt(args[args.length - 1].substring(6));
            int[] st = inState[dumpIdx];
            System.out.println("in-state at idx " + dumpIdx + " (abs "
                + Integer.toHexString(addr.get(dumpIdx)) + "):");
            for (int r = 0; r < nRegs; r++) {
                System.out.println("  v" + r + " = " + kindName(st == null ? UNINIT : st[r]));
            }
            return;
        }
        System.out.println("TypeMergeCheck " + cls + "->" + name + " (" + n
            + " insns, " + nRegs + " regs): "
            + (conflicts.isEmpty() ? "0 conflicts" : conflicts.size() + " CONFLICT(S)"));
        for (String c : conflicts) System.out.println("  " + c);
        System.exit(conflicts.isEmpty() ? 0 : 1);
    }
}
