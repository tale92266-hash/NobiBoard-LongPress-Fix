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
 * Phase 2Fix2 (code 176071831): DIRECTION-based long-press popup selection.
 *
 * Replaces the Phase 2 bounds-based k() head (outside popup -> freeze on the
 * current selection) with a direction-based mapping that works at ANY distance:
 *
 *   - The touch point is measured from the POPUP CENTER (dx, dy).
 *   - Near the center (|dx| < cellW and |dy| < cellH) -> CENTER (index 3).
 *   - Else by dominant diagonal: dy <= dx and dy <= -dx -> UP (6);
 *     dy >= dx and dy >= -dx -> DOWN (0); dx <= dy and dx <= -dy -> LEFT (5);
 *     else RIGHT (4). The four directions partition the whole plane, so the
 *     finger is always on one emoji or directly on the next -- NO blank/dead
 *     zones anywhere, however far outside the popup the finger goes.
 *   - RESTING rule (the finger still on the key below the popup, before any
 *     swipe): if the touch is just below the popup (height <= localY <
 *     height+cellH), within the popup's horizontal span, and the current
 *     selection is already the center (this.g == 3), the center is kept.
 *     Any real swipe (into the popup, far below, or sideways) goes to the
 *     direction mapping.
 *
 * Plus layout (lists re-permuted in fd2.xml): center=3, UP=6, DOWN=0,
 * LEFT=5, RIGHT=4 (under the proven grid mapping). Corners are invisible
 * U+200B cells that can never be selected now (direction mapping only
 * ever yields one of the 5 emoji indices).
 *
 * Registers: v0-v9 scratch (stock k() writes them before reading), v10=this,
 * v11=x, v12=y are only READ. Zero new refs (Lqml.h/a, List, CharSequence,
 * Character, LinearLayout getWidth/getHeight, Lqmn g/h -- all already in
 * classes2.dex). Head is 170 code units (EVEN). No try blocks, no payloads.
 *
 * Head layout (addresses are absolute code units):
 *   Phase A guard      0..49   (failures -> STOCK)
 *   Phase B geometry   50..85  (localX, width, localY, height, cellW, cellH)
 *   Phase C resting    86..106 (resting -> return a(); else -> DIRECTION)
 *   Phase D direction  107..169 (center/up/down/left/right -> SELECT)
 *   STOCK = 170 (original k() insn 0 follows)
 */
public class PopupDirectionPatch {
    static final ImmutableFieldReference QML_E =
        new ImmutableFieldReference("Lqml;", "e", "Ljava/util/List;");
    static final ImmutableFieldReference QML_D =
        new ImmutableFieldReference("Lqml;", "d", "Ljava/util/List;");
    static final ImmutableFieldReference QML_F =
        new ImmutableFieldReference("Lqml;", "f", "Lqmo;");
    static final ImmutableFieldReference QML_C =
        new ImmutableFieldReference("Lqml;", "c", "Landroid/widget/LinearLayout;");
    static final ImmutableFieldReference QML_G =
        new ImmutableFieldReference("Lqml;", "g", "I");
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

    static final int STOCK = 170;      // end of head (original k() insn 0)
    static final int DIRECTION = 107;  // Phase D start
    static final int NOT_CENTER = 135;
    static final int CHECK_DOWN = 141;
    static final int CHECK_LEFT = 147;
    static final int RIGHT = 153;
    static final int SELECT = 154;

    /** Build the 170-unit head block. */
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
        // ---- Phase B: geometry ----
        // v0=localX, v1=width, v2=localY, v3=height, v4=cellW, v5=cellH
        // addr 50
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 0, 11));
        // addr 51
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_F));
        // addr 53
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 1, QMO_D));
        // addr 55
        h.add(new ImmutableInstruction22c(Opcode.IGET, 2, 1, QMN_G));
        // addr 57
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 0, 0, 2));
        // addr 59
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_C));
        // addr 61
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 1, 0, 0, 0, 0, LL_GETWIDTH));
        // addr 64
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // addr 65
        h.add(new ImmutableInstruction12x(Opcode.FLOAT_TO_INT, 2, 12));
        // addr 66
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 3, 10, QML_F));
        // addr 68
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 3, 3, QMO_D));
        // addr 70
        h.add(new ImmutableInstruction22c(Opcode.IGET, 4, 3, QMN_H));
        // addr 72
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 2, 2, 4));
        // addr 74
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 3, 10, QML_C));
        // addr 76
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 3, 0, 0, 0, 0, LL_GETHEIGHT));
        // addr 79
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // addr 80
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 3));
        // addr 81
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 4, 1, 4));
        // addr 83
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 5, 3));
        // addr 84
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 5, 3, 5));
        // ---- Phase C: resting zone ----
        // addr 86: if localY < height -> DIRECTION (inside/above)
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 2, 3, DIRECTION - 86));
        // addr 88: v6 = height + cellH
        h.add(new ImmutableInstruction23x(Opcode.ADD_INT, 6, 3, 5));
        // addr 90: if localY >= height+cellH -> DIRECTION (far below)
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 2, 6, DIRECTION - 90));
        // addr 92: v6 = 0
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 6, 0));
        // addr 93: if localX <= 0 -> DIRECTION
        h.add(new ImmutableInstruction22t(Opcode.IF_LE, 0, 6, DIRECTION - 93));
        // addr 95: if localX >= width -> DIRECTION
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 0, 1, DIRECTION - 95));
        // addr 97: v6 = this.g
        h.add(new ImmutableInstruction22c(Opcode.IGET, 6, 10, QML_G));
        // addr 99: v7 = 3
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 7, 3));
        // addr 100: if g != 3 -> DIRECTION
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 6, 7, DIRECTION - 100));
        // addr 102: resting -> return a() (keep center)
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 10, 0, 0, 0, 0, QML_A));
        // addr 105
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        // addr 106
        h.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0));
        // ---- Phase D: direction ----
        // v4=dx, v5=dy, v6=negDx, v7=negDy, v8=cellW, v9=cellH
        // addr 107
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 4, 2));
        // addr 108
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 4, 1, 4));
        // addr 110
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 4, 0, 4));
        // addr 112
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 5, 2));
        // addr 113
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 5, 3, 5));
        // addr 115
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 5, 2, 5));
        // addr 117
        h.add(new ImmutableInstruction12x(Opcode.NEG_INT, 6, 4));
        // addr 118
        h.add(new ImmutableInstruction12x(Opcode.NEG_INT, 7, 5));
        // addr 119
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 8, 3));
        // addr 120
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 8, 1, 8));
        // addr 122
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 9, 3));
        // addr 123
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 9, 3, 9));
        // center? |dx|<cellW and |dy|<cellH  (dx<cellW, negDx<cellW, dy<cellH, negDy<cellH)
        // addr 125
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 4, 8, NOT_CENTER - 125));
        // addr 127
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 6, 8, NOT_CENTER - 127));
        // addr 129
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 5, 9, NOT_CENTER - 129));
        // addr 131
        h.add(new ImmutableInstruction22t(Opcode.IF_GE, 7, 9, NOT_CENTER - 131));
        // addr 133: center -> index 3
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 3));
        // addr 134
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 134));
        // NOT_CENTER addr 135: up? dy<=dx and dy<=negDx
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 5, 4, CHECK_DOWN - 135));
        // addr 137
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 5, 6, CHECK_DOWN - 137));
        // addr 139: up -> index 6
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 6));
        // addr 140
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 140));
        // CHECK_DOWN addr 141: down? dy>=dx and dy>=negDx
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 5, 4, CHECK_LEFT - 141));
        // addr 143
        h.add(new ImmutableInstruction22t(Opcode.IF_LT, 5, 6, CHECK_LEFT - 143));
        // addr 145: down -> index 0
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
        // addr 146
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 146));
        // CHECK_LEFT addr 147: left? dx<=dy and dx<=negDy
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 4, 5, RIGHT - 147));
        // addr 149
        h.add(new ImmutableInstruction22t(Opcode.IF_GT, 4, 7, RIGHT - 149));
        // addr 151: left -> index 5
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 5));
        // addr 152
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 152));
        // RIGHT addr 153: right -> index 4
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 4));
        // SELECT addr 154: view = this.d.get(index); h(view); return a()
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 10, QML_D));
        // addr 156
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 1, 0, 0, 0, 0, LIST_GET));
        // addr 159
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 1));
        // addr 160
        h.add(new ImmutableInstruction21c(Opcode.CHECK_CAST, 1, VIEW_TYPE));
        // addr 162
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 10, 1, 0, 0, 0, QML_H));
        // addr 165
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 10, 0, 0, 0, 0, QML_A));
        // addr 168
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        // addr 169
        h.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0));
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
        // Phase 2Fix k(): 46-insn old head (86u) + 198-insn stock body = 244 insns, regs 13
        if (insns.size() != 244 || impl.getRegisterCount() != 13) {
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
                || insns.get(46).getOpcode() != Opcode.IGET_OBJECT) {
            System.out.println("Lqml.k: boundary mismatch: " + insns.get(0).getOpcode()
                + " / " + insns.get(1).getOpcode() + " / " + insns.get(46).getOpcode());
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        all.addAll(insns.subList(46, insns.size())); // stock body verbatim
        System.out.println("Lqml.k: new direction head (" + STOCK + " units, even) + stock body");
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
