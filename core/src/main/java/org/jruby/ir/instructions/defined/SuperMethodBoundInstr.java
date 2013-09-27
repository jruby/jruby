/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions.defined;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

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
                flag = Helpers.findImplementerIfNecessary(receiver.getMetaClass(), frameClass).getSuperClass().isMethodBound(frameName, false);
            }
        }
        return context.runtime.newBoolean(flag);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SuperMethodBoundInstr(this);
    }
}
