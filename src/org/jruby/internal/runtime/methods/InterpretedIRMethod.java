package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.interpreter.Interpreter;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.interpreter.NaiveInterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRMethod extends DynamicMethod {
    public final IRMethod method;
    private final int temporaryVariableSize;
    boolean displayedCFG = false; // FIXME: Remove when we find nicer way of logging CFG

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRMethod method, RubyModule implementationClass) {
        super(implementationClass, Visibility.PRIVATE, CallConfiguration.FrameNoneScopeNone);

        this.temporaryVariableSize = method.getTemporaryVariableSize();
        this.method = method;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
            IRubyObject[] args, Block block) {
        InterpreterContext interp = new NaiveInterpreterContext(context, self, method.getLocalVariablesCount(),
                temporaryVariableSize, method.getRenamedVariableSize(), args, block);
//        Arity.checkArgumentCount(context.getRuntime(), args.length, requiredArgsCount, method.get???);
        if (Interpreter.isDebug()) {
            // FIXME: name should probably not be "" ever.
            String realName = name == null || "".equals(name) ? method.getName() : name;
            System.out.println("Executing '" + realName + "'");
        }

        CFG c = method.getCFG();
        if (c == null) {
            // The base IR may not have been processed yet because the method is added dynamically.
            method.prepareForInterpretation();
            c = method.getCFG();
        }
        if (Interpreter.isDebug() && displayedCFG == false) {
            System.out.println("CFG:\n" + c.getGraph());
            System.out.println("\nInstructions:\n" + c.toStringInstrs());
            displayedCFG = true;
        }

        return Interpreter.INTERPRET_METHOD(context, c, interp, name, getImplementationClass(), false);
    }

    @Override
    public DynamicMethod dup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
