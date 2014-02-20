package org.jruby.ir.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
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
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.StaticScope;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
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
    public static void initiateNonLocalReturn(ThreadContext context, IRStaticScope scope, int methodToReturnFrom, IRubyObject returnValue) {
        IRScopeType scopeType = scope.getScopeType();

        // SSS FIXME: Why is scopeType empty? Looks like this static-scope
        // was not associated with the AST scope that got converted to IR.
        //
        // Ruby code: lambda { Thread.new { return }.join }.call
        //
        // To be investigated.
        if (   (scopeType == null || (scopeType.isClosureType() && scopeType != IRScopeType.EVAL_SCRIPT))
            && (methodToReturnFrom == -1 || !context.scopeExistsOnCallStack(methodToReturnFrom)))
        {
            // Cannot return from the call that we have long since exited.
            throw IRException.RETURN_LocalJumpError.getException(context.runtime);
        }

        // methodtoReturnFrom will not be -1 for explicit returns from class/module/sclass bodies
        throw IRReturnJump.create(methodToReturnFrom, returnValue);
    }

    public static IRubyObject handleNonlocalReturn(IRStaticScope scope, Object rjExc, Block.Type blockType) throws RuntimeException {
        if (!(rjExc instanceof IRReturnJump)) {
            Helpers.throwException((Throwable)rjExc);
            return null;
        } else {
            IRReturnJump rj = (IRReturnJump)rjExc;

            // - If we are in a lambda or if we are in the method scope we are supposed to return from, stop propagating
            if (inNonMethodBodyLambda(scope, blockType) || (rj.methodToReturnFrom == scope.getScopeId())) return (IRubyObject) rj.returnValue;

            // - If not, Just pass it along!
            throw rj;
        }
    }

    public static IRubyObject initiateBreak(ThreadContext context, IRStaticScope scope, int scopeIdToReturnTo, IRubyObject breakValue, Block.Type blockType) throws RuntimeException {
        if (inLambda(blockType)) {
            // Ensures would already have been run since the IR builder makes
            // sure that ensure code has run before we hit the break.  Treat
            // the break as a regular return from the closure.
            return breakValue;
        } else {
            IRScopeType scopeType = scope.getScopeType();
            if (!scopeType.isClosureType()) {
                // Error -- breaks can only be initiated in closures
                throw IRException.BREAK_LocalJumpError.getException(context.runtime);
            }

            IRBreakJump bj = IRBreakJump.create(scopeIdToReturnTo, breakValue);
            if (scopeType == IRScopeType.EVAL_SCRIPT) {
                // If we are in an eval, record it so we can account for it
                bj.breakInEval = true;
            }

            // Start the process of breaking through the intermediate scopes
            throw bj;
        }
    }

    public static IRubyObject handleBreakAndReturnsInLambdas(ThreadContext context, IRStaticScope scope, Object exc, Block.Type blockType) throws RuntimeException {
        if ((exc instanceof IRBreakJump) && inNonMethodBodyLambda(scope, blockType)) {
            // We just unwound all the way up because of a non-local break
            throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());
        } else if (exc instanceof IRReturnJump) {
            return handleNonlocalReturn(scope, exc, blockType);
        } else {
            // Propagate
            Helpers.throwException((Throwable)exc);
            // should not get here
            return null;
        }
    }

    public static IRubyObject handlePropagatedBreak(ThreadContext context, IRStaticScope scope, Object bjExc, Block.Type blockType) {
        if (!(bjExc instanceof IRBreakJump)) {
            Helpers.throwException((Throwable)bjExc);
            return null;
        }

        IRBreakJump bj = (IRBreakJump)bjExc;
        if (bj.breakInEval) {
            // If the break was in an eval, we pretend as if it was in the containing scope
            IRScopeType scopeType = scope.getScopeType();
            if (!scopeType.isClosureType()) {
                // Error -- breaks can only be initiated in closures
                throw IRException.BREAK_LocalJumpError.getException(context.getRuntime());
            } else {
                bj.breakInEval = false;
                throw bj;
            }
        } else if (bj.scopeIdToReturnTo == scope.getScopeId()) {
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

    // SSS FIXME: Is this code effectively equivalent to Helpers.isJavaExceptionHandled?
    public static boolean exceptionHandled(ThreadContext context, IRubyObject excType, Object excObj) {
        Ruby runtime = context.runtime;

        // unwrap Ruby exceptions
        if (excObj instanceof RaiseException) {
            excObj = ((RaiseException)excObj).getException();
        }

        if (excObj instanceof IRubyObject) {
            // regular ruby exception
            if (!(excType instanceof RubyModule)) throw runtime.newTypeError("class or module required for rescue clause. Found: " + excType);
            return excType.callMethod(context, "===", (IRubyObject)excObj).isTrue();
        } else if (runtime.getException().op_ge(excType).isTrue() || runtime.getObject() == excType) {
            // convert java obj to a ruby object and try again
            return excType.callMethod(context, "===", JavaUtil.convertJavaToUsableRubyObject(runtime, excObj)).isTrue();
        } else if (excType instanceof RubyClass && excType.getInstanceVariables().hasInstanceVariable("@java_class")) {
            // java exception where the rescue clause has an embedded java class that could catch it
            RubyClass rubyClass = (RubyClass)excType;
            JavaClass javaClass = (JavaClass)rubyClass.getInstanceVariable("@java_class");
            if (javaClass != null) {
                Class cls = javaClass.javaClass();
                if (cls.isInstance(excObj)) return true;
            }
        }

        return false;
    }

    public static IRubyObject isExceptionHandled(ThreadContext context, IRubyObject excType, Object excObj) {
        Ruby runtime = context.runtime;

        boolean isUndefExc = excObj == UndefinedValue.UNDEFINED;
        if (excType instanceof RubyArray) {
            RubyArray testTypes = (RubyArray)excType;
            for (int i = 0, n = testTypes.getLength(); i < n; i++) {
                IRubyObject testType = testTypes.eltInternal(i);
                boolean handled = isUndefExc ? testType.isTrue() : IRRuntimeHelpers.exceptionHandled(context, testType, excObj);
                if (handled) return runtime.newBoolean(true);
            }
            return runtime.newBoolean(false);
        } else {
            // SSS FIXME: Why are we returning 'excType'? Shouldn't this be a boolean?
            return isUndefExc ? excType : runtime.newBoolean(IRRuntimeHelpers.exceptionHandled(context, excType, excObj));
        }
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

    public static void checkArity(ThreadContext context, Object[] args, int required, int opt, int rest, boolean receivesKwargs) {
        int numArgs = args.length;
        int kwArgHashCount = extractKwargsCount(args, receivesKwargs);
        if ((numArgs < required) || ((rest == -1) && (numArgs > (required + opt + kwArgHashCount)))) {
            Arity.raiseArgumentError(context.runtime, numArgs, required, required + opt);
        }
    }

    public static int extractKwargsCount(Object[] args, boolean receivesKwargs) {
        return (receivesKwargs && args.length > 0 && args[args.length - 1] instanceof RubyHash) ? 1 : 0;
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
}
