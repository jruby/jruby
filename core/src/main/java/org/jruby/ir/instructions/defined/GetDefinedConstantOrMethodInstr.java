/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions.defined;

import java.util.Map;
import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.FixedArityInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class GetDefinedConstantOrMethodInstr extends DefinedInstr implements FixedArityInstr {
    public GetDefinedConstantOrMethodInstr(Variable result, Operand object, StringLiteral name) {
        super(Operation.DEFINED_CONSTANT_OR_METHOD, result, new Operand[] { object, name });

        assert operands.length >= 2 : "Too few operands to " + getClass().getName();
        assert operands[1] instanceof StringLiteral : "Operand 1 must be a string literal.  Was '" + operands[1].getClass() + "'";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new GetDefinedConstantOrMethodInstr((Variable) getResult().cloneForInlining(inlinerInfo),
                getObject().cloneForInlining(inlinerInfo),
                (StringLiteral) getName().cloneForInlining(inlinerInfo));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject value = (IRubyObject) getObject().retrieve(context, self, currDynScope, temp);
        String name = getName().string;
        RubyString definedType = Helpers.getDefinedConstantOrBoundMethod(value, name);

        return definedType == null ? context.nil : definedType;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetDefinedConstantOrMethodInstr(this);
    }
    public Operand getObject() {
        return operands[0];
    }

    public StringLiteral getName() {
        return (StringLiteral) operands[1];
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        // ENEBO: can variables ever simplify?  (CallBase does it?)
        //result = (Variable) result.getSimplifiedOperand(valueMap, force);

        for (int i = 0; i < operands.length; i++) {
            operands[i] = operands[i].getSimplifiedOperand(valueMap, force);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getObject() + ", " + getName() + ")";
    }
}
