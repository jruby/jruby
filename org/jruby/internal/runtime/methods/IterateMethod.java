package org.jruby.internal.runtime.methods;

import org.jruby.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class IterateMethod extends AbstractMethod {
    private Callback callback;
    private RubyObject data;

    public IterateMethod(Callback callback, RubyObject data) {
        this.callback = callback;
        this.data = data;
    }

    /**
     * @see IMethod#execute(Ruby, RubyObject, String, RubyObject[], boolean)
	 * @fixme implement it 
     */
    public RubyObject execute(Ruby ruby, RubyObject receiver, String name, RubyObject[] args, boolean noSuper) {
        return callback.execute(args[0], new RubyObject[] { data, receiver }, ruby);
    }
}