package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

// Represents a splat value in Ruby code: *array
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls that implement splat semantics
public class Splat extends Operand implements DepthCloneable {
    final private Operand array;

    public Splat(Operand array) {
        super(OperandType.SPLAT);
        this.array = array;
    }

    @Override
    public String toString() {
        return "*(unsplat)" + array;
    }

    @Override
    public boolean hasKnownValue() {
        return false; /*_array.isConstant();*/
    }

    public Operand getArray() {
        return array;
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newArray = array.getSimplifiedOperand(valueMap, force);
        return (newArray == array) ? this : new Splat(newArray);
    }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        array.addUsedVariables(l);
    }

    /** When fixing up splats in nested closure we need to tweak the operand if it is a LocalVariable */
    public Operand cloneForDepth(int n) {
        return array instanceof LocalVariable ? new Splat(((LocalVariable) array).cloneForDepth(n)) : this;
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return hasKnownValue() ? this : new Splat(array.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        // Splat is now only used in call arg lists where it is guaranteed that
        // the splat-arg is an array.
        //
        // It is:
        // - either a result of a args-cat/args-push (which generate an array),
        // - or a result of a BuildSplatInstr (which also generates an array),
        // - or a rest-arg that has been received (which also generates an array)
        //   and is being passed via zsuper.
        //
        // In addition, since this only shows up in call args, the array itself is
        // never modified. The array elements are extracted out and inserted into
        // a java array. So, a dup is not required either.
        //
        // So, besides retrieving the array, nothing more to be done here!
        return array.retrieve(context, self, currScope, currDynScope, temp);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArray());
    }

    public static Splat decode(IRReaderDecoder d) {
        return new Splat(d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Splat(this);
    }
}
