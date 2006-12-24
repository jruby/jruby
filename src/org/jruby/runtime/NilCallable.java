/*
 * NilCallable.java
 *
 * Created on December 24, 2006, 1:16 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.AbstractCallable;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class NilCallable extends AbstractCallable{
    public static final NilCallable NIL_CALLABLE = new NilCallable(null);
    
    public NilCallable(Visibility visibility) {
        super(visibility);
    }

    public IRubyObject internalCall(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
        return context.getRuntime().getNil();
    }

    public ICallable dup() {
        return this;
    }

    public void preMethod(ThreadContext context, RubyModule implementationClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper) {
    }

    public void postMethod(ThreadContext context) {
    }
}
