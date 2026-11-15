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
 * CONTEXT FIX 2 (code 176071861) for NobiBoard def-parse customization.
 * Base = shipped 176071860.
 *
 * PROVEN DEFECTS (diagnosis-860, BranchDump on shipped 860 Lpud.m, 498 insns):
 *   (1) Block L (the last_run write, insns 20-42) only runs on the FIRST
 *       Lpud.m call per process: insn 16 IF_NEZ -> idx 70 skips insns 17-69
 *       (Block L AND the P0 Toast/Log) on every call after the first.
 *   (2) The Context is null on the first Lpud.m call (deduced: Lpud.m IS
 *       called after force-stop + keyboard open; if v5 were non-null on the
 *       first call, last_run would be written; it is NOT in nobi_diag).
 *       Def-parse runs on a background executor thread very early in process
 *       startup, so the first call lands before Lesb.onCreate() /
 *       ActivityThread.currentApplication() have set a Context. The same null
 *       Context makes the substitution hook go to GUARD (defaults).
 *   (3) Structural flaw: the diagnostic cannot record a null Context —
 *       writing SharedPreferences requires a Context.
 *
 * FIX (a) — Move Block L OUTSIDE the diagFirst gate. The last_run write
 *   runs on EVERY Lpud.m call (retried when the Context becomes available).
 *   The P0 Toast stays gated by diagFirst (once per process, no spam).
 *   New Lpud.m head (494 insns), assembled from 860's verbatim insns plus
 *   ONE fresh Block L' (same code as 860's Block L, built by buildL()):
 *     Seg A: 860 insns 0-14   (Block E + CONST_4 v2,-1 sentinel)   [15]
 *     Seg B: fresh Block L' (every-call last_run write)            [24]
 *     Seg C: 860 insns 15-18 (flag check/set) + 47-69 (P0 Toast/Log)[27]
 *     Seg D: 860 insns 70-497 (NOP + fingerprint + rest, verbatim) [428]
 *   Old insns 19-42 (old Block L) and 43-46 (4 NOPs) are DROPPED.
 *   The ONLY branch retargeted is insn 16 (IF_NEZ -> old 70) which now
 *   targets Seg D start (new idx 66). All other original branch offsets
 *   are verified UNCHANGED (Seg D shifts by a constant -4 units; Seg C's
 *   internal branches 47->53 and 55->58 are copied contiguously).
 *
 * FIX (b) — Set appCtx EARLIER via Lmxa.attachBaseContext(Context)V.
 *   Lmxa.attachBaseContext (4 insns, 2 regs, 0 tries) is the most-derived
 *   override in the chain (HiltLatinApp declares only <init>; Lesb/Lesf
 *   declare no attachBaseContext). The framework calls it on the Application
 *   instance BEFORE onCreate(), with the base Context in v1 (guaranteed
 *   non-null). Insert at idx 0: SPUT_OBJECT v1, Lpud->appCtx (2 units,
 *   even; no branches in the method, so no retargeting). Lesb.onCreate()'s
 *   hook is KEPT as a second setter; currentApplication() stays the final
 *   fallback in Block E.
 *
 * NO new fields, NO new method refs (appCtx/diagCtxSrc/diagPrefs/diagFirst
 * and all method refs used already exist in 860). method_ids and field_ids
 * must be UNCHANGED (asserted).
 *
 * REGISTER DISCIPLINE (Lpud.m, 16 regs — same as 858/859/860):
 *   v0 INT temp (wide low half in Block L', reset to INT(0) after use)
 *   v1 INT temp (wide high half in Block L', reset to INT(0) after use)
 *   v2 INT centre CP (sentinel -1 at entry; never written by Block L')
 *   v5 REF Context (resolved by Block E; never overwritten again)
 *   v6 REF (StringBuilder / SharedPreferences / Editor / Toast)
 *   v8 REF (record string)
 *   v11 REF const strings
 *   v12 this, v13 [I, v14 [Lpuz;, v15 [Object;
 * Block L' leaves v0/v1 as INT(0) and v6/v8/v11 as REF — the same register
 * types 860's Block L left at the merge into the fingerprint region, so the
 * TypeMergeCheck profile is unchanged (verified differentially).
 * MOVE_RESULT destinations: only v6 and v8 (dead scratch at those points).
 */
public class CtxFix2Patch {
    // ---------- registers (Lpud.m: 16 regs, params v12..v15) ----------
    static final int R_SLOT = 0;
    static final int R_TMP = 1;
    static final int R_CP = 2;
    static final int R_CTX = 5;
    static final int R_PREFS = 6;
    static final int R_PREFSTR = 7;
    static final int R_PARTS = 8;
    static final int R_PART = 11;

    static final String TAG = "NobiDiag";
    static final String DIAG_PREFS_NAME = "nobi_diag";

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
    static final ImmutableMethodReference SB_APP_LONG =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "append",
            Arrays.asList("J"), "Ljava/lang/StringBuilder;");
    static final ImmutableMethodReference SB_TOSTRING =
        new ImmutableMethodReference("Ljava/lang/StringBuilder;", "toString",
            Collections.<String>emptyList(), "Ljava/lang/String;");
    static final ImmutableMethodReference CURRENT_TIME_MILLIS =
        new ImmutableMethodReference("Ljava/lang/System;", "currentTimeMillis",
            Collections.<String>emptyList(), "J");
    static final ImmutableMethodReference GET_SHARED_PREFS =
        new ImmutableMethodReference("Landroid/content/Context;", "getSharedPreferences",
            Arrays.asList("Ljava/lang/String;", "I"), "Landroid/content/SharedPreferences;");
    static final ImmutableMethodReference SP_EDIT =
        new ImmutableMethodReference("Landroid/content/SharedPreferences;", "edit",
            Collections.<String>emptyList(), "Landroid/content/SharedPreferences$Editor;");
    static final ImmutableMethodReference SP_PUT_STRING =
        new ImmutableMethodReference("Landroid/content/SharedPreferences$Editor;", "putString",
            Arrays.asList("Ljava/lang/String;", "Ljava/lang/String;"),
            "Landroid/content/SharedPreferences$Editor;");
    static final ImmutableMethodReference SP_APPLY =
        new ImmutableMethodReference("Landroid/content/SharedPreferences$Editor;", "apply",
            Collections.<String>emptyList(), "V");

    static final ImmutableFieldReference APP_CTX =
        new ImmutableFieldReference("Lpud;", "appCtx", "Landroid/content/Context;");

    // ---------- tiny label-based block builder (from DefParsePatch/DiagPatch/CtxFixPatch) ----------
    static class B {
        List<Instruction> code = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();
        static class Fix { int idx; String target; Fix(int i, String t) { idx = i; target = t; } }
        List<Fix> fixups = new ArrayList<>();
        void label(String n) { if (labels.put(n, code.size()) != null) throw new RuntimeException("dup label " + n); }
        void add(Instruction i) { code.add(i); }
        void br21t(Opcode op, int reg, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction21t(op, reg, 0)); }
        void br10t(Opcode op, String target) { fixups.add(new Fix(code.size(), target)); code.add(new ImmutableInstruction10t(op, 0)); }
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
                if (old instanceof Instruction21t && (off < -32768 || off > 32767))
                    throw new RuntimeException("16-bit branch out of range at block idx " + f.idx + " -> " + f.target + " off=" + off);
                if (old instanceof Instruction21t) { Instruction21t o = (Instruction21t) old; code.set(f.idx, new ImmutableInstruction21t(o.getOpcode(), o.getRegisterA(), off)); }
                else throw new RuntimeException("bad fixup insn " + old.getOpcode());
            }
            if (totalUnits() % 2 != 0) throw new RuntimeException("block units still odd: " + totalUnits());
        }
    }

    static ImmutableInstruction35c inv35(Opcode op, int count, int a, int b, int c, int d, int e, MethodReference ref) {
        return new ImmutableInstruction35c(op, count, a, b, c, d, e, ref);
    }

    static void emitPrefsWrite(B b, String keyName, String skipLabel) {
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(DIAG_PREFS_NAME)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_CTX, R_PART, R_SLOT, 0, 0, GET_SHARED_PREFS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.br21t(Opcode.IF_EQZ, R_PREFS, skipLabel);
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_EDIT));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference(keyName)));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_PART, R_PARTS, 0, 0, SP_PUT_STRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SP_APPLY));
    }

    /**
     * Block L' — the last_run write, IDENTICAL code to 860's Block L, but now
     * placed OUTSIDE the diagFirst gate so it runs on EVERY Lpud.m call.
     * "run ts=<currentTimeMillis>" -> nobi_diag/last_run. Null-guarded on
     * the Context (v5); wide J regs reset to INT(0) after use.
     */
    static B buildL() {
        B b = new B();
        b.br21t(Opcode.IF_EQZ, R_CTX, "L_SKIP");
        b.add(new ImmutableInstruction21c(Opcode.NEW_INSTANCE, R_PREFS,
            new ImmutableTypeReference("Ljava/lang/StringBuilder;")));
        b.add(inv35(Opcode.INVOKE_DIRECT, 1, R_PREFS, 0, 0, 0, 0, SB_INIT));
        b.add(new ImmutableInstruction21c(Opcode.CONST_STRING, R_PART,
            new ImmutableStringReference("run ts=")));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 2, R_PREFS, R_PART, 0, 0, 0, SB_APP_STR));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0, CURRENT_TIME_MILLIS));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_WIDE, R_SLOT));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 3, R_PREFS, R_SLOT, R_TMP, 0, 0, SB_APP_LONG));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PREFS));
        b.add(inv35(Opcode.INVOKE_VIRTUAL, 1, R_PREFS, 0, 0, 0, 0, SB_TOSTRING));
        b.add(new ImmutableInstruction11x(Opcode.MOVE_RESULT_OBJECT, R_PARTS));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_SLOT, 0));
        b.add(new ImmutableInstruction11n(Opcode.CONST_4, R_TMP, 0));
        emitPrefsWrite(b, "last_run", "L_SKIP");
        b.label("L_SKIP");
        return b;
    }

    static int countIds(File f, int off) throws Exception {
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
        try {
            raf.seek(off);
            int b0 = raf.read(); int b1 = raf.read(); int b2 = raf.read(); int b3 = raf.read();
            return (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        } finally { raf.close(); }
    }

    static int[] computeAddrs(List<Instruction> ins) {
        int[] a = new int[ins.size()];
        int x = 0;
        for (int i = 0; i < ins.size(); i++) { a[i] = x; x += ins.get(i).getCodeUnits(); }
        return a;
    }

    public static void main(String[] args) throws Exception {
        File inFile = new File(args[0]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(inFile, Opcodes.forApi(35));
        int inMethodIds = countIds(inFile, 88);
        int inFieldIds = countIds(inFile, 80);
        System.out.println("input method_ids_size=" + inMethodIds + " field_ids_size=" + inFieldIds);
        // ============ Load + verify Lpud.m (860: 498 insns, 16 regs) ============
        List<Instruction> orig = null;
        MethodImplementation origImpl = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lpud;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("m") && m.getParameters().size() == 3) {
                    origImpl = m.getImplementation();
                    orig = new ArrayList<>();
                    for (Instruction i : origImpl.getInstructions()) orig.add(i);
                    if (orig.size() != 498 || origImpl.getRegisterCount() != 16) {
                        System.out.println("Lpud.m: unexpected shape ins=" + orig.size() + " regs=" + origImpl.getRegisterCount() + " (expected 498/16)"); return;
                    }
                    if (!origImpl.getTryBlocks().isEmpty()) { System.out.println("Lpud.m: unexpected try blocks"); return; }
                }
            }
        }
        if (orig == null) { System.out.println("Lpud.m not found"); return; }
        int n = orig.size();
        int[] origAddr = computeAddrs(orig);
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
        // Fingerprints (860 head structure)
        Instruction i0 = orig.get(0);
        if (i0.getOpcode() != Opcode.CONST_4 || !(i0 instanceof Instruction11n) || ((Instruction11n) i0).getRegisterA() != 0) {
            System.out.println("Lpud.m: insn0 not CONST_4 v0,0 (got " + i0 + ")"); return;
        }
        Instruction i2 = orig.get(2);
        if (i2.getOpcode() != Opcode.SGET_OBJECT) { System.out.println("Lpud.m: insn2 not SGET_OBJECT (got " + i2 + ")"); return; }
        Instruction i14 = orig.get(14);
        if (i14.getOpcode() != Opcode.CONST_4 || !(i14 instanceof Instruction11n) || ((Instruction11n) i14).getRegisterA() != 2 || ((Instruction11n) i14).getNarrowLiteral() != -1) {
            System.out.println("Lpud.m: insn14 not CONST_4 v2,-1 (got " + i14 + ")"); return;
        }
        if (orig.get(15).getOpcode() != Opcode.SGET_BOOLEAN) { System.out.println("Lpud.m: insn15 not SGET_BOOLEAN"); return; }
        if (origTarget[16] != 70) { System.out.println("Lpud.m: insn16 IF_NEZ does not target 70 (got " + origTarget[16] + ")"); return; }
        if (origTarget[19] != 43) { System.out.println("Lpud.m: insn19 does not target 43 (got " + origTarget[19] + ")"); return; }
        if (origTarget[36] != 43) { System.out.println("Lpud.m: insn36 does not target 43 (got " + origTarget[36] + ")"); return; }
        if (origTarget[47] != 53) { System.out.println("Lpud.m: insn47 does not target 53 (got " + origTarget[47] + ")"); return; }
        if (origTarget[55] != 58) { System.out.println("Lpud.m: insn55 does not target 58 (got " + origTarget[55] + ")"); return; }
        if (orig.get(70).getOpcode() != Opcode.NOP) { System.out.println("Lpud.m: insn70 not NOP (got " + orig.get(70) + ")"); return; }
        if (orig.get(71).getOpcode() != Opcode.ARRAY_LENGTH) { System.out.println("Lpud.m: insn71 not ARRAY_LENGTH"); return; }
        Instruction i448 = orig.get(448);
        if (i448.getOpcode() != Opcode.CONST || !(i448 instanceof Instruction31i) || ((Instruction31i) i448).getNarrowLiteral() != 0x1F496) {
            System.out.println("Lpud.m: insn448 not CONST 0x1F496 (got " + i448 + ")"); return;
        }
        if (origTarget[497] != 381) { System.out.println("Lpud.m: insn497 GOTO_16 does not target 381 (got " + origTarget[497] + ")"); return; }
        // GUARD branches in 860 target P3 = 448. Count them.
        List<Integer> guardSrc = new ArrayList<>();
        for (int i = 0; i < n; i++) if (origTarget[i] == 448) guardSrc.add(i);
        System.out.println("Lpud.m: GUARD branches (target 448): " + guardSrc.size() + " " + guardSrc);
        // No branch from insns >= 72 targets < 72 (tail self-contained wrt head).
        for (int i = 72; i < n; i++) {
            if (origTarget[i] >= 0 && origTarget[i] < 72) {
                System.out.println("Lpud.m: tail branch at " + i + " targets head insn " + origTarget[i] + " — abort"); return;
            }
        }
        // No branch other than insn 16 targets insn 70.
        for (int i = 0; i < n; i++) {
            if (i != 16 && origTarget[i] == 70) { System.out.println("Lpud.m: unexpected branch at " + i + " targets 70 — abort"); return; }
        }
        System.out.println("Lpud.m: shape + fingerprints OK (498 insns, 16 regs, IF_NEZ 16->70, head branches verified)");
        // ============ Load + verify Lmxa.attachBaseContext (4 insns, 2 regs) ============
        List<Instruction> mxaOrig = null;
        MethodImplementation mxaImpl = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lmxa;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("attachBaseContext") && m.getParameters().size() == 1) {
                    mxaImpl = m.getImplementation();
                    mxaOrig = new ArrayList<>();
                    for (Instruction i : mxaImpl.getInstructions()) mxaOrig.add(i);
                    if (mxaOrig.size() != 4 || mxaImpl.getRegisterCount() != 2) {
                        System.out.println("Lmxa.attachBaseContext: unexpected shape ins=" + mxaOrig.size() + " regs=" + mxaImpl.getRegisterCount() + " (expected 4/2)"); return;
                    }
                    if (!mxaImpl.getTryBlocks().isEmpty()) { System.out.println("Lmxa.attachBaseContext: unexpected try blocks"); return; }
                    if (mxaOrig.get(0).getOpcode() != Opcode.INVOKE_SUPER) { System.out.println("Lmxa.attachBaseContext: insn0 not INVOKE_SUPER"); return; }
                    Instruction mi1 = mxaOrig.get(1);
                    if (mi1.getOpcode() != Opcode.CONST_4 || !(mi1 instanceof Instruction11n) || ((Instruction11n) mi1).getRegisterA() != 1 || ((Instruction11n) mi1).getNarrowLiteral() != 0) {
                        System.out.println("Lmxa.attachBaseContext: insn1 not CONST_4 v1,0 (got " + mi1 + ")"); return;
                    }
                    if (mxaOrig.get(2).getOpcode() != Opcode.INVOKE_STATIC) { System.out.println("Lmxa.attachBaseContext: insn2 not INVOKE_STATIC"); return; }
                    if (mxaOrig.get(3).getOpcode() != Opcode.RETURN_VOID) { System.out.println("Lmxa.attachBaseContext: insn3 not RETURN_VOID"); return; }
                    System.out.println("Lmxa.attachBaseContext: shape OK (4 insns, 2 regs, INVOKE_SUPER/CONST_4/INVOKE_STATIC/RETURN_VOID)");
                }
            }
        }
        if (mxaOrig == null) { System.out.println("Lmxa.attachBaseContext not found"); return; }
        // ============ Verify Lesb.onCreate (860: 10 insns, hook at idx 0) — NOT modified ============
        boolean lesbOk = false;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lesb;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("onCreate") && m.getParameters().size() == 0) {
                    MethodImplementation impl = m.getImplementation();
                    List<Instruction> li = new ArrayList<>();
                    for (Instruction i : impl.getInstructions()) li.add(i);
                    if (li.size() != 10 || impl.getRegisterCount() != 2) {
                        System.out.println("Lesb.onCreate: unexpected shape ins=" + li.size() + " regs=" + impl.getRegisterCount() + " (expected 10/2)"); return;
                    }
                    Instruction l0 = li.get(0);
                    if (l0.getOpcode() != Opcode.SPUT_OBJECT) { System.out.println("Lesb.onCreate: insn0 not SPUT_OBJECT (860 hook missing!)"); return; }
                    Instruction l2 = li.get(2);
                    if (l2.getOpcode() != Opcode.IF_NEZ || !(l2 instanceof Instruction21t) || ((Instruction21t) l2).getCodeOffset() != 11) {
                        System.out.println("Lesb.onCreate: insn2 not IF_NEZ +11 (got " + l2 + ")"); return;
                    }
                    if (li.get(9).getOpcode() != Opcode.RETURN_VOID) { System.out.println("Lesb.onCreate: insn9 not RETURN_VOID"); return; }
                    lesbOk = true;
                    System.out.println("Lesb.onCreate: OK (10 insns, hook at idx 0, IF_NEZ +11) — NOT modified");
                }
            }
        }
        if (!lesbOk) { System.out.println("Lesb.onCreate not found"); return; }
        // ============ Build Block L' (fresh, same code as 860 Block L) ============
        B l = buildL();
        l.padEven();
        int sL = l.totalUnits();
        // Base of Block L' in the final method: Seg A = 860 insns 0-14 = 15 insns.
        int baseL = origAddr[15]; // address of old insn 15 (== end of Seg A, since Seg A is contiguous 0..14)
        // Sanity: origAddr[15] must equal total units of 860 insns 0..14.
        System.out.println("Block L': " + l.code.size() + " insns, " + sL + " units, base=" + baseL);
        l.fixup(baseL);
        // ============ Assemble the new Lpud.m ============
        // Seg A: old 0-14 | Seg B: Block L' | Seg C: old 15-18 + old 47-69 | Seg D: old 70-497
        List<Instruction> all = new ArrayList<>();
        int[] origToFinal = new int[n];
        Arrays.fill(origToFinal, -1);
        for (int i = 0; i <= 14; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        int lStart = all.size();
        all.addAll(l.code);
        // Seg C part 1: old 15-18 (flag check/set)
        for (int i = 15; i <= 18; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // Seg C part 2: old 47-69 (Toast guard + Toast + P0 Log)
        for (int i = 47; i <= 69; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        // Seg D: old 70-497
        for (int i = 70; i <= 497; i++) { origToFinal[i] = all.size(); all.add(orig.get(i)); }
        for (int i = 0; i < n; i++) {
            if (i >= 19 && i <= 46) continue; // dropped: old Block L (19-42) + NOPs (43-46)
            if (origToFinal[i] < 0) { System.out.println("origToFinal missing for old insn " + i); return; }
        }
        int m = all.size();
        int[] finAddr = computeAddrs(all);
        int finTotal = finAddr[m - 1] + all.get(m - 1).getCodeUnits();
        System.out.println("new Lpud.m: " + m + " insns (expected 494), " + finTotal + " units (orig " + origTotal + ")");
        if (m != 494) { System.out.println("unexpected new insn count " + m); return; }
        // NOTE: total units need NOT be even — 860's total is 921 (odd) and
        // works. What matters is the DELTA is even (removed 52 units, added
        // 48 = -4), so payload alignment parity is preserved.
        if ((finTotal - origTotal) % 2 != 0) { System.out.println("unit delta ODD: " + (finTotal - origTotal)); return; }
        // Sanity: Block L' start address must equal its base.
        if (finAddr[lStart] != baseL) { System.out.println("Block L' start addr mismatch: " + finAddr[lStart] + " != " + baseL); return; }
        // Sanity: Seg D start (old 70) must be at new idx 66.
        if (origToFinal[70] != 66) { System.out.println("Seg D start not at new idx 66 (got " + origToFinal[70] + ")"); return; }
        // ============ Retarget the ONE branch: old insn 16 (IF_NEZ) -> Seg D start ============
        int fi16 = origToFinal[16];
        Instruction ins16 = all.get(fi16);
        if (!(ins16 instanceof Instruction21t) || ins16.getOpcode() != Opcode.IF_NEZ) {
            System.out.println("new insn at origToFinal[16] is not IF_NEZ (got " + ins16.getOpcode() + ")"); return;
        }
        int newOff16 = finAddr[origToFinal[70]] - finAddr[fi16];
        if (newOff16 < -32768 || newOff16 > 32767) { System.out.println("IF_NEZ retarget out of range: " + newOff16); return; }
        all.set(fi16, new ImmutableInstruction21t(Opcode.IF_NEZ, ((Instruction21t) ins16).getRegisterA(), newOff16));
        System.out.println("retargeted IF_NEZ (old 16 -> new " + fi16 + ") to Seg D start (new " + origToFinal[70] + "), offset=" + newOff16);
        // Verify every other original branch's offset is UNCHANGED.
        for (int i = 0; i < n; i++) {
            if (i >= 19 && i <= 46) continue; // dropped region
            if (!(orig.get(i) instanceof OffsetInstruction)) continue;
            if (i == 16) continue; // retargeted above (intentional)
            int fi = origToFinal[i];
            Instruction ins = all.get(fi);
            int oldOff = ((OffsetInstruction) orig.get(i)).getCodeOffset();
            int newOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                System.out.println("ORIGINAL branch offset CHANGED at old " + i + " old=" + oldOff + " new=" + newOff + " — abort");
                return;
            }
        }
        System.out.println("all other original branch offsets unchanged (verified)");
        // Verify Block L' internal branches target the block end (Seg C start).
        // (fixup already asserted non-zero offsets; check the first IF_EQZ targets Seg C start.)
        // ============ Build new implementations ============
        MethodImplementation newLpudImpl = new ImmutableMethodImplementation(
            origImpl.getRegisterCount(), all, origImpl.getTryBlocks(), origImpl.getDebugItems());
        List<Instruction> mxaNew = new ArrayList<>();
        mxaNew.add(new ImmutableInstruction21c(Opcode.SPUT_OBJECT, 1, APP_CTX));
        for (Instruction i : mxaOrig) mxaNew.add(i);
        MethodImplementation newMxaImpl = new ImmutableMethodImplementation(
            mxaImpl.getRegisterCount(), mxaNew, mxaImpl.getTryBlocks(), mxaImpl.getDebugItems());
        System.out.println("Lmxa.attachBaseContext: new impl " + mxaNew.size() + " insns (SPUT_OBJECT v1, appCtx at idx 0 + orig 4)");
        // ============ Module: rewrite Lpud.m + Lmxa.attachBaseContext (NO new fields/refs) ============
        final MethodImplementation finLpud = newLpudImpl;
        final MethodImplementation finMxa = newMxaImpl;
        Module mod = new Module(finLpud, finMxa);
        DexRewriter rw = new DexRewriter(mod);
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes.dex written to " + args[1]);
        int outMethodIds = countIds(new File(args[1]), 88);
        int outFieldIds = countIds(new File(args[1]), 80);
        System.out.println("post-check: method_ids_size=" + outMethodIds + " (input " + inMethodIds + ", expect UNCHANGED)");
        System.out.println("post-check: field_ids_size=" + outFieldIds + " (input " + inFieldIds + ", expect UNCHANGED)");
        if (outMethodIds != inMethodIds) throw new RuntimeException("method_ids changed (got " + outMethodIds + ", expected " + inMethodIds + ")");
        if (outFieldIds != inFieldIds) throw new RuntimeException("field_ids changed (got " + outFieldIds + ", expected " + inFieldIds + ")");
    }

    static class Module extends RewriterModule {
        final MethodImplementation finLpud;
        final MethodImplementation finMxa;
        Module(MethodImplementation fl, MethodImplementation fm) { finLpud = fl; finMxa = fm; }
        @Override
        public Rewriter<Method> getMethodRewriter(final Rewriters rewriters) {
            final Rewriter<Method> def = super.getMethodRewriter(rewriters);
            return new Rewriter<Method>() {
                public Method rewrite(Method method) {
                    if (method.getDefiningClass().equals("Lpud;")
                            && method.getName().equals("m") && method.getParameters().size() == 3) {
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, finLpud);
                    }
                    if (method.getDefiningClass().equals("Lmxa;")
                            && method.getName().equals("attachBaseContext") && method.getParameters().size() == 1) {
                        return new ImmutableMethod(method.getDefiningClass(), method.getName(),
                            method.getParameters(), method.getReturnType(),
                            method.getAccessFlags(), method.getAnnotations(), null, finMxa);
                    }
                    return def.rewrite(method);
                }
            };
        }
    }
}
