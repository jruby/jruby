package org.jruby.ext.securerandom;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ConvertBytes;

import java.security.SecureRandom;

/**
 * Created by headius on 1/14/14.
 */
public class SecureRandomLibrary {
    @JRubyMethod(meta = true)
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self) {
        return RubyString.newStringNoCopy(context.runtime, nextBytes(context, 16));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self, IRubyObject n) {
        return RubyString.newStringNoCopy(context.runtime, nextBytes(context, n));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject hex(ThreadContext context, IRubyObject self) {
        return RubyString.newStringNoCopy(context.runtime, ConvertBytes.twosComplementToHexBytes(nextBytes(context, 16), false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject hex(ThreadContext context, IRubyObject self, IRubyObject n) {
        return RubyString.newStringNoCopy(context.runtime, ConvertBytes.twosComplementToHexBytes(nextBytes(context, n), false));
    }

    private static byte[] nextBytes(ThreadContext context, IRubyObject n) {
        int size = n.isNil() ? 16 : (int)n.convertToInteger().getLongValue();

        return nextBytes(context, size);
    }

    private static byte[] nextBytes(ThreadContext context, int size) {
        if (size < 0) throw context.runtime.newArgumentError("negative argument: " + size);

        byte[] bytes = new byte[size];
        context.secureRandom.nextBytes(bytes);

        return bytes;
    }
}
