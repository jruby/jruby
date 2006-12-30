/*
 * DynamicMethod.java
 *
 * Created on December 28, 2006, 4:20 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public interface DynamicMethod {
    IRubyObject call(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper);
    
    void preMethod(ThreadContext context, RubyModule implementationClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper);
    void postMethod(ThreadContext context);

    IRubyObject internalCall(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper);
    
    public String getOriginalName();
    
    public RubyModule getImplementationClass();

    public void setImplementationClass(RubyModule implClass);

    public Visibility getVisibility();

    public void setVisibility(Visibility visibility);

    public boolean isUndefined();
    
    boolean needsImplementer();
    
    boolean isCallableFrom(IRubyObject caller, CallType callType);

    Arity getArity();
    
    DynamicMethod dup();
}
