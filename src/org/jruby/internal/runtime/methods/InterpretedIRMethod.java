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
            String realName;
            // FIXME: name should probably not be "" ever.
            if (name == null || "".equals(name)) {
                realName = method.getName();
            } else {
                realName = name;
            }

            System.out.println("Executing '" + realName + "'");
        }

        CFG c = method.getCFG();
        if (c == null) {
           // The base IR may not have been processed yet because the method is added dynamically.
           method.prepareForInterpretation();
           c = method.getCFG();
        }
        return Interpreter.interpret(context, c, interp);
    }

    @Override
    public DynamicMethod dup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
