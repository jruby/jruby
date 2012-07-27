package org.jruby.runtime.callsite;

import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ArefCallSite extends NormalCachingCallSite {
    public ArefCallSite() {
        super("[]");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        if (self.getMetaClass() == context.runtime.getArray()) {
            return ((RubyArray) self).entry(fixnum);
        }
        return super.call(context, caller, self, fixnum);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (self.getMetaClass() == context.runtime.getArray()) {
            return ((RubyArray) self).aref(arg);
        }
        return super.call(context, caller, self, arg);
    }
}
