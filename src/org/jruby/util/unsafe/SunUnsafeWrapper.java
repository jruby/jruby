package org.jruby.util.unsafe;

import java.lang.reflect.Field;

public class SunUnsafeWrapper implements Unsafe {
    private sun.misc.Unsafe sunUnsafe;

    public SunUnsafeWrapper() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            this.sunUnsafe = (sun.misc.Unsafe)field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void throwException(Throwable t) {
        sunUnsafe.throwException(t);
    }
}
