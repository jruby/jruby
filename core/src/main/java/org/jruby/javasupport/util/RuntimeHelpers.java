package org.jruby.javasupport.util;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.embed.util.CoreConstructors;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;

@Deprecated
public class RuntimeHelpers extends Helpers {
    @Deprecated
    public static RubyHash constructHash(Ruby runtime, IRubyObject key, IRubyObject value) {
        return CoreConstructors.createHash(runtime, key, value);
    }

    @Deprecated
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1,
                                         IRubyObject key2, IRubyObject value2) {
        return CoreConstructors.createHash(runtime, key1, value1, key2, value2);
    }
}
