package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class DefineModuleInstr extends Instr implements ResultInstr, FixedArityInstr {
    private final IRModuleBody newIRModuleBody;
    private Operand container;
    private Variable result;

    public DefineModuleInstr(Variable result, IRModuleBody newIRModuleBody, Operand container) {
        super(Operation.DEF_MODULE);

        assert result != null : "DefineModuleInstr result is null";

        this.newIRModuleBody = newIRModuleBody;
        this.container = container;
        this.result = result;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{container};
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
    }

    @Override
    public String toString() {
        return super.toString() + "(" + newIRModuleBody.getName() + ", " + container + ", " + newIRModuleBody.getFileName() + ")";
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineModuleInstr(ii.getRenamedVariable(result), this.newIRModuleBody, container.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object rubyContainer = container.retrieve(context, self, currScope, currDynScope, temp);

        if (!(rubyContainer instanceof RubyModule)) {
            throw context.runtime.newTypeError("no outer class/module");
        }

        RubyModule newRubyModule = ((RubyModule) rubyContainer).defineOrGetModuleUnder(newIRModuleBody.getName());
        newIRModuleBody.getStaticScope().setModule(newRubyModule);
        return new InterpretedIRMethod(newIRModuleBody, Visibility.PUBLIC, newRubyModule);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineModuleInstr(this);
    }

    public IRModuleBody getNewIRModuleBody() {
        return newIRModuleBody;
    }

    public Operand getContainer() {
        return container;
    }
}
