package org.jruby.api;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

public class API {
    public static IRubyObject rb_sys_fail_path(Ruby runtime, String path) {
        throw runtime.newSystemCallError("bad path for cloexec: " + path);
    }
}
