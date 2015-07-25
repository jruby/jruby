package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Array;
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

import java.util.Map;

public class ToAryInstr extends ResultBaseInstr implements FixedArityInstr {
    public ToAryInstr(Variable result, Operand array) {
        super(Operation.TO_ARY, result, new Operand[] { array });

        assert result != null: "ToAryInstr result is null";
    }

    public Operand getArray() {
        return operands[0];
    }

    @Override
    public boolean canBeDeleted(IRScope s) {
        // This is an instruction that can be safely deleted
        // since it is inserted by JRuby to facilitate other operations
        // and has no real side effects. Currently, this has been marked
        // as side-effecting in Operation.java. FIXME: Revisit that!
        return true;
    }

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        Operand a = getArray().getValue(valueMap);
        return a instanceof Array ? a : null;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ToAryInstr((Variable) result.cloneForInlining(ii), getArray().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArray());
    }

    public static ToAryInstr decode(IRReaderDecoder d) {
        return new ToAryInstr(d.decodeVariable(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return IRRuntimeHelpers.irToAry(context,
                (IRubyObject) getArray().retrieve(context, self, currScope, currDynScope, temp));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ToAryInstr(this);
    }
}
