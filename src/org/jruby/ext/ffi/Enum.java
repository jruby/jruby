
package org.jruby.ext.ffi;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.jcodings.util.IntHash;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents a C enum
 */
@JRubyClass(name="FFI::Enum", parent="FFI::Type")
public class Enum extends Type {

    private volatile Map<RubySymbol, Integer> symbolToValue = new IdentityHashMap<RubySymbol, Integer>();
    private volatile IntHash<RubySymbol> valueToSymbol = new IntHash<RubySymbol>();

    public static RubyClass createEnumClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass enumClass = ffiModule.defineClassUnder("Enum", ffiModule.fastGetClass("Type"),
                Allocator.INSTANCE);
        enumClass.defineAnnotatedMethods(Enum.class);
        enumClass.defineAnnotatedConstants(Enum.class);

        
        return enumClass;
    }

    private static final class Allocator implements ObjectAllocator {
        private static final ObjectAllocator INSTANCE = new Allocator();

        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Enum(runtime, klass);
        }
    }

    private Enum(Ruby runtime, RubyClass klass) {
        super(runtime, klass, NativeType.INT);
    }

    @JRubyMethod(name = "init_values")
    public final IRubyObject init_values(ThreadContext context, IRubyObject values) {
        if (!(values instanceof RubyHash)) {
            throw context.getRuntime().newTypeError(values, context.getRuntime().getHash());
        }
        Map<RubySymbol, Integer> s2v = new IdentityHashMap<RubySymbol, Integer>();
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
            Integer value = RubyNumeric.num2int((IRubyObject) entry.getValue());
            s2v.put(sym, value);
            v2s.put(value, sym);
        }
        
        symbolToValue = s2v;
        valueToSymbol = v2s;

        return this;
    }

    public final int intValue(IRubyObject name) {
        if (name instanceof RubySymbol) {
            Integer value = symbolToValue.get((RubySymbol) name);
            if (value == null) {
                throw name.getRuntime().newArgumentError("invalid enum value, " + name.inspect());
            }

            return value;
        } else if (name instanceof RubyInteger) {

            return (int) name.convertToInteger().getLongValue();

        } else {
            throw name.getRuntime().newArgumentError("invalid enum value, " + name.inspect());
        }
    }

    public final IRubyObject symbolValue(int value) {
        RubySymbol sym = valueToSymbol.get(value);
        
        return sym != null ? sym : getRuntime().newFixnum(value);
    }
}
