package org.jruby.ir.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.ir.Counter;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveOptArgBase;
import org.jruby.ir.instructions.ReceiveRestArgBase;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.ruby19.ReceivePostReqdArgInstr;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.runtime.IRReturnJump;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.util.unsafe.UnsafeFactory;

public class Interpreter {
    private static final Logger LOG = LoggerFactory.getLogger("Interpreter");

    private static int inlineCount = 0;
    private static int interpInstrsCount = 0;
    private static int codeModificationsCount = 0;
    private static int numCyclesWithNoModifications = 0;
    private static int globalThreadPollCount = 0;
    private static HashMap<IRScope, Counter> scopeThreadPollCounts = new HashMap<IRScope, Counter>();

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
        IREvalScript evalScript = IRBuilder.createIRBuilder(runtime.getIRManager(), is_1_9).buildEvalRoot(ss, containingIRScope, file, lineNumber, rootNode);
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

        IRScriptBody root = (IRScriptBody) IRBuilder.createIRBuilder(runtime.getIRManager(), runtime.is1_9()).buildRoot((RootNode) rootNode);

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
            if (IRRuntimeHelpers.isDebug() || IRRuntimeHelpers.inProfileMode()) LOG.info("-- Interpreted instructions: {}", interpInstrsCount);
            return rv;
        } catch (IRBreakJump bj) {
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
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
            @Override
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
                        CallSite cs = call.getCallSite();
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
            IRScope scope, Visibility visibility, RubyModule implClass, IRubyObject[] args, Block block, Block.Type blockType) {
        boolean debug = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Instr[] instrs = scope.getInstrsForInterpretation();

        // The base IR may not have been processed yet
        if (instrs == null) instrs = scope.prepareForInterpretation(blockType == Block.Type.LAMBDA);

        int temporaryVariablesSize = scope.getTemporaryVariableSize();
        Object[] temp = temporaryVariablesSize > 0 ? new Object[temporaryVariablesSize] : null;
        int n   = instrs.length;
        int ipc = 0;
        Instr instr = null;
        Object exception = null;
        Ruby runtime = context.runtime;
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
            instr = instrs[ipc];
            Operation operation = instr.getOperation();

            if (debug) {
                LOG.info("I: {}", instr);
               interpInstrsCount++;
            } else if (profile) {
                if (operation.modifiesCode()) codeModificationsCount++;
               interpInstrsCount++;
            }

            try {
                Variable resultVar = null;
                Object result = null;
                switch(operation) {

                // ----------- Control-transfer instructions -------------
                case JUMP: {
                    ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC();
                    break;
                }
                case MODULE_GUARD:
                case JUMP_INDIRECT:
                case B_TRUE:
                case B_FALSE:
                case B_NIL:
                case B_UNDEF:
                case BEQ:
                case BNE: {
                    ipc = instr.interpretAndGetNewIPC(context, currDynScope, self, temp, ipc);
                    break;
                }

                // ------------- Arg-receive instructions ------------
                case RECV_PRE_REQD_ARG: {
                    ReceivePreReqdArgInstr ra = (ReceivePreReqdArgInstr)instr;
                    int argIndex = ra.getArgIndex();
                    result = (argIndex < args.length) ? args[argIndex] : context.nil; // SSS FIXME: This check is only required for closures, not methods
                    resultVar = ra.getResult();
                    ipc++;
                    break;
                }
                case RECV_POST_REQD_ARG: {
                    ReceivePostReqdArgInstr ra = (ReceivePostReqdArgInstr)instr;
                    result = ra.receivePostReqdArg(args);
                    if (result == null) result = context.nil; // For blocks
                    resultVar = ra.getResult();
                    ipc++;
                    break;
                }
                case RECV_OPT_ARG: {
                    ReceiveOptArgBase ra = (ReceiveOptArgBase)instr;
                    result = ra.receiveOptArg(args);
                    resultVar = ra.getResult();
                    ipc++;
                    break;
                }
                case RECV_REST_ARG: {
                    ReceiveRestArgBase ra = (ReceiveRestArgBase)instr;
                    result = ra.receiveRestArg(runtime, args);
                    resultVar = ra.getResult();
                    ipc++;
                    break;
                }
                case RECV_CLOSURE: {
                    result = block == Block.NULL_BLOCK ? context.nil : runtime.newProc(Block.Type.PROC, block);
                    resultVar = ((ResultInstr)instr).getResult();
                    ipc++;
                    break;
                }
                case RECV_EXCEPTION: {
                    ReceiveExceptionInstr rei = (ReceiveExceptionInstr)instr;
                    result = (exception instanceof RaiseException && rei.checkType) ? ((RaiseException)exception).getException() : exception;
                    resultVar = rei.getResult();
                    ipc++;
                    break;
                }

                // --------- Return flavored instructions --------
                case BREAK: {
                    BreakInstr bi = (BreakInstr)instr;
                    IRubyObject rv = (IRubyObject)bi.getReturnValue().retrieve(context, self, currDynScope, temp);
                    // This also handles breaks in lambdas -- by converting them to a return
                    return IRRuntimeHelpers.initiateBreak(context, scope, bi.getScopeToReturnTo(), rv, blockType);
                }
                case RETURN: {
                    return (IRubyObject)((ReturnBase)instr).getReturnValue().retrieve(context, self, currDynScope, temp);
                }
                case NONLOCAL_RETURN: {
                    NonlocalReturnInstr ri = (NonlocalReturnInstr)instr;
                    IRubyObject rv = (IRubyObject)ri.getReturnValue().retrieve(context, self, currDynScope, temp);
                    ipc = n;
                    // If not in a lambda, check if this was a non-local return
                    if (!IRRuntimeHelpers.inLambda(blockType)) {
                        IRRuntimeHelpers.initiateNonLocalReturn(context, scope, ri.methodToReturnFrom, rv);
                    }
                    return rv;
                }

                // --------- Bookkeeping instructions --------
                case CHECK_ARITY: {
                    ((CheckArityInstr)instr).checkArity(runtime, args.length);
                    ipc++;
                    break;
                }
                case PUSH_FRAME: {
                    context.preMethodFrameAndClass(implClass, scope.getName(), self, block, scope.getStaticScope());
                    context.setCurrentVisibility(visibility);
                    ipc++;
                    break;
                }
                case PUSH_BINDING: {
                    // SSS NOTE: Method scopes only!
                    //
                    // Blocks are a headache -- so, these instrs. are only added to IRMethods.
                    // Blocks have more complicated logic for pushing a dynamic scope (see InterpretedIRBlockBody)
                    currDynScope = DynamicScope.newDynamicScope(scope.getStaticScope());
                    context.pushScope(currDynScope);
                    ipc++;
                    break;
                }
                case POP_FRAME: {
                    context.popFrame();
                    context.popRubyClass();
                    ipc++;
                    break;
                }
                case POP_BINDING: {
                    context.popScope();
                    ipc++;
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
                    context.setLine(((LineNumberInstr)instr).lineNumber);
                    ipc++;
                    break;
                }
                case RUNTIME_HELPER: {
                    ipc++;
                    resultVar = ((ResultInstr)instr).getResult();
                    result = ((RuntimeHelperCall)instr).callHelper(context, currDynScope, self, temp, scope, blockType);
                    break;
                }

                // ---------- Common instruction ---------
                case COPY: {
                    CopyInstr c = (CopyInstr)instr;
                    result = c.getSource().retrieve(context, self, currDynScope, temp);
                    resultVar = ((ResultInstr)instr).getResult();
                    ipc++;
                    break;
                }

                // ---------- All the rest ---------
                default:
                    ipc++;
                    if (instr instanceof ResultInstr) resultVar = ((ResultInstr)instr).getResult();
                    result = instr.interpret(context, currDynScope, self, temp, block);
                    break;
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
                    UnsafeFactory.getUnsafe().throwException((Throwable)t);
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
