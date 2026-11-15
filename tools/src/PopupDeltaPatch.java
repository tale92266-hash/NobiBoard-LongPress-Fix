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
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
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
import org.jf.dexlib2.iface.instruction.formats.Instruction21ih;
import org.jf.dexlib2.iface.instruction.formats.Instruction21s;
import org.jf.dexlib2.iface.instruction.formats.Instruction21t;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.instruction.formats.Instruction22t;
import org.jf.dexlib2.iface.instruction.formats.Instruction23x;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2Fix3 (code 176071832): RELATIVE swipe-delta selection in Lqml.k().
 *
 * DIAGNOSIS (proven from the dex + Sam's screenshot, see change-report):
 * The Phase 2Fix2 direction head IS firing (verified in the 176071831 dex:
 * head present and correctly wired; k() is called by ALL THREE popup views
 * Material/Rectangular/Silk.a; guard passes for G's popup), but its
 * ABSOLUTE-position mapping is wrong for a resting finger. Measured from
 * Sam's screenshot (924x2048 -> dp): popup view 95.1x95.1dp, top 543.1dp,
 * finger (G key center) at localY = 146.5dp. The resting zone
 * [height, height+cellH) = [95.1, 126.8) is MISSED by ~20dp, so Phase D
 * computes dy = +99dp from the popup center and the dominant-diagonal
 * logic yields DOWN at open. Left/right swipes keep dy dominant (a swipe
 * would need |dx| > 99dp to change selection), so the selection looks
 * stuck on DOWN and "swipe hi nhi hora h".
 *
 * FIX: drive the selection by the SWIPE DELTA from the finger's START
 * point, not by absolute position. The start point needs NO new field:
 * stock Lqml.c() already stores the initial touch/key coordinates in
 * this.i (X) and this.b (Y) at popup creation, and stock k() itself reads
 * them as the anchor (proven from the stock body: i is used as an X edge,
 * b as a Y edge, both compared against Lqmn.g/h in keyboard coords).
 *   dx = (int)x - (int)this.i ; dy = (int)y - (int)this.b
 *   center iff |dx| < cellW and |dy| < cellH (rest: delta ~ 0 -> center)
 *   else dominant diagonal: UP(6) / DOWN(0) / LEFT(5) / RIGHT(4).
 * At rest the delta is ~0 so the center STAYS; any real swipe (however
 * far, any direction) selects the emoji in that direction. The mapping
 * only ever yields indices 3/6/0/5/4, so the 4 U+200B corner blanks stay
 * unreachable. e() (post-block forces center at open) is UNTOUCHED.
 *
 * Registers: v0-v9 scratch (stock k() writes them before reading), v10=this,
 * v11=x, v12=y are only READ. Zero new refs (QML_I/QML_B are already used
 * by the stock k() body). Head is 132 code units (EVEN, NOP-padded).
 * No try blocks, no payloads.
 *
 * Head layout (absolute code units):
 *   Phase A guard   0..49   (failures -> STOCK)
 *   Phase B delta   50..85  (anchor, dx/dy, cellW/cellH)
 *   Phase C dir     86..114 (center/up/down/left/right -> SELECT)
 *   SELECT          115..130
 *   NOP pad         131
 *   STOCK = 132
 */
public class PopupDeltaPatch {
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QML_D =
        new ImmutableFieldReference("Lqml;", "d", "Ljava/util/List;");
    static final ImmutableFieldReference QML_C =
        new ImmutableFieldReference("Lqml;", "c", "Landroid/widget/LinearLayout;");
    static final ImmutableFieldReference QML_I =
        new ImmutableFieldReference("Lqml;", "i", "F");
    static final ImmutableFieldReference QML_B =
        new ImmutableFieldReference("Lqml;", "b", "F");
    static final ImmutableFieldReference PVA_E =
        new ImmutableFieldReference("Lpva;", "e", "Ljava/lang/Object;");
    static final ImmutableMethodReference LIST_SIZE =
        new ImmutableMethodReference("Ljava/util/List;", "size",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference LIST_GET =
        new ImmutableMethodReference("Ljava/util/List;", "get",
            java.util.Arrays.asList("I"), "Ljava/lang/Object;");
    static final ImmutableMethodReference LIST_ISEMPTY =
        new ImmutableMethodReference("Ljava/util/List;", "isEmpty",
            java.util.Collections.emptyList(), "Z");
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
    static final ImmutableMethodReference QML_H =
        new ImmutableMethodReference("Lqml;", "h",
            java.util.Arrays.asList("Landroid/view/View;"), "V");
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");
    static final ImmutableTypeReference VIEW_TYPE = new ImmutableTypeReference("Landroid/view/View;");

    static final int STOCK = 132;      // end of head (original k() insn 97 follows)
    static final int CONTINUE = 62;    // after the (0,0) anchor check
    static final int NOT_CENTER = 96;
    static final int CHECK_DOWN = 102;
    static final int CHECK_LEFT = 108;
    static final int RIGHT = 114;
    static final int SELECT = 115;

    /** Build the 132-unit head block. */
    static List<Instruction> headBlock() {
        List<Instruction> h = new ArrayList<>();
        // ---- Phase A: emoji popup check (not emoji -> STOCK) ----
        // addr 0
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 10, QML_E));
        // addr 2
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 0, STOCK - 2));
        // addr 4
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_SIZE));
        // addr 7
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // addr 8
        h.add(new ImmutableInstruction21s(Opcode.CONST_16, 2, 9));
        // addr 10
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 1, 2, STOCK - 10));
        // addr 12
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 1, 3));
        // addr 13
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0, LIST_GET));
        // addr 16
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        // addr 17
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 2, 1, PVA_TYPE));
        // addr 19
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 2, STOCK - 19));
        // addr 21
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 2, 1, PVA_E));
        // addr 23
        h.add(new ImmutableInstruction22c(Opcode.INSTANCE_OF, 3, 2, CHARSEQ_TYPE));
        // addr 25
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, STOCK - 25));
        // addr 27
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 2, 0, 0, 0, 0, CHARSEQ_LENGTH));
        // addr 30
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // addr 31
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, STOCK - 31));
        // addr 33
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));
        // addr 34
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 3, 0, 0, 0, CHAR_CODEPOINTAT));
        // addr 37
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // addr 38
        h.add(new ImmutableInstruction21ih(Opcode.CONST_HIGH16, 2, 0x10000));
        // addr 40
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 3, 2, STOCK - 40));
        // addr 42: this.d must be non-empty (else STOCK, stock k() handles it)
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 0, 10, QML_D));
        // addr 44
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 1, 0, 0, 0, 0, 0, LIST_ISEMPTY));
        // addr 47
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 0));
        // addr 48
        h.add(new ImmutableInstruction21t(Opcode.IF_NEZ, 0, STOCK - 48));
        // ---- Phase B: delta from the stock anchor (this.i=X, this.b=Y) ----
        // v0=dx, v1=anchorX, v2=dy, v3=anchorY, v4=cellW, v5=cellH
        // addr 50
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 0, 11));
        // addr 51
        h.add(new ImmutableInstruction22c(Opcode.IGET, 1, 10, QML_I));
        // addr 53
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 1, 1));
        // addr 54
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 2, 12));
        // addr 55
        h.add(new ImmutableInstruction22c(Opcode.IGET, 3, 10, QML_B));
        // addr 57
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 3, 3));
        // addr 58: if anchorX != 0 -> CONTINUE (anchor valid)
        h.add(new ImmutableInstruction21t(Opcode.IF_NEZ, 1, CONTINUE - 58));
        // addr 60: if anchorY == 0 too -> STOCK (anchor never set)
        h.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 3, STOCK - 60));
        // addr 62: dx = (int)x - anchorX
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 0, 0, 1));
        // addr 64: dy = (int)y - anchorY
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 2, 2, 3));
        // addr 66: cellW = this.c.getWidth() / 3
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_C));
        // addr 68
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, LL_GETWIDTH));
        // addr 71
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // addr 72
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 3));
        // addr 73
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 4, 1, 4));
        // addr 75: cellH = this.c.getHeight() / 3
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 3, 10, QML_C));
        // addr 77
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 3, 0, 0, 0, 0, LL_GETHEIGHT));
        // addr 80
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // addr 81
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 5, 3));
        // addr 82
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 5, 3, 5));
        // addr 84: v6 = -dx, v7 = -dy
        h.add(new ImmutableInstruction12x(Opcode.NEG_INT, 6, 0));
        // addr 85
        h.add(new ImmutableInstruction12x(Opcode.NEG_INT, 7, 2));
        // ---- Phase C: direction from the delta ----
        // center? |dx|<cellW and |dy|<cellH
        // addr 86
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 0, 4, NOT_CENTER - 86));
        // addr 88
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 6, 4, NOT_CENTER - 88));
        // addr 90
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 2, 5, NOT_CENTER - 90));
        // addr 92
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 7, 5, NOT_CENTER - 92));
        // addr 94: center -> index 3
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 3));
        // addr 95
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 95));
        // NOT_CENTER addr 96: up? dy<=dx and dy<=negDx
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 2, 0, CHECK_DOWN - 96));
        // addr 98
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 2, 6, CHECK_DOWN - 98));
        // addr 100: up -> index 6
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 6));
        // addr 101
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 101));
        // CHECK_DOWN addr 102: down? dy>=dx and dy>=negDx
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 2, 0, CHECK_LEFT - 102));
        // addr 104
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 2, 6, CHECK_LEFT - 104));
        // addr 106: down -> index 0
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        // addr 107
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 107));
        // CHECK_LEFT addr 108: left? dx<=dy and dx<=negDy
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 0, 2, RIGHT - 108));
        // addr 110
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 0, 7, RIGHT - 110));
        // addr 112: left -> index 5
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 5));
        // addr 113
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 113));
        // RIGHT addr 114: right -> index 4
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 4));
        // SELECT addr 115: view = this.d.get(index); h(view); return a()
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_D));
        // addr 117
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 1, 0, 0, 0, 0, LIST_GET));
        // addr 120
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        // addr 121
        h.add(new ImmutableInstruction21c(Opcode.CHECK_CAST, 1, VIEW_TYPE));
        // addr 123
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 10, 1, 0, 0, 0, QML_H));
        // addr 126
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 10, 0, 0, 0, 0, QML_A));
        // addr 129
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        // addr 130
        h.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0));
        // addr 131: NOP pad (head must be EVEN)
        h.add(new ImmutableInstruction10x(Opcode.NOP));
        int total = 0;
        for (Instruction i : h) total += i.getCodeUnits();
        if (total != STOCK) throw new RuntimeException("k() head is " + total + " units, expected " + STOCK);
        if ((total % 2) != 0) throw new RuntimeException("k() head is ODD (" + total + "), must be EVEN");
        return h;
    }

    static int patchedK = 0;

    static MethodImplementation patchK(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        // Phase 2Fix2 k(): 97-insn direction head (170u) + 198-insn stock body = 295 insns, regs 13
        if (insns.size() != 295 || impl.getRegisterCount() != 13) {
            System.out.println("Lqml.k: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.k: unexpected try blocks");
            return null;
        }
        if (insns.get(0).getOpcode() != Opcode.IGET_OBJECT
                || insns.get(1).getOpcode() != Opcode.IF_EQZ
                || insns.get(97).getOpcode() != Opcode.IGET_OBJECT) {
            System.out.println("Lqml.k: boundary mismatch: " + insns.get(0).getOpcode()
                + " / " + insns.get(1).getOpcode() + " / " + insns.get(97).getOpcode());
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        all.addAll(insns.subList(97, insns.size())); // stock body verbatim
        System.out.println("Lqml.k: new delta head (" + STOCK + " units, even) + stock body");
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
