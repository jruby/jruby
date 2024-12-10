package org.jruby.runtime.callsite;

import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyBasicObject.getMetaClass;
import static org.jruby.api.Access.arrayClass;

public class ArefCallSite extends MonomorphicCallSite {
    public ArefCallSite() {
        super("[]");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, long fixnum) {
        if (getMetaClass(self) == arrayClass(context)) {
            return ((RubyArray) self).entry(fixnum);
        }
        return super.call(context, caller, self, fixnum);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg) {
        if (getMetaClass(self) == arrayClass(context)) {
            return ((RubyArray) self).aref(context, arg);
        }
        return super.call(context, caller, self, arg);
    }
}
