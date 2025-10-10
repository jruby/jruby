package org.jruby.ast.executable;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public interface Script {
    public IRubyObject __file__(ThreadContext context, IRubyObject self, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg, IRubyObject arg2, IRubyObject arg3, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    
    public IRubyObject run(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    @Deprecated(since = "1.7.0")
    public IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    public IRubyObject load(ThreadContext context, IRubyObject self, boolean wrap);
    public void setFilename(String filename);
    public void setRootScope(StaticScope scope);
}
