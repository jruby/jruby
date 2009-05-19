package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;

public class CacheEntry {
    public static final CacheEntry NULL_CACHE = new CacheEntry(UndefinedMethod.INSTANCE, new Object());
    public final DynamicMethod method;
    public final Object token;

    public CacheEntry(DynamicMethod method, Object token) {
        this.method = method;
        this.token = token;
    }

    public final boolean typeOk(RubyClass incomingType) {
        return token == incomingType.getCacheToken();
    }
}
