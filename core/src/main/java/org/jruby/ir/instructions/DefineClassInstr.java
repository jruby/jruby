package org.jruby.ir.instructions;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineClassInstr extends ResultBaseInstr implements FixedArityInstr {
    private final IRClassBody newIRClassBody;

    public DefineClassInstr(Variable result, IRClassBody newIRClassBody, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS, result, new Operand[] { container, superClass == null ? UndefinedValue.UNDEFINED : superClass });

        assert result != null: "DefineClassInstr result is null";

        this.newIRClassBody = newIRClassBody;
    }

    public IRClassBody getNewIRClassBody() {
        return newIRClassBody;
    }

    public Operand getContainer() {
        return operands[0];
    }

    public Operand getSuperClass() {
        return operands[1];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineClassInstr(ii.getRenamedVariable(result), this.newIRClassBody,
                getContainer().cloneForInlining(ii), getSuperClass().cloneForInlining(ii));
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + newIRClassBody.getName() };
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getNewIRClassBody());
        e.encode(getContainer());
        e.encode(getSuperClass());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object container = getContainer().retrieve(context, self, currScope, currDynScope, temp);
        Object superClass = getSuperClass().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.newInterpretedClassBody(context, newIRClassBody, container, superClass);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassInstr(this);
    }
}
