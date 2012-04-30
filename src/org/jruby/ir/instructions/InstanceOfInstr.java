package org.jruby.ir.instructions;

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

// SSS FIXME: Get rid of this instruction
public class InstanceOfInstr extends Instr implements ResultInstr {
    private Class type;
    private String className;
    private Operand object;
    private Variable result;

    public InstanceOfInstr(Variable result, Operand object, String className) {
        super(Operation.INSTANCE_OF);

        assert result != null : "InstanceOfInstr result is null";
        
        this.object = object;
        this.className = className;
        this.result = result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new InstanceOfInstr(ii.getRenamedVariable(result), object.cloneForInlining(ii), className);
    }

    public Operand[] getOperands() {
        return new Operand[]{object};
    }

    public Variable getResult() {
        return result;
    }
    
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        object = object.getSimplifiedOperand(valueMap, force);
    }

    @Override 
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (result + " = ") + getOperation() + "(" + object + ", " + className + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        try {
            if (type == null) type = Class.forName(className);
        } catch (ClassNotFoundException e) {
            // Since we are using this instruction to implement JRuby, the class names will be well-defined
            // ex: RubyMatchData.
            //
            // So, if we get an exception, something is seriously wrong.  If we repurpose this IR instruction 
            // for user ruby code, this may no longer be true and we have to appropriately fix this code then.
            throw new RuntimeException(e);
        }
        return context.getRuntime().newBoolean(type.isInstance(object.retrieve(context, self, currDynScope, temp))); 
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InstanceOfInstr(this);
    }
}
