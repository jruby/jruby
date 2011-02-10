package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// A break instruction is not just any old instruction.
// Like a return instruction, it exits a scope and returns a value
//
// Ex: (1..5).collect { |n| break if n > 3; n } returns nil
//
// All break instructions like returns have an associated return value
// In the absence of an explicit value to return, nil is returned
//
// Ex: (1..5).collect { |n| break "Hurrah" if n > 3; n } returns "Hurrah"
//
// But, whereas a return exits the innermost method it is in,
// a break only exits out of the innermost non-method scope it is in.
// So, an exposed/naked break inside a method throws an exception!
//
// def foo(n); break if n > 5; end; foo(100) will throw an exception
//
public class BREAK_Instr extends OneOperandInstr
{
    public BREAK_Instr(Operand rv) {
        super(Operation.BREAK, null, rv);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BREAK_Instr(argument.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        interp.setReturnValue(getArg().retrieve(interp));
        return interp.getMethodExitLabel();
    }
}
