package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

//    is_true(a) = (!a.nil? && a != false) 
//
// Only nil and false compute to false
//
public class IsTrueInstr extends OneOperandInstr {
    public IsTrueInstr(Variable result, Operand arg) {
        super(Operation.IS_TRUE, result, arg);
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        if (!argument.isConstant()) return null;

        return argument == Nil.NIL || argument == BooleanLiteral.FALSE ?
                    BooleanLiteral.FALSE : BooleanLiteral.TRUE;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new IsTrueInstr(ii.getRenamedVariable(getResult()), argument.cloneForInlining(ii));
    }

    // Can this instruction raise exceptions?
    @Override
    public boolean canRaiseException() {
        return false;
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // ENEBO: This seems like a lot of extra work...
        getResult().store(interp, context, self, 
                context.getRuntime().newBoolean(((IRubyObject) getArg().retrieve(interp, context, self)).isTrue()));
        return null;
    }
}
