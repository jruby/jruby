package org.jruby.ext.ffi.jffi;

import com.headius.invokebinder.Binder;
import com.kenai.jffi.CallContext;
import com.kenai.jffi.Platform;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.JRubyCallSite;
import org.jruby.util.CodegenUtils;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;

import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.findStatic;

/**
 *
 */
public final class InvokeDynamic {
    private static final Logger LOG = LoggerFactory.getLogger("ffi invokedynamic");
    private InvokeDynamic() {}


    public static MethodHandle getMethodHandle(JRubyCallSite site, DynamicMethod method) {
        MethodHandle fast = getFastNumericMethodHandle(site, method);
        if (fast == null) {
            return generateNativeInvokerHandle(site, method);
        }

        MethodHandle guard = getDirectPointerParameterGuard(site, method);
        return guard != null
            ? MethodHandles.guardWithTest(guard, fast, generateNativeInvokerHandle(site, method))
            : fast;
    }

    public static MethodHandle getFastNumericMethodHandle(JRubyCallSite site, DynamicMethod method) {
        Signature signature = (method instanceof NativeInvoker)
                ? ((NativeInvoker) method).getSignature()
                : ((DefaultMethod) method).getSignature();


        CallContext callContext = (method instanceof NativeInvoker)
                ? ((NativeInvoker) method).getCallContext()
                : ((DefaultMethod) method).getCallContext();

        long functionAddress = (method instanceof NativeInvoker)
                ? ((NativeInvoker) method).getFunctionAddress()
                : ((DefaultMethod) method).getFunctionAddress();

        MethodHandle nativeInvoker;
        Method invokerMethod;

        com.kenai.jffi.InvokeDynamicSupport.Invoker jffiInvoker = com.kenai.jffi.InvokeDynamicSupport.getFastNumericInvoker(callContext, functionAddress);
        nativeInvoker = (MethodHandle) jffiInvoker.getMethodHandle();
        invokerMethod = jffiInvoker.getMethod();

        Class nativeIntClass = invokerMethod.getReturnType();

        MethodHandle resultFilter = getResultFilter(method.getImplementationClass().getRuntime(), signature.getResultType().getNativeType(), nativeIntClass);
        if (resultFilter == null) {
            return null;
        }

        MethodHandle[] parameterFilters = getParameterFilters(signature, nativeIntClass);
        if (parameterFilters == null) {
            return null;
        }

        MethodHandle targetHandle = Binder.from(IRubyObject.class, CodegenUtils.params(IRubyObject.class, signature.getParameterCount()))
                .filter(0, parameterFilters)
                .filterReturn(resultFilter)
                .invoke(nativeInvoker);

        if (signature.getParameterCount() > 3) {
            // Expand the incoming IRubyObject[] parameter array to individual params
            targetHandle = targetHandle.asSpreader(IRubyObject[].class, signature.getParameterCount());
        }

        MethodHandle methodHandle = Binder.from(site.type())
                .drop(0, 3).invoke(targetHandle);

        if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name()
                + "\tbound to ffi method "
                + logMethod(method)
                + String.format("[function address=%x]: ", functionAddress)
                + invokerMethod);

        return methodHandle;
    }

    private static MethodHandle generateNativeInvokerHandle(JRubyCallSite site, DynamicMethod method) {
        if (method instanceof org.jruby.ext.ffi.jffi.DefaultMethod) {
            NativeInvoker nativeInvoker = ((org.jruby.ext.ffi.jffi.DefaultMethod) method).forceCompilation();
            if (nativeInvoker == null) {
                // Compilation failed, cannot build a native handle for it
                return null;
            }

            method = nativeInvoker;
        }

        if (method.getArity().isFixed() && method.getArity().getValue() <= 6 && method.getCallConfig() == CallConfiguration.FrameNoneScopeNone) {
            Class[] callMethodParameters = new Class[4 + method.getArity().getValue()];
            callMethodParameters[0] = ThreadContext.class;
            callMethodParameters[1] = IRubyObject.class;
            callMethodParameters[2] = RubyModule.class;
            callMethodParameters[3] = String.class;
            Arrays.fill(callMethodParameters, 4, callMethodParameters.length, IRubyObject.class);

            MethodHandle nativeTarget;
            try {
                nativeTarget = site.lookup().findVirtual(method.getClass(), "call",
                        methodType(IRubyObject.class, callMethodParameters));

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            int argCount = method.getArity().getValue();
            if (argCount > 3) {
                // Expand the incoming IRubyObject[] parameter array to individual params
                nativeTarget = nativeTarget.asSpreader(IRubyObject[].class, argCount);
            }

            nativeTarget = Binder.from(site.type())
                    .drop(1, 1)
                    .insert(2, method.getImplementationClass(), site.name())
                    .invoke(nativeTarget.bindTo(method));

            method.setHandle(nativeTarget);
            if (RubyInstanceConfig.LOG_INDY_BINDINGS) LOG.info(site.name() + "\tbound to ffi method "
                    + logMethod(method) + ": "
                    + IRubyObject.class.getSimpleName() + " "
                    + method.getClass().getSimpleName() + ".call"
                    + CodegenUtils.prettyShortParams(callMethodParameters));

            return nativeTarget;
        }


        // can't build native handle for it
        return null;
    }

    private static String logMethod(DynamicMethod method) {
        return "[#" + method.getSerialNumber() + " " + method.getImplementationClass() + "]";
    }

    private static MethodHandle findResultHelper(String name, Class nativeIntClass) {
        return findStatic(JITRuntime.class, name, MethodType.methodType(IRubyObject.class, Ruby.class, nativeIntClass));
    }

    private static MethodHandle getResultFilter(Ruby runtime, NativeType nativeType, Class nativeIntClass) {
        MethodHandle resultFilter;
        switch (nativeType) {
            case VOID:
                // just discard the int return value, and return nil
                return Binder.from(IRubyObject.class, nativeIntClass).drop(0).constant(runtime.getNil());

            case BOOL:
                resultFilter = findResultHelper("newBoolean", nativeIntClass);
                break;

            case CHAR:
                resultFilter = findResultHelper("newSigned8", nativeIntClass);
                break;

            case UCHAR:
                resultFilter = findResultHelper("newUnsigned8", nativeIntClass);
                break;

            case SHORT:
                resultFilter = findResultHelper("newSigned16", nativeIntClass);
                break;

            case USHORT:
                resultFilter = findResultHelper("newUnsigned16", nativeIntClass);
                break;

            case INT:
                resultFilter = findResultHelper("newSigned32", nativeIntClass);
                break;

            case UINT:
                resultFilter = findResultHelper("newUnsigned32", nativeIntClass);
                break;

            case LONG:
                resultFilter = findResultHelper("newSigned" + Platform.getPlatform().longSize(), nativeIntClass);
                break;

            case ULONG:
                resultFilter = findResultHelper("newUnsigned" + Platform.getPlatform().longSize(), nativeIntClass);
                break;

            case LONG_LONG:
                resultFilter = findResultHelper("newSigned64", nativeIntClass);
                break;

            case ULONG_LONG:
                resultFilter = findResultHelper("newUnsigned64", nativeIntClass);
                break;

            case FLOAT:
                resultFilter = findResultHelper("newFloat32", nativeIntClass);
                break;

            case DOUBLE:
                resultFilter = findResultHelper("newFloat64", nativeIntClass);
                break;

            case POINTER:
                resultFilter = findResultHelper("newPointer" + Platform.getPlatform().addressSize(), nativeIntClass);
                break;

            case STRING:
            case TRANSIENT_STRING:
                resultFilter = findResultHelper("newString", nativeIntClass);
                break;

            default:
                return null;
        }

        return MethodHandles.insertArguments(resultFilter, 0, runtime);
    }

    private static MethodHandle findParameterHelper(String name, Class nativeIntClass) {
        return findStatic(JITRuntime.class, name + (int.class == nativeIntClass ? 32 : 64),
                MethodType.methodType(nativeIntClass, IRubyObject.class));
    }

    private static MethodHandle[] getParameterFilters(Signature signature, Class nativeIntClass) {
        MethodHandle[] parameterFilters = new MethodHandle[signature.getParameterCount()];
        for (int i = 0; i < signature.getParameterCount(); i++) {
            if (!(signature.getParameterType(i) instanceof Type.Builtin)) {
                return null;
            }

            MethodHandle ph;
            switch (signature.getParameterType(i).getNativeType()) {
                case BOOL:
                    ph = findParameterHelper("boolValue", nativeIntClass);
                    break;

                case CHAR:
                    ph = findParameterHelper("s8Value", nativeIntClass);
                    break;

                case UCHAR:
                    ph = findParameterHelper("u8Value", nativeIntClass);
                    break;

                case SHORT:
                    ph = findParameterHelper("s16Value", nativeIntClass);
                    break;

                case USHORT:
                    ph = findParameterHelper("u16Value", nativeIntClass);
                    break;

                case INT:
                    ph = findParameterHelper("s32Value", nativeIntClass);
                    break;

                case UINT:
                    ph = findParameterHelper("u32Value", nativeIntClass);
                    break;

                case LONG:
                    ph = findParameterHelper("s" + Platform.getPlatform().longSize() + "Value", nativeIntClass);
                    break;

                case ULONG:
                    ph = findParameterHelper("u" + Platform.getPlatform().longSize() + "Value", nativeIntClass);
                    break;

                case LONG_LONG:
                    ph = findParameterHelper("s64Value", nativeIntClass);
                    break;

                case ULONG_LONG:
                    ph = findParameterHelper("u64Value", nativeIntClass);
                    break;

                case FLOAT:
                    ph = findParameterHelper("f32Value", nativeIntClass);
                    break;

                case DOUBLE:
                    ph = findParameterHelper("f64Value", nativeIntClass);
                    break;

                case POINTER:
                case BUFFER_IN:
                case BUFFER_OUT:
                case BUFFER_INOUT:
                    ph = findParameterHelper("pointerValue", nativeIntClass);
                    break;

                default:
                    return null;
            }

            parameterFilters[i] = ph;

        }
        return parameterFilters;
    }

    private static MethodHandle getDirectPointerParameterGuard(JRubyCallSite site, DynamicMethod method) {
        Signature signature = (method instanceof NativeInvoker)
                ? ((NativeInvoker) method).getSignature()
                : ((DefaultMethod) method).getSignature();
        MethodHandle[] guards = new MethodHandle[signature.getParameterCount()];
        Arrays.fill(guards, 0, guards.length, Binder.from(boolean.class, IRubyObject.class).drop(0, 1).constant(true));

        boolean guardNeeded = false;
        for (int i = 0; i < signature.getParameterCount(); i++) {
            switch (signature.getParameterType(i).getNativeType()) {
                case POINTER:
                case BUFFER_IN:
                case BUFFER_OUT:
                case BUFFER_INOUT:
                    guards[i] = findStatic(JITRuntime.class, "isDirectPointer", MethodType.methodType(boolean.class, IRubyObject.class));
                    guardNeeded = true;
                    break;

            }
        }

        if (!guardNeeded) {
            return null;
        }

        MethodHandle isTrue = findStatic(JITRuntime.class, "isTrue",
                methodType(boolean.class, CodegenUtils.params(boolean.class, signature.getParameterCount())));

        if (signature.getParameterCount() > 3) {
            // Expand the incoming IRubyObject[] parameter array to individual params
            isTrue = isTrue.asSpreader(IRubyObject[].class, signature.getParameterCount());
        }

        isTrue = Binder.from(boolean.class, CodegenUtils.params(IRubyObject.class, signature.getParameterCount()))
                .filter(0, guards)
                .invoke(isTrue);

        return Binder.from(site.type().changeReturnType(boolean.class))
                .drop(0, 3)
                .invoke(isTrue);
    }
}
