package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineModuleInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final IRModuleBody body;

    public DefineModuleInstr(Variable result, Operand container, IRModuleBody body) {
        super(Operation.DEF_MODULE, result, container);

        assert result != null : "DefineModuleInstr result is null";

        this.body = body;
    }


    public IRModuleBody getNewIRModuleBody() {
        return body;
    }

    public Operand getContainer() {
        return getOperand1();
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + body.getId() };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineModuleInstr(ii.getRenamedVariable(result), getContainer().cloneForInlining(ii), body);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getNewIRModuleBody());
    }

    public static DefineModuleInstr decode(IRReaderDecoder d) {
        return new DefineModuleInstr(d.decodeVariable(), d.decodeOperand(), (IRModuleBody) d.decodeScope());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object container = getContainer().retrieve(context, self, currScope, currDynScope, temp);

        IRModuleBody body = this.body;

        RubyModule clazz = IRRuntimeHelpers.newRubyModuleFromIR(context, body.getId(), body.getStaticScope(), container, body.maybeUsingRefinements());


        return Interpreter.INTERPRET_MODULE(context, body, clazz, body.getId());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineModuleInstr(this);
    }
}
