package org.jruby.compiler.ir.representations;

import java.util.Map;
import java.util.HashMap;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.CallInstruction;

public class InlinerInfo {
    public final CFG callerCFG;
    public final CallInstruction call;

    private Operand[] callArgs;
    private Map<Label, Label> lblRenameMap;
    private Map<Variable, Variable> varRenameMap;

    public InlinerInfo(CallInstruction call, CFG c) {
        this.call = call;
        this.callArgs = call.getCallArgs();
        this.callerCFG = c;
        this.varRenameMap = new HashMap<Variable, Variable>();
        this.lblRenameMap = new HashMap<Label, Label>();
    }

    public Label getRenamedLabel(Label l) {
        Label newLbl = this.lblRenameMap.get(l);
        if (newLbl == null) {
           newLbl = this.callerCFG.getScope().getNewLabel();
           this.lblRenameMap.put(l, newLbl);
        }
        return newLbl;
    }

    public Variable getRenamedVariable(Variable v) {
        Variable newVar = this.varRenameMap.get(v);
        if (newVar == null) {
           newVar = this.callerCFG.getScope().getNewInlineVariable();
           this.varRenameMap.put(v, newVar);
        }
        return newVar;
    }

    public Operand getCallArg(int index) {
        return index < callArgs.length ? callArgs[index] : null;
    }

    public Operand getCallArg(int index, boolean restOfArgArray) {
        if (index >= callArgs.length) {
            return new Array();
        }
        else {
            Operand[] args = new Operand[callArgs.length - index];
            for (int i = index; i < callArgs.length; i++)
                args[i-index] = callArgs[i];

            return new Array(args);
        }
    }

    public Operand getCallReceiver() {
        return call.getReceiver();
    }

    public Operand getCallClosure() {
        return call.getClosureArg();
    }

    public Variable getCallResultVariable() {
        return call._result;
    }
}
