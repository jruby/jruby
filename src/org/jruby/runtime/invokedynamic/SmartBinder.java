/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.runtime.invokedynamic;

import com.headius.invokebinder.Binder;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

/**
 * Maintains both a Binder, for building a series of transformations, and a
 * current Signature that maps symbolic names to arguments.
 * 
 * @author headius
 */
public class SmartBinder {
    private final Signature signature;
    private final Binder binder;

    private SmartBinder(Signature signature, Binder binder) {
        this.signature = signature;
        this.binder = binder;
    }

    public static SmartBinder from(Signature inbound) {
        return new SmartBinder(inbound, Binder.from(inbound.methodType()));
    }

    public SmartBinder fold(String newName, MethodHandle folder) {
        return new SmartBinder(signature.prependArg(newName, folder.type().returnType()), binder.fold(folder));
    }

    public SmartBinder permute(Signature target) {
        return new SmartBinder(target, binder.permute(signature.to(target)));
    }

    public SmartBinder permute(String... targetNames) {
        return permute(signature.permute(targetNames));
    }
    
    public SmartBinder insert(int index, String[] names, Class[] types, Object... values) {
        return new SmartBinder(signature.insertArgs(index, names, types), binder.insert(index, types, values));
    }

    public SmartBinder cast(Signature target) {
        return new SmartBinder(target, binder.cast(target.methodType()));
    }

    public SmartHandle invokeVirtualQuiet(Lookup lookup, String name) {
        return new SmartHandle(signature, binder.invokeVirtualQuiet(lookup, name));
    }
    
}
