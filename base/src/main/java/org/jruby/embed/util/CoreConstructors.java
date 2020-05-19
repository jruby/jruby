package org.jruby.embed.util;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * API's which can be used by embedders to construct Ruby builtin core types.
 */
public class CoreConstructors {
    public static RubyHash createHash(Ruby runtime, IRubyObject key, IRubyObject value) {
        RubyHash hash = RubyHash.newHash(runtime);

        hash.fastASet(key, value);

        return hash;
    }

    public static RubyHash createHash(Ruby runtime, IRubyObject key1, IRubyObject value1,
                                      IRubyObject key2, IRubyObject value2) {
        RubyHash hash = createHash(runtime, key1, value1);

        hash.fastASet(key2, value2);

        return hash;
    }
}
