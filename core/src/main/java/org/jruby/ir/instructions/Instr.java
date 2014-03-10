package org.jruby.ir.instructions;

// A generic IR instruction is of the form: v = OP(arg_array, attribute_array)

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private int ipc;
    private final Operation operation;
    // Is this instruction live or dead?  During optimization passes, if this instruction
    // causes no side-effects and the result of the instruction is not needed by anyone else,
    // we can remove this instruction altogether without affecting program correctness.
    private boolean isDead;
    private boolean hasUnusedResult;

    public Instr(Operation operation) {
        this.ipc = -1;
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

    public int getIPC() { return ipc; }

    public void setIPC(int ipc) { this.ipc = ipc; }

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

    /**
     * Does this instruction do anything the scope is interested in?
     * @return true if it modified the scope.
     */
    public boolean computeScopeFlags(IRScope scope) {
        return false;
    }

    public boolean canBeDeleted(IRScope s) {
         if (hasSideEffects() || getOperation().isDebugOp() || getOperation().canRaiseException() || transfersControl()) {
             return false;
         } else if (this instanceof ResultInstr) {
             Variable r = ((ResultInstr)this).getResult();
             if (s.bindingHasEscaped() && !r.getName().equals(Variable.BLOCK)) {
                 // If the binding of this scope has escaped, then we have to preserve writes to
                 // all local variables because anyone who uses the binding might query any of the
                 // local variables from the binding.  This is safe, but extremely conservative.
                 return !(r instanceof LocalVariable);
             } else if (s.usesEval() && r.getName().equals(Variable.BLOCK)) {
                 // If this scope (or any nested scope) has any evals, then the eval might have a yield which
                 // would use %block.  In that scenario, we cannot delete the '%block = recv_closure' instruction.
                 // This is safe, but conservative.
                 return false;
             } else if (s.usesZSuper() && getOperation().isArgReceive()) {
                 // If this scope (or any nested scope) has a ZSuperInstr, then the arguments of this
                 // scope could be used by any of those ZSuper instructions.  If so, we cannot delete
                 // the argument receive.
                 // SSS FIXME: This check may be redundant. ZSuper now explicits lists
                 // all arguments that it may use.
                 return false;
             } else {
                 return true;
             }
         } else {
             return true;
         }
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
     * Clone the instruction for use in an inlining context (either when a scope is inlined into
     * another scope, or when a block has to be cloned because its associated call belongs to
     * an inlined scope). This might renaming variables and labels to eliminate naming conflicts.
     *
     * The implementation might vary on the cloning mode.
     *
     * @param inlinerInfo This object manages renaming of variables and labels, handles
     *                    args and return values.
     * @return a new instruction that can be used in the target scope.
     */
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        throw new RuntimeException("cloneForInlining: Not implemented for: " + this.getOperation());
    }

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
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);

        return null; // By default, no simplifications!
    }

    @Interp
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly interpreted");
    }

    @Interp
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, int ipc) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly interpreted");
    }

    public void visit(IRVisitor visitor) {
        throw new RuntimeException(this.getClass().getSimpleName() + " has no compile logic");
    }
}
