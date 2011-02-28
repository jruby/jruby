package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowVar;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LiveVariablesProblem extends DataFlowProblem
{
/* ----------- Public Interface ------------ */
    public String getName()                    { return "Live Variables Analysis"; }
    public LiveVariablesProblem()              { super(DataFlowProblem.DF_Direction.BACKWARD); _udVars = new HashSet<Variable>(); }
    public DataFlowVar   getDFVar(Variable v)  { return _dfVarMap.get(v); }
    public Variable      getVariable(int id)   { return _varDfVarMap.get(id); }
    public FlowGraphNode buildFlowGraphNode(BasicBlock bb) { return new LiveVariableNode(this, bb);  }

    private void addDFVar(Variable v, boolean recordVar)  {
        DataFlowVar dfv = new DataFlowVar(this); 
        _dfVarMap.put(v, dfv); 
        _varDfVarMap.put(dfv._id, v);
        if (recordVar)
            _udVars.add(v);
    }

    public void addDFVar(Variable v) { addDFVar(v, true); }

    /**
     * Initialize the exit cfg with variables that are live on exit
     * This is the case for closures where vars defined in the closure (or accessed from the surrounding scope)
     * can be used outside the closure. 
     *
     *      sum = 0; a.each { |i| sum += i }; return sum
     *
     * In the code snippet above, 'sum' is live on exit from the closure.
     **/
    public void initVarsLiveOnExit(Collection<Variable> vars) { _varsLiveOnExit = vars; }

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

		  // Consider this example below
		  //
		  //     def foo
		  //       (1..100).each { |i| v = ... }
		  //       (1..100).each { |i| x = v+i }
		  //       puts x
		  //     end
		  //
		  // In this method 'v' that is defined in the first closure is live on exit from the first closure because
		  // it is used in the second closure.  Nowhere within foo itself (except in closures) is v referenced and
		  // hence won't be added as a dataflow var.
		  //
		  // So, we need to find all variables that are used before being defined in any closure in this method.
		  //
		  // But, this is not sufficient because of evals.  In general, we may have to pick defined vars as well.
		  // Consider this:
		  //
		  //     def foo(str)
		  //       (1..100).each { |i| v = ... }
		  //       (1..100).each { |i| eval(str) }
		  //       puts x
		  //     end
		  //     foo("x=v+i")
		  //
		  // So, in this example, even though 'v' is not used anywhere else, it is still live on exit because of the eval!
		  //
		  // SSS FIXME: For now, we are going to pick all used and defined variables across all closures as a proxy.

        if (c.getScope() instanceof IRMethod) {
            c.setUpUseDefLocalVarMaps();
            for (Variable v: c.usedLocalVarsFromClosures()) {
                addDFVar(v, true);
            }
            for (Variable v: c.definedLocalVarsFromClosures()) {
                addDFVar(v, true);
            }
        }

        // Update setup with info. about variables live on exit.
        if ((_varsLiveOnExit != null) && !_varsLiveOnExit.isEmpty()) {
            for (Variable v: _varsLiveOnExit) {
//                System.out.println("variable " + v + " is live on exit of closure!");
                if (getDFVar(v) == null)
                    addDFVar(v, false); // We aren't recording these vars
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

    public Collection<Variable> getVarsLiveOnExit()
    {
        return _varsLiveOnExit;
    }

    // SSS FIXME: Not used anywhere right now
    public boolean isDefinedOrUsed(Variable v)
    {
        return _udVars.contains(v);
    }

    // SSS FIXME: Not used anywhere right now
    public Set<Variable> allDefinedOrUsedVariables()
    {
        return _udVars;
    }

    public Set<Variable> getAllVars()
    {
        return _dfVarMap.keySet();
    }

/* ----------- Private Interface ------------ */
    private HashMap<Variable, DataFlowVar> _dfVarMap    = new HashMap<Variable, DataFlowVar>();
    private HashMap<Integer, Variable> _varDfVarMap = new HashMap<Integer, Variable>();
    private Collection<Variable> _varsLiveOnExit;
    private Set<Variable> _udVars;
}
