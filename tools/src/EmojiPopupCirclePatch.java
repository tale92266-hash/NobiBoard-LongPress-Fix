import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
import org.jf.dexlib2.iface.instruction.formats.Instruction10x;
import org.jf.dexlib2.iface.instruction.formats.Instruction11n;
import org.jf.dexlib2.iface.instruction.formats.Instruction11x;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction31i;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21ih;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21s;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction22c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction22t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction31i;
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
 * Phase 2Fix (code 176071830): circular emoji popup, center = default (index 3).
 *
 * WHY the list was re-permuted: the stock 3x3 grid (verified from Sam's Phase 2
 * screenshots, 5/5 points) maps data index i to visual cell
 *   (col,row) = ((i%3 + 1) % 3, 2 - (i/3))
 * i.e. the columns are rotated +1 vs the Phase 1 design. The old order put the
 * center emoji at visual middle-right and corners at wrong cells. The new order
 *   [_, BR, BL, C, _, _, _, TR, TL]   (blanks = U+200B)
 * puts the 5 real emojis at visual corners+center and the 4 blanks at the
 * edge-centers (invisible), so visually ONLY the 5 emojis show, corners+center.
 * Center is now data index 3 (was 4), so both hooks move 4 -> 3.
 *
 * CIRCLE: the popup background (rounded rect, drawable 0x7f080319 on the outer
 * FrameLayout 0706) is replaced at runtime, scoped to OUR emoji popups only:
 *   emoji popup  -> 0706 gets the new oval drawable 0x7f080786
 *                    (oval, solid #ff3c474c = popup bg color, 1dp #ff9aa0a6 stroke)
 *   other popups -> 0706 gets its stock drawable back (0x7f080319)
 * The view is REUSED across popups (MaterialRectangularPopupView.b reuses field e),
 * hence the reset branch. The background is set on this.c.getParent() (0706);
 * this.c itself (0705) keeps its transparent stock bg. Cells are square 28x28dp
 * (popup 84x84dp + padding), so the oval renders as a true circle.
 *
 * New head block layout (addresses computed in code):
 *   guard (44u, branches -> RESET) | OVAL (14u) | GOTO STOCK (1u) | RESET (14u) | NOP? | STOCK
 * Total is forced EVEN (pad NOP at end of RESET if odd). Zero new refs: every
 * method/field/type used already exists in classes2.dex (View.getParent and
 * View.setBackgroundResource both have callers there; drawable ids are int
 * literals, not refs). e() has no payloads/try blocks.
 *
 * Input dex MUST be the Phase 1Fix backup (e() = old 44u head + 21-insn stock
 * body with the NOPed CONST_4). The old head (first 24 insns) is dropped and
 * replaced; the stock body is kept verbatim.
 */
public class EmojiPopupCirclePatch {
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QML_C =
        new ImmutableFieldReference("Lqml;", "c", "Landroid/widget/LinearLayout;");
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
    static final ImmutableMethodReference VIEW_GETPARENT =
        new ImmutableMethodReference("Landroid/view/View;", "getParent",
            java.util.Collections.emptyList(), "Landroid/view/ViewParent;");
    static final ImmutableMethodReference VIEW_SETBGRES =
        new ImmutableMethodReference("Landroid/view/View;", "setBackgroundResource",
            java.util.Arrays.asList("I"), "V");
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");
    static final ImmutableTypeReference VIEW_TYPE = new ImmutableTypeReference("Landroid/view/View;");

    static final int OVAL_DRAWABLE = 0x7f080786;   // new emoji_popup_circle.xml
    static final int STOCK_POPUP_BG = 0x7f080319;  // Material popup bg (rounded rect)

    static int patchedE = 0;

    /** setBackground block: v0 = (View) this.c.getParent(); v0.setBackgroundResource(id). 14 units. */
    static List<Instruction> bgBlock(int drawableId) {
        List<Instruction> b = new ArrayList<>();
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 5, QML_C));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, VIEW_GETPARENT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        b.add(new ImmutableInstruction21c(Opcode.CHECK_CAST, 0, VIEW_TYPE));
        b.add(new ImmutableInstruction31i(Opcode.CONST, 1, drawableId));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 0, 1, 0, 0, 0, VIEW_SETBGRES));
        int total = 0;
        for (Instruction i : b) total += i.getCodeUnits();
        if (total != 14) throw new RuntimeException("bg block is " + total + " units, expected 14");
        return b;
    }

    /** Build the new head block. RESET/STOCK addresses computed from the layout. */
    static List<Instruction> headBlock() {
        // guard = 44 units (same shape as Phase 1 head, index 4 -> 3, branches -> RESET)
        // OVAL = 14, GOTO = 1, RESET = 14  =>  RESET = 44+14+1 = 59, STOCK = 59+14 = 73 (+1 NOP pad => 74)
        final int RESET = 59;
        final int STOCK = 74; // 73 + 1 NOP pad
        List<Instruction> h = new ArrayList<>();
        // ---- guard (addr 0..43), failures -> RESET ----
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 0));                 // addr 0: v4 = 0
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 5, QML_E));     // addr 1: v0 = this.e
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, RESET - 3));         // addr 3
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_SIZE)); // addr 5
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));               // addr 8: v1 = size
        h.add(new ImmutableInstruction21s(Opcode.CONST_16, 2, 9));               // addr 9: v2 = 9
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 1, 2, RESET - 11));      // addr 11
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 1, 3));                 // addr 13: v1 = 3 (center index)
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0, LIST_GET)); // addr 14
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));        // addr 17: v1 = item[3]
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 2, 1, PVA_TYPE));  // addr 18
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 2, RESET - 20));        // addr 20
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, PVA_E));     // addr 22: v2 = item.e
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, CHARSEQ_TYPE)); // addr 24
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, RESET - 26));        // addr 26
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CHARSEQ_LENGTH)); // addr 28
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));               // addr 31: v3 = length
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, RESET - 32));        // addr 32
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));                 // addr 34: v3 = 0
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 3, 0, 0, 0, CHAR_CODEPOINTAT)); // addr 35
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));               // addr 38: v3 = codePointAt
        h.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 2, 0x10000));    // addr 39
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 3, 2, RESET - 41));      // addr 41
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 3));                 // addr 43: v4 = 3 (center)
        // ---- OVAL block (addr 44..57) ----
        h.addAll(bgBlock(OVAL_DRAWABLE));
        // ---- GOTO STOCK (addr 58) ----
        h.add(new ImmutableInstruction10t(Opcode.GOTO, STOCK - 58));
        // ---- RESET block (addr 59..72) ----
        h.addAll(bgBlock(STOCK_POPUP_BG));
        // ---- pad NOP (addr 73) so total is EVEN ----
        h.add(new ImmutableInstruction10x(Opcode.NOP));
        int total = 0;
        for (Instruction i : h) total += i.getCodeUnits();
        if (total != STOCK) throw new RuntimeException("head block is " + total + " units, expected " + STOCK);
        if ((total % 2) != 0) throw new RuntimeException("head block is ODD (" + total + "), must be EVEN");
        return h;
    }

    static MethodImplementation patchE(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        // Phase 1Fix e(): 24-insn old head + 21-insn stock body = 45 insns, regs 6
        if (insns.size() != 45 || impl.getRegisterCount() != 6) {
            System.out.println("Lqml.e: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (insns.get(0).getOpcode() != Opcode.CONST_4
                || insns.get(23).getOpcode() != Opcode.CONST_4
                || insns.get(24).getOpcode() != Opcode.IGET_OBJECT) {
            System.out.println("Lqml.e: head boundary mismatch: "
                + insns.get(0).getOpcode() + " / " + insns.get(23).getOpcode()
                + " / " + insns.get(24).getOpcode());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.e: unexpected try blocks");
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        all.addAll(insns.subList(24, insns.size())); // stock body verbatim (incl. NOPed CONST_4)
        System.out.println("Lqml.e: new head (74 units, even, center=3, circle bg) + stock body");
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
                            && method.getName().equals("e")
                            && method.getParameters().size() == 0) {
                        MethodImplementation impl = method.getImplementation();
                        if (impl == null) return def.rewrite(method);
                        MethodImplementation ni = patchE(impl);
                        if (ni != null) {
                            patchedE++;
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
        System.out.println("patchedE=" + patchedE);
        if (patchedE != 1) throw new RuntimeException("expected exactly 1 Lqml.e patch");
    }
}
