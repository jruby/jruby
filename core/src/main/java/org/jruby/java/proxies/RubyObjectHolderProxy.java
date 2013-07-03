
package org.jruby.java.proxies;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Additional interface implemented by proxies that back Java interfaces by
 * Ruby objects so that we can get back to the original Ruby object from the proxy.
 * @author nicksieger
 */
public interface RubyObjectHolderProxy {
    IRubyObject __ruby_object();
}
