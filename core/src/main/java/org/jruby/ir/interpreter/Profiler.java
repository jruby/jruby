package org.jruby.ir.interpreter;

import org.jruby.RubyModule;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.Counter;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.JIT;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.callsite.CachingCallSite;

import java.util.*;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

/**
 * Definitions in profiler:
 *   modification tick - called for every instr in the system which changes the structure of the program (e.g. def,
 *   class, alias, undef...).  Purpose is to detect when things are settling down (most programs are not endlessly
 *   modifying their applications).
 *   clock tick - hotness profiler granularity (currently denoted as # of thread_poll instrs).
 *   period - how long between attempts to analyze collected stats (PROFILE_PERIOD).  This is number of clock ticks.
 *
 * Basics of this profiler as-of now.  It will record a call site before invoking any call.  At all calls, we will
 * record the callsite (id in JIT and callbase in interpreter).  When we enter the called scope we then record this
 * reached scope with the last recorded callsite.  All saved sites are store globally and every so many stable
 * periods (e.g. not when lots of definitions are changing) it will look for most active monomorphic sites.  It
 * might inline some of those and then it will flush collected data and start over.
 */
public class Profiler {
    private static final Logger LOG = LoggerFactory.getLogger(Profiler.class);

    public static final long INVALID_CALLSITE_ID = -1;
    public static final int UNASSIGNED_VERSION = -1;
    private static final int PROFILE_PERIOD = 20000;
    private static final float INSIGNIFICANT_PERCENTAGE = 1.0f; // FIXME: arbitrarily chosen

    private static final int MAX_NUMBER_OF_INTERESTING_CALLSITES = 10; // We will only look at top n sites

    // FIXME: This is a bad statistic by itself.  In a super tight loop the hottest site might be >99% of the freq.
    private static final float MAX_FREQUENCY = 100.0f; // After we covered this much of the space we do not care

    private static final int MAX_INSTRUCTIONS_TO_INLINE = 500;

    // Structure on what a callsite is.  It lives in an IC. It will be some form of call (CallBase).
    // It might have been called count times.
    public static class IRCallSite {
        IRScope scope;                  // Scope where callsite is located
        long id = INVALID_CALLSITE_ID;  // Callsite id (unique to system)
        private CallBase call = null;   // which instr is at this callsite
        long count;                     // how many times callsite has been executed
        Compilable liveMethod;          // winner winner chicken dinner we think we have monomorphic target method to inline.

        public IRCallSite() {}

        public IRCallSite(IRCallSite cs) {
            scope = cs.scope;
            id = cs.id;
            call  = cs.call;
            count = 0;
        }

        /**
         * Interpreter saves call but JIT only has the callsiteId and must find it based on that id.
         */
        public CallBase getCall() {
            if (call == null) call = findCall();
            return call;
        }

        private CallBase findCall() {
            FullInterpreterContext fic = scope.getFullInterpreterContext();
            for (BasicBlock bb: fic.getLinearizedBBList()) {
                for (Instr instr: bb.getInstrs()) {
                    if (instr instanceof CallBase && ((CallBase) instr).getSiteId() == id) return (CallBase) instr;
                }
            }

            return null;
        }

        public int hashCode() {
            return (int) call.getSiteId();
        }

        public void update(long callSiteId, IRScope scope) {
            this.scope = scope;
            this.id = callSiteId;
        }

        public void update(CallBase call, IRScope scope) {
            this.scope = scope;
            this.id = call.getSiteId();
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

    static class CallSiteComparator implements Comparator<IRCallSite> {
        @Override
        public int compare(IRCallSite a, IRCallSite b) {
            if (a.count == b.count) return 0;
            return (a.count < b.count) ? 1 : -1;
        }
    }

    static CallSiteComparator callSiteComparator = new CallSiteComparator();

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

        if (Options.IR_PROFILE_DEBUG.load()) LOG.info("begin findInliningCandidate {} candidates to examine.", callProfile.size());

        // Register all monomorphic found callsites which are eligible for inlining.
        for (Long id: callProfile.keySet()) {
            CallSiteProfile callSiteProfile = callProfile.get(id);
            IRCallSite callSite  = callSiteProfile.cs;
            boolean monomorphic = callSiteProfile.retallyCallCount();
            CallBase call = callSite.getCall();

            if (Options.IR_PROFILE_DEBUG.load()) {
                LOG.info("id: {}, # of calls: {}, # of targets: {}", callSite.id, callSite.count, callSiteProfile.counters.size());
            }

            // FIXME: Why is call sometimes null?
            if (monomorphic && call != null && !call.inliningBlocked() && !callSite.scope.inliningAllowed()) {
                IRScope inlineScope = callSiteProfile.counters.keySet().iterator().next();

                if (Options.IR_PROFILE_DEBUG.load()) LOG.info("  mono scope: {}", inlineScope);

                Compilable compilable = inlineScope.getCompilable();
                if (compilable != null && compilable instanceof CompiledIRMethod || compilable instanceof MixedModeIRMethod) {
                    if (callSite.scope.getCompilable() == null) {
                        if (Options.IR_PROFILE_DEBUG.load()) {
                            if (callSite.scope instanceof IREvalScript) {
                                LOG.info("    rejected: scope in script body [JITTED]");
                            } else {
                                LOG.info("    rejected: no parent compilable found [JITTED]");
                            }
                        }
                        continue;
                    }

                    if (Options.IR_PROFILE_DEBUG.load()) LOG.info("    aceepted [JITTED] {}", call);
                    callSites.add(callSite);
                    callSite.liveMethod = compilable;
                } else {  // Interpreter
                    CallSite runtimeCallSite = callSite.getCall().getCallSite();
                    if (runtimeCallSite != null && runtimeCallSite instanceof CachingCallSite) {
                        DynamicMethod method = ((CachingCallSite) runtimeCallSite).getCache().method;

                        if (!(method instanceof Compilable)) {
                            if (Options.IR_PROFILE_DEBUG.load()) LOG.info("    rejected: no parent compilable found [INTERPD]");
                            continue;
                        }

                        if (!areScopesFullyBuilt(callSite.scope, (Compilable) method)) {
                            if (Options.IR_PROFILE_DEBUG.load()) LOG.info("    rejected: still startup scope [INTERPD]");
                            continue;
                        }

                        if (Options.IR_PROFILE_DEBUG.load()) LOG.info("    aceepted [INTERPD]");
                        callSites.add(callSite);
                        callSite.liveMethod = (Compilable) method;
                    }
                }
            }

            total += callSite.count;
        }

        if (Options.IR_PROFILE_DEBUG.load()) LOG.info("end findInliningCandidates: {} total calls found processed", total);

        return total;
    }

    private static boolean areScopesFullyBuilt(IRScope callingScope, Compilable liveMethod) {
        return liveMethod.getIRScope().isFullBuildComplete() && callingScope.isFullBuildComplete();
    }

    private static void analyzeProfile() {
        //System.out.println("MOD COUNT: " + codeModificationsCount + ", Periods wo change: " + periodsWithoutChanges);
        // Don't bother doing any analysis until we see the system start to settle down from lots of modifications.
        if (isStillBootstrapping()) return;

        versionCount++;

        final ArrayList<IRCallSite> callSites = new ArrayList<>();
        long total = findInliningCandidates(callSites);
        Collections.sort(callSites, callSiteComparator);

        if (Options.IR_PROFILE_DEBUG.load()) LOG.info("begin analyzeProfile: # of calls {} of callsites: {}", total, callSites.size());

        // Find top N call sites
        double freq = 0.0;
        int i = 0;
        Set<IRScope> inlinedScopes = new HashSet<>();
        for (IRCallSite callSite: callSites) {
            double percentOfTotalCalls = (callSite.count * 100.0) / total;

            if (percentOfTotalCalls < INSIGNIFICANT_PERCENTAGE) break;
            freq += percentOfTotalCalls;

            if (Options.IR_PROFILE_DEBUG.load()) LOG.info("  call: {}, % of calls: {}", callSite.call, percentOfTotalCalls);

            if (i++ >= MAX_NUMBER_OF_INTERESTING_CALLSITES) {
                if (Options.IR_PROFILE_DEBUG.load()) LOG.info("  rejected: too many callsite examined {}", i);
                break;
            }
            if (freq > MAX_FREQUENCY) {
                if (Options.IR_PROFILE_DEBUG.load()) LOG.info("  rejected: too high a freq {}", freq);
                break;
            }


            //System.out.println("Considering: " + callSite.call + " with id: " + callSite.call.getSiteId() +
            //" in scope " + callSite.scope + " with count " + callSite.count + "; % of calls: " + percentOfTotalCalls +
            //        "; freq: " + freq);

            if (callSite.scope.getFullInterpreterContext() == null) continue;

            InterpreterContext parentIC = callSite.scope.getFullInterpreterContext();
            boolean isClosure = parentIC.getScope() instanceof IRClosure;

            // This has several of assumptions in it:
            // 1. nothing hot could ever not exist in a non-fully built parent scope so FIC is available.  This assumption cannot be true
            // 2. if we ever have three ICs (startup, full, profiled) [or more than three] then we can:
            //    a. use full and ignore profiled
            //    b. use profiled (or last profiled in case more multiple profiled versions)
            parentIC = isClosure ? parentIC.getScope().getLexicalParent().getFullInterpreterContext() : parentIC;

            // For now we are not inlining into anything but a method
            if (parentIC == null || !(parentIC.getScope() instanceof IRMethod)) {
                if (Options.IR_PROFILE_DEBUG.load()) LOG.info("  rejected: can only inline into a method");
                continue;
            }

            Compilable methodToInline = callSite.liveMethod;

            if (methodToInline instanceof CompiledIRMethod || methodToInline instanceof MixedModeIRMethod) {
                if (Options.IR_PROFILE_DEBUG.load()) LOG.info("Inlining " + methodToInline.getName() + " into " + parentIC.getName());
                IRScope scope = ((AbstractIRMethod) methodToInline).getIRScope();
                CallBase call;
                // If we are in same batch of interesting callsites then the underlying scope has already
                // cloned any other interesting callsites.
                if (inlinedScopes.contains(callSite.scope)) {
                    call = callSite.findCall();
                } else {
                    call = callSite.getCall();
                }
                RubyModule implClass = methodToInline.getImplementationClass();
                long start = new java.util.Date().getTime();
                parentIC.getScope().inlineMethodJIT(methodToInline, implClass, implClass.getGeneration(), null, call, false);//!inlinedScopes.contains(ic));
                inlinedScopes.add(parentIC.getScope());
                long end = new java.util.Date().getTime();
                if (Options.IR_PROFILE_DEBUG.load()) LOG.info("Inlined " + methodToInline.getName() + " into " + parentIC.getName() + " @ instr " + callSite.getCall() +
                        " in time (ms): " + (end - start));

            } else if (methodToInline instanceof Compilable) {
                IRScope scope = (methodToInline).getIRScope();
                if (shouldInline(scope, callSite.getCall(), parentIC, isClosure)) {
                    RubyModule implClass = methodToInline.getImplementationClass();
                    long start = new java.util.Date().getTime();
                    parentIC.getScope().inlineMethod(methodToInline, implClass, implClass.getGeneration(), null, callSite.getCall(), false);//!inlinedScopes.contains(ic));
                    inlinedScopes.add(parentIC.getScope());
                    long end = new java.util.Date().getTime();
                    if (Options.IR_PROFILE_DEBUG.load()) LOG.info("Inlined " + methodToInline.getName() + " into " + parentIC.getName() + " @ instr " + callSite.getCall() +
                            " in time (ms): " + (end - start) + " # of inlined instrs: " +
                            scope.getFullInterpreterContext().getInstructions().length);
                }
            }
        }

        //for (IRScope x: inlinedScopes) {
//            x.setVersion(versionCount); // Update version count for inlined scopes
//        }

        // Reset
        codeModificationsCount = 0;
        callProfile = new HashMap<>();

        // Every 1M thread polls, discard stats
        if (globalClockCount % 1000000 == 0)  globalClockCount = 0;

        if (Options.IR_PROFILE_DEBUG.load()) LOG.info("end analyzCallsites");
    }

    /**
     * All methods will inline so long as they have been fully built.  A hot closure will inline through the method
     * which call it.
     */
    private static boolean shouldInline(IRScope scopeToInline, CallBase call, InterpreterContext parentIC, boolean isClosure) {
        Instr[] instrs = scopeToInline.getFullInterpreterContext().getInstructions();
        if (instrs == null || instrs.length > MAX_INSTRUCTIONS_TO_INLINE) return false;

        // FIXME: Closure getting lexical parent can end up with null.  We should fix that in parent method to remove this null check.
        boolean fullBuild = parentIC != null && parentIC.getScope().isFullBuildComplete();

        if (isClosure) {
            Operand closureArg = call.getClosureArg(null);
            return fullBuild && closureArg instanceof WrappedIRClosure && ((WrappedIRClosure) closureArg).getClosure() == parentIC.getScope();
        }

        return fullBuild;
    }

    public static void initProfiling(IRScope scope) {
        //int scopeVersion = scope.getFullInterpreterContext().getVersion();
        //if (scopeVersion == UNASSIGNED_VERSION) ic.setVersion(versionCount);

        // FIXME: JIT is somehow registering callProfile from original call and registering itself
        // as a called scope.  Why does only the JIT cause this?
        if (scope instanceof IRClosure) return;

        // FIXME: I think there is a bug here.  If we call IR -> native -> IR then this may still be set and it will
        // be inaccurate to record this save callsite info with the currently executing scope.

        // This allows us to register the last callsite and associate it with the current scope.
        if (callerSite.id != -1) {
            CallSiteProfile csp = callProfile.get(callerSite.id);  // get saved profile
            if (csp == null) {                                     // or make one
                csp = new CallSiteProfile(callerSite);
                callProfile.put(callerSite.id, csp);
            }

            Counter csCount = csp.counters.get(scope);    // store which method is getting called (local to site)
            if (csCount == null) {                        // make new counter
                csCount = new Counter();
                csp.counters.put(scope, csCount);
            }
            csCount.count++;                              // this particular method was called one more time
        }
    }

    @JIT
    public static void markCallAboutToBeCalled(long callsiteId, IRScope scope) {
        callerSite.call = null;
        callerSite.update(callsiteId, scope);
    }

    // We do not pass profiling instructions through call so we temporarily tuck away last IR executed
    // call in Profiler.
    public static void markCallAboutToBeCalled(CallBase call, InterpreterContext ic) {
        callerSite.update(call, ic.getScope());
    }

    public static void clockTick() {
        if (globalClockCount++ % PROFILE_PERIOD == 0) analyzeProfile();
    }

    public static void modificationTick() {
        codeModificationsCount++;
    }
}
