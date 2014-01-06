package org.jruby.ir.instructions;

import java.util.Map;

import org.jruby.RubyArray;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReqdArgMultipleAsgnInstr extends MultipleAsgnBase implements FixedArityInstr {
    private final int preArgsCount;    // # of reqd args before rest-arg (-1 if we are fetching a pre-arg)
    private final int postArgsCount;   // # of reqd args after rest-arg  (-1 if we are fetching a pre-arg)

    public ReqdArgMultipleAsgnInstr(Variable result, Operand array, int preArgsCount, int postArgsCount, int index) {
        super(Operation.MASGN_REQD, result, array, index);
        this.preArgsCount = preArgsCount;
        this.postArgsCount = postArgsCount;
    }

    public ReqdArgMultipleAsgnInstr(Variable result, Operand array, int index) {
        this(result, array, -1, -1, index);
    }

    public int getPreArgsCount() { return preArgsCount; }
    public int getPostArgsCount() { return postArgsCount; }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { array, new Fixnum(preArgsCount), new Fixnum(postArgsCount), new Fixnum(index) };
    }

    @Override
    public String toString() {
        return super.toString() + "(" + array + ", " + index + ", " + preArgsCount + ", " + postArgsCount + ")";
    }

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        Operand val = array.getValue(valueMap);
        if (val instanceof Array) {
            Array a = (Array)val;
            int n = a.size();
            int i = Helpers.irReqdArgMultipleAsgnIndex(n, preArgsCount, index, postArgsCount);
            return i == -1 ? scope.getManager().getNil() : a.get(i);
        } else {
            return null;
        }
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), array.cloneForInlining(ii), preArgsCount, postArgsCount, index);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray rubyArray = (RubyArray) array.retrieve(context, self, currDynScope, temp);
        int i = Helpers.irReqdArgMultipleAsgnIndex(rubyArray.getLength(), preArgsCount, index, postArgsCount);
        return i == -1 ? context.nil : rubyArray.entry(i);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReqdArgMultipleAsgnInstr(this);
    }
}
