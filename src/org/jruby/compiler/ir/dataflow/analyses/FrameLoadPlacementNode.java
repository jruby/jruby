package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.LOAD_FROM_FRAME_Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class FrameLoadPlacementNode extends FlowGraphNode
{
/* ---------- Public fields, methods --------- */
    public FrameLoadPlacementNode(DataFlowProblem prob, BasicBlock n) { super(prob, n); }

    public void init()
    {
        _inReqdLoads  = new HashSet<Variable>();
        _outReqdLoads = new HashSet<Variable>();
    }

    public void buildDataFlowVars(IR_Instr i)
    {
        FrameLoadPlacementProblem flp = (FrameLoadPlacementProblem)_prob;
        for (Variable v: i.getUsedVariables())
            flp.recordUsedVar(v);

        Variable v = i.getResult();
        if (v != null)
            flp.recordDefVar(v);
    }

    public void initSolnForNode() 
    {
        if (_bb == _prob.getCFG().getExitBB())
            _outReqdLoads = ((FrameLoadPlacementProblem)_prob).getNestedProblemInitLoads();
    }

    public void compute_MEET(CFG_Edge edge, FlowGraphNode pred)
    {
        // We have to take a union of all load sets of flow graph predecessors -- but that can lead
        // to useless loads on *all* paths based on loads being required on *some* paths.
        //
        // So, take an intersection instead -- but, while adding loads, we have to add the missing
        // loads on individual execution paths -- see addLoads in FrameLoadPlacementProblem 
		  //
		  // SSS FIXME: Work through this and see if there are problems with the monotonicity of the lattice value
		  // With union, the load set keeps increasing till it hits bottom (all variables!)
		  // But, is there a possibility that with intersection, we get stuck in an infinite loop??
        _inReqdLoads.retainAll(((FrameLoadPlacementNode)pred)._outReqdLoads);
    }

    public boolean applyTransferFunction()
    {
        FrameLoadPlacementProblem flp = (FrameLoadPlacementProblem)_prob;
        Set<Variable> reqdLoads = new HashSet<Variable>(_inReqdLoads);

        List<IR_Instr> instrs = _bb.getInstrs();
        ListIterator<IR_Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            IR_Instr i = it.previous();
            // Process calls specially -- these are the sites of frame loads!
            if (i instanceof CALL_Instr) {
                CALL_Instr call = (CALL_Instr)i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    IR_Closure cl = (IR_Closure)((MetaObject)o)._scope;
                    CFG cl_cfg = cl.getCFG();
                    FrameLoadPlacementProblem cl_flp = new FrameLoadPlacementProblem();
                    cl_flp.initNestedProblem(reqdLoads);
                    cl_flp.setup(cl_cfg);
                    cl_flp.compute_MOP_Solution();
                    cl_cfg.setDataFlowSolution(cl_flp.getName(), cl_flp);

                    // Only those variables that are defined in the closure, and are in the required loads set 
                    // will need to be loaded from the frame after the call!
                    Set<Variable> newReqdLoads = new HashSet<Variable>(reqdLoads);
                    for (Variable v: reqdLoads) {
                        if (cl_flp.scopeDefinesVariable(v))
                            newReqdLoads.remove(v);
                    }
                    reqdLoads = newReqdLoads;
                }
                // In this case, we are going to blindly load everything -- so, at the call site, pending loads dont carry over!
                else if (call.requiresFrame()) {
                    reqdLoads.clear();
                }
            }

            // The variable used to store the instruction result won't need to be loaded!
            Variable v = i.getResult();
            if (v != null)
                reqdLoads.remove(v);

            // The variables used as arguments will need to be loaded
            for (Variable x: i.getUsedVariables())
                reqdLoads.add(x);
        }

        if (_outReqdLoads.equals(reqdLoads)) {
            return false;
        }
        else {
            _outReqdLoads = reqdLoads;
            return true;
        }
    }

    public String toString() { return ""; }

    public void addLoads()
    {
        FrameLoadPlacementProblem flp    = (FrameLoadPlacementProblem)_prob;
        IR_ExecutionScope         s      = flp.getCFG().getScope();
        List<IR_Instr>            instrs = _bb.getInstrs();
        ListIterator<IR_Instr>    it     = instrs.listIterator(instrs.size());
        Set<Variable>             reqdLoads = new HashSet<Variable>(_inReqdLoads);
        while (it.hasPrevious()) {
            IR_Instr i = it.previous();
            if (i instanceof CALL_Instr) {
                CALL_Instr call = (CALL_Instr)i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    CFG cl_cfg = ((IR_Closure)((MetaObject)o)._scope).getCFG();
                    FrameLoadPlacementProblem cl_flp = (FrameLoadPlacementProblem)cl_cfg.getDataFlowSolution(flp.getName());

                    // Only those variables that are defined in the closure, and are in the required loads set 
                    // will need to be loaded from the frame after the call!
                    Set<Variable> newReqdLoads = new HashSet<Variable>(reqdLoads);
                    it.next();
                    for (Variable v: reqdLoads) {
                        if (cl_flp.scopeDefinesVariable(v)) {
                            it.add(new LOAD_FROM_FRAME_Instr(v, s, v._name));
                            it.previous();
                            newReqdLoads.remove(v);
                        }
                    }
                    it.previous();
                    reqdLoads = newReqdLoads;

                    // add loads in the closure
                    ((FrameLoadPlacementProblem)cl_cfg.getDataFlowSolution(flp.getName())).addLoads();
                }
                else if (call.requiresFrame()) {
                    it.next();
                    for (Variable v: reqdLoads) {
                        it.add(new LOAD_FROM_FRAME_Instr(v, s, v._name));
                        it.previous();
                    }
                    it.previous();
                    reqdLoads.clear();
                }
            }

            Variable v = i.getResult();
            if (v != null)
                reqdLoads.add(v);

            // The variables used as arguments will need to be loaded
            for (Variable x: i.getUsedVariables())
                reqdLoads.add(x);
        }

        // Load first use of variables in closures
        if ((s instanceof IR_Closure) && (_bb == _prob.getCFG().getEntryBB())) {
            for (Variable v: _outReqdLoads) {
                if (flp.scopeUsesVariable(v)) {
                    it.add(new LOAD_FROM_FRAME_Instr(v, s, v._name));
                }
            }
        }
    }

/* ---------- Private fields, methods --------- */
    Set<Variable> _inReqdLoads;     // On entry to flow graph node:  Variables that need to be loaded from the heap frame
    Set<Variable> _outReqdLoads;    // On exit from flow graph node: Variables that need to be loaded from the heap frame
}
