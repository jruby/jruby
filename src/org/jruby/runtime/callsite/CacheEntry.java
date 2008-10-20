package org.jruby.runtime.callsite;

import org.jruby.RubyClass;
import org.jruby.internal.runtime.methods.DynamicMethod;

public class CacheEntry {
    public static final CacheEntry NULL_CACHE = new CacheEntry(null, -1);
    public final DynamicMethod method;
    public final int generation;

    public CacheEntry(DynamicMethod method, int gen) {
        super();
        this.method = method;
        generation = gen;
    }

    public boolean typeOk(RubyClass incomingType) {
        return generation == incomingType.getSerialNumber();
    }
}
