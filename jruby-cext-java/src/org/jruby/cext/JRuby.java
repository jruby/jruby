/*
 *
 */

package org.jruby.cext;

import org.jruby.RubyBasicObject;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class JRuby {

    public static long callRubyMethod(IRubyObject recv, Object methodName, IRubyObject[] args) {
        IRubyObject retval = recv.callMethod(recv.getRuntime().getCurrentContext(),
                methodName.toString(), args);

        return Handle.valueOf(retval).getAddress();
    }
}
