package org.jruby.ir.interpreter;

import java.util.List;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyFloat;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.IRTranslator;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.boxing.AluInstr;
import org.jruby.ir.instructions.boxing.BoxFloatInstr;
import org.jruby.ir.instructions.boxing.BoxInstr;
import org.jruby.ir.instructions.boxing.UnboxInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.instructions.ReceiveRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveJRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.RecordEndBlockInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.TemporaryFloatVariable;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
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
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class Interpreter extends IRTranslator<IRubyObject, IRubyObject> {

    private static final Logger LOG = LoggerFactory.getLogger("Interpreter");

    private static int interpInstrsCount = 0;

    // we do not need instances of Interpreter
    // FIXME: Should we make it real singleton and get rid of static methods?
    private Interpreter() { }

    private static class InterpreterHolder {
        // FIXME: Remove static reference unless lifus does later
        public static final Interpreter instance = new Interpreter();
    }

    public static Interpreter getInstance() {
        return InterpreterHolder.instance;
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

    public static IRubyObject interpretCommonEval(Ruby runtime, String file, int lineNumber, String backtraceName, RootNode rootNode, IRubyObject self, Block block) {
        StaticScope ss = rootNode.getStaticScope();
        IRScope containingIRScope = getEvalContainerScope(runtime, ss);
        IREvalScript evalScript = IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildEvalRoot(ss, containingIRScope, file, lineNumber, rootNode);
        evalScript.prepareForInterpretation(false);
        ThreadContext context = runtime.getCurrentContext();

        IRubyObject rv = null;
        try {
            DynamicScope s = rootNode.getScope();
            context.pushScope(s);

            // Since IR introduces additional local vars, we may need to grow the dynamic scope.
            // To do that, IREvalScript has to tell the dyn-scope how many local vars there are.
            // Since the same static scope (the scope within which the eval string showed up)
            // might be shared by multiple eval-scripts, we cannot 'setIRScope(this)' once and
            // forget about it.  We need to set this right before we are ready to grow the
            // dynamic scope local var space.
            ((IRStaticScope)evalScript.getStaticScope()).setIRScope(evalScript);
            s.growIfNeeded();

            runBeginEndBlocks(evalScript.getBeginBlocks(), context, self, null); // FIXME: No temp vars yet right?
            rv = evalScript.call(context, self, evalScript.getStaticScope().getModule(), s, block, backtraceName);
            runBeginEndBlocks(evalScript.getEndBlocks(), context, self, null); // FIXME: No temp vars right?
        } finally {
            context.popScope();
        }
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
            Block blk = (Block)(new WrappedIRClosure(b.getSelf(), b)).retrieve(context, self, context.getCurrentScope(), temp);
            blk.yield(context, null);
        }
    }

    @Override
    protected IRubyObject execute(Ruby runtime, IRScope scope, IRubyObject self) {
        IRScriptBody root = (IRScriptBody) scope;

        // FIXME: Removed as part of merge...likely broken at this point in merge.
    //    IRScriptBody root = (IRScriptBody) IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildRoot((RootNode) rootNode);

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

    private static void setResult(Object[] temp, DynamicScope currDynScope, Variable resultVar, Object result) {
        if (resultVar instanceof TemporaryVariable) {
            // Unboxed Java primitives (float/double/int/long) don't come here because result is an Object
            // So, it is safe to use offset directly without any correction as long as IRScope uses
            // three different allocators (each with its own 'offset' counter)
            // * one for LOCAL, BOOLEAN, CURRENT_SCOPE, CURRENT_MODULE, CLOSURE tmpvars
            // * one for FIXNUM
            // * one for FLOAT
            temp[((TemporaryLocalVariable)resultVar).offset] = result;
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
        } else if (r instanceof TemporaryLocalVariable) {
            res = temp[((TemporaryLocalVariable)r).offset];
            return res == null ? context.nil : res;
        } else if (r instanceof LocalVariable) {
            LocalVariable lv = (LocalVariable)r;
            res = currDynScope.getValue(lv.getLocation(), lv.getScopeDepth());
            return res == null ? context.nil : res;
        } else {
            return r.retrieve(context, self, currDynScope, temp);
        }

    }

    private static double getFloatArg(double[] floats, Operand arg) {
        if (arg instanceof Float) {
            return ((Float)arg).value;
        } else if (arg instanceof Fixnum) {
            return (double)((Fixnum)arg).value;
        } else if (arg instanceof Bignum) {
            return ((Bignum)arg).value.doubleValue();
        } else if (arg instanceof TemporaryLocalVariable) {
            return floats[((TemporaryLocalVariable)arg).offset];
        } else {
            return 0.0/0.0;
        }
    }

    private static void setFloatVar(double[] floats, TemporaryLocalVariable var, double val) {
        floats[var.offset] = val;
    }

    private static void setBooleanVar(ThreadContext context, Object[] temp, TemporaryLocalVariable var, boolean val) {
        BooleanLiteral bVal = val ? BooleanLiteral.TRUE : BooleanLiteral.FALSE;
        temp[var.offset] = bVal.cachedObject(context);
    }

    private static void computeResult(AluInstr instr, Operation op, ThreadContext context, double[] floats, Object[] temp) {
        TemporaryLocalVariable dst = (TemporaryLocalVariable)instr.getResult();
        double a1 = getFloatArg(floats, instr.getArg1());
        double a2 = getFloatArg(floats, instr.getArg2());
        switch (op) {
        case FADD: setFloatVar(floats, dst, a1 + a2); break;
        case FSUB: setFloatVar(floats, dst, a1 - a2); break;
        case FMUL: setFloatVar(floats, dst, a1 * a2); break;
        case FDIV: setFloatVar(floats, dst, a1 / a2); break;
        case FLT : setBooleanVar(context, temp, dst, a1 < a2); break;
        case FGT : setBooleanVar(context, temp, dst, a1 > a2); break;
        }
    }

    private static void receiveArg(ThreadContext context, Instr i, Operation operation, IRubyObject[] args, int kwArgHashCount, DynamicScope currDynScope, Object[] temp, Object exception, Block block) {
        Object result = null;
        ResultInstr instr = (ResultInstr)i;
        switch(operation) {
        case RECV_PRE_REQD_ARG:
            int argIndex = ((ReceivePreReqdArgInstr)instr).getArgIndex();
            result = argIndex < args.length ? args[argIndex] : context.nil; // SSS FIXME: This check is only required for closures, not methods
            setResult(temp, currDynScope, instr.getResult(), result);
            return;
        case RECV_CLOSURE:
            result = (block == Block.NULL_BLOCK) ? context.nil : context.runtime.newProc(Block.Type.PROC, block);
            setResult(temp, currDynScope, instr.getResult(), result);
            return;
        case RECV_OPT_ARG:
            result = ((ReceiveOptArgInstr)instr).receiveOptArg(args, kwArgHashCount);
            // For blocks, missing arg translates to nil
            setResult(temp, currDynScope, instr.getResult(), result);
            return;
        case RECV_POST_REQD_ARG:
            result = ((ReceivePostReqdArgInstr)instr).receivePostReqdArg(args, kwArgHashCount);
            // For blocks, missing arg translates to nil
            setResult(temp, currDynScope, instr.getResult(), result == null ? context.nil : result);
            return;
        case RECV_RUBY_EXC:
            setResult(temp, currDynScope, instr.getResult(), IRRuntimeHelpers.unwrapRubyException(exception));
            return;
        case RECV_JRUBY_EXC:
            setResult(temp, currDynScope, instr.getResult(), exception);
            return;
        default:
            result = ((ReceiveArgBase)instr).receiveArg(context, kwArgHashCount, args);
            setResult(temp, currDynScope, instr.getResult(), result);
            return;
        }
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

    private static void processBookKeepingOp(ThreadContext context, Instr instr, Operation operation, IRScope scope, int numArgs, int kwArgHashCount, IRubyObject self, Block block, RubyModule implClass, Visibility visibility)
    {
        switch(operation) {
        case PUSH_FRAME:
            context.preMethodFrameAndClass(implClass, scope.getName(), self, block, scope.getStaticScope());
            context.setCurrentVisibility(visibility);
            break;
        case POP_FRAME:
            context.popFrame();
            context.popRubyClass();
            break;
        case POP_BINDING:
            context.popScope();
            break;
        case THREAD_POLL:
            if (IRRuntimeHelpers.inProfileMode()) Profiler.clockTick();
            context.callThreadPoll();
            break;
        case CHECK_ARITY:
            ((CheckArityInstr)instr).checkArity(context.runtime, numArgs, kwArgHashCount);
            break;
        case LINE_NUM:
            context.setLine(((LineNumberInstr)instr).lineNumber);
            break;
        case RECORD_END_BLOCK:
            ((RecordEndBlockInstr)instr).interpret();
            break;
        }
    }

    private static IRubyObject processReturnOp(ThreadContext context, Instr instr, Operation operation, IRScope scope, DynamicScope currDynScope, Object[] temp, IRubyObject self, Block.Type blockType)
    {
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
            // If not in a lambda, check if this was a non-local return
            if (!IRRuntimeHelpers.inLambda(blockType)) {
                IRRuntimeHelpers.initiateNonLocalReturn(context, scope, ri.methodToReturnFrom, rv);
            }
            return rv;
        }
        }
        return null;
    }

    private static IRubyObject interpret(ThreadContext context, IRubyObject self,
            IRScope scope, Visibility visibility, RubyModule implClass, IRubyObject[] args, Block block, Block.Type blockType)
    {
        Instr[] instrs = scope.getInstrsForInterpretation(blockType == Block.Type.LAMBDA);
        Map<Integer, Integer> rescueMap = scope.getRescueMap();

        int      numTempVars    = scope.getTemporaryVariablesCount();
//        System.out.println("NUM: temp vars: " + numTempVars);
        Object[] temp           = numTempVars > 0 ? new Object[numTempVars] : null;
        int      numFloatVars   = scope.getFloatVariablesCount();
        double[] floats         = numFloatVars > 0 ? new double[numFloatVars] : null;
        int      n              = instrs.length;
        int      ipc            = 0;
        Instr    instr          = null;
        Object   exception      = null;
        int      kwArgHashCount = (scope.receivesKeywordArgs() && args.length > 0 && args[args.length - 1] instanceof RubyHash) ? 1 : 0;
        DynamicScope currDynScope = context.getCurrentScope();

        // Counter tpCount = null;

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? Profiler.initProfiling(scope) : 0;

        // Enter the looooop!
        while (ipc < n) {
            instr = instrs[ipc];
            ipc++;
            Operation operation = instr.getOperation();
            if (debug) {
                LOG.info("I: {}", instr);
                interpInstrsCount++;
            } else if (profile) {
                Profiler.instrTick(operation);
                interpInstrsCount++;
            }

            try {
                Object result = null;
                switch (operation.opClass) {
                case ALU_OP:
                    computeResult((AluInstr)instr, operation, context, floats, temp);
                    break;
                case ARG_OP:
                    receiveArg(context, instr, operation, args, kwArgHashCount, currDynScope, temp, exception, block);
                    break;
                case BRANCH_OP:
                    switch (operation) {
                    case JUMP: ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC(); break;
                    default: ipc = instr.interpretAndGetNewIPC(context, currDynScope, self, temp, ipc); break;
                    }
                    break;
                case CALL_OP:
                    if (profile) Profiler.updateCallSite(instr, scope, scopeVersion);
                    processCall(context, instr, operation, scope, currDynScope, temp, self, block, blockType);
                    break;
                case BOOK_KEEPING_OP:
                    if (operation == Operation.PUSH_BINDING) {
                        // SSS NOTE: Method scopes only!
                        //
                        // Blocks are a headache -- so, these instrs. are only added to IRMethods.
                        // Blocks have more complicated logic for pushing a dynamic scope (see InterpretedIRBlockBody)
                        currDynScope = DynamicScope.newDynamicScope(scope.getStaticScope());
                        context.pushScope(currDynScope);
                    } else {
                        processBookKeepingOp(context, instr, operation, scope, args.length, kwArgHashCount, self, block, implClass, visibility);
                    }
                    break;
                case RET_OP:
                    return processReturnOp(context, instr, operation, scope, currDynScope, temp, self, blockType);
                case OTHER_OP:
                    switch(operation) {
                    // ---------- Other instructions ---------
                    case COPY: {
                        CopyInstr c = (CopyInstr)instr;
                        Operand  src = c.getSource();
                        Variable res = c.getResult();
                        if (res instanceof TemporaryFloatVariable) {
                            setFloatVar(floats, (TemporaryFloatVariable)res, getFloatArg(floats, src));
                        } else {
                            setResult(temp, currDynScope, res, retrieveOp(src, context, self, currDynScope, temp));
                        }
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

                    case BOX_FLOAT: {
                        RubyFloat f = context.runtime.newFloat(getFloatArg(floats, ((BoxFloatInstr)instr).getValue()));
                        setResult(temp, currDynScope, ((BoxInstr)instr).getResult(), f);
                        break;
                    }

                    case UNBOX_FLOAT: {
                        UnboxInstr ui = (UnboxInstr)instr;
                        Object val = retrieveOp(ui.getValue(), context, self, currDynScope, temp);
                        if (val instanceof RubyFloat) {
                            floats[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFloat)val).getValue();
                        } else {
                            floats[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFixnum)val).getDoubleValue();
                        }
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
            } catch (Throwable t) {
                if (debug) LOG.info("in scope: " + scope + ", caught Java throwable: " + t + "; excepting instr: " + instr);
                ipc = rescueMap.get(instr.getIPC());
                if (debug) LOG.info("ipc for rescuer: " + ipc);

                if (ipc == -1) {
                    Helpers.throwException(t);
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
