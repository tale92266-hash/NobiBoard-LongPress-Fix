import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import java.io.File;
public class MethodInfo {
  public static void main(String[] args) throws Exception {
    DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
    for (ClassDef cls : dex.getClasses()) {
      if (!cls.getType().equals(args[1])) continue;
      for (Method m : cls.getMethods()) {
        if (!m.getName().equals(args[2])) continue;
        MethodImplementation impl = m.getImplementation();
        if (impl==null) { System.out.println("no impl"); return; }
        System.out.println("regs="+impl.getRegisterCount()+" insns="+count(impl)+" tryBlocks="+impl.getTryBlocks().size());
        for (TryBlock tb : impl.getTryBlocks()) System.out.println("  try start="+tb.getStartCodeAddress()+" count="+tb.getCodeUnitCount()+" handlers="+tb.getExceptionHandlers().size());
        boolean payload=false;
        for (Instruction ins : impl.getInstructions()) {
          String op=ins.getOpcode().name();
          if (op.contains("SWITCH")||op.contains("FILL_ARRAY")||op.contains("PAYLOAD")) { System.out.println("  payload op: "+op); payload=true; }
        }
        System.out.println("hasPayload="+payload);
      }
    }
  }
  static int count(MethodImplementation impl){int n=0;for(Instruction i:impl.getInstructions())n++;return n;}
}
