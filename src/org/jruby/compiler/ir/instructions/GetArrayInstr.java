package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyArray;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is an internal ruby array generated during multiple assignment expressions.
// FIXME: is this array subject to monkey-patching of Array methods?
// i.e. if I override the elt accessor method [], will multiple-assignment
// semantics change as well?
//
// FIXME: Rename GetArrayInstr to ArrayArefInstr which would be used
// in later passes as well when compiler passes replace ruby-array []
// calls with inlined lookups
public class GetArrayInstr extends OneOperandInstr {
    public final int index;
    public final boolean all;  // If true, returns the rest of the array starting at the index

    public GetArrayInstr(Variable dest, Operand array, int index, boolean getRestOfArray) {
        super(Operation.GET_ARRAY, dest, array);
        this.index = index;
        all = getRestOfArray;
    }

    @Override
    public String toString() {
        return "" + result + " = " + argument + "[" + index + (all ? ":END" : "") + "] (GET_ARRAY)";
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        Operand val = argument.getValue(valueMap);
        return val.fetchCompileTimeArrayElement(index, all);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetArrayInstr(ii.getRenamedVariable(result), argument.cloneForInlining(ii), index, all);
    }

    @Override
    public Label interpret(InterpreterContext interp) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray array = (RubyArray) getArg().retrieve(interp);
        Object val;
        if (!all) {
            val = array.entry(index);
        }
        else {
            // SSS FIXME: This is inefficient!  Better implementation exists?
            int n = array.getLength();
            int size = n - index;
            if (size < 5) {
                IRubyObject[] rest = new IRubyObject[size];
                for (int i = 0; i < size; i++) {
                    rest[i] = array.entry(index+i);
                }
                val = RubyArray.newArrayNoCopyLight(interp.getRuntime(), rest);
            }
            else {
                val = RubyArray.newArrayNoCopy(interp.getRuntime(), array.toJavaArray(), index);
            }
        }
        getResult().store(interp, val);
        return null;
    }
}
