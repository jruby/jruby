package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CmpCallSite extends NormalCachingCallSite {

    public CmpCallSite() {
        super("<=>");
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        if (self instanceof RubyFixnum && !context.runtime.isFixnumReopened()) {
            return ((RubyFixnum) self).op_cmp(context, fixnum);
        } else if (self instanceof RubyFloat && !context.runtime.isFloatReopened()) {
            return ((RubyFloat) self).op_cmp(context, fixnum);
        }
        return super.call(context, caller, self, fixnum);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double flote) {
        if (self instanceof RubyFloat && !context.runtime.isFloatReopened()) {
            return ((RubyFloat) self).op_cmp(context, flote);
        }
        return super.call(context, caller, self, flote);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (self instanceof RubyFixnum && !context.runtime.isFixnumReopened()) {
            return ((RubyFixnum) self).op_cmp(context, arg);
        } else if (self instanceof RubyFloat && !context.runtime.isFloatReopened()) {
            return ((RubyFloat) self).op_cmp(context, arg);
        }
        return super.call(context, caller, self, arg);
    }
}
