
package org.jruby;

import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author nicksieger
 */
public interface RubyRuntimeAdapter {
    IRubyObject eval(Ruby runtime, String script);
}
