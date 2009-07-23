package org.jruby.compiler.ir.instructions;

import java.util.List;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;

// NOTE: 'variables' are used only during optimizations -- they don't contribute to
// the list of inputs to the case statement during dataflow analyses.
public class CASE_Instr extends OneOperandInstr {
    List<Label> labels;
    List<Variable> variables;
    Label endLabel;
    Label elseLabel;

    public CASE_Instr(Variable result, Operand arg, Label endLabel) {
        super(Operation.CASE, result, arg);
        this.endLabel = endLabel;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public void setElse(Label elseLabel) {
        this.elseLabel = elseLabel;
    }

    public String toString()
    {
       return "\t" + _result + " = CASE(" + _arg + ", ELSE: " + elseLabel + ")";
    }
}
