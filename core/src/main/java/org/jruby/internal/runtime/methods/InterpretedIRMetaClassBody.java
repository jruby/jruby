package org.jruby.internal.runtime.methods;

import java.util.ArrayList;
import java.util.List;

import org.jruby.RubyModule;
import org.jruby.ir.*;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRMetaClassBody extends InterpretedIRMethod {
    public InterpretedIRMetaClassBody(IRScope metaClassBody, RubyModule implementationClass) {
        super(metaClassBody, Visibility.PUBLIC, implementationClass);
    }

    public List<String[]> getParameterList() {
        return new ArrayList<String[]>();
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
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        DynamicMethodBox box = this.box;
        if (box.callCount >= 0) tryJit(context, box);
        DynamicMethod actualMethod = box.actualMethod;
        if (actualMethod != null) return actualMethod.call(context, self, clazz, name, args, block);

        InterpreterContext ic = ensureInstrsReady();

        if (IRRuntimeHelpers.isDebug()) doDebug();

        if (ic.hasExplicitCallProtocol()) {
            return Interpreter.INTERPRET_METHOD(context, this, self, name, args, block);
        } else {
            try {
                pre(ic, context, self, name, block);

                return Interpreter.INTERPRET_METHOD(context, this, self, name, args, block);
            } finally {
                post(ic, context);
            }
        }
    }

    @Override
    public DynamicMethod dup() {
        InterpretedIRMetaClassBody x = new InterpretedIRMetaClassBody(method, implementationClass);
        x.dupBox(this);

        return x;
    }
}
