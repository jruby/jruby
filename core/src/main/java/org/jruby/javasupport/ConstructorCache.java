package org.jruby.javasupport;

import org.jruby.Ruby;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.NonBlockingHashMapLong;

/**
 * Used for concrete reified classes. Constructed in generated code (RubyClass)
 */
public class ConstructorCache implements CallableSelector.CallableCache<ParameterTypes> {

    final NonBlockingHashMapLong<ParameterTypes> cache = new NonBlockingHashMapLong<>(4);
    public final JavaConstructor[] constructors;
    public final int noArgIndex;

    private volatile ConcreteJavaProxy.SplitCtorPlan splitCtorPlan;

    /**
     * Disambiguate which constructor index to call from the given cache
     * @param args argument list for the custructor
     * @param cache
     * @return index of ctor in cache to call, or throws an argument error
     */
    public static int findIndex(Ruby runtime, ConstructorCache cache, IRubyObject[] args) {
        // fast path: check the callable signature cache before doing arity filtering + full match
        JavaConstructor[] constructors = cache.constructors;
        int signatureCode = CallableSelector.argsHashCode(args);
        ParameterTypes cached = cache.getSignature(signatureCode);
        if (cached == null) {
            // cache miss — do full match which populates the cache for next time
            ThreadContext context = runtime.getCurrentContext();
            cached = Java.JCreateMethod.matchConstructorInternal(context, constructors, cache, args);
        }

        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i] == cached) return i;
        }

        throw new AssertionError("BUG: matched constructor not found in cache");
    }

    public ConstructorCache(JavaConstructor[] constructors) {
        this.constructors = constructors;
        this.noArgIndex = findNoArgConstructor(constructors);
    }

    private static int findNoArgConstructor(JavaConstructor[] constructors) {
        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i].getParameterTypes().length == 0) return i;
        }
        return -1;
    }

    public final ParameterTypes getSignature(int signatureCode) {
        return cache.get(signatureCode);
    }

    public final void putSignature(int signatureCode, ParameterTypes callable) {
        cache.put(signatureCode, callable);
    }

    public ConcreteJavaProxy.SplitCtorPlan getSplitCtorPlan() {
        return splitCtorPlan;
    }

    public void setSplitCtorPlan(ConcreteJavaProxy.SplitCtorPlan splitCtorPlan) {
        this.splitCtorPlan = splitCtorPlan;
    }
}
