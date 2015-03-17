package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.InterpretedIRBodyMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ProcessModuleBodyInstr extends ResultBaseInstr implements FixedArityInstr {
    public ProcessModuleBodyInstr(Variable result, Operand moduleBody, Operand block) {
        super(Operation.PROCESS_MODULE_BODY, result, new Operand[] { moduleBody, block });

        assert result != null: "ProcessModuleBodyInstr result is null";
    }

    public Operand getModuleBody() {
        return operands[0];
    }

    public Operand getBlock() {
        return operands[1];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ProcessModuleBodyInstr(ii.getRenamedVariable(result), getModuleBody().cloneForInlining(ii),
                getBlock().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getModuleBody());
        e.encode(getBlock());
    }

    public static ProcessModuleBodyInstr decode(IRReaderDecoder d) {
        return new ProcessModuleBodyInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        InterpretedIRBodyMethod bodyMethod = (InterpretedIRBodyMethod) getModuleBody().retrieve(context, self, currScope, currDynScope, temp);
        Block b = (Block) getBlock().retrieve(context, self, currScope, currDynScope, temp);
		RubyModule implClass = bodyMethod.getImplementationClass();

        return bodyMethod.call(context, implClass, implClass, null, b);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ProcessModuleBodyInstr(this);
    }
}
