package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class TypeResolver {
    private final FFI ffi;
    private volatile Map<RubySymbol, Type> symbolTypeCache = Collections.emptyMap();

    TypeResolver(FFI ffi) {
        this.ffi = ffi;
    }

    public final Type findType(Ruby runtime, IRubyObject name) {
        if (name instanceof Type) {
            return (Type) name;

        } else if (name instanceof RubySymbol) {
            return findType(runtime, (RubySymbol) name);

        } else {
            return lookupType(runtime, name);
        }
    }
    public final Type findType(Ruby runtime, RubySymbol name) {
        Object obj = name.getFFIHandle();
        if (obj instanceof Type) {
            return ((Type) obj);
        }

        Type type = symbolTypeCache.get(name);
        if (type != null) {
            return type;
        }

        return lookupAndCacheType(runtime, name);
    }
    
    private synchronized Type lookupAndCacheType(Ruby runtime, RubySymbol name) {
        Type type = lookupType(runtime, name);

        Map<RubySymbol, Type> map = new IdentityHashMap<RubySymbol, Type>(symbolTypeCache);
        map.put(name, type);
        symbolTypeCache = map;
        name.setFFIHandle(type);

        return type;
    }

    private Type lookupType(Ruby runtime, IRubyObject name) {
        IRubyObject type = ffi.typedefs.fastARef(name);
        if (type instanceof Type) {
            return (Type) type;
        }

        if ((type = ffi.ffiModule.callMethod(runtime.getCurrentContext(), "find_type", name)) instanceof Type) {
            return (Type) type;
        }

        throw runtime.newTypeError("cannot resolve type " + name);
    }
}
