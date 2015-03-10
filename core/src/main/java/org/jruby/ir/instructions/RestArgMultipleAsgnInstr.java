package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RestArgMultipleAsgnInstr extends MultipleAsgnBase implements FixedArityInstr {
    private final int preArgsCount;       // # of reqd args before rest-arg
    private final int postArgsCount;      // # of reqd args after rest-arg

    public RestArgMultipleAsgnInstr(Variable result, Operand array, int preArgsCount, int postArgsCount, int index) {
        super(Operation.MASGN_REST, result, array, index);
        this.preArgsCount = preArgsCount;
        this.postArgsCount = postArgsCount;
    }

    public RestArgMultipleAsgnInstr(Variable result, Operand array, int index) {
        this(result, array, -1, -1, index);
    }

    public int getPreArgsCount() {
        return preArgsCount;
    }

    public int getPostArgsCount() {
        return postArgsCount;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "index: " + index, "pre: " + preArgsCount, "post: " + postArgsCount};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new RestArgMultipleAsgnInstr(ii.getRenamedVariable(result), getArray().cloneForInlining(ii), preArgsCount, postArgsCount, index);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArray());
        e.encode(getPreArgsCount());
        e.encode(getPostArgsCount());
        e.encode(getIndex());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray rubyArray = (RubyArray) getArray().retrieve(context, self, currScope, currDynScope, temp);

        return Helpers.viewArgsArray(context, rubyArray, preArgsCount, postArgsCount);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RestArgMultipleAsgnInstr(this);
    }
}
