package org.jruby.ir.interpreter;

import org.jruby.*;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.*;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.boxing.*;
import org.jruby.ir.instructions.specialized.*;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Float;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.opto.ConstantCache;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.util.List;

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

    public static void dumpStats() {
        if ((IRRuntimeHelpers.isDebug() || IRRuntimeHelpers.inProfileMode()) && interpInstrsCount > 10000) {
            LOG.info("-- Interpreted instructions: {}", interpInstrsCount);
            /*
            for (Operation o: opStats.keySet()) {
                System.out.println(o + " = " + opStats.get(o).count);
            }
            */
        }
    }

    private static IRScope getEvalContainerScope(StaticScope evalScope) {
        // We cannot get the containing IR scope from evalScope because of static-scope wrapping
        // that is going on.
        // 1. In all cases, DynamicScope.getEvalScope wraps the executing static scope in a new local scope.
        // 2. For instance-eval (module-eval, class-eval) scenarios, there is an extra scope that is added to
        //    the stack in ThreadContext.java:preExecuteUnder

        // SSS FIXME: Hack! Have to figure out why moduleEvals introduce an extra static scope
        // and, if possible, eliminate it.
        //
        // The unwrapping is based on the binding that is used for the eval.
        // If the binding came from an instance/module eval, we need to unpeel 3 layers.
        // If the binding came from a non-module eval, we need to unpeel 2 layers.
        IRScope s = evalScope.getEnclosingScope().getIRScope();
        if (s == null) {
            s = evalScope.getEnclosingScope().getEnclosingScope().getIRScope();
        }
        return s;
    }

    public static IRubyObject interpretCommonEval(Ruby runtime, String file, int lineNumber, String backtraceName, RootNode rootNode, IRubyObject self, Block block, EvalType evalType) {
        StaticScope ss = rootNode.getStaticScope();
        IRScope containingIRScope = getEvalContainerScope(ss);
        IREvalScript evalScript = IRBuilder.createIRBuilder(runtime, runtime.getIRManager()).buildEvalRoot(ss, containingIRScope, file, lineNumber, rootNode, evalType);
        // ClosureInterpreterContext never retrieved as an operand in this context.
        // So, self operand is not required here.
        // Passing null to force early crasher if ever used differently.
        evalScript.prepareInterpreterContext(null);
        ThreadContext context = runtime.getCurrentContext();

        IRubyObject rv = null;
        DynamicScope s = rootNode.getScope();
        s.setEvalType(evalType);
        context.pushScope(s);

        try {
            // Since IR introduces additional local vars, we may need to grow the dynamic scope.
            // To do that, IREvalScript has to tell the dyn-scope how many local vars there are.
            // Since the same static scope (the scope within which the eval string showed up)
            // might be shared by multiple eval-scripts, we cannot 'setIRScope(this)' once and
            // forget about it.  We need to set this right before we are ready to grow the
            // dynamic scope local var space.
            evalScript.getStaticScope().setIRScope(evalScript);
            s.growIfNeeded();

            runBeginEndBlocks(evalScript.getBeginBlocks(), context, self, ss, null); // FIXME: No temp vars yet right?
            rv = evalScript.call(context, self, evalScript.getStaticScope().getModule(), s, block, backtraceName);
            runBeginEndBlocks(evalScript.getEndBlocks(), context, self, ss, null); // FIXME: No temp vars right?
        } finally {
            s.clearEvalType();
            context.popScope();
        }
        return rv;
    }

    public static IRubyObject interpretSimpleEval(Ruby runtime, String file, int lineNumber, String backtraceName, Node node, IRubyObject self, EvalType evalType) {
        return interpretCommonEval(runtime, file, lineNumber, backtraceName, (RootNode)node, self, Block.NULL_BLOCK, evalType);
    }

    public static IRubyObject interpretBindingEval(Ruby runtime, String file, int lineNumber, String backtraceName, Node node, IRubyObject self, Block block) {
        return interpretCommonEval(runtime, file, lineNumber, backtraceName, (RootNode)node, self, block, EvalType.BINDING_EVAL);
    }

    public static void runBeginEndBlocks(List<IRClosure> beBlocks, ThreadContext context, IRubyObject self, StaticScope currScope, Object[] temp) {
        if (beBlocks == null) return;

        for (IRClosure b: beBlocks) {
            // SSS FIXME: Should I piggyback on WrappedIRClosure.retrieve or just copy that code here?
            b.prepareInterpreterContext(b.getSelf());
            Block blk = (Block)(new WrappedIRClosure(b.getSelf(), b)).retrieve(context, self, currScope, context.getCurrentScope(), temp);
            blk.yield(context, null);
        }
    }

    @Override
    protected IRubyObject execute(Ruby runtime, IRScope scope, IRubyObject self) {
        return ((IRScriptBody)scope).interpret(runtime.getCurrentContext(), self);
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
            setResult(temp, currDynScope, ((ResultInstr) instr).getResult(), result);
        }
    }

    private static Object retrieveOp(Operand r, ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope, Object[] temp) {
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
            return r.retrieve(context, self, currScope, currDynScope, temp);
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
            throw new RuntimeException("invalid float operand: " + arg);
        }
    }

    private static long getFixnumArg(long[] fixnums, Operand arg) {
        if (arg instanceof Float) {
            return (long)((Float)arg).value;
        } else if (arg instanceof Fixnum) {
            return ((Fixnum)arg).value;
        } else if (arg instanceof Bignum) {
            return ((Bignum)arg).value.longValue();
        } else if (arg instanceof TemporaryLocalVariable) {
            return fixnums[((TemporaryLocalVariable)arg).offset];
        } else {
            throw new RuntimeException("invalid fixnum operand: " + arg);
        }
    }

    private static boolean getBooleanArg(boolean[] booleans, Operand arg) {
        if (arg instanceof UnboxedBoolean) {
            return ((UnboxedBoolean)arg).isTrue();
        } else if (arg instanceof TemporaryLocalVariable) {
            return booleans[((TemporaryLocalVariable)arg).offset];
        } else {
            throw new RuntimeException("invalid fixnum operand: " + arg);
        }
    }

    private static void setFloatVar(double[] floats, TemporaryLocalVariable var, double val) {
        floats[var.offset] = val;
    }

    private static void setFixnumVar(long[] fixnums, TemporaryLocalVariable var, long val) {
        fixnums[var.offset] = val;
    }

    private static void setBooleanVar(boolean[] booleans, TemporaryLocalVariable var, boolean val) {
        booleans[var.offset] = val;
    }

    private static void interpretIntOp(AluInstr instr, Operation op, long[] fixnums, boolean[] booleans) {
        TemporaryLocalVariable dst = (TemporaryLocalVariable)instr.getResult();
        long i1 = getFixnumArg(fixnums, instr.getArg1());
        long i2 = getFixnumArg(fixnums, instr.getArg2());
        switch (op) {
            case IADD: setFixnumVar(fixnums, dst, i1 + i2); break;
            case ISUB: setFixnumVar(fixnums, dst, i1 - i2); break;
            case IMUL: setFixnumVar(fixnums, dst, i1 * i2); break;
            case IDIV: setFixnumVar(fixnums, dst, i1 / i2); break;
            case IOR : setFixnumVar(fixnums, dst, i1 | i2); break;
            case IAND: setFixnumVar(fixnums, dst, i1 & i2); break;
            case IXOR: setFixnumVar(fixnums, dst, i1 ^ i2); break;
            case ISHL: setFixnumVar(fixnums, dst, i1 << i2); break;
            case ISHR: setFixnumVar(fixnums, dst, i1 >> i2); break;
            case ILT : setBooleanVar(booleans, dst, i1 < i2); break;
            case IGT : setBooleanVar(booleans, dst, i1 > i2); break;
            case IEQ : setBooleanVar(booleans, dst, i1 == i2); break;
            default: throw new RuntimeException("Unhandled int op: " + op + " for instr " + instr);
        }
    }

    private static void interpretFloatOp(AluInstr instr, Operation op, double[] floats, boolean[] booleans) {
        TemporaryLocalVariable dst = (TemporaryLocalVariable)instr.getResult();
        double a1 = getFloatArg(floats, instr.getArg1());
        double a2 = getFloatArg(floats, instr.getArg2());
        switch (op) {
            case FADD: setFloatVar(floats, dst, a1 + a2); break;
            case FSUB: setFloatVar(floats, dst, a1 - a2); break;
            case FMUL: setFloatVar(floats, dst, a1 * a2); break;
            case FDIV: setFloatVar(floats, dst, a1 / a2); break;
            case FLT : setBooleanVar(booleans, dst, a1 < a2); break;
            case FGT : setBooleanVar(booleans, dst, a1 > a2); break;
            case FEQ : setBooleanVar(booleans, dst, a1 == a2); break;
            default: throw new RuntimeException("Unhandled float op: " + op + " for instr " + instr);
        }
    }

    private static void receiveArg(ThreadContext context, Instr i, Operation operation, IRubyObject[] args, boolean acceptsKeywordArgument, DynamicScope currDynScope, Object[] temp, Object exception, Block block) {
        Object result;
        ResultInstr instr = (ResultInstr)i;

        switch(operation) {
        case RECV_PRE_REQD_ARG:
            int argIndex = ((ReceivePreReqdArgInstr)instr).getArgIndex();
            result = IRRuntimeHelpers.getPreArgSafe(context, args, argIndex);
            setResult(temp, currDynScope, instr.getResult(), result);
            return;
        case RECV_CLOSURE:
            result = IRRuntimeHelpers.newProc(context.runtime, block);
            setResult(temp, currDynScope, instr.getResult(), result);
            return;
        case RECV_POST_REQD_ARG:
            result = ((ReceivePostReqdArgInstr)instr).receivePostReqdArg(args, acceptsKeywordArgument);
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
            result = ((ReceiveArgBase)instr).receiveArg(context, args, acceptsKeywordArgument);
            setResult(temp, currDynScope, instr.getResult(), result);
        }
    }

    private static void processCall(ThreadContext context, Instr instr, Operation operation, DynamicScope currDynScope, StaticScope currScope, Object[] temp, IRubyObject self) {
        Object result;
        switch(operation) {
        case CALL_1F: {
            OneFixnumArgNoBlockCallInstr call = (OneFixnumArgNoBlockCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
            result = call.getCallSite().call(context, self, r, call.getFixnumArg());
            setResult(temp, currDynScope, call.getResult(), result);
            break;
        }
        case CALL_1O: {
            OneOperandArgNoBlockCallInstr call = (OneOperandArgNoBlockCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
            IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
            result = call.getCallSite().call(context, self, r, o);
            setResult(temp, currDynScope, call.getResult(), result);
            break;
        }
        case CALL_1OB: {
            OneOperandArgBlockCallInstr call = (OneOperandArgBlockCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
            IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
            Block preparedBlock = call.prepareBlock(context, self, currScope, currDynScope, temp);
            result = call.getCallSite().call(context, self, r, o, preparedBlock);
            setResult(temp, currDynScope, call.getResult(), result);
            break;
        }
        case CALL_0O: {
            ZeroOperandArgNoBlockCallInstr call = (ZeroOperandArgNoBlockCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
            result = call.getCallSite().call(context, self, r);
            setResult(temp, currDynScope, call.getResult(), result);
            break;
        }
        case NORESULT_CALL_1O: {
            OneOperandArgNoBlockNoResultCallInstr call = (OneOperandArgNoBlockNoResultCallInstr)instr;
            IRubyObject r = (IRubyObject)retrieveOp(call.getReceiver(), context, self, currDynScope, currScope, temp);
            IRubyObject o = (IRubyObject)call.getArg1().retrieve(context, self, currScope, currDynScope, temp);
            call.getCallSite().call(context, self, r, o);
            break;
        }
        case NORESULT_CALL:
            instr.interpret(context, currScope, currDynScope, self, temp);
            break;
        case CALL:
        default:
            result = instr.interpret(context, currScope, currDynScope, self, temp);
            setResult(temp, currDynScope, instr, result);
            break;
        }
    }

    private static void processBookKeepingOp(ThreadContext context, Instr instr, Operation operation,
                                             String name, IRubyObject[] args, IRubyObject self, Block block,
                                             RubyModule implClass, Visibility visibility) {
        switch(operation) {
        case PUSH_FRAME:
            context.preMethodFrameOnly(implClass, name, self, block);
            context.setCurrentVisibility(visibility);
            break;
        case POP_FRAME:
            context.popFrame();
            break;
        case POP_BINDING:
            context.popScope();
            break;
        case THREAD_POLL:
            if (IRRuntimeHelpers.inProfileMode()) Profiler.clockTick();
            context.callThreadPoll();
            break;
        case CHECK_ARITY:
            ((CheckArityInstr)instr).checkArity(context, args);
            break;
        case LINE_NUM:
            context.setLine(((LineNumberInstr)instr).lineNumber);
            break;
        case RECORD_END_BLOCK:
            ((RecordEndBlockInstr)instr).interpret();
            break;
        case TRACE: {
            if (context.runtime.hasEventHooks()) {
                TraceInstr trace = (TraceInstr) instr;
                // FIXME: Try and statically generate END linenumber instead of hacking it.
                int linenumber = trace.getLinenumber() == -1 ? context.getLine()+1 : trace.getLinenumber();

                context.trace(trace.getEvent(), trace.getName(), context.getFrameKlazz(),
                        trace.getFilename(), linenumber);
            }
            break;
        }
        }
    }

    private static IRubyObject processReturnOp(ThreadContext context, Instr instr, Operation operation, DynamicScope currDynScope, Object[] temp, IRubyObject self, Block.Type blockType, StaticScope currScope)
    {
        switch(operation) {
        // --------- Return flavored instructions --------
        case RETURN: {
            return (IRubyObject)retrieveOp(((ReturnBase)instr).getReturnValue(), context, self, currDynScope, currScope, temp);
        }
        case BREAK: {
            BreakInstr bi = (BreakInstr)instr;
            IRubyObject rv = (IRubyObject)bi.getReturnValue().retrieve(context, self, currScope, currDynScope, temp);
            // This also handles breaks in lambdas -- by converting them to a return
            //
            // This assumes that scopes with break instr. have a frame / dynamic scope
            // pushed so that we can get to its static scope. For-loops now always have
            // a dyn-scope pushed onto stack which makes this work in all scenarios.
            return IRRuntimeHelpers.initiateBreak(context, currDynScope, rv, blockType);
        }
        case NONLOCAL_RETURN: {
            NonlocalReturnInstr ri = (NonlocalReturnInstr)instr;
            IRubyObject rv = (IRubyObject)retrieveOp(ri.getReturnValue(), context, self, currDynScope, currScope, temp);
            return IRRuntimeHelpers.initiateNonLocalReturn(context, currDynScope, blockType, ri.maybeLambda, rv);
        }
        }
        return null;
    }

    private static void processOtherOp(ThreadContext context, Instr instr, Operation operation, DynamicScope currDynScope, StaticScope currScope, Object[] temp, IRubyObject self, Block.Type blockType, double[] floats, long[] fixnums, boolean[] booleans)
    {
        Object result;
        switch(operation) {
        case COPY: {
            CopyInstr c = (CopyInstr)instr;
            Operand  src = c.getSource();
            Variable res = c.getResult();
            if (res instanceof TemporaryFloatVariable) {
                setFloatVar(floats, (TemporaryFloatVariable)res, getFloatArg(floats, src));
            } else if (res instanceof TemporaryFixnumVariable) {
                setFixnumVar(fixnums, (TemporaryFixnumVariable)res, getFixnumArg(fixnums, src));
            } else {
                setResult(temp, currDynScope, res, retrieveOp(src, context, self, currDynScope, currScope, temp));
            }
            break;
        }

        case GET_FIELD: {
            GetFieldInstr gfi = (GetFieldInstr)instr;
            IRubyObject object = (IRubyObject)gfi.getSource().retrieve(context, self, currScope, currDynScope, temp);
            VariableAccessor a = gfi.getAccessor(object);
            result = a == null ? null : (IRubyObject)a.get(object);
            if (result == null) {
                if (context.runtime.isVerbose()) {
                    context.runtime.getWarnings().warning(ID.IVAR_NOT_INITIALIZED, "instance variable " + gfi.getRef() + " not initialized");
                }
                result = context.nil;
            }
            setResult(temp, currDynScope, gfi.getResult(), result);
            break;
        }

        case SEARCH_CONST: {
            SearchConstInstr sci = (SearchConstInstr)instr;
            ConstantCache cache = sci.getConstantCache();
            if (!ConstantCache.isCached(cache)) {
                result = sci.cache(context, currScope, currDynScope, self, temp);
            } else {
                result = cache.value;
            }
            setResult(temp, currDynScope, sci.getResult(), result);
            break;
        }

        case RUNTIME_HELPER: {
            RuntimeHelperCall rhc = (RuntimeHelperCall)instr;
            result = rhc.callHelper(context, currScope, currDynScope, self, temp, blockType);
            setResult(temp, currDynScope, rhc.getResult(), result);
            break;
        }

        case BOX_FLOAT: {
            RubyFloat f = context.runtime.newFloat(getFloatArg(floats, ((BoxFloatInstr)instr).getValue()));
            setResult(temp, currDynScope, ((BoxInstr)instr).getResult(), f);
            break;
        }

        case BOX_FIXNUM: {
            RubyFixnum f = context.runtime.newFixnum(getFixnumArg(fixnums, ((BoxFixnumInstr) instr).getValue()));
            setResult(temp, currDynScope, ((BoxInstr)instr).getResult(), f);
            break;
        }

        case BOX_BOOLEAN: {
            RubyBoolean f = context.runtime.newBoolean(getBooleanArg(booleans, ((BoxBooleanInstr) instr).getValue()));
            setResult(temp, currDynScope, ((BoxInstr)instr).getResult(), f);
            break;
        }

        case UNBOX_FLOAT: {
            UnboxInstr ui = (UnboxInstr)instr;
            Object val = retrieveOp(ui.getValue(), context, self, currDynScope, currScope, temp);
            if (val instanceof RubyFloat) {
                floats[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFloat)val).getValue();
            } else {
                floats[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFixnum)val).getDoubleValue();
            }
            break;
        }

        case UNBOX_FIXNUM: {
            UnboxInstr ui = (UnboxInstr)instr;
            Object val = retrieveOp(ui.getValue(), context, self, currDynScope, currScope, temp);
            if (val instanceof RubyFloat) {
                fixnums[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFloat)val).getLongValue();
            } else {
                fixnums[((TemporaryLocalVariable)ui.getResult()).offset] = ((RubyFixnum)val).getLongValue();
            }
            break;
        }

        // ---------- All the rest ---------
        default:
            result = instr.interpret(context, currScope, currDynScope, self, temp);
            setResult(temp, currDynScope, instr, result);
            break;
        }
    }

    private static DynamicScope getNewDynScope(ThreadContext context, IRScope scope, StaticScope currScope) {
        // SSS NOTE: Method/module scopes only!
        //
        // Blocks are a headache -- so, these instrs. are only added to IRMethods.
        // Blocks have more complicated logic for pushing a dynamic scope (see InterpretedIRBlockBody)
        if (scope instanceof IRMetaClassBody) {
            // Add a parent-link to current dynscope to support non-local returns cheaply
            // This doesn't affect variable scoping since local variables will all have
            // the right scope depth.
            return DynamicScope.newDynamicScope(currScope, context.getCurrentScope());
        } else {
            return DynamicScope.newDynamicScope(currScope);
        }
    }

    private static IRubyObject interpret(ThreadContext context, IRubyObject self,
            IRScope scope, Visibility visibility, RubyModule implClass, String name, IRubyObject[] args, Block block, Block.Type blockType) {
        InterpreterContext interpreterContext = scope.getInterpreterContext();
        Instr[] instrs = interpreterContext.getInstructions();
        int      numTempVars    = interpreterContext.getTemporaryVariablecount();
        Object[] temp           = numTempVars > 0 ? new Object[numTempVars] : null;
        int      numFloatVars   = interpreterContext.getTemporaryFloatVariablecount();
        int      numFixnumVars  = interpreterContext.getTemporaryFixnumVariablecount();
        int      numBooleanVars = interpreterContext.getTemporaryBooleanVariablecount();
        double[] floats         = numFloatVars > 0 ? new double[numFloatVars] : null;
        long[]   fixnums        = numFixnumVars > 0 ? new long[numFixnumVars] : null;
        boolean[]   booleans    = numBooleanVars > 0 ? new boolean[numBooleanVars] : null;
        int      n              = instrs.length;
        int      ipc            = 0;
        Object   exception      = null;
        DynamicScope currDynScope = context.getCurrentScope();
        StaticScope currScope = interpreterContext.getStaticScope();

        // Init profiling this scope
        boolean debug   = IRRuntimeHelpers.isDebug();
        boolean profile = IRRuntimeHelpers.inProfileMode();
        Integer scopeVersion = profile ? Profiler.initProfiling(scope) : 0;
        boolean acceptsKeywordArgument = interpreterContext.receivesKeywordArguments();

        // Enter the looooop!
        while (ipc < n) {
            Instr instr = instrs[ipc];
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
                switch (operation.opClass) {
                case INT_OP:
                    interpretIntOp((AluInstr) instr, operation, fixnums, booleans);
                    break;
                case FLOAT_OP:
                    interpretFloatOp((AluInstr) instr, operation, floats, booleans);
                    break;
                case ARG_OP:
                    receiveArg(context, instr, operation, args, acceptsKeywordArgument, currDynScope, temp, exception, block);
                    break;
                case CALL_OP:
                    if (profile) Profiler.updateCallSite(instr, scope, scopeVersion);
                    processCall(context, instr, operation, currDynScope, currScope, temp, self);
                    break;
                case RET_OP:
                    return processReturnOp(context, instr, operation, currDynScope, temp, self, blockType, currScope);
                case BRANCH_OP:
                    switch (operation) {
                    case JUMP: ipc = ((JumpInstr)instr).getJumpTarget().getTargetPC(); break;
                    default: ipc = instr.interpretAndGetNewIPC(context, currDynScope, currScope, self, temp, ipc); break;
                    }
                    break;
                case BOOK_KEEPING_OP:
                    if (operation == Operation.PUSH_BINDING) {
                        currDynScope = getNewDynScope(context, scope, currScope);
                        context.pushScope(currDynScope);
                    } else {
                        processBookKeepingOp(context, instr, operation, name, args, self, block, implClass, visibility);
                    }
                    break;
                case OTHER_OP:
                    processOtherOp(context, instr, operation, currDynScope, currScope, temp, self, blockType, floats, fixnums, booleans);
                    break;
                }
            } catch (Throwable t) {
                extractToMethodToAvoidC2Crash(context, instr, t);

                if (debug) LOG.info("in scope: " + scope + ", caught Java throwable: " + t + "; excepting instr: " + instr);
                ipc = instr.getRPC();
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

    /*
     * If you put this code into the method above it will hard crash some production builds of C2 in Java 8. We aren't
     * sure exactly which builds, but it seems to appear more often in Linux builds than Mac. - Chris Seaton
     */

    private static void extractToMethodToAvoidC2Crash(ThreadContext context, Instr instr, Throwable t) {
        if (!(t instanceof Unrescuable)) {
            if (!instr.canRaiseException()) {
                System.err.println("ERROR: Got exception " + t + " but instr " + instr + " is not supposed to be raising exceptions!");
            }
            if ((t instanceof RaiseException) && context.runtime.getGlobalVariables().get("$!") != IRRuntimeHelpers.unwrapRubyException(t)) {
                System.err.println("ERROR: $! and exception are not matching up.");
                System.err.println("$!: " + context.runtime.getGlobalVariables().get("$!"));
                System.err.println("t : " + t);
            }
        }
    }

    public static IRubyObject INTERPRET_ROOT(ThreadContext context, IRubyObject self,
            IRScope scope, RubyModule clazz, String name) {
        try {
            ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            return interpret(context, self, scope, null, clazz, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK, null);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_EVAL(ThreadContext context, IRubyObject self,
            IRScope scope, RubyModule clazz, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            return interpret(context, self, scope, null, clazz, name, args, block, blockType);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_BLOCK(ThreadContext context, IRubyObject self,
            IRScope scope, IRubyObject[] args, String name, Block block, Block.Type blockType) {
        try {
            ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());
            return interpret(context, self, scope, null, null, name, args, block, blockType);
        } finally {
            ThreadContext.popBacktrace(context);
        }
    }

    public static IRubyObject INTERPRET_METHOD(ThreadContext context, InterpretedIRMethod method,
        IRubyObject self, String name, IRubyObject[] args, Block block) {
        IRScope    scope     = method.getIRMethod();
        boolean syntheticMethod = name == null || name.equals("");

        try {
            if (!syntheticMethod) ThreadContext.pushBacktrace(context, name, scope.getFileName(), context.getLine());

            return interpret(context, self, scope, method.getVisibility(), method.getImplementationClass(), name, args, block, null);
        } finally {
            if (!syntheticMethod) ThreadContext.popBacktrace(context);
        }
    }

    /**
     * Evaluate the given string.
     * @param context the current thread's context
     * @param self the self to evaluate under
     * @param src The string containing the text to be evaluated
     * @param file The filename to use when reporting errors during the evaluation
     * @param lineNumber that the eval supposedly starts from
     * @return An IRubyObject result from the evaluation
     */
    public static IRubyObject evalSimple(ThreadContext context, IRubyObject self, RubyString src, String file, int lineNumber, EvalType evalType) {
        // this is ensured by the caller
        assert file != null;

        Ruby runtime = src.getRuntime();

        // no binding, just eval in "current" frame (caller's frame)
        RubyString source = src.convertToString();

        DynamicScope evalScope = context.getCurrentScope().getEvalScope(runtime);
        evalScope.getStaticScope().determineModule();

        try {
            Node node = runtime.parseEval(source.getByteList(), file, evalScope, lineNumber);

            if (runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.TRUFFLE) {
                throw new UnsupportedOperationException();
            }

            // SSS FIXME: AST interpreter passed both a runtime (which comes from the source string)
            // and the thread-context rather than fetch one from the other.  Why is that?
            return Interpreter.interpretSimpleEval(runtime, file, lineNumber, "(eval)", node, self, evalType);
/*
 * SSS FIXME: Why was this here?
 * Do we need an IR equivalent here?
 *
        } catch (JumpException.BreakJump bj) {
            throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "unexpected break");
*/
        } catch (StackOverflowError soe) {
            throw runtime.newSystemStackError("stack level too deep", soe);
        }
    }

    /**
     * Evaluate the given string under the specified binding object. If the binding is not a Proc or Binding object
     * (RubyProc or RubyBinding) throw an appropriate type error.
     * @param context the thread context for the current thread
     * @param self the self against which eval was called; used as self in the eval in 1.9 mode
     * @param src The string containing the text to be evaluated
     * @param binding The binding object under which to perform the evaluation
     * @return An IRubyObject result from the evaluation
     */
    public static IRubyObject evalWithBinding(ThreadContext context, IRubyObject self, IRubyObject src, Binding binding) {
        Ruby runtime = src.getRuntime();
        DynamicScope evalScope;

        // in 1.9, eval scopes are local to the binding
        evalScope = binding.getEvalScope(runtime);
        evalScope.setEvalType(EvalType.BINDING_EVAL);

        // FIXME:  This determine module is in a strange location and should somehow be in block
        evalScope.getStaticScope().determineModule();

        Frame lastFrame = context.preEvalWithBinding(binding);
        try {
            // Binding provided for scope, use it
            RubyString source = src.convertToString();
            Node node = runtime.parseEval(source.getByteList(), binding.getFile(), evalScope, binding.getLine());
            Block block = binding.getFrame().getBlock();

            if (runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.TRUFFLE) {
                throw new UnsupportedOperationException();
            }

            // SSS FIXME: AST interpreter passed both a runtime (which comes from the source string)
            // and the thread-context rather than fetch one from the other.  Why is that?
            return Interpreter.interpretBindingEval(runtime, binding.getFile(), binding.getLine(), binding.getMethod(), node, self, block);
        } catch (StackOverflowError soe) {
            throw runtime.newSystemStackError("stack level too deep", soe);
        } finally {
            context.postEvalWithBinding(binding, lastFrame);
        }
    }
}
