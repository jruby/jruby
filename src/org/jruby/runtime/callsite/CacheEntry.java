package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;

class CacheEntry {
    public static final CacheEntry NULL_CACHE = new CacheEntry(null, null, null);
    public final DynamicMethod cachedMethod;
    public final RubyClass cachedType;
    public final String methodName;

    public CacheEntry(DynamicMethod method, RubyClass type, String name) {
        super();
        cachedMethod = method;
        cachedType = type;
        methodName = name;
    }

    public boolean typeOk(RubyClass incomingType) {
        RubyClass cachedType = this.cachedType;
        return cachedType == incomingType || typeGoodEnough(cachedType, incomingType);
    }

    private final boolean typeGoodEnough(RubyClass cachedType, RubyClass incomingType) {
        return cachedType != null && cachedMethod.getImplementationClass() != cachedType && incomingType.getSuperClass() == cachedType.getSuperClass() && incomingType.retrieveMethod(methodName) == null;
    }
}
