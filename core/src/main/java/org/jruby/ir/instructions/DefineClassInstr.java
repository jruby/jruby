package org.jruby.ir.instructions;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class DefineClassInstr extends Instr implements ResultInstr, FixedArityInstr {
    private final IRClassBody newIRClassBody;
    private Operand container;
    private Operand superClass;
    private Variable result;

    public DefineClassInstr(Variable result, IRClassBody newIRClassBody, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS);

        assert result != null: "DefineClassInstr result is null";

        this.container = container;
        this.superClass = superClass == null ? UndefinedValue.UNDEFINED : superClass;
        this.newIRClassBody = newIRClassBody;
        this.result = result;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{container, superClass};
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
        superClass = superClass.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + newIRClassBody.getName() + ", " + container + ", " + superClass + ", " + newIRClassBody.getFileName() + ")";
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineClassInstr(ii.getRenamedVariable(result), this.newIRClassBody, container.cloneForInlining(ii), superClass.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object container = this.container.retrieve(context, self, currScope, currDynScope, temp);
        Object superClass = this.superClass.retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.newInterpretedClassBody(context, newIRClassBody, container, superClass);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassInstr(this);
    }

    public IRClassBody getNewIRClassBody() {
        return newIRClassBody;
    }

    public Operand getContainer() {
        return container;
    }

    public Operand getSuperClass() {
        return superClass;
    }
}
