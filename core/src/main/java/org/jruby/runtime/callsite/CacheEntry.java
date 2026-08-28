package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;

public class CacheEntry {
    public static final CacheEntry NULL_CACHE = new CacheEntry(UndefinedMethod.INSTANCE, 0);
    public final DynamicMethod method;
    public final RubyModule sourceModule;
    public final int token;

    public CacheEntry(DynamicMethod method, int token) {
        this.method = method;
        this.sourceModule = method.getImplementationClass();
        this.token = token;
    }

    public CacheEntry(DynamicMethod method, RubyModule source, int token) {
        this.method = method;
        this.sourceModule = source;
        this.token = token;
    }

    public final boolean typeOk(RubyClass incomingType) {
        return token == incomingType.getGeneration();
    }

    @Override
    public String toString() {
        return getClass().getName() + '@' +
            Integer.toHexString(System.identityHashCode(this)) +
            "<method=" + method + ", token=" + token + ">";
    }

}
