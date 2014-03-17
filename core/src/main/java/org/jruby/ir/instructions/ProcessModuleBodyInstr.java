package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.RubyNil;
import org.jruby.RubyProc;
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
        return new Operand[]{moduleBody, block};
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
        moduleBody = moduleBody.getSimplifiedOperand(valueMap, force);
        block = block.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + moduleBody + "," + block + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ProcessModuleBodyInstr(ii.getRenamedVariable(result), moduleBody.cloneForInlining(ii), block.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        InterpretedIRMethod bodyMethod = (InterpretedIRMethod)moduleBody.retrieve(context, self, currDynScope, temp);
		RubyModule implClass = bodyMethod.getImplementationClass();
        Object blk = block.retrieve(context, self, currDynScope, temp);
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        return bodyMethod.call(context, implClass, implClass, "", new IRubyObject[]{}, (Block)blk);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ProcessModuleBodyInstr(this);
    }

    public Operand getModuleBody() {
        return moduleBody;
    }

    public Operand getBlockArg() {
        return block;
    }
}
