package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DivCallSite extends NormalCachingCallSite {

    public DivCallSite() {
        super("/");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (self instanceof RubyFixnum) {
            return ((RubyFixnum) self).op_div(context, arg);
        }
        return super.call(context, caller, self, arg);
    }
}
