import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.*;
import java.io.File;
public class ListFields {
  public static void main(String[] args) throws Exception {
    DexFile dex = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.getDefault());
    for (ClassDef cls : dex.getClasses()) {
      if (cls.getType().equals(args[1])) {
        System.out.println("CLASS "+cls.getType());
        for (Field f : cls.getFields()) System.out.println("  F "+f.getName()+" : "+f.getType());
      }
    }
  }
}
