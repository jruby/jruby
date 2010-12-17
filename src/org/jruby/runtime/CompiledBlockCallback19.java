package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

public interface CompiledBlockCallback19 {
    public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    public String getFile();
    public int getLine();
}
