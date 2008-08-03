/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import java.lang.reflect.Field;
import org.jruby.util.unsafe.Unsafe;

/**
 *
 * @author headius
 */
public class UnsafeFactory {
    private static Unsafe unsafe;
    
    static {
        // first try to unwrap the real Unsafe    Unsafe unsafe = null;
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            // if we get here, the class and field exist; construct our Unsafe impl
            // that calls it directly
            unsafe = (Unsafe)Class.forName("org.jruby.util.unsafe.SunUnsafeWrapper").newInstance();
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }
    
    public static Unsafe getUnsafe() {
        return unsafe;
    }
}
