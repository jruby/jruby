package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.runtime.Callback;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class IterateMethod extends AbstractMethod {
    private Callback callback;
    private IRubyObject data;

    public IterateMethod(Callback callback, IRubyObject data) {
        this.callback = callback;
        this.data = data;
    }

    /**
     * @see IMethod#execute(Ruby, RubyObject, String, RubyObject[], boolean)
	 * @fixme implement it 
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return callback.execute(args[0], new IRubyObject[] { data, receiver });
    }
}