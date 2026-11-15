import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.ImmutableTryBlock;
import org.jf.dexlib2.immutable.ImmutableExceptionHandler;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NobiBoard DIAG (code 176071842) — diagnostic instrumentation ONLY, no
 * behavior change. Patches exactly ONE method, Lqck.D(J)V (the long-press
 * popup-creation entry, 325 insns, registers=27, one catch-all try block
 * start=9 count=614 handler=627), in two places:
 *
 *  (a) ENTRY MARKER, inserted after insn 1 (the parameter moves), before
 *      insn 2: Log.e("NobiDiag", "D-enter 176071842") + a Toast with the
 *      same text (guarded: view/context null -> Toast skipped, Log still
 *      runs). Proves WHICH build is running and whether D is entered.
 *  (b) CATCH-HANDLER LOG, inserted after insn 322 (MOVE_EXCEPTION v0),
 *      before insn 323: Log.e("NobiDiag", "ERR", throwable) (3-arg form,
 *      full stack trace) + a Toast "ERR <Throwable.toString()>".
 *      The caught Throwable stays in v0 for the original THROW.
 *
 * DESIGN CONSTRAINTS (all verified against the shipped 176071841 dex):
 *  - NO register-count bump (registers stay 27). Bumping would shift the
 *    parameter registers (this=v24, long param=v25/v26) and break the
 *    MOVE_FROM16s at insns 0-1 — the SAME class of bug that killed
 *    Lqmu.o() in 835/836 (registers 6->9 left the body reading v4/v5
 *    after this/param moved to v7/v8). Scratch is v3/v4/v5, which are
 *    written before being read in both inserted blocks, and which the
 *    stock body overwrites before any read (stock verifies).
 *  - `this` in the handler is read from v24 (the parameter register).
 *    v24/v25/v26 are NEVER written by D's body (verified by dexdump:
 *    they appear only in insns 0, 1 and the invoke-direct/range at old
 *    insn 319, all reads). The patch asserts this with a dest-register
 *    scan and fails loudly otherwise.
 *  - Both insertions are EVEN in size (entry 28 units, handler 52 units
 *    incl. a NOP pad), and neither changes any existing branch offset:
 *    every branch source AND target sits after the entry insertion point
 *    (abs off 4), so both shift by the same +28. The patch recomputes
 *    every branch offset anyway and ASSERTS each original offset is
 *    unchanged; any change fails the patch. Only the try block moves:
 *    start 9 -> 37, handler 627 -> 655, count stays 614.
 *  - All invoked refs already exist in classes2.dex (method_ids is full
 *    at 65536): Toast.makeText / Toast.show / Log.e (2-arg and 3-arg) /
 *    View.getContext / StringBuilder.<init>/append/toString /
 *    Throwable.toString. Zero new refs.
 *  - D runs on the UI thread (long-press is dispatched from touch
 *    handling), so Toast.show() is legal here. Every Toast is null
 *    guarded (view null -> skip; context null -> skip), so the marker
 *    cannot throw and cannot disturb the popup path.
 */
public class DiagPatch {
    static final String TAG = "NobiDiag";
    static final String ENTER = "D-enter 176071842";

    static final ImmutableMethodReference LOG_E_2 =
        new ImmutableMethodReference("Landroid/util/Log;", "e",
            java.util.Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"), "I");
    static final ImmutableMethodReference LOG_E_3 =
        new ImmutableMethodReference("Landroid/util/Log;", "e",
            java.util.Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/Throwable;"), "I");
    static final ImmutableMethodReference TOAST_MAKE_TEXT =
        new ImmutableMethodReference("Landroid/widget/Toast;", "makeText",
            java.util.Arrays.asList("Landroid/content/Context;", "Ljava/lang/CharSequence;", "I"),
            "Landroid/widget/Toast;");
    static final ImmutableMethodReference TOAST_SHOW =
        new ImmutableMethodReference("Landroid/widget/Toast;", "show",
            java.util.Collections.emptyList(), "V");
    static final ImmutableMethodReference VIEW_GET_CONTEXT =
        new ImmutableMethodReference("Landroid/view/View;", "getContext",
            java.util.Collections.emptyList(), "Landroid/content/Context;");
    static final ImmutableMethodReference SB_INIT =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "<init>",
            java.util.Collections.emptyList(), "V");
    static final ImmutableMethodReference SB_APPEND =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            java.util.Arrays.asList("Ljava/lang/String;"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_TOSTRING =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "toString",
            java.util.Collections.emptyList(), "Ljava/lang/String;");
    static final ImmutableMethodReference THROWABLE_TOSTRING =
        new ImmutableMethodReference("Ljava/lang/Throwable;", "toString",
            java.util.Collections.emptyList(), "Ljava/lang/String;");
    static final ImmutableFieldReference LQCK_M =
        new ImmutableFieldReference("Lqck;", "m",
            "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;");
    static final ImmutableTypeReference SB_TYPE =
        new ImmutableTypeReference("Ljava/lang/StringBuilder;");

    static ImmutableInstruction21c CONST_STR(int reg, String s) {
        return new ImmutableInstruction21c(Opcode.CONST_STRING, reg,
            new ImmutableStringReference(s));
    }

    /** Entry block. Branch targets are placeholders (offset 0); the caller
     *  records them as (blockIndex -> targetNewListIndex) and fixes them
     *  after splicing. Block layout (v0=this, scratch v3/v4/v5):
     *   0 CONST_STRING v4, TAG
     *   1 CONST_STRING v5, ENTER
     *   2 INVOKE_STATIC {v4,v5} Log.e
     *   3 MOVE_RESULT v4
     *   4 IGET_OBJECT v4, v0, Lqck.m
     *   5 IF_EQZ v4 -> SKIP   (index 14)
     *   6 INVOKE_VIRTUAL {v4} View.getContext
     *   7 MOVE_RESULT_OBJECT v4
     *   8 IF_EQZ v4 -> SKIP   (index 14)
     *   9 CONST_STRING v5, ENTER
     *  10 CONST_4 v3, 1
     *  11 INVOKE_STATIC {v4,v5,v3} Toast.makeText
     *  12 MOVE_RESULT_OBJECT v4
     *  13 INVOKE_VIRTUAL {v4} Toast.show
     *  14 (SKIP: falls through to original insn 2)
     *  28 units total (even). */
    static List<Instruction> entryBlock() {
        List<Instruction> b = new ArrayList<>();
        b.add(CONST_STR(4, TAG));
        b.add(CONST_STR(5, ENTER));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 4, 5, 0, 0, 0, LOG_E_2));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 4));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 0, LQCK_M));
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 4, 0)); // -> SKIP (idx 14)
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 4, 0, 0, 0, 0, VIEW_GET_CONTEXT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 4, 0)); // -> SKIP (idx 14)
        b.add(CONST_STR(5, ENTER));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 1));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 3, 4, 5, 3, 0, 0, TOAST_MAKE_TEXT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 4, 0, 0, 0, 0, TOAST_SHOW));
        int total = 0;
        for (Instruction i : b) total += i.getCodeUnits();
        if (total != 28) throw new RuntimeException("entry block is " + total + " units, expected 28");
        return b;
    }

    /** Handler block, inserted after MOVE_EXCEPTION v0 (v0=Throwable, DO NOT
     *  touch; this is in v24). Branch targets are placeholders; the caller
     *  fixes them. Block layout (scratch v3/v4/v5):
     *   0 CONST_STRING v4, TAG
     *   1 CONST_STRING v5, "ERR"
     *   2 INVOKE_STATIC {v4,v5,v0} Log.e (3-arg: full stack trace)
     *   3 MOVE_RESULT v4
     *   4 NEW_INSTANCE v4, StringBuilder
     *   5 INVOKE_DIRECT {v4} StringBuilder.<init>
     *   6 CONST_STRING v5, "ERR "
     *   7 INVOKE_VIRTUAL {v4,v5} StringBuilder.append
     *   8 MOVE_RESULT_OBJECT v4
     *   9 INVOKE_VIRTUAL {v0} Throwable.toString
     *  10 MOVE_RESULT_OBJECT v5
     *  11 INVOKE_VIRTUAL {v4,v5} StringBuilder.append
     *  12 MOVE_RESULT_OBJECT v4
     *  13 INVOKE_VIRTUAL {v4} StringBuilder.toString
     *  14 MOVE_RESULT_OBJECT v5   (v5 = "ERR <class>: <msg>")
     *  15 MOVE_OBJECT_FROM16 v4, v24   (this -> v4; IGET_OBJECT's object
     *     register is a 4-bit nibble, so v24 cannot be used directly)
     *  16 IGET_OBJECT v4, v4, Lqck.m
     *  17 IF_EQZ v4 -> SKIP  (index 25)
     *  18 INVOKE_VIRTUAL {v4} View.getContext
     *  19 MOVE_RESULT_OBJECT v4
     *  20 IF_EQZ v4 -> SKIP  (index 25)
     *  21 CONST_4 v3, 1
     *  22 INVOKE_STATIC {v4,v5,v3} Toast.makeText
     *  23 MOVE_RESULT_OBJECT v4
     *  24 INVOKE_VIRTUAL {v4} Toast.show
     *  25 NOP (pad to even; SKIP lands here, falls through to endSection)
     *  52 units total (even). */
    static List<Instruction> handlerBlock() {
        List<Instruction> b = new ArrayList<>();
        b.add(CONST_STR(4, TAG));
        b.add(CONST_STR(5, "ERR"));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 3, 4, 5, 0, 0, 0, LOG_E_3));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, 4));
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, 4, SB_TYPE));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_DIRECT, 1, 4, 0, 0, 0, 0, SB_INIT));
        b.add(CONST_STR(5, "ERR "));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 4, 5, 0, 0, 0, SB_APPEND));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 0, 0, 0, 0, 0, THROWABLE_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 5));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 4, 5, 0, 0, 0, SB_APPEND));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 4, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 5));
        b.add(new ImmutableInstruction22x(Opcode.MOVE_OBJECT_FROM16, 4, 24));
        b.add(new ImmutableInstruction22c(Opcode.IGET_OBJECT, 4, 4, LQCK_M));
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 4, 0)); // -> SKIP (idx 25)
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 4, 0, 0, 0, 0, VIEW_GET_CONTEXT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        b.add(new ImmutableInstruction21t(Opcode.IF_EQZ, 4, 0)); // -> SKIP (idx 25)
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, 3, 1));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 3, 4, 5, 3, 0, 0, TOAST_MAKE_TEXT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, 4));
        b.add(new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 1, 4, 0, 0, 0, 0, TOAST_SHOW));
        b.add(new ImmutableInstruction10x(Opcode.NOP));
        int total = 0;
        for (Instruction i : b) total += i.getCodeUnits();
        if (total != 52) throw new RuntimeException("handler block is " + total + " units, expected 52");
        return b;
    }

    /** Destination register written by ins, or -1 if it writes no register.
     *  Throws on any opcode/format not explicitly handled, so a write can
     *  never be silently missed. */
    static int destReg(Instruction ins) {
        Opcode op = ins.getOpcode();
        if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                || op == Opcode.FILL_ARRAY_DATA) {
            throw new RuntimeException("payload opcode in D: " + op);
        }
        if (ins instanceof Instruction11x) {
            if (op == Opcode.MOVE_RESULT || op == Opcode.MOVE_RESULT_OBJECT
                    || op == Opcode.MOVE_EXCEPTION) {
                return ((Instruction11x) ins).getRegisterA();
            }
            return -1; // RETURN*, THROW, MONITOR_*
        }
        if (ins instanceof Instruction11n) return ((Instruction11n) ins).getRegisterA();
        if (ins instanceof Instruction20t) return -1; // GOTO/16
        if (ins instanceof Instruction12x) return ((Instruction12x) ins).getRegisterA();
        if (ins instanceof Instruction21c) return ((Instruction21c) ins).getRegisterA();
        if (ins instanceof Instruction21s) return ((Instruction21s) ins).getRegisterA();
        if (ins instanceof Instruction21ih) return ((Instruction21ih) ins).getRegisterA();
        if (ins instanceof Instruction21lh) return ((Instruction21lh) ins).getRegisterA();
        if (ins instanceof Instruction21t) return -1; // branch: reads A
        if (ins instanceof Instruction22c) {
            if (op.name().startsWith("IPUT")) return -1; // writes a field
            return ((Instruction22c) ins).getRegisterA(); // IGET*, INSTANCE_OF, NEW_ARRAY
        }
        if (ins instanceof Instruction22x) return ((Instruction22x) ins).getRegisterA();
        if (ins instanceof Instruction22b) return ((Instruction22b) ins).getRegisterA();
        if (ins instanceof Instruction22s) return ((Instruction22s) ins).getRegisterA();
        if (ins instanceof Instruction22t) return -1; // branch
        if (ins instanceof Instruction23x) return ((Instruction23x) ins).getRegisterA();
        if (ins instanceof Instruction31c) return ((Instruction31c) ins).getRegisterA();
        if (ins instanceof Instruction31i) return ((Instruction31i) ins).getRegisterA();
        if (ins instanceof Instruction32x) return ((Instruction32x) ins).getRegisterA();
        if (ins instanceof Instruction35c) return -1; // invoke: no reg dest
        if (ins instanceof Instruction3rc) return -1; // invoke/range: no reg dest
        if (ins instanceof Instruction10x) return -1;
        if (ins instanceof Instruction10t) return -1; // GOTO
        if (ins instanceof Instruction30t) return -1; // GOTO/32
        if (ins instanceof Instruction51l) return ((Instruction51l) ins).getRegisterA();
        throw new RuntimeException("unhandled instruction format for dest check: "
            + ins.getClass().getSimpleName() + " " + op);
    }

    /** Rebuild a branch instruction with a new code offset. */
    static Instruction withOffset(Instruction ins, int newOff) {
        Opcode op = ins.getOpcode();
        if (ins instanceof Instruction10t) return new ImmutableInstruction10t(op, newOff);
        if (ins instanceof Instruction20t) return new ImmutableInstruction20t(op, newOff);
        if (ins instanceof Instruction21t) {
            return new ImmutableInstruction21t(op, ((Instruction21t) ins).getRegisterA(), newOff);
        }
        if (ins instanceof Instruction22t) {
            Instruction22t t = (Instruction22t) ins;
            return new ImmutableInstruction22t(op, t.getRegisterA(), t.getRegisterB(), newOff);
        }
        if (ins instanceof Instruction30t) return new ImmutableInstruction30t(op, newOff);
        throw new RuntimeException("not a branch instruction: " + ins.getClass().getSimpleName() + " " + op);
    }

    static void fail(String msg) { throw new RuntimeException("DiagPatch: " + msg); }

    /** Patch Lqck.D(J)V: entry marker after insn 1, handler log after
     *  insn 322. Registers stay 27. */
    static MethodImplementation patchD(MethodImplementation impl) {
        List<Instruction> orig = new ArrayList<>();
        for (Instruction i : impl.getInstructions()) orig.add(i);
        if (orig.size() != 325) fail("D insns=" + orig.size() + ", expected 325");
        if (impl.getRegisterCount() != 27) fail("D registers=" + impl.getRegisterCount() + ", expected 27");
        if (impl.getTryBlocks().size() != 1) fail("D tryBlocks=" + impl.getTryBlocks().size() + ", expected 1");
        TryBlock tb = impl.getTryBlocks().get(0);
        if (tb.getStartCodeAddress() != 9 || tb.getCodeUnitCount() != 614)
            fail("D try start=" + tb.getStartCodeAddress() + " count=" + tb.getCodeUnitCount()
                + ", expected 9/614");
        if (tb.getExceptionHandlers().size() != 1) fail("D handlers=" + tb.getExceptionHandlers().size());
        ExceptionHandler h = (ExceptionHandler) tb.getExceptionHandlers().get(0);
        if (h.getExceptionType() != null || h.getHandlerCodeAddress() != 627)
            fail("D handler type=" + h.getExceptionType() + " addr=" + h.getHandlerCodeAddress()
                + ", expected null/627");
        // boundary shapes
        if (orig.get(0).getOpcode() != Opcode.MOVE_OBJECT_FROM16) fail("insn 0 is not MOVE_OBJECT_FROM16");
        if (orig.get(1).getOpcode() != Opcode.MOVE_WIDE_FROM16) fail("insn 1 is not MOVE_WIDE_FROM16");
        if (!(orig.get(2) instanceof Instruction21c)
                || orig.get(2).getOpcode() != Opcode.CONST_STRING
                || !((Instruction21c) orig.get(2)).getReference().toString()
                    .contains("TouchActionBundle.showPopup"))
            fail("insn 2 is not CONST_STRING TouchActionBundle.showPopup: " + orig.get(2));
        if (orig.get(3).getOpcode() != Opcode.INVOKE_STATIC) fail("insn 3 is not INVOKE_STATIC");
        if (orig.get(320).getOpcode() != Opcode.INVOKE_STATIC) fail("insn 320 is not INVOKE_STATIC");
        if (orig.get(321).getOpcode() != Opcode.RETURN_VOID) fail("insn 321 is not RETURN_VOID");
        if (orig.get(322).getOpcode() != Opcode.MOVE_EXCEPTION) fail("insn 322 is not MOVE_EXCEPTION");
        if (orig.get(323).getOpcode() != Opcode.INVOKE_STATIC) fail("insn 323 is not INVOKE_STATIC");
        if (orig.get(324).getOpcode() != Opcode.THROW) fail("insn 324 is not THROW");
        // no payload opcodes, and no instruction writes v24/v25/v26
        for (int i = 0; i < orig.size(); i++) {
            Instruction ins = orig.get(i);
            Opcode op = ins.getOpcode();
            if (op == Opcode.PACKED_SWITCH || op == Opcode.SPARSE_SWITCH
                    || op == Opcode.FILL_ARRAY_DATA)
                fail("payload opcode at insn " + i + ": " + op);
            int d = destReg(ins);
            if (d == 24 || d == 25 || d == 26)
                fail("insn " + i + " writes parameter register v" + d + ": " + ins);
        }
        // old absolute offsets
        int[] oldAbs = new int[orig.size()];
        int off = 0;
        for (int i = 0; i < orig.size(); i++) { oldAbs[i] = off; off += orig.get(i).getCodeUnits(); }
        int oldTotal = off;
        Map<Integer, Integer> absToOld = new HashMap<>();
        for (int i = 0; i < orig.size(); i++) absToOld.put(oldAbs[i], i);

        List<Instruction> entry = entryBlock();
        List<Instruction> handler = handlerBlock();
        int entryLen = 28, handlerLen = 52;

        // splice: [0,1] + entry + [2..321] + [322] + handler + [323,324]
        List<Instruction> all = new ArrayList<>();
        int[] oldToNew = new int[orig.size()];
        java.util.Arrays.fill(oldToNew, -1);
        all.add(orig.get(0)); oldToNew[0] = 0;
        all.add(orig.get(1)); oldToNew[1] = 1;
        int entryStart = all.size();
        all.addAll(entry);
        int skip1NewIdx = all.size(); // original insn 2 lands here
        for (int i = 2; i <= 321; i++) { oldToNew[i] = all.size(); all.add(orig.get(i)); }
        oldToNew[322] = all.size(); all.add(orig.get(322));
        int handlerStart = all.size();
        all.addAll(handler);
        int skip2NewIdx = all.size(); // original insn 323 lands here
        for (int i = 323; i <= 324; i++) { oldToNew[i] = all.size(); all.add(orig.get(i)); }

        // new absolute offsets
        int[] newAbs = new int[all.size()];
        off = 0;
        for (int i = 0; i < all.size(); i++) { newAbs[i] = off; off += all.get(i).getCodeUnits(); }
        int newTotal = off;

        // fix my inserted branches: entry insns 5,8 -> skip1NewIdx; handler insns 16,19 -> skip2NewIdx
        int[][] myBranches = {
            {entryStart + 5, skip1NewIdx},
            {entryStart + 8, skip1NewIdx},
            {handlerStart + 17, skip2NewIdx},
            {handlerStart + 20, skip2NewIdx},
        };
        for (int[] mb : myBranches) {
            int idx = mb[0], tgt = mb[1];
            Instruction cur = all.get(idx);
            if (!(cur instanceof OffsetInstruction)) fail("my branch at " + idx + " is not an offset insn");
            int newOff = newAbs[tgt] - newAbs[idx];
            all.set(idx, withOffset(cur, newOff));
        }

        // recompute every ORIGINAL branch offset; assert unchanged
        for (int i = 0; i < orig.size(); i++) {
            Instruction ins = orig.get(i);
            if (!(ins instanceof OffsetInstruction)) continue;
            int oldOff = ((OffsetInstruction) ins).getCodeOffset();
            int oldTargetAbs = oldAbs[i] + oldOff;
            int newTargetAbs;
            int newIdx = oldToNew[i];
            if (oldTargetAbs == oldTotal) {
                newTargetAbs = newTotal;
            } else {
                Integer tj = absToOld.get(oldTargetAbs);
                if (tj == null) fail("branch at old insn " + i + " targets unknown abs " + oldTargetAbs);
                newTargetAbs = newAbs[oldToNew[tj]];
            }
            int newOff = newTargetAbs - newAbs[newIdx];
            if (newOff != oldOff)
                fail("original branch at old insn " + i + " offset would change "
                    + oldOff + " -> " + newOff + " (insertion moved a target)");
            // offset unchanged: keep the original instruction object as-is
        }

        // rebuild the try block: start 9 -> 9+entryLen, handler 627 -> 627+entryLen, count same
        List<ImmutableTryBlock> newTries = new ArrayList<>();
        newTries.add(new ImmutableTryBlock(9 + entryLen, 614,
            java.util.Collections.singletonList(
                new ImmutableExceptionHandler(null, 627 + entryLen))));
        System.out.println("Lqck.D: entry marker +" + entryLen + " units after insn 1; "
            + "handler log +" + handlerLen + " units after insn 322; "
            + "try 9/614/627 -> " + (9 + entryLen) + "/614/" + (627 + entryLen)
            + "; registers stay 27; total " + oldTotal + " -> " + newTotal + " units");
        return new ImmutableMethodImplementation(
            impl.getRegisterCount(), all, newTries, impl.getDebugItems());
    }
}
