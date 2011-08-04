package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.RubyClass;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * What is the class of the operand supplied
 */
public class ClassOf extends OneOperandInstr {
    public ClassOf(Variable destination, Operand value) {
        super(Operation.CLASS_OF, destination, value);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ClassOf(ii.getRenamedVariable(result), getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
		  IRubyObject arg = (IRubyObject) getArg().retrieve(interp);
        getResult().store(interp, (arg instanceof RubyClass) ? ((RubyClass)arg).getRealClass() : arg.getType());

        return null;
    }
}
