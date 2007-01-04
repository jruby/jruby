/*
 * NilCallable.java
 *
 * Created on December 24, 2006, 1:16 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.internal.runtime.methods.AbstractCallable;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class NilCallable extends AbstractCallable{
    public static final NilCallable NIL_CALLABLE = new NilCallable();
    
    public NilCallable() {
    }

    public IRubyObject call(ThreadContext context, IRubyObject receiver, IRubyObject[] args) {
        return context.getRuntime().getNil();
    }

    public ICallable dup() {
        return this;
    }
}
