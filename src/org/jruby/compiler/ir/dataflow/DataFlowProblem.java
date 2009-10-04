package org.jruby.compiler.ir.dataflow;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Map;
import java.util.Set;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.Tuple;
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
            // 1. Build Flow Graph
            buildFlowGraph();

            // 2. Initialize work list based on flow direction to make processing efficient!
            Tuple<LinkedList<FlowGraphNode>, BitSet> t = getInitialWorkList();
            LinkedList<FlowGraphNode> workList = t._a;
            BitSet bbSet = t._b;

            // 3. Iteratively compute data flow info
            while (!workList.isEmpty()) {
                workList.removeFirst().computeDataFlowInfo(workList, bbSet);
            }
        }
    }

    private Tuple<LinkedList<FlowGraphNode>, BitSet> getInitialWorkList()
    {
        LinkedList<FlowGraphNode> wl = new LinkedList<FlowGraphNode>();
        Stack<BasicBlock> stack = new Stack<BasicBlock>();
        BasicBlock root  = _cfg.getRoot();
        stack.push(root);
        BitSet bbSet = new BitSet(1+_cfg.getMaxNodeID());
        bbSet.set(root.getID());

        // Non-recursive post-order traversal (the added flag is required to handle cycles and common ancestors)
        while (!stack.empty()) {
            // Check if all children of the top of the stack have been added
            BasicBlock b = stack.peek();
            boolean allChildrenDone = true;
            for (CFG_Edge e: outgoingEdgesOf(b)) {
                BasicBlock dst = e._dst;
                int dstID = dst.getID();
                if (!bbSet.get(dstID)) {
                    allChildrenDone = false;
                    stack.push(dst);
                    bbSet.set(dstID);
                }
            }

            // If all children have been added previously, we are ready with 'b' in this round!
            if (allChildrenDone) {
                stack.pop();
                wl.add(_bbTofgMap.get(b.getID()));
//                System.out.println("Added: " + b.getID()); 
            }
        }

        if (_direction == DF_Direction.FORWARD) 
            java.util.Collections.reverse(wl);

        return new Tuple<LinkedList<FlowGraphNode>, BitSet>(wl, bbSet);
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
        return _bbTofgMap.get(b.getID());
    }

/* -------------- Protected fields and methods below ---------------- */
    protected CFG                    _cfg;
    protected List<FlowGraphNode>    _fgNodes;

/* -------------- Private fields and methods below ---------------- */
    private int     _nextDFVarId;
    private ArrayList<DataFlowVar> _dfVars;
    private Map<Integer, FlowGraphNode> _bbTofgMap;

    private void buildFlowGraph()
    {
        _fgNodes = new LinkedList<FlowGraphNode>();
        _bbTofgMap = new HashMap<Integer, FlowGraphNode>();

        for (BasicBlock bb: _cfg.getNodes()) {
            FlowGraphNode fgNode = buildFlowGraphNode(bb);
            fgNode.buildDataFlowVars();
            _fgNodes.add(fgNode);
            _bbTofgMap.put(bb.getID(), fgNode);
        }

        // Initialize all flow graph nodes 
        for (FlowGraphNode fg: _fgNodes)
            fg.init();
    }
}
