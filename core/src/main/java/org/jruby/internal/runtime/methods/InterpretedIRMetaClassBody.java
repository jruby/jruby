package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRMetaClassBody extends InterpretedIRBodyMethod {
    public InterpretedIRMetaClassBody(IRScope metaClassBody, RubyModule implementationClass) {
        super(metaClassBody, implementationClass);
    }

    @Override
    protected void post(InterpreterContext ic, ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (ic.popDynScope()) {
            context.popScope();
        }
    }

    @Override
    protected DynamicScope pre(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block, RubyModule implClass) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(getImplementationClass(), name, self, block);
        context.setCurrentVisibility(getVisibility());
        if (ic.pushNewDynScope()) {
            // Add a parent-link to current dynscope to support non-local returns cheaply
            // This doesn't affect variable scoping since local variables will all have
            // the right scope depth.
            DynamicScope currentScope = context.getCurrentScope();
            DynamicScope scope = DynamicScope.newDynamicScope(ic.getStaticScope(), currentScope);
            context.pushScope(scope);
            return scope;
        }
        return null;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (IRRuntimeHelpers.isDebug()) doDebug();

        return callInternal(context, self, clazz, name, block);
    }
}
