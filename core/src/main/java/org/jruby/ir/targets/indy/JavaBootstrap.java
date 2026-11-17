package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyString;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.NativeCallMethod;
import org.jruby.java.invokers.SingletonMethodInvoker;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.explicitCastArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.findStatic;
import static org.jruby.runtime.invokedynamic.InvokeDynamicSupport.findVirtual;

public class JavaBootstrap {
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch from Ruby to Java via Java integration
    ////////////////////////////////////////////////////////////////////////////

    private static final Logger LOG = LoggerFactory.getLogger(JavaBootstrap.class);

    public static boolean subclassProxyTest(Object target) {
        return target instanceof ReifiedJavaProxy;
    }

    private static final MethodHandle IS_JAVA_SUBCLASS = findStatic(JavaBootstrap.class, "subclassProxyTest", methodType(boolean.class, Object.class));

    private static Object nullValue(Class type) {
        if (type == boolean.class || type == Boolean.class) return false;
        if (type == byte.class || type == Byte.class) return (byte)0;
        if (type == short.class || type == Short.class) return (short)0;
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == float.class || type == Float.class) return 0.0F;
        if (type == double.class || type == Double.class)return 0.0;
        return null;
    }

    public static MethodHandle createJavaHandle(InvokeSite site, DynamicMethod method) {
        if (!(method instanceof NativeCallMethod)) return null;

        DynamicMethod.NativeCall nativeCall = ((NativeCallMethod) method).getNativeCall();

        if (nativeCall == null) return null;

        boolean isStatic = nativeCall.isStatic();

        // This logic does not handle closure conversion yet
        if (site.signature.lastArgType() == Block.class) {
            if (Options.INVOKEDYNAMIC_LOG_BINDING.load()) {
                LOG.info(site.name() + "\tpassed a closure to Java method " + nativeCall + ": " + Bootstrap.logMethod(method));
            }
            return null;
        }

        // mismatched arity not supported
        if (site.arity != nativeCall.getNativeSignature().length) {
            return null;
        }

        // varargs broken, so ignore methods that take a trailing array
        Class[] signature = nativeCall.getNativeSignature();
        if (signature.length > 0 && signature[signature.length - 1].isArray()) {
            return null;
        }

        // Scala singletons have slightly different JI logic, so skip for now
        if (method instanceof SingletonMethodInvoker) return null;

        // get prepared handle if previously cached
        MethodHandle nativeTarget = (MethodHandle)method.getHandle();

        if (nativeTarget == null) {
            // no cache, proceed to construct the adapted handle

            // the "apparent" type from the NativeCall, excluding receiver
            MethodType apparentType = methodType(nativeCall.getNativeReturn(), nativeCall.getNativeSignature());

            if (isStatic) {
                nativeTarget = findStatic(nativeCall.getNativeTarget(), nativeCall.getNativeName(), apparentType);
            } else {
                nativeTarget = findVirtual(nativeCall.getNativeTarget(), nativeCall.getNativeName(), apparentType);
            }

            // the actual native type with receiver
            MethodType nativeType = nativeTarget.type();
            Class[] nativeParams = nativeType.parameterArray();
            Class nativeReturn = nativeType.returnType();

            // convert arguments
            MethodHandle[] argConverters = new MethodHandle[nativeType.parameterCount()];
            for (int i = 0; i < argConverters.length; i++) {
                MethodHandle converter;
                if (!isStatic && i == 0) {
                    // handle non-static receiver specially
                    converter = Binder
                            .from(nativeParams[0], IRubyObject.class)
                            .cast(Object.class, IRubyObject.class)
                            .invokeStaticQuiet(lookup(), JavaUtil.class, "objectFromJavaProxy");
                } else {
                    // all other arguments use toJava
                    converter = Binder
                            .from(nativeParams[i], IRubyObject.class)
                            .insert(1, nativeParams[i])
                            .cast(Object.class, IRubyObject.class, Class.class)
                            .invokeVirtualQuiet(lookup(), "toJava");
                }
                argConverters[i] = converter;
            }
            nativeTarget = filterArguments(nativeTarget, 0, argConverters);

            Ruby runtime = method.getImplementationClass().getRuntime();
            Class[] convertedParams = CodegenUtils.params(IRubyObject.class, nativeTarget.type().parameterCount());

            // handle return value
            MethodHandle returnFilter;
            if (nativeReturn == byte.class
                    || nativeReturn == short.class
                    || nativeReturn == char.class
                    || nativeReturn == int.class
                    || nativeReturn == long.class) {
                // native integral type, produce a Fixnum
                nativeTarget = explicitCastArguments(nativeTarget, methodType(long.class, convertedParams));
                returnFilter = insertArguments(
                        findStatic(RubyFixnum.class, "newFixnum", methodType(RubyFixnum.class, Ruby.class, long.class)),
                        0,
                        runtime);
            } else if (nativeReturn == Byte.class
                    || nativeReturn == Short.class
                    || nativeReturn == Character.class
                    || nativeReturn == Integer.class
                    || nativeReturn == Long.class) {
                // boxed integral type, produce a Fixnum or nil
                returnFilter = insertArguments(
                        findStatic(JavaBootstrap.class, "fixnumOrNil", methodType(IRubyObject.class, Ruby.class, nativeReturn)),
                        0,
                        runtime);
            } else if (nativeReturn == float.class
                    || nativeReturn == double.class) {
                // native decimal type, produce a Float
                nativeTarget = explicitCastArguments(nativeTarget, methodType(double.class, convertedParams));
                returnFilter = insertArguments(
                        findStatic(RubyFloat.class, "newFloat", methodType(RubyFloat.class, Ruby.class, double.class)),
                        0,
                        runtime);
            } else if (nativeReturn == Float.class
                    || nativeReturn == Double.class) {
                // boxed decimal type, produce a Float or nil
                returnFilter = insertArguments(
                        findStatic(JavaBootstrap.class, "floatOrNil", methodType(IRubyObject.class, Ruby.class, nativeReturn)),
                        0,
                        runtime);
            } else if (nativeReturn == boolean.class) {
                // native boolean type, produce a Boolean
                nativeTarget = explicitCastArguments(nativeTarget, methodType(boolean.class, convertedParams));
                returnFilter = insertArguments(
                        findStatic(RubyBoolean.class, "newBoolean", methodType(RubyBoolean.class, Ruby.class, boolean.class)),
                        0,
                        runtime);
            } else if (nativeReturn == Boolean.class) {
                // boxed boolean type, produce a Boolean or nil
                returnFilter = insertArguments(
                        findStatic(JavaBootstrap.class, "booleanOrNil", methodType(IRubyObject.class, Ruby.class, Boolean.class)),
                        0,
                        runtime);
            } else if (CharSequence.class.isAssignableFrom(nativeReturn)) {
                // character sequence, produce a String or nil
                nativeTarget = explicitCastArguments(nativeTarget, methodType(CharSequence.class, convertedParams));
                returnFilter = insertArguments(
                        findStatic(JavaBootstrap.class, "stringOrNil", methodType(IRubyObject.class, Ruby.class, CharSequence.class)),
                        0,
                        runtime);
            } else if (nativeReturn == void.class) {
                // void return, produce nil
                returnFilter = constant(IRubyObject.class, runtime.getNil());
            } else if (nativeReturn == ByteList.class) {
                // bytelist, produce a String or nil
                nativeTarget = explicitCastArguments(nativeTarget, methodType(ByteList.class, convertedParams));
                returnFilter = insertArguments(
                        findStatic(JavaBootstrap.class, "stringOrNil", methodType(IRubyObject.class, Ruby.class, ByteList.class)),
                        0,
                        runtime);
            } else if (nativeReturn == BigInteger.class) {
                // bytelist, produce a String or nil
                nativeTarget = explicitCastArguments(nativeTarget, methodType(BigInteger.class, convertedParams));
                returnFilter = insertArguments(
                        findStatic(JavaBootstrap.class, "bignumOrNil", methodType(IRubyObject.class, Ruby.class, BigInteger.class)),
                        0,
                        runtime);
            } else {
                // all other object types
                nativeTarget = explicitCastArguments(nativeTarget, methodType(Object.class, convertedParams));
                returnFilter = insertArguments(
                        findStatic(JavaUtil.class, "convertJavaToUsableRubyObject", methodType(IRubyObject.class, Ruby.class, Object.class)),
                        0,
                        runtime);
            }

            nativeTarget = MethodHandles.filterReturnValue(nativeTarget, returnFilter);

            // cache adapted handle
            method.setHandle(nativeTarget);
        }

        // perform final adaptation by dropping JRuby-specific arguments
        int dropCount;
        if (isStatic) {
            if (site.functional) {
                dropCount = 2; // context, self
            } else {
                dropCount = 3; // context, caller, self
            }
        } else {
            if (site.functional) {
                dropCount = 1; // context
            } else {
                dropCount = 2; // context, caller
            }
        }

        return Binder
                .from(site.type())
                .drop(0, dropCount)
                .invoke(nativeTarget);
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
        return b == null ? runtime.getNil() : runtime.newBoolean(b);
    }

    public static IRubyObject stringOrNil(Ruby runtime, CharSequence cs) {
        return cs == null ? runtime.getNil() : RubyString.newUnicodeString(runtime, cs);
    }

    public static IRubyObject stringOrNil(Ruby runtime, ByteList bl) {
        return bl == null ? runtime.getNil() : RubyString.newString(runtime, bl);
    }

    public static IRubyObject bignumOrNil(Ruby runtime, BigInteger bi) {
        return bi == null ? runtime.getNil() : RubyBignum.newBignum(runtime, bi);
    }
}
