package org.jruby.ir.runtime;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyMatchData;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.NativeException;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
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

    public static boolean inNonMethodBodyLambda(IRStaticScope scope, Block.Type blockType) {
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
    public static void initiateNonLocalReturn(ThreadContext context, DynamicScope dynScope, boolean maybeLambda, IRubyObject returnValue) {
        IRStaticScope scope = (IRStaticScope)dynScope.getStaticScope();
        IRScopeType scopeType = scope.getScopeType();
        while (dynScope != null) {
            IRStaticScope ss = (IRStaticScope)dynScope.getStaticScope();
            // SSS FIXME: Why is scopeType empty? Looks like this static-scope
            // was not associated with the AST scope that got converted to IR.
            //
            // Ruby code: lambda { Thread.new { return }.join }.call
            //
            // To be investigated.
            IRScopeType ssType = ss.getScopeType();
            if (ssType != null && ssType.isMethodType()) {
                break;
            }
            dynScope = dynScope.getNextCapturedScope();
        }

        // SSS FIXME: Why is scopeType empty? Looks like this static-scope
        // was not associated with the AST scope that got converted to IR.
        //
        // Ruby code: lambda { Thread.new { return }.join }.call
        //
        // To be investigated.
        if (   (scopeType == null || (scopeType.isClosureType() && scopeType != IRScopeType.EVAL_SCRIPT))
            && (maybeLambda || !context.scopeExistsOnCallStack(dynScope)))
        {
            // Cannot return from the call that we have long since exited.
            throw IRException.RETURN_LocalJumpError.getException(context.runtime);
        }

        // methodtoReturnFrom will not be -1 for explicit returns from class/module/sclass bodies
        throw IRReturnJump.create(dynScope, returnValue);
    }

    public static IRubyObject handleNonlocalReturn(IRStaticScope scope, DynamicScope dynScope, Object rjExc, Block.Type blockType) throws RuntimeException {
        if (!(rjExc instanceof IRReturnJump)) {
            Helpers.throwException((Throwable)rjExc);
            return null;
        } else {
            IRReturnJump rj = (IRReturnJump)rjExc;

            // - If we are in a lambda or if we are in the method scope we are supposed to return from, stop propagating
            if (inNonMethodBodyLambda(scope, blockType) || (rj.methodToReturnFrom == dynScope)) return (IRubyObject) rj.returnValue;

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
            IRStaticScope scope = (IRStaticScope)dynScope.getStaticScope();
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

    public static IRubyObject handleBreakAndReturnsInLambdas(ThreadContext context, IRStaticScope scope, DynamicScope dynScope, Object exc, Block.Type blockType) throws RuntimeException {
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
            IRStaticScope scope = (IRStaticScope)dynScope.getStaticScope();
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

    public static IRubyObject defCompiledIRMethod(ThreadContext context, MethodHandle handle, String rubyName, StaticScope parentScope, String scopeDesc,
                                  String filename, int line, String parameterDesc) {
        Ruby runtime = context.runtime;

        RubyModule containingClass = context.getRubyClass();
        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, containingClass, rubyName, currVisibility);

        StaticScope scope = Helpers.decodeScope(context, parentScope, scopeDesc);

        DynamicMethod method = new CompiledIRMethod(handle, rubyName, filename, line, scope, newVisibility, containingClass, parameterDesc);

        return Helpers.addInstanceMethod(containingClass, rubyName, method, currVisibility, context, runtime);
    }

    public static IRubyObject defCompiledIRClassMethod(ThreadContext context, IRubyObject obj, MethodHandle handle, String rubyName, StaticScope parentScope, String scopeDesc,
                                                  String filename, int line, String parameterDesc) {
        Ruby runtime = context.runtime;

        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton method \"" + rubyName + "\" for " + obj.getMetaClass().getBaseName());
        }

        if (obj.isFrozen()) throw runtime.newFrozenError("object");

        RubyClass containingClass = obj.getSingletonClass();

        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, containingClass, rubyName, currVisibility);

        StaticScope scope = Helpers.decodeScope(context, parentScope, scopeDesc);

        DynamicMethod method = new CompiledIRMethod(handle, rubyName, filename, line, scope, newVisibility, containingClass, parameterDesc);

        return Helpers.addInstanceMethod(containingClass, rubyName, method, currVisibility, context, runtime);
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
            if (!arrayCheck) {
                // wrap Throwable in a NativeException object
                IRubyObject exceptionObj = new NativeException(runtime, runtime.getNativeException(), throwable);
                ((NativeException)exceptionObj).prepareIntegratedBacktrace(context, throwable.getStackTrace());
                runtime.getGlobalVariables().set("$!", exceptionObj);
            }
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
                    return true;
                }
            }
        } else if (excObj instanceof IRubyObject) {
            // SSS FIXME: Should this check be "runtime.getModule().isInstance(excType)"??
            if (!(excType instanceof RubyModule)) {
                throw context.runtime.newTypeError("class or module required for rescue clause. Found: " + excType);
            }

            return excType.callMethod(context, "===", (IRubyObject)excObj).isTrue();
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
        return (unwrapArray && (yieldVal instanceof RubyArray)) ? b.yieldArray(context, yieldVal, null, null) : b.yield(context, yieldVal);
    }

    public static IRubyObject yieldSpecific(ThreadContext context, Object blk) {
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        Block b = (Block)blk;
        return b.yieldSpecific(context);
    }

    public static IRubyObject[] convertValueIntoArgArray(ThreadContext context, IRubyObject value, Arity arity, boolean passArrayArg, boolean argIsArray) {
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
                       value = passArrayArg ? RubyArray.newEmptyArray(context.runtime) : context.nil;
                   } else if (!passArrayArg) {
                       value = valArray.eltInternal(0);
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
        RubyBasicObject objClass = context.runtime.getObject();
        // We have to rely on the frame stack to find the implementation class
        RubyModule klazz = context.getFrameKlazz();
        String methodName = context.getCurrentFrame().getName();

        checkSuperDisabledOrOutOfMethod(context, klazz, methodName);
        RubyClass superClass = Helpers.findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(methodName) : UndefinedMethod.INSTANCE;

        IRubyObject rVal = method.isUndefined() ? Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, CallType.SUPER, args, block)
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

    public static void defineAlias(ThreadContext context, IRubyObject object, String newNameString, String oldNameString) {
        if (object == null || object instanceof RubyFixnum || object instanceof RubySymbol) {
            throw context.runtime.newTypeError("no class to make alias");
        }

        RubyModule module = (object instanceof RubyModule) ? (RubyModule) object : object.getMetaClass();
        module.defineAlias(newNameString, oldNameString);
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
}
