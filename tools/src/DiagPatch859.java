// DiagPatch859: the 176071859 diagnostic patcher — Toast/Log instrumentation
// ONLY in Lpud.m (def-parse), NO behaviour change. Renamed from DiagPatch.java
// (the 176071837 Lqck.D diagnostic) to avoid clobbering the older tool.
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.rewriter.*;
import java.io.File;
import java.util.*;

/**
 * DIAGNOSTIC variant (code 176071859) of NobiBoard 176071858.
 * Instrumentation ONLY — NO behaviour change. Adds Toasts + Log.e (tag NobiDiag)
 * inside Lpud.m to find out WHY the def-parse customization hook does not work.
 * Sam confirmed: he DID force-stop after customizing, it did NOT work.
 * Key A, centre slot = a laughing emoji. Fresh install + force-stop.
 *
 * GROUND TRUTH CORRECTION (verified from fd2.xml + Phase3Tables):
 * The A key's token[3] (the fingerprint index) is 0x1F496 (U+1F496), NOT
 * 0x1F60D. fd2.xml A entry (Fix11 left-edge permutation):
 * [U+200B,U+1F495,U+200B,U+1F496,U+1F60D,U+1F618,U+200B,U+1F970,U+200B]
 * token[3] = 0x1F496. 0x1F60D is token[4] (the displayed centre). The parent
 * brief's "0x1F60D" is WRONG for the fingerprint; this patcher uses 0x1F496
 * (== Phase3Tables.LOOKUP_CP[0], the value the hook's own chain checks).
 *
 * INSTRUMENTATION POINTS (858 patched Lpud.m, 312 insns, 16 regs):
 *   P0 — insert at orig idx 0 (method entry, before the hook).
 *        First-call Toast (once per process, static flag Lpud->diagFirst:Z)
 *        "DEF 859: Lpud.m runs" + Log.e the same + centre code point.
 *        Also initialises v2 = -1 (centre-CP sentinel) so v2 is
 *        definitely-assigned at GUARD (the fingerprint overwrites it when
 *        length == 9).
 *   P1 — insert at orig idx 146 (after prefs read, v7 non-null).
 *        A-key only (v2 == 0x1F496): Toast + Log "A: prefs=<value>".
 *   P2 — insert at orig idx 245 (finalize, SUCCESS path only).
 *        A-key only: Toast + Log "A: SUB OK". Falls through to clean-up.
 *        (P2 is on the SUCCESS fall-through path ONLY; GUARD branches skip it.)
 *   P3 — APPENDED at the end of the method (after RETURN_VOID).
 *        A-key only: Toast + Log "A: GUARD", then GOTO_16 to clean-up.
 *        The 11 original GUARD branches are retargeted to P3's start.
 *        (P3 is on the GUARD path ONLY; SUCCESS never reaches it.)
 *   Log.e (tag "NobiDiag") at every point above. Toasts use the Application
 *   Context (from ActivityThread.currentApplication()), null-guarded.
 *
 * BRANCH RETARGETING: inserting P0 (at 0), P1 (at 146), P2 (at 245) shifts
 * code units. All original branches keep their offsets EXCEPT the 11 GUARD
 * branches (which are retargeted to P3's start). Verified by construction:
 * every non-GUARD original branch has source and target in the same region
 * ([0,146) / [146,245) / [245,312)), so both shift equally and the offset
 * is unchanged. P3's GOTO_16 targets the clean-up (orig 245).
 *
 * REGISTER DISCIPLINE: instrumentation uses the hook's own scratch (v0, v1,
 * v2, v5, v6, v7, v8, v11), each holding ONE kind. v2 is INT throughout.
 * v5/v6/v7/v8/v11 are REF in the instrumentation and in the hook; the
 * clean-up resets v3/v6/v7/v8/v11 to INT(0) before the original code, which
 * uses them as INT. No new register is added (still 16 regs).
 *
 * NEW REFS: only 2 — Toast.makeText + Toast.show (method_ids 57,517 -> 57,519).
 * Log.e, StringBuilder (init/append/toString), ActivityThread.currentApplication
 * are already present in the 858 classes.dex (verified by RefExists).
 * NEW FIELD: 1 — Lpud->diagFirst:Z (private static boolean, defaults false).
 * field_ids 38,006 -> 38,007.
 */
public class DiagPatch859 {
    // ---------- registers (Lpud.m: 16 regs, params v12..v15) ----------
    static final int R_SLOT = 0;    // INT temp
    static final int R_TMP = 1;     // INT temp
    static final int R_CP = 2;      // INT centre code point
    static final int R_PREFKEY = 3; // (hook) REF pref key — NOT used by diag
    static final int R_COL = 4;     // (hook) INT colId — NOT used by diag
    static final int R_CTX = 5;     // REF Context (Application)
    static final int R_PREFS = 6;   // REF (hook: SharedPreferences; diag: StringBuilder/Toast)
    static final int R_PREFSTR = 7; // REF (hook: prefs string; diag: Log message)
    static final int R_PARTS = 8;   // REF (hook: parts array; diag: Log tag / message)
    static final int R_CLONE_I = 9; // (hook) — NOT used by diag
    static final int R_CLONE_O = 10;// (hook) — NOT used by diag
    static final int R_PART = 11;   // REF const strings
    static final int R_THIS = 12;
    static final int R_ARR_I = 13;
    static final int R_ARR_U = 14;
    static final int R_ARR_O = 15;

    static final int CP_A = 0x1F496; // A-key token[3] (Fix11 fd2.xml) — NOT 0x1F60D
    static final String TAG = "NobiDiag";

    // ---------- method refs ----------
    static final ImmutableMethodReference TOAST_MAKE =
        new ImmutableMethodReference("Landroid/widget/Toast;", "makeText",
            Arrays.asList("Landroid/content/Context;", "Ljava/lang/CharSequence;", "I"),
            "Landroid/widget/Toast;");
    static final ImmutableMethodReference TOAST_SHOW =
        new ImmutableMethodReference("Landroid/widget/Toast;", "show",
            Collections.<String>emptyList(), "V");
    static final ImmutableMethodReference LOG_E =
        new ImmutableMethodReference("Landroid/util/Log;", "e",
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"), "I");
    static final ImmutableMethodReference SB_INIT =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "<init>",
            Collections.<String>emptyList(), "V");
    static final ImmutableMethodReference SB_APP_STR =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("Ljava/lang/String;"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_APP_INT =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("I"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_TOSTRING =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "toString",
            Collections.<String>emptyList(), "Ljava/lang/String;");
    static final ImmutableMethodReference ACTIVITY_THREAD_APP =
        new ImmutableMethodReference("Landroid/app/ActivityThread;", "currentApplication",
            Collections.<String>emptyList(), "Landroid/app/Application;");

    static final ImmutableFieldReference DIAG_FIRST =
        new ImmutableFieldReference("Lpud;", "diagFirst", "Z");

    // ---------- tiny label-based block builder (from DefParsePatch) ----------
    static class B {
        List<Instruction> code = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();
        static class Fix { int idx; String target; Fix(int i, String t) { idx = i; target = t; } }
        List<Fix> fixups = new ArrayList<>();
        void label(String n) { if (labels.put(n, code.size()) != null) throw new RuntimeException("dup label " + n); }
        void add(Instruction i) { code.add(i); }
        void br21t(Opcode op, int reg, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction21t(op, reg, 0)); }
        void br22t(Opcode op, int ra, int rb, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction22t(op, ra, rb, 0)); }
        void br10t(Opcode op, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction10t(op, 0)); }
        void br20t(Opcode op, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction20t(op, 0)); }
        int totalUnits() { int t = 0; for (Instruction i : code) t += i.getCodeUnits(); return t; }
        void padEven() { if (totalUnits() % 2 != 0) code.add(new ImmutableInstruction10x(Opcode.NOP)); }
        void fixup(int base) {
            padEven();
            List<Integer> addr = new ArrayList<>();
            int a = base;
            for (Instruction i : code) { addr.add(a); a += i.getCodeUnits(); }
            int total = a - base;
            for (Fix f : fixups) {
                int targetAddr;
                Integer ti = labels.get(f.target);
                if (ti == null) throw new RuntimeException("unknown label " + f.target);
                targetAddr = (ti == code.size()) ? base + total : addr.get(ti);
                int insnAddr = addr.get(f.idx);
                int off = targetAddr - insnAddr;
                if (off == 0) throw new RuntimeException("zero branch offset at block idx " + f.idx + " -> " + f.target + " (self-loop!)");
                Instruction old = code.get(f.idx);
                if (old instanceof Instruction10t && (off < -128 || off > 127))
                    throw new RuntimeException("10t branch out of range at block idx " + f.idx + " -> " + f.target + " off=" + off);
                if ((old instanceof Instruction21t || old instanceof Instruction22t || old instanceof Instruction20t) && (off < -32768 || off > 32767))
                    throw new RuntimeException("16-bit branch out of range at block idx " + f.idx + " -> " + f.target + " off=" + off);
                if (old instanceof Instruction21t) { Instruction21t o = (Instruction21t) old; code.set(f.idx, new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), off)); }
                else if (old instanceof Instruction22t) { Instruction22t o = (Instruction22t) old; code.set(f.idx, new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(), o.getRegisterB(), off)); }
                else if (old instanceof Instruction10t) { Instruction10t o = (Instruction10t) old; code.set(f.idx, new ImmutableInstruction10t(o.getOpcode(), off)); }
                else if (old instanceof Instruction20t) { Instruction20t o = (Instruction20t) old; code.set(f.idx, new ImmutableInstruction20t(o.getOpcode(), off)); }
                else throw new RuntimeException("bad fixup insn " + old.getOpcode());
            }
            if (totalUnits() % 2 != 0) throw new RuntimeException("block units still odd: " + totalUnits());
        }
    }

    static ImmutableInstruction35c inv35(Opcode op, int count, int a, int b, int c, int d, int e, MethodReference ref) {
        return new ImmutableInstruction35c(op, count, a, b, c, d, e, ref);
    }

    /** Toast.show() on the Toast object in v6. */
    static void emitShow(B b) {
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, TOAST_SHOW));
    }

    /** Toast.makeText(v5 ctx, v11 msg, v1 duration) -> v6, then show(). */
    static void emitToast(B b) {
        b.add(inv35(Opcode.INVOKE_STATIC, 3, R_CTX, R_PART, R_TMP, 0, 0, TOAST_MAKE));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        emitShow(b);
    }

    /** Log.e(tag, msgReg). tagReg holds the tag string; msgReg holds the message.
     *  tagReg MUST differ from msgReg (loading the tag would clobber the message). */
    static void emitLog(B b, int msgReg, int tagReg) {
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, tagReg,
            new ImmutableStringReference(TAG)));
        b.add(inv35(Opcode.INVOKE_STATIC, 2, tagReg, msgReg, 0, 0, 0, LOG_E));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT, R_TMP));
    }

    /** Build StringBuilder message into v7 (R_PREFSTR) from a constant prefix + an int (v2). */
    static void emitSbMsgInt(B b, String prefix) {
        // NEW_INSTANCE is format 21c (one register + type), NOT 22c.
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS,
            new ImmutableTypeReference("Ljava/lang/StringBuilder;")));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(prefix)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_CP, 0, 0, 0, SB_APP_INT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFSTR));
    }

    /** Build StringBuilder message into v8 (R_PARTS) from a constant prefix + a String (v7). */
    static void emitSbMsgStr(B b, String prefix) {
        // NEW_INSTANCE is format 21c (one register + type), NOT 22c.
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS,
            new ImmutableTypeReference("Ljava/lang/StringBuilder;")));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(prefix)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PREFSTR, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
    }

    /** P0: method entry — first-call Toast + Log (with centre CP), init v2=-1. */
    static B buildP0() {
        B b = new B();
        // v2 = -1 sentinel (definitely-assigned at GUARD; fingerprint overwrites when len==9)
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_CP, -1));
        // first-call flag check
        b.add(new ImmutableInstruction21c(Opcode.SGET_BOOLEAN, R_SLOT, DIAG_FIRST));
        b.br21t(Opcode.IF_NEZ, R_SLOT, "P0_SKIP");
        // set flag
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 1));
        b.add(new ImmutableInstruction21c(Opcode.SPUT_BOOLEAN, R_SLOT, DIAG_FIRST));
        // get Context for the Toast
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, ACTIVITY_THREAD_APP));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_CTX));
        b.br21t(Opcode.IF_EQZ, R_CTX, "P0_LOG");
        // Toast "DEF 859: Lpud.m runs" (LENGTH_LONG = 1)
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("DEF 859: Lpud.m runs")));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
        emitToast(b);
        // P0_LOG: compute centre CP (duplicate fingerprint's AGET, guarded by len==9)
        b.label("P0_LOG");
        b.add(new ImmutableInstruction12x(Opcode.ARRAY_LENGTH, R_TMP, R_ARR_I));
        b.add(new ImmutableInstruction21s(Opcode.CONST_16, R_SLOT, 9));
        b.br22t(Opcode.IF_NE, R_TMP, R_SLOT, "P0_CP_DONE");
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 3));
        b.add(new ImmutableInstruction23x(Opcode.AGET, R_CP, R_ARR_I, R_SLOT));
        b.label("P0_CP_DONE");
        // Log "Lpud.m runs cp=<v2>"
        emitSbMsgInt(b, "Lpud.m runs cp=");
        emitLog(b, R_PREFSTR, R_PARTS);
        b.label("P0_SKIP");
        return b;
    }

    /** P1: after prefs read (orig 146) — A-key only: Toast + Log "A: prefs=<value>". */
    static B buildP1() {
        B b = new B();
        // A-check: v2 == 0x1F496 ?
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, CP_A));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "P1_SKIP");
        // build "A: prefs=" + v7 (prefs string) into v8
        emitSbMsgStr(b, "A: prefs=");
        // Toast it (ctx=v5, msg=v8, LONG)
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
        b.add(inv35(Opcode.INVOKE_STATIC, 3, R_CTX, R_PARTS, R_TMP, 0, 0, TOAST_MAKE));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        emitShow(b);
        // Log.e("NobiDiag", message in v8)
        emitLog(b, R_PARTS, R_PART);
        b.label("P1_SKIP");
        return b;
    }

    /** P2: finalize (orig 245, SUCCESS only) — A-key only: Toast + Log "A: SUB OK". */
    static B buildP2() {
        B b = new B();
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, CP_A));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "P2_SKIP");
        // Toast "A: SUB OK"
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("A: SUB OK")));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
        emitToast(b);
        // Log.e("NobiDiag", "A: SUB OK") — message still in v11
        emitLog(b, R_PART, R_PARTS);
        b.label("P2_SKIP");
        return b;
    }

    /** P3: appended at method end — A-key only: Toast + Log "A: GUARD", then GOTO clean-up. */
    static B buildP3() {
        B b = new B();
        b.add(new ImmutableInstruction31i(Opcode.CONST, R_SLOT, CP_A));
        b.br22t(Opcode.IF_NE, R_CP, R_SLOT, "P3_SKIP");
        // message "A: GUARD" into v11
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("A: GUARD")));
        // get Context (v5 may not be assigned on the fingerprint-fail path)
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, ACTIVITY_THREAD_APP));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_CTX));
        b.br21t(Opcode.IF_EQZ, R_CTX, "P3_LOG");
        // Toast "A: GUARD"
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 1));
        emitToast(b);
        // Log.e("NobiDiag", "A: GUARD")
        b.label("P3_LOG");
        emitLog(b, R_PART, R_PARTS);
        b.label("P3_SKIP");
        return b;
    }

    /** Entry gate: report. */
    static String methodKey(MethodReference r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getDefiningClass()).append("->").append(r.getName()).append("(");
        for (CharSequence p : r.getParameterTypes()) sb.append(p);
        sb.append(")").append(r.getReturnType());
        return sb.toString();
    }

    static int countIds(File f, int off) throws Exception {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
        try {
            raf.seek(off);
            int b0 = raf.read(); int b1 = raf.read(); int b2 = raf.read(); int b3 = raf.read();
            return (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        } finally { raf.close(); }
    }

    public static void main(String[] args) throws Exception {
        File inFile = new File(args[0]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(inFile, Opcodes.forApi(35));
        int inMethodIds = countIds(inFile, 88);
        int inFieldIds = countIds(inFile, 80);
        System.out.println("input method_ids_size=" + inMethodIds + " field_ids_size=" + inFieldIds);
        // Load Lpud.m (858 patched: 312 insns, 16 regs). Verify shape + fingerprints.
        List<Instruction> orig = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lpud;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("m") && m.getParameters().size() == 3) {
                    MethodImplementation impl = m.getImplementation();
                    orig = new ArrayList<>();
                    for (Instruction i : impl.getInstructions()) orig.add(i);
                    if (orig.size() != 312 || impl.getRegisterCount() != 16) {
                        System.out.println("Lpud.m: unexpected shape ins=" + orig.size() + " regs=" + impl.getRegisterCount() + " (expected 312/16)");
                        return;
                    }
                    if (!impl.getTryBlocks().isEmpty()) { System.out.println("Lpud.m: unexpected try blocks"); return; }
                    // fingerprints: insn0 ARRAY_LENGTH, insn134 currentApplication,
                    // insn245 CONST_4 v3,0 (clean-up start), insn311 RETURN_VOID
                    if (orig.get(0).getOpcode() != Opcode.ARRAY_LENGTH) { System.out.println("Lpud.m: insn0 not ARRAY_LENGTH"); return; }
                    if (orig.get(134).getOpcode() != Opcode.INVOKE_STATIC) { System.out.println("Lpud.m: insn134 not INVOKE_STATIC"); return; }
                    Instruction i245 = orig.get(245);
                    if (i245.getOpcode() != Opcode.CONST_4 || !(i245 instanceof Instruction11n)
                            || ((Instruction11n) i245).getRegisterA() != 3 || ((Instruction11n) i245).getNarrowLiteral() != 0) {
                        System.out.println("Lpud.m: insn245 not CONST_4 v3,0 (got " + i245 + ")"); return;
                    }
                    if (orig.get(311).getOpcode() != Opcode.RETURN_VOID) { System.out.println("Lpud.m: insn311 not RETURN_VOID"); return; }
                    // A-chain entry: insn5 CONST 0x1F496
                    Instruction i5 = orig.get(5);
                    if (i5.getOpcode() != Opcode.CONST || !(i5 instanceof Instruction31i)
                            || ((Instruction31i) i5).getNarrowLiteral() != CP_A) {
                        System.out.println("Lpud.m: insn5 not CONST 0x1F496 (got " + i5 + ")"); return;
                    }
                    System.out.println("Lpud.m: shape + fingerprints OK (312 insns, 16 regs, A CP=0x1F496)");
                }
            }
        }
        if (orig == null) { System.out.println("Lpud.m not found"); return; }
        // Original code-unit addresses + branch target original indices.
        int n = orig.size();
        int[] origAddr = new int[n];
        { int a = 0; for (int i = 0; i < n; i++) { origAddr[i] = a; a += orig.get(i).getCodeUnits(); } }
        int origTotal = origAddr[n - 1] + orig.get(n - 1).getCodeUnits();
        Map<Integer, Integer> addrToIdx = new HashMap<>();
        for (int i = 0; i < n; i++) addrToIdx.put(origAddr[i], i);
        int[] origTarget = new int[n];
        Arrays.fill(origTarget, -1);
        for (int i = 0; i < n; i++) {
            Instruction ins = orig.get(i);
            if (ins instanceof OffsetInstruction) {
                int tgt = origAddr[i] + ((OffsetInstruction) ins).getCodeOffset();
                Integer ti = addrToIdx.get(tgt);
                if (ti == null) { System.out.println("Lpud.m: branch at orig " + i + " targets invalid addr " + tgt); return; }
                origTarget[i] = ti;
            }
        }
        // Count GUARD branches (target orig 245).
        List<Integer> guardSrc = new ArrayList<>();
        for (int i = 0; i < n; i++) if (origTarget[i] == 245) guardSrc.add(i);
        System.out.println("Lpud.m: GUARD branches (target orig 245): " + guardSrc);
        if (guardSrc.size() != 11) { System.out.println("Lpud.m: expected 11 GUARD branches, got " + guardSrc.size()); return; }
        // Build blocks (fixup not yet called — need bases first).
        B p0 = buildP0(); B p1 = buildP1(); B p2 = buildP2(); B p3 = buildP3();
        // Pre-pad to even so sizes are final before base computation.
        p0.padEven(); p1.padEven(); p2.padEven(); p3.padEven();
        int s0 = p0.totalUnits(), s1 = p1.totalUnits(), s2 = p2.totalUnits(), s3 = p3.totalUnits();
        System.out.println("block sizes (units): P0=" + s0 + " P1=" + s1 + " P2=" + s2 + " P3=" + s3
            + " (insns P0=" + p0.code.size() + " P1=" + p1.code.size() + " P2=" + p2.code.size() + " P3=" + p3.code.size() + ")");
        // Bases (code-unit address where each block starts in the final method):
        // P0 at 0; P1 before orig146; P2 before orig245; P3 appended after orig311.
        int base0 = 0;
        int base1 = origAddr[146] + s0;
        int base2 = origAddr[245] + s0 + s1;
        int base3 = origTotal + s0 + s1 + s2;
        int cleanupAddr = origAddr[245] + s0 + s1 + s2; // orig245 final address (= P3 end target)
        System.out.println("bases: P0=" + base0 + " P1=" + base1 + " P2=" + base2 + " P3=" + base3 + " cleanup=" + cleanupAddr + " origTotal=" + origTotal);
        p0.fixup(base0); p1.fixup(base1); p2.fixup(base2); p3.fixup(base3);
        // P3's trailing GOTO_16 -> clean-up (appended after fixup; 2 units, keeps even).
        int gotoIdxInP3 = p3.code.size();
        p3.add(new ImmutableInstruction20t(Opcode.GOTO_16, 0));
        int gotoAddr = base3 + s3; // P3 size after fixup == s3 (padEven before fixup; fixup adds nothing)
        int gotoOff = cleanupAddr - gotoAddr;
        if (gotoOff < -32768 || gotoOff > 32767) { System.out.println("P3 GOTO_16 out of range: " + gotoOff); return; }
        p3.code.set(gotoIdxInP3, new ImmutableInstruction20t(Opcode.GOTO_16, gotoOff));
        System.out.println("P3 GOTO_16 at addr " + gotoAddr + " -> cleanup " + cleanupAddr + " off=" + gotoOff);
        // Assemble the final instruction list.
        List<Instruction> all = new ArrayList<>();
        // Map: original index -> final index (for retargeting original branches).
        int[] origToFinal = new int[n];
        // P0 + orig 0..145
        all.addAll(p0.code);
        for (int i = 0; i < 146; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // P1 + orig 146..244
        all.addAll(p1.code);
        for (int i = 146; i < 245; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // P2 + orig 245..311
        all.addAll(p2.code);
        for (int i = 245; i < n; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // P3 appended after orig 311 (RETURN_VOID) — reachable ONLY via the
        // retargeted GUARD branches; the SUCCESS fall-through (P2 -> orig 245)
        // never reaches it. (Layout B: matches base3 = origTotal + s0 + s1 + s2
        // and cleanupAddr = origAddr[245] + s0 + s1 + s2.)
        int p3StartFinal = all.size(); // P3's first instruction final index
        all.addAll(p3.code);
        // Final addresses.
        int m = all.size();
        int[] finAddr = new int[m];
        { int a = 0; for (int i = 0; i < m; i++) { finAddr[i] = a; a += all.get(i).getCodeUnits(); } }
        int finTotal = finAddr[m - 1] + all.get(m - 1).getCodeUnits();
        System.out.println("final method: " + m + " insns, " + finTotal + " units (orig 312 insns, " + origTotal + " units)");
        // Sanity: P3 start address must equal base3, clean-up address must equal cleanupAddr.
        if (finAddr[p3StartFinal] != base3) { System.out.println("P3 start addr mismatch: " + finAddr[p3StartFinal] + " != " + base3); return; }
        if (finAddr[origToFinal[245]] != cleanupAddr) { System.out.println("cleanup addr mismatch: " + finAddr[origToFinal[245]] + " != " + cleanupAddr); return; }
        // Retarget the 11 original GUARD branches to P3's start (NOT the clean-up).
        // (All other original branches keep their offsets: source and target are in
        // the same region, so both shifted equally. Verified below.)
        for (int s : guardSrc) {
            int fi = origToFinal[s];
            Instruction ins = all.get(fi);
            int newOff = finAddr[p3StartFinal] - finAddr[fi];
            if (ins instanceof Instruction21t) { Instruction21t o = (Instruction21t) ins; all.set(fi, new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), newOff)); }
            else if (ins instanceof Instruction22t) { Instruction22t o = (Instruction22t) ins; all.set(fi, new ImmutableInstruction22t(o.getOpcode(), o.getRegisterA(), o.getRegisterB(), newOff)); }
            else { System.out.println("GUARD branch at orig " + s + " is not 21t/22t: " + ins.getOpcode()); return; }
            if (newOff < -32768 || newOff > 32767) { System.out.println("GUARD branch at orig " + s + " out of range: " + newOff); return; }
        }
        System.out.println("retargeted " + guardSrc.size() + " GUARD branches to P3 start (addr " + finAddr[p3StartFinal] + ")");
        // Verify every other original branch's offset is UNCHANGED.
        for (int i = 0; i < n; i++) {
            if (!(orig.get(i) instanceof OffsetInstruction)) continue;
            if (origTarget[i] == 245) continue; // retargeted above (intentional)
            int fi = origToFinal[i];
            Instruction ins = all.get(fi);
            int oldOff = ((OffsetInstruction) orig.get(i)).getCodeOffset();
            int newOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                System.out.println("ORIGINAL branch offset CHANGED at orig " + i + " old=" + oldOff + " new=" + newOff + " — abort");
                return;
            }
        }
        System.out.println("all non-GUARD original branch offsets unchanged (verified)");
        // Build the new method implementation (same 16 regs, no tries, same debug items).
        MethodImplementation newImpl = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lpud;")) continue;
            for (Method mm : cd.getMethods()) {
                if (mm.getName().equals("m") && mm.getParameters().size() == 3) {
                    newImpl = new ImmutableMethodImplementation(mm.getImplementation().getRegisterCount(),
                        all, mm.getImplementation().getTryBlocks(), mm.getImplementation().getDebugItems());
                }
            }
        }
        if (newImpl == null) { System.out.println("failed to build new impl"); return; }
        final MethodImplementation finImpl = newImpl;
        // Module: rewrite Lpud.m + add the static field Lpud->diagFirst:Z.
        Module mod = new Module(finImpl);
        DexRewriter rw = new DexRewriter(mod);
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("instrumented classes.dex written to " + args[1]);
        int outMethodIds = countIds(new File(args[1]), 88);
        int outFieldIds = countIds(new File(args[1]), 80);
        System.out.println("post-check: method_ids_size=" + outMethodIds + " (input was " + inMethodIds + ", expect +2 = Toast.makeText + Toast.show)");
        System.out.println("post-check: field_ids_size=" + outFieldIds + " (input was " + inFieldIds + ", expect +1 = diagFirst)");
        if (outMethodIds != inMethodIds + 2) throw new RuntimeException("method_ids did not grow by exactly 2 (got " + outMethodIds + ")");
        if (outFieldIds != inFieldIds + 1) throw new RuntimeException("field_ids did not grow by exactly 1 (got " + outFieldIds + ")");
    }

    static class Module extends RewriterModule {
        final MethodImplementation finImpl;
        Module(MethodImplementation fi) { finImpl = fi; }
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    if (method.getDefiningClass().equals("Lpud;")
                            && method.getName().equals("m") && method.getParameters().size() == 3) {
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, finImpl);
                    }
                    return def.rewrite(method);
                }
            };
        }
        @Override
        public Rewriter<ClassDef> getClassDefRewriter(final Rewriters rewriters) {
            final Rewriter<ClassDef> def = super.getClassDefRewriter(rewriters);
            return new Rewriter<ClassDef>() {
                public ClassDef rewrite(ClassDef classDef) {
                    ClassDef rw = def.rewrite(classDef); // applies method rewriter first
                    if (!rw.getType().equals("Lpud;")) return rw;
                    // add static boolean field diagFirst (private static, defaults false)
                    List<Field> fields = new ArrayList<>();
                    for (Field f : rw.getFields()) fields.add(f);
                    fields.add(new ImmutableField("Lpud;", "diagFirst", "Z",
                        0x2 | 0x8, null, null, null));
                    return new ImmutableClassDef(rw.getType(), rw.getAccessFlags(),
                        rw.getSuperclass(), rw.getInterfaces(), rw.getSourceFile(),
                        rw.getAnnotations(), fields, rw.getMethods());
                }
            };
        }
    }
}
