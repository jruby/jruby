package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;

public class CacheEntry {
    public static final CacheEntry NULL_CACHE = new CacheEntry(UndefinedMethod.INSTANCE, 0);
    public final DynamicMethod method;
    public final int token;

    public CacheEntry(DynamicMethod method, int token) {
        this.method = method;
        this.token = token;
    }

    public final boolean typeOk(RubyClass incomingType) {
        return typeOk(this, incomingType);
    }

    public static final boolean typeOk(CacheEntry entry, RubyClass incomingType) {
        return entry.token == incomingType.getGeneration();
    }

    @Override
    public String toString() {
        return getClass().getName() + '@' +
            Integer.toHexString(System.identityHashCode(this)) +
            "<method=" + method + ", token=" + token + ">";
    }

}
