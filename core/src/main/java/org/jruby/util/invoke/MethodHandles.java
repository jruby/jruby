package org.jruby.util.invoke;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MethodHandles {

    public static class Lookup {

        public MethodHandle findConstructor(Class<?> refc, MethodType type) throws NoSuchMethodException {
            Constructor constructor = refc.getDeclaredConstructor(type.parameterArray());
            return new MethodHandle(constructor);
        }
    }

    public static Lookup publicLookup() {
        return new Lookup();
    }

    public static Lookup lookup() {
        return new Lookup();
    }
}
