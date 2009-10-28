package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.ALLOC_FRAME_Instr;
import org.jruby.compiler.ir.instructions.STORE_TO_FRAME_Instr;
import org.jruby.compiler.ir.instructions.CLOSURE_RETURN_Instr;
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
            fsp.recordUsedVar(v);

        Variable v = i.getResult();
        if (v != null)
            fsp.recordDefVar(v);
    }

    public void initSolnForNode() 
    {
        if (_bb == _prob.getCFG().getEntryBB())
            _inDirtyVars = ((FrameStorePlacementProblem)_prob).getNestedProblemInitStores();
    }

    public void compute_MEET(CFG_Edge edge, FlowGraphNode pred)
    {
        // Intersection of predecessor store sets
        // We have to take a union of all store sets of flow graph predecessors -- but that can lead
        // to useless stores on *all* paths based on stores being required on *some* paths.
        //
        // So, take an intersection instead -- but, while adding stores, we have to add the missing
        // loads on individual execution paths -- see addStores in FrameStorePlacementProblem 
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
                        // Only those variables that are:
                        // (a) dirty, and
                        // (b) used/defined in the closure
                        //     (FIXME: Strictly only those vars that are live at the call site -- but we dont have this info!)
                        // will need to be stored into the frame before the call!
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

            if (i._op.isReturn())
                dirtyVars.clear();
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

                        // Only those variables that are:
                        // (a) dirty, and
                        // (b) used/defined in the closure
                        //     (FIXME: Strictly only those vars that are live at the call site -- but we dont have this info!)
                        // will need to be stored into the frame before the call!
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
            else if (i instanceof CLOSURE_RETURN_Instr) {
                // At closure return instructions (which are closure exits), for all variables which are:
                //
                //   (a) dirty,
                //   (b) live on exit from the closure
                //       condition reqd. because the variable could be dirty but not used outside.
                //         Ex: s=0; a.each { |i| j = i+1; sum += j; }; puts sum
                //       i,j are dirty inside the block, but not used outside
                //
                // add a frame store

                LiveVariablesProblem lvp = (LiveVariablesProblem)fsp.getCFG().getDataFlowSolution((new LiveVariablesProblem()).getName());
                dirtyVars.retainAll(lvp.getVarsLiveOnExit()); // Intersection with variables live on exit from the scope

                instrs.previous();
                for (Variable v: dirtyVars) {
                    instrs.add(new STORE_TO_FRAME_Instr(s, v._name, v));
                }
                instrs.next();
            }

            Variable v = i.getResult();
            if (v != null)
                dirtyVars.add(v);
        }
    }

/* ---------- Package fields, methods --------- */
    Set<Variable> _inDirtyVars;     // On entry to flow graph node:  Variables that need to be stored to the heap frame
    Set<Variable> _outDirtyVars;    // On exit from flow graph node: Variables that need to be stored to the heap frame
}
