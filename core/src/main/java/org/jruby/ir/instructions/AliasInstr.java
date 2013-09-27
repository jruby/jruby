/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions;

import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

/**
 *
 * @author enebo
 */
public class AliasInstr extends Instr {
    private final Variable receiver;
    private Operand newName;
    private Operand oldName;

    public AliasInstr(Variable receiver, Operand newName, Operand oldName) {
        super(Operation.ALIAS);

        this.receiver = receiver;
        this.newName = newName;
        this.oldName = oldName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] {getReceiver(), getNewName(), getOldName()};
    }

    @Override
    public String toString() {
        return getOperation().toString() + "(" + getReceiver() + ", " + getNewName() + ", " + getOldName() + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        oldName = getOldName().getSimplifiedOperand(valueMap, force);
        newName = getNewName().getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new AliasInstr((Variable) receiver.cloneForInlining(ii), getNewName().cloneForInlining(ii),
                getOldName().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, currDynScope, temp);

        if (object == null || object instanceof RubyFixnum || object instanceof RubySymbol) {
            throw context.runtime.newTypeError("no class to make alias");
        }

        String newNameString = getNewName().retrieve(context, self, currDynScope, temp).toString();
        String oldNameString = getOldName().retrieve(context, self, currDynScope, temp).toString();

        RubyModule module = (object instanceof RubyModule) ? (RubyModule) object : object.getMetaClass();
        module.defineAlias(newNameString, oldNameString);

        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AliasInstr(this);
    }

    public Variable getReceiver() {
        return receiver;
    }

    public Operand getNewName() {
        return newName;
    }

    public Operand getOldName() {
        return oldName;
    }
}
