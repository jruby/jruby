package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;

public class AsFixnumInstr extends OneOperandResultBaseInstr {
    public AsFixnumInstr(Variable result, Operand operand) {
        super(Operation.AS_FIXNUM, result, operand);
    }

    @Override
    public Instr clone(CloneInfo info) {
        return new AsFixnumInstr(result, getOperand1());
    }

    public static AsFixnumInstr decode(IRReaderDecoder d) {
        return new AsFixnumInstr(d.decodeVariable(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AsFixnumInstr(this);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return asFixnum(context, (int) getOperand1().retrieve(context, self, currScope, currDynScope, temp));
    }
}
