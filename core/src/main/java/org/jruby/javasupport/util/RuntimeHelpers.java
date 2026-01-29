package org.jruby.javasupport.util;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.embed.util.CoreConstructors;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;

// Deprecated in 2012, but still in use in Nokogiri until Feb 2019
// See https://github.com/sparklemotion/nokogiri/pull/1874
// Relates to https://github.com/sparklemotion/nokogiri/pull/2027
@Deprecated(since = "1.7.4")
public class RuntimeHelpers extends Helpers {
    @Deprecated(since = "9.0.0.0")
    public static RubyHash constructHash(Ruby runtime, IRubyObject key, IRubyObject value) {
        return CoreConstructors.createHash(runtime, key, value);
    }

    @Deprecated(since = "9.0.0.0")
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1,
                                         IRubyObject key2, IRubyObject value2) {
        return CoreConstructors.createHash(runtime, key1, value1, key2, value2);
    }
}
