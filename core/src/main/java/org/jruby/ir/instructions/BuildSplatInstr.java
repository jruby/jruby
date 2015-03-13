package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
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

// Represents a splat value in Ruby code: *array
public class BuildSplatInstr extends ResultBaseInstr {
    public BuildSplatInstr(Variable result, Operand array) {
        super(Operation.BUILD_SPLAT, result, new Operand[] { array });
    }

    public Operand getArray() {
        return operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildSplatInstr(ii.getRenamedVariable(result), getArray().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return IRRuntimeHelpers.irSplat(context,
                (IRubyObject) getArray().retrieve(context, self, currScope, currDynScope, temp));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArray());
    }

    public static BuildSplatInstr decode(IRReaderDecoder d) {
        return new BuildSplatInstr(d.decodeVariable(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildSplatInstr(this);
    }
}
