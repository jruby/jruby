package org.jruby.ir.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.Counter;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Operation;
import org.jruby.ir.OpClass;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.RecordEndBlockInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.ruby19.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ruby20.ReceiveKeywordArgInstr;
import org.jruby.ir.instructions.ruby20.ReceiveKeywordRestArgInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class Interpreter {
    private static class IRCallSite {
        IRScope  s;
        int      v; // scope version
        CallBase call;
        long     count;
        InterpretedIRMethod tgtM;

        public IRCallSite() { }

        public IRCallSite(IRCallSite cs) {
            this.s     = cs.s;
            this.v     = cs.v;
            this.call  = cs.call;
            this.count = 0;
        }

        public int hashCode() {
            return (int)this.call.callSiteId;
        }
    }

    private static class CallSiteProfile {
        IRCallSite cs;
        HashMap<IRScope, Counter> counters;

        public CallSiteProfile(IRCallSite cs) {
            this.cs = new IRCallSite(cs);
            this.counters = new HashMap<IRScope,Counter>();
        }
    }

    private static IRCallSite callerSite = new IRCallSite();

    private static final Logger LOG = LoggerFactory.getLogger("Interpreter");

    private static int versionCount = 1;
    private static HashMap<IRScope, Integer> scopeVersionMap = new HashMap<IRScope, Integer>();

    private static int inlineCount = 0;
    private static int interpInstrsCount = 0;
    private static int codeModificationsCount = 0;
    private static int numCyclesWithNoModifications = 0;
    private static int globalThreadPollCount = 0;
    private static HashMap<IRScope, Counter> scopeThreadPollCounts = new HashMap<IRScope, Counter>();
    private static HashMap<Long, CallSiteProfile> callProfile = new HashMap<Long, CallSiteProfile>();
    private static HashMap<Operation, Counter> opStats = new HashMap<Operation, Counter>();

    private static IRScope getEvalContainerScope(Ruby runtime, StaticScope evalScope) {
        // SSS FIXME: Weirdness here.  We cannot get the containing IR scope from evalScope because of static-scope wrapping
        // that is going on
        // 1. In all cases, DynamicScope.getEvalScope wraps the executing static scope in a new local scope.
        // 2. For instance-eval (module-eval, class-eval) scenarios, there is an extra scope that is added to
        //    the stack in ThreadContext.java:preExecuteUnder
        // I dont know what rule to apply when.  However, in both these cases, since there is no IR-scope associated,
        // I have used the hack below where I first unwrap once and see if I get a non-null IR scope.  If that doesn't
        // work, I unwarp once more and I am guaranteed to get the IR scope I want.
        IRScope containingIRScope = ((IRStaticScope)evalScope.getEnclosingScope()).getIRScope();
        if (containingIRScope == null) containingIRScope = ((IRStaticScope)evalScope.getEnclosingScope().getEnclosingScope()).getIRScope();
        return containingIRScope;
    }

    public static IRubyObject interpretCommonEval(Ruby runtime, String file, int lineNumber, String backtraceName, RootNode rootNode, IRubyObject self, Block block) {
        // SSS FIXME: Is this required here since the IR version cannot change from eval-to-eval? This is much more of a global setting.
        boolean is_1_9 = runtime.is1_9();
        if (is_1_9) IRBuilder.setRubyVersion("1.9");

        StaticScope ss = rootNode.getStaticScope();
        IRScope containingIRScope = getEvalContainerScope(runtime, ss);
        IREvalScript evalScript = IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildEvalRoot(ss, containingIRScope, file, lineNumber, rootNode);
        evalScript.prepareForInterpretation(false);
//        evalScript.runCompilerPass(new CallSplitter());
        ThreadContext context = runtime.getCurrentContext();
        runBeginEndBlocks(evalScript.getBeginBlocks(), context, self, null); // FIXME: No temp vars yet right?
        IRubyObject rv = evalScript.call(context, self, evalScript.getStaticScope().getModule(), rootNode.getScope(), block, backtraceName);
        runBeginEndBlocks(evalScript.getEndBlocks(), context, self, null); // FIXME: No temp vars right?
        return rv;
    }

    public static IRubyObject interpretSimpleEval(Ruby runtime, String file, int lineNumber, String backtraceName, Node node, IRubyObject self) {
        return interpretCommonEval(runtime, file, lineNumber, backtraceName, (RootNode)node, self, Block.NULL_BLOCK);
    }

    public static IRubyObject interpretBindingEval(Ruby runtime, String file, int lineNumber, String backtraceName, Node node, IRubyObject self, Block block) {
        return interpretCommonEval(runtime, file, lineNumber, backtraceName, (RootNode)node, self, block);
    }

    public static void runBeginEndBlocks(List<IRClosure> beBlocks, ThreadContext context, IRubyObject self, Object[] temp) {
        if (beBlocks == null) return;

        for (IRClosure b: beBlocks) {
            // SSS FIXME: Should I piggyback on WrappedIRClosure.retrieve or just copy that code here?
            b.prepareForInterpretation(false);
            Block blk = (Block)(new WrappedIRClosure(b)).retrieve(context, self, context.getCurrentScope(), temp);
            blk.yield(context, null);
        }
    }

    public static IRubyObject interpret(Ruby runtime, Node rootNode, IRubyObject self) {
        if (runtime.is1_9()) IRBuilder.setRubyVersion("1.9");

        IRScriptBody root = (IRScriptBody) IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildRoot((RootNode) rootNode);

        // We get the live object ball rolling here.  This give a valid value for the top
        // of this lexical tree.  All new scope can then retrieve and set based on lexical parent.
        if (root.getStaticScope().getModule() == null) { // If an eval this may already be setup.
            root.getStaticScope().setModule(runtime.getObject());
        }

        RubyModule currModule = root.getStaticScope().getModule();

        // Scope state for root?
        IRStaticScopeFactory.newIRLocalScope(null).setModule(currModule);
        ThreadContext context = runtime.getCurrentContext();

        try {
            runBeginEndBlocks(root.getBeginBlocks(), context, self, null); // FIXME: No temp vars yet...not needed?
            InterpretedIRMethod method = new InterpretedIRMethod(root, currModule);
            IRubyObject rv =  method.call(context, self, currModule, "(root)", IRubyObject.NULL_ARRAY);
            runBeginEndBlocks(root.getEndBlocks(), context, self, null); // FIXME: No temp vars yet...not needed?
            if ((IRRuntimeHelpers.isDebug() || IRRuntimeHelpers.inProfileMode()) && interpInstrsCount > 10000) {
                LOG.info("-- Interpreted instructions: {}", interpInstrsCount);
                /*
                for (Operation o: opStats.keySet()) {
                    System.out.println(o + " = " + opStats.get(o).count);
                }
                */
            }
            return rv;
        } catch (IRBreakJump bj) {
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
        }
    }

    private static void analyzeProfile() {
        versionCount++;

        //if (inlineCount == 2) return;

        if (codeModificationsCount == 0) numCyclesWithNoModifications++;
        else numCyclesWithNoModifications = 0;

        codeModificationsCount = 0;

        if (numCyclesWithNoModifications < 3) return;

        // We are now good to go -- start analyzing the profile

        // System.out.println("-------------------start analysis-----------------------");

        final HashMap<IRScope, Long> scopeCounts = new HashMap<IRScope, Long>();
        final ArrayList<IRCallSite> callSites = new ArrayList<IRCallSite>();
        HashMap<IRCallSite, Long> callSiteCounts = new HashMap<IRCallSite, Long>();
        // System.out.println("# call sites: " + callProfile.keySet().size());
        long total = 0;
        for (Long id: callProfile.keySet()) {
            Long c;

            CallSiteProfile csp = callProfile.get(id);
            IRCallSite      cs  = csp.cs;

            if (cs.v != scopeVersionMap.get(cs.s).intValue()) {
                // System.out.println("Skipping callsite: <" + cs.s + "," + cs.v + "> with compiled version: " + scopeVersionMap.get(cs.s));
                continue;
            }

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

                    if (!(ce.method instanceof InterpretedIRMethod)) {
                        // System.out.println("NOT IR-M!");
                        continue;
                    } else {
                        callSites.add(cs);
                        cs.tgtM = (InterpretedIRMethod)ce.method;
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
        Set<IRScope> inlinedScopes = new HashSet<IRScope>();
        for (IRCallSite ircs: callSites) {
            double contrib = (ircs.count*100.0)/total;

            // 1% is arbitrary
            if (contrib < 1.0) break;

            i++;
            freq += contrib;

            // This check is arbitrary
            if (i == 100 || freq > 99.0) break;

            // System.out.println("Considering: " + ircs.call + " with id: " + ircs.call.callSiteId +
            // " in scope " + ircs.s + " with count " + ircs.count + "; contrib " + contrib + "; freq: " + freq);

            // Now inline here!
            CallBase call = ircs.call;

            IRScope hs = ircs.s;
            boolean isHotClosure = hs instanceof IRClosure;
            IRScope hc = isHotClosure ? hs : null;
            hs = isHotClosure ? hs.getLexicalParent() : hs;

            IRScope tgtMethod = ircs.tgtM.getIRMethod();

            Instr[] instrs = tgtMethod.getInstrsForInterpretation();
            // Dont inline large methods -- 500 is arbitrary
            // Can be null if a previously inlined method hasn't been rebuilt
            if ((instrs == null) || instrs.length > 500) {
                // if (instrs == null) System.out.println("no instrs!");
                // else System.out.println("large method with " + instrs.length + " instrs. skipping!");
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

            if (inlineCall) {
                noInlining = false;
                long start = new java.util.Date().getTime();
                hs.inlineMethod(tgtMethod, implClass, classToken, null, call);
                inlinedScopes.add(hs);
                long end = new java.util.Date().getTime();
                // System.out.println("Inlined " + tgtMethod + " in " + hs +
                //     " @ instr " + call + " in time (ms): "
                //     + (end-start) + " # instrs: " + instrs.length);

                inlineCount++;
            } else {
                //System.out.println("--no inlining--");
            }
        }

        for (IRScope x: inlinedScopes) {
            // update version count for 'hs'
            scopeVersionMap.put(x, versionCount);
            // System.out.println("Updating version of " + x + " to " + versionCount);
            //System.out.println("--- pre-inline-instrs ---");
            //System.out.println(x.getCFG().toStringInstrs());
            //System.out.println("--- post-inline-instrs ---");
            //System.out.println(x.getCFG().toStringInstrs());
        }

        // reset
        codeModificationsCount = 0;
        callProfile = new HashMap<Long, CallSiteProfile>();

        // Every 1M thread polls, discard stats by reallocating the thread-poll count map
        if (globalThreadPollCount % 1000000 == 0)  {
            globalThreadPollCount = 0;
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
                float aCount = scopeThreadPollCounts.get(a).count * (1.0f * a.getInstrsForInterpretation().length/aden);
                float bCount = scopeThreadPollCounts.get(b).count * (1.0f * b.getInstrsForInterpretation().length/bden);
                if (aCount == bCount) return 0;
                return (aCount < bCount) ? 1 : -1;
            }
        });


        /*
        LOG.info("------------------------");
        LOG.info("Stats after " + globalThreadPollCount + " thread polls:");
        LOG.info("------------------------");
        LOG.info("# instructions: " + interpInstrsCount);
        LOG.info("# code modifications in this period : " + codeModificationsCount);
        LOG.info("------------------------");
        */
        int i = 0;
        float f1 = 0.0f;
        for (IRScope s: scopes) {
            long n = scopeThreadPollCounts.get(s).count;
            float p1 =  ((n*1000)/globalThreadPollCount)/10.0f;
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
         if (globalThreadPollCount % 1000000 == 0)  {
            //System.out.println("---- resetting thread-poll counters ----");
            scopeThreadPollCounts = new HashMap<IRScope, Counter>();
            globalThreadPollCount = 0;
        }
    }

    private static Integer initProfiling(IRScope scope) {
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

    private static void setResult(Object[] temp, DynamicScope currDynScope, Variable resultVar, Object result) {
        if (resultVar instanceof TemporaryVariable) {
            temp[((TemporaryVariable)resultVar).offset] = result;
        } else {
            LocalVariable lv = (LocalVariable)resultVar;
            currDynScope.setValue((IRubyObject)result, lv.getLocation(), lv.getScopeDepth());
        }
    }

    private static void setResult(Object[] temp, DynamicScope currDynScope, Instr instr, Object result) {
        if (instr instanceof ResultInstr) {
            setResult(temp, currDynScope, ((ResultInstr)instr).getResult(), result);
        }
    }

    private static Object retrieveOp(Operand r, ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        Object res;
        if (r instanceof Self) {
            return self;
        } else if (r instanceof TemporaryVariable) {
            res = temp[((TemporaryVariable)r).offset];
            return res == null ? context.nil : res;
        } else if (r instanceof LocalVariable) {
            LocalVariable lv = (LocalVariable)r;
            res = currDynScope.getValue(lv.getLocation(), lv.getScopeDepth());
            return res == null ? context.nil : res;
        } else {
            return r.retrieve(context, self, currDynScope, temp);
        }

    }

    private static void updateCallSite(Instr instr, IRScope scope, Integer scopeVersion) {
        if (instr instanceof CallBase) {
            callerSite.s = scope;
            callerSite.v = scopeVersion;
            callerSite.call = (CallBase)instr;
        }
    }

    private static void receiveArg(ThreadContext context, Instr i, Operation operation, IRubyObject[] args, int kwArgHashCount, DynamicScope currDynScope, Object[] temp, Object exception, Block block) {
        Object result = null;
        ResultInstr instr = (ResultInstr)i;
        switch(operation) {
        case RECV_PRE_REQD_ARG:
            int argIndex = ((ReceivePreReqdArgInstr)instr).getArgIndex();
            result = ((argIndex + kwArgHashCount) < args.length) ? args[argIndex] : context.nil; // SSS FIXME: This check is only required for closures, not methods
            break;
        case RECV_CLOSURE:
            result = (block == Block.NULL_BLOCK) ? context.nil : context.runtime.newProc(Block.Type.PROC, block);
            break;
        case RECV_OPT_ARG:
            result = ((ReceiveOptArgInstr)instr).receiveOptArg(args, kwArgHashCount);
            break;
        case RECV_POST_REQD_ARG:
            result = ((ReceivePostReqdArgInstr)instr).receivePostReqdArg(args, kwArgHashCount);
            // For blocks, missing arg translates to nil
            result = result == null ? context.nil : result;
            break;
        case RECV_REST_ARG:
            result = ((ReceiveRestArgInstr)instr).receiveRestArg(context.runtime, args, kwArgHashCount);
            break;
        case RECV_KW_ARG:
            result = ((ReceiveKeywordArgInstr)instr).receiveKWArg(context, kwArgHashCount, args);
            break;
        case RECV_KW_REST_ARG:
            result = ((ReceiveKeywordRestArgInstr)instr).receiveKWArg(context, kwArgHashCount, args);
            break;
        case RECV_EXCEPTION: {
            ReceiveExceptionInstr rei = (ReceiveExceptionInstr)instr;
            result = (exception instanceof RaiseException && rei.checkType) ? ((RaiseException)exception).getException() : exception;
            break;
        }
        }

        setResult(temp, currDynScope, instr.getResult(), result);
    }

    private static void processCall(ThreadContext context, Instr instr, Operation operation, IRScope scope, DynamicScope currDynScope, Object[] temp, IRubyObject self, Block block, Block.Type blockType) {
        Object result = null;
        switch(operation) {
        case RUNTIME_HELPER: {
            RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
            result = rhc.callHelper(context, currDynScope, self, temp, scope, blockType);
            setResult(temp, currDynScope, rhc.getResult(), result);
            break;
        }
        case CALL_1F: {
            OneFixnumArgNoBlockCallInstr call = (OneFixnumArgNoBlockCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, temp);
            result = call.getCallSite().call(context, self, r, call.getFixnumArg());
            setResult(temp, currDynScope, call.getResult(), result);
            break;
        }
        case CALL_1O: {
            OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, temp);
            IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currDynScope, temp);
            result = call.getCallSite().call(context, self, r, o);
            setResult(temp, currDynScope, call.getResult(), result);
            break;
        }
        case CALL_0O: {
            ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, temp);
            result = call.getCallSite().call(context, self, r);
            setResult(temp, currDynScope, call.getResult(), result);
            break;
        }
        case NORESULT_CALL_1O: {
            OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, temp);
            IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currDynScope, temp);
            call.getCallSite().call(context, self, r, o);
            break;
        }
        case NORESULT_CALL:
            instr.interpret(context, currDynScope, self, temp, block);
            break;
        case CALL:
        default:
            result = instr.interpret(context, currDynScope, self, temp, block);
            setResult(temp, currDynScope, instr, result);
            break;
        }
    }

    private static IRubyObject interpret(ThreadContext context, IRubyObject self,
            IRScope scope, Visibility visibility, RubyModule implClass, IRubyObject[] args, Block block, Block.Type blockType) {
        Instr[] instrs = scope.getInstrsForInterpretation();

        // The base IR may not have been processed yet
        if (instrs == null) instrs = scope.prepareForInterpretation(blockType == Block.Type.LAMBDA);

        int      numTempVars    = scope.getTemporaryVariableSize();
        Object[] temp           = numTempVars > 0 ? new Object[numTempVars] : null;
        int      n              = instrs.length;
        int      ipc            = 0;
        Instr    instr          = null;
        Object   exception      = null;
        int      kwArgHashCount = (scope.receivesKeywordArgs() && args[args.length - 1] instanceof RubyHash) ? 1 : 0;
        DynamicScope currDynScope = context.getCurrentScope();

        // Counter tpCount = null;

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? initProfiling(scope) : 0;

        // Enter the looooop!
        while (ipc < n) {
            instr = instrs[ipc];
            ipc++;
            Operation operation = instr.getOperation();
            if (debug) {
                LOG.info("I: {}", instr);
               interpInstrsCount++;
            } else if (profile) {
                if (operation.modifiesCode()) codeModificationsCount++;
               interpInstrsCount++;
               /*
               Counter cnt = opStats.get(operation);
               if (cnt == null) {
                   cnt = new Counter();
                   opStats.put(operation, cnt);
               }
               cnt.count++;
               */
            }

            try {
                switch (operation.opClass) {
                case ARG_OP: {
                    receiveArg(context, instr, operation, args, kwArgHashCount, currDynScope, temp, exception, block);
                    break;
                }
                case BRANCH_OP: {
                    if (operation == Operation.JUMP) {
                        ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC();
                    } else {
                        ipc = instr.interpretAndGetNewIPC(context, currDynScope, self, temp, ipc);
                    }
                    break;
                }
                case CALL_OP: {
                    if (profile) updateCallSite(instr, scope, scopeVersion);
                    processCall(context, instr, operation, scope, currDynScope, temp, self, block, blockType);
                    break;
                }
                case BOOK_KEEPING_OP: {
                    switch(operation) {
                    case PUSH_FRAME: {
                        context.preMethodFrameAndClass(implClass, scope.getName(), self, block, scope.getStaticScope());
                        context.setCurrentVisibility(visibility);
                        break;
                    }
                    case PUSH_BINDING: {
                        // SSS NOTE: Method scopes only!
                        //
                        // Blocks are a headache -- so, these instrs. are only added to IRMethods.
                        // Blocks have more complicated logic for pushing a dynamic scope (see InterpretedIRBlockBody)
                        currDynScope = DynamicScope.newDynamicScope(scope.getStaticScope());
                        context.pushScope(currDynScope);
                        break;
                    }
                    case CHECK_ARITY:
                        ((CheckArityInstr)instr).checkArity(context.runtime, args.length);
                        break;
                    case POP_FRAME:
                        context.popFrame();
                        context.popRubyClass();
                        break;
                    case POP_BINDING:
                        context.popScope();
                        break;
                    case THREAD_POLL:
                        if (profile) {
                            // SSS: Not being used currently
                            // tpCount.count++;
                            globalThreadPollCount++;

                            // 20K is arbitrary
                            // Every 20K profile counts, spit out profile stats
                            if (globalThreadPollCount % 20000 == 0) {
                                analyzeProfile();
                                // outputProfileStats();
                            }
                        }
                        context.callThreadPoll();
                        break;
                    case LINE_NUM:
                        context.setLine(((LineNumberInstr)instr).lineNumber);
                        break;
                    case RECORD_END_BLOCK:
                        ((RecordEndBlockInstr)instr).interpret();
                        break;
                    }
                    break;
                }
                case OTHER_OP: {
                    Object result = null;
                    switch(operation) {
                    // --------- Return flavored instructions --------
                    case BREAK: {
                        BreakInstr bi = (BreakInstr)instr;
                        IRubyObject rv = (IRubyObject)bi.getReturnValue().retrieve(context, self, currDynScope, temp);
                        // This also handles breaks in lambdas -- by converting them to a return
                        return IRRuntimeHelpers.initiateBreak(context, scope, bi.getScopeToReturnTo().getScopeId(), rv, blockType);
                    }
                    case RETURN: {
                        return (IRubyObject)retrieveOp(((ReturnBase)instr).getReturnValue(), context, self, currDynScope, temp);
                    }
                    case NONLOCAL_RETURN: {
                        NonlocalReturnInstr ri = (NonlocalReturnInstr)instr;
                        IRubyObject rv = (IRubyObject)retrieveOp(ri.getReturnValue(), context, self, currDynScope, temp);
                        ipc = n;
                        // If not in a lambda, check if this was a non-local return
                        if (!IRRuntimeHelpers.inLambda(blockType)) {
                            IRRuntimeHelpers.initiateNonLocalReturn(context, scope, ri.methodToReturnFrom, rv);
                        }
                        return rv;
                    }

                    // ---------- Common instruction ---------
                    case COPY: {
                        CopyInstr c = (CopyInstr)instr;
                        result = retrieveOp(c.getSource(), context, self, currDynScope, temp);
                        setResult(temp, currDynScope, c.getResult(), result);
                        break;
                    }

                    case GET_FIELD: {
                        GetFieldInstr gfi = (GetFieldInstr)instr;
                        IRubyObject object = (IRubyObject)gfi.getSource().retrieve(context, self, currDynScope, temp);
                        VariableAccessor a = gfi.getAccessor(object);
                        result = a == null ? null : (IRubyObject)a.get(object);
                        if (result == null) {
                            result = context.nil;
                        }
                        setResult(temp, currDynScope, gfi.getResult(), result);
                        break;
                    }

                    case SEARCH_CONST: {
                        SearchConstInstr sci = (SearchConstInstr)instr;
                        result = sci.getCachedConst();
                        if (!sci.isCached(context, result)) result = sci.cache(context, currDynScope, self, temp);
                        setResult(temp, currDynScope, sci.getResult(), result);
                        break;
                    }

                    // ---------- All the rest ---------
                    default:
                        result = instr.interpret(context, currDynScope, self, temp, block);
                        setResult(temp, currDynScope, instr, result);
                        break;
                    }

                    break;
                }
                }
            } catch (Throwable t) {
                // Unrescuable:
                //    IRReturnJump, ThreadKill, RubyContinuation, MainExitException, etc.
                //    These cannot be rescued -- only run ensure blocks
                //
                // Others:
                //    IRBreakJump, Ruby exceptions, errors, and other java exceptions.
                //    These can be rescued -- run rescue blocks

                if (debug) LOG.info("in scope: " + scope + ", caught Java throwable: " + t + "; excepting instr: " + instr);
                ipc = (t instanceof Unrescuable) ? scope.getEnsurerPC(instr) : scope.getRescuerPC(instr);
                if (debug) LOG.info("ipc for rescuer/ensurer: " + ipc);

                if (ipc == -1) {
                    Helpers.throwException((Throwable)t);
                } else {
                    exception = t;
                }
            }
        }

        // Control should never get here!
        // SSS FIXME: But looks like BEGIN/END blocks get here -- needs fixing
        return null;
    }

    public static IRubyObject INTERPRET_EVAL(ThreadContext context, IRubyObject self,
            IRScope scope, RubyModule clazz, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            return interpret(context, self, scope, null, clazz, args, block, blockType);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_BLOCK(ThreadContext context, IRubyObject self,
            IRScope scope, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            return interpret(context, self, scope, null, null, args, block, blockType);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, InterpretedIRMethod irMethod,
        IRubyObject self, String name, IRubyObject[] args, Block block, Block.Type blockType, boolean isTraceable) {
        Ruby       runtime   = context.runtime;
        IRScope    scope     = irMethod.getIRMethod();
        RubyModule implClass = irMethod.getImplementationClass();
        Visibility viz       = irMethod.getVisibility();
        boolean syntheticMethod = name == null || name.equals("");

        try {
            if (!syntheticMethod) ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            if (isTraceable) methodPreTrace(runtime, context, name, implClass);
            return interpret(context, self, scope, viz, implClass, args, block, blockType);
        } finally {
            if (isTraceable) {
                try {methodPostTrace(runtime, context, name, implClass);}
                finally { if (!syntheticMethod) ThreadContext.popBacktrace(context);}
            } else {
                if (!syntheticMethod) ThreadContext.popBacktrace(context);
            }
        }
    }

    private static void methodPreTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.CALL, name, implClass);
    }

    private static void methodPostTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.RETURN, name, implClass);
    }
}
