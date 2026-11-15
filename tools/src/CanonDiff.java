import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/**
 * Canonical dex differ: renders each instruction as opcode + registers +
 * resolved reference + branch target as INSTRUCTION INDEX (stable across
 * dexlib2 re-serializations, unlike Instruction.toString() which proved
 * noisy: 42,071/46,053 false diffs on the agent's own 824 pair).
 */
public class CanonDiff {
    static String render(Instruction ins, Map<Integer,Integer> off2idx, List<Integer> absOff, int idx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ins.getOpcode().name());
        int units = ins.getCodeUnits();
        // registers / literals via toString parsing is fragile; use formats
        try {
            if (ins instanceof Instruction10x) { /* nop/return: nothing */ }
            else if (ins instanceof Instruction11n) { Instruction11n i = (Instruction11n) ins; sb.append(" v").append(i.getRegisterA()).append("=").append(i.getNarrowLiteral()); }
            else if (ins instanceof Instruction11x) { Instruction11x i = (Instruction11x) ins; sb.append(" v").append(i.getRegisterA()); }
            else if (ins instanceof Instruction12x) { Instruction12x i = (Instruction12x) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()); }
            else if (ins instanceof Instruction21c) { Instruction21c i = (Instruction21c) ins; sb.append(" v").append(i.getRegisterA()).append(",").append(refStr(i.getReference())); }
            else if (ins instanceof Instruction22c) { Instruction22c i = (Instruction22c) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()).append(",").append(refStr(i.getReference())); }
            else if (ins instanceof Instruction35c) { Instruction35c i = (Instruction35c) ins; sb.append(" {").append(i.getRegisterCount()).append("} ").append(refStr(i.getReference())); sb.append(" regs=").append(regList(i)); }
            else if (ins instanceof Instruction3rc) { Instruction3rc i = (Instruction3rc) ins; sb.append(" {v").append(i.getStartRegister()).append("..} ").append(refStr(i.getReference())); }
            else if (ins instanceof Instruction21t) { Instruction21t i = (Instruction21t) ins; int tgt = absOff.get(idx) + i.getCodeOffset(); sb.append(" v").append(i.getRegisterA()).append("->idx").append(off2idx.getOrDefault(tgt, -999)); }
            else if (ins instanceof Instruction22t) { Instruction22t i = (Instruction22t) ins; int tgt = absOff.get(idx) + i.getCodeOffset(); sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()).append("->idx").append(off2idx.getOrDefault(tgt, -999)); }
            else if (ins instanceof Instruction10t) { Instruction10t i = (Instruction10t) ins; int tgt = absOff.get(idx) + i.getCodeOffset(); sb.append(" ->idx").append(off2idx.getOrDefault(tgt, -999)); }
            else if (ins instanceof Instruction30t) { Instruction30t i = (Instruction30t) ins; int tgt = absOff.get(idx) + i.getCodeOffset(); sb.append(" ->idx").append(off2idx.getOrDefault(tgt, -999)); }
            else if (ins instanceof Instruction31t) { Instruction31t i = (Instruction31t) ins; int tgt = absOff.get(idx) + i.getCodeOffset(); sb.append(" v").append(i.getRegisterA()).append("->idx").append(off2idx.getOrDefault(tgt, -999)); }
            else if (ins instanceof Instruction21s) { Instruction21s i = (Instruction21s) ins; sb.append(" v").append(i.getRegisterA()).append("=").append(i.getNarrowLiteral()); }
            else if (ins instanceof Instruction22s) { Instruction22s i = (Instruction22s) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()).append("=").append(i.getNarrowLiteral()); }
            else if (ins instanceof Instruction21lh) { Instruction21lh i = (Instruction21lh) ins; sb.append(" v").append(i.getRegisterA()).append("=").append(i.getWideLiteral()); }
            else if (ins instanceof Instruction21ih) { Instruction21ih i = (Instruction21ih) ins; sb.append(" v").append(i.getRegisterA()).append("=").append(i.getNarrowLiteral()); }
            else if (ins instanceof Instruction22x) { Instruction22x i = (Instruction22x) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()); }
            else if (ins instanceof Instruction22cs) { Instruction22cs i = (Instruction22cs) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()).append(" fieldOff=").append(i.getFieldOffset()); }
            else if (ins instanceof Instruction20t) { Instruction20t i = (Instruction20t) ins; int tgt = absOff.get(idx) + i.getCodeOffset(); sb.append(" ->idx").append(off2idx.getOrDefault(tgt, -999)); }
            else if (ins instanceof Instruction20bc) { Instruction20bc i = (Instruction20bc) ins; sb.append(" verr=").append(i.getVerificationError()).append(",").append(refStr(i.getReference())); }
            else if (ins instanceof Instruction31c) { Instruction31c i = (Instruction31c) ins; sb.append(" v").append(i.getRegisterA()).append(",").append(refStr(i.getReference())); }
            else if (ins instanceof PackedSwitchPayload) { PackedSwitchPayload p = (PackedSwitchPayload) ins; sb.append(" targets=["); boolean f = true; for (SwitchElement e : p.getSwitchElements()) { if (!f) sb.append(","); f = false; sb.append(e.getKey()).append("->off").append(e.getOffset()); } sb.append("]"); }
            else if (ins instanceof SparseSwitchPayload) { SparseSwitchPayload p = (SparseSwitchPayload) ins; sb.append(" targets=["); boolean f = true; for (SwitchElement e : p.getSwitchElements()) { if (!f) sb.append(","); f = false; sb.append(e.getKey()).append("->off").append(e.getOffset()); } sb.append("]"); }
            else if (ins instanceof ArrayPayload) { ArrayPayload p = (ArrayPayload) ins; sb.append(" width=").append(p.getElementWidth()).append(" elements=").append(p.getArrayElements()); }
            else if (ins instanceof Instruction31i) { Instruction31i i = (Instruction31i) ins; sb.append(" v").append(i.getRegisterA()).append("=").append(i.getNarrowLiteral()); }
            else if (ins instanceof Instruction51l) { Instruction51l i = (Instruction51l) ins; sb.append(" v").append(i.getRegisterA()).append("=").append(i.getWideLiteral()); }
            else if (ins instanceof Instruction22b) { Instruction22b i = (Instruction22b) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()).append("=").append(i.getNarrowLiteral()); }
            else if (ins instanceof Instruction23x) { Instruction23x i = (Instruction23x) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()).append(",v").append(i.getRegisterC()); }
            else if (ins instanceof Instruction32x) { Instruction32x i = (Instruction32x) ins; sb.append(" v").append(i.getRegisterA()).append(",v").append(i.getRegisterB()); }
            else { sb.append(" ").append(ins.toString()); }
        } catch (Exception e) { sb.append(" <?").append(e.getMessage()).append(">"); }
        return sb.toString();
    }
    static String regList(Instruction35c i) {
        StringBuilder sb = new StringBuilder();
        int n = i.getRegisterCount();
        int[] regs = {i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG()};
        for (int k = 0; k < n && k < 5; k++) { if (k > 0) sb.append(","); sb.append("v").append(regs[k]); }
        return sb.toString();
    }
    static String refStr(Reference r) {
        if (r instanceof FieldReference) { FieldReference f = (FieldReference) r; return f.getDefiningClass() + "->" + f.getName() + ":" + f.getType(); }
        if (r instanceof MethodReference) { MethodReference m = (MethodReference) r; return m.getDefiningClass() + "->" + m.getName() + m.getParameterTypes() + m.getReturnType(); }
        if (r instanceof StringReference) { return "\"" + ((StringReference) r).getString() + "\""; }
        if (r instanceof TypeReference) { return ((TypeReference) r).getType(); }
        return String.valueOf(r);
    }
    static Map<String, List<String>> methods(DexFile dex) {
        Map<String, List<String>> m = new TreeMap<>();
        for (ClassDef c : dex.getClasses()) {
            for (Method meth : c.getMethods()) {
                String key = c.getType() + "->" + meth.getName() + meth.getParameters() + meth.getReturnType();
                MethodImplementation impl = meth.getImplementation();
                List<String> ins = new ArrayList<>();
                if (impl != null) {
                    List<Instruction> list = new ArrayList<>();
                    List<Integer> absOff = new ArrayList<>();
                    Map<Integer,Integer> off2idx = new HashMap<>();
                    int off = 0;
                    for (Instruction i : impl.getInstructions()) { off2idx.put(off, list.size()); absOff.add(off); list.add(i); off += i.getCodeUnits(); }
                    for (int idx = 0; idx < list.size(); idx++) ins.add(render(list.get(idx), off2idx, absOff, idx));
                    // try blocks (start/count/handler as instruction indices)
                    for (TryBlock<? extends ExceptionHandler> tb : impl.getTryBlocks()) {
                        StringBuilder tb2 = new StringBuilder("TRY startIdx=").append(off2idx.getOrDefault(tb.getStartCodeAddress(), -999))
                            .append(" count=").append(tb.getCodeUnitCount());
                        for (ExceptionHandler h : tb.getExceptionHandlers()) tb2.append(" [").append(h.getExceptionType()).append("->idx").append(off2idx.getOrDefault(h.getHandlerCodeAddress(), -999)).append("]");
                        ins.add(tb2.toString());
                    }
                }
                m.put(key, ins);
            }
        }
        return m;
    }
    public static void main(String[] args) throws Exception {
        DexFile a = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        DexFile b = DexFileFactory.loadDexFile(new File(args[1]), Opcodes.forApi(35));
        Map<String, List<String>> ma = methods(a), mb = methods(b);
        int diff = 0;
        for (String k : new TreeSet<>(ma.keySet())) {
            if (!mb.containsKey(k)) { System.out.println("REMOVED: " + k); diff++; continue; }
            if (!ma.get(k).equals(mb.get(k))) {
                System.out.println("CHANGED: " + k);
                List<String> la = ma.get(k), lb = mb.get(k);
                for (int i = 0; i < Math.max(la.size(), lb.size()); i++) {
                    String sa = i < la.size() ? la.get(i) : "<none>";
                    String sb = i < lb.size() ? lb.get(i) : "<none>";
                    if (!sa.equals(sb)) System.out.println("    [" + i + "] A: " + sa + "\n         B: " + sb);
                }
                diff++;
            }
        }
        for (String k : new TreeSet<>(mb.keySet())) {
            if (!ma.containsKey(k)) { System.out.println("ADDED: " + k); diff++; }
        }
        System.out.println("total methods: a=" + ma.size() + " b=" + mb.size() + " diff=" + diff);
    }
}
