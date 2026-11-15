import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
public class FindCallers {
  public static void main(String[] args) throws Exception {
    DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
    String targetCls = args[1], targetM = args[2];
    for (ClassDef cls : dex.getClasses()) {
      for (Method m : cls.getMethods()) {
        MethodImplementation impl = m.getImplementation();
        if (impl==null) continue;
        for (Instruction ins : impl.getInstructions()) {
          Reference ref = null;
          if (ins instanceof Instruction35c) ref = ((Instruction35c)ins).getReference();
          else if (ins instanceof Instruction3rc) ref = ((Instruction3rc)ins).getReference();
          if (ref instanceof MethodReference) {
            MethodReference mr = (MethodReference)ref;
            if (mr.getDefiningClass().equals(targetCls) && mr.getName().equals(targetM)) {
              System.out.println(cls.getType()+" -> "+m.getName()+" calls "+targetCls+"->"+targetM);
            }
          }
        }
      }
    }
  }
}
