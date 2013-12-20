package org.jruby.ir.util;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

/**
 * Logic shared by both interpreter and JIT. Ensure both sides are happy with any changes made here.
 */
public class SharedLogic {
    public static IRubyObject defCompiledIRMethod(ThreadContext context, MethodHandle handle, String rubyName, StaticScope parentScope, String scopeDesc,
                                  String filename, int line, String parameterDesc) {
        Ruby runtime = context.runtime;

        RubyModule containingClass = context.getRubyClass();
        Visibility visibility = context.getCurrentVisibility();

        visibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, containingClass, rubyName, visibility);

        StaticScope scope = Helpers.decodeScope(context, parentScope, scopeDesc);

        DynamicMethod method = new CompiledIRMethod(handle, rubyName, filename, line, scope, visibility, containingClass, parameterDesc);

        return Helpers.addInstanceMethod(containingClass, rubyName, method, visibility, context, runtime);
    }
}
