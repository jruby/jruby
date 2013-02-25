package org.jruby.internal.runtime.methods;

import java.util.ArrayList;
import java.util.List;

import org.jruby.RubyModule;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class InterpretedIRMethod extends DynamicMethod implements IRMethodArgs, PositionAware {
    private static final Logger LOG = LoggerFactory.getLogger("InterpretedIRMethod");

    private final IRScope method;
    private Arity arity;
    boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG
    
    public InterpretedIRMethod(IRScope method, Visibility visibility, RubyModule implementationClass) {
        super(implementationClass, visibility, CallConfiguration.FrameNoneScopeNone);
        this.method = method;
        this.method.getStaticScope().determineModule();
        this.arity = calculateArity();
    }

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRScope method, RubyModule implementationClass) {
        this(method, Visibility.PRIVATE, implementationClass);
    }
    
    public IRScope getIRMethod() {
        return method;
    }

    public List<String[]> getParameterList() {
        return (method instanceof IRMethod) ? ((IRMethod)method).getArgDesc() : new ArrayList<String[]>();
    }

    private Arity calculateArity() {
        StaticScope s = method.getStaticScope();
        if (s.getOptionalArgs() > 0 || s.getRestArg() >= 0) return Arity.required(s.getRequiredArgs());

        return Arity.createArity(s.getRequiredArgs());
    }

    @Override
    public Arity getArity() {
        return this.arity;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        // SSS FIXME: Move this out of here to some other place?
        // Prepare method if not yet done so we know if the method has an explicit/implicit call protocol
        if (method.getInstrsForInterpretation() == null) method.prepareForInterpretation(false);

        if (IRRuntimeHelpers.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? method.getName() : name;
            LOG.info("Executing '" + realName + "'");
            if (displayedCFG == false) {
                // The base IR may not have been processed yet
                CFG cfg = method.getCFG();
                LOG.info("Graph:\n" + cfg.toStringGraph());
                LOG.info("CFG:\n" + cfg.toStringInstrs());
                displayedCFG = true;
            }
        }

        if (method.hasExplicitCallProtocol()) {
            return Interpreter.INTERPRET_METHOD(context, this, self, name, args, block, null, false);
        } else {
            try {
                // update call stacks (push: frame, class, scope, etc.)
                context.preMethodFrameAndScope(getImplementationClass(), name, self, block, method.getStaticScope());
                context.setCurrentVisibility(getVisibility());
                return Interpreter.INTERPRET_METHOD(context, this, self, name, args, block, null, false);
            } finally {
                // update call stacks (pop: ..)
                context.popFrame();
                context.popRubyClass();
                context.popScope();
            }
        }
    }

    @Override
    public DynamicMethod dup() {
        return new InterpretedIRMethod(method, visibility, implementationClass);
    }

    public String getFile() {
        return method.getFileName();
    }

    public int getLine() {
        return method.getLineNumber();
   }
}
