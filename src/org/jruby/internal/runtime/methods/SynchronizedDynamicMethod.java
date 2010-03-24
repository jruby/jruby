/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class SynchronizedDynamicMethod extends DynamicMethod {
    private final DynamicMethod delegate;

    public SynchronizedDynamicMethod(DynamicMethod delegate) {
        super(delegate.getImplementationClass(), delegate.getVisibility(), delegate.getCallConfig());
        this.delegate = delegate;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        synchronized (self) {
            return delegate.call(context, self, clazz, name, args, block);
        }
    }

    @Override
    public DynamicMethod dup() {
        return new SynchronizedDynamicMethod(delegate);
    }

}
