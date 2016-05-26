package org.jruby.ir.runtime;

import com.headius.invokebinder.Signature;
import org.jcodings.Encoding;
import org.jruby.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.CompiledIRMetaClassBody;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRBodyMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMetaClassBody;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Interp;
import org.jruby.ir.JIT;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.callsite.NormalCachingCallSite;
import org.jruby.runtime.callsite.RefinedCachingCallSite;
import org.jruby.runtime.callsite.VariableCachingCallSite;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.util.ByteList;
import org.jruby.util.DefinedMessage;
import org.jruby.util.RegexpOptions;
import org.jruby.util.TypeConverter;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Map;

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
        return blockType == Block.Type.LAMBDA && !scope.isArgumentScope();
    }

    public static boolean inLambda(Block.Type blockType) {
        return blockType == Block.Type.LAMBDA;
    }

    public static boolean inProc(Block.Type blockType) {
        return blockType == Block.Type.PROC;
    }

    public static void checkForLJE(ThreadContext context, DynamicScope dynScope, boolean maybeLambda, Block.Type blockType) {
        if (IRRuntimeHelpers.inLambda(blockType)) return;

        StaticScope scope = dynScope.getStaticScope();
        IRScopeType scopeType = scope.getScopeType();
        boolean inDefineMethod = false;
        while (dynScope != null) {
            StaticScope ss = dynScope.getStaticScope();
            // SSS FIXME: Why is scopeType empty? Looks like this static-scope
            // was not associated with the AST scope that got converted to IR.
            //
            // Ruby code: lambda { Thread.new { return }.join }.call
            //
            // To be investigated.
            IRScopeType ssType = ss.getScopeType();
            if (ssType != null) {
                if (ssType.isMethodType()) {
                    break;
                } else if (ss.isArgumentScope() && ssType.isClosureType() && ssType != IRScopeType.EVAL_SCRIPT) {
                    inDefineMethod = true;
                    break;
                }
            }
            dynScope = dynScope.getParentScope();
        }

        // SSS FIXME: Why is scopeType empty? Looks like this static-scope
        // was not associated with the AST scope that got converted to IR.
        //
        // Ruby code: lambda { Thread.new { return }.join }.call
        //
        // To be investigated.
        if (   (scopeType == null || (!inDefineMethod && scopeType.isClosureType() && scopeType != IRScopeType.EVAL_SCRIPT))
            && (maybeLambda || !context.scopeExistsOnCallStack(dynScope)))
        {
            // Cannot return from the call that we have long since exited.
            throw IRException.RETURN_LocalJumpError.getException(context.runtime);
        }
    }

    /*
     * Handle non-local returns (ex: when nested in closures, root scopes of module/class/sclass bodies)
     */
    public static IRubyObject initiateNonLocalReturn(ThreadContext context, DynamicScope dynScope, Block.Type blockType, IRubyObject returnValue) {
        if (IRRuntimeHelpers.inLambda(blockType)) throw new IRWrappedLambdaReturnValue(returnValue);

        // If not in a lambda, check if this was a non-local return
        while (dynScope != null) {
            StaticScope ss = dynScope.getStaticScope();
            // SSS FIXME: Why is scopeType empty? Looks like this static-scope
            // was not associated with the AST scope that got converted to IR.
            //
            // Ruby code: lambda { Thread.new { return }.join }.call
            //
            // To be investigated.
            IRScopeType ssType = ss.getScopeType();
            if (ssType != null) {
                if (ssType.isMethodType() ||
                        (ss.isArgumentScope() && ssType.isClosureType() && ssType != IRScopeType.EVAL_SCRIPT) ||
                        (ssType.isClosureType() && dynScope.isLambda())) {
                    break;
                }
            }
            dynScope = dynScope.getParentScope();
        }

        // methodtoReturnFrom will not be -1 for explicit returns from class/module/sclass bodies
        throw IRReturnJump.create(dynScope, returnValue);
    }

    @JIT
    public static IRubyObject handleNonlocalReturn(StaticScope scope, DynamicScope dynScope, Object rjExc, Block.Type blockType) throws RuntimeException {
        if (!(rjExc instanceof IRReturnJump)) {
            Helpers.throwException((Throwable)rjExc);
            return null; // Unreachable
        } else {
            IRReturnJump rj = (IRReturnJump)rjExc;

            // If we are in the method scope we are supposed to return from, stop propagating.
            if (rj.methodToReturnFrom == dynScope) {
                if (isDebug()) System.out.println("---> Non-local Return reached target in scope: " + dynScope + " matching dynscope? " + (rj.methodToReturnFrom == dynScope));
                return (IRubyObject) rj.returnValue;
            }

            // If not, Just pass it along!
            throw rj;
        }
    }

    public static IRubyObject initiateBreak(ThreadContext context, DynamicScope dynScope, IRubyObject breakValue, Block.Type blockType) throws RuntimeException {
        if (inLambda(blockType)) {
            // Wrap the return value in an exception object
            // and push it through the break exception paths so
            // that ensures are run, frames/scopes are popped
            // from runtime stacks, etc.
            throw new IRWrappedLambdaReturnValue(breakValue);
        } else {
            StaticScope scope = dynScope.getStaticScope();
            IRScopeType scopeType = scope.getScopeType();
            if (!scopeType.isClosureType()) {
                // Error -- breaks can only be initiated in closures
                throw IRException.BREAK_LocalJumpError.getException(context.runtime);
            }

            IRBreakJump bj = IRBreakJump.create(dynScope.getParentScope(), breakValue);
            if (scopeType == IRScopeType.EVAL_SCRIPT) {
                // If we are in an eval, record it so we can account for it
                bj.breakInEval = true;
            }

            // Start the process of breaking through the intermediate scopes
            throw bj;
        }
    }

    @JIT
    public static IRubyObject handleBreakAndReturnsInLambdas(ThreadContext context, StaticScope scope, DynamicScope dynScope, Object exc, Block.Type blockType) throws RuntimeException {
        if (exc instanceof IRWrappedLambdaReturnValue) {
            // Wrap the return value in an exception object
            // and push it through the nonlocal return exception paths so
            // that ensures are run, frames/scopes are popped
            // from runtime stacks, etc.
            return ((IRWrappedLambdaReturnValue)exc).returnValue;
        } else if ((exc instanceof IRBreakJump) && inNonMethodBodyLambda(scope, blockType)) {
            // We just unwound all the way up because of a non-local break
            context.setSavedExceptionInLambda(IRException.BREAK_LocalJumpError.getException(context.runtime));
            return null;
        } else if (exc instanceof IRReturnJump && (blockType == null || inLambda(blockType))) {
            try {
                // Ignore non-local return processing in non-lambda blocks.
                // Methods have a null blocktype
                return handleNonlocalReturn(scope, dynScope, exc, blockType);
            } catch (Throwable e) {
                context.setSavedExceptionInLambda(e);
                return null;
            }
        } else {
            // Propagate the exception
            context.setSavedExceptionInLambda((Throwable)exc);
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
    public static IRubyObject handlePropagatedBreak(ThreadContext context, DynamicScope dynScope, Object bjExc, Block.Type blockType) {
        if (!(bjExc instanceof IRBreakJump)) {
            Helpers.throwException((Throwable)bjExc);
            return null; // Unreachable
        }

        IRBreakJump bj = (IRBreakJump)bjExc;
        if (bj.breakInEval) {
            // If the break was in an eval, we pretend as if it was in the containing scope
            StaticScope scope = dynScope.getStaticScope();
            IRScopeType scopeType = scope.getScopeType();
            if (!scopeType.isClosureType()) {
                // Error -- breaks can only be initiated in closures
                throw IRException.BREAK_LocalJumpError.getException(context.runtime);
            } else {
                bj.breakInEval = false;
                throw bj;
            }
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

    @JIT
    public static void defCompiledIRMethod(ThreadContext context, MethodHandle handle, String rubyName, DynamicScope currDynScope, IRubyObject self, IRScope irScope) {
        Ruby runtime = context.runtime;

        RubyModule containingClass = IRRuntimeHelpers.findInstanceMethodContainer(context, currDynScope, self);
        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, containingClass, rubyName, currVisibility);

        DynamicMethod method = new CompiledIRMethod(handle, irScope, newVisibility, containingClass, irScope.receivesKeywordArgs());

        Helpers.addInstanceMethod(containingClass, rubyName, method, currVisibility, context, runtime);
    }

    @JIT
    public static void defCompiledIRClassMethod(ThreadContext context, IRubyObject obj, MethodHandle handle, String rubyName, IRScope irScope) {
        Ruby runtime = context.runtime;

        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton method \"" + rubyName + "\" for " + obj.getMetaClass().getBaseName());
        }

        if (obj.isFrozen()) throw runtime.newFrozenError("object");

        RubyClass containingClass = obj.getSingletonClass();

        DynamicMethod method = new CompiledIRMethod(handle, irScope, Visibility.PUBLIC, containingClass, irScope.receivesKeywordArgs());

        containingClass.addMethod(rubyName, method);

        obj.callMethod(context, "singleton_method_added", runtime.fastNewSymbol(rubyName));
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

    public static double unboxFloat(IRubyObject val) {
        if (val instanceof RubyFloat) {
            return ((RubyFloat)val).getValue();
        } else {
            return ((RubyFixnum)val).getDoubleValue();
        }
    }

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
        return (excObj instanceof RaiseException) ? ((RaiseException)excObj).getException() : excObj;
    }

    private static boolean isJavaExceptionHandled(ThreadContext context, IRubyObject excType, Object excObj, boolean arrayCheck) {
        if (!(excObj instanceof Throwable)) {
            return false;
        }

        Ruby runtime = context.runtime;
        Throwable throwable = (Throwable)excObj;

        if (excType instanceof RubyArray) {
            RubyArray testTypes = (RubyArray)excType;
            for (int i = 0, n = testTypes.getLength(); i < n; i++) {
                IRubyObject testType = testTypes.eltInternal(i);
                if (IRRuntimeHelpers.isJavaExceptionHandled(context, testType, throwable, true)) {
                    IRubyObject exceptionObj;
                    if (n == 1 && testType == runtime.getNativeException()) {
                        // wrap Throwable in a NativeException object
                        exceptionObj = new NativeException(runtime, runtime.getNativeException(), throwable);
                        ((NativeException)exceptionObj).prepareIntegratedBacktrace(context, throwable.getStackTrace());
                    } else {
                        // wrap as normal JI object
                        exceptionObj = JavaUtil.convertJavaToUsableRubyObject(runtime, throwable);
                    }

                    runtime.getGlobalVariables().set("$!", exceptionObj);
                    return true;
                }
            }
        } else if (Helpers.checkJavaException(throwable, excType, context)) {
            IRubyObject exceptionObj;
            if (excType == runtime.getNativeException()) {
                // wrap Throwable in a NativeException object
                exceptionObj = new NativeException(runtime, runtime.getNativeException(), throwable);
                ((NativeException)exceptionObj).prepareIntegratedBacktrace(context, throwable.getStackTrace());
            } else {
                // wrap as normal JI object
                exceptionObj = JavaUtil.convertJavaToUsableRubyObject(runtime, throwable);
            }

            runtime.getGlobalVariables().set("$!", exceptionObj);
            return true;
        }

        return false;
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

        return context.runtime.newBoolean(ret);
    }

    public static IRubyObject isEQQ(ThreadContext context, IRubyObject receiver, IRubyObject value, CallSite callSite) {
        boolean isUndefValue = value == UndefinedValue.UNDEFINED;
        if (receiver instanceof RubyArray) {
            RubyArray testVals = (RubyArray)receiver;
            for (int i = 0, n = testVals.getLength(); i < n; i++) {
                IRubyObject v = testVals.eltInternal(i);
                IRubyObject eqqVal = isUndefValue ? v : callSite.call(context, v, v, value);
                if (eqqVal.isTrue()) return eqqVal;
            }
            return context.runtime.newBoolean(false);
        } else {
            return isUndefValue ? receiver : callSite.call(context, receiver, receiver, value);
        }
    }

    public static IRubyObject newProc(Ruby runtime, Block block) {
        return (block == Block.NULL_BLOCK) ? runtime.getNil() : runtime.newProc(Block.Type.PROC, block);
    }

    public static IRubyObject yield(ThreadContext context, Block b, IRubyObject yieldArg, boolean unwrapArray) {
        IRubyObject yieldVal = (IRubyObject)yieldArg;
        return (unwrapArray && (yieldVal instanceof RubyArray)) ? b.yieldArray(context, yieldVal, null) : b.yield(context, yieldVal);
    }

    public static IRubyObject yieldSpecific(ThreadContext context, Block b) {
        return b.yieldSpecific(context);
    }

    public static IRubyObject[] convertValueIntoArgArray(ThreadContext context, IRubyObject value, int blockArity, boolean argIsArray) {
        // SSS FIXME: This should not really happen -- so, some places in the runtime library are breaking this contract.
        if (argIsArray && !(value instanceof RubyArray)) argIsArray = false;

        switch (blockArity) {
            case -1 : return argIsArray ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
            case  0 : return new IRubyObject[] { value };
            case  1 : {
               if (argIsArray) {
                   RubyArray valArray = ((RubyArray)value);
                   if (valArray.size() == 0) {
                       value = RubyArray.newEmptyArray(context.runtime);
                   }
               }
               return new IRubyObject[] { value };
            }
            default :
                if (argIsArray) {
                    RubyArray valArray = (RubyArray)value;
                    if (valArray.size() == 1) value = valArray.eltInternal(0);
                    value = Helpers.aryToAry(value);
                    return (value instanceof RubyArray) ? ((RubyArray)value).toJavaArray() : new IRubyObject[] { value };
                } else {
                    IRubyObject val0 = Helpers.aryToAry(value);
                    // FIXME: This logic exists in RubyProc and IRRubyBlockBody. consolidate when we do block call protocol work
                    if (val0.isNil()) {
                        return new IRubyObject[] { value };
                    } else if (!(val0 instanceof RubyArray)) {
                        throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
                    } else {
                        return ((RubyArray) val0).toJavaArray();
                    }
                }
        }
    }

    @JIT
    public static Block getBlockFromObject(ThreadContext context, Object value) {
        Block block;
        if (value instanceof Block) {
            block = (Block) value;
        } else if (value instanceof RubyProc) {
            block = ((RubyProc) value).getBlock();
        } else if (value instanceof RubyMethod) {
            block = ((RubyProc)((RubyMethod)value).to_proc(context)).getBlock();
        } else if ((value instanceof IRubyObject) && ((IRubyObject)value).isNil()) {
            block = Block.NULL_BLOCK;
        } else if (value instanceof IRubyObject) {
            block = ((RubyProc) TypeConverter.convertToType((IRubyObject) value, context.runtime.getProc(), "to_proc", true)).getBlock();
        } else {
            throw new RuntimeException("Unhandled case in CallInstr:prepareBlock.  Got block arg: " + value);
        }
        return block;
    }

    public static void checkArity(Ruby runtime, StaticScope scope, Object[] args, int required, int opt, boolean rest,
                                  boolean receivesKwargs, int restKey, Block.Type blockType) {
        int argsLength = args.length;
        RubyHash keywordArgs = extractKwargsHash(args, required, receivesKwargs);

        if (restKey == -1 && keywordArgs != null) checkForExtraUnwantedKeywordArgs(runtime, scope, keywordArgs);

        // keyword arguments value is not used for arity checking.
        if (keywordArgs != null) argsLength -= 1;

        if ((blockType == null || blockType.checkArity) && (argsLength < required || (!rest && argsLength > (required + opt)))) {
//            System.out.println("NUMARGS: " + argsLength + ", REQUIRED: " + required + ", OPT: " + opt + ", AL: " + args.length + ",RKW: " + receivesKwargs );
//            System.out.println("ARGS[0]: " + args[0]);

            Arity.raiseArgumentError(runtime, argsLength, required, required + opt);
        }
    }

    // Due to our current strategy of destructively processing the kwargs hash we need to dup
    // and make sure the copy is not frozen.  This has a poor name as encouragement to rewrite
    // how we handle kwargs internally :)
    public static void frobnicateKwargsArgument(ThreadContext context, IRubyObject[] args, int requiredArgsCount) {
        if (args.length <= requiredArgsCount) return; // No kwarg because required args slurp them up.

        RubyHash kwargs = toHash(args[args.length - 1], context);

        if (kwargs != null) {
            kwargs = (RubyHash) kwargs.dup(context);
            kwargs.setFrozen(false);
            args[args.length - 1] = kwargs;
        }
    }

    private static RubyHash toHash(IRubyObject lastArg, ThreadContext context) {
        if (lastArg instanceof RubyHash) return (RubyHash) lastArg;
        if (lastArg.respondsTo("to_hash")) {
            if ( context == null ) context = lastArg.getRuntime().getCurrentContext();
            lastArg = lastArg.callMethod(context, "to_hash");
            if (lastArg instanceof RubyHash) return (RubyHash) lastArg;
        }
        return null;
    }

    public static RubyHash extractKwargsHash(Object[] args, int requiredArgsCount, boolean receivesKwargs) {
        if (!receivesKwargs) return null;
        if (args.length <= requiredArgsCount) return null; // No kwarg because required args slurp them up.

        Object lastArg = args[args.length - 1];

        if (lastArg instanceof IRubyObject) return toHash((IRubyObject) lastArg, null);
        return null;
    }

    public static void checkForExtraUnwantedKeywordArgs(final Ruby runtime, final StaticScope scope, RubyHash keywordArgs) {
        keywordArgs.visitAll(new RubyHash.Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                String keyAsString = key.asJavaString();
                int slot = scope.isDefined((keyAsString));

                // Found name in higher variable scope.  Therefore non for this block/method def.
                if ((slot >> 16) > 0) throw runtime.newArgumentError("unknown keyword: " + keyAsString);
                // Could not find it anywhere.
                if (((short) (slot & 0xffff)) < 0) throw runtime.newArgumentError("unknown keyword: " + keyAsString);
            }
        });
    }

    public static IRubyObject match3(ThreadContext context, RubyRegexp regexp, IRubyObject argValue) {
        if (argValue instanceof RubyString) {
            return regexp.op_match19(context, argValue);
        } else {
            return argValue.callMethod(context, "=~", regexp);
        }
    }

    public static IRubyObject extractOptionalArgument(RubyArray rubyArray, int minArgsLength, int index) {
        int n = rubyArray.getLength();
        return minArgsLength < n ? rubyArray.entry(index) : UndefinedValue.UNDEFINED;
    }

    @JIT
    public static IRubyObject isDefinedBackref(ThreadContext context) {
        return RubyMatchData.class.isInstance(context.getBackRef()) ?
                context.runtime.getDefinedMessage(DefinedMessage.GLOBAL_VARIABLE) : context.nil;
    }

    @JIT
    public static IRubyObject isDefinedGlobal(ThreadContext context, String name) {
        return context.runtime.getGlobalVariables().isDefined(name) ?
                context.runtime.getDefinedMessage(DefinedMessage.GLOBAL_VARIABLE) : context.nil;
    }

    // FIXME: This checks for match data differently than isDefinedBackref.  Seems like they should use same mechanism?
    @JIT
    public static IRubyObject isDefinedNthRef(ThreadContext context, int matchNumber) {
        IRubyObject backref = context.getBackRef();

        if (backref instanceof RubyMatchData) {
            if (!((RubyMatchData) backref).group(matchNumber).isNil()) {
                return context.runtime.getDefinedMessage(DefinedMessage.GLOBAL_VARIABLE);
            }
        }

        return context.nil;
    }

    @JIT
    public static IRubyObject isDefinedClassVar(ThreadContext context, RubyModule receiver, String name) {
        boolean defined = receiver.isClassVarDefined(name);

        if (!defined && receiver.isSingleton()) { // Look for class var in singleton if it is one.
            IRubyObject attached = ((MetaClass) receiver).getAttached();

            if (attached instanceof RubyModule) defined = ((RubyModule) attached).isClassVarDefined(name);
        }

        return defined ? context.runtime.getDefinedMessage(DefinedMessage.CLASS_VARIABLE) : context.nil;
    }

    @JIT
    public static IRubyObject isDefinedInstanceVar(ThreadContext context, IRubyObject receiver, String name) {
        return receiver.getInstanceVariables().hasInstanceVariable(name) ?
                context.runtime.getDefinedMessage(DefinedMessage.INSTANCE_VARIABLE) : context.nil;
    }

    @JIT
    public static IRubyObject isDefinedCall(ThreadContext context, IRubyObject self, IRubyObject receiver, String name) {
        RubyString boundValue = Helpers.getDefinedCall(context, self, receiver, name);

        return boundValue == null ? context.nil : boundValue;
    }

    @JIT
    public static IRubyObject isDefinedConstantOrMethod(ThreadContext context, IRubyObject receiver, String name) {
        RubyString definedType = Helpers.getDefinedConstantOrBoundMethod(receiver, name);

        return definedType == null ? context.nil : definedType;
    }

    @JIT
    public static IRubyObject isDefinedMethod(ThreadContext context, IRubyObject receiver, String name, boolean checkIfPublic) {
        DynamicMethod method = receiver.getMetaClass().searchMethod(name);

        boolean defined = !method.isUndefined();

        if (defined) {
            // If we find the method we optionally check if it is public before returning "method".
            defined = !checkIfPublic || method.getVisibility() == Visibility.PUBLIC;
        } else {
            // If we did not find the method, check respond_to_missing?
            defined = receiver.respondsToMissing(name, checkIfPublic);
        }

        if (defined) return context.runtime.getDefinedMessage(DefinedMessage.METHOD);

        return context.nil;
    }

    @JIT
    public static IRubyObject isDefinedSuper(ThreadContext context, IRubyObject receiver) {
        boolean flag = false;
        String frameName = context.getFrameName();

        if (frameName != null) {
            RubyModule frameClass = context.getFrameKlazz();
            if (frameClass != null) {
                flag = Helpers.findImplementerIfNecessary(receiver.getMetaClass(), frameClass).getSuperClass().isMethodBound(frameName, false);
            }
        }
        return flag ? context.runtime.getDefinedMessage(DefinedMessage.SUPER) : context.nil;
    }

    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String methodName) {
        // FIXME: super/zsuper in top-level script still seems to have a frameClass so it will not make it into this if
        if (frameClass == null) {
            if (methodName == null || !methodName.equals("")) {
                throw context.runtime.newNameError("superclass method '" + methodName + "' disabled", methodName);
            } else {
                throw context.runtime.newNoMethodError("super called outside of method", null, context.nil);
            }
        }
    }

    public static IRubyObject nthMatch(ThreadContext context, int matchNumber) {
        return RubyRegexp.nth_match(matchNumber, context.getBackRef());
    }

    public static void defineAlias(ThreadContext context, IRubyObject self, DynamicScope currDynScope, String newNameString, String oldNameString) {
        if (self == null || self instanceof RubyFixnum || self instanceof RubySymbol) {
            throw context.runtime.newTypeError("no class to make alias");
        }

        RubyModule module = findInstanceMethodContainer(context, currDynScope, self);
        module.defineAlias(newNameString, oldNameString);
        module.callMethod(context, "method_added", context.runtime.newSymbol(newNameString));
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

    @JIT
    public static IRubyObject mergeKeywordArguments(ThreadContext context, IRubyObject restKwarg, IRubyObject explcitKwarg) {
        return ((RubyHash) TypeConverter.checkHashType(context.runtime, restKwarg)).merge(context, explcitKwarg, Block.NULL_BLOCK);
    }

    @JIT
    public static IRubyObject restoreExceptionVar(ThreadContext context, IRubyObject exc, IRubyObject savedExc) {
        if (exc instanceof IRReturnJump || exc instanceof IRBreakJump) {
            context.runtime.getGlobalVariables().set("$!", savedExc);
        }
        return null;
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
                                return (RubyModule) self;

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
        return context.runtime.newBoolean( ((Block) blk).isGiven() );
    }

    public static IRubyObject receiveRestArg(ThreadContext context, Object[] args, int required, int argIndex, boolean acceptsKeywordArguments) {
        RubyHash keywordArguments = extractKwargsHash(args, required, acceptsKeywordArguments);
        return constructRestArg(context, args, keywordArguments, required, argIndex);
    }

    public static IRubyObject receiveRestArg(ThreadContext context, IRubyObject[] args, int required, int argIndex, boolean acceptsKeywordArguments) {
        RubyHash keywordArguments = extractKwargsHash(args, required, acceptsKeywordArguments);
        return constructRestArg(context, args, keywordArguments, required, argIndex);
    }

    public static IRubyObject constructRestArg(ThreadContext context, Object[] args, RubyHash keywordArguments, int required, int argIndex) {
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;
        int remainingArguments = argsLength - required;

        if (remainingArguments <= 0) return context.runtime.newEmptyArray();

        IRubyObject[] restArgs = new IRubyObject[remainingArguments];
        System.arraycopy(args, argIndex, restArgs, 0, remainingArguments);

        return RubyArray.newArrayNoCopy(context.runtime, restArgs);
    }

    private static IRubyObject constructRestArg(ThreadContext context, IRubyObject[] args, RubyHash keywordArguments, int required, int argIndex) {
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;
        if ( required == 0 && argsLength == args.length ) {
            return RubyArray.newArray(context.runtime, args);
        }
        int remainingArguments = argsLength - required;

        if (remainingArguments <= 0) return context.runtime.newEmptyArray();

        IRubyObject[] restArgs = new IRubyObject[remainingArguments];
        System.arraycopy(args, argIndex, restArgs, 0, remainingArguments);

        return RubyArray.newArrayNoCopy(context.runtime, restArgs);
    }

    @JIT
    public static IRubyObject receivePostReqdArg(ThreadContext context, IRubyObject[] args, int preReqdArgsCount, int postReqdArgsCount, int argIndex, boolean acceptsKeywordArgument) {
        boolean kwargs = extractKwargsHash(args, preReqdArgsCount + postReqdArgsCount, acceptsKeywordArgument) != null;
        int n = kwargs ? args.length - 1 : args.length;
        int remaining = n - preReqdArgsCount;
        if (remaining <= argIndex) return context.nil;

        return (remaining > postReqdArgsCount) ? args[n - postReqdArgsCount + argIndex] : args[preReqdArgsCount + argIndex];
    }

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

    public static IRubyObject receiveKeywordArg(ThreadContext context, IRubyObject[] args, int required, String argName, boolean acceptsKeywordArgument) {
        RubyHash keywordArguments = extractKwargsHash(args, required, acceptsKeywordArgument);

        if (keywordArguments == null) return UndefinedValue.UNDEFINED;

        RubySymbol keywordName = context.runtime.newSymbol(argName);

        if (keywordArguments.fastARef(keywordName) == null) return UndefinedValue.UNDEFINED;

        // SSS FIXME: Can we use an internal delete here?
        // Enebo FIXME: Delete seems wrong if we are doing this for duplication purposes.
        return keywordArguments.delete(context, keywordName, Block.NULL_BLOCK);
    }

    public static IRubyObject receiveKeywordRestArg(ThreadContext context, IRubyObject[] args, int required, boolean keywordArgumentSupplied) {
        RubyHash keywordArguments = extractKwargsHash(args, required, keywordArgumentSupplied);

        return keywordArguments == null ? RubyHash.newSmallHash(context.runtime) : keywordArguments;
    }

    public static IRubyObject setCapturedVar(ThreadContext context, IRubyObject matchRes, String varName) {
        IRubyObject val;
        if (matchRes.isNil()) {
            val = context.nil;
        } else {
            IRubyObject backref = context.getBackRef();
            int n = ((RubyMatchData)backref).getNameToBackrefNumber(varName);
            val = RubyRegexp.nth_match(n, backref);
        }

        return val;
    }

    @JIT // for JVM6
    public static IRubyObject instanceSuperSplatArgs(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block, boolean[] splatMap) {
        return instanceSuper(context, self, methodName, definingModule, splatArguments(args, splatMap), block);
    }

    @Interp
    public static IRubyObject instanceSuper(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block) {
        RubyClass superClass = definingModule.getMethodLocation().getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        IRubyObject rVal = method.isUndefined() ?
                Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                : method.call(context, self, superClass, methodName, args, block);
        return rVal;
    }

    @JIT // for JVM6
    public static IRubyObject classSuperSplatArgs(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block, boolean[] splatMap) {
        return classSuper(context, self, methodName, definingModule, splatArguments(args, splatMap), block);
    }

    @Interp
    public static IRubyObject classSuper(ThreadContext context, IRubyObject self, String methodName, RubyModule definingModule, IRubyObject[] args, Block block) {
        RubyClass superClass = definingModule.getMetaClass().getMethodLocation().getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        IRubyObject rVal = method.isUndefined() ?
            Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                : method.call(context, self, superClass, methodName, args, block);
        return rVal;
    }

    public static IRubyObject unresolvedSuperSplatArgs(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, boolean[] splatMap) {
        return unresolvedSuper(context, self, splatArguments(args, splatMap), block);
    }

    public static IRubyObject unresolvedSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {

        // We have to rely on the frame stack to find the implementation class
        RubyModule klazz = context.getFrameKlazz();
        String methodName = context.getCurrentFrame().getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyModule implMod = Helpers.findImplementerIfNecessary(self.getMetaClass(), klazz);
        RubyClass superClass = implMod.getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;

        IRubyObject rVal;
        if (method.isUndefined()) {
            rVal = Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block);
        } else {
            rVal = method.call(context, self, superClass, methodName, args, block);
        }

        return rVal;
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
                count += splatMap[i] ? ((RubyArray)args[i]).size() : 1;
            }

            IRubyObject[] newArgs = new IRubyObject[count];
            int actualOffset = 0;
            for (int i = 0; i < splatMap.length; i++) {
                if (splatMap[i]) {
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
        return new ByteList(str.getBytes(RubyEncoding.ISO), runtime.getEncodingService().getEncodingFromString(encoding), false);
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

        RubyHash hash = useSmallHash ? RubyHash.newHash(runtime) : RubyHash.newSmallHash(runtime);
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
            hash.fastASet(runtime, pairs[i++], pairs[i++], true);
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
        RubyClass singletonClass = newMetaClassFromIR(runtime, metaClassBody, obj);

        return new InterpretedIRMetaClassBody(metaClassBody, singletonClass);
    }

    /**
     * Construct a new DynamicMethod to wrap the given IRModuleBody and singletonizable object. Used by JIT.
     */
    @JIT
    public static DynamicMethod newCompiledMetaClass(ThreadContext context, MethodHandle handle, IRScope metaClassBody, IRubyObject obj) {
        RubyClass singletonClass = newMetaClassFromIR(context.runtime, metaClassBody, obj);

        return new CompiledIRMetaClassBody(handle, metaClassBody, singletonClass);
    }

    private static RubyClass newMetaClassFromIR(Ruby runtime, IRScope metaClassBody, IRubyObject obj) {
        RubyClass singletonClass = Helpers.getSingletonClass(runtime, obj);

        StaticScope metaClassScope = metaClassBody.getStaticScope();

        metaClassScope.setModule(singletonClass);
        return singletonClass;
    }

    /**
     * Construct a new DynamicMethod to wrap the given IRModuleBody and singletonizable object. Used by interpreter.
     */
    @Interp
    public static DynamicMethod newInterpretedModuleBody(ThreadContext context, IRScope irModule, Object rubyContainer) {
        RubyModule newRubyModule = newRubyModuleFromIR(context, irModule, rubyContainer);
        return new InterpretedIRBodyMethod(irModule, newRubyModule);
    }

    @JIT
    public static DynamicMethod newCompiledModuleBody(ThreadContext context, MethodHandle handle, IRScope irModule, Object rubyContainer) {
        RubyModule newRubyModule = newRubyModuleFromIR(context, irModule, rubyContainer);
        return new CompiledIRMethod(handle, irModule, Visibility.PUBLIC, newRubyModule, false);
    }

    private static RubyModule newRubyModuleFromIR(ThreadContext context, IRScope irModule, Object rubyContainer) {
        if (!(rubyContainer instanceof RubyModule)) {
            throw context.runtime.newTypeError("no outer class/module");
        }

        RubyModule newRubyModule = ((RubyModule) rubyContainer).defineOrGetModuleUnder(irModule.getName());
        irModule.getStaticScope().setModule(newRubyModule);
        return newRubyModule;
    }

    @Interp
    public static DynamicMethod newInterpretedClassBody(ThreadContext context, IRScope irClassBody, Object container, Object superClass) {
        RubyModule newRubyClass = newRubyClassFromIR(context.runtime, irClassBody, superClass, container);

        return new InterpretedIRBodyMethod(irClassBody, newRubyClass);
    }

    @JIT
    public static DynamicMethod newCompiledClassBody(ThreadContext context, MethodHandle handle, IRScope irClassBody, Object container, Object superClass) {
        RubyModule newRubyClass = newRubyClassFromIR(context.runtime, irClassBody, superClass, container);

        return new CompiledIRMethod(handle, irClassBody, Visibility.PUBLIC, newRubyClass, false);
    }

    public static RubyModule newRubyClassFromIR(Ruby runtime, IRScope irClassBody, Object superClass, Object container) {
        if (!(container instanceof RubyModule)) {
            throw runtime.newTypeError("no outer class/module");
        }

        RubyModule newRubyClass;

        if (irClassBody instanceof IRMetaClassBody) {
            newRubyClass = ((RubyModule)container).getMetaClass();
        } else {
            RubyClass sc;
            if (superClass == UndefinedValue.UNDEFINED) {
                sc = null;
            } else {
                RubyClass.checkInheritable((IRubyObject) superClass);

                sc = (RubyClass) superClass;
            }

            newRubyClass = ((RubyModule)container).defineOrGetClassUnder(irClassBody.getName(), sc);
        }

        irClassBody.getStaticScope().setModule(newRubyClass);
        return newRubyClass;
    }

    @Interp
    public static void defInterpretedClassMethod(ThreadContext context, IRScope method, IRubyObject obj) {
        RubyClass rubyClass = checkClassForDef(context, method, obj);

        DynamicMethod newMethod;
        if (context.runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.OFF) {
            newMethod = new InterpretedIRMethod(method, Visibility.PUBLIC, rubyClass);
        } else {
            newMethod = new MixedModeIRMethod(method, Visibility.PUBLIC, rubyClass);
        }
        // FIXME: needs checkID and proper encoding to force hard symbol
        rubyClass.addMethod(method.getName(), newMethod);
        if (!rubyClass.isRefinement()) {
            obj.callMethod(context, "singleton_method_added", context.runtime.fastNewSymbol(method.getName()));
        }
    }

    @JIT
    public static void defCompiledClassMethod(ThreadContext context, MethodHandle handle, IRScope method, IRubyObject obj) {
        RubyClass rubyClass = checkClassForDef(context, method, obj);

        // FIXME: needs checkID and proper encoding to force hard symbol
        rubyClass.addMethod(method.getName(), new CompiledIRMethod(handle, method, Visibility.PUBLIC, rubyClass, method.receivesKeywordArgs()));
        if (!rubyClass.isRefinement()) {
            // FIXME: needs checkID and proper encoding to force hard symbol
            obj.callMethod(context, "singleton_method_added", context.runtime.fastNewSymbol(method.getName()));
        }
    }

    @JIT
    public static void defCompiledClassMethod(ThreadContext context, MethodHandle variable, MethodHandle specific, int specificArity, IRScope method, IRubyObject obj) {
        RubyClass rubyClass = checkClassForDef(context, method, obj);

        // FIXME: needs checkID and proper encoding to force hard symbol
        rubyClass.addMethod(method.getName(), new CompiledIRMethod(variable, specific, specificArity, method, Visibility.PUBLIC, rubyClass, method.receivesKeywordArgs()));
        if (!rubyClass.isRefinement()) {
            // FIXME: needs checkID and proper encoding to force hard symbol
            obj.callMethod(context, "singleton_method_added", context.runtime.fastNewSymbol(method.getName()));
        }
    }

    private static RubyClass checkClassForDef(ThreadContext context, IRScope method, IRubyObject obj) {
        if (obj instanceof RubyFixnum || obj instanceof RubySymbol || obj instanceof RubyFloat) {
            throw context.runtime.newTypeError("can't define singleton method \"" + method.getName() + "\" for " + obj.getMetaClass().getBaseName());
        }

        // if (obj.isFrozen()) throw context.runtime.newFrozenError("object");

        return obj.getSingletonClass();
    }

    @Interp
    public static void defInterpretedInstanceMethod(ThreadContext context, IRScope method, DynamicScope currDynScope, IRubyObject self) {
        Ruby runtime = context.runtime;
        RubyModule rubyClass = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, rubyClass, method.getName(), currVisibility);

        DynamicMethod newMethod;
        if (context.runtime.getInstanceConfig().getCompileMode() == RubyInstanceConfig.CompileMode.OFF) {
            newMethod = new InterpretedIRMethod(method, newVisibility, rubyClass);
        } else {
            newMethod = new MixedModeIRMethod(method, newVisibility, rubyClass);
        }

        // FIXME: needs checkID and proper encoding to force hard symbol
        Helpers.addInstanceMethod(rubyClass, method.getName(), newMethod, currVisibility, context, runtime);
    }

    @JIT
    public static void defCompiledInstanceMethod(ThreadContext context, MethodHandle handle, IRScope method, DynamicScope currDynScope, IRubyObject self) {
        Ruby runtime = context.runtime;
        RubyModule clazz = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, clazz, method.getName(), currVisibility);

        DynamicMethod newMethod = new CompiledIRMethod(handle, method, newVisibility, clazz, method.receivesKeywordArgs());

        // FIXME: needs checkID and proper encoding to force hard symbol
        Helpers.addInstanceMethod(clazz, method.getName(), newMethod, currVisibility, context, runtime);
    }

    @JIT
    public static void defCompiledInstanceMethod(ThreadContext context, MethodHandle variable, MethodHandle specific, int specificArity, IRScope method, DynamicScope currDynScope, IRubyObject self) {
        Ruby runtime = context.runtime;
        RubyModule clazz = findInstanceMethodContainer(context, currDynScope, self);

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, clazz, method.getName(), currVisibility);

        DynamicMethod newMethod = new CompiledIRMethod(variable, specific, specificArity, method, newVisibility, clazz, method.receivesKeywordArgs());

        // FIXME: needs checkID and proper encoding to force hard symbol
        Helpers.addInstanceMethod(clazz, method.getName(), newMethod, currVisibility, context, runtime);
    }

    @JIT
    public static IRubyObject invokeModuleBody(ThreadContext context, DynamicMethod method, Block block) {
        RubyModule implClass = method.getImplementationClass();

        return method.call(context, implClass, implClass, "", block);
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject[] pieces, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, pieces, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, arg0, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, arg0, arg1, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, arg0, arg1, arg2, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, arg0, arg1, arg2, arg3, options);
        RubyRegexp re = RubyRegexp.newDRegexp(context.runtime, pattern, options);
        re.setLiteral();

        return re;
    }

    @JIT
    public static RubyRegexp newDynamicRegexp(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, int embeddedOptions) {
        RegexpOptions options = RegexpOptions.fromEmbeddedOptions(embeddedOptions);
        RubyString pattern = RubyRegexp.preprocessDRegexp(context.runtime, arg0, arg1, arg2, arg3, arg4, options);
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
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck19(ary, runtime.getArray(), "to_a");
        if (tmp.isNil()) {
            tmp = runtime.newArray(ary);
        }
        else if (true /**RTEST(flag)**/) { // this logic is only used for bare splat, and MRI dups
            tmp = ((RubyArray)tmp).aryDup();
        }
        return (RubyArray)tmp;
    }

    public static IRubyObject irToAry(ThreadContext context, IRubyObject value) {
        if (!(value instanceof RubyArray)) {
            value = RubyArray.aryToAry(value);
        }

        return value;
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
        return context.runtime.newBoolean(!(obj.isTrue()));
    }

    @JIT
    public static RaiseException newRequiredKeywordArgumentError(ThreadContext context, String name) {
        return context.runtime.newArgumentError("missing keyword: " + name);
    }

    @JIT
    public static void pushExitBlock(ThreadContext context, Block blk) {
        context.runtime.pushExitBlock(context.runtime.newProc(Block.Type.LAMBDA, blk));
    }

    @JIT
    public static FunctionalCachingCallSite newFunctionalCachingCallSite(String name) {
        return new FunctionalCachingCallSite(name);
    }

    @JIT
    public static NormalCachingCallSite newNormalCachingCallSite(String name) {
        return new NormalCachingCallSite(name);
    }

    @JIT
    public static VariableCachingCallSite newVariableCachingCallSite(String name) {
        return new VariableCachingCallSite(name);
    }

    @JIT
    public static RefinedCachingCallSite newRefinedCachingCallSite(String name, String callType) {
        return new RefinedCachingCallSite(name, CallType.valueOf(callType));
    }

    @JIT
    public static IRScope decodeScopeFromBytes(Ruby runtime, byte[] scopeBytes, String filename) {
        try {
            return IRReader.load(runtime.getIRManager(), new IRReaderStream(runtime.getIRManager(), new ByteArrayInputStream(scopeBytes), new ByteList(filename.getBytes())));
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
        IRubyObject result = (IRubyObject)accessor.get(self);
        if (result == null) {
            if (context.runtime.isVerbose()) {
                context.runtime.getWarnings().warning(IRubyWarnings.ID.IVAR_NOT_INITIALIZED, "instance variable " + name + " not initialized");
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

    private static IRubyObject[] toAry(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1 && args[0].respondsTo("to_ary")) {
            IRubyObject newAry = Helpers.aryToAry(args[0]);
            if (newAry.isNil()) {
                args = new IRubyObject[] { args[0] };
            } else if (newAry instanceof RubyArray) {
                args = ((RubyArray) newAry).toJavaArray();
            } else {
                throw context.runtime.newTypeError(args[0].getType().getName() + "#to_ary should return Array");
            }
        }
        return args;
    }

    private static IRubyObject[] prepareProcArgs(ThreadContext context, Block b, IRubyObject[] args) {
        if (args.length == 1) {
            int arityValue = b.getBody().getSignature().arityValue();
            return IRRuntimeHelpers.convertValueIntoArgArray(context, args[0], arityValue, b.type == Block.Type.NORMAL && args[0] instanceof RubyArray);
        } else {
            return args;
        }
    }

    private static IRubyObject[] prepareBlockArgsInternal(ThreadContext context, Block block, IRubyObject[] args) {
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }

        boolean isProcCall = context.getCurrentBlockType() == Block.Type.PROC;
        org.jruby.runtime.Signature sig = block.getBody().getSignature();
        if (block.type == Block.Type.LAMBDA) {
            if (!isProcCall && sig.arityValue() != -1 && sig.required() != 1) {
                args = toAry(context, args);
            }
            sig.checkArity(context.runtime, args);
            return args;
        }

        if (isProcCall) {
            return prepareProcArgs(context, block, args);
        }

        int arityValue = sig.arityValue();
        if (arityValue >= -1 && arityValue <= 1) {
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

        if (sig.isFixed() && required > 0 && required+needsKwargs != actual) {
            // Make sure we have a ruby-hash
            IRubyObject[] newArgs = Arrays.copyOf(args, required + needsKwargs);
            if (actual < required+needsKwargs) {
                // Not enough args and we need an empty {} for kwargs processing.
                newArgs[newArgs.length - 1] = RubyHash.newHash(context.runtime);
            } else {
                // We have more args than we need and kwargs is always the last arg.
                newArgs[newArgs.length - 1] = args[args.length - 1];
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
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }

        if (block.type == Block.Type.LAMBDA) {
            block.getSignature().checkArity(context.runtime, args);
        }

        return args;
    }

    @Interp @JIT
    public static IRubyObject[] prepareSingleBlockArgs(ThreadContext context, Block block, IRubyObject[] args) {
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }

        if (block.type == Block.Type.LAMBDA) {
            block.getBody().getSignature().checkArity(context.runtime, args);
            return args;
        }

        boolean isProcCall = context.getCurrentBlockType() == Block.Type.PROC;
        if (isProcCall) {
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
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }

        boolean isProcCall = context.getCurrentBlockType() == Block.Type.PROC;
        if (block.type == Block.Type.LAMBDA) {
            org.jruby.runtime.Signature sig = block.getBody().getSignature();
            // We don't need to check for the 1 required arg case here
            // since that goes down the prepareSingleBlockArgs route
            if (!isProcCall && sig.arityValue() != 1) {
                args = toAry(context, args);
            }
            sig.checkArity(context.runtime, args);
            return args;
        }

        if (isProcCall) {
            return prepareProcArgs(context, block, args);
        }

        // If we need more than 1 reqd arg, convert a single value to an array if possible.
        // If there are insufficient args, ReceivePreReqdInstr will return nil
        return toAry(context, args);
    }

    // This is the placeholder for scenarios not handled by specialized instructions.
    @Interp @JIT
    public static IRubyObject[] prepareBlockArgs(ThreadContext context, Block block, IRubyObject[] args, boolean usesKwArgs) {
        args = prepareBlockArgsInternal(context, block, args);
        if (usesKwArgs) {
            frobnicateKwargsArgument(context, args, block.getBody().getSignature().required());
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

    @Interp @JIT
    public static DynamicScope pushBlockDynamicScopeIfNeeded(ThreadContext context, Block block, boolean pushNewDynScope, boolean reuseParentDynScope) {
        DynamicScope newScope = getNewBlockScope(block, pushNewDynScope, reuseParentDynScope);
        if (newScope != null) {
            context.pushScope(newScope);
        }
        return newScope;
    }

    @Interp @JIT
    public static IRubyObject updateBlockState(Block block, IRubyObject self) {
        // SSS FIXME: Why is self null in non-binding-eval contexts?
        if (self == null || block.getEvalType() == EvalType.BINDING_EVAL) {
            // Update self to the binding's self
            self = useBindingSelf(block.getBinding());
        }

        // Clear block's eval type
        block.setEvalType(EvalType.NONE);

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
     * @param symbol
     * @return
     */
    @Interp
    public static RubyProc newSymbolProc(ThreadContext context, String symbol, Encoding encoding) {
        return (RubyProc)context.runtime.newSymbol(symbol, encoding).to_proc(context);
    }

    /**
     * Create a new Symbol.to_proc for the given symbol name and encoding.
     *
     * @param context
     * @param symbol
     * @return
     */
    @JIT
    public static RubyProc newSymbolProc(ThreadContext context, String symbol, String encoding) {
        return newSymbolProc(context, symbol, retrieveJCodingsEncoding(context, encoding));
    }

    @JIT
    public static IRubyObject[] singleBlockArgToArray(IRubyObject value) {
        IRubyObject[] args;
        if (value instanceof RubyArray) {
            args = value.convertToArray().toJavaArray();
        } else {
            args = new IRubyObject[] { value };
        }
        return args;
    }

    @JIT
    public static Block prepareBlock(ThreadContext context, IRubyObject self, DynamicScope scope, BlockBody body) {
        Block block = new Block(body, context.currentBinding(self, scope));

        return block;
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

    public static RubyString freezeLiteralString(ThreadContext context, RubyString string, String file, int line) {
        Ruby runtime = context.runtime;

        if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
            // stuff location info into the string and then freeze it
            RubyArray info = (RubyArray) runtime.newArray(runtime.newString(file).freeze(context), runtime.newFixnum(line)).freeze(context);
            string.setInstanceVariable(RubyString.DEBUG_INFO_FIELD, info);
        }

        string.setFrozen(true);

        return string;
    }

    @JIT
    public static IRubyObject callOptimizedAref(ThreadContext context, IRubyObject caller, IRubyObject target, RubyString keyStr, CallSite site) {
        if (target instanceof RubyHash && ((CachingCallSite) site).retrieveCache(target.getMetaClass(), "[]").method.isBuiltin()) {
            // call directly with cached frozen string
            return ((RubyHash) target).op_aref(context, keyStr);
        }

        return site.call(context, caller, target, keyStr.strDup(context.runtime));
    }

    public static DynamicMethod getRefinedMethodForClass(StaticScope refinedScope, RubyModule target, String methodName) {
        Map<RubyClass, RubyModule> refinements;
        RubyModule refinement;
        DynamicMethod method = null;
        RubyModule overlay;

        while (true) {
            if (refinedScope == null) break;

            overlay = refinedScope.getOverlayModuleForRead();

            if (overlay != null) {

                refinements = overlay.getRefinements();

                if (!refinements.isEmpty()) {

                    refinement = refinements.get(target);

                    if (refinement != null) {

                        DynamicMethod maybeMethod = refinement.searchMethod(methodName);

                        if (!maybeMethod.isUndefined()) {
                            method = maybeMethod;
                            break;
                        }
                    }
                }
            }

            refinedScope = refinedScope.getEnclosingScope();
        }

        return method;
    }
}
