package org.jruby.ast.executable;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public interface Script {
    public IRubyObject run(ThreadContext context, IRubyObject self);
}
