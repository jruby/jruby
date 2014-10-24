package org.jruby.ir.targets;

import org.jruby.RubyClass;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodType;

/**
* Created by headius on 10/23/14.
*/
public class InstanceSuperInvokeSite extends SuperInvokeSite {
    public InstanceSuperInvokeSite(MethodType type, String name) {
        super(type, name);
    }

    public IRubyObject invoke(String methodName, boolean[] splatMap, ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        return IRRuntimeHelpers.instanceSuperSplatArgs(context, self, methodName, definingModule, args, block, splatMap);
    }
}
