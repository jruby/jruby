package org.jruby.ir.instructions;

import org.jcodings.Encoding;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetEncodingInstr extends Instr implements ResultInstr, FixedArityInstr {
    private final Encoding encoding;
    private Variable result;

    public GetEncodingInstr(Variable result, Encoding encoding) {
        super(Operation.GET_ENCODING);

        this.result = result;
        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new StringLiteral(encoding.toString()) };
    }

    @Override
    public String toString() {
        return super.toString() + "(" + encoding + ")";
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetEncodingInstr(ii.getRenamedVariable(result), encoding);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return context.runtime.getEncodingService().getEncoding(encoding);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetEncodingInstr(this);
    }
}
