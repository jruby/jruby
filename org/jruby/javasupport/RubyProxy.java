package org.jruby.javasupport;

import org.jruby.RubyObject;

public interface RubyProxy
{
    public RubyObject getRubyObject ();
    public RubyProxyFactory getRubyProxyFactory ();
}
