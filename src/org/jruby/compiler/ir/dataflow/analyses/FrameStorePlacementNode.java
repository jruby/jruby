package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.ALLOC_FRAME_Instr;
import org.jruby.compiler.ir.instructions.STORE_TO_FRAME_Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;

import java.util.Set;
import java.util.HashSet;
import java.util.ListIterator;

public class FrameStorePlacementNode extends FlowGraphNode
{
/* ---------- Public fields, methods --------- */
    public FrameStorePlacementNode(DataFlowProblem prob, BasicBlock n) { super(prob, n); }

    public void init()
    {
        _inDirtyVars  = new HashSet<Variable>();
        _outDirtyVars = new HashSet<Variable>();
    }

    public void buildDataFlowVars(IR_Instr i) 
    { 
        FrameStorePlacementProblem fsp = (FrameStorePlacementProblem)_prob;
        for (Variable v: i.getUsedVariables())
            fsp.recordUseDefVar(v);

        Variable v = i.getResult();
        if (v != null)
            fsp.recordUseDefVar(v);
    }

    public void initSolnForNode() 
    {
        if (_bb == _prob.getCFG().getEntryBB())
            _inDirtyVars = ((FrameStorePlacementProblem)_prob).getNestedProblemInitStores();
    }

    public void compute_MEET(CFG_Edge edge, FlowGraphNode pred)
    {
        // Intersection of predecessor store sets
        _inDirtyVars.retainAll(((FrameStorePlacementNode)pred)._outDirtyVars);
    }

    public boolean applyTransferFunction()
    {
        FrameStorePlacementProblem fsp = (FrameStorePlacementProblem)_prob;
        Set<Variable> dirtyVars = new HashSet<Variable>(_inDirtyVars);

        for (IR_Instr i: _bb.getInstrs()) {
            // Process calls specially -- these are the sites of frame stores!
            if (i instanceof CALL_Instr) {
                CALL_Instr call = (CALL_Instr)i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    IR_Closure cl = (IR_Closure)((MetaObject)o)._scope;
                    CFG cl_cfg = cl.getCFG();
                    FrameStorePlacementProblem cl_fsp = new FrameStorePlacementProblem();
                    cl_fsp.initNestedProblem(dirtyVars);
                    cl_fsp.setup(cl_cfg);
                    cl_fsp.compute_MOP_Solution();
                    cl_cfg.setDataFlowSolution(cl_fsp.getName(), cl_fsp);
                    if (call.usesCallersFrame()) {
                        // Only those variables that are used/defined in the closure, and are in the required stores set 
                        // will need to be stored into the frame before the call!
                        // FIXME: Strictly only those vars that are used before being defined need to be stored ... but, that is
                        // a minor detail!
                        Set<Variable> newDirtyVars = new HashSet<Variable>(dirtyVars);
                        for (Variable v: dirtyVars) {
                            if (cl_fsp.scopeDefinesOrUsesVariable(v))
                                newDirtyVars.remove(v);
                        }
                        dirtyVars = newDirtyVars;
                    }
                }
                // Call has no closure && it requires stores
                else if (call.usesCallersFrame()) {
                    dirtyVars.clear();
                }
            }

            Variable v = i.getResult();
            if (v != null)
                dirtyVars.add(v);
        }

        if (_outDirtyVars.equals(dirtyVars)) {
            return false;
        }
        else {
            _outDirtyVars = dirtyVars;
            return true;
        }
    }

    public String toString() { return ""; }

    public void addStores()
    {
        FrameStorePlacementProblem fsp    = (FrameStorePlacementProblem)_prob;
        IR_ExecutionScope          s      = fsp.getCFG().getScope();
        ListIterator<IR_Instr>     instrs = _bb.getInstrs().listIterator();
        Set<Variable>              dirtyVars = new HashSet<Variable>(_inDirtyVars);
        while (instrs.hasNext()) {
            IR_Instr i = instrs.next();
            if (i instanceof CALL_Instr) {
                CALL_Instr call = (CALL_Instr)i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    CFG cl_cfg = ((IR_Closure)((MetaObject)o)._scope).getCFG();
                    if (call.usesCallersFrame()) {
                        FrameStorePlacementProblem cl_fsp = (FrameStorePlacementProblem)cl_cfg.getDataFlowSolution(fsp.getName());
                        instrs.previous();
                        instrs.add(new ALLOC_FRAME_Instr(s));

                        // Only those variables that are used/defined in the closure, and are in the required stores set 
                        // will need to be stored into the frame before the call!
                        // FIXME: Strictly only those vars that are used before being defined need to be stored ... but, that is
                        // a minor detail!
                        Set<Variable> newDirtyVars = new HashSet<Variable>(dirtyVars);
                        for (Variable v: dirtyVars) {
                            if (cl_fsp.scopeDefinesOrUsesVariable(v)) {
                                instrs.add(new STORE_TO_FRAME_Instr(s, v._name, v));
                                newDirtyVars.remove(v);
                            }
                        }
                        dirtyVars = newDirtyVars;
                        instrs.next();
                    }

                    // add stores in the closure
                    ((FrameStorePlacementProblem)cl_cfg.getDataFlowSolution(fsp.getName())).addStores();
                }
                // Call has no closure && it requires stores
                else if (call.usesCallersFrame()) {
                    instrs.previous();
                    instrs.add(new ALLOC_FRAME_Instr(s));
                    for (Variable v: dirtyVars)
                        instrs.add(new STORE_TO_FRAME_Instr(s, v._name, v));
                    instrs.next();
                    dirtyVars.clear();
                }
            }

            Variable v = i.getResult();
            if (v != null)
                dirtyVars.add(v);
        }
    }

/* ---------- Private fields, methods --------- */
    private Set<Variable> _inDirtyVars;     // On entry to flow graph node:  Variables that need to be stored to the heap frame
    private Set<Variable> _outDirtyVars;    // On exit from flow graph node: Variables that need to be stored to the heap frame
}
