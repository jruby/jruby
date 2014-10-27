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

import java.lang.invoke.MethodType;

import static org.jruby.ir.runtime.IRRuntimeHelpers.splatArguments;

/**
* Created by headius on 10/23/14.
*/
public class InstanceSuperInvokeSite extends SuperInvokeSite {
    public InstanceSuperInvokeSite(MethodType type, String name, String splatmapString) {
        super(type, name, splatmapString);
    }

    public IRubyObject invoke(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        return fail(context, caller, self, definingModule, args, block);
    }

    public IRubyObject fail(ThreadContext context, IRubyObject caller, IRubyObject self, RubyClass definingModule, IRubyObject[] args, Block block) throws Throwable {
        // TODO: get rid of caller

        RubyClass superClass = definingModule.getSuperClass();
        String name = methodName;
        CacheEntry entry = cache;

        if (entry.typeOk(superClass)) {
            return entry.method.call(context, self, superClass, name, splatArguments(args, splatMap), block);
        }

        entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;

        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), methodName, callType, splatArguments(args, splatMap), block);
        }

        cache = entry;

        return method.call(context, self, superClass, methodName, splatArguments(args, splatMap), block);
    }
}
