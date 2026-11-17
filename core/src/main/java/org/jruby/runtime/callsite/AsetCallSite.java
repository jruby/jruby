package org.jruby.runtime.callsite;

import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyBasicObject.getMetaClass;
import static org.jruby.api.Access.arrayClass;

public class AsetCallSite extends MonomorphicCallSite {
    public AsetCallSite() {
        super("[]=");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        return getMetaClass(self) == arrayClass(context) ?
                ((RubyArray) self).aset(context, arg0, arg1) : super.call(context, caller, self, arg0, arg1);
    }
}
