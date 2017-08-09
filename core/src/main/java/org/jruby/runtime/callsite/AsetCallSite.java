package org.jruby.runtime.callsite;

import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AsetCallSite extends NormalCachingCallSite {
    public AsetCallSite() {
        super("[]=");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        if (self.getMetaClass() == context.runtime.getArray()) {
            return ((RubyArray) self).aset(arg0, arg1);
        }
        return super.call(context, caller, self, arg0, arg1);
    }
}
