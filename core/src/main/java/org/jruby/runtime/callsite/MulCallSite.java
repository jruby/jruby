package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyBasicObject.getMetaClass;

public class MulCallSite extends BimorphicCallSite {

    public MulCallSite() {
        super("*");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long arg) {
        if (self instanceof RubyFixnum) {
            if (isBuiltin(getMetaClass(self))) return ((RubyFixnum) self).op_mul(context, arg);
        } else if (self instanceof RubyFloat) {
            if (isSecondaryBuiltin(getMetaClass(self))) return ((RubyFloat) self).op_mul(context, arg);
        }
        return super.call(context, caller, self, arg);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, double arg) {
        if (self instanceof RubyFixnum) {
            if (isBuiltin(getMetaClass(self))) return ((RubyFixnum) self).op_mul(context, arg);
        } else if (self instanceof RubyFloat) {
            if (isSecondaryBuiltin(getMetaClass(self))) return ((RubyFloat) self).op_mul(context, arg);
        }
        return super.call(context, caller, self, arg);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (self instanceof RubyFixnum) {
            if (isBuiltin(getMetaClass(self))) return ((RubyFixnum) self).op_mul(context, arg);
        } else if (self instanceof RubyFloat) {
            if (isSecondaryBuiltin(getMetaClass(self))) return ((RubyFloat) self).op_mul(context, arg);
        }
        return super.call(context, caller, self, arg);
    }

}
