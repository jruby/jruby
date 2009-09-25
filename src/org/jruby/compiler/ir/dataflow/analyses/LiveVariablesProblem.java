package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;

import java.util.HashMap;

public class LiveVariablesProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public LiveVariablesProblem()              { super(DataFlowProblem.DF_Direction.BACKWARD); }
    public String        getProblemName()      { return "Live Variables Analysis"; }
    public DataFlowVar   getDFVar(Variable v)  { return _dfVarMap.get(v); }
    public void          addDFVar(Variable v)  { _dfVarMap.put(v, new DataFlowVar(this)); }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new LiveVariableNode(this, bb);  }

    /* Individual analyses should override this */
    public String getDataFlowVarsForOutput() 
    {
        StringBuffer buf = new StringBuffer();
        for (Variable v: _dfVarMap.keySet())
            buf.append("DF Var ").append(_dfVarMap.get(v)._id).append(" = ").append(v).append("\n");

        return buf.toString();
    }

    public void markDeadInstructions()
    {
        for (FlowGraphNode n: _fgNodes)
            ((LiveVariableNode)n).markDeadInstructions();
    }

/* ----------- Private Interface ------------ */
    private static HashMap<Variable, DataFlowVar> _dfVarMap = new HashMap<Variable, DataFlowVar>();
}
