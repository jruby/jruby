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

public class EnsureRubyArrayInstr extends Instr implements ResultInstr {
    private Operand object;
    private final Variable result;

    public EnsureRubyArrayInstr(Variable result, Operand s) {
        super(Operation.ENSURE_RUBY_ARRAY);
        
        this.object = s;
        this.result = result;
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        return (object instanceof Array) ? object : null;
    }

    public Operand[] getOperands() {
        return new Operand[]{object};
    }
    
    public Variable getResult() {
        return result;
    }
    
    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        object = object.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + object + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new EnsureRubyArrayInstr(ii.getRenamedVariable(result), object.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject val = (IRubyObject)object.retrieve(interp, context, self);
        if (!(val instanceof RubyArray)) val = ArgsUtil.convertToRubyArray(context.getRuntime(), val, false);
        result.store(interp, context, self, val);
        return null;
    }
}
