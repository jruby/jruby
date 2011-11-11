/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class Match3Instr extends Instr implements ResultInstr {
    private final Variable result;
    private final Operand receiver;
    private final Operand arg;
    
    public Match3Instr(Variable result, Operand receiver, Operand arg) {
        super(Operation.MATCH3);
        
        assert result != null: "Match3Instr result is null";
        
        this.result = result;
        this.receiver = receiver;
        this.arg = arg;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { receiver, arg };
    }

    public Variable getResult() {
        return result;
    }
    
    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new Match3Instr((Variable) result.cloneForInlining(ii),
                receiver.cloneForInlining(ii), arg.cloneForInlining(ii));
    }

    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block) {
        RubyRegexp regexp = (RubyRegexp) receiver.retrieve(interp, context, self);
        IRubyObject argValue = (IRubyObject) arg.retrieve(interp, context, self);
        
        Object resultValue;
        if (argValue instanceof RubyString) {
            resultValue = regexp.op_match(context, argValue);
        } else {
            resultValue = argValue.callMethod(context, "=~", regexp);
        }
        
        result.store(interp, context, self, resultValue);                
        return null;                
    }
}
