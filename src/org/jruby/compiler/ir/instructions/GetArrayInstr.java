package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyArray;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is an internal ruby array generated during multiple assignment expressions.
// FIXME: is this array subject to monkey-patching of Array methods?
// i.e. if I override the elt accessor method [], will multiple-assignment
// semantics change as well?
//
// FIXME: Rename GetArrayInstr to ArrayArefInstr which would be used
// in later passes as well when compiler passes replace ruby-array []
// getArraySlices with inlined lookups
public class GetArrayInstr extends Instr implements ResultInstr {
    private Operand array;
    private final int preArgsCount;       // # of reqd args before rest-arg (-1 if we are fetching a pre-arg)
    private final int postArgsCount;      // # of reqd args after rest-arg  (-1 if we are fetching a pre-arg)
    private final int index;              // index within pre/post arg slice
    private final boolean getArraySlice;  // If true, returns an array slice between indexFromStart and indexFromEnd (rest of the array if indexFromEnd is -1)
    private Variable result;

    public GetArrayInstr(Variable result, Operand array, int preArgsCount, int postArgsCount, int index, boolean getRestOfArray) {
        super(Operation.GET_ARRAY);
        
        assert result != null : "GetArrayInstr result is null";
        
        this.result = result;
        this.array = array;
        this.preArgsCount = preArgsCount;
        this.postArgsCount = postArgsCount;
        this.index = index;
        this.getArraySlice = getRestOfArray;
    }

    public GetArrayInstr(Variable result, Operand array, int index, boolean getRestOfArray) {
        this(result, array, -1, -1, index, getRestOfArray);
    }

    public Operand[] getOperands() {
        return new Operand[]{array};
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        array = array.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return result + " = " + array + "[" + preArgsCount + "," + postArgsCount + ", " + index + ", " + getArraySlice + "] (GET_ARRAY)";
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        return super.simplifyAndGetResult(valueMap);
        // SSS FIXME!  This is buggy code for 1.9 mode
/*
        simplifyOperands(valueMap, false);
        Operand val = array.getValue(valueMap);
        return val.fetchCompileTimeArrayElement(index, getArraySlice);
*/
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetArrayInstr(ii.getRenamedVariable(result), array.cloneForInlining(ii), preArgsCount, postArgsCount, index, getArraySlice);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray rubyArray = (RubyArray) array.retrieve(context, self, currDynScope, temp);
        Object val;
        
        int n = rubyArray.getLength();
        if (!getArraySlice) {
            if (preArgsCount == -1) {
                return rubyArray.entry(index);
            } else {
                int remaining = n - preArgsCount;
                if (remaining <= index) {
                    return context.nil;
                } else {
                    return (remaining > postArgsCount) ? rubyArray.entry(n - postArgsCount + index) : rubyArray.entry(preArgsCount + index);
                }
            }
        } else {
            if ((preArgsCount >= n) || (preArgsCount + postArgsCount >= n)) {
                return RubyArray.newEmptyArray(context.getRuntime());
            } else {
                // FIXME: Perf win to use COW between source Array and this new one (remove toJavaArray)
                return RubyArray.newArrayNoCopy(context.getRuntime(), rubyArray.toJavaArray(), preArgsCount, (n - preArgsCount - postArgsCount));
            }
        }
    }
}
