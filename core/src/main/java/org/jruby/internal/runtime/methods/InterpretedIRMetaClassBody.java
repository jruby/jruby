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

    protected void post(InterpreterContext ic, ThreadContext context) {
        // update call stacks (pop: ..)
        context.popFrame();
        if (ic.popDynScope()) {
            context.popScope();
        }
    }

    protected void pre(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, Block block) {
        // update call stacks (push: frame, class, scope, etc.)
        context.preMethodFrameOnly(getImplementationClass(), name, self, block);
        if (ic.pushNewDynScope()) {
            // Add a parent-link to current dynscope to support non-local returns cheaply
            // This doesn't affect variable scoping since local variables will all have
            // the right scope depth.
            context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope(), context.getCurrentScope()));
        }
        context.setCurrentVisibility(getVisibility());
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        InterpreterContext ic = ensureInstrsReady();

        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (ic.hasExplicitCallProtocol()) {
            return ic.engine.interpret(context, self, ic, getImplementationClass().getMethodLocation(), name, block, null);
        } else {
            try {
                pre(ic, context, self, name, block);

                return ic.engine.interpret(context, self, ic, getImplementationClass().getMethodLocation(), name, block, null);
            } finally {
                post(ic, context);
            }
        }
    }
}
