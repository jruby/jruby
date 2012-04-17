package org.jruby.ext.ffi;

import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

final class TypeSizeMapper {
    private final FFI ffi;
    private volatile Map<RubySymbol, Type> symbolTypeCache = Collections.emptyMap();

    TypeSizeMapper(FFI ffi) {
        this.ffi = ffi;
    }

    public final int sizeof(ThreadContext context, RubySymbol name) {
        Object obj = name.getFFIHandle();
        if (obj instanceof Type) {
            return ((Type) obj).size;
        }

        Type type = symbolTypeCache.get(name);
        if (type != null) {
            return type.size;
        }

        return lookupAndCacheSize(context, name);
    }

    private synchronized int lookupAndCacheSize(ThreadContext context, RubySymbol name) {
        Type type = lookupType(context, name);

        Map<RubySymbol, Type> map = new IdentityHashMap<RubySymbol, Type>(symbolTypeCache);
        map.put(name, type);
        symbolTypeCache = map;
        name.setFFIHandle(type);

        return type.size;
    }

    private Type lookupType(ThreadContext context, IRubyObject name) {
        IRubyObject type = ffi.typedefs.fastARef(name);
        if (type instanceof Type) {
            return (Type) type;
        }

        if ((type = ffi.ffiModule.callMethod(context, "find_type", name)) instanceof Type) {
            return (Type) type;
        }

        throw context.getRuntime().newTypeError("cannot resolve type " + name);
    }

    public static final int getTypeSize(ThreadContext context, RubySymbol sizeArg) {
        return context.getRuntime().getFFI().getSizeMapper().sizeof(context, sizeArg);
    }
}
