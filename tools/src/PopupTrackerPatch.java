import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction10t;
import org.jf.dexlib2.iface.instruction.formats.Instruction10x;
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
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2Fix4 (code 176071833): drive the Lqmr TRACKER in Lqml.k().
 *
 * DIAGNOSIS (proven, see diagnosis.md): Sam's letter popup is the SILK popup
 * (controller Lqmu). Its green circle + inserted value are BOTH driven by the
 * Lqmr spatial tracker, NOT by Lqml.g / pressed state. All three previous
 * k() heads set g + pressed state and early-returned, BYPASSING the stock
 * k() body -- the ONLY caller of Lqmu.i(), the only method that updates the
 * tracker. So the tracker stayed frozen at its init point (1,2) = DOWN.
 *
 * FIX (this file): keep the Phase 2Fix3 relative-delta direction computation
 * (proven correct: dx/dy from the stock anchor this.i/this.b, center iff
 * |dx|<cellW and |dy|<cellH, else dominant diagonal -> UP(6)/DOWN(0)/
 * LEFT(5)/RIGHT(4)), but change its DESTINATION: instead of h(view) (writes
 * g, which Silk never reads), compute the PIXEL CENTER of the target cell
 * in container-content-local coordinates -- the exact coordinate space the
 * stock k() body passes to i() (it subtracts Lqmn.g/h + padding and clamps
 * to content dims before INVOKE_VIRTUAL Lqml->i(II)V) -- and call
 * this.i(px,py) VIRTUALLY. Virtual dispatch sends it to Lqmu.i for Silk
 * (tracker moves, indicator moves, a() returns the swiped item) and to
 * Lqml.i for Material (presses the right view). Then return a().
 *
 * Pixel centers (contentW/H = container size minus padding, cellW/H = /3):
 *   CENTER (1,1): (contentW/2, contentH/2)
 *   UP     (1,0): (contentW/2, cellH/2)
 *   DOWN   (1,2): (contentW/2, contentH - cellH/2)
 *   LEFT   (0,1): (cellW/2, contentH/2)
 *   RIGHT  (2,1): (contentW - cellW/2, contentH/2)
 * The mapping only ever yields indices 3/6/0/5/4, so the 4 U+200B corner
 * blanks stay unreachable, exactly as before.
 *
 * Registers: v0-v9 scratch (stock k() writes them before reading), v10=this,
 * v11=x, v12=y are only READ. Zero new refs (QML_I, padding getters, i(),
 * a() are all already referenced by the stock k() body). Head is 200 code
 * units (EVEN, NOP-padded). No try blocks, no payloads.
 *
 * Head layout (absolute code units):
 *   Phase A guard   0..49    (failures -> STOCK)
 *   Phase B delta   50..85   (anchor, dx/dy, cellW/cellH thresholds)
 *   Phase C dir     86..114  (center/up/down/left/right -> index in v0)
 *   Phase D pixels  115..190 (content dims + cell-center mapping -> px/py)
 *   SELECT          191..198 (i(px,py); return a())
 *   NOP pad         199
 *   STOCK = 200
 */
public class PopupTrackerPatch {
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
    static final ImmutableMethodReference VIEW_GETPADLEFT =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingLeft",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_GETPADRIGHT =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingRight",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_GETPADTOP =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingTop",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference VIEW_GETPADBOTTOM =
        new ImmutableMethodReference("Landroid/view/View;", "getPaddingBottom",
            java.util.Collections.emptyList(), "I");
    static final ImmutableMethodReference QML_A =
        new ImmutableMethodReference("Lqml;", "a",
            java.util.Collections.emptyList(), "Lpva;");
    static final ImmutableMethodReference QML_I2 =
        new ImmutableMethodReference("Lqml;", "i",
            java.util.Arrays.asList("I", "I"), "V");
    static final ImmutableTypeReference PVA_TYPE = new ImmutableTypeReference("Lpva;");
    static final ImmutableTypeReference CHARSEQ_TYPE =
        new ImmutableTypeReference("Ljava/lang/CharSequence;");

    static final int STOCK = 200;      // end of head (original k() insn 76 follows)
    static final int CONTINUE = 62;    // after the (0,0) anchor check
    static final int NOT_CENTER = 96;
    static final int CHECK_DOWN = 102;
    static final int CHECK_LEFT = 108;
    static final int RIGHT = 114;      // index 4 (fall-through into Phase D)
    static final int CHECK_UP = 169;   // pixel mapping
    static final int CHECK_DOWN_P = 175;
    static final int CHECK_LEFT_P = 182;
    static final int PIX_RIGHT = 188;
    static final int SELECT = 191;

    /** Build the 200-unit head block. */
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
        // ---- Phase C: direction from the delta (index -> v0) ----
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
        // RIGHT addr 114: right -> index 4 (falls through into Phase D)
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 4));
        // ---- Phase D: pixel center of the target cell (content-local) ----
        // v9 = container; v1 = contentW; v3 = contentH
        // addr 115
        h.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 9, 10, QML_C));
        // addr 117
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, LL_GETWIDTH));
        // addr 120
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 1));
        // addr 121
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADLEFT));
        // addr 124
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        // addr 125
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 1, 1, 2));
        // addr 127
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADRIGHT));
        // addr 130
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        // addr 131
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 1, 1, 2));
        // addr 133
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, LL_GETHEIGHT));
        // addr 136
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 3));
        // addr 137
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADTOP));
        // addr 140
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        // addr 141
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 3, 3, 2));
        // addr 143
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 9, 0, 0, 0, 0, VIEW_GETPADBOTTOM));
        // addr 146
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 2));
        // addr 147
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 3, 3, 2));
        // addr 149: v8 = 3, v2 = 2 (divisors)
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 8, 3));
        // addr 150
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 2));
        // addr 151: v4 = cellW = contentW/3
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 4, 1, 8));
        // addr 153: v5 = cellH = contentH/3
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 5, 3, 8));
        // addr 155: v6 = centerX = contentW/2
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 6, 1, 2));
        // addr 157: v7 = centerY = contentH/2
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 7, 3, 2));
        // addr 159: v8 = halfCellW = cellW/2
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 8, 4, 2));
        // addr 161: v9 = halfCellH = cellH/2
        h.add(new ImmutableInstruction23x(Opcode.DIV_INT, 9, 5, 2));
        // ---- pixel mapping: v0 = index -> (v4=px, v5=py) ----
        // addr 163
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 3));
        // addr 164
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 0, 2, CHECK_UP - 164));
        // addr 166: CENTER: px = centerX, py = centerY
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 4, 6));
        // addr 167
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 5, 7));
        // addr 168
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 168));
        // CHECK_UP addr 169
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 6));
        // addr 170
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 0, 2, CHECK_DOWN_P - 170));
        // addr 172: UP: px = centerX, py = halfCellH
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 4, 6));
        // addr 173
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 5, 9));
        // addr 174
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 174));
        // CHECK_DOWN_P addr 175
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 0));
        // addr 176
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 0, 2, CHECK_LEFT_P - 176));
        // addr 178: DOWN: px = centerX, py = contentH - halfCellH
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 4, 6));
        // addr 179
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 5, 3, 9));
        // addr 181
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 181));
        // CHECK_LEFT_P addr 182
        h.add(new ImmutableInstruction11n(Opcode.CONST_4, 2, 5));
        // addr 183
        h.add(new ImmutableInstruction22t(Opcode.IF_NE, 0, 2, PIX_RIGHT - 183));
        // addr 185: LEFT: px = halfCellW, py = centerY
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 4, 8));
        // addr 186
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 5, 7));
        // addr 187
        h.add(new ImmutableInstruction10t(Opcode.GOTO, SELECT - 187));
        // PIX_RIGHT addr 188: px = contentW - halfCellW, py = centerY
        h.add(new ImmutableInstruction23x(Opcode.SUB_INT, 4, 1, 8));
        // addr 190
        h.add(new ImmutableInstruction12x(Opcode.MOVE, 5, 7));
        // SELECT addr 191: this.i(px, py) -- virtual dispatch to Lqmu.i (Silk) / Lqml.i (Material)
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 10, 4, 5, 0, 0, QML_I2));
        // addr 194
        h.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 10, 0, 0, 0, 0, QML_A));
        // addr 197
        h.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 0));
        // addr 198
        h.add(new ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0));
        // addr 199: NOP pad (head must be EVEN)
        h.add(new ImmutableInstruction10x(Opcode.NOP));
        int total = 0;
        for (Instruction i : h) total += i.getCodeUnits();
        if (total != STOCK) throw new RuntimeException("k() head is " + total + " units, expected " + STOCK);
        if ((total % 2) != 0) throw new RuntimeException("k() head is ODD (" + total + "), must be EVEN");
        return h;
    }

    static MethodImplementation patchK(MethodImplementation impl) {
        List<Instruction> insns = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) insns.add(i);
        // Phase 2Fix3 k(): 76-insn delta head (132u) + 198-insn stock body = 274 insns, regs 13
        if (insns.size() != 274 || impl.getRegisterCount() != 13) {
            System.out.println("Lqml.k: unexpected shape ins=" + insns.size()
                + " regs=" + impl.getRegisterCount());
            return null;
        }
        if (!impl.getTryBlocks().isEmpty()) {
            System.out.println("Lqml.k: unexpected try blocks");
            return null;
        }
        if (insns.get(0).getOpcode() != Opcode.IGET_OBJECT
                || insns.get(75).getOpcode() != Opcode.NOP
                || insns.get(76).getOpcode() != Opcode.IGET_OBJECT) {
            System.out.println("Lqml.k: boundary mismatch: " + insns.get(0).getOpcode()
                + " / " + insns.get(75).getOpcode() + " / " + insns.get(76).getOpcode());
            return null;
        }
        List<Instruction> all = new ArrayList<>();
        all.addAll(headBlock());
        all.addAll(insns.subList(76, insns.size())); // stock body verbatim
        System.out.println("Lqml.k: new tracker head (" + STOCK + " units, even) + stock body");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, impl.getTryBlocks(), impl.getDebugItems());
    }
}
