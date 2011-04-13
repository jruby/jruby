package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GtCallSite extends NormalCachingCallSite {

    public GtCallSite() {
        super(">");
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        if (self instanceof RubyFixnum && !context.runtime.isFixnumReopened()) {
            return ((RubyFixnum) self).op_gt(context, fixnum);
        } else if (self instanceof RubyFloat && !context.runtime.isFloatReopened()) {
            return ((RubyFloat) self).op_gt(context, fixnum);
        }
        return super.call(context, caller, self, fixnum);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double flote) {
        if (self instanceof RubyFloat && !context.runtime.isFloatReopened()) {
            return ((RubyFloat) self).op_gt(context, flote);
        }
        return super.call(context, caller, self, flote);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (self instanceof RubyFixnum && !context.runtime.isFixnumReopened()) {
            return ((RubyFixnum) self).op_gt(context, arg);
        } else if (self instanceof RubyFloat && !context.runtime.isFloatReopened()) {
            return ((RubyFloat) self).op_gt(context, arg);
        }
        return super.call(context, caller, self, arg);
    }
}
