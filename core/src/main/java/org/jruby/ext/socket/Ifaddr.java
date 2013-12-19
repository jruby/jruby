package org.jruby.ext.socket;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Lucas Allan Amorim
 */
@JRubyClass(name = "Socket::Ifaddr", parent = "Data")
public class Ifaddr extends RubyObject {

    public Ifaddr(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {

        return context.runtime.newString("#<Socket::Ifaddr: " + ">");
    }

}
