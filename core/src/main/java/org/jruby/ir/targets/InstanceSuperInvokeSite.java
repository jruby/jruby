package org.jruby.ir.targets;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;

import static org.jruby.ir.runtime.IRRuntimeHelpers.splatArguments;

/**
* Created by headius on 10/23/14.
*/
public class InstanceSuperInvokeSite extends ResolvedSuperInvokeSite {
    public InstanceSuperInvokeSite(MethodType type, String name, String splatmapString, String file, int line) {
        super(type, name, splatmapString, file, line);
    }

    @Override
    protected RubyClass getSuperClass(RubyClass definingModule) {
        return definingModule.getSuperClass();
    }

    // FIXME: indy cached version was not doing splat mapping; revert to slow logic for now

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        // TODO: get rid of caller
        // TODO: caching
        return IRRuntimeHelpers.instanceSuperSplatArgs(context, self, superName, definingModule, args, block, splatMap);
    }
}
