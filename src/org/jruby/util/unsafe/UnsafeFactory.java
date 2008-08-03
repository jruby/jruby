package org.jruby.util.unsafe;

import java.lang.reflect.Field;

public class UnsafeFactory {
    private static Unsafe unsafe;
    private static final boolean DEBUG = false;
    
    static {
        // first try our custom-generated Unsafe
        try {
            Class unsafeClass = Class.forName("org.jruby.util.unsafe.GeneratedUnsafe");
            unsafe = (Unsafe)unsafeClass.newInstance();
        } catch (Throwable ignore) {
            if (DEBUG) ignore.printStackTrace();
        }
        
        // then try Sun's Unsafe
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            // if we get here, the class and field exist; construct our Unsafe impl
            // that calls it directly
            unsafe = (Unsafe)Class.forName("org.jruby.util.unsafe.SunUnsafeWrapper").newInstance();
        } catch (Throwable ignore) {
            if (DEBUG) ignore.printStackTrace();
        }
        
        // else leave it null
        if (DEBUG && unsafe == null) System.err.println("No Unsafe implementation available");
    }
    
    public static Unsafe getUnsafe() {
        return unsafe;
    }
}
