package org.jruby.compiler.ir.instructions;

// A generic IR instruction is of the form: v = OP(arg_array, attribute_array)
import org.jruby.compiler.ir.Interp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

//
// Specialized forms:
//   v = OP(arg1, arg2, attribute_array); Ex: v = ADD(v1, v2)
//   v = OP(arg, attribute_array);        Ex: v = NOT(v1)
//
// _attributes store information about the operands of the instruction that have
// been collected as part of analysis.  For more information, see documentation
// in Attribute.java
//
// Ex: v = BOXED_FIXNUM(n)
//     v = HAS_TYPE(Fixnum)
public abstract class Instr {
    public static final Operand[] EMPTY_OPERANDS = new Operand[] {};
    
    private final Operation operation;
    // Is this instruction live or dead?  During optimization passes, if this instruction
    // causes no side-effects and the result of the instruction is not needed by anyone else,
    // we can remove this instruction altogether without affecting program correctness.
    private boolean isDead;
    private boolean hasUnusedResult;

    public Instr(Operation operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "" + (isDead() ? "[DEAD]" : "") + (hasUnusedResult ? "[DEAD-RESULT]" : "") + ((this instanceof ResultInstr) ? ((ResultInstr)this).getResult() + " = " : "") + operation;
    }

    @Interp
    public Operation getOperation() {
        return operation;
    }

    // Does this instruction have side effects as a result of its operation
    // This information is used in optimization phases to impact dead code elimination
    // and other optimization passes
    public boolean hasSideEffects() {
        return operation.hasSideEffects();
    }

    // Can this instruction raise exceptions -- this superclass method has to be conservative and cannot affect program correctness.
    public boolean canRaiseException() { 
		  return operation.canRaiseException();
	 }

    // Can this instruction raise exceptions -- this superclass method has to be conservative and cannot affect program correctness.
    public boolean transfersControl() { 
		  return operation.transfersControl();
	 }

    public boolean canBeDeleted() {
         return !hasSideEffects() && !canRaiseException() && !getOperation().isDebugOp() && !transfersControl();
    }

    public void markDead() {
        isDead = true;
    }

    @Interp
    public boolean isDead() {
        return isDead;
    }

    public void markUnusedResult() {
        hasUnusedResult = true;
    }

    public boolean hasUnusedResult() {
        return hasUnusedResult;
    }

    /* --------- "Abstract"/"please-override" methods --------- */

    /* Array of all operands for this instruction */
    @Interp
    public abstract Operand[] getOperands();

    /* List of all variables used by all operands of this instruction */
    public List<Variable> getUsedVariables() {
        ArrayList<Variable> vars = new ArrayList<Variable>();
        for (Operand o : getOperands()) {
            o.addUsedVariables(vars);
        }

        return vars;
    }

    public void renameVars(Map<Operand, Operand> renameMap) {
        simplifyOperands(renameMap, true);
        if (this instanceof ResultInstr) {
            ResultInstr ri = (ResultInstr)this;
            Variable oldVar = ri.getResult();
            Variable newVar = (Variable)renameMap.get(oldVar);
            if (newVar != null) ri.updateResult(newVar);
        }
    }

    /**
     * Clone the instruction for inlining -- this will rename all variables (including local variables and self!)
     * and replace RECV_ARG and RETURN instructions to regular copy instructions,
     */
    public abstract Instr cloneForInlining(InlinerInfo ii);

    /**
     * This method takes as input a map of operands to their values, and outputs
     *
     * If the value map provides a value for any of the instruction's operands
     * this method is expected to replace the original operands with the simplified values.
     * It is not required that it do so -- code correctness is not compromised by failure
     * to simplify
     */
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
    }

    /**
     * This method takes as input a map of operands to their values, and outputs
     * the result of this instruction.
     *
     * If the value map provides a value for any of the instruction's operands
     * the expectation is that the operand will be replaced with the simplified value.
     * It is not required that it do so -- code correctness is not compromised by failure
     * to simplify.
     *
     * @param valueMap Mapping from operands to their simplified values
     * @returns simplified result / output of this instruction
     */
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);

        return null; // By default, no simplifications!
    }

    @Interp
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly interpreted");
    }
}
