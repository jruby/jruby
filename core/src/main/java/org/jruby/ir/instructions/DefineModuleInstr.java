package org.jruby.ir.instructions;

import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineModuleInstr extends ResultBaseInstr implements FixedArityInstr {
    private final IRModuleBody newIRModuleBody;

    public DefineModuleInstr(Variable result, IRModuleBody newIRModuleBody, Operand container) {
        super(Operation.DEF_MODULE, result, new Operand[] { container });

        assert result != null : "DefineModuleInstr result is null";

        this.newIRModuleBody = newIRModuleBody;
    }


    public IRModuleBody getNewIRModuleBody() {
        return newIRModuleBody;
    }

    public Operand getContainer() {
        return operands[0];
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + newIRModuleBody.getName() };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineModuleInstr(ii.getRenamedVariable(result), this.newIRModuleBody, getContainer().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getNewIRModuleBody());
        e.encode(getContainer());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object rubyContainer = getContainer().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.newInterpretedModuleBody(context, newIRModuleBody, rubyContainer);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineModuleInstr(this);
    }
}
