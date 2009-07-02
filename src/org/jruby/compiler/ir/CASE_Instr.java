package org.jruby.compiler.ir;

import java.util.List;

public class CASE_Instr extends OneOperandInstr {
    List<Label> labels;
    List<Variable> variables;
    Label endLabel;
    Label elseLbl;

    public CASE_Instr(Operand arg, Label endLabel) {
        super(Operation.CASE, null, arg);
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
