package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IRaiseListener {
    void exceptionRaised(IRubyObject exception);

}
