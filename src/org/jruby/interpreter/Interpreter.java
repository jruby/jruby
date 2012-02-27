package org.jruby.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.compiler.ir.Counter;
import org.jruby.compiler.ir.IRBuilder;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IREvalScript;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRScriptBody;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.BEQInstr;
import org.jruby.compiler.ir.instructions.BNEInstr;
import org.jruby.compiler.ir.instructions.BranchInstr;
import org.jruby.compiler.ir.instructions.BreakInstr;
import org.jruby.compiler.ir.instructions.CallBase;
import org.jruby.compiler.ir.instructions.calladapter.CallAdapter;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JumpIndirectInstr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.LineNumberInstr;
import org.jruby.compiler.ir.instructions.ModuleVersionGuardInstr;
import org.jruby.compiler.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.compiler.ir.instructions.ReceiveOptArgBase;
import org.jruby.compiler.ir.instructions.ReceiveRestArgBase;
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.instructions.ReturnBase;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.instructions.jruby.CheckArityInstr;
import org.jruby.compiler.ir.instructions.ruby19.ReceivePostReqdArgInstr;
import org.jruby.compiler.ir.operands.IRException;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.TemporaryVariable;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.WrappedIRClosure;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.unsafe.UnsafeFactory;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;

public class Interpreter {
    private static final Logger LOG = LoggerFactory.getLogger("Interpreter");

    private static int inlineCount = 0;
    private static int interpInstrsCount = 0;
    private static int codeModificationsCount = 0;
    private static int numCyclesWithNoModifications = 0;
    private static int globalThreadPollCount = 0;
    private static HashMap<IRScope, Counter> scopeThreadPollCounts = new HashMap<IRScope, Counter>();

    public static boolean inProfileMode() {
        return RubyInstanceConfig.IR_PROFILE;
    }

    public static boolean isDebug() {
        return RubyInstanceConfig.IR_DEBUG;
    }

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

    private static IRScope getEvalContainerScope19(Ruby runtime, StaticScope evalScope) {
        // SSS FIXME: Weirdness here.  Different from 1.8.  There is no localvar scope wrapping.
        // 1. For instance-eval (module-eval, class-eval) scenarios, there is an extra scope that is added to 
        //    the stack in ThreadContext.java:preExecuteUnder
        // I dont know what rule to apply when.  However, in both these cases, since there is no IR-scope associated,
        // I have used the hack below where I first unwrap once and see if I get a non-null IR scope.  If that doesn't
        // work, I unwarp once more and I am guaranteed to get the IR scope I want.
        IRScope containingIRScope = ((IRStaticScope)evalScope).getIRScope();
        if (containingIRScope == null) containingIRScope = ((IRStaticScope)evalScope.getEnclosingScope()).getIRScope();
        if (containingIRScope == null) containingIRScope = ((IRStaticScope)evalScope.getEnclosingScope().getEnclosingScope()).getIRScope();
        return containingIRScope;
    }

    public static IRubyObject interpretCommonEval(Ruby runtime, String file, int lineNumber, String backtraceName, RootNode rootNode, IRubyObject self, Block block) {
        boolean is_1_9 = runtime.is1_9();
        // SSS FIXME: Is this required here since the IR version cannot change from eval-to-eval? This is much more of a global setting.
        if (is_1_9) IRBuilder.setRubyVersion("1.9");

        StaticScope ss = rootNode.getStaticScope();
        IRScope containingIRScope = is_1_9 ? getEvalContainerScope19(runtime, ss) : getEvalContainerScope(runtime, ss);
        IREvalScript evalScript = IRBuilder.createIRBuilder(runtime.getIRManager()).buildEvalRoot(ss, containingIRScope, file, lineNumber, rootNode);
        evalScript.prepareForInterpretation();
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
            b.prepareForInterpretation();
            Block blk = (Block)(new WrappedIRClosure(b)).retrieve(context, self, context.getCurrentScope(), temp);
            blk.yield(context, null);
        }
    }

    public static IRubyObject interpret(Ruby runtime, Node rootNode, IRubyObject self) {
        if (runtime.is1_9()) IRBuilder.setRubyVersion("1.9");

        IRScriptBody root = (IRScriptBody) IRBuilder.createIRBuilder(runtime.getIRManager()).buildRoot((RootNode) rootNode);

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
            if (isDebug() || inProfileMode()) LOG.info("-- Interpreted instructions: {}", interpInstrsCount);
            return rv;
        } catch (IRBreakJump bj) {
            throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());
        }
    }

    private static void analyzeProfile() {
        //if (inlineCount == 2) return;

        if (codeModificationsCount == 0) numCyclesWithNoModifications++;
        else numCyclesWithNoModifications = 0;

        codeModificationsCount = 0;

        if (numCyclesWithNoModifications < 3) return;

        // We are now good to go -- start analyzing the profile
        ArrayList<IRScope> scopes = new ArrayList<IRScope>(scopeThreadPollCounts.keySet());
        Collections.sort(scopes, new java.util.Comparator<IRScope> () {
            public int compare(IRScope a, IRScope b) {
                float aCount = scopeThreadPollCounts.get(a).count;
                float bCount = scopeThreadPollCounts.get(b).count;
                if (aCount == bCount) return 0;
                return (aCount < bCount) ? 1 : -1;
            }
        });

        // Find top N scopes
        Set<IRScope> hotScopes = new HashSet<IRScope>();
        int i = 0;
        float f = 0.0f;
        for (IRScope s: scopes) {
            long sCount = scopeThreadPollCounts.get(s).count;

            // If the scope is accounting for very little additional execution, exit!
            float sPerc = ((sCount*1000)/globalThreadPollCount)/10.0f;
            if (sPerc < 1) {
                Instr[] instrs = s.getInstrsForInterpretation();
                if (instrs == null) continue; // can happen if a previously inlined method hasn't been rebuilt

                // Allow smaller methods to inline more liberally
                if (instrs.length > (5 + sPerc * 10)) continue;
            }

            //System.out.println("Hot scope: " + s + "; %contribution: " + sPerc + "; cumulative: " + (f + sPerc));
            hotScopes.add(s);

            f += sPerc;
            i++;
            if (i == 50 || f >= 99.0) break;
        }

        // Identify inlining sites
        // Heuristic: In hot methods, identify monomorphic call sites to hot methods
        boolean revisitScope = false;
        Iterator<IRScope> hsIter = hotScopes.iterator();
        IRScope hs = null;
        while (hsIter.hasNext()) {
            if (!revisitScope) hs = hsIter.next();
            revisitScope = false;

            boolean skip = false;
            boolean isHotClosure = hs instanceof IRClosure;
            IRScope hc = isHotClosure ? hs : null;
            hs = isHotClosure ? hs.getLexicalParent() : hs;
            for (BasicBlock b : hs.getCFG().getBasicBlocks()) {
                for (Instr instr : b.getInstrs()) {
                    if ((instr instanceof CallBase) && !((CallBase)instr).inliningBlocked()) {
                        // System.out.println("checking: " + instr);
                        CallBase call = (CallBase)instr;
                        CallAdapter ca = call.getCallAdapter();
                        CallSite cs = ca == null ? null: ca.getCallSite();
                        // System.out.println("callsite: " + cs);
                        if (cs != null && (cs instanceof CachingCallSite)) {
                            CachingCallSite ccs = (CachingCallSite)cs;
                            // SSS FIXME: To use this, CachingCallSite.java needs modification
                            // isPolymorphic or something equivalent needs to be enabled there.
                            if (ccs.isOptimizable()) {
                                CacheEntry ce = ccs.getCache();
                                DynamicMethod tgt = ce.method;
                                if (tgt instanceof InterpretedIRMethod) {
                                    InterpretedIRMethod dynMeth = (InterpretedIRMethod)tgt;
                                    IRScope tgtMethod = dynMeth.getIRMethod();
                                    Instr[] instrs = tgtMethod.getInstrsForInterpretation();
                                    // Dont inline large methods -- 200 is arbitrary
                                    // Can be null if a previously inlined method hasn't been rebuilt
                                    if ((instrs == null) || instrs.length > 150) continue; 

                                    RubyModule implClass = dynMeth.getImplementationClass();
                                    int classToken = implClass.getGeneration();
                                    String n = tgtMethod.getName();
                                    boolean inlineCall = false;
                                    if (isHotClosure) {
                                        Operand clArg = call.getClosureArg(null);
                                        inlineCall = (clArg instanceof WrappedIRClosure) && (((WrappedIRClosure)clArg).getClosure() == hc);
                                    } else if (hotScopes.contains(tgtMethod)) {
                                        inlineCall = true;
                                    }

                                    if (inlineCall) {
                                        System.out.println("Inlining " + tgtMethod + " in " + hs + " @ instr " + instr);

                                        hs.inlineMethod(tgtMethod, implClass, classToken, b, call);
                                        // reset tp counters
                                        scopeThreadPollCounts.remove(isHotClosure ? hc : hs);
                                        scopeThreadPollCounts.remove(tgtMethod);
                                        inlineCount++;
                                        skip = true;
                                        revisitScope = true;

                                        break;
                                        //return;
                                    }
                                }
                            }
                        }
                    }
                }

                // We skip the rest of the method because we will run into concurrent modification exceptions in the iterators
                // SSS FIXME: We may miss some inlining sites because of this
                if (skip) break;
            }
        }
    }

    private static void outputProfileStats() {
        ArrayList<IRScope> scopes = new ArrayList<IRScope>(scopeThreadPollCounts.keySet());
        Collections.sort(scopes, new java.util.Comparator<IRScope> () {
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


        LOG.info("------------------------");
        LOG.info("Stats after " + globalThreadPollCount + " thread polls:");
        LOG.info("------------------------");
        LOG.info("# instructions: " + interpInstrsCount);
        LOG.info("# code modifications in this period : " + codeModificationsCount);
        LOG.info("------------------------");
        int i = 0;
        float f1 = 0.0f;
        for (IRScope s: scopes) {
            long n = scopeThreadPollCounts.get(s).count;
            float p1 =  ((n*1000)/globalThreadPollCount)/10.0f;
            String msg = i + ". " + s + " [file:" + s.getFileName() + ":" + s.getLineNumber() + "] = " + n + "; (" + p1 + "%)";
            if (s instanceof IRClosure) {
                IRMethod m = s.getNearestMethod();
                if (m != null) LOG.info(msg + " -- nearest enclosing method: " + m);
                else LOG.info(msg + " -- no enclosing method --");
            } else {
                LOG.info(msg);
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
            System.out.println("---- resetting thread-poll counters ----");
            scopeThreadPollCounts = new HashMap<IRScope, Counter>();
            globalThreadPollCount = 0;
        }
    }

    private static IRubyObject interpret(ThreadContext context, IRubyObject self, 
            IRScope scope, IRubyObject[] args, Block block, Block.Type blockType) {
        boolean debug = isDebug();
        boolean profile = inProfileMode();
        boolean inClosure = (scope instanceof IRClosure);
        Instr[] instrs = scope.getInstrsForInterpretation();

        // The base IR may not have been processed yet
        if (instrs == null) instrs = scope.prepareForInterpretation();

        int temporaryVariablesSize = scope.getTemporaryVariableSize();
        Object[] temp = temporaryVariablesSize > 0 ? new Object[temporaryVariablesSize] : null;
        int n   = instrs.length;
        int ipc = 0;
        Instr lastInstr = null;
        IRubyObject rv = null;
        Object exception = null;
        Ruby runtime = context.getRuntime();
        DynamicScope currDynScope = context.getCurrentScope();

        // Set up thread-poll counter for this scope
        Counter tpCount = null;
        if (profile) {
            tpCount = scopeThreadPollCounts.get(scope);
            if (tpCount == null) {
                tpCount = new Counter();
                scopeThreadPollCounts.put(scope, tpCount);
            }
        }

        // Enter the looooop!
        while (ipc < n) {
            lastInstr = instrs[ipc];
            Operation operation = lastInstr.getOperation();

            if (debug) {
                LOG.info("I: {}", lastInstr);
               interpInstrsCount++;
            } else if (profile) {
                if (operation.modifiesCode()) codeModificationsCount++;
               interpInstrsCount++;
            }

            // We need a nested try-catch:
            // - The first try-catch around the instruction captures JRuby-implementation exceptions
            //   generated by return and break instructions.  This catch could then raise Ruby-visible
            //   LocalJump errors which could be caught by Ruby-level exception handlers.
            // - The second try-catch around the first try-catch handles Ruby-visible exceptions and
            //   invokes Ruby-level exceptions handlers.
            try {
                Variable resultVar = null;
                Object result = null;
                try {
                    switch(operation) {
                    case JUMP: {
                        ipc = ((JumpInstr)lastInstr).getJumpTarget().getTargetPC();
                        break;
                    }
                    case JUMP_INDIRECT: {
                        ipc = ((Label)((JumpIndirectInstr)lastInstr).getJumpTarget().retrieve(context, self, currDynScope, temp)).getTargetPC();
                        break;
                    }
                    case B_TRUE: {
                        BranchInstr br = (BranchInstr)lastInstr;
                        Object value1 = br.getArg1().retrieve(context, self, currDynScope, temp);
                        ipc = ((IRubyObject)value1).isTrue()? br.getJumpTarget().getTargetPC() : ipc+1;
                        break;
                    }
                    case B_FALSE: {
                        BranchInstr br = (BranchInstr)lastInstr;
                        Object value1 = br.getArg1().retrieve(context, self, currDynScope, temp);
                        ipc = !((IRubyObject)value1).isTrue()? br.getJumpTarget().getTargetPC() : ipc+1;
                        break;
                    }
                    case B_NIL: {
                        BranchInstr br = (BranchInstr)lastInstr;
                        Object value1 = br.getArg1().retrieve(context, self, currDynScope, temp);
                        ipc = value1 == context.nil ? br.getJumpTarget().getTargetPC() : ipc+1;
                        break;
                    }
                    case B_UNDEF: {
                        BranchInstr br = (BranchInstr)lastInstr;
                        Object value1 = br.getArg1().retrieve(context, self, currDynScope, temp);
                        ipc = value1 == UndefinedValue.UNDEFINED ? br.getJumpTarget().getTargetPC() : ipc+1;
                        break;
                    }
                    case BEQ: {
                        BEQInstr beq = (BEQInstr)lastInstr;
                        Object value1 = beq.getArg1().retrieve(context, self, currDynScope, temp);
                        Object value2 = beq.getArg2().retrieve(context, self, currDynScope, temp);
                        boolean eql = ((IRubyObject) value1).op_equal(context, (IRubyObject)value2).isTrue();
                        ipc = eql ? beq.getJumpTarget().getTargetPC() : ipc+1;
                        break;
                    }
                    case BNE: {
                        BNEInstr bne = (BNEInstr)lastInstr;
                        Operand arg1 = bne.getArg1();
                        Operand arg2 = bne.getArg2();
                        Object value1 = arg1.retrieve(context, self, currDynScope, temp);
                        Object value2 = arg2.retrieve(context, self, currDynScope, temp);
                        boolean eql = arg2 == scope.getManager().getNil() || arg2 == UndefinedValue.UNDEFINED ?
                                value1 == value2 : ((IRubyObject) value1).op_equal(context, (IRubyObject)value2).isTrue();
                        ipc = !eql ? bne.getJumpTarget().getTargetPC() : ipc+1;
                        break;
                    }
                    case MODULE_GUARD: {
                        ModuleVersionGuardInstr mvg = (ModuleVersionGuardInstr)lastInstr;
                        ipc = mvg.versionMatches(context, currDynScope, self, temp) ? ipc + 1 : mvg.getFailurePathLabel().getTargetPC();
                        break;
                    }
                    case RECV_PRE_REQD_ARG: {
                        ReceivePreReqdArgInstr ra = (ReceivePreReqdArgInstr)lastInstr;
                        int argIndex = ra.getArgIndex();
                        result = (argIndex < args.length) ? args[argIndex] : context.nil; // SSS FIXME: This check is only required for closures, not methods
                        resultVar = ra.getResult();
                        ipc++;
                        break;
                    }
                    case RECV_POST_REQD_ARG: {
                        ReceivePostReqdArgInstr ra = (ReceivePostReqdArgInstr)lastInstr;
                        result = ra.receivePostReqdArg(args);
                        if (result == null) result = context.nil; // For blocks
                        resultVar = ra.getResult();
                        ipc++;
                        break;
                    }
                    case RECV_OPT_ARG: {
                        ReceiveOptArgBase ra = (ReceiveOptArgBase)lastInstr;
                        result = ra.receiveOptArg(args);
                        resultVar = ra.getResult();
                        ipc++;
                        break;
                    }
                    case RECV_REST_ARG: {
                        ReceiveRestArgBase ra = (ReceiveRestArgBase)lastInstr;
                        result = ra.receiveRestArg(runtime, args);
                        resultVar = ra.getResult();
                        ipc++;
                        break;
                    }
                    case RECV_CLOSURE: {
                        result = block == Block.NULL_BLOCK ? context.nil : runtime.newProc(Block.Type.PROC, block);
                        resultVar = ((ResultInstr)lastInstr).getResult();
                        ipc++;
                        break;
                    }
                    case RECV_EXCEPTION: {
                        result = exception;
                        resultVar = ((ResultInstr)lastInstr).getResult();
                        ipc++;
                        break;
                    }
                    case ATTR_ASSIGN:
                    case CALL: {
                        CallBase c = (CallBase)lastInstr;
                        IRubyObject object = (IRubyObject)c.getReceiver().retrieve(context, self, currDynScope, temp);
                        Object callResult = c.getCallAdapter().call(context, self, object, currDynScope, temp);
                        if (c instanceof ResultInstr) {
                            result = callResult;
                            resultVar = ((ResultInstr)c).getResult();
                        }
                        ipc++;
                        break;
                    }
                    case CLOSURE_RETURN:
                    case RETURN: {
                        rv = (IRubyObject)((ReturnBase)lastInstr).getReturnValue().retrieve(context, self, currDynScope, temp);
                        ipc = n;
                        break;
                    }
                    case THREAD_POLL: {
                        if (profile) { 
                            tpCount.count++; 
                            globalThreadPollCount++;
                            // SSS: Uncomment this to analyze profile
                            // Every 10K profile counts, spit out profile stats
                            // if (globalThreadPollCount % 10000 == 0) analyzeProfile(); //outputProfileStats();
                        }
                        context.callThreadPoll();
                        ipc++;
                        break;
                    }
                    case LINE_NUM: {
                        context.setLine(((LineNumberInstr)lastInstr).lineNumber);
                        ipc++;
                        break;
                    }
                    case COPY: {
                        CopyInstr c = (CopyInstr)lastInstr;
                        result = c.getSource().retrieve(context, self, currDynScope, temp);
                        resultVar = ((ResultInstr)lastInstr).getResult();
                        ipc++;
                        break;
                    }
                    case CHECK_ARITY: {
                        CheckArityInstr ca = (CheckArityInstr)lastInstr;
                        int numArgs = args.length;
                        if ((numArgs < ca.required) || ((ca.rest == -1) && (numArgs > (ca.required + ca.opt)))) {
                            Arity.raiseArgumentError(runtime, numArgs, ca.required, ca.required + ca.opt);
                        }
                         ipc++;
                        break;
                    }
                    default: {
                        result = lastInstr.interpret(context, currDynScope, self, temp, block);
                        if (lastInstr instanceof ResultInstr) resultVar = ((ResultInstr)lastInstr).getResult();
                        ipc++;
                        break;
                    }
                    }

                    if (resultVar != null) {
                        if (resultVar instanceof TemporaryVariable) {
                            temp[((TemporaryVariable)resultVar).offset] = result;
                        }
                        else {
                            LocalVariable lv = (LocalVariable)resultVar;
                            currDynScope.setValue((IRubyObject) result, lv.getLocation(), lv.getScopeDepth());
                        }
                    }
                } catch (IRReturnJump rj) {
                    ipc = scope.getEnsurerPC(lastInstr);
                    // Because of yields, control can exit this scope via returns in the block being yielded to.
                    // Before we exit this scope, we have to run ensure blocks in this scope.
                    //
                    // In the current IR design, the IR builder cannot generate explicit IR instructions that
                    // trap the return and run the ensure code.  Hence, we have to implicitly handle this in the
                    // interpreter.
                    if (ipc == -1) {
                        // No ensure block here, propagate the return
                        return handleReturnJumpInClosure(scope, rj, blockType);
                    } else {
                        // Set the return jump as the exception to the ensure block and continue
                        // The ensure block will rethrow this exception at which time control
                        // will go the if-block above and the return jump will get handled properly
                        exception = rj;
                    }
                } catch (IRBreakJump bj) {
                    if ((lastInstr instanceof BreakInstr) || bj.breakInEval) {
                        handleBreakJump(context, scope, bj, self, blockType, inClosure);
                    } else if (inLambda(blockType)) {
                        // We just unwound all the way up because of a non-local break
                        throw IRException.BREAK_LocalJumpError.getException(runtime);
                    } else if (bj.caughtByLambda || (bj.scopeToReturnTo == scope)) {
                        // We got where we need to get to (because a lambda stopped us, or because we popped to the
                        // lexical scope where we got called from).  Retrieve the result and store it.

                        // SSS FIXME: why cannot I just use resultVar from the loop above?? why did it break something?
                        if (lastInstr instanceof ResultInstr) {
                            resultVar = ((ResultInstr) lastInstr).getResult();
                            if (resultVar instanceof TemporaryVariable) {
                                temp[((TemporaryVariable)resultVar).offset] = bj.breakValue;
                            }
                            else {
                                LocalVariable lv = (LocalVariable)resultVar;
                                currDynScope.setValue((IRubyObject) bj.breakValue, lv.getLocation(), lv.getScopeDepth());
                            }
                        }
                        ipc += 1;
                    } else {
                        // We need to continue to break upwards.
                        // Run any ensures we need to run before breaking up. 
                        // Quite easy to do this by passing 'bj' as the exception to the ensure block!
                        ipc = scope.getEnsurerPC(lastInstr);
                        if (ipc == -1) throw bj; // No ensure block here, just rethrow bj
                        exception = bj; // Found an ensure block, set 'bj' as the exception and transfer control
                    }
                }
            } catch (RaiseException re) {
                if (debug) LOG.info("in scope: " + scope + ", caught raise exception: " + re.getException() + "; excepting instr: " + lastInstr);
                ipc = scope.getRescuerPC(lastInstr);
                if (debug) LOG.info("ipc for rescuer: " + ipc);
                if (ipc == -1) throw re; // No one rescued exception, pass it on!

                exception = re.getException();
                // SSS: Copied this comment and line from ast/RescueNode.java:handleException
                // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
                // falsely set $! to nil and this sets it back to something valid.  This should 
                // get fixed at the same time we address bug #1296484.
                runtime.getGlobalVariables().set("$!", (IRubyObject)exception);
            } catch (Throwable t) {
                if (t instanceof Unrescuable) {
                    // ThreadKill, RubyContinuation, MainExitException, etc.
                    // these cannot be rescued -- only run ensure blocks
                    ipc = scope.getEnsurerPC(lastInstr);
                } else {
                    // Error and other java exceptions which could be rescued
                    if (debug) {
                        LOG.info("in scope: " + scope + ", caught Java throwable: " + t + "; excepting instr: " + lastInstr);
                    }
                    //t.printStackTrace();
                    ipc = scope.getRescuerPC(lastInstr);
                    if (debug) LOG.info("ipc for rescuer: " + ipc);
                }
                if (ipc == -1) UnsafeFactory.getUnsafe().throwException(t); // No ensure block here, pass it on! 
                exception = t;
            }
        }

        // If not in a lambda, and lastInstr was a return, check if this was a non-local return
        if ((lastInstr instanceof ReturnInstr) && !inLambda(blockType)) {
            handleNonLocalReturn(context, scope, (ReturnInstr) lastInstr, rv, inClosure);
        }

        return rv;
    }

    /*
     * Handle non-local returns (ex: when nested in closures, root scopes of module/class/sclass bodies)
     */
    private static void handleNonLocalReturn(ThreadContext context, IRScope scope, ReturnInstr returnInstr, IRubyObject returnValue, boolean inClosure) {
        IRMethod methodToReturnFrom = returnInstr.methodToReturnFrom;

        if (inClosure) {
            if (methodToReturnFrom == null) {
                if (context.getThread() == context.getRuntime().getThreadService().getMainThread()) {
                    throw IRException.RETURN_LocalJumpError.getException(context.getRuntime());
                } else {
                    throw context.getRuntime().newThreadError("return can't jump across threads");
                }
            }

            // Cannot return to the call that we have long since exited.
            if (!context.scopeExistsOnCallStack(methodToReturnFrom.getStaticScope())) {
                if (isDebug()) LOG.info("in scope: " + scope + ", raising unexpected return local jump error");
                throw IRException.RETURN_LocalJumpError.getException(context.getRuntime());
            }

            throw IRReturnJump.create(methodToReturnFrom, returnValue);
        } else if ((methodToReturnFrom != null)) {
            // methodtoReturnFrom will not be null for explicit returns from class/module/sclass bodies
            throw IRReturnJump.create(methodToReturnFrom, returnValue);
        }        
    }

    private static IRubyObject handleReturnJumpInClosure(IRScope scope, IRReturnJump rj, Type blockType) throws IRReturnJump {
        // - If we are in a lambda or if we are in the method scope we are supposed to return from, stop propagating
        if (inNonMethodBodyLambda(scope, blockType) || (rj.methodToReturnFrom == scope)) return (IRubyObject) rj.returnValue;

        // - If not, Just pass it along!
        throw rj;
    }

    private static void handleBreakJump(ThreadContext context, IRScope scope, IRBreakJump bj, IRubyObject self, Type blockType, boolean inClosure) throws RaiseException, IRBreakJump {
        bj.breakInEval = false;  // Clear eval flag

        // Error
        if (!inClosure) throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());

        if (inProc(blockType)) {
            // SSS FIXME: Here we need to check if the current executing block has escaped
            // which means the block has to be passed in from Block.call -> BlockBody.call -> Interpreter.interpret
        } else if (inLambda(blockType)) {
            bj.caughtByLambda = true;
        } else if (scope instanceof IREvalScript) {
            // If we are in an eval, record it so we can account for it
            bj.breakInEval = true;
        }

        // Pass it upward
        throw bj;
    }

    public static IRubyObject INTERPRET_EVAL(ThreadContext context, IRubyObject self, 
            IRScope scope, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            return interpret(context, self, scope, args, block, blockType);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_BLOCK(ThreadContext context, IRubyObject self, 
            IRScope scope, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            return interpret(context, self, scope, args, block, blockType);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, IRScope scope, 
        IRubyObject self, String name, RubyModule implClass, IRubyObject[] args, Block block, Block.Type blockType, boolean isTraceable) {
        Ruby runtime = context.getRuntime();
        boolean syntheticMethod = name == null || name.equals("");

        try {
            if (!syntheticMethod) ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            if (isTraceable) methodPreTrace(runtime, context, name, implClass);
            return interpret(context, self, scope, args, block, blockType);
        } finally {
            if (isTraceable) {
                try {methodPostTrace(runtime, context, name, implClass);}
                finally { if (!syntheticMethod) ThreadContext.popBacktrace(context);}
            } else {
                if (!syntheticMethod) ThreadContext.popBacktrace(context);
            }
        }
    }
    
    private static boolean inNonMethodBodyLambda(IRScope scope, Block.Type blockType) {
        // SSS FIXME: Hack! AST interpreter and JIT compiler marks a proc's static scope as
        // an argument scope if it is used to define a method's body via :define_method.
        // Since that is exactly what we want to figure out here, am just using that flag here.
        // But, this is ugly (as is the original hack in the current runtime).  What is really
        // needed is a new block type -- a block that is used to define a method body.
        return blockType == Block.Type.LAMBDA && !scope.getStaticScope().isArgumentScope();
    }
    
    private static boolean inLambda(Block.Type blockType) {
        return blockType == Block.Type.LAMBDA;
    }

    public static boolean inProc(Block.Type blockType) {
        return blockType == Block.Type.PROC;
    }    

    private static void methodPreTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.CALL, name, implClass);
    }

    private static void methodPostTrace(Ruby runtime, ThreadContext context, String name, RubyModule implClass) {
        if (runtime.hasEventHooks()) context.trace(RubyEvent.RETURN, name, implClass);
    }
}
