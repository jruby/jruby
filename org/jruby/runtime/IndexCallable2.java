package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public interface IndexCallable2 {
    IRubyObject callIndexed(int index, IRubyObject recv, IRubyObject[] args);
}