package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.UnboundMethod;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class MethodMethod extends AbstractMethod {
    private UnboundMethod method;

    /**
     * Constructor for MethodMethod.
     * @param visibility
     */
    public MethodMethod(UnboundMethod method, Visibility visibility) {
        super(visibility);
        this.method = method;
    }

    /**
     * @see org.jruby.runtime.ICallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return method.bind(receiver).call(args);
    }
}