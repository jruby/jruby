package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.interpreter.Interpreter;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.interpreter.NaiveInterpreterContext;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRMethod extends DynamicMethod {
    public final IRMethod method;
    boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRMethod method, RubyModule implementationClass) {
        super(implementationClass, Visibility.PRIVATE, CallConfiguration.FrameNoneScopeNone);
        this.method = method;
    }

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRMethod method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone);
        this.method = method;
    }
    
    @Override
    public Arity getArity() {
        return method.getStaticScope().getArity();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        RubyModule currentModule = getImplementationClass();
        context.preMethodFrameOnly(currentModule, name, self, block);
        InterpreterContext interp = new NaiveInterpreterContext(context, method, currentModule, self, name, args, block, null);
        if (Interpreter.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? method.getName() : name;
            System.out.println("Executing '" + realName + "'");
        }

        CFG cfg = method.getCFG();
        if (cfg == null) {
            // The base IR may not have been processed yet because the method is added dynamically.
            method.prepareForInterpretation();
            cfg = method.getCFG();
        }
        // Do this *after* the method has been prepared!
        interp.allocateSharedBindingScope(context, method);
        if (Interpreter.isDebug() && displayedCFG == false) {
            System.out.println("CFG:\n" + cfg.getGraph());
            System.out.println("\nInstructions:\n" + cfg.toStringInstrs());
            displayedCFG = true;
        }

        context.getCurrentScope().getStaticScope().setModule(clazz);
        try {
            return Interpreter.INTERPRET_METHOD(context, method, interp, self, name, getImplementationClass(), false);
        } finally {
            context.popFrame();
            interp.setFrame(null);
            if (interp.hasAllocatedDynamicScope()) context.postMethodScopeOnly();
        }
    }

    @Override
    public DynamicMethod dup() {
        return new InterpretedIRMethod(method, visibility, implementationClass);
    }
}
