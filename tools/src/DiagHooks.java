import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

public class DiagHooks {
    static void dumpMethod(DexFile dex, String cls, String name) {
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals(cls)) continue;
            for (Method m : cd.getMethods()) {
                if (!m.getName().equals(name)) continue;
                MethodImplementation impl = m.getImplementation();
                if (impl == null) { System.out.println(cls + "->" + name + ": no impl"); return; }
                int flags = m.getAccessFlags();
                boolean isStatic = (flags & 0x0008) != 0;
                System.out.println("METHOD " + cls + "->" + name + " static=" + isStatic
                    + " flags=0x" + Integer.toHexString(flags)
                    + " regs=" + impl.getRegisterCount()
                    + " params=" + m.getParameters());
                List<Instruction> list = new ArrayList<>();
                List<Integer> absOff = new ArrayList<>();
                int off = 0;
                for (Instruction ins : impl.getInstructions()) { absOff.add(off); list.add(ins); off += ins.getCodeUnits(); }
                System.out.println("  total code units=" + off + " insn count=" + list.size());
                for (int i = 0; i < Math.min(8, list.size()); i++) {
                    System.out.println("  [" + i + "] off=" + absOff.get(i) + " " + list.get(i).getOpcode() + " " + fmtRef(list.get(i)));
                }
                System.out.println("  try blocks: " + impl.getTryBlocks().size());
                for (TryBlock<? extends ExceptionHandler> tb : impl.getTryBlocks()) {
                    System.out.print("    start=" + tb.getStartCodeAddress() + " count=" + tb.getCodeUnitCount() + " handlers:");
                    for (ExceptionHandler h : tb.getExceptionHandlers()) {
                        System.out.print(" [" + h.getExceptionType() + " -> " + h.getHandlerCodeAddress() + "]");
                    }
                    System.out.println();
                }
                System.out.println("  payload refs (switch/array):");
                for (int i = 0; i < list.size(); i++) {
                    Instruction ins = list.get(i);
                    Opcode op = ins.getOpcode();
                    if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH || op == Opcode.FILL_ARRAY_DATA) {
                        int tgt = absOff.get(i) + ((OffsetInstruction) ins).getCodeOffset();
                        System.out.println("    [" + i + "] off=" + absOff.get(i) + " " + op + " -> payload off=" + tgt + " parity=" + (tgt % 2 == 0 ? "EVEN" : "ODD"));
                    }
                }
            }
        }
    }

    static String fmtRef(Instruction ins) {
        try {
            if (ins instanceof Instruction21c) {
                Reference r = ((Instruction21c) ins).getReference();
                if (r instanceof FieldReference) { FieldReference f = (FieldReference) r; return f.getDefiningClass() + "->" + f.getName() + ":" + f.getType(); }
                if (r instanceof MethodReference) { MethodReference mr = (MethodReference) r; return mr.getDefiningClass() + "->" + mr.getName() + mr.getParameterTypes(); }
                return String.valueOf(r);
            }
            if (ins instanceof Instruction22c) {
                Reference r = ((Instruction22c) ins).getReference();
                if (r instanceof FieldReference) { FieldReference f = (FieldReference) r; return f.getDefiningClass() + "->" + f.getName() + ":" + f.getType(); }
                return String.valueOf(r);
            }
            if (ins instanceof Instruction35c) {
                Reference r = ((Instruction35c) ins).getReference();
                if (r instanceof MethodReference) { MethodReference mr = (MethodReference) r; return mr.getDefiningClass() + "->" + mr.getName() + mr.getParameterTypes(); }
                return String.valueOf(r);
            }
        } catch (Exception e) { return "?"; }
        return "";
    }

    static void dumpFieldBRefs(DexFile dex) {
        System.out.println("ALL refs to Lovx;->b (any type):");
        for (ClassDef cd : dex.getClasses()) {
            for (Method m : cd.getMethods()) {
                MethodImplementation impl = m.getImplementation();
                if (impl == null) continue;
                for (Instruction ins : impl.getInstructions()) {
                    if (ins instanceof Instruction22c) {
                        Reference r = ((Instruction22c) ins).getReference();
                        if (r instanceof FieldReference) {
                            FieldReference f = (FieldReference) r;
                            if (f.getDefiningClass().equals("Lovx;") && f.getName().equals("b")) {
                                System.out.println("  " + cd.getType() + "->" + m.getName() + " : " + f.getType() + " op=" + ins.getOpcode());
                            }
                        }
                    }
                }
            }
        }
    }

    static void dumpFieldDecl(DexFile dex) {
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lovx;")) continue;
            for (Field f : cd.getFields()) {
                if (f.getName().equals("b")) {
                    System.out.println("FIELD DECL Lovx;->b declared type = " + f.getType());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        System.out.println("== " + args[0] + " ==");
        dumpFieldDecl(dex);
        dumpMethod(dex, "Lovx;", "h");
        dumpMethod(dex, "Lovc;", "G");
        dumpMethod(dex, "Levt;", "b");
        dumpFieldBRefs(dex);
    }
}
