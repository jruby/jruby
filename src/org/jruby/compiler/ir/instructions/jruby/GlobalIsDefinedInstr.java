/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import java.util.Map;

import org.jruby.Ruby;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class GlobalIsDefinedInstr extends DefinedInstr {
   public GlobalIsDefinedInstr(Variable result, StringLiteral name) {
        super(Operation.GLOBAL_IS_DEFINED, result, new Operand[] { name });
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
         operands[0] = operands[0].getSimplifiedOperand(valueMap, force);
    }
    
    public StringLiteral getName() {
        return (StringLiteral) operands[0];
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new GlobalIsDefinedInstr((Variable) getResult().cloneForInlining(inlinerInfo), 
                (StringLiteral) getOperands()[0].cloneForInlining(inlinerInfo));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getName() + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        
        return runtime.newBoolean(runtime.getGlobalVariables().isDefined(getName().string));
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    }
}
