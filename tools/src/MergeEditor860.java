import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.dexlib2.writer.io.FileDataStore;
import java.io.File;
import java.util.*;

/**
 * Merge the NEW editor.dex (with the Diagnostics section) into classes6.
 * Usage: MergeEditor860 <859 classes6.dex> <new editor.dex> <out classes6.dex>
 *
 * Takes the new editor.dex classes FIRST (13 classes, com.nobi.nobiboard),
 * then the 859 classes6 classes EXCEPT the old editor classes
 * (com/nobi/nobiboard/*) — keeping only CapsLockHelper
 * (Lcom/nobiboard/helper/CapsLockHelper;).
 * The old synthetic "$EmojiGridAdapter-IA" class is dropped (it was an empty
 * d8 desugaring artifact, referenced only by its own descriptor + a synthetic
 * constructor that the new d8 --lib build does not generate).
 * Uses DexPool.internClass (DexBuilder cannot merge DexBacked dexes).
 */
public class MergeEditor860 {
    public static void main(String[] args) throws Exception {
        DexBackedDexFile c6 = DexFileFactory.loadDexFile(new File(args[0]), Opcodes.forApi(35));
        DexBackedDexFile ed = DexFileFactory.loadDexFile(new File(args[1]), Opcodes.forApi(35));
        DexPool pool = new DexPool(Opcodes.forApi(35));
        Set<String> seen = new HashSet<>();
        int nEd = 0, nKept = 0, nDropped = 0;
        for (ClassDef cd : ed.getClasses()) {
            if (!seen.add(cd.getType())) throw new RuntimeException("dup in editor.dex: " + cd.getType());
            pool.internClass(cd);
            nEd++;
        }
        System.out.println("editor.dex: interned " + nEd + " classes");
        for (ClassDef cd : c6.getClasses()) {
            if (cd.getType().startsWith("Lcom/nobi/nobiboard/")) { nDropped++; continue; }
            if (!seen.add(cd.getType())) throw new RuntimeException("dup class: " + cd.getType());
            pool.internClass(cd);
            nKept++;
            System.out.println("kept from 859 classes6: " + cd.getType());
        }
        System.out.println("859 classes6: kept " + nKept + ", dropped " + nDropped + " old editor classes");
        FileDataStore store = new FileDataStore(new File(args[2]));
        pool.writeTo(store);
        store.close();
        System.out.println("merged classes6 written to " + args[2]);
    }
}
