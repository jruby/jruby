package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.CFGData;
import org.jruby.interpreter.Interpreter;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.interpreter.NaiveInterpreterContext;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class InterpretedIRMethod extends DynamicMethod {
    private static final Logger LOG = LoggerFactory.getLogger("InterpretedIRMethod");

    private final boolean  isTopLevel;
    private final IRMethod method;
    boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRMethod method, RubyModule implementationClass) {
        super(implementationClass, Visibility.PRIVATE, CallConfiguration.FrameNoneScopeNone);
        this.method = method;
        this.isTopLevel = false;
    }

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRMethod method, RubyModule implementationClass, boolean isTopLevel) {
        super(implementationClass, Visibility.PRIVATE, CallConfiguration.FrameNoneScopeNone);
        this.method = method;
        this.isTopLevel = isTopLevel;
    }

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRMethod method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone);
        this.method = method;
        this.isTopLevel = false;
    }
    
    @Override
    public Arity getArity() {
        return method.getStaticScope().getArity();
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        if (Interpreter.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? method.getName() : name;
            LOG.info("Executing '" + realName + "'");
        }

        CFGData cfgData = method.getCFGData();
        if (cfgData == null) {
            // The base IR may not have been processed yet because the method is added dynamically.
            method.prepareForInterpretation();
            cfgData = method.getCFGData();
        }

        if (Interpreter.isDebug() && displayedCFG == false) {
            LOG.info("CFG:\n" + cfgData.cfg());
            LOG.info("\nInstructions:\n" + cfgData.toStringInstrs());
            displayedCFG = true;
        }

        context.pushScope(DynamicScope.newDynamicScope(method.getStaticScope()));
        // SSS FIXME: Is this correct?
        if (isTopLevel) context.getRuntime().getObject().setConstantQuiet("TOPLEVEL_BINDING", context.getRuntime().newBinding(context.currentBinding()));
        RubyModule currentModule = getImplementationClass();
        context.preMethodFrameOnly(currentModule, name, self, block);
        context.getCurrentScope().getStaticScope().setModule(clazz);
        context.setCurrentVisibility(getVisibility());
        InterpreterContext interp = new NaiveInterpreterContext(context, method, currentModule, self, name, args, block, null);
        try {
            return Interpreter.INTERPRET_METHOD(context, method, interp, self, name, currentModule, false);
        } finally {
            context.popFrame();
            context.postMethodScopeOnly();
        }
    }

    @Override
    public DynamicMethod dup() {
        return new InterpretedIRMethod(method, visibility, implementationClass);
    }
}
