package org.jruby.runtime.variables;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IGlobalVariable {
    public IRubyObject getValue();
    public IRubyObject setValue(IRubyObject newValue);
}
