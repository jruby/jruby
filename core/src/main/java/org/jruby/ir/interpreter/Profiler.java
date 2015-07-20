package org.jruby.ir.interpreter;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.Counter;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;

import java.util.*;

/**
 * Definitions in profiler:
 *   instruction tick - called for every significant instr in the system.  Significant currently means instrs which
 *      are ideally recognized as part of bootstrapping.
 *   clock tick - hotness profiler granularity (currently denoted as # of thread_poll instrs).
 *   period - how long between attempts to analyze collected stats (PROFILE_PERIOD).  This is number of clock ticks.
 */
public class Profiler {
    private static final int PROFILE_PERIOD = 20000;

    public static class IRCallSite {
        IRScope scope; // where call resides in
        CallBase call; // callsite
        long count;   // how many times call has occurred
        MixedModeIRMethod tgtM;

        public IRCallSite() {}

        public IRCallSite(IRCallSite cs) {
            this.scope = cs.scope;
            this.call  = cs.call;
            this.count = 0;
        }

        public int hashCode() {
            return (int)this.call.callSiteId;
        }

        public void update(Instr call, IRScope scope) {
            this.scope = scope;
            this.call = (CallBase)call;
        }
    }

    private static class CallSiteProfile {
        IRCallSite cs;
        HashMap<IRScope, Counter> counters;

        public CallSiteProfile(IRCallSite cs) {
            this.cs = new IRCallSite(cs);
            this.counters = new HashMap<>();
        }
    }

    // Last or about to be called IR scope
    public static IRCallSite callerSite = new IRCallSite();

    private static int inlineCount = 0;
    private static int clockCount = 0;
    private static int codeModificationsCount = 0;
    private static int numCyclesWithNoModifications = 0;
    private static int versionCount = 1;

    private static HashMap<IRScope, Integer> scopeVersionMap = new HashMap<>();
    private static HashMap<IRScope, Counter> scopeThreadPollCounts = new HashMap<>();
    private static HashMap<Long, CallSiteProfile> callProfile = new HashMap<>();
    private static HashMap<Operation, Counter> opStats = new HashMap<>();

    private static final int NUMBER_OF_NON_MODIFYING_EXECUTIONS = 3;

    /*
     * Have we seen enough new method churn to start looking for hot methods?  We defer
     * looking for hot methods too quickly by examining rate of change of new methods
     * coming in.   Note: We should consider whether we want to plug this so we can play
     * with different mechanisms.
     */
    private static boolean isStillBootstrapping() {
        if (codeModificationsCount == 0) {
            numCyclesWithNoModifications++;
        } else {
            numCyclesWithNoModifications = 0;
        }

        codeModificationsCount = 0;

        return numCyclesWithNoModifications < NUMBER_OF_NON_MODIFYING_EXECUTIONS;
    }

    private static void analyzeProfile() {
        versionCount++;

        System.out.println("CMC: " + codeModificationsCount + ", NMWNM: " + numCyclesWithNoModifications);
        if (isStillBootstrapping()) return;

//         System.out.println("-------------------start analysis-----------------------");

        final HashMap<IRScope, Long> scopeCounts = new HashMap<>();
        final ArrayList<IRCallSite> callSites = new ArrayList<>();
        HashMap<IRCallSite, Long> callSiteCounts = new HashMap<>();
        // System.out.println("# call sites: " + callProfile.keySet().size());
        long total = 0;
        for (Long id: callProfile.keySet()) {
            Long c;

            CallSiteProfile csp = callProfile.get(id);
            IRCallSite      cs  = csp.cs;

            Set<IRScope> calledScopes = csp.counters.keySet();
            cs.count = 0;
            for (IRScope s: calledScopes) {
                c = scopeCounts.get(s);
                if (c == null) {
                    c = new Long(0);
                    scopeCounts.put(s, c);
                }

                long x = csp.counters.get(s).count;
                c += x;
                cs.count += x;
            }

            CallBase call = cs.call;
            if (calledScopes.size() == 1 && !call.inliningBlocked()) {
                CallSite runtimeCS = call.getCallSite();
                if (runtimeCS != null && (runtimeCS instanceof CachingCallSite)) {
                    CachingCallSite ccs = (CachingCallSite)runtimeCS;
                    CacheEntry ce = ccs.getCache();

                    if (!(ce.method instanceof MixedModeIRMethod)) {
                        // System.out.println("NOT IR-M!");
                        continue;
                    } else {
                        callSites.add(cs);
                        cs.tgtM = (MixedModeIRMethod)ce.method;
                    }
                }
            }

            total += cs.count;
        }

        Collections.sort(callSites, new java.util.Comparator<IRCallSite> () {
            @Override
            public int compare(IRCallSite a, IRCallSite b) {
                if (a.count == b.count) return 0;
                return (a.count < b.count) ? 1 : -1;
            }
        });

        // Find top N call sites
        double freq = 0.0;
        int i = 0;
        boolean noInlining = true;
        Set<IRScope> inlinedScopes = new HashSet<>();
        for (IRCallSite ircs: callSites) {
            double contrib = (ircs.count*100.0)/total;

            // 1% is arbitrary
            if (contrib < 1.0) break;

            i++;
            freq += contrib;

            // This check is arbitrary
            if (i == 100 || freq > 99.0) break;

            System.out.println("Considering: " + ircs.call + " with id: " + ircs.call.callSiteId +
            " in scope " + ircs.scope + " with count " + ircs.count + "; contrib " + contrib + "; freq: " + freq);

            // Now inline here!
            CallBase call = ircs.call;

            IRScope hs = ircs.scope;
            boolean isHotClosure = hs instanceof IRClosure;
            IRScope hc = isHotClosure ? hs : null;
            hs = isHotClosure ? hs.getLexicalParent() : hs;

            IRScope tgtMethod = ircs.tgtM.getIRScope();

            Instr[] instrs = tgtMethod.getInterpreterContext().getInstructions();
            // Dont inline large methods -- 500 is arbitrary
            // Can be null if a previously inlined method hasn't been rebuilt
            if ((instrs == null) || instrs.length > 500) {
                if (instrs == null) System.out.println("no instrs!");
                else System.out.println("large method with " + instrs.length + " instrs. skipping!");
                continue;
            }

            RubyModule implClass = ircs.tgtM.getImplementationClass();
            int classToken = implClass.getGeneration();
            String n = tgtMethod.getName();
            boolean inlineCall = true;
            if (isHotClosure) {
                Operand clArg = call.getClosureArg(null);
                inlineCall = (clArg instanceof WrappedIRClosure) && (((WrappedIRClosure)clArg).getClosure() == hc);
            }
/*
            if (inlineCall) {
                noInlining = false;
                long start = new java.util.Date().getTime();
                hs.inlineMethod(tgtMethod, implClass, classToken, null, call, !inlinedScopes.contains(hs));
                inlinedScopes.add(hs);
                long end = new java.util.Date().getTime();
                // System.out.println("Inlined " + tgtMethod + " in " + hs +
                //     " @ instr " + call + " in time (ms): "
                //     + (end-start) + " # instrs: " + instrs.length);

                inlineCount++;
            } else {
                //System.out.println("--no inlining--");
            }
*/
        }

        for (IRScope x: inlinedScopes) {
            // Update version count for inlined scopes
            scopeVersionMap.put(x, versionCount);
            // System.out.println("Updating version of " + x + " to " + versionCount);
            //System.out.println("--- pre-inline-instrs ---");
            //System.out.println(x.getCFG().toStringInstrs());
            //System.out.println("--- post-inline-instrs ---");
            //System.out.println(x.getCFG().toStringInstrs());
        }

        // Reset
        codeModificationsCount = 0;
        callProfile = new HashMap<Long, CallSiteProfile>();

        // Every 1M thread polls, discard stats
        if (clockCount % 1000000 == 0)  {
            clockCount = 0;
        }
    }

    private static void outputProfileStats() {
        ArrayList<IRScope> scopes = new ArrayList<IRScope>(scopeThreadPollCounts.keySet());
        Collections.sort(scopes, new java.util.Comparator<IRScope> () {
            @Override
            public int compare(IRScope a, IRScope b) {
                // In non-methods and non-closures, we may not have any thread poll instrs.
                int aden = a.getThreadPollInstrsCount();
                if (aden == 0) aden = 1;
                int bden = b.getThreadPollInstrsCount();
                if (bden == 0) bden = 1;

                // Use estimated instr count to order scopes -- rather than raw thread-poll count
                float aCount = scopeThreadPollCounts.get(a).count * (1.0f * a.getInterpreterContext().getInstructions().length/aden);
                float bCount = scopeThreadPollCounts.get(b).count * (1.0f * b.getInterpreterContext().getInstructions().length/bden);
                if (aCount == bCount) return 0;
                return (aCount < bCount) ? 1 : -1;
            }
        });


        /*
        LOG.info("------------------------");
        LOG.info("Stats after " + clockCount + " thread polls:");
        LOG.info("------------------------");
        LOG.info("# instructions: " + interpInstrsCount);
        LOG.info("# code modifications in this period : " + codeModificationsCount);
        LOG.info("------------------------");
        */
        int i = 0;
        float f1 = 0.0f;
        for (IRScope s: scopes) {
            long n = scopeThreadPollCounts.get(s).count;
            float p1 =  ((n*1000)/ clockCount)/10.0f;
            String msg = i + ". " + s + " [file:" + s.getFileName() + ":" + s.getLineNumber() + "] = " + n + "; (" + p1 + "%)";
            if (s instanceof IRClosure) {
                IRMethod m = s.getNearestMethod();
                //if (m != null) LOG.info(msg + " -- nearest enclosing method: " + m);
                //else LOG.info(msg + " -- no enclosing method --");
            } else {
                //LOG.info(msg);
            }

            i++;
            f1 += p1;

            // Top 20 or those that account for 95% of thread poll events.
            if (i == 20 || f1 >= 95.0) break;
        }

        // reset code modification counter
        codeModificationsCount = 0;

        // Every 1M thread polls, discard stats by reallocating the thread-poll count map
         if (clockCount % 1000000 == 0)  {
            //System.out.println("---- resetting thread-poll counters ----");
            scopeThreadPollCounts = new HashMap<IRScope, Counter>();
            clockCount = 0;
        }
    }

    public static int initProfiling(IRScope scope) {
        /* SSS: Not being used currently
        tpCount = scopeThreadPollCounts.get(scope);
        if (tpCount == null) {
            tpCount = new Counter();
            scopeThreadPollCounts.put(scope, tpCount);
        }
        */

        Integer scopeVersion = scopeVersionMap.get(scope);
        if (scopeVersion == null) {
            scopeVersionMap.put(scope, versionCount);
            scopeVersion = new Integer(versionCount);
        }

        if (callerSite.call != null) {
            Long id = callerSite.call.callSiteId;
            CallSiteProfile csp = callProfile.get(id);
            if (csp == null) {
                csp = new CallSiteProfile(callerSite);
                callProfile.put(id, csp);
            }

            Counter csCount = csp.counters.get(scope);
            if (csCount == null) {
                csCount = new Counter();
                csp.counters.put(scope, csCount);
            }
            csCount.count++;
        }

        return scopeVersion;
    }

    // We do not pass profiling instructions through call so we temporarily tuck away last IR executed call in Profiler.
    public static void markCallAboutToBeCalled(CallBase call, IRScope scope) {
        callerSite.call = call;
        callerSite.scope = scope;
    }

    public static void clockTick() {
        // tpCount.count++; // SSS: Not being used currently
        if (clockCount++ % PROFILE_PERIOD == 0) analyzeProfile();
    }

    public static void instrTick(Operation operation) {
        if (operation.modifiesCode()) codeModificationsCount++;
        /*
        Counter cnt = opStats.get(operation);
        if (cnt == null) {
            cnt = new Counter();
            opStats.put(operation, cnt);
        }
        cnt.count++;
        */
    }
}
