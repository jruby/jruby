package org.jruby.ext.securerandom;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.security.SecureRandom;

/**
 * Created by headius on 1/14/14.
 */
public class SecureRandomLibrary {
    @JRubyMethod(meta = true)
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self) {
        return nextBytes(context.runtime, 16);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self, IRubyObject n) {
        Ruby runtime = context.runtime;

        if (!(n instanceof RubyFixnum)) throw runtime.newArgumentError("non-integer argument: " + n);

        int size = (int)n.convertToInteger().getLongValue();

        return nextBytes(runtime, size);
    }

    private static IRubyObject nextBytes(Ruby runtime, int size) {
        if (size < 0) throw runtime.newArgumentError("negative argument: " + size);

        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);

        return RubyString.newStringNoCopy(runtime, bytes);
    }
}
