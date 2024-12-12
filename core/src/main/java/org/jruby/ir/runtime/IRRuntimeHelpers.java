package org.jruby.ir.runtime;

import com.headius.invokebinder.Signature;

import java.io.IOException;
import java.lang.invoke.MethodHandle;

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
import org.jruby.RubyMatchData;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
import org.jruby.RubyRational;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.api.Create;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.CompiledIRNoProtocolMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRBodyMethod;
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
import org.jruby.runtime.TraceEventManager;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
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

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.ir.operands.UndefinedValue.UNDEFINED;
import static org.jruby.runtime.Block.Type.LAMBDA;
import static org.jruby.runtime.ThreadContext.*;
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
            throw Helpers.newLocalJumpErrorForBreak(context.runtime, breakValue);
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
        String name = (nameArg instanceof String str) ? str : nameArg.toString();

        if (module == null) {
            throw typeError(context, str(context.runtime, "No class to undef method '",  asSymbol(context, name), "'."));
        }

        module.undef(context, name);

        return context.nil;
    }

    @JIT
    public static double unboxFloat(IRubyObject val) {
        return val instanceof RubyFloat flote ? flote.getValue() : ((RubyFixnum)val).getDoubleValue();
    }

    @JIT
    public static long unboxFixnum(IRubyObject val) {
        return val instanceof RubyFloat flote ? (long) flote.getValue() : ((RubyFixnum)val).getLongValue();
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
            if (!(excType instanceof RubyModule)) {
                throw typeError(context, str(context.runtime, "class or module required for rescue clause. Found: ", excType));
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

        return asBoolean(context, ret);
    }

    // partially: vm_insnhelper.c - vm_check_match + check_match
    public static IRubyObject isEQQ(ThreadContext context, IRubyObject receiver, IRubyObject value, CallSite callSite, boolean splattedValue) {
        boolean isUndefValue = value == UNDEFINED;

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
                        ((RubyArray) value).toJavaArray(context) :
                        new IRubyObject[] { value };
            case  0:
            case  1:
                return signature.rest() == org.jruby.runtime.Signature.Rest.ANON ?
                        IRBlockBody.toAry(context, value) :
                        new IRubyObject[] { value };
        }

        return IRBlockBody.toAry(context, value);
    }

    // NORMAL yield paths passed through yieldSpecific and call (for when block is passed through -- some internal weirdness on our part).
    public static IRubyObject[] convertValueIntoArgArray(ThreadContext context, RubyArray array, org.jruby.runtime.Signature signature) {
        switch (signature.arityValue()) {
            case -1:
                return array.toJavaArray(context);
            case 0:
            case 1:
                return signature.rest() == org.jruby.runtime.Signature.Rest.ANON ?
                        IRBlockBody.toAry(context, array) :
                        new IRubyObject[] { array };
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

    /*
     *  Specific arity methods made during the JIT only accept the arguments they accept.  One strange case with
     * Ruby 3 keywords is any method can recieve an extra argument for keyword arguments.  For specific arity we do
     * no accept keyword args but any method can pass them anyways.  We have instructions for processing keywords
     * but in specific arity methods we check arity mismatches in an args[] signature and it will complain if it
     * sees that extra kwarg....and it should...unless it happens to be empty (**{}).  In that case we silently ignore
     * that empty arg.
     *
     * As of 9.4.0.0 the design is such that in Ruby we bifurcate and check for empty kwargs and do not pass them.
     * For ruby2_keyword methods we do not do this (or we would have to put this same if conditional for every call
     * in Ruby along with a dynamic check looking to see if it is this kind of method -- which would be before our
     * actual callsite).  So this special logic here literally only exists for ruby2_keyword arg methods which receive and
     * pass splatted values (where last one might be an empty kwarg hash).
     *
     * Note: we probably need this logic baked into every callsite in the system as this would cause a hit in the case
     * of keywords but it is much simpler than having all kwargs parameter calls emit an if statement looking for
     * emptiness in the kwarg.  Also all native methods will complain for this case if you call with an empty kwarg.
     */
    public static void checkAritySpecificArgs(ThreadContext context, StaticScope scope, Object[] args,
                                  int required, int opt, boolean rest, int restKey, Block block) {
        int argsLength = args.length;

        if ((block == null || block.type.checkArity) && (argsLength < required || (!rest && argsLength > (required + opt)))) {
            if (argsLength > (required + opt) && args[argsLength - 1] instanceof RubyHash) {
                RubyHash last = (RubyHash) args[argsLength - 1];
                if (last.isRuby2KeywordHash() && last.isEmpty()) return;
            }
            //System.out.println("C: " + context.getFile() + ":" + context.getLine());
            Arity.raiseArgumentError(context, argsLength, required, rest ? UNLIMITED_ARGUMENTS : (required + opt));
        }
    }

    public static void checkArity(ThreadContext context, StaticScope scope, Object[] args, Object keywords,
                                  int required, int opt, boolean rest, int restKey, Block block) {
        int argsLength = args.length - (keywords != UNDEFINED ? 1: 0);

        if ((block == null || block.type.checkArity) && (argsLength < required || (!rest && argsLength > (required + opt)))) {
            //System.out.println("C: " + context.getFile() + ":" + context.getLine());
            Arity.raiseArgumentError(context, argsLength, required, rest ? UNLIMITED_ARGUMENTS : (required + opt));
        }

        if (restKey == -1 && keywords != UNDEFINED) checkForExtraUnwantedKeywordArgs(context, scope, (RubyHash) keywords);
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
     * Check if the scope matches the configured ir.print.pattern, or if no pattern is set.
     *
     * @param scope the scope to match
     * @return whether to print the scope
     */
    public static boolean shouldPrintScope(IRScope scope) {
        String pattern = Options.IR_PRINT_PATTERN.load();

        return pattern.equals(Options.IR_PRINT_PATTERN_NO_PATTERN_STRING) || scope.getId().matches(pattern);
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

        if (data.isRunning()) data.coverLine(filename, line);
    }

    @JIT @Interp
    public static IRubyObject hashCheck(ThreadContext context, IRubyObject hash) {
        return TypeConverter.checkHashType(context.runtime, hash);
    }

    public static IRubyObject isHashEmpty(ThreadContext context, IRubyObject hash) {
        // ARGSPUSH ended up realizing we have an empty kwargs already and stripped off the empty value (meaning
        // any call need not worry about subtracting a kwargs argument.
        if ((context.callInfo & CALL_KEYWORD_EMPTY) != 0) return context.fals;

        hash = TypeConverter.checkHashType(context.runtime, hash);

        boolean isEmpty = hash instanceof RubyHash && ((RubyHash) hash).size() == 0;
        if (isEmpty) context.callInfo |= CALL_KEYWORD_EMPTY;
        return  isEmpty ? context.tru : context.fals;
    }

    public static IRubyObject undefined() {
        return UNDEFINED;
    }

    // specific arity methods in JIT will only be fixed arity meaning no kwargs and no rest args.
    // this logic is the same as recieveKeywords but we know it will never be a keyword argument (jit
    // will save %undefined as the keyword value).
    // Due to jruby/jruby#8119 and the potential for ruby2_keywords flags to change after JIT, jitted code will always
    // call on this path and pass in the live value of ruby2_keywords from the scope.
    @JIT
    public static IRubyObject receiveSpecificArityKeywords(ThreadContext context, IRubyObject last, boolean ruby2Keywords) {
        if (!(last instanceof RubyHash)) {
            ThreadContext.clearCallInfo(context);
            return last;
        }

        return ruby2Keywords ?
                receiveSpecificArityRuby2HashKeywords(context, last) :
                receiveSpecificArityHashKeywords(context, last);
    }

    private static IRubyObject receiveSpecificArityHashKeywords(ThreadContext context, IRubyObject last) {
        int callInfo = ThreadContext.resetCallInfo(context);
        boolean isKwarg = (callInfo & CALL_KEYWORD) != 0;

        return receiverSpecificArityKwargsCommon(context, last, callInfo, isKwarg);
    }

    private static IRubyObject receiveSpecificArityRuby2HashKeywords(ThreadContext context, IRubyObject last) {
        int callInfo = ThreadContext.resetCallInfo(context);
        boolean isKwarg = (callInfo & CALL_KEYWORD) != 0;

        // ruby2_keywords only get unmarked if it enters a method which accepts keywords.
        // This means methods which don't just keep that marked hash around in case it is passed
        // onto another method which accepts keywords.
        if (isKwarg) {
            // a ruby2_keywords method which happens to receive a keyword.  Mark hash as ruby2_keyword
            // So it can be used similarly to an ordinary hash passed in this way.

            RubyHash hash = (RubyHash) last;
            hash = hash.dupFast(context);
            hash.setRuby2KeywordHash(true);

            return hash;
        }

        return receiverSpecificArityKwargsCommon(context, last, callInfo, false);
    }

    private static IRubyObject receiverSpecificArityKwargsCommon(ThreadContext context, IRubyObject last, int callInfo, boolean isKwarg) {
        // ruby2_keywords only get unmarked if it enters a method which accepts keywords.
        // This means methods which don't just keep that marked hash around in case it is passed
        // onto another method which accepts keywords.
        if ((callInfo & CALL_KEYWORD_REST) != 0) {
            // This is kwrest passed to a method which does not accept kwargs

            // We pass empty kwrest through so kwrest does not try and slurp it up as normal argument.
            // This complicates check_arity but empty ** is special case.
            RubyHash hash = (RubyHash) last;
            return hash;
        } else if (!isKwarg) {
            // This is just an ordinary hash as last argument
            return last;
        } else {
            RubyHash hash = (RubyHash) last;
            return hash.dupFast(context);
        }
    }

    /**
     * Simplified receiveKeywords when:
     * <li>receiver is not a ruby2_keywords method</li>
     * <li>receiver does not accept keywords</li>
     * <li>there's no rest argument</li>
     *
     * @param context
     * @param args
     * @return the prepared kwargs hash, or UNDEFINED as a sigil for no kwargs
     */
    @JIT
    public static IRubyObject receiveNormalKeywordsNoRestNoKeywords(ThreadContext context, IRubyObject[] args) {
        int callInfo = ThreadContext.resetCallInfo(context);
        if (shouldHandleKwargs(args, callInfo) && (callInfo & CALL_SPLATS) != 0) {
            return receiveKeywordsWithSplatsNoRestNoKeywords(context, args);
        }

        return UNDEFINED;
    }

    /**
     * Handle incoming keyword arguments given the receiver's rest arg, keyword acceptance, and need for ruby2_keywords.
     *
     * We return as undefined and not null when no kwarg since null gets auto-converted to nil because
     * temp vars do this to work around no explicit initialization of temp values (e.g. they might start as null).
     *
     * @param context
     * @param args
     * @param hasRestArgs
     * @param acceptsKeywords
     * @param ruby2_keywords_method
     * @return
     */
    @Interp
    public static IRubyObject receiveKeywords(ThreadContext context, IRubyObject[] args, boolean hasRestArgs,
                                              boolean acceptsKeywords, boolean ruby2_keywords_method) {
        int callInfo = ThreadContext.resetCallInfo(context);
        if (shouldHandleKwargs(args, callInfo)) {
            return receiveKeywordsHash(context, args, hasRestArgs, acceptsKeywords, ruby2_keywords_method, callInfo);
        }

        return UNDEFINED;
    }

    private static IRubyObject receiveKeywordsWithSplatsNoRestNoKeywords(ThreadContext context, IRubyObject[] args) {
        RubyHash hash = (RubyHash) args[args.length - 1];

        if (hash.isRuby2KeywordHash()) {
            if (hash.isEmpty()) {
                // case where we somehow (hash.clear) a marked ruby2_keyword.  We pass it as keyword even in non-keyword
                // accepting methods so it is subtracted from the arity count.  Normally empty keyword arguments are not
                // passed along but ruby2_keyword is a strange case since it is mutable by users.
                return hash;
            }

            clearTrailingHashRuby2Keywords(context, args, hash);
        }

        return UNDEFINED;
    }

    private static void clearTrailingHashRuby2Keywords(ThreadContext context, IRubyObject[] args, RubyHash hash) {
        RubyHash newHash = hash.dupFast(context);
        newHash.setRuby2KeywordHash(false);
        args[args.length - 1] = newHash;
    }

    private static boolean shouldHandleKwargs(IRubyObject[] args, int callInfo) {
        return (callInfo & CALL_KEYWORD_EMPTY) == 0 && args.length >= 1 && args[args.length - 1] instanceof RubyHash;
    }

    private static IRubyObject receiveKeywordsHash(ThreadContext context, IRubyObject[] args, boolean hasRestArgs, boolean acceptsKeywords, boolean ruby2_keywords_method, int callInfo) {
        RubyHash hash = (RubyHash) args[args.length - 1];

        // We record before funging last arg because we may unmark and replace last arg.
        boolean ruby2_keywords_hash = hash.isRuby2KeywordHash();

        // ruby2_keywords only get unmarked if it enters a method which accepts keywords.
        // This means methods which don't just keep that marked hash around in case it is passed
        // onto another method which accepts keywords.
        if (ruby2_keywords_hash && acceptsKeywords) {
            if (!hash.isEmpty()) hash = hash.dupFast(context);
            if (!ruby2_keywords_method) hash.setRuby2KeywordHash(false);
            return hash;
        }

        boolean callSplats = (callInfo & CALL_SPLATS) != 0;
        boolean callSplatsWithRuby2KeywordsHash = callSplats && ruby2_keywords_hash;

        // if we're splatting a ruby2_keywords hash
        //    AND the hash is non-empty
        //    AND keywords are accepted OR there's no rest args,
        // clear the ruby2_keywords flag from the hash
        if (callSplatsWithRuby2KeywordsHash
                && !hash.isEmpty()
                && (acceptsKeywords || !hasRestArgs)) {
            clearTrailingHashRuby2Keywords(context, args, hash);
        }

        boolean callKeyword = (callInfo & CALL_KEYWORD) != 0;

        // If method wants ruby2 keywords and call has keywords, convert to ruby2_keywords hash
        if (ruby2_keywords_method && callKeyword) {
            setTrailingHashRuby2Keywords(context, args, hash);
        } else {
            // If splat call with ruby2_keywords hash that's not empty, just return it
            if (callSplatsWithRuby2KeywordsHash && hash.isEmpty()) {
                return hash;
            }

            // If ordinary hash as last argument, dup and return it
            if (callKeyword && acceptsKeywords && !hash.isEmpty()) {
                return hash.dupFast(context);
            }
        }

        // All other situations no-op
        return UNDEFINED;
    }

    private static void setTrailingHashRuby2Keywords(ThreadContext context, IRubyObject[] args, RubyHash hash) {
        hash = hash.dupFast(context);
        hash.setRuby2KeywordHash(true);
        args[args.length - 1] = hash;
    }

    /**
     * Methods like Kernel#send if it receives a key-splatted value at a send site (send :foo, **h)
     * it will dup h.
     */
    public static IRubyObject dupIfKeywordRestAtCallsite(ThreadContext context, IRubyObject arg) {
        int callInfo = context.callInfo;

        if ((callInfo & CALL_KEYWORD_EMPTY) == 0 && (callInfo & CALL_KEYWORD_REST) != 0) {
            arg = arg.dup();
            context.callInfo = callInfo;
        }

        return arg;
    }

    @JIT @Interp
    public static void setCallInfo(ThreadContext context, int flags) {
        // FIXME: This may propagate empty more than the current call?   empty might need to be stuff elsewhere to prevent this.
        context.callInfo = (context.callInfo & CALL_KEYWORD_EMPTY) | flags;
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

        if (irScope != null && irScope.isScriptScope() && !irScope.hasFlipFlops()) {
            DynamicScope tlbScope = ((IRScriptBody) irScope).getScriptDynamicScope();
            if (tlbScope != null) {
                context.preScopedBody(tlbScope);
                return tlbScope;
            }
        }

        DynamicScope dynScope = DynamicScope.newDynamicScope(scope);
        context.pushScope(dynScope);

        return dynScope;
    }

    public static IRubyObject blockGivenOrCall(ThreadContext context, IRubyObject self, FunctionalCachingCallSite blockGivenSite, Object blk) {
        CacheEntry blockGivenEntry = blockGivenSite.retrieveCache(self);

        if (!blockGivenEntry.method.getRealMethod().isBuiltin()) {
            return blockGivenSite.call(context, self, self);
        }

        return isBlockGiven(context, blk);
    }

    private static class InvalidKeyException extends RuntimeException {}
    private static final InvalidKeyException INVALID_KEY_EXCEPTION = new InvalidKeyException();
    private static final RubyHash.VisitorWithState<StaticScope> CheckUnwantedKeywordsVisitor = new RubyHash.VisitorWithState<StaticScope>() {
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, StaticScope scope) {
            if (!isValidKeyword(scope, key)) throw INVALID_KEY_EXCEPTION;
        }
    };

    private static boolean isValidKeyword(StaticScope scope, IRubyObject key) {
        return key instanceof RubySymbol && scope.keywordExists(((RubySymbol) key).idString());
    }

    private static class GatherUnwantedKeywordsVisitor extends RubyHash.VisitorWithState<StaticScope> {
        RubyArray invalidKwargs;
        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, StaticScope scope) {
            if (!isValidKeyword(scope, key)) {
                if (invalidKwargs == null) invalidKwargs = newArray(context);
                invalidKwargs.add(key.inspect(context));
            }
        }

        public void raiseIfError(ThreadContext context) {
            if (invalidKwargs != null) {
                //System.out.println("RAISEEEEE: " + context.getFile() + ":" + context.getLine());
                RubyString errorMessage = (RubyString) invalidKwargs.join(context, newString(context, ", "));
                String prefix = invalidKwargs.size() == 1 ? "unknown keyword: " : "unknown keywords: ";

                throw argumentError(context, prefix + errorMessage);
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
        return minArgsLength < n ? rubyArray.entry(index) : UNDEFINED;
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
                frameClass.getSuperClass() != null &&
                frameClass.getSuperClass().isMethodBound(frameName, false);

        return defined ? definedMessage : context.nil;
    }

    public static IRubyObject nthMatch(ThreadContext context, int matchNumber) {
        return RubyRegexp.nth_match(matchNumber, context.getBackRef());
    }

    public static void defineAlias(ThreadContext context, IRubyObject self, DynamicScope currDynScope,
                                   IRubyObject newName, IRubyObject oldName) {
        if (self == null || self instanceof RubyFixnum || self instanceof RubySymbol) {
            throw typeError(context, "no class to make alias");
        }

        findInstanceMethodContainer(context, currDynScope, self).aliasMethod(context, newName, oldName);
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

        // We ran out of scopes to check -- look in arg's metaclass
        if ((scope == null) && (arg != null)) rubyClass = arg.getMetaClass();
        if (rubyClass == null) throw typeError(context, "no class/module to define class variable");

        return rubyClass;
    }

    @JIT @Interp
    public static IRubyObject mergeKeywordArguments(ThreadContext context, IRubyObject restKwarg,
                                                    IRubyObject explicitKwarg, boolean checkForDuplicates) {
        // FIXME: JIT is generating a hash which is empty but seems to contain an %undefined within it.
        //   This was crashing because it would dup it and then try and dup the undefined within it.
        //   This replacement logic is correct even if that was figured out but this should just be
        //   hash = checkHashType(...).dup().
        RubyHash hash;
        if (!(restKwarg instanceof RubyHash hsh)) {
            hash = (RubyHash) TypeConverter.checkHashType(context.runtime, restKwarg);
        } else {
            hash = !hsh.isEmpty() ? (RubyHash) hsh.dup() : newHash(context);
        }
        hash.modify();

        RubyHash otherHash = explicitKwarg.convertToHash();

        // If all the kwargs are empty let's discard them
        if (otherHash.empty_p(context).isTrue()) {
            return hash;
        }

        if (checkForDuplicates) {
            otherHash.visitAll(context, new KwargMergeVisitor(hash), Block.NULL_BLOCK);
        } else {
            hash.merge_bang(context, new IRubyObject[] { otherHash }, Block.NULL_BLOCK);
        }

        return hash;
    }

    private static class KwargMergeVisitor extends RubyHash.VisitorWithState<Block> {
        final RubyHash target;

        KwargMergeVisitor(RubyHash target) {
            this.target = target;
        }

        @Override
        public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Block block) {
            if (target.fastARef(key) != null) {
                context.runtime.getWarnings().warn(context, key, KwargMergeVisitor::duplicationWarning);
            }
            target.op_aset(context, key, value);
        }

        private static String duplicationWarning(ThreadContext c, IRubyObject k, RubyStackTraceElement trace) {
            return str(c.runtime, "key ", k.inspect(c), " is duplicated and overwritten on line " + (trace.getLineNumber()));
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
                                // FIXME: JIT has different module set here M vs MetaM.  Figure out discrepency.
                                // Instance methods when defined within a method which is a singleton should
                                // use the singletons container scope.
                                if (!(self instanceof MetaClass) && self.getMetaClass().isSingleton()) {
                                    return ds.getStaticScope().getModule();
                                }

                                // This is a similar scenario as the FIXME above that was added
                                // in b65a5842ecf56ca32edc2a17800968f021b6a064. At that time,
                                // I was wondering if it would affect this site here and looks
                                // like it does.
                                return self instanceof RubyModule ? (RubyModule) self : self.getMetaClass();

                            case INSTANCE_METHOD:
                                return self.getMetaClass();

                            case SCRIPT_BODY:
                                return currDynScope.getStaticScope().getModule();

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
        return asBoolean(context,  ((Block) blk).isGiven() );
    }

    @JIT @Interp
    public static IRubyObject receiveRestArg(ThreadContext context, IRubyObject[] args, IRubyObject keywords, int required, int argIndex) {
        int argsLength = args.length + (keywords != UNDEFINED ? -1 : 0);

        if (required == 0 && argsLength == args.length ) return RubyArray.newArray(context.runtime, args);

        int remainingArguments = argsLength - required;
        if (remainingArguments <= 0) return newEmptyArray(context);

        return RubyArray.newArrayMayCopy(context.runtime, args, argIndex, remainingArguments);
    }

    @JIT @Interp
    public static IRubyObject receivePostReqdArg(ThreadContext context, IRubyObject[] args, IRubyObject keywords,
                                                 int pre, int opt, boolean rest, int post, int argIndex) {
        int required = pre + post;
        // FIXME: Once we extract kwargs from rest of args processing we can delete this extract and n calc.
        int n = keywords != UNDEFINED ? args.length - 1 : args.length;
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

    @JIT @Interp
    public static IRubyObject receiveOptArg(IRubyObject[] args, IRubyObject keywords, int requiredArgs,
                                            int preArgs, int argIndex) {
        int optArgIndex = argIndex;  // which opt arg we are processing? (first one has index 0, second 1, ...).
        int argsLength = keywords != UNDEFINED ? args.length - 1 : args.length;

        if (requiredArgs + optArgIndex >= argsLength) return UNDEFINED; // No more args left

        return args[preArgs + optArgIndex];
    }

    public static IRubyObject getPreArgSafe(ThreadContext context, IRubyObject[] args, int argIndex) {
        IRubyObject result;
        result = argIndex < args.length ? args[argIndex] : context.nil; // SSS FIXME: This check is only required for closures, not methods
        return result;
    }

    @JIT
    public static IRubyObject receiveKeywordArg(ThreadContext context, IRubyObject keywords, String id) {
        return receiveKeywordArg(keywords, asSymbol(context, id));
    }

    @Interp
    public static IRubyObject receiveKeywordArg(IRubyObject keywords, RubySymbol key) {
        if (keywords == UNDEFINED) return UNDEFINED;

        IRubyObject value = ((RubyHash) keywords).delete(key);

        return value == null ? UNDEFINED : value;
    }

    @JIT
    public static IRubyObject keywordRestOnHash(ThreadContext context, IRubyObject rest) {
        TypeConverter.checkType(context, rest, context.runtime.getHash());
        return ((RubyHash) rest).dupFast(context);
    }

    @JIT @Interp
    public static IRubyObject receiveKeywordRestArg(ThreadContext context, IRubyObject keywords) {
        return keywords == UNDEFINED ? newSmallHash(context) : (RubyHash) keywords;
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

        Object[] newArgs = RubyToJavaInvoker.convertArguments(javaMethod, args);

        JavaProxyClass jpc = JavaProxyClass.getProxyClass(context.runtime, definingModule);

        if (jpc != null) {
            // self is a Java subclass, need to do a bit more logic to dispatch the right method
            JavaProxyMethod jpm = jpc.getMethod(id, javaMethod.getParameterTypes());

            if (jpm != null && jpm.hasSuperImplementation()) {
                return javaMethod.invokeDirectSuperWithExceptionHandling(context, jpm.getSuperMethod(), javaInvokee, newArgs);
            }
        }

        return javaMethod.invokeDirectWithExceptionHandling(context, javaMethod.getValue(), javaInvokee, newArgs);
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
    public static RubyString newFrozenStringFromRaw(ThreadContext context, String str, String encoding, int cr) {
        return newFrozenString(context, newByteListFromRaw(context.runtime, str, encoding), cr);
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
    public static RubyHash constructHashFromArray(ThreadContext context, IRubyObject[] pairs) {
        int length = pairs.length / 2;
        boolean useSmallHash = length <= 10;

        RubyHash hash = useSmallHash ? newSmallHash(context) : newHash(context);

        for (int i = 0; i < pairs.length;) {
            if (useSmallHash) {
                hash.fastASetSmall(context.runtime, pairs[i++], pairs[i++], true);
            } else {
                hash.fastASet(context.runtime, pairs[i++], pairs[i++], true);
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
        RubyModule object = objectClass(context);
        IRubyObject constant = staticScope == null ? object.getConstant(constName) : staticScope.getConstantInner(constName);

        // Inheritance lookup
        RubyModule module = null;
        if (constant == null) {
            // SSS FIXME: Is this null check case correct?
            module = staticScope == null ? object : staticScope.getModule();
            constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);
        }

        // Call const_missing or cache
        return constant != null ?
                constant : module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(constName));
    }

    @JIT
    public static IRubyObject inheritedSearchConst(ThreadContext context, IRubyObject cmVal, String constName, boolean noPrivateConsts) {
        if (!(cmVal instanceof RubyModule)) throw typeError(context, "", cmVal, " is not a class/module");
        RubyModule module = (RubyModule) cmVal;

        IRubyObject constant = noPrivateConsts ? module.getConstantFromNoConstMissing(constName, false) : module.getConstantNoConstMissing(constName);

        if (constant == null) {
            constant = UNDEFINED;
        }

        return constant;
    }

    @JIT
    public static IRubyObject lexicalSearchConst(ThreadContext context, StaticScope staticScope, String constName) {
        IRubyObject constant = staticScope.getConstantInner(constName);

        if (constant == null) {
            constant = UNDEFINED;
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

        return new InterpretedIRBodyMethod(metaClassBody, singletonClass);
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
        if (!(rubyContainer instanceof RubyModule)) throw typeError(context, "no outer class/module");

        RubyModule newRubyModule = ((RubyModule) rubyContainer).defineOrGetModuleUnder(context, id, scope.getFile(), context.getLine() + 1);
        scope.setModule(newRubyModule);

        if (maybeRefined) scope.captureParentRefinements(context);

        return newRubyModule;
    }

    @JIT
    public static DynamicMethod newCompiledClassBody(ThreadContext context, MethodHandle handle, String id, int line,
                                                     StaticScope scope, Object container,
                                                     Object superClass, boolean maybeRefined) {
        RubyModule newRubyClass = newRubyClassFromIR(context, id, scope, superClass, container, maybeRefined);

        return new CompiledIRMethod(handle, id, line, scope, Visibility.PUBLIC, newRubyClass);
    }

    @Interp
    public static RubyModule newRubyClassFromIR(ThreadContext context, String id, StaticScope scope, Object superClass,
                                                Object container, boolean maybeRefined) {
        if (!(container instanceof RubyModule)) throw typeError(context, "no outer class/module");

        RubyClass sc;
        if (superClass == UNDEFINED) {
            sc = null;
        } else {
            RubyClass.checkInheritable(context, (IRubyObject) superClass);

            sc = (RubyClass) superClass;
        }

        RubyModule newRubyClass = ((RubyModule)container).defineOrGetClassUnder(id, sc, scope.getFile(), context.getLine() + 1);

        scope.setModule(newRubyClass);

        if (maybeRefined) scope.captureParentRefinements(context);

        return newRubyClass;
    }

    @Interp
    public static void defInterpretedClassMethod(ThreadContext context, IRScope method, IRubyObject obj) {
        context.setLine(method.getLine());
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
        context.setLine(line);
        RubyClass rubyClass = checkClassForDef(context, id, obj);

        if (maybeRefined) scope.captureParentRefinements(context);

        // FIXME: needs checkID and proper encoding to force hard symbol
        rubyClass.addMethod(id,
                new CompiledIRMethod(handle, null, -1, id, line, scope, Visibility.PUBLIC, rubyClass,
                        encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer));

        if (!rubyClass.isRefinement()) {
            // FIXME: needs checkID and proper encoding to force hard symbol
            obj.callMethod(context, "singleton_method_added", asSymbol(context, id));
        }
    }

    @JIT
    public static void defCompiledClassMethod(ThreadContext context, MethodHandle variable, MethodHandle specific,
                                              int specificArity, String id, int line, StaticScope scope,
                                              String encodedArgumentDescriptors,
                                              IRubyObject obj, boolean maybeRefined, boolean receivesKeywordArgs,
                                              boolean needsToFindImplementer) {
        context.setLine(line);
        RubyClass rubyClass = checkClassForDef(context, id, obj);

        if (maybeRefined) scope.captureParentRefinements(context);

        rubyClass.addMethod(id, new CompiledIRMethod(variable, specific, specificArity, id, line, scope,
                Visibility.PUBLIC, rubyClass, encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer));

        if (!rubyClass.isRefinement()) obj.callMethod(context, "singleton_method_added", asSymbol(context, id));
    }

    private static RubyClass checkClassForDef(ThreadContext context, String id, IRubyObject obj) {
        if (obj instanceof RubyFixnum || obj instanceof RubySymbol || obj instanceof RubyFloat) {
            throw typeError(context, str(context.runtime, "can't define singleton method \"",
                    ids(context.runtime, id), "\" for ", obj.getMetaClass().rubyBaseName()));
        }

        // if (obj.isFrozen()) throw context.runtime.newFrozenError("object");

        return obj.getSingletonClass();
    }

    @Interp
    public static void defInterpretedInstanceMethod(ThreadContext context, IRScope method, DynamicScope currDynScope, IRubyObject self) {
        context.setLine(method.getLine());
        RubySymbol methodName = method.getName();
        RubyModule rubyClass = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(context.runtime, rubyClass, methodName, currVisibility);

        if (method.maybeUsingRefinements()) method.getStaticScope().captureParentRefinements(context);

        DynamicMethod newMethod = context.runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.OFF ?
            new InterpretedIRMethod(method, newVisibility, rubyClass) : new MixedModeIRMethod(method, newVisibility, rubyClass);

        Helpers.addInstanceMethod(rubyClass, methodName, newMethod, currVisibility, context);
    }

    @JIT
    public static void defCompiledInstanceMethod(ThreadContext context, MethodHandle handle, String id, int line,
                                                 StaticScope scope, String encodedArgumentDescriptors,
                                                 DynamicScope currDynScope, IRubyObject self, boolean maybeRefined,
                                                 boolean receivesKeywordArgs, boolean needsToFindImplementer) {
        context.setLine(line);
        RubySymbol methodName = asSymbol(context, id);
        RubyModule clazz = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(context.runtime, clazz, methodName, currVisibility);

        if (maybeRefined) scope.captureParentRefinements(context);

        DynamicMethod newMethod = new CompiledIRMethod(handle, null, -1, id, line, scope,
                newVisibility, clazz, encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer);

        // FIXME: needs checkID and proper encoding to force hard symbol
        Helpers.addInstanceMethod(clazz, methodName, newMethod, currVisibility, context);
    }

    @JIT
    public static void defCompiledInstanceMethod(ThreadContext context, MethodHandle variable, MethodHandle specific,
                                                 int specificArity, String id, int line, StaticScope scope,
                                                 String encodedArgumentDescriptors,
                                                 DynamicScope currDynScope, IRubyObject self, boolean maybeRefined,
                                                 boolean receivesKeywordArgs, boolean needsToFindImplementer) {
        context.setLine(line);
        RubySymbol methodName = asSymbol(context, id);
        RubyModule clazz = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(context.runtime, clazz, methodName, currVisibility);

        if (maybeRefined) scope.captureParentRefinements(context);

        DynamicMethod newMethod = new CompiledIRMethod(variable, specific, specificArity, id, line, scope,
                newVisibility, clazz, encodedArgumentDescriptors, receivesKeywordArgs, needsToFindImplementer);

        // FIXME: needs checkID and proper encoding to force hard symbol
        Helpers.addInstanceMethod(clazz, methodName, newMethod, currVisibility, context);
    }
    
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
        int callInfo = ThreadContext.resetCallInfo(context);
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck(context, ary, context.runtime.getArray(), sites(context).to_a_checked);
        RubyArray<?> result;
        if (tmp.isNil()) {
            result = newArray(context, ary);
            context.callInfo = callInfo;
        } else if (true /**RTEST(flag)**/) { // this logic is only used for bare splat, and MRI dups
            result = (RubyArray<?>) ((RubyArray)tmp).aryDup();

            // We have concat'd an empty keyword rest.   This comes from MERGE_KEYWORDS noticing it is empty.
            if (result.last() == UNDEFINED) {
                result.pop(context);
                context.callInfo |= callInfo | CALL_KEYWORD_EMPTY;
            }
        }
        return result;
    }

    /**
     * Call to_ary to get Array or die typing.  The optionally dup it if specified.  Some conditional
     * cases in compiler we know we are safe in not-duping.  This method is the same impl as MRIs
     * splatarray instr in the YARV instruction set.
     */
    @JIT @Interp
    public static RubyArray splatArray(ThreadContext context, IRubyObject ary, boolean dupArray) {
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck(context, ary, context.runtime.getArray(), sites(context).to_a_checked);

        if (tmp.isNil()) return newArray(context, ary);
        if (dupArray) return ((RubyArray<?>) tmp).aryDup();

        return (RubyArray<?>) tmp;
    }

    /**
     * Call to_ary to get Array or die typing.  The optionally dup it if specified.  Some conditional
     * cases in compiler we know we are safe in not-duping.  This method is the same impl as MRIs
     * splatarray instr in the YARV instruction set.
     */
    @JIT @Interp
    public static RubyArray splatArray(ThreadContext context, IRubyObject ary) {
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck(context, ary, context.runtime.getArray(), sites(context).to_a_checked);

        if (tmp.isNil()) return newArray(context, ary);

        return (RubyArray<?>) tmp;
    }

    /**
     * Call to_ary to get Array or die typing.  The optionally dup it if specified.  Some conditional
     * cases in compiler we know we are safe in not-duping.  This method is the same impl as MRIs
     * splatarray instr in the YARV instruction set.
     */
    @JIT @Interp
    public static RubyArray splatArrayDup(ThreadContext context, IRubyObject ary) {
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck(context, ary, context.runtime.getArray(), sites(context).to_a_checked);

        return tmp.isNil() ? newArray(context, ary) : ((RubyArray<?>) tmp).aryDup();
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
        return asBoolean(context, !(obj.isTrue()));
    }

    @JIT
    public static RaiseException newRequiredKeywordArgumentError(ThreadContext context, String id) {
        return argumentError(context, str(context.runtime, "missing keyword: ", ids(context.runtime, id)));
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
            return IRReader.load(runtime.getIRManager(), new IRReaderStream(runtime.getIRManager(), scopeBytes, filename));
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
    public static RubyFixnum getArgScopeDepth(ThreadContext context, StaticScope currScope) {
        int i = 0;
        while (!currScope.isArgumentScope()) {
            currScope = currScope.getEnclosingScope();
            i++;
        }
        return asFixnum(context, i);
    }

    public static IRubyObject[] toAry(ThreadContext context, IRubyObject[] args) {
        IRubyObject ary;
        if (args.length == 1 && (ary = Helpers.aryOrToAry(context, args[0])) != context.nil) {
            if (!(ary instanceof RubyArray)) throw typeError(context, "", args[0], "#to_ary should return Array");
            args = ((RubyArray) ary).toJavaArray(context);
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

        // FIXME: For NORMAL/THREAD but it is unclear if we really need any keyword logic in here anymore.
        org.jruby.runtime.Signature sig = block.getBody().getSignature();
        if (!sig.isSpreadable()) return args;

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
                newArgs[len - 1] = newHash(context);
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
    public static IRubyObject[] prepareBlockArgs(ThreadContext context, Block block, IRubyObject[] args, boolean usesKwArgs, boolean ruby2Keywords) {
        return prepareBlockArgsInternal(context, block, args);
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
        return IRRuntimeHelpers.newSymbolProc(context, asSymbol(context, value));
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
                ((RubyArray) value).toJavaArray(value.getRuntime().getCurrentContext()) :
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

        if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
            return RubyString.newDebugFrozenString(runtime, runtime.getString(), bytelist, coderange, file, line + 1);
        }

        return runtime.freezeAndDedupString(RubyString.newString(runtime, bytelist, coderange));
    }

    public static RubyString newFrozenString(ThreadContext context, ByteList bytelist, int coderange) {
        Ruby runtime = context.runtime;

        return runtime.freezeAndDedupString(RubyString.newString(runtime, bytelist, coderange));
    }


    public static RubyString newChilledString(ThreadContext context, ByteList bytelist, int coderange, String file, int line) {
        return RubyString.newString(context.runtime, bytelist, coderange).chill();
    }

    @JIT @Interp
    public static RubyString freezeLiteralString(RubyString string) {
        string.setFrozen(true);

        return string;
    }

    @JIT @Interp
    public static RubyString chillLiteralString(RubyString string) {
        string.chill();

        return string;
    }

    @JIT
    public static IRubyObject callOptimizedAref(ThreadContext context, IRubyObject caller, IRubyObject target, RubyString keyStr, CallSite site) {
        return target instanceof RubyHash h && !h.isComparedByIdentity() && ((CachingCallSite) site).isBuiltin(target) ?
                h.op_aref(context, keyStr) : // call directly with cached frozen string
                site.call(context, caller, target, dupString(context, keyStr));
    }

    /**
     * asString using a given call site
     */
    @JIT
    public static RubyString asString(ThreadContext context, IRubyObject caller, IRubyObject target, CallSite site) {
        IRubyObject str = site.call(context, caller, target);
        return str instanceof RubyString string ? string : (RubyString) target.anyToString();
    }

    @JIT
    public static RubyArray newArray(ThreadContext context) {
        return RubyArray.newEmptyArray(context.runtime);
    }

    @JIT
    public static RubyArray newArray(ThreadContext context, IRubyObject obj) {
        return Create.newArray(context, obj);
    }

    @JIT
    public static RubyArray newArray(ThreadContext context, IRubyObject obj0, IRubyObject obj1) {
        return Create.newArray(context, obj0, obj1);
    }

    @JIT @Interp
    public static RubyString getFileNameStringFromScope(ThreadContext context, StaticScope currScope) {
        String file = currScope.getFile();

        // FIXME: Not very efficient to do all this every time
        return newString(context, file);
    }

    @JIT
    public static void callTrace(ThreadContext context, IRubyObject selfClass, RubyEvent event, String name, String filename, int line) {
        TraceEventManager traceEvents = context.traceEvents;
        if (traceEvents.hasEventHooks()) {
            // FIXME: Try and statically generate END linenumber instead of hacking it.
            int linenumber = line == -1 ? context.getLine() : line;

            traceEvents.callEventHooks(context, event, filename, linenumber, name, selfClass);
        }
    }

    public static void traceRaise(ThreadContext context) {
        TraceEventManager traceEvents = context.traceEvents;
        if (traceEvents.hasEventHooks()) {
            RubyStackTraceElement backtraceElement = context.getSingleBacktrace();
            String file = backtraceElement.getFileName();
            int line = backtraceElement.getLineNumber();

            // FIXME: Try and statically generate END linenumber instead of hacking it.
            int linenumber = line == -1 ? context.getLine() : line;

            traceEvents.callEventHooks(context, RubyEvent.RAISE, file, linenumber, null, context.nil);
        }
    }

    public static void traceRescue(ThreadContext context, String file, int line) {
        TraceEventManager traceEvents = context.traceEvents;
        if (traceEvents.hasEventHooks()) {
            traceEvents.callEventHooks(context, RubyEvent.RESCUE, file, line, null, context.getErrorInfo());
        }
    }

    @JIT
    public static void callTrace(ThreadContext context, Block selfBlock, RubyEvent event, String name, String filename, int line) {
        TraceEventManager traceEvents = context.traceEvents;
        if (traceEvents.hasEventHooks()) {
            // FIXME: Try and statically generate END linenumber instead of hacking it.
            int linenumber = line == -1 ? context.getLine() : line;

            traceEvents.callEventHooks(context, event, filename, linenumber, name, selfBlock.getFrameClass());
        }
    }

    @JIT
    public static void callTraceHooks(ThreadContext context, Block selfBlock, RubyEvent event, String name, String filename, int line) {
        // FIXME: Try and statically generate END linenumber instead of hacking it.
        int linenumber = line == -1 ? context.getLine() : line;

        context.traceEvents.callEventHooks(context, event, filename, linenumber, name, selfBlock.getFrameClass());
    }

    public static void warnSetConstInRefinement(ThreadContext context, IRubyObject self) {
        if (self instanceof RubyModule && ((RubyModule) self).isRefinement()) {
            context.runtime.getWarnings().warn("not defined at the refinement, but at the outer class/module");
        }
    }

    @Interp
    public static void putConst(ThreadContext context, IRubyObject self, IRubyObject module, String id, IRubyObject value) {
        putConst(context, self, module, id, value, context.getFile(), context.getLine() + 1);
    }

    @JIT
    public static void putConst(ThreadContext context, IRubyObject self, IRubyObject module, String id, IRubyObject value, StaticScope scope, int line) {
        putConst(context, self, module, id, value, scope.getFile(), line);
    }

    private static void putConst(ThreadContext context, IRubyObject self, IRubyObject module, String id, IRubyObject value, String filename, int line) {
        if (!(module instanceof RubyModule)) throw typeError(context, module.inspect(context) + " is not a class/module");

        warnSetConstInRefinement(context, self);

        ((RubyModule) module).setConstant(id, value, filename, line);
    }

    @Interp @JIT
    public static IRubyObject getClassVariable(ThreadContext context, RubyModule module, String id) {
        return module.getClassVar(id);
    }

    @Interp @JIT
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
        return asSymbol(context, symbol.asString());
    }

    @JIT
    public static RubyClass getStandardError(ThreadContext context) {
        return context.runtime.getStandardError();
    }

    @JIT
    public static RubyClass getArray(ThreadContext context) {
        return context.runtime.getArray();
    }

    @JIT
    public static RubyClass getHash(ThreadContext context) {
        return context.runtime.getHash();
    }

    @JIT
    public static RubyClass getObject(ThreadContext context) {
        return context.runtime.getObject();
    }

    @JIT
    public static RubyClass getSymbol(ThreadContext context) {
        return context.runtime.getSymbol();
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

    @Interp @JIT
    public static int arrayLength(RubyArray array) {
        return array.getLength();
    }

    @Interp @JIT
    public static String getFrameNameFromBlock(Block block) {
        // FIXME: binding.getMethod does not appear to be the right name in defined_method bodies... WHY?
        return block.getBinding().getFrame().getName();
    }

    @Interp @JIT
    public static Block getFrameBlockFromBlock(Block block) {
        return block.getBinding().getFrame().getBlock();
    }
}
