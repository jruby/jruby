package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.LoadFromBindingInstr;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

public class BindingLoadPlacementProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public BindingLoadPlacementProblem()
    { 
        super(DataFlowProblem.DF_Direction.BACKWARD);
        _initLoadsOnExit = new java.util.HashSet<Variable>();
        _defVars = new java.util.HashSet<Variable>();
        _usedVars = new java.util.HashSet<Variable>();
        _bindingHasEscaped = false;
    }

    public String        getName() { return "Binding Loads Placement Analysis"; }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new BindingLoadPlacementNode(this, bb);  }
    public String        getDataFlowVarsForOutput() { return ""; }
    public void          initLoadsOnScopeExit(Set<Variable> loads) { _initLoadsOnExit = loads; }
    public Set<Variable> getLoadsOnScopeExit() { return _initLoadsOnExit; }
    public void          recordDefVar(Variable v) { _defVars.add(v); }
    public void          recordUsedVar(Variable v) { _usedVars.add(v); }
    public boolean       bindingHasEscaped() { return _bindingHasEscaped; }
    public void          setBindingHasEscaped(boolean flag) { _bindingHasEscaped = flag; }

    public boolean scopeDefinesVariable(Variable v) { 
        if (_defVars.contains(v)) {
            return true;
        }
        else {
            for (IRClosure cl: getCFG().getScope().getClosures()) {
                BindingLoadPlacementProblem nestedProblem = (BindingLoadPlacementProblem)cl.getCFG().getDataFlowSolution(DataFlowConstants.BLP_NAME);
                if (nestedProblem.scopeDefinesVariable(v)) 
                    return true;
            }

            return false;
        }
    }

    public boolean scopeUsesVariable(Variable v) { 
        if (_usedVars.contains(v)) {
            return true;
        }
        else {
            for (IRClosure cl: getCFG().getScope().getClosures()) {
                BindingLoadPlacementProblem nestedProblem = (BindingLoadPlacementProblem)cl.getCFG().getDataFlowSolution(DataFlowConstants.BLP_NAME);
                if (nestedProblem.scopeUsesVariable(v)) 
                    return true;
            }

            return false;
        }
    }

    public void addLoads()
    {
        for (FlowGraphNode n: _fgNodes) {
            BindingLoadPlacementNode blpn = (BindingLoadPlacementNode)n;
            blpn.addLoads();
        }
    }

/* ----------- Private Interface ------------ */
    private Set<Variable> _initLoadsOnExit;
    private Set<Variable> _defVars;      // Variables defined in this scope
    private Set<Variable> _usedVars;     // Variables used in this scope
    private boolean       _bindingHasEscaped; // Has this method's (or the containing method's binding in the case of a closure) binding escaped?
}
