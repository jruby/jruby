/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.runtime.invokedynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Arrays;

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
    
    public SmartHandle insert(int index, String name, Object arg) {
        return new SmartHandle(signature.insertArg(0, name, arg.getClass()), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, boolean arg) {
        return new SmartHandle(signature.insertArg(0, name, boolean.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, byte arg) {
        return new SmartHandle(signature.insertArg(0, name, byte.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, short arg) {
        return new SmartHandle(signature.insertArg(0, name, short.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, char arg) {
        return new SmartHandle(signature.insertArg(0, name, char.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, int arg) {
        return new SmartHandle(signature.insertArg(0, name, int.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, long arg) {
        return new SmartHandle(signature.insertArg(0, name, long.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, float arg) {
        return new SmartHandle(signature.insertArg(0, name, float.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public SmartHandle insert(int index, String name, double arg) {
        return new SmartHandle(signature.insertArg(0, name, double.class), MethodHandles.insertArguments(handle, index, arg));
    }
    
    public MethodHandle guard(MethodHandle target, MethodHandle fallback) {
        return MethodHandles.guardWithTest(handle, target, fallback);
    }
    
    public SmartHandle guard(SmartHandle target, SmartHandle fallback) {
        return new SmartHandle(target.signature, MethodHandles.guardWithTest(handle, target.handle, fallback.handle));
    }
    
}
