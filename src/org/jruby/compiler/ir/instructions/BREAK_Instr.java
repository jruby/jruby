package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
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
public class BREAK_Instr extends Instr
{
    public final Label target; 
    private Operand returnValue;

    public BREAK_Instr(Operand rv, Label target) {
        super(Operation.BREAK, null);
        this.returnValue = rv;
        this.target = target;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new BREAK_Instr(returnValue.cloneForInlining(ii), (Label)(target == null ? null : target.cloneForInlining(ii)));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + returnValue + ", " + target + ")";
    }

    @Override
    public Operand[] getOperands() {
        return target == null ? new Operand[] { returnValue } : new Operand[] { returnValue, target };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        returnValue = returnValue.getSimplifiedOperand(valueMap);
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        interp.setReturnValue(returnValue.retrieve(interp));
        return target == null ? interp.getMethodExitLabel() : target;
    }
}
