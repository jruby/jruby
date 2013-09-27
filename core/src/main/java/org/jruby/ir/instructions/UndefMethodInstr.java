package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class UndefMethodInstr extends Instr implements ResultInstr {
    private Variable result;
    private Operand methodName;

    public UndefMethodInstr(Variable result, Operand methodName) {
        super(Operation.UNDEF_METHOD);

        this.result = result;
        this.methodName = methodName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { methodName };
    }

    @Override
    public String toString() {
        return super.toString() + "(" + methodName + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        methodName = methodName.getSimplifiedOperand(valueMap, force);
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new UndefMethodInstr((Variable) result.cloneForInlining(ii),
                methodName.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return Helpers.undefMethod(context, methodName.retrieve(context, self, currDynScope, temp));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UndefMethodInstr(this);
    }
}
