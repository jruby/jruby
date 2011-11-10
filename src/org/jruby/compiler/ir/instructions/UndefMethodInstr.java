/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public class UndefMethodInstr extends Instr implements ResultInstr {
    private final Variable result;
    private final Operand methodName;
    
    public UndefMethodInstr(Variable result, Operand methodName) {
        super(Operation.UNDEF_METHOD);
        
        this.result = result;
        this.methodName = methodName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { methodName };
    }
    
    public Variable getResult() {
        return result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new UndefMethodInstr((Variable) result.cloneForInlining(ii),
                methodName.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRExecutionScope scope, ThreadContext context, IRubyObject self) {
        result.store(interp, context, self, 
                RuntimeHelpers.undefMethod(context, methodName.retrieve(interp, context, self)));
        return null;
    }
    
}
