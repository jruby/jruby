package org.jruby.ir.runtime;

import org.jruby.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.DefinedMessage;
import org.jruby.util.TypeConverter;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;

public class IRRuntimeHelpers {
    private static final Logger LOG = LoggerFactory.getLogger("IRRuntimeHelpers");

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

    /*
     * Handle non-local returns (ex: when nested in closures, root scopes of module/class/sclass bodies)
     */
    public static IRubyObject initiateNonLocalReturn(ThreadContext context, DynamicScope dynScope, Block.Type blockType, boolean maybeLambda, IRubyObject returnValue) {
        // If not in a lambda, check if this was a non-local return
        if (IRRuntimeHelpers.inLambda(blockType)) return returnValue;

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
            dynScope = dynScope.getNextCapturedScope();
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

        // methodtoReturnFrom will not be -1 for explicit returns from class/module/sclass bodies
        throw IRReturnJump.create(dynScope, returnValue);
    }

    public static IRubyObject handleNonlocalReturn(StaticScope scope, DynamicScope dynScope, Object rjExc, Block.Type blockType) throws RuntimeException {
        if (!(rjExc instanceof IRReturnJump)) {
            Helpers.throwException((Throwable)rjExc);
            return null;
        } else {
            IRReturnJump rj = (IRReturnJump)rjExc;

            // - If we are in a lambda or if we are in the method scope we are supposed to return from, stop propagating
            if (inNonMethodBodyLambda(scope, blockType) || (rj.methodToReturnFrom == dynScope)) {
                if (isDebug()) System.out.println("---> Non-local Return reached target in scope: " + dynScope);
                return (IRubyObject) rj.returnValue;
            }

            // - If not, Just pass it along!
            throw rj;
        }
    }

    public static IRubyObject initiateBreak(ThreadContext context, DynamicScope dynScope, IRubyObject breakValue, Block.Type blockType) throws RuntimeException {
        if (inLambda(blockType)) {
            // Ensures would already have been run since the IR builder makes
            // sure that ensure code has run before we hit the break.  Treat
            // the break as a regular return from the closure.
            return breakValue;
        } else {
            StaticScope scope = dynScope.getStaticScope();
            IRScopeType scopeType = scope.getScopeType();
            if (!scopeType.isClosureType()) {
                // Error -- breaks can only be initiated in closures
                throw IRException.BREAK_LocalJumpError.getException(context.runtime);
            }

            IRBreakJump bj = IRBreakJump.create(dynScope.getNextCapturedScope(), breakValue);
            if (scopeType == IRScopeType.EVAL_SCRIPT) {
                // If we are in an eval, record it so we can account for it
                bj.breakInEval = true;
            }

            // Start the process of breaking through the intermediate scopes
            throw bj;
        }
    }

    public static IRubyObject handleBreakAndReturnsInLambdas(ThreadContext context, StaticScope scope, DynamicScope dynScope, Object exc, Block.Type blockType) throws RuntimeException {
        if ((exc instanceof IRBreakJump) && inNonMethodBodyLambda(scope, blockType)) {
            // We just unwound all the way up because of a non-local break
            throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());
        } else if (exc instanceof IRReturnJump) {
            return handleNonlocalReturn(scope, dynScope, exc, blockType);
        } else {
            // Propagate
            Helpers.throwException((Throwable)exc);
            // should not get here
            return null;
        }
    }

    public static IRubyObject handlePropagatedBreak(ThreadContext context, DynamicScope dynScope, Object bjExc, Block.Type blockType) {
        if (!(bjExc instanceof IRBreakJump)) {
            Helpers.throwException((Throwable)bjExc);
            return null;
        }

        IRBreakJump bj = (IRBreakJump)bjExc;
        if (bj.breakInEval) {
            // If the break was in an eval, we pretend as if it was in the containing scope
            StaticScope scope = dynScope.getStaticScope();
            IRScopeType scopeType = scope.getScopeType();
            if (!scopeType.isClosureType()) {
                // Error -- breaks can only be initiated in closures
                throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());
            } else {
                bj.breakInEval = false;
                throw bj;
            }
        } else if (bj.scopeToReturnTo == dynScope) {
            // Done!! Hurray!
            if (isDebug()) System.out.println("---> Break reached target in scope: " + dynScope);
            return bj.breakValue;
/* ---------------------------------------------------------------
 * FIXME: Puzzled .. Why is this not needed?
        } else if (!context.scopeExistsOnCallStack(bj.scopeToReturnTo.getStaticScope())) {
            throw IRException.BREAK_LocalJumpError.getException(context.runtime);
 * --------------------------------------------------------------- */
        } else {
            // Propagate
            throw bj;
        }
    }

    // Used by JIT
    public static void defCompiledIRMethod(ThreadContext context, MethodHandle handle, String rubyName, DynamicScope currDynScope, IRubyObject self, String scopeDesc,
                                  String filename, int line, String parameterDesc, boolean hasExplicitCallProtocol) {
        Ruby runtime = context.runtime;
        StaticScope parentScope = currDynScope.getStaticScope();

        RubyModule containingClass = IRRuntimeHelpers.findInstanceMethodContainer(context, currDynScope, self);
        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, containingClass, rubyName, currVisibility);

        StaticScope scope = Helpers.decodeScope(context, parentScope, scopeDesc);

        DynamicMethod method = new CompiledIRMethod(handle, rubyName, filename, line, scope, newVisibility, containingClass, parameterDesc, hasExplicitCallProtocol);

        Helpers.addInstanceMethod(containingClass, rubyName, method, currVisibility, context, runtime);
    }

    // Used by JIT
    public static void defCompiledIRClassMethod(ThreadContext context, IRubyObject obj, MethodHandle handle, String rubyName, StaticScope parentScope, String scopeDesc,
                                                  String filename, int line, String parameterDesc, boolean hasExplicitCallProtocol) {
        Ruby runtime = context.runtime;

        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton method \"" + rubyName + "\" for " + obj.getMetaClass().getBaseName());
        }

        if (obj.isFrozen()) throw runtime.newFrozenError("object");

        RubyClass containingClass = obj.getSingletonClass();

        StaticScope scope = Helpers.decodeScope(context, parentScope, scopeDesc);

        DynamicMethod method = new CompiledIRMethod(handle, rubyName, filename, line, scope, Visibility.PUBLIC, containingClass, parameterDesc, hasExplicitCallProtocol);

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

        return context.runtime.getNil();
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

    public static IRubyObject isEQQ(ThreadContext context, IRubyObject receiver, IRubyObject value) {
        boolean isUndefValue = value == UndefinedValue.UNDEFINED;
        if (receiver instanceof RubyArray) {
            RubyArray testVals = (RubyArray)receiver;
            for (int i = 0, n = testVals.getLength(); i < n; i++) {
                IRubyObject v = testVals.eltInternal(i);
                IRubyObject eqqVal = isUndefValue ? v : v.callMethod(context, "===", value);
                if (eqqVal.isTrue()) return eqqVal;
            }
            return context.runtime.newBoolean(false);
        } else {
            return isUndefValue ? receiver : receiver.callMethod(context, "===", value);
        }
    }

    public static IRubyObject newProc(Ruby runtime, Block block) {
        return (block == Block.NULL_BLOCK) ? runtime.getNil() : runtime.newProc(Block.Type.PROC, block);
    }

    public static IRubyObject yield(ThreadContext context, Object blk, Object yieldArg, boolean unwrapArray) {
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        Block b = (Block)blk;
        IRubyObject yieldVal = (IRubyObject)yieldArg;
        return (unwrapArray && (yieldVal instanceof RubyArray)) ? b.yieldArray(context, yieldVal, null) : b.yield(context, yieldVal);
    }

    public static IRubyObject yieldSpecific(ThreadContext context, Object blk) {
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        Block b = (Block)blk;
        return b.yieldSpecific(context);
    }

    public static IRubyObject[] convertValueIntoArgArray(ThreadContext context, IRubyObject value, Arity arity, boolean argIsArray) {
        // SSS FIXME: This should not really happen -- so, some places in the runtime library are breaking this contract.
        if (argIsArray && !(value instanceof RubyArray)) argIsArray = false;

        int blockArity = arity.getValue();
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
                    if (!(val0 instanceof RubyArray)) {
                        throw context.runtime.newTypeError(value.getType().getName() + "#to_ary should return Array");
                    }
                    return ((RubyArray)val0).toJavaArray();
                }
        }
    }

    public static Block getBlockFromObject(ThreadContext context, Object value) {
        Block block;
        if (value instanceof Block) {
            block = (Block) value;
        } else if (value instanceof RubyProc) {
            block = ((RubyProc) value).getBlock();
        } else if (value instanceof RubyMethod) {
            block = ((RubyProc)((RubyMethod)value).to_proc(context, null)).getBlock();
        } else if ((value instanceof IRubyObject) && ((IRubyObject)value).isNil()) {
            block = Block.NULL_BLOCK;
        } else if (value instanceof IRubyObject) {
            block = ((RubyProc) TypeConverter.convertToType((IRubyObject) value, context.runtime.getProc(), "to_proc", true)).getBlock();
        } else {
            throw new RuntimeException("Unhandled case in CallInstr:prepareBlock.  Got block arg: " + value);
        }
        return block;
    }

    public static void checkArity(ThreadContext context, Object[] args, int required, int opt, int rest,
                                  boolean receivesKwargs, int restKey) {
        int argsLength = args.length;
        RubyHash keywordArgs = (RubyHash) extractKwargsHash(args, required, receivesKwargs);

        if (restKey == -1 && keywordArgs != null) checkForExtraUnwantedKeywordArgs(context, keywordArgs);

        // keyword arguments value is not used for arity checking.
        if (keywordArgs != null) argsLength -= 1;

        if (argsLength < required || (rest == -1 && argsLength > (required + opt))) {
//            System.out.println("NUMARGS: " + argsLength + ", REQUIRED: " + required + ", OPT: " + opt + ", AL: " + args.length + ",RKW: " + receivesKwargs );
//            System.out.println("ARGS[0]: " + args[0]);

            Arity.raiseArgumentError(context.runtime, argsLength, required, required + opt);
        }
    }

    public static RubyHash extractKwargsHash(Object[] args, int requiredArgsCount, boolean receivesKwargs) {
        if (!receivesKwargs) return null;
        if (args.length <= requiredArgsCount) return null; // No kwarg because required args slurp them up.

        Object lastArg = args[args.length - 1];

        return !(lastArg instanceof RubyHash) ? null : (RubyHash) lastArg;
    }

    public static void checkForExtraUnwantedKeywordArgs(final ThreadContext context, RubyHash keywordArgs) {
        final StaticScope scope = context.getCurrentStaticScope();

        keywordArgs.visitAll(new RubyHash.Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject value) {
                String keyAsString = key.asJavaString();
                int slot = scope.isDefined((keyAsString));

                // Found name in higher variable scope.  Therefore non for this block/method def.
                if ((slot >> 16) > 0) throw context.runtime.newArgumentError("unknown keyword: " + keyAsString);
                // Could not find it anywhere.
                if (((short) (slot & 0xffff)) < 0) throw context.runtime.newArgumentError("unknown keyword: " + keyAsString);
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

    public static IRubyObject unresolvedSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {

        // We have to rely on the frame stack to find the implementation class
        RubyModule klazz = context.getFrameKlazz();
        String methodName = context.getCurrentFrame().getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyClass superClass = Helpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;

        // TODO tduehr 7/2014 There has to be a better way to break the recursion
        IRubyObject rVal = (method.isUndefined() || (superClass.isPrepended() && (method.getImplementationClass() == self.getType()))) ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                : method.call(context, self, superClass, methodName, args, block);

        return rVal;
    }

    public static IRubyObject isDefinedBackref(ThreadContext context) {
        return RubyMatchData.class.isInstance(context.getBackRef()) ?
                context.runtime.getDefinedMessage(DefinedMessage.GLOBAL_VARIABLE) : context.nil;
    }

    public static IRubyObject isDefinedGlobal(ThreadContext context, String name) {
        return context.runtime.getGlobalVariables().isDefined(name) ?
                context.runtime.getDefinedMessage(DefinedMessage.GLOBAL_VARIABLE) : context.nil;
    }

    // FIXME: This checks for match data differently than isDefinedBackref.  Seems like they should use same mechanism?
    public static IRubyObject isDefinedNthRef(ThreadContext context, int matchNumber) {
        IRubyObject backref = context.getBackRef();

        if (backref instanceof RubyMatchData) {
            if (!((RubyMatchData) backref).group(matchNumber).isNil()) {
                return context.runtime.getDefinedMessage(DefinedMessage.GLOBAL_VARIABLE);
            }
        }

        return context.nil;
    }

    public static IRubyObject isDefinedClassVar(ThreadContext context, RubyModule receiver, String name) {
        boolean defined = receiver.isClassVarDefined(name);

        if (!defined && receiver.isSingleton()) { // Look for class var in singleton if it is one.
            IRubyObject attached = ((MetaClass) receiver).getAttached();

            if (attached instanceof RubyModule) defined = ((RubyModule) attached).isClassVarDefined(name);
        }

        return defined ? context.runtime.getDefinedMessage(DefinedMessage.CLASS_VARIABLE) : context.nil;
    }

    public static IRubyObject isDefinedInstanceVar(ThreadContext context, IRubyObject receiver, String name) {
        return receiver.getInstanceVariables().hasInstanceVariable(name) ?
                context.runtime.getDefinedMessage(DefinedMessage.INSTANCE_VARIABLE) : context.nil;
    }

    public static IRubyObject isDefinedCall(ThreadContext context, IRubyObject self, IRubyObject receiver, String name) {
        RubyString boundValue = Helpers.getDefinedCall(context, self, receiver, name);

        return boundValue == null ? context.nil : boundValue;
    }

    public static IRubyObject isDefinedConstantOrMethod(ThreadContext context, IRubyObject receiver, String name) {
        RubyString definedType = Helpers.getDefinedConstantOrBoundMethod(receiver, name);

        return definedType == null ? context.nil : definedType;
    }

    public static IRubyObject isDefinedMethod(ThreadContext context, IRubyObject receiver, String name, boolean checkIfPublic) {
        DynamicMethod method = receiver.getMetaClass().searchMethod(name);

        // If we find the method we optionally check if it is public before returning "method".
        if (!method.isUndefined() &&  (!checkIfPublic || method.getVisibility() == Visibility.PUBLIC)) {
            return context.runtime.getDefinedMessage(DefinedMessage.METHOD);
        }

        return context.nil;
    }

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
                throw context.runtime.newNoMethodError("super called outside of method", null, context.runtime.getNil());
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

    public static IRubyObject mergeKeywordArguments(ThreadContext context, IRubyObject restKwarg, IRubyObject explcitKwarg) {
        return ((RubyHash) TypeConverter.checkHashType(context.runtime, restKwarg)).merge(context, explcitKwarg, Block.NULL_BLOCK);
    }

    public static RubyModule findInstanceMethodContainer(ThreadContext context, DynamicScope currDynScope, IRubyObject self) {
        boolean inBindingEval = currDynScope.inBindingEval();

        if (!inBindingEval && self == context.runtime.getTopSelf()) {
            // Top-level-scripts are special
            // but, not if binding-evals are in force!
            return self.getType();
        }

        DynamicScope ds = currDynScope;
        while (ds != null) {
            IRScopeType scopeType = ds.getStaticScope().getScopeType();
            switch (ds.getEvalType()) {
                case MODULE_EVAL  : return (RubyModule)self;
                case INSTANCE_EVAL: return self.getSingletonClass();
                case BINDING_EVAL : ds = ds.getNextCapturedScope(); break;
                case NONE:
                    if (scopeType == null || scopeType.isClosureType()) {
                        ds = ds.getNextCapturedScope();
                    } else if (inBindingEval) {
                        // Binding evals are special!
                        return ds.getStaticScope().getModule();
                    } else if (scopeType == IRScopeType.CLASS_METHOD) {
                        return (RubyModule)self;
                    } else if (scopeType == IRScopeType.INSTANCE_METHOD) {
                        return self.getMetaClass();
                    } else {
                        switch (scopeType) {
                            case MODULE_BODY:
                            case CLASS_BODY:
                            case METACLASS_BODY:
                                return (RubyModule)self;

                            default:
                                throw new RuntimeException("Should not get here!");
                        }
                    }
                    break;
            }
        }
        throw new RuntimeException("Should not get here!");
    }

    public static RubyBoolean isBlockGiven(ThreadContext context, Object blk) {
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        Block b = (Block)blk;
        return context.runtime.newBoolean(b.isGiven());
    }

    public static IRubyObject receiveRestArg(ThreadContext context, Object[] args, int required, int argIndex, boolean acceptsKeywordArguments) {
        RubyHash keywordArguments = extractKwargsHash(args, required, acceptsKeywordArguments);
        return constructRestArg(context, args, keywordArguments, required, argIndex);
    }

    public static IRubyObject constructRestArg(ThreadContext context, Object[] args, RubyHash keywordArguments, int required, int argIndex) {
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;
        int remainingArguments = argsLength - required;

        if (remainingArguments <= 0) return context.runtime.newArray(IRubyObject.NULL_ARRAY);

        IRubyObject[] restArgs = new IRubyObject[remainingArguments];
        System.arraycopy(args, argIndex, restArgs, 0, remainingArguments);

        return context.runtime.newArray(restArgs);
    }

    public static IRubyObject receivePostReqdArg(IRubyObject[] args, int preReqdArgsCount, int postReqdArgsCount, int argIndex, boolean acceptsKeywordArgument) {
        boolean kwargs = extractKwargsHash(args, preReqdArgsCount + postReqdArgsCount, acceptsKeywordArgument) != null;
        int n = kwargs ? args.length - 1 : args.length;
        int remaining = n - preReqdArgsCount;
        if (remaining <= argIndex) return null;  // For blocks!

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

        RubySymbol keywordName = context.getRuntime().newSymbol(argName);

        if (keywordArguments.fastARef(keywordName) == null) return UndefinedValue.UNDEFINED;

        // SSS FIXME: Can we use an internal delete here?
        // Enebo FIXME: Delete seems wrong if we are doing this for duplication purposes.
        return keywordArguments.delete(context, keywordName, Block.NULL_BLOCK);
    }

    public static IRubyObject receiveKeywordRestArg(ThreadContext context, IRubyObject[] args, int required, boolean keywordArgumentSupplied) {
        RubyHash keywordArguments = extractKwargsHash(args, required, keywordArgumentSupplied);

        return keywordArguments == null ? RubyHash.newSmallHash(context.getRuntime()) : keywordArguments;
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

    public static Object classSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, String methodName, RubyModule definingModule) {
        RubyClass superClass = definingModule.getMetaClass().getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;
        Object rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
                                           : method.call(context, self, superClass, methodName, args, block);
        return rVal;
    }
}
