package org.jruby.ast.executable;

import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public interface Script {
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    public IRubyObject run(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    public IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
}
