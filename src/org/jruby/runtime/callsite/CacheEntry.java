package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;

class CacheEntry {
    public static final CacheEntry NULL_CACHE = new CacheEntry(null, null, null);
    public final DynamicMethod cachedMethod;
    public final int generation;
    public final String methodName;

    public CacheEntry(DynamicMethod method, RubyClass type, String name) {
        super();
        cachedMethod = method;
        generation = type == null ? 0 : type.getSerialNumber();
        methodName = name;
    }

    public boolean typeOk(RubyClass incomingType) {
        return generation == incomingType.getSerialNumber();
    }
}
