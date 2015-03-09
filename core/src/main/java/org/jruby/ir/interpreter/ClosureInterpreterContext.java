package org.jruby.ir.interpreter;

import java.util.List;
import org.jruby.ir.IRClosure;
import org.jruby.ir.instructions.Instr;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;

/**
 * Interpreter knowledge needed to interpret a closure.
 */
public class ClosureInterpreterContext extends InterpreterContext {
    public ClosureInterpreterContext(IRClosure scope, List<Instr> instructions) {
        super(scope, instructions);
    }

    /**
     * Blocks have more complicated logic for pushing a dynamic scope (see InterpretedIRBlockBody).
     * We throw an error in case somehow we mistakenly try and push a binding.
     */
    @Override
    public DynamicScope newDynamicScope(ThreadContext context) {
        throw new RuntimeException("We do not push bindings for closures");
    }
}
