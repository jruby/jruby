package org.jruby.javasupport;

import java.util.Arrays;
import java.util.List;

import org.jruby.Ruby;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.NonBlockingHashMapLong;

/**
 * Used for concrete reified classes. Constructed in generated code (RubyClass)
 */
public class ConstructorCache implements CallableSelector.CallableCache<ParameterTypes> {

    private final NonBlockingHashMapLong<ParameterTypes> cache = new NonBlockingHashMapLong<>(8);
    public final JavaConstructor[] constructors;
    public final int noArgConstructorIndex;
    private final List<JavaConstructor> constructorList;
    private volatile ConcreteJavaProxy.SplitCtorPlan splitCtorPlan;

    /**
     * Disambiguate which constructor index to call from the given cache
     * @param args argument list for the custructor
     * @param cache
     * @return index of ctor in cache to call, or throws an argument error
     */
    public static int findIndex(Ruby runtime, ConstructorCache cache, IRubyObject[] args) {
        JavaConstructor[] constructors = cache.constructors;
        int signatureCode = CallableSelector.argsHashCode(args);
        ParameterTypes cached = cache.getSignature(signatureCode);
        if (cached == null) {
            ThreadContext context = runtime.getCurrentContext();
            cached = Java.JCreateMethod.matchConstructorIndex(context, constructors, cache, args.length, args);
        }

        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i] == cached) return i;
        }

        throw new AssertionError("BUG: matched constructor not found in cache");
    }
    public ConstructorCache(JavaConstructor[] constructors) {
        this.constructors = constructors;
        constructorList = Arrays.asList(constructors);
        noArgConstructorIndex = findNoArgConstructor(constructors);
    }

    private static int findNoArgConstructor(JavaConstructor[] constructors) {
        for (int i = 0; i < constructors.length; i++) {
            if (constructors[i].getParameterTypes().length == 0) return i;
        }

        return -1;
    }

    public int indexOf(JavaConstructor ctor) {
        return constructorList.indexOf(ctor);
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
