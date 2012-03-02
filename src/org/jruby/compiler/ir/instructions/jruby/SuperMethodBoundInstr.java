/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class SuperMethodBoundInstr extends DefinedInstr {
   public SuperMethodBoundInstr(Variable result, Operand object) {
        super(Operation.SUPER_METHOD_BOUND, result, new Operand[] { object });
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
         operands[0] = operands[0].getSimplifiedOperand(valueMap, force);
    }
    
    public Operand getObject() {
        return operands[0];
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new SuperMethodBoundInstr((Variable) getResult().cloneForInlining(inlinerInfo), 
                getObject().cloneForInlining(inlinerInfo));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getObject() + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject receiver = (IRubyObject) getObject().retrieve(context, self, currDynScope, temp);
        boolean flag = false;
        String frameName = context.getFrameName();
        if (frameName != null) {
            RubyModule frameClass = context.getFrameKlazz();
            if (frameClass != null) {
                flag = RuntimeHelpers.findImplementerIfNecessary(receiver.getMetaClass(), frameClass).getSuperClass().isMethodBound(frameName, false);
            }
        }
        return context.runtime.newBoolean(flag);        
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    }
}
