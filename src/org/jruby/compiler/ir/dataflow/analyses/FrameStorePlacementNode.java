package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IR_Closure;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.CallInstruction;
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
import org.jruby.compiler.ir.operands.SelfVariable;
import org.jruby.compiler.ir.operands.LocalVariable;

public class FrameStorePlacementNode extends FlowGraphNode {
    /* ---------- Public fields, methods --------- */
    public FrameStorePlacementNode(DataFlowProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        _inDirtyVars = new HashSet<Variable>();
        _outDirtyVars = new HashSet<Variable>();

        // For closure scopes, the heap frame will already have been allocated in the parent scope
        // So, don't even bother with the frame allocation in closures!
        if (_prob.getCFG().getScope() instanceof IR_Closure) {
            _inFrameAllocated = _outFrameAllocated = true;
        } else {
            _inFrameAllocated = _outFrameAllocated = false;
        }
    }

    // Only ruby local variables are candidates for frame stores.  Ignore the rest!
    // SSS FIXME: What about self?
    public void buildDataFlowVars(IR_Instr i) {
        FrameStorePlacementProblem fsp = (FrameStorePlacementProblem) _prob;
        for (Variable v : i.getUsedVariables()) {
            if ((v instanceof LocalVariable) || (v instanceof SelfVariable))
                fsp.recordUsedVar(v);
        }

        Variable v = i.getResult();

        if ((v != null) && ((v instanceof LocalVariable) || (v instanceof SelfVariable))) fsp.recordDefVar(v);
    }

    public void initSolnForNode() {
    }

    public void compute_MEET(CFG_Edge edge, FlowGraphNode pred) {
        FrameStorePlacementNode n = (FrameStorePlacementNode) pred;
        _inDirtyVars.addAll(n._outDirtyVars);

        // For frame allocation, we are using the and operator -- so only if the frame has been allocated
        // on all incoming paths do we consider that a frame has been allocated 
        _inFrameAllocated = _inFrameAllocated && n._outFrameAllocated;
    }

    public boolean applyTransferFunction() {
        boolean frameAllocated = _inFrameAllocated;

        FrameStorePlacementProblem fsp = (FrameStorePlacementProblem) _prob;
        Set<Variable> dirtyVars = new HashSet<Variable>(_inDirtyVars);

        for (IR_Instr i : _bb.getInstrs()) {
            if (i._op == Operation.FRAME_LOAD) continue;

            // Process calls specially -- these are the sites of frame stores!
            if (i instanceof CallInstruction) {
                CallInstruction call = (CallInstruction) i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    // At this call site, a frame will get allocated if it has not been already!
                    frameAllocated = true;

                    IR_Closure cl = (IR_Closure) ((MetaObject) o)._scope;
                    CFG cl_cfg = cl.getCFG();
                    FrameStorePlacementProblem cl_fsp = new FrameStorePlacementProblem();
                    cl_fsp.setup(cl_cfg);
                    cl_fsp.compute_MOP_Solution();
                    cl_cfg.setDataFlowSolution(cl_fsp.getName(), cl_fsp);

                    // If the call is an eval, or if the callee can capture this method's frame, we have to spill all variables.
                    boolean spillAllVars = call.canBeEval() || call.canCaptureCallersFrame();

                    // - If all variables have to be spilled, then those variables will no longer be dirty after the call site
                    // - If a variable is used in the closure (FIXME: Strictly only those vars that are live at the call site -- 
                    //   but we dont have this info!), it has to be spilt. So, these variables are no longer dirty after the call site.
                    // - If a variable is (re)defined in the closure, it will be saved inside the closure.  So, these variables
                    //   won't be dirty after the call site either!
                    Set<Variable> newDirtyVars = new HashSet<Variable>(dirtyVars);
                    for (Variable v : dirtyVars) {
                        if (spillAllVars || cl_fsp.scopeUsesVariable(v) || cl_fsp.scopeDefinesVariable(v)) {
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                } // Call has no closure && it requires stores
                else if (call.requiresFrame()) {
                    dirtyVars.clear();
                    frameAllocated = true;
                }
            }

            Variable v = i.getResult();

            if ((v != null) && ((v instanceof LocalVariable) || (v instanceof SelfVariable))) dirtyVars.add(v);
            if (i._op.isReturn()) dirtyVars.clear();
        }

        // At the end of the scope, there are no more dirty vars!
        // Dirty vars at the end of the closure are handled specially by 'addStoreAndFrameAllocInstructions'
        CFG cfg = _prob.getCFG();

        if (_bb == cfg.getExitBB()) dirtyVars.clear();

        if (_outDirtyVars.equals(dirtyVars) && (_outFrameAllocated == frameAllocated)) return false;

        _outDirtyVars = dirtyVars;
        _outFrameAllocated = frameAllocated;
        return true;
    }

    @Override
    public String toString() {
        return "";
    }

    public void addStoreAndFrameAllocInstructions() {
        FrameStorePlacementProblem fsp = (FrameStorePlacementProblem) _prob;
        IR_ExecutionScope s = fsp.getCFG().getScope();
        ListIterator<IR_Instr> instrs = _bb.getInstrs().listIterator();
        Set<Variable> dirtyVars = new HashSet<Variable>(_inDirtyVars);
        boolean frameAllocated = _inFrameAllocated;

        while (instrs.hasNext()) {
            IR_Instr i = instrs.next();
            if (i._op == Operation.FRAME_LOAD) continue;

            if (i instanceof CallInstruction) {
                CallInstruction call = (CallInstruction) i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    CFG cl_cfg = ((IR_Closure) ((MetaObject) o)._scope).getCFG();
                    FrameStorePlacementProblem cl_fsp = (FrameStorePlacementProblem) cl_cfg.getDataFlowSolution(fsp.getName());

                    // Add a frame allocation instruction, if necessary
                    instrs.previous();
                    if (!frameAllocated) {
                        instrs.add(new ALLOC_FRAME_Instr(s));
                        frameAllocated = true;
                    }

                    // If the call is an eval, or if the callee can capture this method's frame,
                    // we have to spill all variables.
                    boolean spillAllVars = call.canBeEval() || call.canCaptureCallersFrame();

                    // Unless we have to spill everything, spill only those dirty variables that are:
                    // - used in the closure (FIXME: Strictly only those vars that are live at the call site -- but we dont have this info!)
                    Set<Variable> newDirtyVars = new HashSet<Variable>(dirtyVars);
                    for (Variable v : dirtyVars) {
                        if (spillAllVars || cl_fsp.scopeUsesVariable(v)) {
                            // FIXME: This may not need check for local variable if it is guaranteed to only be local variables.
                            instrs.add(new STORE_TO_FRAME_Instr(s, v.getName(), v));
                            newDirtyVars.remove(v);
                        } // These variables will be spilt inside the closure -- so they will no longer be dirty after the call site!
                        else if (cl_fsp.scopeDefinesVariable(v)) {
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                    instrs.next();

                    // add stores in the closure
                    ((FrameStorePlacementProblem) cl_cfg.getDataFlowSolution(fsp.getName())).addStoreAndFrameAllocInstructions();
                } // Call has no closure && it requires stores
                else if (call.requiresFrame()) {
                    instrs.previous();
                    if (!frameAllocated) {
                        instrs.add(new ALLOC_FRAME_Instr(s));
                        frameAllocated = true;
                    }
                    for (Variable v : dirtyVars) {
                        instrs.add(new STORE_TO_FRAME_Instr(s, v.getName(), v));
                    }
                    instrs.next();
                    dirtyVars.clear();
                }
            } else if (i instanceof CLOSURE_RETURN_Instr) {
                // At closure return instructions (which are closure exits), for all variables which are:
                //
                //   (a) dirty,
                //   (b) live on exit from the closure
                //       condition reqd. because the variable could be dirty but not used outside.
                //         Ex: s=0; a.each { |i| j = i+1; sum += j; }; puts sum
                //       i,j are dirty inside the block, but not used outside
                //
                // add a frame store

                LiveVariablesProblem lvp = (LiveVariablesProblem) fsp.getCFG().getDataFlowSolution(DataFlowConstants.LVP_NAME);
                dirtyVars.retainAll(lvp.getVarsLiveOnExit()); // Intersection with variables live on exit from the scope

                instrs.previous();
                for (Variable v : dirtyVars) {
                    instrs.add(new STORE_TO_FRAME_Instr(s, v.getName(), v));
                }
                instrs.next();
            }

            Variable v = i.getResult();
            
            if ((v != null) && ((v instanceof LocalVariable) || (v instanceof SelfVariable))) dirtyVars.add(v);
        }
    }

    /* ---------- Package fields, methods --------- */
    Set<Variable> _inDirtyVars;     // On entry to flow graph node:  Variables that need to be stored to the heap frame
    Set<Variable> _outDirtyVars;    // On exit from flow graph node: Variables that need to be stored to the heap frame
    boolean _inFrameAllocated;   // Flag on entry to bb as to whether a frame has been allocated?
    boolean _outFrameAllocated;   // Flag on exit to bb as to whether a frame has been allocated?
}
