package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.RubyArray;

public class EnsureRubyArrayInstr extends OneOperandInstr {
    public EnsureRubyArrayInstr(Variable d, Operand s) {
        super(Operation.ENSURE_RUBY_ARRAY, d, s);
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        return (getArg() instanceof Array) ? getArg() : null;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new EnsureRubyArrayInstr(ii.getRenamedVariable(getResult()), argument.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject val = (IRubyObject)getArg().retrieve(interp, context, self);
        if (!(val instanceof RubyArray)) val = ArgsUtil.convertToRubyArray(context.getRuntime(), val, false);
        getResult().store(interp, context, self, val);
        return null;
    }
}
