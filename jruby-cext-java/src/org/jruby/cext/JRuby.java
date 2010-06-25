/*
 *
 */

package org.jruby.cext;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.internal.runtime.methods.DynamicMethod;
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

    public static long newString(Ruby runtime, byte[] bytes, boolean tainted) {
        IRubyObject retval = RubyString.newStringNoCopy(runtime, bytes);
        if (tainted) {
            retval.setTaint(tainted);
        }

        return Handle.valueOf(retval).getAddress();
    }

    public static DynamicMethod newMethod(RubyModule module, long fn, int arity) {
        switch (arity) {
            case 0:
                return new NativeMethod0(module, arity, fn);

            case 1:
                return new NativeMethod1(module, arity, fn);

            default:
                return new NativeMethod(module, arity, fn);
        }
    }
}
