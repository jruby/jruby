package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class LiveVariablesProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public LiveVariablesProblem()              { super(DataFlowProblem.DF_Direction.BACKWARD); }
    public String        getProblemName()      { return "Live Variables Analysis"; }
    public void          addDFVar(Variable v)  { DataFlowVar dfv = new DataFlowVar(this); _dfVarMap.put(v, dfv); _varDfVarMap.put(dfv._id, v); }
    public DataFlowVar   getDFVar(Variable v)  { return _dfVarMap.get(v); }
    public Variable      getVariable(int id){ return _varDfVarMap.get(id); }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new LiveVariableNode(this, bb);  }

    /**
     * Initialize the exit cfg with variables that are live on exit
     * This is the case for closures where vars defined in the closure (or accessed from the surrounding scope)
     * can be used outside the closure. 
     *
     *      sum = 0; a.each { |i| sum += i }; return sum
     *
     * In the code snippet above, 'sum' is live on exit from the closure.
     **/
    public void initVarsLiveOnExit(List<Variable> vars) { _varsLiveOnExit = vars; }

    /**
     * Get variables that are live on entry to the cfg.
     * This is the case for closures which access variables from the parent scope.
     *
     *      sum = 0; a.each { |i| sum += i }; return sum
     *
     * In the code snippet above, 'sum' is live on entry to the closure
     */
    public List<Variable> getVarsLiveOnEntry()
    {
        List<Variable> liveVars = new ArrayList<Variable>();
        BitSet liveIn = ((LiveVariableNode)getFlowGraphNode(_cfg.getEntryBB())).getLiveInBitSet();
        for (int i = 0; i < liveIn.size(); i++) {
            if (liveIn.get(i) == true) {
                Variable v = getVariable(i);
                liveVars.add(v);
//                System.out.println("variable " + v + " is live on entry!");
            }
        }
        return liveVars;
    }

    public void setup(CFG c)
    {
        super.setup(c);

        // Update setup with info. about variables live on exit.
        BasicBlock b = c.getExitBB();
        if ((_varsLiveOnExit != null) && !_varsLiveOnExit.isEmpty()) {
            LiveVariableNode lvn = (LiveVariableNode)getFlowGraphNode(b);
            for (Variable v: _varsLiveOnExit) {
//                System.out.println("variable " + v + " is live on exit of closure!");
                if (getDFVar(v) == null)
                    addDFVar(v);
            }
        }
    }

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
    private HashMap<Variable, DataFlowVar> _dfVarMap    = new HashMap<Variable, DataFlowVar>();
    private HashMap<Integer, Variable> _varDfVarMap = new HashMap<Integer, Variable>();
    private List<Variable> _varsLiveOnExit;
}
