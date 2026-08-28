package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.api.Create;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyArrayOneObject;
import org.jruby.specialized.RubyArraySpecialized;
import org.jruby.specialized.RubyArrayTwoObject;
import org.jruby.util.func.ObjectObjectIntFunction;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.stream.IntStream;

import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class ArrayBootstrap {
    public static final Handle ARRAY_H = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(ArrayBootstrap.class),
            "array",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class),
            false);
    public static final Handle NUMERIC_ARRAY = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(ArrayBootstrap.class),
            "literalArray",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class),
            false);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle ARRAY_HANDLE =
            Binder
                    .from(RubyArray.class, ThreadContext.class, IRubyObject[].class)
                    .invokeStaticQuiet(LOOKUP, ArrayBootstrap.class, "array");

    public static CallSite array(MethodHandles.Lookup lookup, String name, MethodType type) {
        MethodHandle handle = Binder
                .from(type)
                .collect(1, IRubyObject[].class)
                .invoke(ARRAY_HANDLE);

        return new ConstantCallSite(handle);
    }

    public static RubyArray array(ThreadContext context, IRubyObject[] ary) {
        assert ary.length > RubyArraySpecialized.MAX_PACKED_SIZE;
        // array() only dispatches here if ^^ holds
        return RubyArray.newArrayNoCopy(context.runtime, ary);
    }

    public static CallSite literalArray(MethodHandles.Lookup lookup, String name, MethodType type, String stringValues) {
        MutableCallSite site = new MutableCallSite(type);

        Object values = switch (name) {
            case "fixnumArray" -> Helpers.decodeLongString(stringValues);
            case "floatArray" -> Helpers.decodeDoubleString(stringValues);
            default -> throw new RuntimeException("invalid literal array type");
        };

        MethodHandle handle = Binder
                .from(type)
                .append(site, values)
                .invokeStaticQuiet(ArrayBootstrap.class, name);

        site.setTarget(handle);

        return site;
    }

    public static RubyArray fixnumArray(ThreadContext context, MutableCallSite site, long[] values) {
        return bindArray(context, site, values, values.length,
                (runtime, vals, index) -> asFixnum(context, values[index]));
    }

    public static RubyArray floatArray(ThreadContext context, MutableCallSite site, double[] values) {
        return bindArray(context, site, values, values.length,
                (runtime, vals, index) -> runtime.newFloat(values[index]));
    }

    private static <ArrayType> RubyArray bindArray(ThreadContext context, MutableCallSite site, ArrayType values, int size, ObjectObjectIntFunction<Ruby, ArrayType, IRubyObject> mapper) {
        Ruby runtime = context.runtime;
        var Array = arrayClass(context);

        return switch (size) {
            case 1 -> bindArray(site, Array, mapper.apply(runtime, values, 0));
            case 2 -> bindArray(site, Array, mapper.apply(runtime, values, 0), mapper.apply(runtime, values, 1));
            default -> bindArray(site, Array,
                    IntStream.range(0, size).mapToObj(i -> mapper.apply(runtime, values, i)).toArray(IRubyObject[]::new));
        };
    }

    private static RubyArray bindArray(MutableCallSite site, RubyClass arrayClass, IRubyObject car) {
        site.setTarget(Binder.from(site.type())
                .drop(0)
                .append(arrayOf(RubyClass.class, IRubyObject.class), arrayClass, car)
                .invokeConstructorQuiet(RubyArrayOneObject.class));

        return new RubyArrayOneObject(arrayClass, car);
    }

    private static RubyArray bindArray(MutableCallSite site, RubyClass arrayClass, IRubyObject car, IRubyObject cdr) {
        site.setTarget(Binder.from(site.type())
                .drop(0)
                .append(arrayOf(RubyClass.class, IRubyObject.class, IRubyObject.class), arrayClass, car, cdr)
                .invokeConstructorQuiet(RubyArrayTwoObject.class));

        return new RubyArrayTwoObject(arrayClass, car, cdr);
    }

    private static RubyArray bindArray(MutableCallSite site, RubyClass arrayClass, IRubyObject[] values) {
        site.setTarget(Binder.from(site.type())
                .drop(0)
                .append(arrayClass, values)
                .invokeStaticQuiet(RubyArray.class, "newSharedArray"));

        return RubyArray.newSharedArray(arrayClass, values);
    }
}
