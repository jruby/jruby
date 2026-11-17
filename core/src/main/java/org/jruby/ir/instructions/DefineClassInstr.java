package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineClassInstr extends TwoOperandResultBaseInstr implements FixedArityInstr {
    private final IRClassBody body;

    public DefineClassInstr(Variable result, IRClassBody body, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS, result, container, superClass == null ? UndefinedValue.UNDEFINED : superClass);

        assert result != null: "DefineClassInstr result is null";

        this.body = body;
    }

    public IRClassBody getNewIRClassBody() {
        return body;
    }

    public Operand getContainer() {
        return getOperand1();
    }

    public Operand getSuperClass() {
        return getOperand2();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineClassInstr(ii.getRenamedVariable(result), body,
                getContainer().cloneForInlining(ii), getSuperClass().cloneForInlining(ii));
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + body.getId() };
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getNewIRClassBody());
        e.encode(getContainer());
        e.encode(getSuperClass());
    }

    public static DefineClassInstr decode(IRReaderDecoder d) {
        return new DefineClassInstr((d.decodeVariable()), (IRClassBody) d.decodeScope(), d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object container = getContainer().retrieve(context, self, currScope, currDynScope, temp);
        Object superClass = getSuperClass().retrieve(context, self, currScope, currDynScope, temp);

        RubyModule clazz = IRRuntimeHelpers.newRubyClassFromIR(context, body.getId(), body.getStaticScope(),
                superClass, container, body.maybeUsingRefinements());

        return Interpreter.INTERPRET_CLASS(context, body, clazz, body.getId());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassInstr(this);
    }
}
