package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;

public class CompiledIRNoProtocolMethod extends AbstractIRMethod {
    private final boolean needsDynamicScope;
    private final MethodHandle variable;

    public CompiledIRNoProtocolMethod(MethodHandle handle, StaticScope scope, String file, int line, RubyModule implementationClass, boolean needsDynamicScope) {
        super(scope, file, line, Visibility.PUBLIC, implementationClass);

        this.needsDynamicScope = needsDynamicScope;
        this.variable = handle;
    }

    public ArgumentDescriptor[] getArgumentDescriptors() {
        return ArgumentDescriptor.EMPTY_ARRAY;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        StaticScope staticScope1 = this.staticScope;
        RubyModule implementationClass1 = this.implementationClass;
        pre(context, staticScope1, implementationClass1, self, name, block);

        try {
            return (IRubyObject) this.variable.invokeExact(context, staticScope1, self, IRubyObject.NULL_ARRAY, block, implementationClass1, name);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        } finally {
            post(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        throw new RuntimeException("BUG: this path should never be called");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        throw new RuntimeException("BUG: this path should never be called");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        throw new RuntimeException("BUG: this path should never be called");
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        throw new RuntimeException("BUG: this path should never be called");
    }

    protected void post(ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (needsDynamicScope) {
            context.popScope();
        }
    }

    protected void pre(ThreadContext context, StaticScope staticScope, RubyModule implementationClass, IRubyObject self, String name, Block block) {
        // update call stacks (push: frame, class, needsDynamicScope, etc.)
        context.preMethodFrameOnly(implementationClass, name, self, getVisibility(), block);
        if (needsDynamicScope) {
            // Add a parent-link to current dynscope to support non-local returns cheaply
            // This doesn't affect variable scoping since local variables will all have
            // the right needsDynamicScope depth.
            context.pushScope(DynamicScope.newDynamicScope(staticScope, context.getCurrentScope()));
        }
    }

    @Override
    public InterpreterContext ensureInstrsReady() {
        // AbstractIRMethod.getMethodData() calls this and we want IC since we have not eliminated any get/put fields.
        return getIRScope().getInterpreterContext();
    }

    @Override
    protected void printMethodIR() {
        // no-op
    }
}
