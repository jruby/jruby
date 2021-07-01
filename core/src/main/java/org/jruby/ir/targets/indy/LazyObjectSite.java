package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.compiler.Constantizable;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
* Created by headius on 10/23/14.
*/
public abstract class LazyObjectSite extends ConstructObjectSite {
    public LazyObjectSite(MethodType type) {
        super(type);
    }

    private static final MethodHandle CACHE = Binder.from(IRubyObject.class, LazyObjectSite.class, IRubyObject.class).invokeVirtualQuiet(MethodHandles.lookup(), "cache");

    public IRubyObject cache(IRubyObject t) {
        MethodHandle constant = null;

        if (t instanceof Constantizable) {
            constant = (MethodHandle) ((Constantizable) t).constant();
        }

        if (constant == null) {
            constant = Binder.from(type())
                    .dropAll()
                    .constant(t);
        }

        setTarget(constant);

        return t;
    }

    @Override
    public Binder prepareBinder() {
        return Binder
                .from(type())
                .filterReturn(CACHE.bindTo(this));
    }
}
