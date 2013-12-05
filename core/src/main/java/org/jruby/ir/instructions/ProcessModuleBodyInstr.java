package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class ProcessModuleBodyInstr extends Instr implements ResultInstr {
    private Operand  moduleBody;
    private Variable result;

    public ProcessModuleBodyInstr(Variable result, Operand moduleBody) {
        super(Operation.PROCESS_MODULE_BODY);

        assert result != null: "ProcessModuleBodyInstr result is null";

        this.result = result;
		  this.moduleBody = moduleBody;
    }

    public Operand[] getOperands() {
        return new Operand[]{moduleBody};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        moduleBody = moduleBody.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + moduleBody + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ProcessModuleBodyInstr(ii.getRenamedVariable(result), moduleBody.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        InterpretedIRMethod bodyMethod = (InterpretedIRMethod)moduleBody.retrieve(context, self, currDynScope, temp);
		  RubyModule implClass = bodyMethod.getImplementationClass();
        // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to ProcessModuleBody, DefineModule instrs?
        return bodyMethod.call(context, implClass, implClass, "", new IRubyObject[]{}, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ProcessModuleBodyInstr(this);
    }

    public Operand getModuleBody() {
        return moduleBody;
    }
}
