package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IStaticCallable {
    IRubyObject callIndexed(int index, IRubyObject receiver, IRubyObject[] args);
}