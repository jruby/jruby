package org.jruby.compiler.ir.dataflow;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;
import org.jruby.compiler.ir.representations.BasicBlock;

public abstract class DataFlowProblem
{
/* -------------- Public fields and methods below ---------------- */
    public enum DF_Direction { FORWARD, BACKWARD, BIDIRECTIONAL };

    public final DF_Direction _direction;

    public DataFlowProblem(DF_Direction d)
    {
        _direction = d;
        _dfVars = new ArrayList<DataFlowVar>();
        _nextDFVarId = -1;
    }

// ------- Abstract methods without a default implementation -------
    public abstract FlowGraphNode buildFlowGraphNode(BasicBlock bb);
    public abstract String getProblemName();

// ------- Default implementation methods below -------
    /** Are there are available data flow facts to run this problem? SSS FIXME: Silly optimization? */
    public boolean isEmpty() { return false; }

    public DF_Direction getFlowDirection() { return _direction; }

    /* Compute Meet Over All Paths solution for this dataflow problem on the input CFG.
     * This implements a standard worklist algorithm. */
    public void compute_MOP_Solution(CFG c)
    {
        _cfg = c;

        /** Are there are available data flow facts to run this problem? SSS FIXME: Silly optimization? */
        if (!isEmpty()) {
            LinkedList<FlowGraphNode> workList = buildFlowGraph();
            while (!workList.isEmpty()) {
                workList.removeFirst().computeDataFlowInfo(workList, _bbSet);
            }
        }
    }

    public int getDFVarsCount() { return _dfVars.size(); }

    public Set<CFG_Edge> incomingEdgesOf(BasicBlock bb) { return _cfg.incomingEdgesOf(bb); }

    public Set<CFG_Edge> outgoingEdgesOf(BasicBlock bb) { return _cfg.outgoingEdgesOf(bb); }

    /* Individual analyses should override this */
    public String getDataFlowVarsForOutput() { return ""; }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("----").append(getProblemName()).append("----\n");
  
        buf.append("---- Data Flow Vars: ----\n");
		  buf.append(getDataFlowVarsForOutput());
        buf.append("-------------------------\n");
  
        for (FlowGraphNode n: _fgNodes)
            buf.append("DF State for BB ").append(n._bb.getID()).append(":\n").append(n.toString());

        return buf.toString();
    }

/* -------------- Packaged/protected fields and methods below ---------------- */
    int addDataFlowVar(DataFlowVar v)
    {
        // We want unique ids for dataflow variables
        _nextDFVarId++;
        _dfVars.add(_nextDFVarId, v);
        return _nextDFVarId;
    }

    FlowGraphNode getFlowGraphNode(BasicBlock b)
    {
        return _bbTofgMap.get(b);
    }

/* -------------- Protected fields and methods below ---------------- */
    protected CFG                    _cfg;
    protected List<FlowGraphNode>    _fgNodes;

/* -------------- Private fields and methods below ---------------- */
    private ArrayList<DataFlowVar> _dfVars;
    private BitSet                 _bbSet;
    private int                    _nextDFVarId;
    private Map<BasicBlock, FlowGraphNode> _bbTofgMap;

    private LinkedList<FlowGraphNode> buildFlowGraph()
    {
        LinkedList<FlowGraphNode> workList = new LinkedList<FlowGraphNode>();
        _bbTofgMap = new HashMap<BasicBlock, FlowGraphNode>();
        _bbSet = new BitSet(1+_cfg.getMaxNodeID());

        for (BasicBlock bb: _cfg.getNodes()) {
            FlowGraphNode fgNode = buildFlowGraphNode(bb);
            fgNode.buildDataFlowVars();
            workList.add(fgNode);
            _bbTofgMap.put(bb, fgNode);
            _bbSet.set(bb.getID());
        }

        _fgNodes = (List<FlowGraphNode>)workList.clone();

        // Initialize all flow graph nodes 
        for (FlowGraphNode fg: workList)
           fg.init();

        return workList;
    }
}
