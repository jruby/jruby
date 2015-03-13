package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;
import java.util.Map;

public abstract class Operand {
    public static final Operand[] EMPTY_ARRAY = new Operand[0];
    private final OperandType type;

    public Operand(OperandType type) {
        this.type = type;
    }

    public final OperandType getOperandType() {
        return type;
    }

    /**
     * Do we know the value of this operand at compile-time?
     *
     * If we do then it may be possible to constant propagate (one case:
     * We also know it is also an ImmutableLiteral).
     *
     * @return true if a known compile-time value.
     */
    public boolean hasKnownValue() {
        return false;
    }

    /**
     * Can we replace every use of a variable 'v' that contains the value of this operand
     * with the operand itself?  This takes importance when there are at least two uses
     * of 'v' within this scope.
     *
     * Ex: v = [1,2,3];  x = v; y = v
     *
     * In this case, we cannot replace the occurences of 'v' because we would then get
     * x = [1,2,3]; y = [1,2,3] which would then result in two different array objects
     * being constructed instead of a single one.
     *
     * @return true if it is safe to copy-propagate the operand.
     */
    public boolean canCopyPropagate() {
        return false;
    }

    // SSS: HUH? Use better names than this .. The distinction is not very clear!
    //
    // getValue returns the value of this operand, fully simplified
    // getSimplifiedOperand returns the operand in a form that can be materialized into bytecode, if it cannot be completely optimized away
    //
    // The value is used during optimizations and propagated through the IR.  But, it is thrown away after that.
    // But, the operand form is used for constructing the compound objects represented by the operand.
    //
    // Example: a = 1, b = [3,4], c = [a,b], d = [2,c]
    //   -- getValue(c) = [1,[3,4]];     getSimplifiedOperand(c) = [1, b]
    //   -- getValue(d) = [2,[1,[3,4]]]; getSimplifiedOperand(d) = [2, c]
    //
    // Note that b,c,d are all compound objects, and c has a reference to objects a and b, and d has a reference to c.
    // So, if contents of b is modified, the "simplified value"s of c and d also change!  This difference
    // is captured by these two methods.
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        return this;
    }

    public Operand getValue(Map<Operand, Operand> valueMap) {
        return this;
    }

    /** Append the list of variables used in this operand to the input list -- force every operand
     *  to implement this because a missing implementation can cause bad failures.
     */
    public abstract void addUsedVariables(List<Variable> l);

    public abstract Operand cloneForInlining(CloneInfo ii);

    public void encode(IRWriterEncoder e) {
        e.encode(getOperandType().getCoded());
    }

    @Interp
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly retrieved.");
    }

    public void visit(IRVisitor visitor) {
        throw new RuntimeException("operand " + this.getClass().getSimpleName() + " has no visit logic.");
    }
}
