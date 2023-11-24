package org.jruby.ir.targets.indy;

import com.headius.invokebinder.Binder;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.JIT;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.insertArguments;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class MetaClassBootstrap {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final Handle OPEN_META_CLASS = new Handle(
            Opcodes.H_INVOKESTATIC,
            p(MetaClassBootstrap.class),
            "openMetaClass",
            sig(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, MethodHandle.class, int.class, int.class, int.class),
            false);
    private static final MethodHandle OPEN_META_CLASS_HANDLE =
            Binder
                    .from(DynamicMethod.class, ThreadContext.class, IRubyObject.class, String.class, StaticScope.class, MethodHandle.class, StaticScope.class, MethodHandle.class, int.class, boolean.class, boolean.class)
                    .invokeStaticQuiet(LOOKUP, MetaClassBootstrap.class, "openMetaClass");

    @JIT
    public static CallSite openMetaClass(MethodHandles.Lookup lookup, String name, MethodType type, MethodHandle body, MethodHandle scope, MethodHandle setScope, int line, int dynscopeEliminated, int refinements) {
        try {
            StaticScope staticScope = (StaticScope) scope.invokeExact();
            return new ConstantCallSite(insertArguments(OPEN_META_CLASS_HANDLE, 4, body, staticScope, setScope, line, dynscopeEliminated == 1 ? true : false, refinements == 1 ? true : false));
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @JIT
    public static DynamicMethod openMetaClass(ThreadContext context, IRubyObject object, String descriptor, StaticScope parent, MethodHandle body, StaticScope scope, MethodHandle setScope, int line, boolean dynscopeEliminated, boolean refinements) throws Throwable {
        if (scope == null) {
            scope = Helpers.restoreScope(descriptor, parent);
            setScope.invokeExact(scope);
        }
        return IRRuntimeHelpers.newCompiledMetaClass(context, body, scope, object, line, dynscopeEliminated, refinements);
    }
}
