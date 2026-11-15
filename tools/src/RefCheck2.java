import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;
public class RefCheck2 {
  public static void main(String[] args) throws Exception {
    DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
    Set<String> mrefs = new HashSet<>();
    // refs used specifically inside Lqml.k
    for (ClassDef cls : dex.getClasses()) {
      if (!cls.getType().equals("Lqml;")) continue;
      for (Method m : cls.getMethods()) {
        if (!m.getName().equals("k")) continue;
        MethodImplementation impl = m.getImplementation();
        for (Instruction ins : impl.getInstructions()) {
          Reference ref=null;
          if (ins instanceof Instruction35c) ref=((Instruction35c)ins).getReference();
          else if (ins instanceof Instruction3rc) ref=((Instruction3rc)ins).getReference();
          else if (ins instanceof Instruction21c) ref=((Instruction21c)ins).getReference();
          else if (ins instanceof Instruction22c) ref=((Instruction22c)ins).getReference();
          if (ref!=null) System.out.println("k uses: "+ref);
        }
      }
    }
    // count unique method refs in whole dex
    for (ClassDef cls : dex.getClasses()) {
      for (Method m : cls.getMethods()) {
        MethodImplementation impl = m.getImplementation();
        if (impl==null) continue;
        for (Instruction ins : impl.getInstructions()) {
          Reference ref=null;
          if (ins instanceof Instruction35c) ref=((Instruction35c)ins).getReference();
          else if (ins instanceof Instruction3rc) ref=((Instruction3rc)ins).getReference();
          if (ref instanceof MethodReference) mrefs.add(ref.toString());
        }
      }
    }
    System.out.println("unique method refs used="+mrefs.size());
    System.out.println("View.getWidth exists="+mrefs.contains("Landroid/view/View;->getWidth()I"));
    System.out.println("View.getHeight exists="+mrefs.contains("Landroid/view/View;->getHeight()I"));
    System.out.println("LinearLayout.getWidth exists="+mrefs.contains("Landroid/widget/LinearLayout;->getWidth()I"));
    System.out.println("LinearLayout.getHeight exists="+mrefs.contains("Landroid/widget/LinearLayout;->getHeight()I"));
    System.out.println("Lqml.a exists="+mrefs.contains("Lqml;->a()Lpva;"));
  }
}
