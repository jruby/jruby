package org.jruby.runtime.load;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IAutoloadMethod {
    public IRubyObject load(Ruby runtime, String name);
}
