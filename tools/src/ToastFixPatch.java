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
 * TOAST FIX (code 176071862) for NobiBoard def-parse customization.
 * Base = shipped 176071861.
 *
 * ROOT CAUSE (diagnosis-861, PROVEN): the P0 Toast in Lpud.m throws on the
 *   def-parse thread and kills the keyboard-definition load. 861 is the first
 *   build in which the Lpud.m diagnostic Toasts actually FIRE (in 858/859/860
 *   they were always skipped because the Context was null on the first call).
 *   Toast.makeText() on a thread with no Looper throws NPE ("Can't toast on
 *   a thread that has not called Looper.prepare()"). Lpud.m has 0 tries, so
 *   the throw propagates (Lpud.m -> Lpwz.f -> Levj.a -> Lcwj.call rethrows)
 *   and the module-load FutureTask completes exceptionally; ModuleInfo.
 *   loadModule() catches ExecutionException and returns null -> no keyboard
 *   definition -> keyboard cannot open. The failed Future is cached, so the
 *   keyboard never opens again for the whole process. No crash dialog.
 *
 * CHANGE 1 (MUST-HAVE): remove ALL FOUR Toast call sites from Lpud.m.
 *   P0 insns 46/47/48, P1 227/228/229, P2 337/338/339, P3 453/454/455.
 *   Each site = INVOKE_STATIC Toast.makeText (35c, 3 units) +
 *   MOVE_RESULT_OBJECT (11x, 1 unit) + INVOKE_VIRTUAL Toast.show (35c,
 *   3 units). Replaced with SEVEN NOPs (3+1+3) so the total code-unit
 *   count is IDENTICAL and every instruction boundary stays at the same
 *   address — therefore EVERY branch offset in the method is unchanged
 *   and NO retargeting is needed. The CONST_STRING message loads and the
 *   Toast method refs stay (unused) — method_ids unchanged.
 *   KEEP the Log.e calls and the nobi_diag SharedPreferences writes.
 *
 * CHANGE 2 (SHOULD-HAVE, flag-controlled): add ONE catch-all try block
 *   covering the hook region (old insns 49..381, i.e. P0-Log fingerprint
 *   through the substitution + clean-up) with the handler jumping to the
 *   existing GUARD path (old insn 444). Handler (appended at the end of
 *   the method, after the final goto/16): move-exception v0; const/4 v0,0;
 *   goto/16 -> GUARD. The const/4 discards the exception object so v0 is
 *   INT at the GUARD join (INT on the normal path too) — the merge is
 *   INT-vs-INT, clean. v0 is INT on every normal GUARD entry (it is an INT
 *   scratch throughout the hook; the wide J in Block L' is reset to INT(0)
 *   before the try region starts). The handler never runs the last_a write
 *   for a non-A key (GUARD's A-key check skips to the final goto), and for
 *   an A key it runs the normal GUARD path. If the try/catch cannot be
 *   added safely, run with mode "notry" and ship Change 1 only.
 *
 * NO new fields, NO new method refs. method_ids and field_ids UNCHANGED.
 * Lmxa.attachBaseContext and Lesb.onCreate are NOT modified (asserted).
 */
public class ToastFixPatch {
    static final int[] SITES = {46, 227, 337, 453};
    static final String[] SITE_MSGS = {"DEF 859: Lpud.m runs", "A: prefs=", "A: SUB OK", "A: GUARD"};
    static final int[] SITE_MSG_IDX = {44, 219, 335, 446};

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

    static String refString(Instruction ins) {
        if (!(ins instanceof ReferenceInstruction)) return "<not-a-ref>";
        Reference r = ((ReferenceInstruction) ins).getReference();
        return r == null ? "<null-ref>" : r.toString();
    }

    public static void main(String[] args) throws Exception {
        boolean doTry = args.length < 3 || !args[2].equals("notry");
        File inFile = new File(args[0]);
        DexBackedDexFile dex = DexFileFactory.loadDexFile(inFile, Opcodes.forApi(35));
        int inMethodIds = countIds(inFile, 88);
        int inFieldIds = countIds(inFile, 80);
        System.out.println("input method_ids_size=" + inMethodIds + " field_ids_size=" + inFieldIds);
        System.out.println("mode: " + (doTry ? "WITH try/catch" : "Change 1 ONLY (no try/catch)"));

        // ============ Load + verify Lpud.m (861: 494 insns, 16 regs, 0 tries) ============
        List<Instruction> orig = null;
        MethodImplementation origImpl = null;
        for (ClassDef cd : dex.getClasses()) {
            if (!cd.getType().equals("Lpud;")) continue;
            for (Method m : cd.getMethods()) {
                if (m.getName().equals("m") && m.getParameters().size() == 3) {
                    origImpl = m.getImplementation();
                    orig = new ArrayList<>();
                    for (Instruction i : origImpl.getInstructions()) orig.add(i);
                    if (orig.size() != 494 || origImpl.getRegisterCount() != 16) {
                        System.out.println("Lpud.m: unexpected shape ins=" + orig.size() + " regs=" + origImpl.getRegisterCount() + " (expected 494/16)"); return;
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
        // ---- Verify the 4 Toast sites by ReferenceInstruction (never toString) ----
        for (int s = 0; s < SITES.length; s++) {
            int i = SITES[s];
            Instruction inv = orig.get(i);
            if (inv.getOpcode() != Opcode.INVOKE_STATIC || !(inv instanceof Instruction35c)) {
                System.out.println("site " + i + ": not INVOKE_STATIC 35c (got " + inv.getOpcode() + ")"); return;
            }
            Reference r = ((ReferenceInstruction) inv).getReference();
            if (!(r instanceof MethodReference)) { System.out.println("site " + i + ": makeText ref not a MethodReference"); return; }
            MethodReference mr = (MethodReference) r;
            if (!mr.getDefiningClass().equals("Landroid/widget/Toast;") || !mr.getName().equals("makeText")
                    || !mr.getParameterTypes().equals(Arrays.asList("Landroid/content/Context;", "Ljava/lang/CharSequence;", "I"))
                    || !mr.getReturnType().equals("Landroid/widget/Toast;")) {
                System.out.println("site " + i + ": makeText ref mismatch: " + mr); return;
            }
            Instruction mr2 = orig.get(i + 1);
            if (mr2.getOpcode() != Opcode.MOVE_RESULT_OBJECT || !(mr2 instanceof Instruction11x) || ((Instruction11x) mr2).getRegisterA() != 6) {
                System.out.println("site " + i + ": insn " + (i + 1) + " not MOVE_RESULT_OBJECT v6 (got " + mr2 + ")"); return;
            }
            Instruction sh = orig.get(i + 2);
            if (sh.getOpcode() != Opcode.INVOKE_VIRTUAL || !(sh instanceof Instruction35c)) {
                System.out.println("site " + i + ": not INVOKE_VIRTUAL 35c (got " + sh.getOpcode() + ")"); return;
            }
            Reference r2 = ((ReferenceInstruction) sh).getReference();
            if (!(r2 instanceof MethodReference)) { System.out.println("site " + i + ": show ref not a MethodReference"); return; }
            MethodReference mr2r = (MethodReference) r2;
            if (!mr2r.getDefiningClass().equals("Landroid/widget/Toast;") || !mr2r.getName().equals("show")
                    || !mr2r.getParameterTypes().isEmpty() || !mr2r.getReturnType().equals("V")) {
                System.out.println("site " + i + ": show ref mismatch: " + mr2r); return;
            }
            // Fingerprint: the CONST_STRING just before the site carries the Toast message.
            Instruction cs = orig.get(SITE_MSG_IDX[s]);
            if (cs.getOpcode() != Opcode.CONST_STRING || !(cs instanceof Instruction21c)) {
                System.out.println("site " + i + ": msg insn " + SITE_MSG_IDX[s] + " not CONST_STRING"); return;
            }
            Reference csr = ((ReferenceInstruction) cs).getReference();
            if (!(csr instanceof StringReference) || !((StringReference) csr).getString().equals(SITE_MSGS[s])) {
                System.out.println("site " + i + ": msg mismatch at " + SITE_MSG_IDX[s] + " (got " + csr + ")"); return;
            }
            System.out.println("Toast site " + i + " verified (makeText/move-result-object v6/show, msg=\"" + SITE_MSGS[s] + "\")");
        }
        // ---- Verify structural anchors (so the try region + GUARD targets are what the diagnosis says) ----
        if (orig.get(49).getOpcode() != Opcode.ARRAY_LENGTH) { System.out.println("anchor: insn 49 not ARRAY_LENGTH (P0-Log fingerprint)"); return; }
        if (orig.get(67).getOpcode() != Opcode.ARRAY_LENGTH) { System.out.println("anchor: insn 67 not ARRAY_LENGTH (real fingerprint)"); return; }
        if (orig.get(377).getOpcode() != Opcode.CONST_4) { System.out.println("anchor: insn 377 not CONST_4 (clean-up start)"); return; }
        if (orig.get(382).getOpcode() != Opcode.CONST_4) { System.out.println("anchor: insn 382 not CONST_4 (stock body start)"); return; }
        if (orig.get(443).getOpcode() != Opcode.RETURN_VOID) { System.out.println("anchor: insn 443 not RETURN_VOID (stock body end)"); return; }
        if (orig.get(444).getOpcode() != Opcode.CONST || !(orig.get(444) instanceof Instruction31i) || ((Instruction31i) orig.get(444)).getNarrowLiteral() != 0x1F496) {
            System.out.println("anchor: insn 444 not CONST 0x1F496 (GUARD start)"); return;
        }
        if (origTarget[493] != 377) { System.out.println("anchor: insn 493 GOTO_16 does not target clean-up 377 (got " + origTarget[493] + ")"); return; }
        // Count GUARD branches (target 444) — expect 11.
        List<Integer> guardSrc = new ArrayList<>();
        for (int i = 0; i < n; i++) if (origTarget[i] == 444) guardSrc.add(i);
        System.out.println("GUARD branches (target 444): " + guardSrc.size() + " " + guardSrc);
        if (guardSrc.size() != 11) { System.out.println("anchor: expected 11 GUARD branches, got " + guardSrc.size()); return; }
        // Count Log.e invokes (must stay 4) and Toast invokes (must be exactly 8: 4 makeText + 4 show).
        int logE = 0, toastInv = 0;
        for (Instruction ins : orig) {
            if (ins instanceof ReferenceInstruction) {
                Reference r = ((ReferenceInstruction) ins).getReference();
                if (r instanceof MethodReference) {
                    MethodReference mr = (MethodReference) r;
                    if (mr.getDefiningClass().equals("Landroid/util/Log;") && mr.getName().equals("e")) logE++;
                    if (mr.getDefiningClass().equals("Landroid/widget/Toast;")) toastInv++;
                }
            }
        }
        System.out.println("Log.e invokes in Lpud.m: " + logE + " (expect 4); Toast invokes: " + toastInv + " (expect 8)");
        if (logE != 4 || toastInv != 8) { System.out.println("anchor: unexpected Log.e/Toast counts"); return; }
        // Count nobi_diag CONST_STRINGs (expect 3: Block L', Block W_SUB, GUARD) and last_run/last_a.
        int nobiDiag = 0, lastRun = 0, lastA = 0;
        for (Instruction ins : orig) {
            if (ins instanceof ReferenceInstruction) {
                Reference r = ((ReferenceInstruction) ins).getReference();
                if (r instanceof StringReference) {
                    String s = ((StringReference) r).getString();
                    if (s.equals("nobi_diag")) nobiDiag++;
                    if (s.equals("last_run")) lastRun++;
                    if (s.equals("last_a")) lastA++;
                }
            }
        }
        System.out.println("nobi_diag strings: " + nobiDiag + " (expect 3); last_run: " + lastRun + " (expect 1); last_a: " + lastA + " (expect 2)");
        if (nobiDiag != 3 || lastRun != 1 || lastA != 2) { System.out.println("anchor: unexpected nobi_diag/last_run/last_a counts"); return; }

        // ============ Build the new Lpud.m: NOP the Toast sites, preserving unit layout ============
        List<Instruction> all = new ArrayList<>();
        int[] origToNew = new int[n];
        Arrays.fill(origToNew, -1);
        Set<Integer> siteInsns = new HashSet<>();
        for (int s : SITES) { siteInsns.add(s); siteInsns.add(s + 1); siteInsns.add(s + 2); }
        for (int i = 0; i < n; i++) {
            origToNew[i] = all.size();
            if (siteInsns.contains(i)) {
                // Replace each Toast-site insn with NOPs totalling the SAME units:
                // 35c invoke = 3 units -> 3 NOPs; 11x move-result = 1 unit -> 1 NOP.
                int units = orig.get(i).getCodeUnits();
                for (int u = 0; u < units; u++) all.add(new ImmutableInstruction10x(Opcode.NOP));
            } else {
                all.add(orig.get(i));
            }
        }
        int preHandlerInsns = all.size();
        int[] newAddr = computeAddrs(all);
        int newTotalPreHandler = newAddr[preHandlerInsns - 1] + all.get(preHandlerInsns - 1).getCodeUnits();
        System.out.println("new Lpud.m (NOPed, no handler yet): " + preHandlerInsns + " insns (expect 510), " + newTotalPreHandler + " units (orig " + origTotal + ", expect EQUAL)");
        if (preHandlerInsns != 510) { System.out.println("unexpected new insn count " + preHandlerInsns); return; }
        if (newTotalPreHandler != origTotal) { System.out.println("unit total CHANGED: " + newTotalPreHandler + " != " + origTotal); return; }
        // Verify every original branch offset is UNCHANGED (unit layout preserved).
        for (int i = 0; i < n; i++) {
            if (!(orig.get(i) instanceof OffsetInstruction)) continue;
            int fi = origToNew[i];
            // The branch insn itself was copied verbatim (Toast sites contain no branches).
            Instruction ins = all.get(fi);
            int oldOff = ((OffsetInstruction) orig.get(i)).getCodeOffset();
            int newOff = ((OffsetInstruction) ins).getCodeOffset();
            if (newOff != oldOff) {
                System.out.println("branch offset CHANGED at old " + i + " old=" + oldOff + " new=" + newOff + " — abort"); return;
            }
            // And its target must be the same original insn.
            int newTgtAddr = newAddr[fi] + newOff;
            Integer ti = null;
            for (int j = 0; j < preHandlerInsns; j++) if (newAddr[j] == newTgtAddr) { ti = j; break; }
            if (ti == null || origToNew[origTarget[i]] != ti) {
                System.out.println("branch target WRONG at old " + i + " -> new idx " + ti + " (expect origToNew[" + origTarget[i] + "]=" + origToNew[origTarget[i]] + ")"); return;
            }
        }
        System.out.println("all original branch offsets + targets unchanged (verified)");
        // Verify NO Toast invoke remains and Log.e / nobi_diag counts are intact.
        int toastAfter = 0, logAfter = 0, diagAfter = 0;
        for (Instruction ins : all) {
            if (ins instanceof ReferenceInstruction) {
                Reference r = ((ReferenceInstruction) ins).getReference();
                if (r instanceof MethodReference) {
                    MethodReference mr = (MethodReference) r;
                    if (mr.getDefiningClass().equals("Landroid/widget/Toast;")) toastAfter++;
                    if (mr.getDefiningClass().equals("Landroid/util/Log;") && mr.getName().equals("e")) logAfter++;
                }
                if (r instanceof StringReference && ((StringReference) r).getString().equals("nobi_diag")) diagAfter++;
            }
        }
        System.out.println("after NOPing: Toast invokes=" + toastAfter + " (expect 0); Log.e=" + logAfter + " (expect 4); nobi_diag=" + diagAfter + " (expect 3)");
        if (toastAfter != 0 || logAfter != 4 || diagAfter != 3) { System.out.println("post-NOP counts wrong"); return; }

        // ============ Change 2: catch-all try block around the hook region -> GUARD ============
        List<ImmutableTryBlock> tryBlocks = new ArrayList<>();
        if (doTry) {
            int tryStartIdx = origToNew[49];
            int tryEndIdx = origToNew[382]; // exclusive
            int guardIdx = origToNew[444];
            int tryStartAddr = newAddr[tryStartIdx];
            int tryEndAddr = newAddr[tryEndIdx];
            int handlerAddr = newTotalPreHandler; // handler appended right after the last insn
            // Handler: move-exception v0; const/4 v0,0; goto/16 -> GUARD.
            int gotoAddr = handlerAddr + 1 + 1; // move-exception (1) + const/4 (1)
            int guardAddr = newAddr[guardIdx];
            int gotoOff = guardAddr - gotoAddr;
            if (gotoOff < -32768 || gotoOff > 32767) { System.out.println("handler GOTO_16 out of range: " + gotoOff); return; }
            all.add(new ImmutableInstruction11x(Opcode.MOVE_EXCEPTION, 0));
            all.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 0));
            all.add(new ImmutableInstruction20t(Opcode.GOTO_16, gotoOff));
            int[] finAddr = computeAddrs(all);
            int finTotal = finAddr[all.size() - 1] + all.get(all.size() - 1).getCodeUnits();
            // Sanity: handler addresses are where we computed them.
            if (finAddr[preHandlerInsns] != handlerAddr || finAddr[preHandlerInsns + 2] != gotoAddr) {
                System.out.println("handler address mismatch"); return;
            }
            if ((finTotal - origTotal) % 2 != 0) { System.out.println("unit delta ODD with handler: " + (finTotal - origTotal)); return; }
            tryBlocks.add(new ImmutableTryBlock(tryStartAddr, tryEndAddr - tryStartAddr,
                Collections.singletonList(new ImmutableExceptionHandler(null, handlerAddr))));
            System.out.println("try/catch added: region [old 49..382) -> new idx [" + tryStartIdx + ".." + tryEndIdx + "), addrs [" + tryStartAddr + ".." + tryEndAddr + "), handler at addr " + handlerAddr + ", GOTO_16 -> GUARD (idx " + guardIdx + ", addr " + guardAddr + ", off " + gotoOff + ")");
            System.out.println("new Lpud.m total: " + all.size() + " insns, " + finTotal + " units (delta +" + (finTotal - origTotal) + ", even)");
        } else {
            System.out.println("try/catch SKIPPED (notry mode): " + all.size() + " insns, " + newTotalPreHandler + " units (delta 0)");
        }

        // ============ Verify Lmxa.attachBaseContext (5 insns) + Lesb.onCreate (10 insns) are 861-shaped (NOT modified) ============
        for (ClassDef cd : dex.getClasses()) {
            if (cd.getType().equals("Lmxa;")) {
                for (Method m : cd.getMethods()) {
                    if (m.getName().equals("attachBaseContext") && m.getParameters().size() == 1) {
                        List<Instruction> li = new ArrayList<>();
                        for (Instruction i : m.getImplementation().getInstructions()) li.add(i);
                        if (li.size() != 5 || li.get(0).getOpcode() != Opcode.SPUT_OBJECT || li.get(4).getOpcode() != Opcode.RETURN_VOID) {
                            System.out.println("Lmxa.attachBaseContext: unexpected shape (expected 861's 5 insns, SPUT at 0)"); return;
                        }
                        System.out.println("Lmxa.attachBaseContext: OK (5 insns, hook at idx 0) — NOT modified");
                    }
                }
            }
            if (cd.getType().equals("Lesb;")) {
                for (Method m : cd.getMethods()) {
                    if (m.getName().equals("onCreate") && m.getParameters().size() == 0) {
                        List<Instruction> li = new ArrayList<>();
                        for (Instruction i : m.getImplementation().getInstructions()) li.add(i);
                        if (li.size() != 10 || li.get(0).getOpcode() != Opcode.SPUT_OBJECT || li.get(9).getOpcode() != Opcode.RETURN_VOID) {
                            System.out.println("Lesb.onCreate: unexpected shape (expected 861's 10 insns, SPUT at 0)"); return;
                        }
                        System.out.println("Lesb.onCreate: OK (10 insns, hook at idx 0) — NOT modified");
                    }
                }
            }
        }

        // ============ Build new implementation + rewrite Lpud.m only ============
        MethodImplementation newImpl = new ImmutableMethodImplementation(
            origImpl.getRegisterCount(), all, tryBlocks, origImpl.getDebugItems());
        final MethodImplementation finImpl = newImpl;
        Module mod = new Module(finImpl);
        DexRewriter rw = new DexRewriter(mod);
        DexFile out = rw.getDexFileRewriter().rewrite(dex);
        DexFileFactory.writeDexFile(args[1], out);
        System.out.println("patched classes.dex written to " + args[1]);
        int outMethodIds = countIds(new File(args[1]), 88);
        int outFieldIds = countIds(new File(args[1]), 80);
        System.out.println("post-check: method_ids_size=" + outMethodIds + " (input " + inMethodIds + ")");
        System.out.println("post-check: field_ids_size=" + outFieldIds + " (input " + inFieldIds + ", expect UNCHANGED)");
        // EXPECTED: nop-ing the 4 Toast call sites removes the LAST references
        // to Toast.makeText + Toast.show in classes.dex, so the DexBuilder
        // drops exactly those 2 unreferenced method refs (verified separately
        // with MethodRefScan: DROPPED = makeText + show, ADDED = nothing).
        // field_ids must be unchanged; method_ids must be input or input-2.
        if (outMethodIds != inMethodIds && outMethodIds != inMethodIds - 2) throw new RuntimeException("method_ids changed unexpectedly (got " + outMethodIds + ", expected " + inMethodIds + " or " + (inMethodIds - 2) + ")");
        if (outMethodIds == inMethodIds - 2) System.out.println("method_ids -2: the 2 dropped refs are Toast.makeText + Toast.show (now unreferenced) — expected");
        if (outFieldIds != inFieldIds) throw new RuntimeException("field_ids changed (got " + outFieldIds + ", expected " + inFieldIds + ")");
    }

    static class Module extends RewriterModule {
        final MethodImplementation finImpl;
        Module(MethodImplementation f) { finImpl = f; }
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
    }
}
