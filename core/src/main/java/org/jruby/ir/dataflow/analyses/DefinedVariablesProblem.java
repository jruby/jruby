package org.jruby.ir.dataflow.analyses;

import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class DefinedVariablesProblem extends DataFlowProblem<DefinedVariablesProblem, DefinedVariableNode> {
    public static final String NAME = "Defined Variables Analysis";

    public DefinedVariablesProblem(FullInterpreterContext fic) {
        super(DataFlowProblem.DF_Direction.FORWARD);
        setup(fic);
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
    public DefinedVariableNode buildFlowGraphNode(BasicBlock bb) {
        return new DefinedVariableNode(this, bb);
    }

    public void addDFVar(Variable v) {
        Integer dfv = addDataFlowVar();
        dfVarMap.put(v, dfv);
        varDfVarMap.put(dfv, v);
        vars.add(v);
    }

    public Set<Variable> findUndefinedVars() {
        Set<Variable> undefinedVars = new HashSet<Variable>();
        for (DefinedVariableNode n : flowGraphNodes) {
            n.identifyInits(undefinedVars);
        }
        return undefinedVars;
    }

    public Set<Variable> getAllVars() {
        return dfVarMap.keySet();
    }

    @Override
    public String getName() {
        return NAME;
    }

    /* ----------- Private Interface ------------ */
    private final HashMap<Variable, Integer> dfVarMap = new HashMap<Variable, Integer>();
    private final HashMap<Integer, Variable> varDfVarMap = new HashMap<Integer, Variable>();
    private final HashSet<Variable> vars = new HashSet<Variable>();
}
