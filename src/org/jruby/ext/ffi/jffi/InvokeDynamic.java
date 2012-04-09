package org.jruby.ext.ffi.jffi;

import com.headius.invokebinder.Binder;
import com.kenai.jffi.CallContext;
import com.kenai.jffi.Invoker;
import com.kenai.jffi.Platform;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.runtime.invokedynamic.JRubyCallSite;
import org.jruby.util.CodegenUtils;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.findStatic;

/**
 *
 */
public class InvokeDynamic {
    private static final Logger LOG = LoggerFactory.getLogger("ffi invokedynamic");
    private InvokeDynamic() {}


    public static MethodHandle getMethodHandle(JRubyCallSite site, DynamicMethod method) {
        return getFastNumericMethodHandle(site, method);
    }

    public static MethodHandle getFastNumericMethodHandle(JRubyCallSite site, DynamicMethod method) {
        Signature signature = (method instanceof NativeInvoker)
                ? ((NativeInvoker) method).getSignature()
                : ((DefaultMethod) method).getSignature();

        JITSignature jitSignature = new JITSignature(signature);
        AbstractNumericMethodGenerator generator = getNumericMethodGenerator(jitSignature);
        if (generator == null) {
            return null;
        }

        Class nativeIntClass = generator.getInvokerIntType();

        MethodHandle resultFilter = getResultFilter(method.getImplementationClass().getRuntime(), signature.getResultType().getNativeType(), nativeIntClass);
        if (resultFilter == null) {
            return null;
        }

        MethodHandle[] parameterFilters = getParameterFilters(signature, nativeIntClass);
        if (parameterFilters == null) {
            return null;
        }

        Class[] invokerParams = new Class[2 + signature.getParameterCount()];
        invokerParams[0] = CallContext.class;
        invokerParams[1] = long.class;
        Arrays.fill(invokerParams, 2, invokerParams.length, nativeIntClass);

        MethodHandle nativeInvoker = InvokeDynamicSupport.findVirtual(Invoker.class,
                generator.getInvokerMethodName(jitSignature),
                MethodType.methodType(nativeIntClass, invokerParams));
        nativeInvoker = nativeInvoker.bindTo(Invoker.getInstance());


        CallContext callContext = (method instanceof NativeInvoker)
                ? ((NativeInvoker) method).getCallContext()
                : ((DefaultMethod) method).getCallContext();

        long functionAddress = (method instanceof NativeInvoker)
                ? ((NativeInvoker) method).getFunctionAddress()
                : ((DefaultMethod) method).getFunctionAddress();

        MethodHandle targetHandle = Binder.from(IRubyObject.class, CodegenUtils.params(IRubyObject.class, signature.getParameterCount()))
                .filter(0, parameterFilters)
                .insert(0, callContext, functionAddress)
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
                + generator.getInvokerIntType() + " "
                + Invoker.class.getName() + "." + generator.getInvokerMethodName(jitSignature)
                + CodegenUtils.prettyShortParams(invokerParams));

        return methodHandle;
    }

    private static AbstractNumericMethodGenerator getNumericMethodGenerator(JITSignature jitSignature) {
        // Co-opt the numeric method generators to figure out which jffi method to call
        AbstractNumericMethodGenerator[] generators = {
                new FastIntMethodGenerator(),
                new FastLongMethodGenerator(),
                new FastNumericMethodGenerator()
        };

        for (AbstractNumericMethodGenerator generator : generators) {
            if (generator.isSupported(jitSignature)) {
                return generator;
            }
        }

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

                default:
                    return null;
            }

            parameterFilters[i] = ph;

        }
        return parameterFilters;
    }
}
