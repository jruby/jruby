package org.jruby.ir.targets.indy;

import org.jruby.RubyClass;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodType;

/**
* Created by headius on 10/23/14.
*/
public class ZSuperInvokeSite extends SuperInvokeSite {
    public ZSuperInvokeSite(MethodType type, String name, String splatmapString, int flags, String file, int line) {
        super(type, name, splatmapString, flags, file, line);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        if (block == null || !block.isGiven()) block = context.getFrameBlock();
        return IRRuntimeHelpers.zSuperSplatArgs(context, self, flags, args, block, splatMap);
    }

    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        return invoke(context, caller, self, definingModule, args, block);
    }
}
