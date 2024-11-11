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
public class ModuleSuperInvokeSite extends SuperInvokeSite {
    public ModuleSuperInvokeSite(MethodType type, String name, String splatmapString, String file, int line) {
        super(type, name, splatmapString, file, line);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        return IRRuntimeHelpers.moduleSuperSplatArgs(context, self, superName, args, block, splatMap);
    }

    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        return invoke(context, caller, self, definingModule, args, block);
    }
}
