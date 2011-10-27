package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// SSS FIXME: Have to find a clean solution to this hackiness of having
// one or more non-Operand fields  in instructions or maybe rename the
// base classes to something more appropriate?
public class InstanceOfInstr extends OneOperandInstr {
    private Class type;
    private String className;

    public InstanceOfInstr(Variable dst, Operand v, String className) {
        super(Operation.INSTANCE_OF, dst, v);
        
        this.className = className;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new InstanceOfInstr(ii.getRenamedVariable(getResult()), getArg().cloneForInlining(ii), className);
    }

    @Override 
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (getResult() + " = ") + getOperation() + "(" + getArg() + ", " + className + ")";
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
                context.getRuntime().newBoolean(type.isInstance(getArg().retrieve(interp, context, self)))); 
        return null;
    }
}
