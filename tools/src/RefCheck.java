import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
public class RefCheck {
  static String find(DexFile dex, String sub) {
    for (ClassDef cls : dex.getClasses()) {
      for (Method m : cls.getMethods()) {
        MethodImplementation impl = m.getImplementation();
        if (impl==null) continue;
        for (Instruction ins : impl.getInstructions()) {
          Reference ref=null;
          if (ins instanceof Instruction35c) ref=((Instruction35c)ins).getReference();
          else if (ins instanceof Instruction3rc) ref=((Instruction3rc)ins).getReference();
          else if (ins instanceof Instruction21c) ref=((Instruction21c)ins).getReference();
          else if (ins instanceof Instruction22c) ref=((Instruction22c)ins).getReference();
          if (ref!=null && ref.toString().contains(sub)) return ref.toString();
        }
      }
    }
    return null;
  }
  public static void main(String[] args) throws Exception {
    DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
    for (String sub : new String[]{"getWidth","getHeight","Lqml;->a()","Lqml;->f:","Lqmo;->d:","Lqmn;->g:","Lqmn;->h:","Lqml;->c:","Lqml;->e:","Lpva;->e:"}) {
      String r = find(dex, sub);
      System.out.println(sub+" => "+(r==null?"NOT FOUND":r));
    }
    int n=0; for (ClassDef c : dex.getClasses()) for (Method m : c.getMethods()) n++;
    System.out.println("method count="+n);
  }
}
