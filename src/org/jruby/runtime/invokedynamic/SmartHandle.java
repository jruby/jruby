/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.runtime.invokedynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

/**
 * A tuple of a Signature and a java.lang.invoke.MethodHandle.
 * 
 * @author headius
 */
public class SmartHandle {
    private final Signature signature;
    private final MethodHandle handle;

    public SmartHandle(Signature signature, MethodHandle handle) {
        this.signature = signature;
        this.handle = handle;
    }

    public static SmartHandle findStaticQuiet(Lookup lookup, Class target, String name, Signature signature) {
        try {
            return new SmartHandle(signature, lookup.findStatic(target, name, signature.methodType()));
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        } catch (IllegalAccessException nae) {
            throw new RuntimeException(nae);
        }
    }

    public Signature signature() {
        return signature;
    }

    public MethodHandle handle() {
        return handle;
    }
    
}
