package org.jruby.internal.runtime.builtin;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.runtime.builtin.IObjectFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ObjectFactory implements IObjectFactory {
    private Ruby runtime;

    /**
     * Constructor for ObjectFactory.
     */
    public ObjectFactory(Ruby runtime) {
        super();
        this.runtime = runtime;
    }

    /**
     * @see org.jruby.runtime.builtin.IObjectFactory#newObject(RubyClass)
     */
    public IRubyObject newObject(RubyClass type) {
        return new RubyObject(runtime, type);
    }
}