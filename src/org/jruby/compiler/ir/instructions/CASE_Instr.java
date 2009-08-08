package org.jruby.compiler.ir.instructions;

import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;

// SSS NOTE: 'variables' are used only during optimizations -- they don't contribute to
// the list of inputs to the case statement during dataflow analyses.
public class CASE_Instr extends OneOperandInstr {
    Label[] labels;
    Operand[] variables;
    Label endLabel;
    Label elseLabel;

    public CASE_Instr(Variable result, Operand arg, Label endLabel) {
        super(Operation.CASE, result, arg);
        this.endLabel = endLabel;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels.toArray(new Label[labels.size()]);
    }

    public void setVariables(List<Operand> variables) {
        this.variables = variables.toArray(new Operand[variables.size()]);
    }

    public void setElse(Label elseLabel) {
        this.elseLabel = elseLabel;
    }

    public String toString()
    {
       return "\t" + _result + " = CASE(" + _arg + ", ELSE: " + elseLabel + ")";
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap)
    {
        super.simplifyOperands(valueMap);

        // Simplify the variables too -- to keep these variables in sync with what is actually used in the when clauses
        // This is not required for correctness reasons, but only for performance reasons.
        for (int i = 0; i < variables.length; i++) {
            variables[i] = variables[i].getSimplifiedOperand(valueMap);
        }
    }
}
