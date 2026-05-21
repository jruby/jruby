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
        ThreadContext context = runtime.getCurrentContext();
        JavaConstructor constructor = Java.JCreateMethod.matchConstructorInternal(
            context, cache.constructors, cache, args
        );
        int index = cache.indexOf(constructor);
        assert index >= 0; // matchConstructor would have already raised
        return index;
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

    private int indexOf(final JavaConstructor constructor) {
        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i].equals(constructor)) return i;
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
