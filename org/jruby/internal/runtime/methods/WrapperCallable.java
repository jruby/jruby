package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class WrapperCallable extends AbstractMethod {
    private ICallable callable;

    /**
     * Constructor for WrapperCallable.
     * @param visibility
     */
    public WrapperCallable(ICallable callable, Visibility visibility) {
        super(visibility);
        this.callable = callable;
    }

    /**
     * @see org.jruby.runtime.ICallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return callable.call(ruby, receiver, name, args, noSuper);
    }
}
