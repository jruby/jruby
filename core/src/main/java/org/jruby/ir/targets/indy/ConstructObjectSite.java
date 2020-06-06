package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.compiler.Constantizable;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

/**
 * Created by headius on 10/23/14.
 */
public class ConstructObjectSite extends MutableCallSite {
    public ConstructObjectSite(MethodType type) {
        super(type);
    }

    public CallSite bootstrap(MethodHandles.Lookup lookup) {
        MethodHandle handle = prepareBinder()
                .insert(0, this)
                .invokeVirtualQuiet(lookup, initialTarget());

        setTarget(handle);

        return this;
    }

    // Default method to initialize object is "construct", overridable by child classes
    protected String initialTarget() {
        return "construct";
    }

    public Binder prepareBinder() {
        return Binder.from(type());
    }
}
