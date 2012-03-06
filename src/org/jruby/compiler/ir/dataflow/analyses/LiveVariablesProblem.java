package org.jruby.compiler.ir.dataflow.analyses;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

public class LiveVariablesProblem extends DataFlowProblem {
    public static final String NAME = "Live Variables Analysis";
    private static final Set<LocalVariable> EMPTY_SET = new HashSet<LocalVariable>();
    
    public LiveVariablesProblem(IRScope scope) {
        this(scope, EMPTY_SET);
    }

    LiveVariablesProblem(IRScope scope, Set<LocalVariable> nonSelfLocalVars) {
        super(DataFlowProblem.DF_Direction.BACKWARD);

        setup(scope, nonSelfLocalVars);
    }
    
    public DataFlowVar getDFVar(Variable v) {
        return dfVarMap.get(v);
    }
    
    public boolean dfVarExists(Variable v) {
        return getDFVar(v) != null;
    }

    public Variable getVariable(int id) {
        return varDfVarMap.get(id);
    }

    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) {
        return new LiveVariableNode(this, bb);
    }

    public void addDFVar(Variable v) {
        DataFlowVar dfv = new DataFlowVar(this);
        dfVarMap.put(v, dfv);
        varDfVarMap.put(dfv.id, v);
        if ((v instanceof LocalVariable) && !((LocalVariable) v).isSelf()) {
            // System.out.println("Adding df var for " + v + ":" + dfv.id);
            localVars.add((LocalVariable) v);
        }
    }

    /**
     * Get variables that are live on entry to the cfg.
     * This is the case for closures which access variables from the parent scope.
     *
     *      sum = 0; a.each { |i| sum += i }; return sum
     *
     * In the code snippet above, 'sum' is live on entry to the closure
     */
    public List<Variable> getVarsLiveOnScopeEntry() {
        List<Variable> liveVars = new ArrayList<Variable>();
        BitSet liveIn = ((LiveVariableNode) getFlowGraphNode(getScope().cfg().getEntryBB())).getLiveOutBitSet();

        for (int i = 0; i < liveIn.size(); i++) {
            if (liveIn.get(i) == true) {
                Variable v = getVariable(i);
                liveVars.add(v);
                // System.out.println("variable " + v + " is live on entry!");
            }
        }
        
        return liveVars;
    }

    /**
     * Initialize the problem with all vars from the surrounding scope variables.
     * In closures, vars defined in the closure (or accessed from the surrounding scope)
     * can be used outside the closure. 
     *
     *      sum = 0; a.each { |i| sum += i }; return sum
     *
     * In the code snippet above, 'sum' is live on entry to and exit from the closure.
     **/
    public final void setup(IRScope scope, Collection<LocalVariable> allVars) {
        // System.out.println("\nCFG:\n" + scope.cfg().toStringGraph());
        // System.out.println("\nInstrs:\n" + scope.cfg().toStringInstrs());
        setup(scope);

        for (Variable v : allVars) {
            if (getDFVar(v) == null) addDFVar(v); 
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

    public void setVarsLiveOnScopeExit(Collection<LocalVariable> varsLiveOnScopeExit) {
        this.varsLiveOnScopeExit = varsLiveOnScopeExit;
    }

    public Collection<LocalVariable> getVarsLiveOnScopeExit() {
        return varsLiveOnScopeExit;
    }

    public Set<Variable> getAllVars() {
        return dfVarMap.keySet();
    }

    public Set<LocalVariable> getNonSelfLocalVars() {
        return localVars;
    }
    
    public String getName() {
        return NAME;
    }    

    /* ----------- Private Interface ------------ */
    private HashMap<Variable, DataFlowVar> dfVarMap = new HashMap<Variable, DataFlowVar>();
    private HashMap<Integer, Variable> varDfVarMap = new HashMap<Integer, Variable>();
    private HashSet<LocalVariable> localVars = new HashSet<LocalVariable>(); // Local variables that can be live across dataflow barriers
    private Collection<LocalVariable> varsLiveOnScopeExit;
}
