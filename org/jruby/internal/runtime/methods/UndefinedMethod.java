package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class UndefinedMethod extends AbstractMethod {
    private static final UndefinedMethod instance = new UndefinedMethod(Visibility.PUBLIC);

    /**
     * Constructor for UndefinedMethod.
     * @param visibility
     */
    private UndefinedMethod(Visibility visibility) {
        super(visibility);
    }

    /**
     * @see org.jruby.runtime.ICallable#call(Ruby, IRubyObject, String, IRubyObject[], boolean)
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        throw new UnsupportedOperationException();
    }

    public boolean isUndefined() {
        return true;
    }

    /**
     * Returns the instance.
     * @return UndefinedMethod
     */
    public static UndefinedMethod getInstance() {
        return instance;
    }

}