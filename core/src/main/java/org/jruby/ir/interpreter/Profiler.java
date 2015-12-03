package org.jruby.ir.interpreter;

import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.Counter;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.callsite.CachingCallSite;

import java.util.*;

/**
 * Definitions in profiler:
 *   modification tick - called for every instr in the system which changes the structure of the program (e.g. def,
 *   class, alias, undef...).  Purpose is to detect when things are settling down (most programs are not endlessly
 *   modifying their applications).
 *   clock tick - hotness profiler granularity (currently denoted as # of thread_poll instrs).
 *   period - how long between attempts to analyze collected stats (PROFILE_PERIOD).  This is number of clock ticks.
 *
 * Basics of this profiler as-of now (currently only works with interpreters).  It will record a call site
 * before invoking any call.  In the interpreter executing the method from the site just saved, it will look for this
 * saved site and then record which scope the site ended up invoking.  All saved sites are store globally and every
 * so many stable periods (e.g. not when lots of definitions are changing) it will look for most active monomorphic
 * sites.  It might inline some of those and then it will flush collected data and start over.
 */
public class Profiler {
    public static final int UNASSIGNED_VERSION = -1;
    private static final int PROFILE_PERIOD = 20000;
    private static final float INSIGNIFICANT_PERCENTAGE = 1.0f; // FIXME: arbitrarily chosen

    // Structure on what a callsite is.  It lives in an IC. It will be some form of call (CallBase).
    // It might have been called count times.
    public static class IRCallSite {
        InterpreterContext ic;  // ic where this call site lives
        CallBase call;          // which instr is at this callsite
        long count;             // how many times callsite has been executed
        Compilable liveMethod;  // winner winner chicken dinner we think we have monomorphic target method to inline.

        public IRCallSite() {}

        public IRCallSite(IRCallSite cs) {
            ic = cs.ic;
            call  = cs.call;
            count = 0;
        }

        public int hashCode() {
            return (int) call.callSiteId;
        }

        public void update(CallBase call, InterpreterContext ic) {
            this.ic = ic;
            this.call = call;
        }
    }

    // ENEBO: I believe this need not retain IRCallSite as it involves a copy constructor to work. CallsitePofile
    // is-a IRCallSite with extra info on which types are calling it.

    // For an individual callsite how many methods is this callsite calling?
    private static class CallSiteProfile {
        IRCallSite cs;
        HashMap<IRScope, Counter> counters;

        public CallSiteProfile(IRCallSite cs) {
            this.cs = new IRCallSite(cs);
            this.counters = new HashMap<>();
        }

        /**
         * Recalculate total number of times this callsite has been hit and also return
         * whether this happens to be a monomorphic site.
         */
        public boolean retallyCallCount() {
            long count = 0;

            for (IRScope s: counters.keySet()) {
                count += counters.get(s).count;
            }

            cs.count = count;

            return counters.size() == 1;
        }
    }

    // Last or about to be called IR scope
    public static IRCallSite callerSite = new IRCallSite();

    private static int globalClockCount = 0;

    // How many code modifications happens during this period?
    private static int codeModificationsCount = 0;
    // If we have enough periods without changes occurring we start getting serious about compiling.
    private static int periodsWithoutChanges = 0;
    private static int versionCount = 1;

    private static HashMap<Long, CallSiteProfile> callProfile = new HashMap<>();

    private static final int NUMBER_OF_NON_MODIFYING_EXECUTIONS = 3;

    /**
     * Have we seen enough new modification churn to start looking for hot methods/closures?
     * We defer looking for hot methods too quickly by examining rate of change of new methods
     * coming in.
     */
    private static boolean isStillBootstrapping() {
        if (codeModificationsCount == 0) {
            periodsWithoutChanges++;
        } else {
            periodsWithoutChanges = 0;
        }

        codeModificationsCount = 0;

        return periodsWithoutChanges < NUMBER_OF_NON_MODIFYING_EXECUTIONS;
    }

    /**
     * Examine callProfiles looking for eligible monomorphic callsites.  Return total number of calls
     * executed from the profile.
     *
     * @param callSites out param of eligible callsites
     * @return total number of calls executed by all entries in the callprofile
     */
    private static long findInliningCandidates(List<IRCallSite> callSites) {
        long total = 0;   // Total number of calls found in this scope.

        // Register all monomorphic found callsites which are eligible for inlining.
        for (Long id: callProfile.keySet()) {
            CallSiteProfile callSiteProfile = callProfile.get(id);
            IRCallSite callSite  = callSiteProfile.cs;
            boolean monomorphic = callSiteProfile.retallyCallCount();

            if (monomorphic && !callSite.call.inliningBlocked()) {
                CallSite runtimeCallSite = callSite.call.getCallSite();
                if (runtimeCallSite != null && runtimeCallSite instanceof CachingCallSite) {
                    DynamicMethod method = ((CachingCallSite) runtimeCallSite).getCache().method;

                    if (!(method instanceof Compilable) || !((Compilable) method).getIRScope().isFullBuildComplete()) continue;

                    callSites.add(callSite);
                    callSite.liveMethod = (Compilable) method;
                }
            }

            total += callSite.count;
        }

        return total;
    }

    private static void analyzeProfile() {
        //System.out.println("MOD COUNT: " + codeModificationsCount + ", Periods wo change: " + periodsWithoutChanges);
        // Don't bother doing any analysis until we see the system start to settle down from lots of modifications.
        if (isStillBootstrapping()) return;

        versionCount++;

        final ArrayList<IRCallSite> callSites = new ArrayList<>();
        long total = findInliningCandidates(callSites);

        //System.out.println("Total calls this period: " + total + ", candidate callsites: " + callSites.size());

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
        Set<InterpreterContext> inlinedScopes = new HashSet<>();
        for (IRCallSite callSite: callSites) {
            double percentOfTotalCalls = (callSite.count * 100.0) / total;

            if (percentOfTotalCalls < INSIGNIFICANT_PERCENTAGE) break;

            i++;
            freq += percentOfTotalCalls;

            // This check is arbitrary
            if (i == 100 || freq > 99.0) break;

            //System.out.println("Considering: " + ircs.call + " with id: " + ircs.call.callSiteId +
            //" in scope " + ircs.ic.getScope() + " with count " + ircs.count + "; contrib " + contrib + "; freq: " + freq);

            // Now inline here!
            CallBase call = callSite.call;

            InterpreterContext ic = callSite.ic;
            boolean isClosure = ic.getScope() instanceof IRClosure;

            // This has several of assumptions in it:
            // 1. nothing hot could ever not exist in a non-fully built parent scope so FIC is available.  This assumption cannot be true
            // 2. if we ever have three ICs (startup, full, profiled) [or more than three] then we can:
            //    a. use full and ignore profiled
            //    b. use profiled (or last profiled in case more multiple profiled versions)
            ic = isClosure ? ic.getScope().getLexicalParent().getFullInterpreterContext() : ic;

            Compilable tgtMethod = callSite.liveMethod;

            // MOVE INTO shouldInline
            Instr[] instrs = tgtMethod.getIRScope().getFullInterpreterContext().getInstructions();
            // Dont inline large methods -- 500 is arbitrary
            // Can be null if a previously inlined method hasn't been rebuilt
            if ((instrs == null) || instrs.length > 500) {
                if (instrs == null) System.out.println("no instrs!");
                else System.out.println("large method with " + instrs.length + " instrs. skipping!");
                continue;
            }

            if (shouldInline(call, ic, isClosure)) {
                RubyModule implClass = callSite.liveMethod.getImplementationClass();
                long start = new java.util.Date().getTime();
                ic.getScope().inlineMethod(tgtMethod, implClass, implClass.getGeneration(), null, call, !inlinedScopes.contains(ic));
                inlinedScopes.add(ic);
                long end = new java.util.Date().getTime();
                System.out.println("Inlined " + tgtMethod + " in " + ic + " @ instr " + call + " in time (ms): " + (end-start) + " # instrs: " + instrs.length);
            }
        }

        for (InterpreterContext x: inlinedScopes) {
            x.setVersion(versionCount); // Update version count for inlined scopes
        }

        // Reset
        codeModificationsCount = 0;
        callProfile = new HashMap<>();

        // Every 1M thread polls, discard stats
        if (globalClockCount % 1000000 == 0)  globalClockCount = 0;
    }

    /**
     * All methods will inline so long as they have been fully built.  A hot closure will inline through the method
     * which call it.
     */
    private static boolean shouldInline(CallBase call, InterpreterContext ic, boolean isClosure) {
        // FIXME: Closure getting lexical parent can end up with null.  We should fix that in parent method to remove this null check.
        boolean fullBuild = ic != null && ic.getScope().isFullBuildComplete();

        if (isClosure) {
            Operand closureArg = call.getClosureArg(null);
            return fullBuild && closureArg instanceof WrappedIRClosure && ((WrappedIRClosure) closureArg).getClosure() == ic.getScope();
        }

        return fullBuild;
    }

    public static void initProfiling(IRScope scope) {
        //int scopeVersion = scope.getFullInterpreterContext().getVersion();
        //if (scopeVersion == UNASSIGNED_VERSION) ic.setVersion(versionCount);

        // FIXME: I think there is a bug here.  If we call IR -> native -> IR then this may still be set and it will
        // be inaccurate to record this save callsite info with the currently executing scope.

        // This allows us to register the last callsite and associate it with the current scope.
        if (callerSite.call != null) {
            Long id = callerSite.call.callSiteId;         // we find id to look up callsite in global table (global to flush?)
            CallSiteProfile csp = callProfile.get(id);    // get saved profile
            if (csp == null) {                            // of make one
                csp = new CallSiteProfile(callerSite);
                callProfile.put(id, csp);
            }

            Counter csCount = csp.counters.get(scope);    // store which method is getting called (local to site)
            if (csCount == null) {                        // make new counter
                csCount = new Counter();
                csp.counters.put(scope, csCount);
            }
            csCount.count++;                              // this particular method was called one more time
        }
    }

    // We do not pass profiling instructions through call so we temporarily tuck away last IR executed
    // call in Profiler.
    public static void markCallAboutToBeCalled(CallBase call, InterpreterContext ic) {
        callerSite.update(call, ic);
    }

    public static void clockTick() {
        if (globalClockCount++ % PROFILE_PERIOD == 0) analyzeProfile();
    }

    public static void modificationTick() {
        codeModificationsCount++;
    }
}