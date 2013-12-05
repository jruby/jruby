/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions.defined;

import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class GetDefinedConstantOrMethodInstr extends DefinedObjectNameInstr {
    public GetDefinedConstantOrMethodInstr(Variable result, Operand object, StringLiteral name) {
        super(Operation.DEFINED_CONSTANT_OR_METHOD, result, new Operand[] { object, name });
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new GetDefinedConstantOrMethodInstr((Variable) getResult().cloneForInlining(inlinerInfo),
                getObject().cloneForInlining(inlinerInfo),
                (StringLiteral) getName().cloneForInlining(inlinerInfo));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject value = (IRubyObject) getObject().retrieve(context, self, currDynScope, temp);
        String name = getName().string;
        RubyString definedType = Helpers.getDefinedConstantOrBoundMethod(value, name);

        return definedType == null ? context.nil : new StringLiteral(definedType.getByteList()).retrieve(context, self, currDynScope, temp);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetDefinedConstantOrMethodInstr(this);
    }
}
