/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.operands.Variable;

/**
 * Common base class for all defined-category instructions.
 */
public abstract class DefinedInstr extends Instr implements ResultInstr {
    protected Variable result;
    
    public DefinedInstr(Operation operation, Variable result) {
        super(operation);
        
        this.result = result;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        result = v;
    }    
}
