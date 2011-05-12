package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
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
        return new CopyInstr(ii.getRenamedVariable(result), getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, ((IRubyObject) getArg().retrieve(interp)).getType());

        return null;
    }
}
