package org.jruby.runtime.builtin;

import org.jruby.RubyClass;

/**
 * @author jpetersen
 * @version $Revision$
 */
public interface IObjectFactory {
    public IRubyObject newObject(RubyClass type);
}
