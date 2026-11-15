import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction12x;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction12x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21ih;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21s;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction22c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction22t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction23x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2 (code 176071829): center-default touch guard for the emoji popup.
 *
 * WHY: Lqml.e()'s Phase-1 hook correctly selects the center (index 4) at popup
 * open, but the touch path re-selects afterwards. When the popup opens, the
 * keyboard forwards the current touch point to the popup (Lqmi.c -> Ltdt.a ->
 * Lqml.k(x,y) -> Lqml.i(row,col) -> Lqml.h(view)). The finger is still on the
 * key BELOW the popup, so k() clamps the touch to the bottom row and the
 * selection lands on a bottom-row item (BL / the top-row key's number "6"),
 * not the center. Sam's screenshots prove it (green highlight on BL / "6").
 *
 * FIX: head hook in Lqml.k(FF)Lpva;. For OUR emoji popups only (9 items,
 * item[3] a non-BMP CharSequence, same guard as the e() hook), if the touch
 * point is OUTSIDE the popup's item area (finger still on the key below the
 * popup), return the CURRENT selection (this.a() == the center set by e())
 * without changing it. Once the finger slides INTO the popup, k() maps
 * normally and swipe selects the swiped emoji.
 *
 * Coordinates: k()'s (x,y) are KEYBOARD coords (MotionEvent.getX/getY).
 * Lqmn.g/h are the popup's left/top offsets (subtracted in k() itself).
 * localX = (int)x - Lqmn.g, localY = (int)y - Lqmn.h; the popup spans
 * [0, container.getWidth()) x [0, container.getHeight()). Outside if
 * localX<0, localX>=width, localY<0, or localY>=height.
 *
 * Registers: k() has regs=13 (v10=this, v11=x, v12=y). Head uses v0-v9 as
 * scratch (stock k() writes them before reading, verified from the dump).
 * v10/v11/v12 (params) are only READ, never written. Zero new refs: every
 * field/method/type used already exists in classes2.dex (verified by
 * RefCheck2: View/LinearLayout getWidth/getHeight, Lqml.a, Lqml.f, Lqmo.d,
 * Lqmn.g/h, Lqml.c, Lqml.e, Lpva.e, List.size/get, CharSequence.length,
 * Character.codePointAt). method_ids stays 65536.
 *
 * Head block is 86 code units (EVEN). No try blocks, no payloads in k()
 * (verified by MethodInfo). Branch targets: STOCK = end of head (addr 86),
 * OUTSIDE = addr 81.
 */
public class PopupTouchGuardPatch3 {
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QML_F =
        new ImmutableFieldReference("Lqml;", "f", "Lqmo;");
    static final ImmutableFieldReference QML_C =
        new ImmutableFieldReference("Lqml;", "c", "Landroid/widget/LinearLayout;");
    static final ImmutableFieldReference QMO_D =
        new ImmutableFieldReference("Lqmo;", "d", "Lqmn;");
    static final ImmutableFieldReference QMN_G =
        new ImmutableFieldReference("Lqmn;", "g", "I");
    static final ImmutableFieldReference QMN_H =
        new ImmutableFieldReference("Lqmn;", "h", "I");
    static final ImmutableFieldReference PVA_E =
        new ImmutableFieldReference("Lpva;", "e", "Ljava/lang/Object;");
    static final ImmutableMethodReference LIST_SIZE =
        new ImmutableMethodReference("Ljava/util/List;", "size",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference LIST_GET =
        new ImmutableMethodReference("Ljava/util/List;", "get",
            java.util.Arrays.asList("I"), "Ljava/lang/Object;");
    static final ImmutableMethodReference CHARSEQ_LENGTH =
        new ImmutableMethodReference("Ljava/lang/CharSequence;", "length",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference CHAR_CODEPOINTAT =
        new ImmutableMethodReference("Ljava/lang/Character;", "codePointAt",
            java.util.Arrays.asList("Ljava/lang/CharSequence;", "I"), "I");
    static final ImmutableMethodReference LL_GETWIDTH =
        new ImmutableMethodReference("Landroid/widget/LinearLayout;", "getWidth",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference LL_GETHEIGHT =
        new ImmutableMethodReference("Landroid/widget/LinearLayout;", "getHeight",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference QML_A =
        new ImmutableMethodReference("Lqml;", "a",
            java.util.Collections.emptyList(), "Lpva;");
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");

    static final int STOCK = 86;   // end of head block (original k() insn 0)
    static final int OUTSIDE = 81; // return-this.a() block

    /** Build the 86-unit head block. Addresses are absolute code units. */
    static List<Instruction> headBlock() {
        List<Instruction> h = new ArrayList<>();
        // ---- Phase A: emoji popup check (not emoji -> STOCK) ----
        // addr 0
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 10, QML_E));          // v0 = this.e
        // addr 2
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, STOCK - 2));                // if null -> stock
        // addr 4
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_SIZE));
        // addr 7
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));                      // v1 = size
        // addr 8
        h.add(new ImmutableInstruction21s(Opcode.CONST_16, 2, 9));                      // v2 = 9
        // addr 10
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 1, 2, STOCK - 10));             // if size != 9 -> stock
        // addr 12
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 1, 3));                       // v1 = 3 (center index, Phase 2Fix permutation)
        // addr 13
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0, LIST_GET));
        // addr 16
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));               // v1 = item[4]
        // addr 17
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 2, 1, PVA_TYPE));         // v2 = item instanceof Lpva
        // addr 19
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 2, STOCK - 19));               // if not -> stock
        // addr 21
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, PVA_E));            // v2 = item.e
        // addr 23
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, CHARSEQ_TYPE));      // v3 = label instanceof CharSequence
        // addr 25
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, STOCK - 25));               // if not -> stock
        // addr 27
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CHARSEQ_LENGTH));
        // addr 30
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));                      // v3 = length
        // addr 31
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, STOCK - 31));               // if length == 0 -> stock
        // addr 33
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));                       // v3 = 0
        // addr 34
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 3, 0, 0, 0, CHAR_CODEPOINTAT));
        // addr 37
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));                      // v3 = codePointAt(label, 0)
        // addr 38
        h.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 2, 0x10000));           // v2 = 0x10000
        // addr 40
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 3, 2, STOCK - 40));             // if cp < 0x10000 -> stock
        // ---- Phase B: bounds check (inside -> STOCK, outside -> OUTSIDE) ----
        // addr 42
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 0, 11));                 // v0 = (int)x
        // addr 43
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_F));           // v1 = this.f
        // addr 45
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 1, QMO_D));            // v1 = Lqmo.d
        // addr 47
        h.add(new ImmutableInstruction22c(Opcode.IGET, 2, 1, QMN_G));                   // v2 = Lqmn.g
        // addr 49
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 0, 0, 2));                    // v0 = localX = (int)x - g
        // addr 51
        h.add(new ImmutableInstruction21t(Opcode.IF_LTZ, 0, OUTSIDE - 51));             // if localX < 0 -> outside
        // addr 53
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_C));           // v1 = this.c
        // addr 55
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, LL_GETWIDTH));
        // addr 58
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));                      // v1 = width
        // addr 59
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 0, 1, OUTSIDE - 59));           // if localX >= width -> outside
        // addr 61
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 0, 12));                 // v0 = (int)y
        // addr 62
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_F));           // v1 = this.f
        // addr 64
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 1, QMO_D));            // v1 = Lqmo.d
        // addr 66
        h.add(new ImmutableInstruction22c(Opcode.IGET, 2, 1, QMN_H));                   // v2 = Lqmn.h
        // addr 68
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 0, 0, 2));                    // v0 = localY = (int)y - h
        // addr 70
        h.add(new ImmutableInstruction21t(Opcode.IF_LTZ, 0, OUTSIDE - 70));             // if localY < 0 -> outside
        // addr 72
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_C));           // v1 = this.c
        // addr 74
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, LL_GETHEIGHT));
        // addr 77
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));                      // v1 = height
        // addr 78
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 0, 1, OUTSIDE - 78));           // if localY >= height -> outside
        // addr 80
        h.add(new ImmutableInstruction10t(Opcode.GOTO, STOCK - 80));                     // inside -> stock
        // ---- OUTSIDE block (addr 81): return this.a() ----
        // addr 81
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 10, 0, 0, 0, 0, QML_A));
        // addr 84
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));               // v0 = this.a()
        // addr 85
        h.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0));                    // return center
        // addr 86 = STOCK (original k() insn 0 follows)
        int total = 0;
        for (Instruction i : h) total += i.getCodeUnits();
        if (total != STOCK) throw new RuntimeException("head block is " + total + " units, expected " + STOCK);
        if ((total % 2) != 0) throw new RuntimeException("head block is ODD (" + total + "), must be EVEN");
        return h;
    }

    static int patchedK = 0;

    static MethodImplementation patchK(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        if (insns.size() != 198 || impl.getRegisterCount() != 13) {
            System.out.println("Lqml.k: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.k: unexpected try blocks");
            return null;
        }
        // sanity: insn 0 must be IGET_OBJECT (this.c) and insn 2 must be IF_EQZ
        if (insns.get(0).getOpcode() != Opcode.IGET_OBJECT
                || insns.get(2).getOpcode() != Opcode.IF_EQZ) {
            System.out.println("Lqml.k: insn 0/2 mismatch: " + insns.get(0).getOpcode() + " / " + insns.get(2).getOpcode());
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        all.addAll(insns);
        System.out.println("Lqml.k: head block (" + STOCK + " units, even) prepended");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }

    static class Module extends RewriterModule {
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    if (method.getDefiningClass().equals("Lqml;")
                            && method.getName().equals("k")
                            && method.getParameters().size() == 2) {
                        MethodImplementation impl = method.getImplementation();
                        if (impl == null) return def.rewrite(method);
                        MethodImplementation ni = patchK(impl);
                        if (ni != null) {
                            patchedK++;
                            return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                                method.getParameters(), method.getReturnType(),
                                method.getAccessFlags(), method.getAnnotations(), null, ni);
                        }
                    }
                    return def.rewrite(method);
                }
            };
        }
    }

    public static void main(String[] args) throws Exception {
        // args: classes2.dex out.dex
        DexBackedDexFile dex = DexFileFactory.loadDexFile(new java.io.File(args[0]), Opcodes.forApi(35));
        DexRewriter rw = new DexRewriter(new Module());
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patchedK=" + patchedK);
        if (patchedK != 1) throw new RuntimeException("expected exactly 1 Lqml.k patch");
    }
}
