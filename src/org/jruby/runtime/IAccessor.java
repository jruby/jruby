package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IAccessor {
    public IRubyObject getValue();
    public IRubyObject setValue(IRubyObject newValue);
}
