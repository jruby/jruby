package org.jruby.ir.runtime;

import com.headius.invokebinder.Signature;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

import org.jcodings.Encoding;
import org.jruby.EvalType;
import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyComplex;
import org.jruby.RubyEncoding;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyMatchData;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
import org.jruby.RubyRational;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.CompiledIRNoProtocolMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMetaClassBody;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Interp;
import org.jruby.ir.JIT;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.java.invokers.InstanceMethodInvoker;
import org.jruby.java.invokers.RubyToJavaInvoker;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaMethod;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.IRBlockBody;
import org.jruby.runtime.JavaSites.IRRuntimeHelpersSites;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.MonomorphicCallSite;
import org.jruby.runtime.callsite.ProfilingCachingCallSite;
import org.jruby.runtime.callsite.RefinedCachingCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Type;

import static org.jruby.runtime.Block.Type.LAMBDA;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.runtime.Arity.UNLIMITED_ARGUMENTS;

public class IRRuntimeHelpers {
    private static final Logger LOG = LoggerFactory.getLogger(IRRuntimeHelpers.class);

    public static boolean inProfileMode() {
        return RubyInstanceConfig.IR_PROFILE;
    }

    public static boolean isDebug() {
        return RubyInstanceConfig.IR_DEBUG;
    }

    public static boolean inNonMethodBodyLambda(StaticScope scope, Block.Type blockType) {
        // SSS FIXME: Hack! AST interpreter and JIT compiler marks a proc's static scope as
        // an argument scope if it is used to define a method's body via :define_method.
        // Since that is exactly what we want to figure out here, am just using that flag here.
        // But, this is ugly (as is the original hack in the current runtime).  What is really
        // needed is a new block type -- a block that is used to define a method body.
        return blockType == LAMBDA && !scope.isArgumentScope();
    }

    public static boolean inMethod(Block.Type blockType) {
        return blockType == null;
    }

    public static boolean inLambda(Block.Type blockType) {
        return blockType == LAMBDA;
    }

    public static boolean inProc(Block.Type blockType) {
        return blockType == Block.Type.PROC;
    }

    // FIXME: ENEBO: If we inline this instr then dynScope will be for the inlined dynscope and that scope could be many things.
    //   CheckForLJEInstr.clone should convert this as appropriate based on what it is being inlined into.
    @Interp @JIT
    public static void checkForLJE(ThreadContext context, DynamicScope currentScope, boolean definedWithinMethod, Block block) {
        if (inLambda(block.type)) return; // break/return in lambda is unconditionally a return.

        DynamicScope returnToScope = getContainingReturnToScope(currentScope);

        if (returnToScope == null || !context.scopeExistsOnCallStack(returnToScope)) {
            throw IRException.RETURN_LocalJumpError.getException(context.runtime);
        }

    }

    /*
     * Closures cannot statically determine whether they are a proc or a lambda.  So we look at the live stack
     * to see if we are within one.
     *
     * Note: as a result of this all lambdas which contain returns in nested scopes (or itself) can never eliminate
     * its binding.
     */
    private static DynamicScope getContainingLambda(DynamicScope dynamicScope) {
        for (DynamicScope scope = dynamicScope; scope != null && scope.getStaticScope().isBlockScope(); scope = scope.getParentScope()) {
            // we are within a lambda but not a define_method (which seemingly advertises itself as a lambda).
            if (scope.isLambda() && !scope.getStaticScope().isArgumentScope()) return scope;
        }

        return null;
    }

    // Create a jump for a non-local return which will return from nearest lambda (which may be itself) or method.
    @JIT @Interp
    public static IRubyObject initiateNonLocalReturn(DynamicScope currentScope, Block block, IRubyObject returnValue) {
        if (block != null && inLambda(block.type)) throw new IRWrappedLambdaReturnValue(returnValue);

        DynamicScope returnToScope = getContainingLambda(currentScope);

        if (returnToScope == null) returnToScope = getContainingReturnToScope(currentScope);

        assert returnToScope != null: "accidental return scope";

        throw IRReturnJump.create(currentScope.getStaticScope(), returnToScope, returnValue);
    }

    // Finds static scope of where we want to *return* to.
    private static DynamicScope getContainingReturnToScope(DynamicScope returnLocationScope) {
        for (DynamicScope current = returnLocationScope; current != null; current = current.getParentScope()) {
            if (current.isReturnTarget()) return current;
        }

        return null;
    }

    @JIT
    public static IRubyObject handleNonlocalReturn(DynamicScope currentScope, Object rjExc) throws RuntimeException {
        if (!(rjExc instanceof IRReturnJump)) {
            Helpers.throwException((Throwable)rjExc);
            return null; // Unreachable
        } else {
            IRReturnJump rj = (IRReturnJump)rjExc;

            // If we are in the method scope we are supposed to return from, stop p<ropagating.
            if (rj.isReturnToScope(currentScope)) {
                if (isDebug()) System.out.println("---> Non-local Return reached target in scope: " + currentScope);
                return (IRubyObject) rj.returnValue;
            }

            // If not, Just pass it along!
            throw rj;
        }
    }

    // Is the current dynamicScope we pass in representing a closure (or eval which is impld internally as closure)?
    private static IRScopeType ensureScopeIsClosure(ThreadContext context, DynamicScope dynamicScope) {
        IRScopeType scopeType = dynamicScope.getStaticScope().getScopeType();

        // Error -- breaks can only be initiated in closures
        if (!scopeType.isClosureType()) throw IRException.BREAK_LocalJumpError.getException(context.runtime);

        return scopeType;
    }

    // FIXME: When we recompile lambdas we can eliminate this binary code path and we can emit as a NONLOCALRETURN directly.
    @JIT
    public static IRubyObject initiateBreak(ThreadContext context, DynamicScope dynScope, IRubyObject breakValue, Block block) throws RuntimeException {
        // Wrap the return value in an exception object and push it through the break exception
        // paths so that ensures are run, frames/scopes are popped from runtime stacks, etc.
        if (inLambda(block.type)) throw new IRWrappedLambdaReturnValue(breakValue, true);

        IRScopeType scopeType = ensureScopeIsClosure(context, dynScope);

        DynamicScope parentScope = dynScope.getParentScope();

        if (block.isEscaped()) {
            throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, breakValue, "unexpected break");
        }

        // Raise a break jump so we can bubble back down the stack to the appropriate place to break from.
        throw IRBreakJump.create(parentScope, breakValue, scopeType.isEval()); // weirdly evals are impld as closures...yes yes.
    }

    // Are we within the scope where we want to return the value we are passing down the stack?
    private static boolean inReturnToScope(Block.Type blockType, IRReturnJump exception, DynamicScope currentScope) {
        return (inMethod(blockType) || inLambda(blockType)) && exception.isReturnToScope(currentScope);
    }

    @JIT
    public static IRubyObject handleBreakAndReturnsInLambdas(ThreadContext context, DynamicScope dynScope, Object exc, Block block) throws RuntimeException {
        if (exc instanceof IRWrappedLambdaReturnValue) {
            // Wrap the return value in an exception object and push it through the nonlocal return exception
            // paths so that ensures are run, frames/scopes are popped from runtime stacks, etc.
            return ((IRWrappedLambdaReturnValue) exc).returnValue;
        } else if (exc instanceof IRReturnJump && dynScope != null && inReturnToScope(block.type, (IRReturnJump) exc, dynScope)) {
            if (isDebug()) System.out.println("---> Non-local Return reached target in scope: " + dynScope);
            return (IRubyObject) ((IRReturnJump) exc).returnValue;
        } else {
            // Propagate the exception
            context.setSavedExceptionInLambda((Throwable) exc);
            return null;
        }
    }

    @JIT
    public static IRubyObject returnOrRethrowSavedException(ThreadContext context, IRubyObject value) {
        // This rethrows the exception saved in handleBreakAndReturnsInLambda
        // after additional code to pop frames, bindings, etc. are done.
        Throwable exc = context.getSavedExceptionInLambda();
        if (exc != null) {
            // IMPORTANT: always clear!
            context.setSavedExceptionInLambda(null);
            Helpers.throwException(exc);
        }

        // otherwise, return value
        return value;
    }

    @JIT
    public static IRubyObject handlePropagatedBreak(ThreadContext context, DynamicScope dynScope, Object bjExc) {
        if (!(bjExc instanceof IRBreakJump)) {
            Helpers.throwException((Throwable)bjExc);
            return null; // Unreachable
        }

        IRBreakJump bj = (IRBreakJump)bjExc;
        if (bj.breakInEval) {
            // If the break was in an eval, we pretend as if it was in the containing scope.
            ensureScopeIsClosure(context, dynScope);

            bj.breakInEval = false;
            throw bj;
        } else if (bj.scopeToReturnTo == dynScope) {
            // Done!! Hurray!
            if (isDebug()) System.out.println("---> Break reached target in scope: " + dynScope);
            return bj.breakValue;
/* ---------------------------------------------------------------
 * SSS FIXME: Puzzled .. Why is this not needed?
        } else if (!context.scopeExistsOnCallStack(bj.scopeToReturnTo.getStaticScope())) {
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
 * --------------------------------------------------------------- */
        } else {
            // Propagate
            throw bj;
        }
    }

    // Used by JIT
    public static IRubyObject undefMethod(ThreadContext context, Object nameArg, DynamicScope currDynScope, IRubyObject self) {
        RubyModule module = IRRuntimeHelpers.findInstanceMethodContainer(context, currDynScope, self);
        String name = (nameArg instanceof String) ?
                (String) nameArg : nameArg.toString();

        if (module == null) {
            throw context.runtime.newTypeError("No class to undef method '" + name + "'.");
        }

        module.undef(context, name);

        return context.nil;
    }

    @JIT
    public static double unboxFloat(IRubyObject val) {
        if (val instanceof RubyFloat) {
            return ((RubyFloat)val).getValue();
        } else {
            return ((RubyFixnum)val).getDoubleValue();
        }
    }

    @JIT
    public static long unboxFixnum(IRubyObject val) {
        if (val instanceof RubyFloat) {
            return (long)((RubyFloat)val).getValue();
        } else {
            return ((RubyFixnum)val).getLongValue();
        }
    }

    public static boolean flt(double v1, double v2) {
        return v1 < v2;
    }

    public static boolean fgt(double v1, double v2) {
        return v1 > v2;
    }

    public static boolean feq(double v1, double v2) {
        return v1 == v2;
    }

    public static boolean ilt(long v1, long v2) {
        return v1 < v2;
    }

    public static boolean igt(long v1, long v2) {
        return v1 > v2;
    }

    public static boolean ieq(long v1, long v2) {
        return v1 == v2;
    }

    public static Object unwrapRubyException(Object excObj) {
        // Unrescuable:
        //   IRBreakJump, IRReturnJump, ThreadKill, RubyContinuation, MainExitException, etc.
        //   These cannot be rescued -- only run ensure blocks
        if (excObj instanceof Unrescuable) {
            Helpers.throwException((Throwable)excObj);
        }
        // Ruby exceptions, errors, and other java exceptions.
        // These can be rescued -- run rescue blocks
        return (excObj instanceof RaiseException) ? ((RaiseException) excObj).getException() : excObj;
    }

    private static boolean isJavaExceptionHandled(ThreadContext context, IRubyObject excType, Object excObj, boolean arrayCheck) {
        if (!(excObj instanceof Throwable)) {
            return false;
        }

        final Ruby runtime = context.runtime;
        final Throwable ex = (Throwable) excObj;

        if (excType instanceof RubyArray) {
            RubyArray testTypes = (RubyArray)excType;
            for (int i = 0, n = testTypes.getLength(); i < n; i++) {
                IRubyObject testType = testTypes.eltInternal(i);
                if (IRRuntimeHelpers.isJavaExceptionHandled(context, testType, ex, true)) {
                    IRubyObject exception;
                    if (n == 1) {
                        exception = wrapJavaException(context, testType, ex);
                    } else { // wrap as normal JI object
                        exception = Helpers.wrapJavaException(runtime, ex);
                    }

                    runtime.getGlobalVariables().set("$!", exception);
                    return true;
                }
            }
        }
        else {
            IRubyObject exception = wrapJavaException(context, excType, ex);
            if (Helpers.checkJavaException(exception, ex, excType, context)) {
                runtime.getGlobalVariables().set("$!", exception);
                return true;
            }
        }

        return false;
    }

    private static IRubyObject wrapJavaException(final ThreadContext context, final IRubyObject excType, final Throwable throwable) {
        final Ruby runtime = context.runtime;
        if (excType == runtime.getNativeException()) {
            return wrapWithNativeException(context, throwable, runtime);
        }
        return Helpers.wrapJavaException(runtime, throwable); // wrap as normal JI object
    }

    @SuppressWarnings("deprecation")
    private static IRubyObject wrapWithNativeException(ThreadContext context, Throwable throwable, Ruby runtime) {
        // wrap Throwable in a NativeException object
        org.jruby.NativeException exception = new org.jruby.NativeException(runtime, runtime.getNativeException(), throwable);
        exception.prepareIntegratedBacktrace(context, throwable.getStackTrace());
        return exception;
    }

    private static boolean isRubyExceptionHandled(ThreadContext context, IRubyObject excType, Object excObj) {
        if (excType instanceof RubyArray) {
            RubyArray testTypes = (RubyArray)excType;
            for (int i = 0, n = testTypes.getLength(); i < n; i++) {
                IRubyObject testType = testTypes.eltInternal(i);
                if (IRRuntimeHelpers.isRubyExceptionHandled(context, testType, excObj)) {
                    context.runtime.getGlobalVariables().set("$!", (IRubyObject)excObj);
                    return true;
                }
            }
        } else if (excObj instanceof IRubyObject) {
            // SSS FIXME: Should this check be "runtime.getModule().isInstance(excType)"??
            if (!(excType instanceof RubyModule)) {
                throw context.runtime.newTypeError("class or module required for rescue clause. Found: " + excType);
            }

            if (excType.callMethod(context, "===", (IRubyObject)excObj).isTrue()) {
                context.runtime.getGlobalVariables().set("$!", (IRubyObject)excObj);
                return true;
            }
        }
        return false;
    }

    public static IRubyObject isExceptionHandled(ThreadContext context, IRubyObject excType, Object excObj) {
        // SSS FIXME: JIT should do an explicit unwrap in code just like in interpreter mode.
        // This is called once for each RescueEQQ instr and unwrapping each time is unnecessary.
        // This is not a performance issue, but more a question of where this belongs.
        // It seems more logical to (a) recv-exc (b) unwrap-exc (c) do all the rescue-eqq checks.
        //
        // Unwrap Ruby exceptions
        excObj = unwrapRubyException(excObj);

        boolean ret = IRRuntimeHelpers.isRubyExceptionHandled(context, excType, excObj)
            || IRRuntimeHelpers.isJavaExceptionHandled(context, excType, excObj, false);

        return RubyBoolean.newBoolean(context, ret);
    }

    public static IRubyObject isEQQ(ThreadContext context, IRubyObject receiver, IRubyObject value, CallSite callSite, boolean splattedValue) {
        boolean isUndefValue = value == UndefinedValue.UNDEFINED;

        if (splattedValue && receiver instanceof RubyArray) {       // multiple value when
            RubyArray testVals = (RubyArray) receiver;
            for (int i = 0, n = testVals.getLength(); i < n; i++) {
                IRubyObject v = testVals.eltInternal(i);
                IRubyObject eqqVal = isUndefValue ? v : callSite.call(context, v, v, value);
                if (eqqVal.isTrue()) return eqqVal;
            }
            return context.fals;
        }

        if (isUndefValue) return receiver;                           // no arg case single when

        return callSite.call(context, receiver, receiver, value);    // single when
    }

    public static IRubyObject newProc(Ruby runtime, Block block) {
        return (block == Block.NULL_BLOCK) ? runtime.getNil() : runtime.newProc(Block.Type.PROC, block);
    }

    @JIT
    public static IRubyObject newProc(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        return (block == Block.NULL_BLOCK) ? runtime.getNil() : runtime.newProc(Block.Type.PROC, block);
    }

    @JIT
    public static RubyProc newLambdaProc(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        return runtime.newProc(LAMBDA, block);
    }

    @JIT
    public static IRubyObject yield(ThreadContext context, Block b, IRubyObject yieldVal, boolean unwrapArray) {
        return (unwrapArray && (yieldVal instanceof RubyArray)) ? b.yieldArray(context, yieldVal, null) : b.yield(context, yieldVal);
    }

    @JIT
    public static IRubyObject yieldSpecific(ThreadContext context, Block b) {
        return b.yieldSpecific(context);
    }

    @JIT
    public static IRubyObject yieldValues(ThreadContext context, Block blk, IRubyObject[] args) {
        return blk.yieldValues(context, args);
    }

    // .call for PROC
    public static IRubyObject[] convertValueIntoArgArray(ThreadContext context, IRubyObject value, org.jruby.runtime.Signature signature) {
        switch (signature.arityValue()) {
            case -1:
                return signature.opt() > 1 && value instanceof RubyArray ?
                        ((RubyArray) value).toJavaArray() :
                        new IRubyObject[] { value };
            case  0:
            case  1:
                return new IRubyObject[] { value };
        }

        return IRBlockBody.toAry(context, value);
    }

    // NORMAL yield paths passed through yieldSpecific and call (for when block is passed through -- some internal weirdness on our part).
    public static IRubyObject[] convertValueIntoArgArray(ThreadContext context, RubyArray array, org.jruby.runtime.Signature signature) {
        switch (signature.arityValue()) {
            case -1:
                return array.toJavaArray();
            case 0:
            case 1:
                return new IRubyObject[] { array };
        }

        return singleBlockArgToArray(Helpers.aryToAry(context, array.size() == 1 ? array.eltInternal(0) : array));
    }

    @JIT
    public static Block getBlockFromObject(ThreadContext context, Object value) {
        Block block;
        if (value instanceof Block) {
            block = (Block) value;
        } else if (value instanceof RubyProc) {
            block = ((RubyProc) value).getBlock();
        } else if (value instanceof RubyMethod) {
            block = ((RubyProc) ((RubyMethod) value).to_proc(context)).getBlock();
        } else if (value instanceof RubySymbol) {
            block = ((RubyProc) ((RubySymbol) value).to_proc(context)).getBlock();
        } else if ((value instanceof IRubyObject) && ((IRubyObject)value).isNil()) {
            block = Block.NULL_BLOCK;
        } else if (value instanceof IRubyObject) {
            block = ((RubyProc) TypeConverter.convertToType((IRubyObject) value, context.runtime.getProc(), "to_proc", true)).getBlock();
        } else {
            throw new RuntimeException("Unhandled case in CallInstr:prepareBlock.  Got block arg: " + value);
        }
        return block;
    }

    @JIT
    public static Block getRefinedBlockFromObject(ThreadContext context, StaticScope scope, Object value) {
        Block block;
        if (value instanceof Block) {
            block = (Block) value;
        } else if (value instanceof RubyProc) {
            block = ((RubyProc) value).getBlock();
        } else if (value instanceof RubyMethod) {
            block = ((RubyProc) ((RubyMethod) value).to_proc(context)).getBlock();
        } else if (value instanceof RubySymbol) {
            block = ((RubyProc) ((RubySymbol) value).toRefinedProc(context, scope)).getBlock();
        } else if ((value instanceof IRubyObject) && ((IRubyObject)value).isNil()) {
            block = Block.NULL_BLOCK;
        } else if (value instanceof IRubyObject) {
            block = ((RubyProc) TypeConverter.convertToType((IRubyObject) value, context.runtime.getProc(), "to_proc", true)).getBlock();
        } else {
            throw new RuntimeException("Unhandled case in CallInstr:prepareBlock.  Got block arg: " + value);
        }
        return block;
    }

    public static void checkArity(ThreadContext context, StaticScope scope, Object[] args, int required, int opt, boolean rest,
                                  boolean receivesKwargs, int restKey, Block block) {
        int argsLength = args.length;
        RubyHash keywordArgs = extractKwargsHash(context, args, required, receivesKwargs);

        if (restKey == -1 && keywordArgs != null) checkForExtraUnwantedKeywordArgs(context, scope, keywordArgs);

        // keyword arguments value is not used for arity checking.
        if (keywordArgs != null) argsLength -= 1;

        if ((block == null || block.type.checkArity) && (argsLength < required || (!rest && argsLength > (required + opt)))) {
            Arity.raiseArgumentError(context.runtime, argsLength, required, rest ? UNLIMITED_ARGUMENTS : (required + opt));
        }
    }

    @JIT
    public static IRubyObject[] frobnicateKwargsArgument(ThreadContext context, IRubyObject[] args, int requiredArgsCount) {
        // FIXME: JIT on block circular args test in spec:compiler is passing in a null value for args.  It does not do this for methods so a bandaid for now.
        if (args == null) return args;

        int length = args.length;

        // No kwarg because required args slurp them up.
        if (length <= requiredArgsCount) return args;

        final IRubyObject maybeKwargs = toHash(context, args[length - 1]);

        if (maybeKwargs != null) {
            if (maybeKwargs.isNil()) { // nil on to_hash is supposed to keep itself as real value so we need to make kwargs hash
                return ArraySupport.newCopy(args, RubyHash.newSmallHash(context.runtime));
            }

            RubyHash kwargs = (RubyHash) maybeKwargs;

            if (kwargs.allSymbols()) {
                args[length - 1] = kwargs.dupFast(context);
            } else {
                args = homogenizeKwargs(context, args, kwargs);
            }
        }

        return args;
    }

    private static IRubyObject[] homogenizeKwargs(ThreadContext context, IRubyObject[] args, RubyHash kwargs) {
        DivvyKeywordsVisitor visitor = new DivvyKeywordsVisitor();

        // We know toHash makes null, nil, or Hash
        kwargs.visitAll(context, visitor, null);

        if (visitor.syms == null) {
            // no symbols, use empty kwargs hash
            visitor.syms = RubyHash.newSmallHash(context.runtime);
        }

        if (visitor.others != null) { // rest args exists too expand args
            args = ArraySupport.newCopy(args, args.length + 1);
            args[args.length - 2] = visitor.others; // opt args
        }
        args[args.length - 1] = visitor.syms; // kwargs hash

        return args;
    }

    /**
     * If the ir.print property is enabled and we are not booting, or the ir.print.all property is enabled and we are
     * booting, return true to indicate IR should be printed.
     *
     * @param runtime the current runtime
     * @return whether to print IR
     */
    public static boolean shouldPrintIR(final Ruby runtime) {
        boolean booting = runtime.isBooting();
        boolean print = Options.IR_PRINT.load();
        boolean printAll = Options.IR_PRINT_ALL.load();

        return (!booting && print) || (booting && printAll);
    }

    /**
     * Update coverage data for the given file and zero-based line number.
     *
     * @param context
     * @param filename
     * @param line
     */
    public static void updateCoverage(ThreadContext context, String filename, int line) {
        CoverageData data = context.runtime.getCoverageData();

        if (data.isCoverageEnabled()) {
            data.coverLine(filename, line);
        }
    }

    public static IRubyObject isHashEmpty(ThreadContext context, IRubyObject hashArg) {
        return hashArg instanceof RubyHash && ((RubyHash) hashArg).size() == 0 ?
                context.tru : context.fals;
    }

    private static class DivvyKeywordsVisitor extends RubyHash.VisitorWithState {
        RubyHash syms;
        RubyHash others;

        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Object unused) {
            if (key instanceof RubySymbol) {
                if (syms == null) syms = RubyHash.newSmallHash(context.runtime);
                syms.fastASetSmall(key, value);
            } else {
                if (others == null) others = RubyHash.newSmallHash(context.runtime);
                others.fastASetSmall(key, value);
            }
        }
    };

    private static IRubyObject toHash(ThreadContext context, IRubyObject lastArg) {
        if (lastArg instanceof RubyHash) return (RubyHash) lastArg;
        if (lastArg.respondsTo("to_hash")) {
            lastArg = lastArg.callMethod(context, "to_hash");
            if (lastArg == context.nil) return lastArg;
            TypeConverter.checkType(context, lastArg, context.runtime.getHash());
            return (RubyHash) lastArg;
        }
        return null;
    }

    public static RubyHash extractKwargsHash(ThreadContext context, Object[] args, int requiredArgsCount, boolean receivesKwargs) {
        if (!receivesKwargs) return null;
        if (args.length <= requiredArgsCount) return null; // No kwarg because required args slurp them up.

        Object lastArg = args[args.length - 1];

        if (lastArg instanceof IRubyObject) {
            IRubyObject returnValue = toHash(context, (IRubyObject) lastArg);
            if (returnValue instanceof RubyHash) return (RubyHash) returnValue;
        }

        return null;
    }

    @Deprecated // not used
    public static RubyHash extractKwargsHash(Object[] args, int requiredArgsCount, boolean receivesKwargs) {
        if (!receivesKwargs) return null;
        if (args.length <= requiredArgsCount) return null; // No kwarg because required args slurp them up.

        Object lastArg = args[args.length - 1];

        if (lastArg instanceof IRubyObject) {
            IRubyObject returnValue = toHash(((IRubyObject) lastArg).getRuntime().getCurrentContext(), (IRubyObject) lastArg);
            if (returnValue instanceof RubyHash) return (RubyHash) returnValue;
        }

        return null;
    }

    public static void checkForExtraUnwantedKeywordArgs(ThreadContext context, final StaticScope scope, RubyHash keywordArgs) {
        // we do an inexpensive non-gathering scan first to see if there's a bad keyword
        try {
            keywordArgs.visitAll(context, CheckUnwantedKeywordsVisitor, scope);
        } catch (InvalidKeyException ike) {
            // there's a bad keyword; perform more expensive scan to gather all bad names
            GatherUnwantedKeywordsVisitor visitor = new GatherUnwantedKeywordsVisitor();
            keywordArgs.visitAll(context, visitor, scope);
            visitor.raiseIfError(context);
        }
    }

    @JIT
    public static DynamicScope prepareScriptScope(ThreadContext context, StaticScope scope) {
        IRScope irScope = scope.getIRScope();

        if (irScope != null && irScope.isScriptScope()) {
            DynamicScope tlbScope = ((IRScriptBody) irScope).getScriptDynamicScope();
            if (tlbScope != null) {
                context.preScopedBody(tlbScope);
                tlbScope.growIfNeeded();
                return tlbScope;
            }
        }

        DynamicScope dynScope = DynamicScope.newDynamicScope(scope);
        context.pushScope(dynScope);

        return dynScope;
    }

    private static class InvalidKeyException extends RuntimeException {}
    private static final InvalidKeyException INVALID_KEY_EXCEPTION = new InvalidKeyException();
    private static final RubyHash.VisitorWithState<StaticScope> CheckUnwantedKeywordsVisitor = new RubyHash.VisitorWithState<StaticScope>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, StaticScope scope) {
            String javaName = key.asJavaString();
            if (!scope.keywordExists(javaName)) {
                throw INVALID_KEY_EXCEPTION;
            }
        }
    };

    private static class GatherUnwantedKeywordsVisitor extends RubyHash.VisitorWithState<StaticScope> {
        ArrayList invalidKwargs;
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, StaticScope scope) {
            String javaName = key.asJavaString();
            if (!scope.keywordExists(javaName)) {
                if (invalidKwargs == null) invalidKwargs = new ArrayList();
                invalidKwargs.add(javaName);
            }
        }

        public void raiseIfError(ThreadContext context) {
            if (invalidKwargs != null) {
                String invalidKwargs = this.invalidKwargs.toString();
                throw context.runtime.newArgumentError(
                        (this.invalidKwargs.size() == 1 ? "unknown keyword: " : "unknown keywords: ")
                                + invalidKwargs.substring(1, invalidKwargs.length() - 1));
            }
        }
    }

    public static IRubyObject match3(ThreadContext context, RubyRegexp regexp, IRubyObject argValue) {
        if (argValue instanceof RubyString) {
            return regexp.op_match(context, argValue);
        } else {
            return argValue.callMethod(context, "=~", regexp);
        }
    }

    public static IRubyObject extractOptionalArgument(RubyArray rubyArray, int minArgsLength, int index) {
        int n = rubyArray.getLength();
        return minArgsLength < n ? rubyArray.entry(index) : UndefinedValue.UNDEFINED;
    }

    @JIT @Interp
    public static IRubyObject isDefinedBackref(ThreadContext context, IRubyObject definedMessage) {
        return RubyMatchData.class.isInstance(context.getBackRef()) ?
                definedMessage : context.nil;
    }

    @JIT @Interp
    public static IRubyObject isDefinedGlobal(ThreadContext context, String name, IRubyObject definedMessage) {
        return context.runtime.getGlobalVariables().isDefined(name) ?
                definedMessage : context.nil;
    }

    // FIXME: This checks for match data differently than isDefinedBackref.  Seems like they should use same mechanism?
    @JIT @Interp
    public static IRubyObject isDefinedNthRef(ThreadContext context, int matchNumber, IRubyObject definedMessage) {
        IRubyObject backref = context.getBackRef();

        if (backref instanceof RubyMatchData) {
            if (!((RubyMatchData) backref).group(matchNumber).isNil()) {
                return definedMessage;
            }
        }

        return context.nil;
    }

    @JIT @Interp
    public static IRubyObject isDefinedClassVar(ThreadContext context, RubyModule receiver, String name, IRubyObject definedMessage) {
        boolean defined = receiver.isClassVarDefined(name);

        if (!defined && receiver.isSingleton()) { // Look for class var in singleton if it is one.
            IRubyObject attached = ((MetaClass) receiver).getAttached();

            if (attached instanceof RubyModule) defined = ((RubyModule) attached).isClassVarDefined(name);
        }

        return defined ? definedMessage : context.nil;
    }

    @JIT @Interp
    public static IRubyObject isDefinedInstanceVar(ThreadContext context, IRubyObject receiver, String name, IRubyObject definedMessage) {
        return receiver.getInstanceVariables().hasInstanceVariable(name) ?
                definedMessage : context.nil;
    }

    @JIT @Interp
    public static IRubyObject isDefinedCall(ThreadContext context, IRubyObject self, IRubyObject receiver, String name, IRubyObject definedMessage) {
        IRubyObject boundValue = Helpers.getDefinedCall(context, self, receiver, name, definedMessage);

        return boundValue == null ? context.nil : boundValue;
    }

    @JIT @Interp
    public static IRubyObject isDefinedConstantOrMethod(ThreadContext context, IRubyObject receiver, RubyString name, IRubyObject definedConstantMessage, IRubyObject definedMethodMessage) {
        IRubyObject definedType = Helpers.getDefinedConstantOrBoundMethod(receiver, name.intern().idString(), definedConstantMessage, definedMethodMessage);

        return definedType == null ? context.nil : definedType;
    }

    @JIT @Interp
    public static IRubyObject isDefinedMethod(ThreadContext context, IRubyObject receiver, String name, boolean checkIfPublic, IRubyObject definedMessage) {
        DynamicMethod method = receiver.getMetaClass().searchMethod(name);

        boolean defined = !method.isUndefined();

        if (defined) {
            // If we find the method we optionally check if it is public before returning "method".
            defined = !checkIfPublic || method.getVisibility() == Visibility.PUBLIC;
        } else {
            // If we did not find the method, check respond_to_missing?
            defined = receiver.respondsToMissing(name, checkIfPublic);
        }

        return defined ? definedMessage : context.nil;
    }

    @Interp
    public static IRubyObject isDefinedSuper(ThreadContext context, IRubyObject receiver, IRubyObject definedMessage) {
        return isDefinedSuper(context, receiver, context.getFrameName(), context.getFrameKlazz(), definedMessage);
    }

    @JIT
    public static IRubyObject isDefinedSuper(ThreadContext context, IRubyObject receiver, String frameName, RubyModule frameClass, IRubyObject definedMessage) {
        boolean defined = frameName != null && frameClass != null &&
                frameClass.getSuperClass().isMethodBound(frameName, false);

        return defined ? definedMessage : context.nil;
    }

    public static IRubyObject nthMatch(ThreadContext context, int matchNumber) {
        return RubyRegexp.nth_match(matchNumber, context.getBackRef());
    }

    public static void defineAlias(ThreadContext context, IRubyObject self, DynamicScope currDynScope, IRubyObject newName, IRubyObject oldName) {
        if (self == null || self instanceof RubyFixnum || self instanceof RubySymbol) {
            throw context.runtime.newTypeError("no class to make alias");
        }

        findInstanceMethodContainer(context, currDynScope, self).alias_method(context, newName, oldName);
    }

    /**
     * Find the base class or "cbase" used for various class-level operations.
     *
     * This should be equivalent to "cbase" in CRuby, as retrieved by vm_get_cbase.
     *
     * See {@link org.jruby.ir.instructions.GetClassVarContainerModuleInstr} and {@link org.jruby.RubyKernel#autoload(ThreadContext, IRubyObject, IRubyObject, IRubyObject)}
     * for example usage.
     *
     * @param context the current context
     * @param self the current self object
     * @return the instance method definition target, or the "cbase" for other purposes
     */
    public static RubyModule getCurrentClassBase(ThreadContext context, IRubyObject self) {
        return getModuleFromScope(context, context.getCurrentStaticScope(), self);
    }

    public static RubyModule getModuleFromScope(ThreadContext context, StaticScope scope, IRubyObject arg) {
        Ruby runtime = context.runtime;
        RubyModule rubyClass = scope.getModule();

        // SSS FIXME: Copied from ASTInterpreter.getClassVariableBase and adapted
        while (scope != null && (rubyClass.isSingleton() || rubyClass == runtime.getDummy())) {
            scope = scope.getPreviousCRefScope();
            rubyClass = scope.getModule();
            if (scope.getPreviousCRefScope() == null) {
                runtime.getWarnings().warn(IRubyWarnings.ID.CVAR_FROM_TOPLEVEL_SINGLETON_METHOD, "class variable access from toplevel singleton method");
            }
        }

        if ((scope == null) && (arg != null)) {
            // We ran out of scopes to check -- look in arg's metaclass
            rubyClass = arg.getMetaClass();
        }

        if (rubyClass == null) {
            throw context.runtime.newTypeError("no class/module to define class variable");
        }

        return rubyClass;
    }

    @JIT @Interp
    public static IRubyObject mergeKeywordArguments(ThreadContext context, IRubyObject restKwarg, IRubyObject explicitKwarg) {
        RubyHash hash = (RubyHash) TypeConverter.checkHashType(context.runtime, restKwarg).dup();

        hash.modify();
        final RubyHash otherHash = explicitKwarg.convertToHash();

        if (otherHash.empty_p(context).isTrue()) return hash;

        otherHash.visitAll(context, new KwargMergeVisitor(hash), Block.NULL_BLOCK);

        return hash;
    }

    private static class KwargMergeVisitor extends RubyHash.VisitorWithState<Block> {
        final RubyHash target;

        KwargMergeVisitor(RubyHash target) {
            this.target = target;
        }

        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Block block) {
            // All kwargs keys must be symbols.
            TypeConverter.checkType(context, key, context.runtime.getSymbol());

            target.op_aset(context, key, value);
        }
    }

    public static RubyModule findInstanceMethodContainer(ThreadContext context, DynamicScope currDynScope, IRubyObject self) {
        boolean inBindingEval = currDynScope.inBindingEval();

        // Top-level-scripts are special but, not if binding-evals are in force!
        if (!inBindingEval && self == context.runtime.getTopSelf()) return self.getType();

        for (DynamicScope ds = currDynScope; ds != null; ) {
            IRScopeType scopeType = ds.getStaticScope().getScopeType();
            switch (ds.getEvalType()) {
                // The most common use case in the MODULE_EVAL case is:
                //   a method is defined inside a closure
                //   that is nested inside a module_eval.
                //   here self = the module
                // In the rare case where it is not (looks like it can
                // happen in some testing frameworks), we have to add
                // the method to self itself => its metaclass.
                //
                // SSS FIXME: Looks like this rare case happens when
                // the closure is used in a "define_method &block" scenario
                // => in reality the scope is not a closure but an
                // instance_method. So, when we fix define_method implementation
                // to actually convert blocks to real instance_method scopes,
                // we will not have this edge case since the code will then
                // be covered by the (scopeType == IRScopeType.INSTANCE_METHOD)
                // scenario below. Whenever we get to fixing define_method
                // implementation, we should rip out this code here.
                //
                // Verify that this test runs:
                // -------------
                //   require "minitest/autorun"
                //
                //   describe "A" do
                //     it "should do something" do
                //       def foo
                //       end
                //     end
                //   end
                // -------------
                case MODULE_EVAL  : return self instanceof RubyModule ? (RubyModule) self : self.getMetaClass();
                case INSTANCE_EVAL: return self.getSingletonClass();
                case BINDING_EVAL : ds = ds.getParentScope(); break;
                case NONE:
                    if (scopeType == null || scopeType.isClosureType()) {
                        // Walk up the dynscope hierarchy
                        ds = ds.getParentScope();
                    } else if (inBindingEval) {
                        // Binding evals are special!
                        return ds.getStaticScope().getModule();
                    } else {
                        switch (scopeType) {
                            case CLASS_METHOD:
                            case MODULE_BODY:
                            case CLASS_BODY:
                            case METACLASS_BODY:
                                // This is a similar scenario as the FIXME above that was added
                                // in b65a5842ecf56ca32edc2a17800968f021b6a064. At that time,
                                // I was wondering if it would affect this site here and looks
                                // like it does.
                                return self instanceof RubyModule ? (RubyModule) self : self.getMetaClass();

                            case INSTANCE_METHOD:
                            case SCRIPT_BODY:
                                return self.getMetaClass();

                            default:
                                throw new RuntimeException("Should not get here! scopeType is " + scopeType);
                        }
                    }
                    break;
            }
        }

        throw new RuntimeException("Should not get here!");
    }

    public static RubyBoolean isBlockGiven(ThreadContext context, Object blk) {
        if (blk instanceof RubyProc) blk = ((RubyProc) blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        return RubyBoolean.newBoolean(context,  ((Block) blk).isGiven() );
    }

    public static IRubyObject receiveRestArg(ThreadContext context, Object[] args, int required, int argIndex, boolean acceptsKeywordArguments) {
        RubyHash keywordArguments = extractKwargsHash(context, args, required, acceptsKeywordArguments);
        return constructRestArg(context, args, keywordArguments, required, argIndex);
    }

    public static IRubyObject receiveRestArg(ThreadContext context, IRubyObject[] args, int required, int argIndex, boolean acceptsKeywordArguments) {
        RubyHash keywordArguments = extractKwargsHash(context, args, required, acceptsKeywordArguments);
        return constructRestArg(context, args, keywordArguments, required, argIndex);
    }

    public static IRubyObject constructRestArg(ThreadContext context, Object[] args, RubyHash keywordArguments, int required, int argIndex) {
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;
        int remainingArguments = argsLength - required;

        if (remainingArguments <= 0) return context.runtime.newEmptyArray();

        return RubyArray.newArrayMayCopy(context.runtime, (IRubyObject[]) args, argIndex, remainingArguments);
    }

    private static IRubyObject constructRestArg(ThreadContext context, IRubyObject[] args, RubyHash keywordArguments, int required, int argIndex) {
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;
        if ( required == 0 && argsLength == args.length ) {
            return RubyArray.newArray(context.runtime, args);
        }
        int remainingArguments = argsLength - required;

        if (remainingArguments <= 0) return context.runtime.newEmptyArray();

        return RubyArray.newArrayMayCopy(context.runtime, args, argIndex, remainingArguments);
    }

    @JIT @Interp
    public static IRubyObject receivePostReqdArg(ThreadContext context, IRubyObject[] args, int pre,
                                                 int opt, boolean rest, int post,
                                                 int argIndex, boolean acceptsKeywordArgument) {
        int required = pre + post;
        // FIXME: Once we extract kwargs from rest of args processing we can delete this extract and n calc.
        boolean kwargs = extractKwargsHash(context, args, required, acceptsKeywordArgument) != null;
        int n = kwargs ? args.length - 1 : args.length;
        int remaining = n - pre;       // we know we have received all pre args by post receives.

        if (remaining < post) {        // less args available than post args need
            if (pre + argIndex >= n) { // argument is past end of arg list
                return context.nil;
            } else {
                return args[pre + argIndex];
            }
        }

        // At this point we know we have enough arguments left for post without worrying about AIOOBE.

        if (rest) {                      // we can read from back since we will take all args we can get.
            return args[n - post + argIndex];
        } else if (n > required + opt) { // we filled all opt so we can read from front (and avoid excess args case from proc).
            return args[pre + opt + argIndex];
        } else {                         // partial opts filled in too few args so we can read from end.
            return args[n - post + argIndex];
        }
    }

    @JIT
    public static IRubyObject receiveOptArg(ThreadContext context, IRubyObject[] args, int requiredArgs, int preArgs, int argIndex, boolean acceptsKeywordArgument) {
        int optArgIndex = argIndex;  // which opt arg we are processing? (first one has index 0, second 1, ...).
        RubyHash keywordArguments = extractKwargsHash(context, args, requiredArgs, acceptsKeywordArgument);
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;

        if (requiredArgs + optArgIndex >= argsLength) return UndefinedValue.UNDEFINED; // No more args left

        return args[preArgs + optArgIndex];
    }

    @Deprecated // not used
    public static IRubyObject receiveOptArg(IRubyObject[] args, int requiredArgs, int preArgs, int argIndex, boolean acceptsKeywordArgument) {
        int optArgIndex = argIndex;  // which opt arg we are processing? (first one has index 0, second 1, ...).
        RubyHash keywordArguments = extractKwargsHash(args, requiredArgs, acceptsKeywordArgument);
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;

        if (requiredArgs + optArgIndex >= argsLength) return UndefinedValue.UNDEFINED; // No more args left

        return args[preArgs + optArgIndex];
    }

    public static IRubyObject getPreArgSafe(ThreadContext context, IRubyObject[] args, int argIndex) {
        IRubyObject result;
        result = argIndex < args.length ? args[argIndex] : context.nil; // SSS FIXME: This check is only required for closures, not methods
        return result;
    }

    public static IRubyObject receiveKeywordArg(ThreadContext context, IRubyObject[] args, int required, String id, boolean acceptsKeywordArgument) {
        RubyHash keywordArguments = extractKwargsHash(context, args, required, acceptsKeywordArgument);

        if (keywordArguments == null) return UndefinedValue.UNDEFINED;

        RubySymbol keywordName = context.runtime.newSymbol(id);

        if (keywordArguments.fastARef(keywordName) == null) return UndefinedValue.UNDEFINED;

        // SSS FIXME: Can we use an internal delete here?
        // Enebo FIXME: Delete seems wrong if we are doing this for duplication purposes.
        return keywordArguments.delete(context, keywordName, Block.NULL_BLOCK);
    }

    public static IRubyObject receiveKeywordArg(ThreadContext context, IRubyObject[] args, int required, RubySymbol key, boolean acceptsKeywordArgument) {
        RubyHash keywordArguments = extractKwargsHash(context, args, required, acceptsKeywordArgument);

        if (keywordArguments == null) return UndefinedValue.UNDEFINED;

        if (keywordArguments.fastARef(key) == null) return UndefinedValue.UNDEFINED;

        // SSS FIXME: Can we use an internal delete here?
        // Enebo FIXME: Delete seems wrong if we are doing this for duplication purposes.
        return keywordArguments.delete(context, key, Block.NULL_BLOCK);
    }

    public static IRubyObject receiveKeywordRestArg(ThreadContext context, IRubyObject[] args, int required, boolean keywordArgumentSupplied) {
        RubyHash keywordArguments = extractKwargsHash(context, args, required, keywordArgumentSupplied);

        return keywordArguments == null ? RubyHash.newSmallHash(context.runtime) : keywordArguments;
    }

    public static IRubyObject setCapturedVar(ThreadContext context, IRubyObject matchRes, String id) {
        if (matchRes.isNil()) return context.nil;

        IRubyObject backref = context.getBackRef();

        return RubyRegexp.nth_match(((RubyMatchData) backref).getNameToBackrefNumber(id), backref);
    }

    @JIT // for JVM6
    public static IRubyObject instanceSuperSplatArgs(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block, boolean[] splatMap) {
        return instanceSuper(context, self, methodName, definingModule, splatArguments(args, splatMap), block);
    }

    @JIT // for JVM6
    public static IRubyObject instanceSuperIterSplatArgs(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block, boolean[] splatMap) {
        return instanceSuperIter(context, self, methodName, definingModule, splatArguments(args, splatMap), block);
    }

    @Interp
    public static IRubyObject instanceSuper(ThreadContext context, IRubyObject self, String id, RubyModule definingModule, IRubyObject[] args, Block block) {
        CacheEntry entry = getSuperMethodEntry(id, definingModule);
        DynamicMethod method = entry.method;

        if (method instanceof InstanceMethodInvoker && self instanceof JavaProxy) {
            return javaProxySuper(
                    context,
                    (JavaProxy) self,
                    id,
                    (RubyClass) definingModule,
                    args,
                    (InstanceMethodInvoker) method);
        }

        if (method.isUndefined()) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), id, CallType.SUPER, args, block);
        }

        return method.call(context, self, entry.sourceModule, id, args, block);
    }

    /**
     * Perform a super invocation against a Java proxy, using proxy logic to locate and invoke the appropriate shim
     * method.
     *
     * This duplicates some logic from InstanceMethodInvoker.call and JavaMethod.tryProxyInvocation in order to properly
     * retrieve the superclass method from the caller's point of view. See GH-6718.
     *
     * @param context the current context
     * @param self the proxy wrapper
     * @param id the method name
     * @param definingModule the module in which the calling method is defined
     * @param args arguments to the call
     * @param superMethod the invoker for the super method found using using Ruby super logic
     * @return the result of invoking the super method via a shim method
     */
    private static IRubyObject javaProxySuper(ThreadContext context, JavaProxy self, String id, RubyClass definingModule, IRubyObject[] args, InstanceMethodInvoker superMethod) {
        Object javaInvokee = self.getObject();

        JavaMethod javaMethod = (JavaMethod) superMethod.findCallable(self, id, args, args.length);

        // self is a Java subclass, need to do a bit more logic to dispatch the right method
        JavaProxyClass jpc = JavaProxyClass.getProxyClass(context.runtime, definingModule);
        JavaProxyMethod jpm;
        Object[] newArgs = RubyToJavaInvoker.convertArguments(javaMethod, args);
        if ((jpm = jpc.getMethod(id, javaMethod.getParameterTypes())) != null && jpm.hasSuperImplementation()) {
            return javaMethod.invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, newArgs);
        } else {
            return javaMethod.invokeDirectWithExceptionHandling(context, javaMethod.getValue(), javaInvokee, newArgs);
        }
    }

    @Interp
    public static IRubyObject instanceSuperIter(ThreadContext context, IRubyObject self, String id, RubyModule definingModule, IRubyObject[] args, Block block) {
        try {
            return instanceSuper(context, self, id, definingModule, args, block);
        } finally {
            block.escape();
        }
    }

    private static CacheEntry getSuperMethodEntry(String id, RubyModule definingModule) {
        RubyClass superClass = definingModule.getMethodLocation().getSuperClass();
        return superClass != null ? superClass.searchWithCache(id) : CacheEntry.NULL_CACHE;
    }

    @JIT // for JVM6
    public static IRubyObject classSuperSplatArgs(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block, boolean[] splatMap) {
        return classSuper(context, self, methodName, definingModule, splatArguments(args, splatMap), block);
    }

    @JIT // for JVM6
    public static IRubyObject classSuperIterSplatArgs(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block, boolean[] splatMap) {
        return classSuperIter(context, self, methodName, definingModule, splatArguments(args, splatMap), block);
    }

    @Interp
    public static IRubyObject classSuper(ThreadContext context, IRubyObject self, String id, RubyModule definingModule, IRubyObject[] args, Block block) {
        CacheEntry entry = getSuperMethodEntry(id, definingModule.getMetaClass());
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), id, CallType.SUPER, args, block);
        }

        return method.call(context, self, entry.sourceModule, id, args, block);
    }

    @Interp
    public static IRubyObject classSuperIter(ThreadContext context, IRubyObject self, String id, RubyModule definingModule, IRubyObject[] args, Block block) {
        try {
            return classSuper(context, self, id, definingModule, args, block);
        } finally {
            block.escape();
        }
    }

    @JIT
    public static IRubyObject unresolvedSuperSplatArgs(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, boolean[] splatMap) {
        return unresolvedSuper(context, self, splatArguments(args, splatMap), block);
    }

    @JIT
    public static IRubyObject unresolvedSuperIterSplatArgs(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, boolean[] splatMap) {
        return unresolvedSuperIter(context, self, splatArguments(args, splatMap), block);
    }

    @Interp
    public static IRubyObject unresolvedSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        // We have to rely on the frame stack to find the implementation class
        RubyModule klazz = context.getFrameKlazz();
        String methodName = context.getFrameName();

        Helpers.checkSuperDisabledOrOutOfMethod(context, klazz, methodName);

        RubyClass superClass = searchNormalSuperclass(klazz);
        CacheEntry entry = superClass != null ? superClass.searchWithCache(methodName) : CacheEntry.NULL_CACHE;

        IRubyObject rVal;
        if (entry.method.isUndefined()) {
            rVal = Helpers.callMethodMissing(context, self, entry.method.getVisibility(), methodName, CallType.SUPER, args, block);
        } else {
            rVal = entry.method.call(context, self, entry.sourceModule, methodName, args, block);
        }

        return rVal;
    }

    @Interp
    public static IRubyObject unresolvedSuperIter(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        try {
            return unresolvedSuper(context, self, args, block);
        } finally {
            block.escape();
        }
    }

    // MRI: vm_search_normal_superclass
    private static RubyClass searchNormalSuperclass(RubyModule klazz) {
        // Unwrap refinements, since super should always dispatch back to the refined class
        if (klazz.isIncluded()
                && klazz.getOrigin().isRefinement()) {
            klazz = klazz.getOrigin();
        }
        klazz = klazz.getMethodLocation();
        return klazz.getSuperClass();
    }

    public static IRubyObject zSuperSplatArgs(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, boolean[] splatMap) {
        if (block == null || !block.isGiven()) block = context.getFrameBlock();
        return unresolvedSuper(context, self, splatArguments(args, splatMap), block);
    }

    public static IRubyObject zSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        if (block == null || !block.isGiven()) block = context.getFrameBlock();
        return unresolvedSuper(context, self, args, block);
    }

    public static IRubyObject[] splatArguments(IRubyObject[] args, boolean[] splatMap) {
        if (splatMap != null && splatMap.length > 0) {
            int count = 0;
            for (int i = 0; i < splatMap.length; i++) {
                // make sure arg is still an array (zsuper can get have any args changed before it is called).
                count += splatMap[i] && args[i] instanceof RubyArray ? ((RubyArray)args[i]).size() : 1;
            }

            IRubyObject[] newArgs = new IRubyObject[count];
            int actualOffset = 0;
            for (int i = 0; i < splatMap.length; i++) {
                if (splatMap[i] && args[i] instanceof RubyArray) {
                    RubyArray ary = (RubyArray) args[i];
                    for (int j = 0; j < ary.size(); j++) {
                        newArgs[actualOffset++] = ary.eltOk(j);
                    }
                } else {
                    newArgs[actualOffset++] = args[i];
                }
            }

            args = newArgs;
        }
        return args;
    }

    public static String encodeSplatmap(boolean[] splatmap) {
        if (splatmap == null) return "";
        StringBuilder builder = new StringBuilder();
        for (boolean b : splatmap) {
            builder.append(b ? '1' : '0');
        }
        return builder.toString();
    }

    public static boolean[] decodeSplatmap(String splatmapString) {
        boolean[] splatMap;
        if (splatmapString.length() > 0) {
            splatMap = new boolean[splatmapString.length()];

            for (int i = 0; i < splatmapString.length(); i++) {
                if (splatmapString.charAt(i) == '1') {
                    splatMap[i] = true;
                }
            }
        } else {
            splatMap = null;
        }
        return splatMap;
    }

    public static boolean[] buildSplatMap(Operand[] args) {
        boolean[] splatMap = null;

        for (int i = 0; i < args.length; i++) {
            Operand operand = args[i];
            if (operand instanceof Splat) {
                if (splatMap == null) splatMap = new boolean[args.length];
                splatMap[i] = true;
            }
        }

        return splatMap;
    }

    public static boolean anyTrue(boolean[] booleans) {
        for (boolean b : booleans) if (b) return true;
        return false;
    }

    public static boolean needsSplatting(boolean[] splatmap) {
        return splatmap != null && splatmap.length > 0 && anyTrue(splatmap);
    }

    public static final Type[] typesFromSignature(Signature signature) {
        Type[] types = new Type[signature.argCount()];
        for (int i = 0; i < signature.argCount(); i++) {
            types[i] = Type.getType(signature.argType(i));
        }
        return types;
    }

    @JIT
    public static RubyString newFrozenStringFromRaw(ThreadContext context, String str, String encoding, int cr, String file, int line) {
        return newFrozenString(context, newByteListFromRaw(context.runtime, str, encoding), cr, file, line);
    }

    @JIT
    public static final ByteList newByteListFromRaw(Ruby runtime, String str, String encoding) {
        return new ByteList(RubyEncoding.encodeISO(str), runtime.getEncodingService().getEncodingFromString(encoding), false);
    }

    @JIT
    public static RubyEncoding retrieveEncoding(ThreadContext context, String name) {
        return context.runtime.getEncodingService().getEncoding(retrieveJCodingsEncoding(context, name));
    }

    @JIT
    public static Encoding retrieveJCodingsEncoding(ThreadContext context, String name) {
        return context.runtime.getEncodingService().findEncodingOrAliasEntry(name.getBytes()).getEncoding();
    }

    @JIT
    public static RubyHash constructHashFromArray(Ruby runtime, IRubyObject[] pairs) {
        int length = pairs.length / 2;
        boolean useSmallHash = length <= 10;

        RubyHash hash = useSmallHash ? RubyHash.newSmallHash(runtime) : RubyHash.newHash(runtime);

        for (int i = 0; i < pairs.length;) {
            if (useSmallHash) {
                hash.fastASetSmall(runtime, pairs[i++], pairs[i++], true);
            } else {
                hash.fastASet(runtime, pairs[i++], pairs[i++], true);
            }

        }
        return hash;
    }

    @JIT
    public static RubyHash dupKwargsHashAndPopulateFromArray(ThreadContext context, RubyHash dupHash, IRubyObject[] pairs) {
        Ruby runtime = context.runtime;
        RubyHash hash = dupHash.dupFast(context);
        for (int i = 0; i < pairs.length;) {
            hash.fastASetCheckString(runtime, pairs[i++], pairs[i++]);
        }
        return hash;
    }

    @JIT
    public static IRubyObject searchConst(ThreadContext context, StaticScope staticScope, String constName, boolean noPrivateConsts) {
        RubyModule object = context.runtime.getObject();
        IRubyObject constant = (staticScope == null) ? object.getConstant(constName) : staticScope.getConstantInner(constName);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);
        }

        // Call const_missing or cache
        if (constant == null) {
            return module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(constName));
        }

        return constant;
    }

    @JIT
    public static IRubyObject inheritedSearchConst(ThreadContext context, IRubyObject cmVal, String constName, boolean noPrivateConsts) {
        RubyModule module;
        if (cmVal instanceof RubyModule) {
            module = (RubyModule) cmVal;
        } else {
            throw context.runtime.newTypeError(cmVal + " is not a type/class");
        }

        IRubyObject constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        return constant;
    }

    @JIT
    public static IRubyObject lexicalSearchConst(ThreadContext context, StaticScope staticScope, String constName) {
        IRubyObject constant = staticScope.getConstantInner(constName);

        if (constant == null) {
            constant = UndefinedValue.UNDEFINED;
        }

        return constant;
    }

    public static IRubyObject setInstanceVariable(IRubyObject self, IRubyObject value, String name) {
        return self.getInstanceVariables().setInstanceVariable(name, value);
    }

    /**
     * Construct a new DynamicMethod to wrap the given IRModuleBody and singletonizable object. Used by interpreter.
     */
    @Interp
    public static DynamicMethod newInterpretedMetaClass(Ruby runtime, IRScope metaClassBody, IRubyObject obj) {
        RubyClass singletonClass = newMetaClassFromIR(runtime, metaClassBody.getStaticScope(), obj, metaClassBody.maybeUsingRefinements());

        return new InterpretedIRMetaClassBody(metaClassBody, singletonClass);
    }

    /**
     * Construct a new DynamicMethod to wrap the given IRModuleBody and singletonizable object. Used by JIT.
     */
    @JIT
    public static DynamicMethod newCompiledMetaClass(ThreadContext context, MethodHandle handle, StaticScope scope, IRubyObject obj, int line, boolean refinements, boolean dynscopeEliminated) {
        RubyClass singletonClass = newMetaClassFromIR(context.runtime, scope, obj, refinements);

        return new CompiledIRNoProtocolMethod(handle, scope, scope.getFile(), line,
                singletonClass, !dynscopeEliminated);
    }

    private static RubyClass newMetaClassFromIR(Ruby runtime, StaticScope scope, IRubyObject obj, boolean refinements) {
        RubyClass singletonClass = Helpers.getSingletonClass(runtime, obj);

        scope.setModule(singletonClass);

        if (refinements) scope.captureParentRefinements(runtime.getCurrentContext());

        return singletonClass;
    }

    @JIT
    public static DynamicMethod newCompiledModuleBody(ThreadContext context, MethodHandle handle, String id, int line,
                                                      StaticScope scope, Object rubyContainer, boolean maybeRefined) {
        RubyModule newRubyModule = newRubyModuleFromIR(context, id, scope, rubyContainer, maybeRefined);

        return new CompiledIRMethod(handle,id, line, scope, Visibility.PUBLIC, newRubyModule);
    }

    @Interp @JIT
    public static RubyModule newRubyModuleFromIR(ThreadContext context, String id, StaticScope scope,
                                                 Object rubyContainer, boolean maybeRefined) {
        if (!(rubyContainer instanceof RubyModule)) {
            throw context.runtime.newTypeError("no outer class/module");
        }

        RubyModule newRubyModule = ((RubyModule) rubyContainer).defineOrGetModuleUnder(id);
        scope.setModule(newRubyModule);

        if (maybeRefined) scope.captureParentRefinements(context);

        return newRubyModule;
    }

    @JIT
    public static DynamicMethod newCompiledClassBody(ThreadContext context, MethodHandle handle, String id, int line,
                                                     StaticScope scope, Object container,
                                                     Object superClass, boolean maybeRefined) {
        RubyModule newRubyClass = newRubyClassFromIR(context.runtime, id, scope, superClass, container, maybeRefined);

        return new CompiledIRMethod(handle, id, line, scope, Visibility.PUBLIC, newRubyClass);
    }

    @Interp @JIT
    public static RubyModule newRubyClassFromIR(Ruby runtime, String id, StaticScope scope, Object superClass,
                                                Object container, boolean maybeRefined) {
        if (!(container instanceof RubyModule)) throw runtime.newTypeError("no outer class/module");

        RubyClass sc;
        if (superClass == UndefinedValue.UNDEFINED) {
            sc = null;
        } else {
            RubyClass.checkInheritable((IRubyObject) superClass);

            sc = (RubyClass) superClass;
        }

        RubyModule newRubyClass = ((RubyModule)container).defineOrGetClassUnder(id, sc);

        scope.setModule(newRubyClass);

        if (maybeRefined) scope.captureParentRefinements(runtime.getCurrentContext());

        return newRubyClass;
    }

    @Interp
    public static void defInterpretedClassMethod(ThreadContext context, IRScope method, IRubyObject obj) {
        String id = method.getId();
        RubyClass rubyClass = checkClassForDef(context, id, obj);

        if (method.maybeUsingRefinements()) method.getStaticScope().captureParentRefinements(context);

        DynamicMethod newMethod;
        if (context.runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.OFF) {
            newMethod = new InterpretedIRMethod(method, Visibility.PUBLIC, rubyClass);
        } else {
            newMethod = new MixedModeIRMethod(method, Visibility.PUBLIC, rubyClass);
        }

        rubyClass.addMethod(id, newMethod);
        if (!rubyClass.isRefinement()) obj.callMethod(context, "singleton_method_added", method.getName());
    }

    @JIT
    public static void defCompiledClassMethod(ThreadContext context, MethodHandle handle, String id, int line,
                                              StaticScope scope, String encodedArgumentDescriptors,
                                              IRubyObject obj, boolean maybeRefined, boolean receivesKeywordArgs,
                                              boolean needsToFindImplementer) {
        RubyClass rubyClass = checkClassForDef(context, id, obj);

        if (maybeRefined) scope.captureParentRefinements(context);

        // FIXME: needs checkID and proper encoding to force hard symbol
        rubyClass.addMethod(id,
                new CompiledIRMethod(handle, null, -1, id, line, scope, Visibility.PUBLIC, rubyClass,
                        encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer));

        if (!rubyClass.isRefinement()) {
            // FIXME: needs checkID and proper encoding to force hard symbol
            obj.callMethod(context, "singleton_method_added", context.runtime.newSymbol(id));
        }
    }

    @JIT
    public static void defCompiledClassMethod(ThreadContext context, MethodHandle variable, MethodHandle specific,
                                              int specificArity, String id, int line, StaticScope scope,
                                              String encodedArgumentDescriptors,
                                              IRubyObject obj, boolean maybeRefined, boolean receivesKeywordArgs,
                                              boolean needsToFindImplementer) {
        RubyClass rubyClass = checkClassForDef(context, id, obj);

        if (maybeRefined) scope.captureParentRefinements(context);

        rubyClass.addMethod(id, new CompiledIRMethod(variable, specific, specificArity, id, line, scope,
                Visibility.PUBLIC, rubyClass, encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer));

        if (!rubyClass.isRefinement()) obj.callMethod(context, "singleton_method_added", context.runtime.newSymbol(id));
    }

    private static RubyClass checkClassForDef(ThreadContext context, String id, IRubyObject obj) {
        if (obj instanceof RubyFixnum || obj instanceof RubySymbol || obj instanceof RubyFloat) {
            throw context.runtime.newTypeError(str(context.runtime, "can't define singleton method \"",
                    ids(context.runtime, id), "\" for ", obj.getMetaClass().rubyBaseName()));
        }

        // if (obj.isFrozen()) throw context.runtime.newFrozenError("object");

        return obj.getSingletonClass();
    }

    @Interp
    public static void defInterpretedInstanceMethod(ThreadContext context, IRScope method, DynamicScope currDynScope, IRubyObject self) {
        Ruby runtime = context.runtime;
        RubySymbol methodName = method.getName();
        RubyModule rubyClass = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, rubyClass, methodName, currVisibility);

        if (method.maybeUsingRefinements()) method.getStaticScope().captureParentRefinements(context);

        DynamicMethod newMethod;
        if (runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.OFF) {
            newMethod = new InterpretedIRMethod(method, newVisibility, rubyClass);
        } else {
            newMethod = new MixedModeIRMethod(method, newVisibility, rubyClass);
        }

        Helpers.addInstanceMethod(rubyClass, methodName, newMethod, currVisibility, context, runtime);
    }

    @JIT
    public static void defCompiledInstanceMethod(ThreadContext context, MethodHandle handle, String id, int line,
                                                 StaticScope scope, String encodedArgumentDescriptors,
                                                 DynamicScope currDynScope, IRubyObject self, boolean maybeRefined,
                                                 boolean receivesKeywordArgs, boolean needsToFindImplementer) {
        Ruby runtime = context.runtime;
        RubySymbol methodName = runtime.newSymbol(id);
        RubyModule clazz = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, clazz, methodName, currVisibility);

        if (maybeRefined) scope.captureParentRefinements(context);

        DynamicMethod newMethod = new CompiledIRMethod(handle, null, -1, id, line, scope,
                newVisibility, clazz, encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer);

        // FIXME: needs checkID and proper encoding to force hard symbol
        Helpers.addInstanceMethod(clazz, methodName, newMethod, currVisibility, context, runtime);
    }

    @JIT
    public static void defCompiledInstanceMethod(ThreadContext context, MethodHandle variable, MethodHandle specific,
                                                 int specificArity, String id, int line, StaticScope scope,
                                                 String encodedArgumentDescriptors,
                                                 DynamicScope currDynScope, IRubyObject self, boolean maybeRefined,
                                                 boolean receivesKeywordArgs, boolean needsToFindImplementer) {
        Ruby runtime = context.runtime;
        RubySymbol methodName = runtime.newSymbol(id);
        RubyModule clazz = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, clazz, methodName, currVisibility);

        if (maybeRefined) scope.captureParentRefinements(context);

        DynamicMethod newMethod = new CompiledIRMethod(variable, specific, specificArity, id, line, scope,
                newVisibility, clazz, encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer);

        // FIXME: needs checkID and proper encoding to force hard symbol
        Helpers.addInstanceMethod(clazz, methodName, newMethod, currVisibility, context, runtime);
    }

    @JIT
    public static IRubyObject invokeModuleBody(ThreadContext context, DynamicMethod method, Block block) {
        RubyModule implClass = method.getImplementationClass();

        return method.call(context, implClass, implClass, "", block);
    }

    // FIXME: Temporary until CompiledIRMethod part of this is removed.
    @JIT
    public static IRubyObject invokeModuleBody(ThreadContext context, DynamicMethod method) {
        RubyModule implClass = method.getImplementationClass();

        return method.call(context, implClass, implClass, "", Block.NULL_BLOCK);
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject[] pieces, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context, options, pieces);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context, options, arg0);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context, options, arg0, arg1);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context, options, arg0, arg1, arg2);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context, options, arg0, arg1, arg2, arg3);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context, options, arg0, arg1, arg2, arg3, arg4);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    public static RubyRegexp newLiteralRegexp(ThreadContext context, ByteList source, RegexpOptions options) {
        RubyRegexp re = RubyRegexp.newRegexp(context.runtime, source, options);
        re.setLiteral();
        return re;
    }

    @JIT
    public static RubyRegexp newLiteralRegexp(ThreadContext context, ByteList source, int embeddedOptions) {
        return newLiteralRegexp(context, source, RegexpOptions.fromEmbeddedOptions(embeddedOptions));
    }

    @JIT
    public static RubyArray irSplat(ThreadContext context, IRubyObject ary) {
        Ruby runtime = context.runtime;
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck(context, ary, runtime.getArray(), sites(context).to_a_checked);
        if (tmp.isNil()) {
            tmp = runtime.newArray(ary);
        }
        else if (true /**RTEST(flag)**/) { // this logic is only used for bare splat, and MRI dups
            tmp = ((RubyArray)tmp).aryDup();
        }
        return (RubyArray)tmp;
    }

    /**
     * Call to_ary to get Array or die typing.  The optionally dup it if specified.  Some conditional
     * cases in compiler we know we are safe in not-duping.  This method is the same impl as MRIs
     * splatarray instr in the YARV instruction set.
     */
    @JIT @Interp
    public static RubyArray splatArray(ThreadContext context, IRubyObject ary, boolean dupArray) {
        Ruby runtime = context.runtime;
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck(context, ary, runtime.getArray(), sites(context).to_a_checked);

        if (tmp.isNil()) {
            tmp = runtime.newArray(ary);
        } else if (dupArray) {
            tmp = ((RubyArray) tmp).aryDup();
        }

        return (RubyArray) tmp;
    }

    public static IRubyObject irToAry(ThreadContext context, IRubyObject value) {
        return value instanceof RubyArray ? value : RubyArray.aryToAry(context, value);
    }

    public static int irReqdArgMultipleAsgnIndex(int n,  int preArgsCount, int index, int postArgsCount) {
        if (preArgsCount == -1) {
            return index < n ? index : -1;
        } else {
            int remaining = n - preArgsCount;
            if (remaining <= index) {
                return -1;
            } else {
                return (remaining > postArgsCount) ? n - postArgsCount + index : preArgsCount + index;
            }
        }
    }

    public static IRubyObject irReqdArgMultipleAsgn(ThreadContext context, RubyArray rubyArray, int preArgsCount, int index, int postArgsCount) {
        int i = irReqdArgMultipleAsgnIndex(rubyArray.getLength(), preArgsCount, index, postArgsCount);
        return i == -1 ? context.nil : rubyArray.entry(i);
    }

    public static IRubyObject irNot(ThreadContext context, IRubyObject obj) {
        return RubyBoolean.newBoolean(context, !(obj.isTrue()));
    }

    @JIT
    public static RaiseException newRequiredKeywordArgumentError(ThreadContext context, String id) {
        return context.runtime.newArgumentError(str(context.runtime, "missing keyword: ", ids(context.runtime, id)));
    }

    public static void pushExitBlock(ThreadContext context, Block blk) {
        context.runtime.pushEndBlock(context.runtime.newProc(LAMBDA, blk));
    }

    @JIT
    public static void pushExitBlock(ThreadContext context, Object blk) {
        context.runtime.pushEndBlock(context.runtime.newProc(LAMBDA, getBlockFromObject(context, blk)));
    }

    @JIT
    public static FunctionalCachingCallSite newFunctionalCachingCallSite(String name) {
        return new FunctionalCachingCallSite(name);
    }

    public static ProfilingCachingCallSite newProfilingCachingCallSite(CallType callType, String name, IRScope scope, long callSiteId) {
        return new ProfilingCachingCallSite(callType, name, scope, callSiteId);
    }

    @JIT
    public static MonomorphicCallSite newMonomorphicCallSite(String name) {
        return new MonomorphicCallSite(name);
    }

    @JIT
    public static VariableCachingCallSite newVariableCachingCallSite(String name) {
        return new VariableCachingCallSite(name);
    }

    @JIT
    public static RefinedCachingCallSite newRefinedCachingCallSite(String name, StaticScope scope, String callType) {
        return new RefinedCachingCallSite(name, scope, CallType.valueOf(callType));
    }

    @JIT
    public static IRScope decodeScopeFromBytes(Ruby runtime, byte[] scopeBytes, String filename) {
        try {
            return IRReader.load(runtime.getIRManager(), new IRReaderStream(runtime.getIRManager(), scopeBytes, new ByteList(filename.getBytes())));
        } catch (IOException ioe) {
            // should not happen for bytes
            return null;
        }
    }

    @JIT
    public static VariableAccessor getVariableAccessorForRead(IRubyObject object, String name) {
        return ((RubyBasicObject)object).getMetaClass().getRealClass().getVariableAccessorForRead(name);
    }

    @JIT
    public static VariableAccessor getVariableAccessorForWrite(IRubyObject object, String name) {
        return ((RubyBasicObject)object).getMetaClass().getRealClass().getVariableAccessorForWrite(name);
    }

    @JIT
    public static IRubyObject getVariableWithAccessor(IRubyObject self, VariableAccessor accessor, ThreadContext context, String name) {
        Ruby runtime = context.runtime;
        IRubyObject result = (IRubyObject)accessor.get(self);
        if (result == null) {
            if (runtime.isVerbose()) {
                runtime.getWarnings().warning(IRubyWarnings.ID.IVAR_NOT_INITIALIZED, str(runtime, "instance variable ", ids(runtime, name)," not initialized"));
            }
            result = context.nil;
        }
        return result;
    }

    @JIT
    public static void setVariableWithAccessor(IRubyObject self, IRubyObject value, VariableAccessor accessor) {
        accessor.set(self, value);
    }

    @JIT
    public static RubyFixnum getArgScopeDepth(ThreadContext context, StaticScope currScope) {
        int i = 0;
        while (!currScope.isArgumentScope()) {
            currScope = currScope.getEnclosingScope();
            i++;
        }
        return context.runtime.newFixnum(i);
    }

    public static IRubyObject[] toAry(ThreadContext context, IRubyObject[] args) {
        IRubyObject ary;
        if (args.length == 1 && (ary = Helpers.aryOrToAry(context, args[0])) != context.nil) {
            if (ary instanceof RubyArray) {
                args = ((RubyArray) ary).toJavaArray();
            } else {
                throw context.runtime.newTypeError(args[0].getType().getName() + "#to_ary should return Array");
            }
        }
        return args;
    }

    // This is always for PROC type
    private static IRubyObject[] prepareProcArgs(ThreadContext context, Block b, IRubyObject[] args) {
        if (args.length != 1) return args;

        // Potentially expand single value if it is an array depending on what we are calling.
        return IRRuntimeHelpers.convertValueIntoArgArray(context, args[0], b.getBody().getSignature());
    }

    private static IRubyObject[] prepareBlockArgsInternal(ThreadContext context, Block block, IRubyObject[] args) {
        if (args == null) args = IRubyObject.NULL_ARRAY;

        switch (block.type) {
            case LAMBDA:
                // FIXME: passing a lambda to each_with_index via enumerator seems to need this.
                // This is fairly complicated but we should try and eliminate needing this arg spreading
                // here (test_enum.rb:test_cycle):
                //     cond = ->(x, i) {a << x}
                //     @obj.each_with_index.cycle(2, &cond)
                org.jruby.runtime.Signature sig = block.getBody().getSignature();
                if (sig.arityValue() != -1 && sig.required() != 1) {
                    args = toAry(context, args);
                }

                sig.checkArity(context.runtime, args);
                return args;
            case PROC:
                return prepareProcArgs(context, block, args);
        }

        org.jruby.runtime.Signature sig = block.getBody().getSignature();
        int arityValue = sig.arityValue();
        if (!sig.hasKwargs() && arityValue >= -1 && arityValue <= 1) {
            return args;
        }

        // We get here only when we need both required and optional/rest args
        // (keyword or non-keyword in either case).
        // So, convert a single value to an array if possible.
        args = toAry(context, args);

        // Deal with keyword args that needs special handling
        int needsKwargs = sig.hasKwargs() ? 1 - sig.getRequiredKeywordForArityCount() : 0;
        int required = sig.required();
        int actual = args.length;
        if (needsKwargs == 0 || required > actual) {
            // Nothing to do if we have fewer args in args than what is required
            // The required arg instructions will return nil in those cases.
            return args;
        }

        if (sig.isFixed() && required > 0 && required + needsKwargs != actual) {
            final int len = required + needsKwargs; // Make sure we have a ruby-hash
            IRubyObject[] newArgs = ArraySupport.newCopy(args, len);
            if (actual < len) {
                // Not enough args and we need an empty {} for kwargs processing.
                newArgs[len - 1] = RubyHash.newHash(context.runtime);
            } else {
                // We have more args than we need and kwargs is always the last arg.
                newArgs[len - 1] = args[args.length - 1];
            }
            args = newArgs;
        }

        return args;
    }

    /**
     * Check whether incoming args are zero length for a lambda, and no-op for non-lambda.
     *
     * This could probably be simplified to just an arity check with no return value, but returns the
     * incoming args currently for consistency with the other prepares.
     *
     * @param context
     * @param block
     * @param args
     * @return
     */
    @Interp @JIT
    public static IRubyObject[] prepareNoBlockArgs(ThreadContext context, Block block, IRubyObject[] args) {
        if (args == null) args = IRubyObject.NULL_ARRAY;

        if (block.type == LAMBDA) block.getSignature().checkArity(context.runtime, args);

        return args;
    }

    @Interp @JIT
    public static IRubyObject[] prepareSingleBlockArgs(ThreadContext context, Block block, IRubyObject[] args) {
        if (args == null) args = IRubyObject.NULL_ARRAY;

        switch (block.type) {
            case LAMBDA:
                block.getBody().getSignature().checkArity(context.runtime, args);
                return args;
            case PROC:
                if (args.length == 0) {
                    args = context.runtime.getSingleNilArray();
                } else if (args.length == 1) {
                    args = prepareProcArgs(context, block, args);
                } else {
                    args = new IRubyObject[] { args[0] };
                }
        }

        // If there are insufficient args, ReceivePreReqdInstr will return nil
        return args;
    }

    @Interp @JIT
    public static IRubyObject[] prepareFixedBlockArgs(ThreadContext context, Block block, IRubyObject[] args) {
        if (args == null) args = IRubyObject.NULL_ARRAY;

        switch (block.type) {
            case LAMBDA:
                block.getBody().getSignature().checkArity(context.runtime, args);
                return args;
            case PROC:
                return prepareProcArgs(context, block, args);
            default:
                // If we need more than 1 reqd arg, convert a single value to an array if possible.
                // If there are insufficient args, ReceivePreReqdInstr will return nil
                return toAry(context, args);
        }
    }

    // This is the placeholder for scenarios not handled by specialized instructions.
    @Interp @JIT
    public static IRubyObject[] prepareBlockArgs(ThreadContext context, Block block, IRubyObject[] args, boolean usesKwArgs) {
        args = prepareBlockArgsInternal(context, block, args);
        if (usesKwArgs) {
            args = frobnicateKwargsArgument(context, args, block.getBody().getSignature().required());
        }
        return args;
    }

    private static DynamicScope getNewBlockScope(Block block, boolean pushNewDynScope, boolean reuseParentDynScope) {
        DynamicScope newScope = block.getBinding().getDynamicScope();
        if (pushNewDynScope) return block.allocScope(newScope);

        // Reuse! We can avoid the push only if surrounding vars aren't referenced!
        if (reuseParentDynScope) return newScope;

        // No change
        return null;
    }

    @Interp
    public static DynamicScope pushBlockDynamicScopeIfNeeded(ThreadContext context, Block block, boolean pushNewDynScope, boolean reuseParentDynScope) {
        DynamicScope newScope = getNewBlockScope(block, pushNewDynScope, reuseParentDynScope);
        if (newScope != null) {
            context.pushScope(newScope);
        }
        return newScope;
    }

    @JIT
    public static DynamicScope pushBlockDynamicScopeNew(ThreadContext context, Block block) {
        DynamicScope newScope = block.allocScope(block.getBinding().getDynamicScope());

        context.pushScope(newScope);

        return newScope;
    }

    @JIT
    public static DynamicScope pushBlockDynamicScopeReuse(ThreadContext context, Block block) {
        DynamicScope newScope = block.getBinding().getDynamicScope();

        context.pushScope(newScope);

        return newScope;
    }

    @Interp @JIT
    public static IRubyObject updateBlockState(Block block, IRubyObject self) {
        // SSS FIXME: Why is self null in non-binding-eval contexts?
        if (self == null || block.getEvalType() == EvalType.BINDING_EVAL) {
            // Update self to the binding's self
            self = useBindingSelf(block.getBinding());
        }

        // Return self in case it has been updated
        return self;
    }

    public static IRubyObject useBindingSelf(Binding binding) {
        IRubyObject self = binding.getSelf();
        binding.getFrame().setSelf(self);

        return self;
    }

    /**
     * Create a new Symbol.to_proc for the given symbol name and encoding.
     *
     * @param context
     * @param value
     * @return
     */
    @JIT
    public static RubyProc newSymbolProc(ThreadContext context, ByteList value) {
        RubySymbol symbol = RubySymbol.newSymbol(context.runtime, value);
        return IRRuntimeHelpers.newSymbolProc(context, symbol);
    }

    /**
     * Create a new Symbol.to_proc for the given symbol name and encoding.
     *
     * @param context
     * @param symbol
     * @return
     */
    @Interp
    public static RubyProc newSymbolProc(ThreadContext context, RubySymbol symbol) {
        return (RubyProc) symbol.to_proc(context);
    }

    @JIT
    public static IRubyObject[] singleBlockArgToArray(IRubyObject value) {
        return value instanceof RubyArray ?
                ((RubyArray) value).toJavaArray() :
                new IRubyObject[] { value };
    }

    @JIT
    public static Block prepareBlock(ThreadContext context, IRubyObject self, DynamicScope scope, BlockBody body) {
        Binding binding = newFrameScopeBinding(context, self, scope);

        return new Block(body, binding);
    }

    public static Binding newFrameScopeBinding(ThreadContext context, IRubyObject self, DynamicScope scope) {
        Frame frame = context.getCurrentFrame().capture();
        Binding binding = new Binding(self, frame, frame.getVisibility(), scope);
        binding.setMethod(frame.getName());
        return binding;
    }

    public static RubyString newFrozenString(ThreadContext context, ByteList bytelist, int coderange, String file, int line) {
        Ruby runtime = context.runtime;

        RubyString string = RubyString.newString(runtime, bytelist, coderange);

        if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
            // stuff location info into the string and then freeze it
            RubyArray info = (RubyArray) runtime.newArray(runtime.newString(file).freeze(context), runtime.newFixnum(line)).freeze(context);
            string.setInstanceVariable(RubyString.DEBUG_INFO_FIELD, info);
            string.setFrozen(true);
        } else {
            string = runtime.freezeAndDedupString(string);
        }

        return string;
    }

    @JIT @Interp
    public static RubyString freezeLiteralString(RubyString string) {
        string.setFrozen(true);

        return string;
    }

    @JIT @Interp
    public static RubyString freezeLiteralString(RubyString string, ThreadContext context, String file, int line) {
        Ruby runtime = context.runtime;

        if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
            // stuff location info into the string and then freeze it
            RubyArray info = (RubyArray) runtime.newArray(runtime.newString(file).freeze(context), runtime.newFixnum(line)).freeze(context);
            string.setInstanceVariable(RubyString.DEBUG_INFO_FIELD, info);
        }

        return freezeLiteralString(string);
    }

    @JIT
    public static IRubyObject callOptimizedAref(ThreadContext context, IRubyObject caller, IRubyObject target, RubyString keyStr, CallSite site) {
        if (target instanceof RubyHash && ((CachingCallSite) site).isBuiltin(target)) {
            // call directly with cached frozen string
            return ((RubyHash) target).op_aref(context, keyStr);
        }

        return site.call(context, caller, target, keyStr.strDup(context.runtime));
    }

    /**
     * asString using a given call site
     */
    @JIT
    public static RubyString asString(ThreadContext context, IRubyObject caller, IRubyObject target, CallSite site) {
        IRubyObject str = site.call(context, caller, target);

        if (!(str instanceof RubyString)) {
            return (RubyString) target.anyToString();
        }

        if (target.isTaint()) str.setTaint(true);

        return (RubyString) str;
    }

    @JIT
    public static RubyArray newArray(ThreadContext context) {
        return RubyArray.newEmptyArray(context.runtime);
    }

    @JIT
    public static RubyArray newArray(ThreadContext context, IRubyObject obj) {
        return RubyArray.newArray(context.runtime, obj);
    }

    @JIT
    public static RubyArray newArray(ThreadContext context, IRubyObject obj0, IRubyObject obj1) {
        return RubyArray.newArray(context.runtime, obj0, obj1);
    }

    @JIT @Interp
    public static RubyString getFileNameStringFromScope(ThreadContext context, StaticScope currScope) {
        String file = currScope.getFile();

        // FIXME: Not very efficient to do all this every time
        return context.runtime.newString(file);
    }

    @JIT
    public static void callTrace(ThreadContext context, RubyEvent event, String name, String filename, int line) {
        if (context.runtime.hasEventHooks()) {
            // FIXME: Try and statically generate END linenumber instead of hacking it.
            int linenumber = line == -1 ? context.getLine() : line;

            context.trace(event, name, context.getFrameKlazz(), filename, linenumber);
        }
    }

    public static void warnSetConstInRefinement(ThreadContext context, IRubyObject self) {
        if (self instanceof RubyModule && ((RubyModule) self).isRefinement()) {
            context.runtime.getWarnings().warn("not defined at the refinement, but at the outer class/module");
        }
    }

    @JIT
    public static void putConst(ThreadContext context, IRubyObject self, RubyModule module, String id, IRubyObject value) {
        warnSetConstInRefinement(context, self);

        module.setConstant(id, value);
    }

    @JIT
    public static void putClassVariable(ThreadContext context, IRubyObject self, RubyModule module, String id, IRubyObject value) {
        warnSetConstInRefinement(context, self);

        module.setClassVar(id, value);
    }

    @JIT
    public static RubyRational newRationalCanonicalize(ThreadContext context, IRubyObject num, IRubyObject den) {
        return (RubyRational) RubyRational.newRationalCanonicalize(context, num, den);
    }

    @JIT
    public static RubyComplex newComplexRaw(ThreadContext context, IRubyObject i) {
        return RubyComplex.newComplexRawImage(context.runtime, i);
    }

    @JIT
    public static RubySymbol newDSymbol(ThreadContext context, IRubyObject symbol) {
        return context.runtime.newSymbol(symbol.asString().getByteList());
    }

    @JIT
    public static RubyClass getStandardError(ThreadContext context) {
        return context.runtime.getStandardError();
    }

    @JIT
    public static RubyClass getObject(ThreadContext context) {
        return context.runtime.getObject();
    }

    @JIT @Interp
    public static IRubyObject svalue(ThreadContext context, Object val) {
        return (val instanceof RubyArray) ? (RubyArray) val : context.nil;
    }

    @JIT
    public static void aliasGlobalVariable(Ruby runtime, Object newName, Object oldName) {
        runtime.getGlobalVariables().alias(newName.toString(), oldName.toString());
    }

    private static IRRuntimeHelpersSites sites(ThreadContext context) {
        return context.sites.IRRuntimeHelpers;
    }
}
