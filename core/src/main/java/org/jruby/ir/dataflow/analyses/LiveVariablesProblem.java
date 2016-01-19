package org.jruby.ir.dataflow.analyses;

import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class LiveVariablesProblem extends DataFlowProblem<LiveVariablesProblem, LiveVariableNode> {
    public static final String NAME = "Live Variables Analysis";

    public LiveVariablesProblem(IRScope scope) {
        super(DataFlowProblem.DF_Direction.BACKWARD);
        alwaysLiveVars = new ArrayList<LocalVariable>();
        varsLiveOnScopeExit = new ArrayList<LocalVariable>(alwaysLiveVars);
        setup(scope);
    }

    public Integer getDFVar(Variable v) {
        return dfVarMap.get(v);
    }

    public boolean dfVarExists(Variable v) {
        return getDFVar(v) != null;
    }

    public Variable getVariable(int id) {
        return varDfVarMap.get(id);
    }

    @Override
    public LiveVariableNode buildFlowGraphNode(BasicBlock bb) {
        return new LiveVariableNode(this, bb);
    }

    public void addDFVar(Variable v) {
        Integer dfv = addDataFlowVar();
        dfVarMap.put(v, dfv);
        varDfVarMap.put(dfv, v);

        if (v instanceof LocalVariable && !v.isSelf()) {
            //System.out.println("Adding df var for " + v + ":" + dfv.id);
            IRScope s = getScope();
            for (int n = ((LocalVariable) v).getScopeDepth(); s != null && n >= 0; n--) {
                if (s instanceof IREvalScript) {
                    // If a variable is at the topmost scope of the eval OR crosses an eval boundary,
                    // it is going to be marked always live since it could be used by other evals (n = 0)
                    // or by enclosing scopes (n > 0)
                    alwaysLiveVars.add((LocalVariable) v);
                    break;
                }

                s = s.getLexicalParent();
            }
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
    public Collection<LocalVariable> getLocalVarsLiveOnScopeEntry() {
        List<LocalVariable> liveVars = new ArrayList<LocalVariable>();
        BitSet liveIn = getFlowGraphNode(getScope().getCFG().getEntryBB()).getLiveOutBitSet();

        for (int i = 0; i < liveIn.size(); i++) {
            if (!liveIn.get(i)) continue;

            Variable v = getVariable(i);
            if (v instanceof LocalVariable) {
                liveVars.add((LocalVariable)v);
            }
            // System.out.println("variable " + v + " is live on entry!");
        }

        return liveVars;
    }

    @Override
    public String getDataFlowVarsForOutput() {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<Variable, Integer> entry : dfVarMap.entrySet()) {
            buf.append("DF Var ").append(entry.getValue()).append(" = ").append(entry.getKey()).append('\n');
        }

        return buf.toString();
    }

    public void markDeadInstructions() {
        for (LiveVariableNode n : flowGraphNodes) {
            n.markDeadInstructions();
        }
    }

    public void setVarsLiveOnScopeExit(Collection<LocalVariable> vars) {
        varsLiveOnScopeExit.addAll(vars);
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

    @Override
    public String getName() {
        return NAME;
    }

    /* ----------- Private Interface ------------ */
    private HashMap<Variable, Integer> dfVarMap = new HashMap<Variable, Integer>();
    private HashMap<Integer, Variable> varDfVarMap = new HashMap<Integer, Variable>();
    private HashSet<LocalVariable> localVars = new HashSet<LocalVariable>(); // Local variables that can be live across dataflow barriers

    // Variables that cross eval boundaries and are always live in this scope
    private List<LocalVariable> alwaysLiveVars;
    private Collection<LocalVariable> varsLiveOnScopeExit;
}
