package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RestArgMultipleAsgnInstr extends MultipleAsgnBase {
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

    @Override
    public String toString() {
        return super.toString() + "(" + array + ", " + index + ", " + preArgsCount + ", " + postArgsCount + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RestArgMultipleAsgnInstr(ii.getRenamedVariable(result), array.cloneForInlining(ii), preArgsCount, postArgsCount, index);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray rubyArray = (RubyArray) array.retrieve(context, self, currDynScope, temp);
        Object val;

        int n = rubyArray.getLength();
        if ((preArgsCount >= n) || (preArgsCount + postArgsCount >= n)) {
            return RubyArray.newEmptyArray(context.runtime);
        } else {
            // FIXME: Perf win to use COW between source Array and this new one (remove toJavaArray)
            return RubyArray.newArrayNoCopy(context.runtime, rubyArray.toJavaArray(), preArgsCount, (n - preArgsCount - postArgsCount));
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RestArgMultipleAsgnInstr(this);
    }
}
