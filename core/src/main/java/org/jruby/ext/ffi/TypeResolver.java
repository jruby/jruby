package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.RubyHash;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.jruby.api.Error.argumentError;

public final class TypeResolver {
    private final FFI ffi;
    private volatile Map<RubySymbol, Type> symbolTypeCache = Collections.emptyMap();

    TypeResolver(FFI ffi) {
        this.ffi = ffi;
    }

    public final Type findType(Ruby runtime, IRubyObject name) {
        return findType(runtime, name, runtime.getNil());
    }

    public final Type findType(Ruby runtime, IRubyObject name, IRubyObject typeMap) {
        if (name instanceof Type type) return type;
        if (name instanceof RubySymbol sym) {
            Object obj = sym.getFFIHandle();
            if (obj instanceof Type type) return type;

            if (typeMap != null && typeMap instanceof RubyHash hash) {
                Type type = (Type) hash.get(name);
                if (type != null && !type.isNil()) return type;
            }

            Type type = symbolTypeCache.get(name);
            if (type != null) return type;

            return lookupAndCacheType(runtime, sym, typeMap);
        } else {
            return lookupType(runtime, name, typeMap);
        }
    }

    private synchronized Type lookupAndCacheType(Ruby runtime, RubySymbol name, IRubyObject typeMap) {
        Type type = lookupType(runtime, name, typeMap);

        Map<RubySymbol, Type> map = new IdentityHashMap<RubySymbol, Type>(symbolTypeCache);
        map.put(name, type);
        symbolTypeCache = map;
        name.setFFIHandle(type);

        return type;
    }

    private Type lookupType(Ruby runtime, IRubyObject name, IRubyObject typeMap) {
        IRubyObject type = ffi.typedefs.fastARef(name);
        if (type instanceof Type t) return t;

        // Unsure why this was there but there's no equivalent in the C code
//        IRubyObject args[] = new IRubyObject[]{name, typeMap};
//        if ((type = ffi.ffiModule.callMethod(runtime.getCurrentContext(), "find_type", args)) instanceof Type) {
//            return (Type) type;
//        }

        throw argumentError(runtime.getCurrentContext(), "cannot resolve type " + name);
    }
}
