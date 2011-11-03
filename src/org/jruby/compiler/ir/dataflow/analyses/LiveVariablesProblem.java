package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jruby.compiler.ir.IRExecutionScope;

public class LiveVariablesProblem extends DataFlowProblem {
    public LiveVariablesProblem() {
        super(DataFlowProblem.DF_Direction.BACKWARD);
        udVars = new HashSet<Variable>();
    }

    public DataFlowVar getDFVar(Variable v) {
        return dfVarMap.get(v);
    }

    public Variable getVariable(int id) {
        return varDfVarMap.get(id);
    }

    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) {
        return new LiveVariableNode(this, bb);
    }

    private void addDFVar(Variable v, boolean recordVar) {
        DataFlowVar dfv = new DataFlowVar(this);
        dfVarMap.put(v, dfv);
        varDfVarMap.put(dfv.id, v);
        if ((v instanceof LocalVariable) && !((LocalVariable) v).isSelf()) {
            localVars.add((LocalVariable) v);
        }
        if (recordVar) udVars.add(v);
    }

    public void addDFVar(Variable v) {
        addDFVar(v, true);
    }

    /**
     * Initialize the exit cfg with variables that are live on exit
     * This is the case for closures where vars defined in the closure (or accessed from the surrounding scope)
     * can be used outside the closure. 
     *
     *      sum = 0; a.each { |i| sum += i }; return sum
     *
     * In the code snippet above, 'sum' is live on exit from the closure.
     **/
    public void initVarsLiveOnExit(Collection<Variable> vars) {
        varsLiveOnExit = vars;
    }

    /**
     * Get variables that are live on entry to the cfg.
     * This is the case for closures which access variables from the parent scope.
     *
     *      sum = 0; a.each { |i| sum += i }; return sum
     *
     * In the code snippet above, 'sum' is live on entry to the closure
     */
    public List<Variable> getVarsLiveOnEntry() {
        List<Variable> liveVars = new ArrayList<Variable>();
        BitSet liveIn = ((LiveVariableNode) getFlowGraphNode(getScope().cfg().getEntryBB())).getLiveOutBitSet();
        
        for (int i = 0; i < liveIn.size(); i++) {
            if (liveIn.get(i) == true) {
                Variable v = getVariable(i);
                liveVars.add(v);
//                System.out.println("variable " + v + " is live on entry!");
            }
        }
        
        return liveVars;
    }

    @Override
    public void setup(IRExecutionScope scope) {
        super.setup(scope);

        // Update setup with info. about variables live on exit.
        if ((varsLiveOnExit != null) && !varsLiveOnExit.isEmpty()) {
            for (Variable v : varsLiveOnExit) {
//                System.out.println("variable " + v + " is live on exit of closure!");
                // We aren't recording these vars
                if (getDFVar(v) == null) addDFVar(v, false); 
            }
        }
    }

    @Override
    public String getDataFlowVarsForOutput() {
        StringBuilder buf = new StringBuilder();
        for (Variable v : dfVarMap.keySet()) {
            buf.append("DF Var ").append(dfVarMap.get(v).getId()).append(" = ").append(v).append("\n");
        }

        return buf.toString();
    }

    public void markDeadInstructions() {
        for (FlowGraphNode n : flowGraphNodes) {
            ((LiveVariableNode) n).markDeadInstructions();
        }
    }

    public Collection<Variable> getVarsLiveOnExit() {
        return varsLiveOnExit;
    }

    // SSS FIXME: Not used anywhere right now
    public boolean isDefinedOrUsed(Variable v) {
        return udVars.contains(v);
    }

    // SSS FIXME: Not used anywhere right now
    public Set<Variable> allDefinedOrUsedVariables() {
        return udVars;
    }

    public Set<Variable> getAllVars() {
        return dfVarMap.keySet();
    }

    public Set<LocalVariable> getNonSelfLocalVars() {
        return localVars;
    }
    
    public String getName() {
        return "Live Variables Analysis";
    }    

    /* ----------- Private Interface ------------ */
    private HashMap<Variable, DataFlowVar> dfVarMap = new HashMap<Variable, DataFlowVar>();
    private HashMap<Integer, Variable> varDfVarMap = new HashMap<Integer, Variable>();
    private HashSet<LocalVariable> localVars = new HashSet<LocalVariable>(); // Local variables that can be live across dataflow barriers
    private Collection<Variable> varsLiveOnExit;
    private Set<Variable> udVars;
}
