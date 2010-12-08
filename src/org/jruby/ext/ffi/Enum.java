
package org.jruby.ext.ffi;

import java.util.IdentityHashMap;
import java.util.Map;
import org.jcodings.util.IntHash;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a C enum
 */
@JRubyClass(name="FFI::Enum", parent="Object")
public final class Enum extends RubyObject {
    private final IRubyObject nativeType;
    private volatile Map<RubySymbol, RubyInteger> symbolToValue = new IdentityHashMap<RubySymbol, RubyInteger>();
    private volatile IntHash<RubySymbol> valueToSymbol = new IntHash<RubySymbol>();

    public static RubyClass createEnumClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass enumClass = ffiModule.defineClassUnder("Enum", runtime.getObject(),
                Allocator.INSTANCE);
        enumClass.defineAnnotatedMethods(Enum.class);
        enumClass.defineAnnotatedConstants(Enum.class);
        enumClass.includeModule(ffiModule.fastGetConstant("DataConverter"));
        
        return enumClass;
    }

    private static final class Allocator implements ObjectAllocator {
        private static final ObjectAllocator INSTANCE = new Allocator();

        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Enum(runtime, klass);
        }
    }

    private Enum(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        nativeType = runtime.fastGetModule("FFI").fastGetClass("Type").fastGetConstant("INT");
    }

    @JRubyMethod(name = "init_values")
    public final IRubyObject init_values(ThreadContext context, IRubyObject values) {
        if (!(values instanceof RubyHash)) {
            throw context.getRuntime().newTypeError(values, context.getRuntime().getHash());
        }
        Map<RubySymbol, RubyInteger> s2v = new IdentityHashMap<RubySymbol, RubyInteger>();
        IntHash<RubySymbol> v2s = new IntHash<RubySymbol>();

        for (Object obj : ((RubyHash) values).directEntrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            if (!(entry.getKey() instanceof RubySymbol)) {
                throw context.getRuntime().newTypeError(values, context.getRuntime().getSymbol());
            }
            if (!(entry.getValue() instanceof RubyInteger)) {
                throw context.getRuntime().newTypeError(values, context.getRuntime().getInteger());
            }
            RubySymbol sym = (RubySymbol) entry.getKey();
            s2v.put(sym, (RubyInteger) entry.getValue());
            v2s.put(RubyNumeric.num2int((IRubyObject) entry.getValue()), sym);
        }
        
        symbolToValue = s2v;
        valueToSymbol = v2s;

        return this;
    }

    @JRubyMethod(name = "native_type")
    public final IRubyObject native_type(ThreadContext context) {
        return nativeType;
    }

    @JRubyMethod(name = "to_native")
    public final IRubyObject to_native(ThreadContext context, IRubyObject name, IRubyObject ctx) {
        RubyInteger value;

        if (name instanceof RubySymbol && (value = symbolToValue.get((RubySymbol) name)) != null) {
            return value;

        } else if (name instanceof RubyInteger) {
            return name;

        } else if (name.respondsTo("to_int")) {

            return name.convertToInteger();

        } else {
            throw name.getRuntime().newArgumentError("invalid enum value, " + name.inspect());
        }
    }

    @JRubyMethod(name = "from_native")
    public final IRubyObject from_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {

        RubySymbol sym;

        if (value instanceof RubyInteger && (sym = valueToSymbol.get((int) ((RubyInteger) value).getLongValue())) != null) {
            return sym;
        }

        return value;
    }
}
