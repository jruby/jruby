package org.jruby.compiler.ir;

import java.util.List;

// NOTE: 'variables' are used only during optimizations -- they don't contribute to
// the list of inputs to the case statement during dataflow analyses.
public class CASE_Instr extends OneOperandInstr {
    List<Label> labels;
    List<Variable> variables;
    Label endLabel;
    Label elseLbl;

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

    public void setElse(Label elseLbl) {
        this.elseLbl = elseLbl;
    }
}
