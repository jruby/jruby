package org.jruby.javasupport;

import org.jruby.runtime.builtin.IRubyObject;

public interface RubyProxy {
    public IRubyObject getRubyObject();
    public RubyProxyFactory getRubyProxyFactory();
}
