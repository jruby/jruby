package org.jruby.ir.operands;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

// Represents a svalue node in Ruby code
//
// According to headius, svalue evaluates its value node and returns:
//  * nil if it does not evaluate to an array or if it evaluates to an empty array
//  * the first element if it evaluates to a one-element array
//  * the array if it evaluates to a >1 element array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
//
public class SValue extends Operand {
    final private Operand array;

    public SValue(Operand array) {
        this.array = array;
    }

    @Override
    public boolean hasKnownValue() {
        return array.hasKnownValue();
    }

    @Override
    public String toString() {
        return "SValue:" + array;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newArray = array.getSimplifiedOperand(valueMap, force);
        if (newArray instanceof Array) {
            Array a = (Array) newArray;
            return (a.elts.length == 1) ? a.elts[0] : a;
        } else {
            return (newArray == array) ? this : new SValue(newArray);
        }
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        array.addUsedVariables(l);
    }
 
    @Override
    public Operand cloneForInlining(InlinerInfo ii) { 
        return hasKnownValue() ? this : new SValue(array.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        Object val = array.retrieve(context, self, currDynScope, temp);
        
        if (context.getRuntime().is1_9()) {
            return (val instanceof RubyArray) ? val : context.getRuntime().getNil();
        } else {
            if (val instanceof RubyArray) {
                int n = ((RubyArray) val).getLength();
                
                if (n == 0) return context.nil;
                if (n == 1) return ((RubyArray) val).entry(0);
                
                return val;
            }

            return context.nil;
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SValue(this);
    }
}
