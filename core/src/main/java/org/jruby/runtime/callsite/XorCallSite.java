package org.jruby.runtime.callsite;

import org.jruby.RubyFixnum;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyBasicObject.getMetaClass;

public class XorCallSite extends MonomorphicCallSite {
    public XorCallSite() {
        super("^");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        if (self instanceof RubyFixnum && isBuiltin(getMetaClass(self))) {
            return ((RubyFixnum) self).op_xor(context, fixnum);
        }
        return super.call(context, caller, self, fixnum);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (self instanceof RubyFixnum && isBuiltin(getMetaClass(self))) {
            return ((RubyFixnum) self).op_xor(context, arg);
        }
        return super.call(context, caller, self, arg);
    }
}
