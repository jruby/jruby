package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceOfInstr extends Instr {
    private Class type;
    private String className;
    private Operand object;

    public InstanceOfInstr(Variable dst, Operand object, String className) {
        super(Operation.INSTANCE_OF, dst);
        
        this.object = object;
        this.className = className;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new InstanceOfInstr(ii.getRenamedVariable(getResult()), object.cloneForInlining(ii), className);
    }

    public Operand[] getOperands() {
        return new Operand[]{object};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        object = object.getSimplifiedOperand(valueMap);
    }

    @Override 
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (getResult() + " = ") + getOperation() + "(" + object + ", " + className + ")";
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
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
        getResult().store(interp, context, self, 
                context.getRuntime().newBoolean(type.isInstance(object.retrieve(interp, context, self)))); 
        return null;
    }
}
