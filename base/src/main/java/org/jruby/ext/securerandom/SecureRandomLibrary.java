package org.jruby.ext.securerandom;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

/**
 * Created by headius on 1/14/14.
 */
public class SecureRandomLibrary implements Library {

    public static void load(Ruby runtime) {
        RubyModule SecureRandom = runtime.defineModule("SecureRandom");
        SecureRandom.defineAnnotatedMethods(RubySecureRandom.class);
    }

    public void load(Ruby runtime, boolean wrap) {
        SecureRandomLibrary.load(runtime);
    }

    @Deprecated
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self) {
        return RubySecureRandom.random_bytes(context, self);
    }

    @Deprecated
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self, IRubyObject n) {
        return RubySecureRandom.random_bytes(context, self, n);
    }

    @Deprecated
    public static IRubyObject hex(ThreadContext context, IRubyObject self) {
        return RubySecureRandom.hex(context, self);
    }

    @Deprecated
    public static IRubyObject hex(ThreadContext context, IRubyObject self, IRubyObject n) {
        return RubySecureRandom.hex(context, self, n);
    }

    @Deprecated
    public static IRubyObject uuid(ThreadContext context, IRubyObject self) {
        return RubySecureRandom.hex(context, self);
    }

}
