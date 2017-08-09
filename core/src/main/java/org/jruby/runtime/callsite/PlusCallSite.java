package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PlusCallSite extends NormalCachingCallSite2 {

    public PlusCallSite() {
        super("+");
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long arg) {
        if (self instanceof RubyFixnum) {
            if (isBuiltin(self.getMetaClass())) return ((RubyFixnum) self).op_plus(context, arg);
        } else if (self instanceof RubyFloat) {
            if (isBuiltin2(self.getMetaClass())) return ((RubyFloat) self).op_plus(context, arg);
        }
        return super.call(context, caller, self, arg);
    }

    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double arg) {
        if (self instanceof RubyFloat && isBuiltin2(self.getMetaClass())) {
            return ((RubyFloat) self).op_plus(context, arg);
        }
        return super.call(context, caller, self, arg);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (self instanceof RubyFixnum) {
            if (isBuiltin(self.getMetaClass())) return ((RubyFixnum) self).op_plus(context, arg);
        } else if (self instanceof RubyFloat) {
            if (isBuiltin2(self.getMetaClass())) return ((RubyFloat) self).op_plus(context, arg);
        }
        return super.call(context, caller, self, arg);
    }

}
