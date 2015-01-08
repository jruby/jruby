package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class ProcessModuleBodyInstr extends Instr implements ResultInstr, FixedArityInstr {
    private Variable result;
    private Operand  moduleBody;
    private Operand block;

    public ProcessModuleBodyInstr(Variable result, Operand moduleBody, Operand block) {
        super(Operation.PROCESS_MODULE_BODY);

        assert result != null: "ProcessModuleBodyInstr result is null";

        this.result = result;
        this.moduleBody = moduleBody;
        this.block = block;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{moduleBody};
    }

    @Override
    public Variable getResult() {
        return result;
    }

    public Operand getBlock() {
        return block;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        moduleBody = moduleBody.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + moduleBody + ", " + block + ")";
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ProcessModuleBodyInstr(ii.getRenamedVariable(result), moduleBody.cloneForInlining(ii), block.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        InterpretedIRMethod bodyMethod = (InterpretedIRMethod)moduleBody.retrieve(context, self, currScope, currDynScope, temp);
        Block b = (Block)block.retrieve(context, self, currScope, currDynScope, temp);
		RubyModule implClass = bodyMethod.getImplementationClass();

        return bodyMethod.call(context, implClass, implClass, null, new IRubyObject[]{}, b);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ProcessModuleBodyInstr(this);
    }

    public Operand getModuleBody() {
        return moduleBody;
    }
}
