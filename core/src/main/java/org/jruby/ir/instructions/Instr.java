package org.jruby.ir.instructions;

// A generic IR instruction is of the form: v = OP(arg_array, attribute_array)

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
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

    protected Operand[] operands;
    private int ipc; // Interpreter-only: instruction pointer
    private int rpc; // Interpreter-only: rescue pointer
    private final Operation operation;
    // Is this instruction live or dead?  During optimization passes, if this instruction
    // causes no side-effects and the result of the instruction is not needed by anyone else,
    // we can remove this instruction altogether without affecting program correctness.
    private boolean isDead;

    public Instr(Operation operation, Operand[] operands) {
        this.ipc = -1;
        this.rpc = -1;
        this.operation = operation;
        this.operands = operands;
    }

    private String[] EMPTY_STRINGS = new String[0];
    public String[] toStringNonOperandArgs() {
        return EMPTY_STRINGS;
    }

    public void encode(IRWriterEncoder e) {
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Instr(" + getOperation() + "): " + this);
        e.encode(getOperation());
    }

    /**
     * Instructions are meant to be in a machine-readable format so offline tooling can parse the
     * debugging output.  The format is:
     *
     * (result_op '=')? instr '(' (operand ',' )* operand? ';' (extra_arg ',')* extra_arg? ')'
     * extra_arg can either be plain value or in a key: value format.
     * @return
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(isDead() ? "[DEAD]" : "");

        if (this instanceof ResultInstr) buf.append(((ResultInstr) this).getResult()).append(" = ");

        buf.append(operation).append('(');
        toArgList(buf, operands);

        String[] extraArgs = toStringNonOperandArgs();
        if (extraArgs.length >= 1) {
            if (operands.length > 0) buf.append(' ');
            buf.append(';');
            toArgList(buf, extraArgs);
        }
        buf.append(')');

        return buf.toString();
    }

    private StringBuilder toArgList(StringBuilder buf, Object[] args) {
        if (args.length <= 0) return buf;

        for (int i = 0; i < args.length - 1; i++) {
            buf.append(args[i]).append(", ");
        }
        buf.append(args[args.length - 1]);

        return buf;
    }

    @Interp
    public Operation getOperation() {
        return operation;
    }

    @Interp
    public int getIPC() { return ipc; }

    @Interp
    public void setIPC(int ipc) { this.ipc = ipc; }

    @Interp
    public int getRPC() { return rpc; }

    @Interp
    public void setRPC(int rpc) { this.rpc = rpc; }

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

    /**
     * Can this instruction be deleted?  LVA will preserve instructions based on whether operands (variables)
     * are living but even if there are no living variables then the instruction itself may not be able to be removed
     * during DCE for other reasons (like if it unconditionally has a side-effect or it happens to be living in a
     * scope where a binding can escape and one of its operands is a local variable).
     */
    public boolean canBeDeleted(IRScope s) {
         if (hasSideEffects() || operation.isDebugOp() || canRaiseException() || transfersControl()) return false;

         if (this instanceof ResultInstr) {
             Variable r = ((ResultInstr) this).getResult();

             // An escaped binding needs to preserve lvars since that consumers of that binding may access lvars.
             if (s.bindingHasEscaped()) return !(r instanceof LocalVariable);
         }

        return true;
    }

    public void markDead() {
        isDead = true;
    }

    @Interp
    public boolean isDead() {
        return isDead;
    }

    /* Array of all operands for this instruction */
    public Operand[] getOperands() {
        return operands;
    }

    /* List of all variables used by all operands of this instruction */
    public List<Variable> getUsedVariables() {
        ArrayList<Variable> vars = new ArrayList<>();
        for (Operand operand : operands) {
            operand.addUsedVariables(vars);
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
     * @param info This object manages renaming of variables and labels, handles
     *                    args and return values.
     * @return a new instruction that can be used in the target scope.
     */
    public abstract Instr clone(CloneInfo info);

    public Operand[] cloneOperands(CloneInfo info) {
        Operand[] newOperands = new Operand[operands.length];

        for (int i = 0; i < operands.length; i++) {
            newOperands[i] = operands[i].cloneForInlining(info);
        }

        return newOperands;
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
        for (int i = 0; i < operands.length; i++) {
            operands[i] = operands[i].getSimplifiedOperand(valueMap, force);
        }
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
     * @param scope where this instr exists
     * @param valueMap Mapping from operands to their simplified values
     * @return simplified result / output of this instruction
     */
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);

        return null; // By default, no simplifications!
    }

    @Interp
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly interpreted");
    }

    @Interp
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should not be directly interpreted");
    }

    public void visit(IRVisitor visitor) {
        throw new RuntimeException(this.getClass().getSimpleName() + " has no compile logic");
    }
}
