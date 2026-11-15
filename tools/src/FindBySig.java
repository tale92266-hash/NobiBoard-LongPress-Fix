import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
public class FindBySig {
  public static void main(String[] args) throws Exception {
    // args: dexFile methodName returnType paramCount
    DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
    String name=args[1], ret=args[2]; int pc=Integer.parseInt(args[3]);
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
            if (mr.getName().equals(name) && mr.getReturnType().equals(ret) && mr.getParameterTypes().size()==pc) {
              System.out.println(cls.getType()+" -> "+m.getName()+" calls "+mr.getDefiningClass()+"->"+name+"("+mr.getParameterTypes()+")"+ret);
            }
          }
        }
      }
    }
  }
}
