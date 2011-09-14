package org.jruby.runtime.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodHandles.*;
import java.lang.invoke.MethodType;
import static java.lang.invoke.MethodType.*;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.AttrReaderMethod;
import org.jruby.internal.runtime.methods.AttrWriterMethod;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.CompiledMethod;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JittedMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.*;

public class InvocationLinker {
    private static final Logger LOG = LoggerFactory.getLogger("InvocationLinker");
    
    public static CallSite invocationBootstrap(Lookup lookup, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        CallSite site;

        if (name.equals("yieldSpecific")) {
            site = new MutableCallSite(type);
            MethodHandle target = lookup.findStatic(InvocationLinker.class, "yieldSpecificFallback", type.insertParameterTypes(0, MutableCallSite.class));
            target = insertArguments(target, 0, site);
            site.setTarget(target);
            return site;
        } else if (name.equals("call")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, false, false, true);
        } else if (name.equals("fcall")) {
            site = new JRubyCallSite(lookup, type, CallType.FUNCTIONAL, false, false, true);
        } else if (name.equals("callIter")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, false, true, true);
        } else if (name.equals("fcallIter")) {
            site = new JRubyCallSite(lookup, type, CallType.FUNCTIONAL, false, true, true);
        } else if (name.equals("attrAssign")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, true, false, false);
        } else if (name.equals("attrAssignSelf")) {
            site = new JRubyCallSite(lookup, type, CallType.VARIABLE, true, false, false);
        } else if (name.equals("attrAssignExpr")) {
            site = new JRubyCallSite(lookup, type, CallType.NORMAL, true, false, true);
        } else if (name.equals("attrAssignSelfExpr")) {
            site = new JRubyCallSite(lookup, type, CallType.VARIABLE, true, false, true);
        } else {
            throw new RuntimeException("wrong invokedynamic target: " + name);
        }
        
        MethodType fallbackType = type.insertParameterTypes(0, JRubyCallSite.class);
        MethodHandle myFallback = insertArguments(
                lookup.findStatic(InvocationLinker.class, "invocationFallback",
                fallbackType),
                0,
                site);
        site.setTarget(myFallback);
        return site;
    }
    
    public static IRubyObject invocationFallback(JRubyCallSite site, 
            ThreadContext context,
            IRubyObject caller,
            IRubyObject self,
            String name) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        
        if (methodMissing(entry, site.callType(), name, caller)) {
            return callMethodMissing(entry, site.callType(), context, self, name);
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 0);
        target = updateInvocationTarget(target, site, selfClass, name, entry, false, 0);

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            IRubyObject mmResult = callMethodMissing(entry, site.callType(), context, self, name, arg0);
            // TODO: replace with handle logic
            return site.isAttrAssign() ? arg0 : mmResult;
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 1);
        target = updateInvocationTarget(target, site, selfClass, name, entry, false, 1);

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, arg0);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            IRubyObject mmResult = callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1);
            // TODO: replace with handle logic
            return site.isAttrAssign() ? arg1 : mmResult;
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 2);
        target = updateInvocationTarget(target, site, selfClass, name, entry, false, 2);

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, arg0, arg1);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            IRubyObject mmResult = callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2);
            // TODO: replace with handle logic
            return site.isAttrAssign() ? arg2 : mmResult;
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, 3);
        target = updateInvocationTarget(target, site, selfClass, name, entry, false, 3);

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, arg0, arg1, arg2);
    }
    
    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);
        if (methodMissing(entry, site.callType(), name, caller)) {
            IRubyObject mmResult = callMethodMissing(entry, site.callType(), context, self, name, args);
            // TODO: replace with handle logic
            return site.isAttrAssign() ? args[args.length - 1] : mmResult;
        }
        
        MethodHandle target = getTarget(site, selfClass, name, entry, -1);
        target = updateInvocationTarget(target, site, selfClass, name, entry, false, 4);

        return (IRubyObject)target.invokeWithArguments(context, caller, self, name, args);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 0);
        target = updateInvocationTarget(target, site, selfClass, name, entry, true, 0);

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 1);
        target = updateInvocationTarget(target, site, selfClass, name, entry, true, 1);

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, arg0, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 2);
        target = updateInvocationTarget(target, site, selfClass, name, entry, true, 2);

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, arg0, arg1, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, 3);
        target = updateInvocationTarget(target, site, selfClass, name, entry, true, 3);

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, arg0, arg1, arg2, block);
    }

    public static IRubyObject invocationFallback(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = selfClass.searchWithCache(name);

        if (methodMissing(entry, site.callType(), name, caller)) {
            try {
                return callMethodMissing(entry, site.callType(), context, self, name, args, block);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                return retryJumpError(context);
            } finally {
                if (site.isIterator()) block.escape();
            }
        }

        MethodHandle target = getTarget(site, selfClass, name, entry, -1);
        target = updateInvocationTarget(target, site, selfClass, name, entry, true, 4);

        return (IRubyObject) target.invokeWithArguments(context, caller, self, name, args, block);
    }

    /**
     * Update the given call site using the new target, wrapping with appropriate
     * guard and argument-juggling logic. Return a handle suitable for invoking
     * with the site's original method type.
     */
    private static MethodHandle updateInvocationTarget(MethodHandle target, JRubyCallSite site, RubyModule selfClass, String name, CacheEntry entry, boolean block, int arity) {
        if (target == null ||
                (!site.hasSeenType(selfClass.id)
                && site.seenTypes() > RubyInstanceConfig.MAX_FAIL_COUNT)) {
            site.setTarget(target = createFail((block?FAILS_B:FAILS)[arity], site, name, entry.method));
        } else {
            target = postProcess(site, target);
            
            boolean curry;
            MethodHandle fallback;
            MethodHandle gwt;
            if (site.getTarget() != null && !site.hasSeenType(selfClass.id)) {
                // new type for site, stack it up
                fallback = site.getTarget();
                curry = false;
            } else {
                // no existing target or rebinding a seen class, wipe out site
                fallback = (block?FALLBACKS_B:FALLBACKS)[arity];
                curry = true;
            }
            site.addType(selfClass.id);
            gwt = createGWT(selfClass, (block?TESTS_B:TESTS)[arity], target, fallback, entry, site, curry);
            
            if (RubyInstanceConfig.INVOKEDYNAMIC_INVOCATION_SWITCHPOINT) {
                // wrap in switchpoint for mutation invalidation
                SwitchPoint switchPoint = (SwitchPoint)selfClass.getInvalidator().getData();
                gwt = switchPoint.guardWithTest(gwt, curry ? insertArguments(fallback, 0, site) : fallback);
            }
            
            site.setTarget(gwt);
            
        }
        return target;
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context) throws Throwable {
        return block.yieldSpecific(context);
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context,
            IRubyObject arg0) throws Throwable {
        return block.yieldSpecific(context, arg0);
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context,
            IRubyObject arg0,
            IRubyObject arg1) throws Throwable {
        return block.yieldSpecific(context, arg0, arg1);
    }
    
    public static IRubyObject yieldSpecificFallback(
            MutableCallSite site,
            Block block,
            ThreadContext context,
            IRubyObject arg0,
            IRubyObject arg1,
            IRubyObject arg2) throws Throwable {
        return block.yieldSpecific(context, arg0, arg1, arg2);
    }

    private static MethodHandle createFail(MethodHandle fail, JRubyCallSite site, String name, DynamicMethod method) {
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to inline cache (failed #" + method.getSerialNumber() + ")");
        
        MethodHandle myFail = insertArguments(fail, 0, site);
        myFail = postProcess(site, myFail);
        return myFail;
    }

    private static MethodHandle createGWT(RubyModule selfClass, MethodHandle test, MethodHandle target, MethodHandle fallback, CacheEntry entry, JRubyCallSite site, boolean curryFallback) {
        MethodHandle myTest =
                RubyInstanceConfig.INVOKEDYNAMIC_INVOCATION_SWITCHPOINT ?
                insertArguments(test, 0, selfClass) :
                insertArguments(test, 0, entry.token);
        MethodHandle myFallback = curryFallback ? insertArguments(fallback, 0, site) : fallback;
        MethodHandle guardWithTest = guardWithTest(myTest, target, myFallback);
        
        return guardWithTest;
    }
    
    private static class IndirectBindingException extends RuntimeException {
        public IndirectBindingException(String reason) {
            super(reason);
        }
    }
    
    private static MethodHandle tryDispatchDirect(JRubyCallSite site, String name, RubyClass cls, DynamicMethod method) {
        // get the "real" method in a few ways
        while (method instanceof AliasMethod) method = method.getRealMethod();
        if (method instanceof DefaultMethod) {
            DefaultMethod defaultMethod = (DefaultMethod)method;
            if (defaultMethod.getMethodForCaching() instanceof JittedMethod) {
                method = defaultMethod.getMethodForCaching();
            }
        }
        
        DynamicMethod.NativeCall nativeCall = method.getNativeCall();
        
        MethodType trimmed = site.type().dropParameterTypes(2, 4);
        int siteArgCount = getArgCount(trimmed.parameterArray(), true);
        
        if (method instanceof AttrReaderMethod) {
            // attr reader
            if (siteArgCount != 0) {
                throw new IndirectBindingException("attr reader with > 0 args");
            }
        } else if (method instanceof AttrWriterMethod) {
            // attr writer
            if (siteArgCount != 1) {
                throw new IndirectBindingException("attr writer with > 1 args");
            }
        } else if (nativeCall != null) {
            // has an explicit native call path
            
            // if frame/scope required, can't dispatch direct
            if (method.getCallConfig() != CallConfiguration.FrameNoneScopeNone) {
                throw new IndirectBindingException("frame or scope required");
            }
            
            if (nativeCall.isJava()) {
                 if (!RubyInstanceConfig.INVOKEDYNAMIC_JAVA) {
                    throw new IndirectBindingException("direct Java dispatch not enabled");
                 }
                 
                // if Java, must be no-arg invocation
                if (nativeCall.getNativeSignature().length != 0 || siteArgCount != 0) {
                    throw new IndirectBindingException("Java call or receiver with > 0 args");
                }
            } else {
                // if non-Java, must:
                // * exactly match arities
                // * 3 or fewer arguments
                
                int nativeArgCount = (method instanceof CompiledMethod || method instanceof JittedMethod)
                        ? getRubyArgCount(nativeCall.getNativeSignature())
                        : getArgCount(nativeCall.getNativeSignature(), nativeCall.isStatic());
                
                // match arity and arity is not 4 (IRubyObject[].class)
                if (nativeArgCount == 4) {
                    throw new IndirectBindingException("target args > 4 or rest/optional");
                }
                
                if (nativeArgCount != siteArgCount) {
                    throw new IndirectBindingException("arity mismatch at call site");
                }
            }
        } else {
            throw new IndirectBindingException("no direct path available");
        }
        
        return handleForMethod(site, name, cls, method);
    }
    
    private static MethodHandle getTarget(JRubyCallSite site, RubyClass cls, String name, CacheEntry entry, int arity) {
        IndirectBindingException ibe;
        try {
            return tryDispatchDirect(site, name, cls, entry.method);
        } catch (IndirectBindingException _ibe) {
            ibe = _ibe;
            // proceed with indirect, if enabled
        }
        
        // if indirect indy-bound methods (via DynamicMethod.call) are disabled, bail out
        if (!RubyInstanceConfig.INVOKEDYNAMIC_INDIRECT) {
            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tfailed to bind to #" + entry.method.getSerialNumber() + ": " + ibe.getMessage());
            return null;
        }
        
        // no direct native path, use DynamicMethod.call
        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound indirectly to #" + entry.method.getSerialNumber() + ": " + ibe.getMessage());
        
        return insertArguments(getDynamicMethodTarget(site.type(), arity), 0, entry);
    }
    
    private static MethodHandle handleForMethod(JRubyCallSite site, String name, RubyClass cls, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        
        if (method.getHandle() != null) {
            nativeTarget = (MethodHandle)method.getHandle();
        } else {
            if (method instanceof AttrReaderMethod) {
                // Ruby to attr reader
                if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound as attr reader #" + method.getSerialNumber() + ":" + ((AttrReaderMethod)method).getVariableName());
                nativeTarget = createAttrReaderHandle(site, cls, method);
            } else if (method instanceof AttrWriterMethod) {
                // Ruby to attr writer
                if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound as attr writer #" + method.getSerialNumber() + ":" + ((AttrWriterMethod)method).getVariableName());
                nativeTarget = createAttrWriterHandle(site, cls, method);
            } else if (method.getNativeCall() != null) {
                DynamicMethod.NativeCall nativeCall = method.getNativeCall();
                
                if (nativeCall.isJava()) {
                    // Ruby to Java
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to Java method #" + method.getSerialNumber() + ": " + nativeCall);
                    nativeTarget = createJavaHandle(method);
                } else if (method instanceof CompiledMethod || method instanceof JittedMethod) {
                    // Ruby to Ruby
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to Ruby method #" + method.getSerialNumber() + ": " + nativeCall);
                    nativeTarget = createRubyHandle(site, method);
                } else {
                    // Ruby to Core
                    if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(name + "\tbound to native method #" + method.getSerialNumber() + ": " + nativeCall);
                    nativeTarget = createNativeHandle(site, method);
                }
            }
        }
                        
        // add NULL_BLOCK if needed
        if (nativeTarget != null) {
            if (
                    site.type().parameterCount() > 0
                    && site.type().parameterArray()[site.type().parameterCount() - 1] != Block.class
                    && nativeTarget.type().parameterCount() > 0
                    && nativeTarget.type().parameterType(nativeTarget.type().parameterCount() - 1) == Block.class) {
                nativeTarget = insertArguments(nativeTarget, nativeTarget.type().parameterCount() - 1, Block.NULL_BLOCK);
            } else if (
                    site.type().parameterCount() > 0
                    && site.type().parameterArray()[site.type().parameterCount() - 1] == Block.class
                    && nativeTarget.type().parameterCount() > 0
                    && nativeTarget.type().parameterType(nativeTarget.type().parameterCount() - 1) != Block.class) {
                // drop block if not used
                nativeTarget = dropArguments(nativeTarget, nativeTarget.type().parameterCount(), Block.class);
            }
        }
        
        return nativeTarget;
    }

    public static boolean testGeneration(int token, IRubyObject self) {
        return token == ((RubyBasicObject)self).getMetaClass().getGeneration();
    }

    public static boolean testMetaclass(RubyModule metaclass, IRubyObject self) {
        return metaclass == ((RubyBasicObject)self).getMetaClass();
    }
    
    public static IRubyObject getLast(IRubyObject[] args) {
        return args[args.length - 1];
    }
    
    private static final MethodHandle BLOCK_ESCAPE = findStatic(InvocationLinker.class, "blockEscape", methodType(IRubyObject.class, IRubyObject.class, Block.class));
    public static IRubyObject blockEscape(IRubyObject retval, Block block) {
        block.escape();
        return retval;
    }
    
    private static final MethodHandle BLOCK_ESCAPE_EXCEPTION = findStatic(InvocationLinker.class, "blockEscapeException", methodType(IRubyObject.class, Throwable.class, Block.class));
    public static IRubyObject blockEscapeException(Throwable throwable, Block block) throws Throwable {
        block.escape();
        throw throwable;
    }
    
    private static final MethodHandle HANDLE_BREAK_JUMP = findStatic(InvokeDynamicSupport.class, "handleBreakJump", methodType(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
//    
//    private static IRubyObject handleRetryJump(JumpException.RetryJump bj, ThreadContext context) {
//        block.escape();
//        throw context.getRuntime().newLocalJumpError(RubyLocalJumpError.Reason.RETRY, context.getRuntime().getNil(), "retry outside of rescue not supported");
//    }
//    private static final MethodHandle HANDLE_RETRY_JUMP = findStatic(InvokeDynamicSupport.class, "handleRetryJump", methodType(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
    
    private static MethodHandle postProcess(JRubyCallSite site, MethodHandle target) {
        if (site.isIterator()) {
            // wrap with iter logic for break, retry, and block escape
            MethodHandle breakHandler = permuteArguments(
                    HANDLE_BREAK_JUMP,
                    site.type().insertParameterTypes(0, JumpException.BreakJump.class),
                    new int[] {0, 1});
//            MethodHandle retryHandler = permuteArguments(
//                    HANDLE_RETRY_JUMP,
//                    site.type().insertParameterTypes(0, JumpException.RetryJump.class),
//                    new int[] {0, 1, site.type().parameterCount()});
//            MethodHandle blockEscape = permuteArguments(
//                    breakHandler,
//                    site.type().insertParameterTypes(0, Throwable.class),
//                    new int[] {0, site.type().parameterCount()});
            target = catchException(target, JumpException.BreakJump.class, breakHandler);
//            target = catchException(target, JumpException.RetryJump.class, retryHandler);
//            target = catchException(target, Throwable.class, retryHandler);
            target = catchException(
                    target,
                    Throwable.class,
                    permuteArguments(BLOCK_ESCAPE_EXCEPTION, site.type().insertParameterTypes(0, Throwable.class), new int[] {0, site.type().parameterCount()}));
            target = foldArguments(
                    permuteArguments(BLOCK_ESCAPE, site.type().insertParameterTypes(0, IRubyObject.class), new int[] {0, site.type().parameterCount()}),
                    target);
        }
        
        // if it's an attr assignment as an expression, need to return n-1th argument
        if (site.isAttrAssign() && site.isExpression()) {
            // return given argument
            MethodHandle newTarget = identity(IRubyObject.class);
            
            // if args are IRubyObject[].class, yank out n-1th
            if (site.type().parameterArray()[site.type().parameterCount() - 1] == IRubyObject[].class) {
                newTarget = filterArguments(newTarget, 0, findStatic(InvocationLinker.class, "getLast", methodType(IRubyObject.class, IRubyObject[].class))); 
            }
            
            // drop standard preamble args plus extra args
            newTarget = dropArguments(newTarget, 0, IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class);
            
            // drop extra arguments, if any
            MethodType dropped = target.type().dropParameterTypes(0, 4);
            if (dropped.parameterCount() > 1) {
                Class[] drops = new Class[dropped.parameterCount() - 1];
                Arrays.fill(drops, IRubyObject.class);
                newTarget = dropArguments(newTarget, 5, drops);
            }
            
            // fold using target
            target = foldArguments(newTarget, target);
        }
        
        return target;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Inline-caching failure paths, for megamorphic call sites
    ////////////////////////////////////////////////////////////////////////////

    public static IRubyObject fail(JRubyCallSite site, 
            ThreadContext context,
            IRubyObject caller,
            IRubyObject self,
            String name) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, args);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, args);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, args);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        
        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
        }
    }

    public static IRubyObject fail(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;

        if (entry.typeOk(selfClass)) {
            return entry.method.call(context, self, selfClass, name, args, block);
        } else {
            entry = selfClass.searchWithCache(name);
            if (methodMissing(entry, site.callType(), name, caller)) {
                return callMethodMissing(entry, site.callType(), context, self, name, args, block);
            }
            site.entry = entry;
            return entry.method.call(context, self, selfClass, name, args, block);
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, arg0, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, arg0, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, arg0, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, arg0, arg1, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, arg0, arg1, arg2, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, arg0, arg1, arg2, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }

    public static IRubyObject failIter(JRubyCallSite site, ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, Block block) throws Throwable {
        RubyClass selfClass = pollAndGetClass(context, self);
        CacheEntry entry = site.entry;
        try {
            if (entry.typeOk(selfClass)) {
                return entry.method.call(context, self, selfClass, name, args, block);
            } else {
                entry = selfClass.searchWithCache(name);
                if (methodMissing(entry, site.callType(), name, caller)) {
                    return callMethodMissing(entry, site.callType(), context, self, name, args, block);
                }
                site.entry = entry;
                return entry.method.call(context, self, selfClass, name, args, block);
            }
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(context, bj);
        } catch (JumpException.RetryJump rj) {
            return retryJumpError(context);
        } finally {
            block.escape();
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via DynamicMethod#call
    ////////////////////////////////////////////////////////////////////////////
    
    private static MethodHandle getDynamicMethodTarget(MethodType callType, int arity) {
        MethodHandle target;
        Class lastParam = callType.parameterType(callType.parameterCount() - 1);
        boolean block = lastParam == Block.class;
        switch (arity) {
            case 0:
                target = block ? TARGET_0_B : TARGET_0;
                break;
            case 1:
                target = block ? TARGET_1_B : TARGET_1;
                break;
            case 2:
                target = block ? TARGET_2_B : TARGET_2;
                break;
            case 3:
                target = block ? TARGET_3_B : TARGET_3;
                break;
            default:
                target = block ? TARGET_N_B : TARGET_N;
        }
        
        return target;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch from Ruby to Java via Java integration
    ////////////////////////////////////////////////////////////////////////////
    
    private static MethodHandle createJavaHandle(DynamicMethod method) {
        MethodHandle nativeTarget = null;
        MethodHandle returnFilter = null;
        
        Ruby runtime = method.getImplementationClass().getRuntime();
        DynamicMethod.NativeCall nativeCall = method.getNativeCall();
        
        if (nativeCall.isStatic()) {
            nativeTarget = findStatic(nativeCall.getNativeTarget(), nativeCall.getNativeName(), methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature()));
            
            if (nativeCall.getNativeSignature().length == 0) {
                // handle return value
                if (
                        nativeCall.getNativeReturn() == byte.class ||
                        nativeCall.getNativeReturn() == short.class ||
                        nativeCall.getNativeReturn() == char.class ||
                        nativeCall.getNativeReturn() == int.class ||
                        nativeCall.getNativeReturn() == long.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(long.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFixnum.class, "newFixnum", methodType(RubyFixnum.class, Ruby.class, long.class)),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == Byte.class ||
                        nativeCall.getNativeReturn() == Short.class ||
                        nativeCall.getNativeReturn() == Character.class ||
                        nativeCall.getNativeReturn() == Integer.class ||
                        nativeCall.getNativeReturn() == Long.class) {
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "fixnumOrNil", methodType(IRubyObject.class, Ruby.class, nativeCall.getNativeReturn())),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == float.class ||
                        nativeCall.getNativeReturn() == double.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(double.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFloat.class, "newFloat", methodType(RubyFloat.class, Ruby.class, double.class)),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == Float.class ||
                        nativeCall.getNativeReturn() == Double.class) {
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "floatOrNil", methodType(RubyFloat.class, Ruby.class, nativeCall.getNativeReturn())),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == boolean.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(boolean.class));
                    returnFilter = insertArguments(
                            findStatic(RubyBoolean.class, "newBoolean", methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == Boolean.class) {
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "booleanOrNil", methodType(IRubyObject.class, Ruby.class, Boolean.class)),
                            0,
                            runtime);
                } else if (CharSequence.class.isAssignableFrom(nativeCall.getNativeReturn())) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(CharSequence.class));
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "stringOrNil", methodType(IRubyObject.class, Ruby.class, CharSequence.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == void.class) {
                    returnFilter = constant(IRubyObject.class, runtime.getNil());
                }

                // we can handle this; do remaining transforms and return
                if (returnFilter != null) {
                    nativeTarget = filterReturnValue(nativeTarget, returnFilter);
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(IRubyObject.class));
                    nativeTarget = dropArguments(nativeTarget, 0, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class);
                    
                    method.setHandle(nativeTarget);
                    return nativeTarget;
                }
            }
        } else {
            nativeTarget = findVirtual(nativeCall.getNativeTarget(), nativeCall.getNativeName(), methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature()));
            
            if (nativeCall.getNativeSignature().length == 0) {
                // convert target
                nativeTarget = filterArguments(
                        nativeTarget,
                        0,
                        explicitCastArguments(
                                findStatic(JavaUtil.class, "objectFromJavaProxy", methodType(Object.class, IRubyObject.class)),
                                methodType(nativeCall.getNativeTarget(), IRubyObject.class)));
                
                // handle return value
                if (
                        nativeCall.getNativeReturn() == byte.class ||
                        nativeCall.getNativeReturn() == short.class ||
                        nativeCall.getNativeReturn() == char.class ||
                        nativeCall.getNativeReturn() == int.class ||
                        nativeCall.getNativeReturn() == long.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(long.class, IRubyObject.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFixnum.class, "newFixnum", methodType(RubyFixnum.class, Ruby.class, long.class)),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == Byte.class ||
                        nativeCall.getNativeReturn() == Short.class ||
                        nativeCall.getNativeReturn() == Character.class ||
                        nativeCall.getNativeReturn() == Integer.class ||
                        nativeCall.getNativeReturn() == Long.class) {
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "fixnumOrNil", methodType(IRubyObject.class, Ruby.class, nativeCall.getNativeReturn())),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == float.class ||
                        nativeCall.getNativeReturn() == double.class) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(double.class, IRubyObject.class));
                    returnFilter = insertArguments(
                            findStatic(RubyFloat.class, "newFloat", methodType(RubyFloat.class, Ruby.class, double.class)),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == Float.class ||
                        nativeCall.getNativeReturn() == Double.class) {
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "floatOrNil", methodType(RubyFloat.class, Ruby.class, nativeCall.getNativeReturn())),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == boolean.class) {
                    returnFilter = insertArguments(
                            findStatic(RubyBoolean.class, "newBoolean", methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                            0,
                            runtime);
                } else if (
                        nativeCall.getNativeReturn() == Boolean.class) {
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "booleanOrNil", methodType(IRubyObject.class, Ruby.class, Boolean.class)),
                            0,
                            runtime);
                } else if (CharSequence.class.isAssignableFrom(nativeCall.getNativeReturn())) {
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(CharSequence.class, IRubyObject.class));
                    returnFilter = insertArguments(
                            findStatic(InvocationLinker.class, "stringOrNil", methodType(IRubyObject.class, Ruby.class, CharSequence.class)),
                            0,
                            runtime);
                } else if (nativeCall.getNativeReturn() == void.class) {
                    returnFilter = constant(IRubyObject.class, runtime.getNil());
                }

                // we can handle this; do remaining transforms and return
                if (returnFilter != null) {
                    nativeTarget = filterReturnValue(nativeTarget, returnFilter);
                    nativeTarget = explicitCastArguments(nativeTarget, methodType(IRubyObject.class, IRubyObject.class));
                    nativeTarget = permuteArguments(
                            nativeTarget,
                            STANDARD_NATIVE_TYPE_BLOCK,
                            SELF_PERMUTE);
                    
                    method.setHandle(nativeTarget);
                    return nativeTarget;
                }
            }
        }
        
        return null;
    }
    
    public static IRubyObject fixnumOrNil(Ruby runtime, Byte b) {
        return b == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, b);
    }
    
    public static IRubyObject fixnumOrNil(Ruby runtime, Short s) {
        return s == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, s);
    }
    
    public static IRubyObject fixnumOrNil(Ruby runtime, Character c) {
        return c == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, c);
    }
    
    public static IRubyObject fixnumOrNil(Ruby runtime, Integer i) {
        return i == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, i);
    }
    
    public static IRubyObject fixnumOrNil(Ruby runtime, Long l) {
        return l == null ? runtime.getNil() : RubyFixnum.newFixnum(runtime, l);
    }
    
    public static IRubyObject floatOrNil(Ruby runtime, Float f) {
        return f == null ? runtime.getNil() : RubyFloat.newFloat(runtime, f);
    }
    
    public static IRubyObject floatOrNil(Ruby runtime, Double d) {
        return d == null ? runtime.getNil() : RubyFloat.newFloat(runtime, d);
    }
    
    public static IRubyObject booleanOrNil(Ruby runtime, Boolean b) {
        return b == null ? runtime.getNil() : RubyBoolean.newBoolean(runtime, b);
    }
    
    public static IRubyObject stringOrNil(Ruby runtime, CharSequence cs) {
        return cs == null ? runtime.getNil() : RubyString.newUnicodeString(runtime, cs);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via direct handle to native core method
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createNativeHandle(JRubyCallSite site, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        
        if (method.getCallConfig() == CallConfiguration.FrameNoneScopeNone) {
            DynamicMethod.NativeCall nativeCall = method.getNativeCall();
            Class[] nativeSig = nativeCall.getNativeSignature();
            boolean isStatic = nativeCall.isStatic();
            
            try {
                if (isStatic) {
                    nativeTarget = site.lookup().findStatic(
                            nativeCall.getNativeTarget(),
                            nativeCall.getNativeName(),
                            methodType(nativeCall.getNativeReturn(),
                            nativeCall.getNativeSignature()));
                } else {
                    nativeTarget = site.lookup().findVirtual(
                            nativeCall.getNativeTarget(),
                            nativeCall.getNativeName(),
                            methodType(nativeCall.getNativeReturn(),
                            nativeCall.getNativeSignature()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            int argCount = getArgCount(nativeCall.getNativeSignature(), isStatic);
            MethodType inboundType = STANDARD_NATIVE_TYPES_BLOCK[argCount];

            int[] permute;
            MethodType convert;
            if (nativeSig.length > 0 && nativeSig[0] == ThreadContext.class) {
                if (nativeSig[nativeSig.length - 1] == Block.class) {
                    convert = isStatic ? TARGET_TC_SELF_ARGS_BLOCK[argCount] : TARGET_SELF_TC_ARGS_BLOCK[argCount];
                    permute = isStatic ? TC_SELF_ARGS_BLOCK_PERMUTES[argCount] : SELF_TC_ARGS_BLOCK_PERMUTES[argCount];
                } else {
                    convert = isStatic ? TARGET_TC_SELF_ARGS[argCount] : TARGET_SELF_TC_ARGS[argCount];
                    permute = isStatic ? TC_SELF_ARGS_PERMUTES[argCount] : SELF_TC_ARGS_PERMUTES[argCount];
                }
            } else {
                if (nativeSig.length > 0 && nativeSig[nativeSig.length - 1] == Block.class) {
                    convert = TARGET_SELF_ARGS_BLOCK[argCount];
                    permute = SELF_ARGS_BLOCK_PERMUTES[argCount];
                } else {
                    convert = TARGET_SELF_ARGS[argCount];
                    permute = SELF_ARGS_PERMUTES[argCount];
                }
            }

            nativeTarget = explicitCastArguments(nativeTarget, convert);
            nativeTarget = permuteArguments(nativeTarget, inboundType, permute);
            method.setHandle(nativeTarget);
            return nativeTarget;
        }
        
        // can't build native handle for it
        return null;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch to attribute accessors
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createAttrReaderHandle(JRubyCallSite site, RubyClass cls, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        AttrReaderMethod attrReader = (AttrReaderMethod)method;
        String varName = attrReader.getVariableName();
        
        RubyClass.VariableAccessor accessor = cls.getRealClass().getVariableAccessorForRead(varName);
        
        MethodHandle target = findVirtual(IRubyObject.class, "getVariable", methodType(Object.class, int.class));
        target = insertArguments(target, 1, accessor.getIndex());
        target = explicitCastArguments(target, methodType(IRubyObject.class, IRubyObject.class));
        MethodHandle filter = findStatic(InvocationLinker.class, "valueOrNil", methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class));
        filter = insertArguments(filter, 1, cls.getRuntime().getNil());
        target = filterReturnValue(target, filter);
        target = permuteArguments(target, site.type(), new int[] {2});
        
        return target;
    }
    
    public static IRubyObject valueOrNil(IRubyObject value, IRubyObject nil) {
        return value == null ? nil : value;
    }

    private static MethodHandle createAttrWriterHandle(JRubyCallSite site, RubyClass cls, DynamicMethod method) {
        MethodHandle nativeTarget = null;
        AttrWriterMethod attrWriter = (AttrWriterMethod)method;
        String varName = attrWriter.getVariableName();
        
        RubyClass.VariableAccessor accessor = cls.getRealClass().getVariableAccessorForWrite(varName);
        
        MethodHandle target = findVirtual(IRubyObject.class, "setVariable", methodType(void.class, int.class, Object.class));
        target = insertArguments(target, 1, accessor.getIndex());
        target = explicitCastArguments(target, methodType(void.class, IRubyObject.class, IRubyObject.class));
        target = filterReturnValue(target, constant(IRubyObject.class, cls.getRuntime().getNil()));
        target = permuteArguments(target, site.type(), new int[] {2, 4});
        
        return target;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via direct handle to Ruby method
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle createRubyHandle(JRubyCallSite site, DynamicMethod method) {
        DynamicMethod.NativeCall nativeCall = method.getNativeCall();
        MethodHandle nativeTarget;
        
        try {
            nativeTarget = site.lookup().findStatic(
                    nativeCall.getNativeTarget(),
                    nativeCall.getNativeName(),
                    methodType(nativeCall.getNativeReturn(),
                    nativeCall.getNativeSignature()));
            Object scriptObject;
            if (method instanceof CompiledMethod) {
                scriptObject = ((CompiledMethod)method).getScriptObject();
            } else if (method instanceof JittedMethod) {
                scriptObject = ((JittedMethod)method).getScriptObject();
            } else {
                throw new RuntimeException("invalid method for ruby handle: " + method);
            }
            
            nativeTarget = insertArguments(nativeTarget, 0, scriptObject);
            
            // juggle args into correct places
            int argCount = getRubyArgCount(nativeCall.getNativeSignature());
            switch (argCount) {
                case 0:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_BLOCK, new int[] {0, 2, 4});
                    break;
                case -1:
                case 1:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_1ARG_BLOCK, new int[] {0, 2, 4, 5});
                    break;
                case 2:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_2ARG_BLOCK, new int[] {0, 2, 4, 5, 6});
                    break;
                case 3:
                    nativeTarget = permuteArguments(nativeTarget, STANDARD_NATIVE_TYPE_3ARG_BLOCK, new int[] {0, 2, 4, 5, 6, 7});
                    break;
                default:
                    throw new RuntimeException("unknown arg count: " + argCount);
            }
            
            method.setHandle(nativeTarget);
            return nativeTarget;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch via DynamicMethod.call to attribute access method
    ////////////////////////////////////////////////////////////////////////////

    private static MethodHandle getAttrTarget(JRubyCallSite site, DynamicMethod method) {
        MethodHandle target = (MethodHandle)method.getHandle();
        if (target != null) return target;
        
        try {
            if (method instanceof AttrReaderMethod) {
                AttrReaderMethod reader = (AttrReaderMethod)method;
                target = site.lookup().findVirtual(
                        AttrReaderMethod.class,
                        "call",
                        methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
                target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
                target = permuteArguments(
                        target,
                        methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class),
                        new int[] {0,2,4,1,5});
                // IRubyObject, DynamicMethod, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = insertArguments(target, 0, reader);
                // IRubyObject, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = foldArguments(target, PGC2_0);
                // IRubyObject, ThreadContext, IRubyObject, IRubyObject, String
            } else {
                AttrWriterMethod writer = (AttrWriterMethod)method;
                target = site.lookup().findVirtual(
                        AttrWriterMethod.class,
                        "call",
                        methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
                target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
                target = permuteArguments(
                        target,
                        methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class),
                        new int[] {0,2,4,1,5,6});
                // IRubyObject, DynamicMethod, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = insertArguments(target, 0, writer);
                // IRubyObject, RubyClass, ThreadContext, IRubyObject, IRubyObject, String
                target = foldArguments(target, PGC2_1);
                // IRubyObject, ThreadContext, IRubyObject, IRubyObject, String
            }
            method.setHandle(target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Additional support code
    ////////////////////////////////////////////////////////////////////////////

    private static int getArgCount(Class[] args, boolean isStatic) {
        int length = args.length;
        boolean hasContext = false;
        if (isStatic) {
            if (args.length > 1 && args[0] == ThreadContext.class) {
                length--;
                hasContext = true;
            }
            
            // remove self object
            assert args.length >= 1;
            length--;

            if (args.length > 1 && args[args.length - 1] == Block.class) {
                length--;
            }
            
            if (length == 1) {
                if (hasContext && args[2] == IRubyObject[].class) {
                    length = 4;
                } else if (args[1] == IRubyObject[].class) {
                    length = 4;
                }
            }
        } else {
            if (args.length > 0 && args[0] == ThreadContext.class) {
                length--;
                hasContext = true;
            }

            if (args.length > 0 && args[args.length - 1] == Block.class) {
                length--;
            }

            if (length == 1) {
                if (hasContext && args[1] == IRubyObject[].class) {
                    length = 4;
                } else if (args[0] == IRubyObject[].class) {
                    length = 4;
                }
            }
        }
        return length;
    }

    private static int getRubyArgCount(Class[] args) {
        int length = args.length;
        boolean hasContext = false;
        
        // remove script object
        length--;
        
        if (args.length > 2 && args[1] == ThreadContext.class) {
            length--;
            hasContext = true;
        }

        // remove self object
        assert args.length >= 2;
        length--;

        if (args.length > 2 && args[args.length - 1] == Block.class) {
            length--;
        }

        if (length == 1) {
            if (hasContext && args[3] == IRubyObject[].class) {
                length = 4;
            } else if (args[2] == IRubyObject[].class) {
                length = 4;
            }
        }

        return length;
    }

    private static final MethodHandle GETMETHOD;
    static {
        MethodHandle getMethod = findStatic(InvocationLinker.class, "getMethod", methodType(DynamicMethod.class, CacheEntry.class));
        getMethod = dropArguments(getMethod, 0, RubyClass.class);
        getMethod = dropArguments(getMethod, 2, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        GETMETHOD = getMethod;
    }

    public static DynamicMethod getMethod(CacheEntry entry) {
        return entry.method;
    }

    private static final MethodHandle PGC = dropArguments(
            dropArguments(
                findStatic(InvokeDynamicSupport.class, "pollAndGetClass",
                    methodType(RubyClass.class, ThreadContext.class, IRubyObject.class)),
                1,
                IRubyObject.class),
            0,
            CacheEntry.class);

    private static final MethodHandle PGC2 = dropArguments(
            findStatic(InvokeDynamicSupport.class, "pollAndGetClass",
                methodType(RubyClass.class, ThreadContext.class, IRubyObject.class)),
            1,
            IRubyObject.class);

    private static final MethodHandle TEST_GENERATION = dropArguments(
            findStatic(InvocationLinker.class, "testGeneration",
                methodType(boolean.class, int.class, IRubyObject.class)),
            1,
            ThreadContext.class, IRubyObject.class);

    private static final MethodHandle TEST_METACLASS = dropArguments(
            findStatic(InvocationLinker.class, "testMetaclass",
                methodType(boolean.class, RubyModule.class, IRubyObject.class)),
            1,
            ThreadContext.class, IRubyObject.class);
    
    private static final MethodHandle TEST =
            RubyInstanceConfig.INVOKEDYNAMIC_INVOCATION_SWITCHPOINT ?
            TEST_METACLASS :
            TEST_GENERATION;

    private static MethodHandle dropNameAndArgs(MethodHandle original, int index, int count, boolean block) {
        switch (count) {
        case -1:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject[].class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject[].class);
            }
        case 0:
            if (block) {
                return dropArguments(original, index, String.class, Block.class);
            } else {
                return dropArguments(original, index, String.class);
            }
        case 1:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject.class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject.class);
            }
        case 2:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class);
            }
        case 3:
            if (block) {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
            } else {
                return dropArguments(original, index, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class);
            }
        default:
            throw new RuntimeException("Invalid arg count (" + count + ") while preparing method handle:\n\t" + original);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Support handles for DynamicMethod.call paths
    ////////////////////////////////////////////////////////////////////////////

    private static final MethodHandle PGC_0 = dropNameAndArgs(PGC, 4, 0, false);
    private static final MethodHandle PGC2_0 = dropNameAndArgs(PGC2, 3, 0, false);
    private static final MethodHandle GETMETHOD_0 = dropNameAndArgs(GETMETHOD, 5, 0, false);
    private static final MethodHandle TEST_0 = dropNameAndArgs(TEST, 4, 0, false);
    private static final MethodHandle TARGET_0;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class),
                new int[] {0,3,5,1,6});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        target = foldArguments(target, GETMETHOD_0);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        target = foldArguments(target, PGC_0);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String
        TARGET_0 = target;
    }
    private static final MethodHandle FALLBACK_0 = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
    private static final MethodHandle FAIL_0 = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));

    private static final MethodHandle PGC_1 = dropNameAndArgs(PGC, 4, 1, false);
    private static final MethodHandle PGC2_1 = dropNameAndArgs(PGC2, 3, 1, false);
    private static final MethodHandle GETMETHOD_1 = dropNameAndArgs(GETMETHOD, 5, 1, false);
    private static final MethodHandle TEST_1 = dropNameAndArgs(TEST, 4, 1, false);
    private static final MethodHandle TARGET_1;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
        // IRubyObject, DynamicMethod, ThreadContext, IRubyObject, RubyModule, String, IRubyObject
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class));
        // IRubyObject, DynamicMethod, ThreadContext, IRubyObject, RubyClass, String, IRubyObject
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        target = foldArguments(target, GETMETHOD_1);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        target = foldArguments(target, PGC_1);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, IRubyObject
        TARGET_1 = target;
    }
    private static final MethodHandle FALLBACK_1 = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
    private static final MethodHandle FAIL_1 = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));

    private static final MethodHandle PGC_2 = dropNameAndArgs(PGC, 4, 2, false);
    private static final MethodHandle GETMETHOD_2 = dropNameAndArgs(GETMETHOD, 5, 2, false);
    private static final MethodHandle TEST_2 = dropNameAndArgs(TEST, 4, 2, false);
    private static final MethodHandle TARGET_2;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_2);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_2);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_2 = target;
    }
    private static final MethodHandle FALLBACK_2 = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FAIL_2 = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle PGC_3 = dropNameAndArgs(PGC, 4, 3, false);
    private static final MethodHandle GETMETHOD_3 = dropNameAndArgs(GETMETHOD, 5, 3, false);
    private static final MethodHandle TEST_3 = dropNameAndArgs(TEST, 4, 3, false);
    private static final MethodHandle TARGET_3;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,3,5,1,6,7,8,9});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_3);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_3);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_3 = target;
    }
    private static final MethodHandle FALLBACK_3 = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));
    private static final MethodHandle FAIL_3 = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class));

    private static final MethodHandle PGC_N = dropNameAndArgs(PGC, 4, -1, false);
    private static final MethodHandle GETMETHOD_N = dropNameAndArgs(GETMETHOD, 5, -1, false);
    private static final MethodHandle TEST_N = dropNameAndArgs(TEST, 4, -1, false);
    private static final MethodHandle TARGET_N;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_N);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_N);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        TARGET_N = target;
    }
    private static final MethodHandle FALLBACK_N = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));
    private static final MethodHandle FAIL_N = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class));

    private static final MethodHandle BREAKJUMP;
    static {
        MethodHandle breakJump = findStatic(
                InvokeDynamicSupport.class,
                "handleBreakJump",
                methodType(IRubyObject.class, JumpException.BreakJump.class, ThreadContext.class));
        // BreakJump, ThreadContext
        breakJump = permuteArguments(
                breakJump,
                methodType(IRubyObject.class, JumpException.BreakJump.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class),
                new int[] {0,2});
        // BreakJump, CacheEntry, ThreadContext, IRubyObject, IRubyObject
        BREAKJUMP = breakJump;
    }

    private static final MethodHandle RETRYJUMP;
    static {
        MethodHandle retryJump = findStatic(
                InvokeDynamicSupport.class,
                "retryJumpError",
                methodType(IRubyObject.class, ThreadContext.class));
        // ThreadContext
        retryJump = permuteArguments(
                retryJump,
                methodType(IRubyObject.class, JumpException.RetryJump.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class),
                new int[] {2});
        // RetryJump, CacheEntry, ThreadContext, IRubyObject, IRubyObject
        RETRYJUMP = retryJump;
    }

    private static final MethodHandle PGC_0_B = dropNameAndArgs(PGC, 4, 0, true);
    private static final MethodHandle GETMETHOD_0_B = dropNameAndArgs(GETMETHOD, 5, 0, true);
    private static final MethodHandle TEST_0_B = dropNameAndArgs(TEST, 4, 0, true);
    private static final MethodHandle TARGET_0_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class),
                new int[] {0,3,5,1,6,7});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_0_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_0_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        TARGET_0_B = target;
    }
    private static final MethodHandle FALLBACK_0_B = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle FAIL_0_B = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));
    private static final MethodHandle FAIL_ITER_0_B = findStatic(InvocationLinker.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, Block.class));

    private static final MethodHandle PGC_1_B = dropNameAndArgs(PGC, 4, 1, true);
    private static final MethodHandle GETMETHOD_1_B = dropNameAndArgs(GETMETHOD, 5, 1, true);
    private static final MethodHandle TEST_1_B = dropNameAndArgs(TEST, 4, 1, true);
    private static final MethodHandle TARGET_1_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_1_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_1_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        TARGET_1_B = target;
    }
    private static final MethodHandle FALLBACK_1_B = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_1_B = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_ITER_1_B = findStatic(InvocationLinker.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_2_B = dropNameAndArgs(PGC, 4, 2, true);
    private static final MethodHandle GETMETHOD_2_B = dropNameAndArgs(GETMETHOD, 5, 2, true);
    private static final MethodHandle TEST_2_B = dropNameAndArgs(TEST, 4, 2, true);
    private static final MethodHandle TARGET_2_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8,9});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_2_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_2_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args

        TARGET_2_B = target;
    }
    private static final MethodHandle FALLBACK_2_B = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_2_B = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_ITER_2_B = findStatic(InvocationLinker.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_3_B = dropNameAndArgs(PGC, 4, 3, true);
    private static final MethodHandle GETMETHOD_3_B = dropNameAndArgs(GETMETHOD, 5, 3, true);
    private static final MethodHandle TEST_3_B = dropNameAndArgs(TEST, 4, 3, true);
    private static final MethodHandle TARGET_3_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class),
                new int[] {0,3,5,1,6,7,8,9,10});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_3_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_3_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        
        TARGET_3_B = target;
    }
    private static final MethodHandle FALLBACK_3_B = findStatic(InvocationLinker.class, "invocationFallback",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_3_B = findStatic(InvocationLinker.class, "fail",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));
    private static final MethodHandle FAIL_ITER_3_B = findStatic(InvocationLinker.class, "failIter",
            methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class));

    private static final MethodHandle PGC_N_B = dropNameAndArgs(PGC, 4, -1, true);
    private static final MethodHandle GETMETHOD_N_B = dropNameAndArgs(GETMETHOD, 5, -1, true);
    private static final MethodHandle TEST_N_B = dropNameAndArgs(TEST, 4, -1, true);
    private static final MethodHandle TARGET_N_B;
    static {
        MethodHandle target = findVirtual(DynamicMethod.class, "call",
                methodType(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));
        target = explicitCastArguments(target, methodType(IRubyObject.class, DynamicMethod.class, ThreadContext.class, IRubyObject.class, RubyClass.class, String.class, IRubyObject[].class, Block.class));
        target = permuteArguments(
                target,
                methodType(IRubyObject.class, DynamicMethod.class, RubyClass.class, CacheEntry.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class),
                new int[] {0,3,5,1,6,7,8});
        // IRubyObject, DynamicMethod, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, GETMETHOD_N_B);
        // IRubyObject, RubyClass, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        target = foldArguments(target, PGC_N_B);
        // IRubyObject, CacheEntry, ThreadContext, IRubyObject, IRubyObject, String, args
        
        TARGET_N_B = target;
    }
    private static final MethodHandle FALLBACK_N_B = findStatic(InvocationLinker.class, "invocationFallback",
                    methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FAIL_N_B = findStatic(InvocationLinker.class, "fail",
                    methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    private static final MethodHandle FAIL_ITER_N_B = findStatic(InvocationLinker.class, "failIter",
                    methodType(IRubyObject.class, JRubyCallSite.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject[].class, Block.class));
    
    private static final MethodHandle[] TESTS = new MethodHandle[] {
        TEST_0,
        TEST_1,
        TEST_2,
        TEST_3,
        TEST_N
    };
    
    private static final MethodHandle[] FALLBACKS = new MethodHandle[] {
        FALLBACK_0,
        FALLBACK_1,
        FALLBACK_2,
        FALLBACK_3,
        FALLBACK_N
    };
    
    private static final MethodHandle[] FAILS = new MethodHandle[] {
        FAIL_0,
        FAIL_1,
        FAIL_2,
        FAIL_3,
        FAIL_N
    };
    
    private static final MethodHandle[] TESTS_B = new MethodHandle[] {
        TEST_0_B,
        TEST_1_B,
        TEST_2_B,
        TEST_3_B,
        TEST_N_B
    };
    
    private static final MethodHandle[] FALLBACKS_B = new MethodHandle[] {
        FALLBACK_0_B,
        FALLBACK_1_B,
        FALLBACK_2_B,
        FALLBACK_3_B,
        FALLBACK_N_B
    };
    
    private static final MethodHandle[] FAILS_B = new MethodHandle[] {
        FAIL_0_B,
        FAIL_1_B,
        FAIL_2_B,
        FAIL_3_B,
        FAIL_N_B
    };
    
    ////////////////////////////////////////////////////////////////////////////
    // Support method types and permutations
    ////////////////////////////////////////////////////////////////////////////
    
    private static final MethodType STANDARD_NATIVE_TYPE = methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class, // caller
            IRubyObject.class, // self
            String.class // method name
            );
    private static final MethodType STANDARD_NATIVE_TYPE_1ARG = STANDARD_NATIVE_TYPE.appendParameterTypes(IRubyObject.class);
    private static final MethodType STANDARD_NATIVE_TYPE_2ARG = STANDARD_NATIVE_TYPE_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType STANDARD_NATIVE_TYPE_3ARG = STANDARD_NATIVE_TYPE_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType STANDARD_NATIVE_TYPE_NARG = STANDARD_NATIVE_TYPE.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] STANDARD_NATIVE_TYPES = {
        STANDARD_NATIVE_TYPE,
        STANDARD_NATIVE_TYPE_1ARG,
        STANDARD_NATIVE_TYPE_2ARG,
        STANDARD_NATIVE_TYPE_3ARG,
        STANDARD_NATIVE_TYPE_NARG,
    };
    
    private static final MethodType STANDARD_NATIVE_TYPE_BLOCK = STANDARD_NATIVE_TYPE.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_1ARG_BLOCK = STANDARD_NATIVE_TYPE_1ARG.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_2ARG_BLOCK = STANDARD_NATIVE_TYPE_2ARG.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_3ARG_BLOCK = STANDARD_NATIVE_TYPE_3ARG.appendParameterTypes(Block.class);
    private static final MethodType STANDARD_NATIVE_TYPE_NARG_BLOCK = STANDARD_NATIVE_TYPE_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] STANDARD_NATIVE_TYPES_BLOCK = {
        STANDARD_NATIVE_TYPE_BLOCK,
        STANDARD_NATIVE_TYPE_1ARG_BLOCK,
        STANDARD_NATIVE_TYPE_2ARG_BLOCK,
        STANDARD_NATIVE_TYPE_3ARG_BLOCK,
        STANDARD_NATIVE_TYPE_NARG_BLOCK,
    };
    
    private static final MethodType TARGET_SELF = methodType(
            IRubyObject.class, // return value
            IRubyObject.class // self
            );
    private static final MethodType TARGET_SELF_1ARG = TARGET_SELF.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_2ARG = TARGET_SELF_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_3ARG = TARGET_SELF_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_NARG = TARGET_SELF.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] TARGET_SELF_ARGS = {
        TARGET_SELF,
        TARGET_SELF_1ARG,
        TARGET_SELF_2ARG,
        TARGET_SELF_3ARG,
        TARGET_SELF_NARG,
    };
    
    private static final MethodType TARGET_SELF_BLOCK = TARGET_SELF.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_1ARG_BLOCK = TARGET_SELF_1ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_2ARG_BLOCK = TARGET_SELF_2ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_3ARG_BLOCK = TARGET_SELF_3ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_NARG_BLOCK = TARGET_SELF_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] TARGET_SELF_ARGS_BLOCK = {
        TARGET_SELF_BLOCK,
        TARGET_SELF_1ARG_BLOCK,
        TARGET_SELF_2ARG_BLOCK,
        TARGET_SELF_3ARG_BLOCK,
        TARGET_SELF_NARG_BLOCK,
    };
    
    private static final MethodType TARGET_SELF_TC = TARGET_SELF.appendParameterTypes(ThreadContext.class);
    private static final MethodType TARGET_SELF_TC_1ARG = TARGET_SELF_TC.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_TC_2ARG = TARGET_SELF_TC_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_TC_3ARG = TARGET_SELF_TC_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_SELF_TC_NARG = TARGET_SELF_TC.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] TARGET_SELF_TC_ARGS = {
        TARGET_SELF_TC,
        TARGET_SELF_TC_1ARG,
        TARGET_SELF_TC_2ARG,
        TARGET_SELF_TC_3ARG,
        TARGET_SELF_TC_NARG,
    };
    
    private static final MethodType TARGET_SELF_TC_BLOCK = TARGET_SELF_TC.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_1ARG_BLOCK = TARGET_SELF_TC_1ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_2ARG_BLOCK = TARGET_SELF_TC_2ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_3ARG_BLOCK = TARGET_SELF_TC_3ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_SELF_TC_NARG_BLOCK = TARGET_SELF_TC_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] TARGET_SELF_TC_ARGS_BLOCK = {
        TARGET_SELF_TC_BLOCK,
        TARGET_SELF_TC_1ARG_BLOCK,
        TARGET_SELF_TC_2ARG_BLOCK,
        TARGET_SELF_TC_3ARG_BLOCK,
        TARGET_SELF_TC_NARG_BLOCK,
    };
    
    private static final MethodType TARGET_TC_SELF = methodType(
            IRubyObject.class, // return value
            ThreadContext.class, //context
            IRubyObject.class // self
            );
    private static final MethodType TARGET_TC_SELF_1ARG = TARGET_TC_SELF.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_TC_SELF_2ARG = TARGET_TC_SELF_1ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_TC_SELF_3ARG = TARGET_TC_SELF_2ARG.appendParameterTypes(IRubyObject.class);
    private static final MethodType TARGET_TC_SELF_NARG = TARGET_TC_SELF.appendParameterTypes(IRubyObject[].class);
    private static final MethodType[] TARGET_TC_SELF_ARGS = {
        TARGET_TC_SELF,
        TARGET_TC_SELF_1ARG,
        TARGET_TC_SELF_2ARG,
        TARGET_TC_SELF_3ARG,
        TARGET_TC_SELF_NARG,
    };
    
    private static final MethodType TARGET_TC_SELF_BLOCK = TARGET_TC_SELF.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_1ARG_BLOCK = TARGET_TC_SELF_1ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_2ARG_BLOCK = TARGET_TC_SELF_2ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_3ARG_BLOCK = TARGET_TC_SELF_3ARG.appendParameterTypes(Block.class);
    private static final MethodType TARGET_TC_SELF_NARG_BLOCK = TARGET_TC_SELF_NARG.appendParameterTypes(Block.class);
    private static final MethodType[] TARGET_TC_SELF_ARGS_BLOCK = {
        TARGET_TC_SELF_BLOCK,
        TARGET_TC_SELF_1ARG_BLOCK,
        TARGET_TC_SELF_2ARG_BLOCK,
        TARGET_TC_SELF_3ARG_BLOCK,
        TARGET_TC_SELF_NARG_BLOCK
    };
    
    private static final int[] SELF_TC_PERMUTE = {2, 0};
    private static final int[] SELF_TC_1ARG_PERMUTE = {2, 0, 4};
    private static final int[] SELF_TC_2ARG_PERMUTE = {2, 0, 4, 5};
    private static final int[] SELF_TC_3ARG_PERMUTE = {2, 0, 4, 5, 6};
    private static final int[] SELF_TC_NARG_PERMUTE = {2, 0, 4};
    private static final int[][] SELF_TC_ARGS_PERMUTES = {
        SELF_TC_PERMUTE,
        SELF_TC_1ARG_PERMUTE,
        SELF_TC_2ARG_PERMUTE,
        SELF_TC_3ARG_PERMUTE,
        SELF_TC_NARG_PERMUTE
    };
    private static final int[] SELF_PERMUTE = {2};
    private static final int[] SELF_1ARG_PERMUTE = {2, 4};
    private static final int[] SELF_2ARG_PERMUTE = {2, 4, 5};
    private static final int[] SELF_3ARG_PERMUTE = {2, 4, 5, 6};
    private static final int[] SELF_NARG_PERMUTE = {2, 4};
    private static final int[][] SELF_ARGS_PERMUTES = {
        SELF_PERMUTE,
        SELF_1ARG_PERMUTE,
        SELF_2ARG_PERMUTE,
        SELF_3ARG_PERMUTE,
        SELF_NARG_PERMUTE
    };
    private static final int[] SELF_TC_BLOCK_PERMUTE = {2, 0, 4};
    private static final int[] SELF_TC_1ARG_BLOCK_PERMUTE = {2, 0, 4, 5};
    private static final int[] SELF_TC_2ARG_BLOCK_PERMUTE = {2, 0, 4, 5, 6};
    private static final int[] SELF_TC_3ARG_BLOCK_PERMUTE = {2, 0, 4, 5, 6, 7};
    private static final int[] SELF_TC_NARG_BLOCK_PERMUTE = {2, 0, 4, 5};
    private static final int[][] SELF_TC_ARGS_BLOCK_PERMUTES = {
        SELF_TC_BLOCK_PERMUTE,
        SELF_TC_1ARG_BLOCK_PERMUTE,
        SELF_TC_2ARG_BLOCK_PERMUTE,
        SELF_TC_3ARG_BLOCK_PERMUTE,
        SELF_TC_NARG_BLOCK_PERMUTE
    };
    private static final int[] SELF_BLOCK_PERMUTE = {2, 4};
    private static final int[] SELF_1ARG_BLOCK_PERMUTE = {2, 4, 5};
    private static final int[] SELF_2ARG_BLOCK_PERMUTE = {2, 4, 5, 6};
    private static final int[] SELF_3ARG_BLOCK_PERMUTE = {2, 4, 5, 6, 7};
    private static final int[] SELF_NARG_BLOCK_PERMUTE = {2, 4, 5};
    private static final int[][] SELF_ARGS_BLOCK_PERMUTES = {
        SELF_BLOCK_PERMUTE,
        SELF_1ARG_BLOCK_PERMUTE,
        SELF_2ARG_BLOCK_PERMUTE,
        SELF_3ARG_BLOCK_PERMUTE,
        SELF_NARG_BLOCK_PERMUTE
    };
    private static final int[] TC_SELF_PERMUTE = {0, 2};
    private static final int[] TC_SELF_1ARG_PERMUTE = {0, 2, 4};
    private static final int[] TC_SELF_2ARG_PERMUTE = {0, 2, 4, 5};
    private static final int[] TC_SELF_3ARG_PERMUTE = {0, 2, 4, 5, 6};
    private static final int[] TC_SELF_NARG_PERMUTE = {0, 2, 4};
    private static final int[][] TC_SELF_ARGS_PERMUTES = {
        TC_SELF_PERMUTE,
        TC_SELF_1ARG_PERMUTE,
        TC_SELF_2ARG_PERMUTE,
        TC_SELF_3ARG_PERMUTE,
        TC_SELF_NARG_PERMUTE,
    };
    private static final int[] TC_SELF_BLOCK_PERMUTE = {0, 2, 4};
    private static final int[] TC_SELF_1ARG_BLOCK_PERMUTE = {0, 2, 4, 5};
    private static final int[] TC_SELF_2ARG_BLOCK_PERMUTE = {0, 2, 4, 5, 6};
    private static final int[] TC_SELF_3ARG_BLOCK_PERMUTE = {0, 2, 4, 5, 6, 7};
    private static final int[] TC_SELF_NARG_BLOCK_PERMUTE = {0, 2, 4, 5};
    private static final int[][] TC_SELF_ARGS_BLOCK_PERMUTES = {
        TC_SELF_BLOCK_PERMUTE,
        TC_SELF_1ARG_BLOCK_PERMUTE,
        TC_SELF_2ARG_BLOCK_PERMUTE,
        TC_SELF_3ARG_BLOCK_PERMUTE,
        TC_SELF_NARG_BLOCK_PERMUTE,
    };
}
