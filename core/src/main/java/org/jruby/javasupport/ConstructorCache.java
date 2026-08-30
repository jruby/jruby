package org.jruby.javasupport;

import java.util.Arrays;
import java.util.List;

import org.jruby.java.dispatch.CallableSelector;
import org.jruby.util.collections.NonBlockingHashMapLong;

/**
 * Used for concrete reified classes. Constructed in generated code (RubyClass)
 */
public class ConstructorCache implements CallableSelector.CallableCache<ParameterTypes> {

    private final NonBlockingHashMapLong<ParameterTypes> cache = new NonBlockingHashMapLong<>(8);
    public final JavaConstructor[] constructors;
    private final List<JavaConstructor> constructorList;

    public ConstructorCache(JavaConstructor[] constructors) {
        this.constructors = constructors;
        constructorList = Arrays.asList(constructors);
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
}
