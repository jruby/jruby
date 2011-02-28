package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.DataFlowConstants;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.AllocateBindingInstr;
import org.jruby.compiler.ir.instructions.StoreToBindingInstr;
import org.jruby.compiler.ir.instructions.ClosureReturnInstr;
import org.jruby.compiler.ir.instructions.BREAK_Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.compiler.ir.representations.CFG.CFG_Edge;

import java.util.Set;
import java.util.HashSet;
import java.util.ListIterator;
import org.jruby.compiler.ir.operands.LocalVariable;

public class BindingStorePlacementNode extends FlowGraphNode {
    /* ---------- Public fields, methods --------- */
    public BindingStorePlacementNode(DataFlowProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        _inDirtyVars = new HashSet<Variable>();
        _outDirtyVars = new HashSet<Variable>();

        // For closure scopes, the heap binding will already have been allocated in the parent scope
        // So, don't even bother with the binding allocation in closures!
        if (_prob.getCFG().getScope() instanceof IRClosure) {
            _inBindingAllocated = _outBindingAllocated = true;
        } else {
            _inBindingAllocated = _outBindingAllocated = false;
        }
    }

    public void buildDataFlowVars(Instr i) {
       // Nothing to do -- because we are going to use LocalVariables as our data flow variables
    }

    public void initSolnForNode() {
       // Nothing to do
    }

    public void compute_MEET(CFG_Edge edge, FlowGraphNode pred) {
        BindingStorePlacementNode n = (BindingStorePlacementNode) pred;
        _inDirtyVars.addAll(n._outDirtyVars);

        // For binding allocation, we are using the and operator -- so only if the binding has been allocated
        // on all incoming paths do we consider that a binding has been allocated 
        _inBindingAllocated = _inBindingAllocated && n._outBindingAllocated;
    }

    public boolean applyTransferFunction() {
        boolean bindingAllocated = _inBindingAllocated;

        BindingStorePlacementProblem bsp = (BindingStorePlacementProblem) _prob;
        Set<Variable> dirtyVars = new HashSet<Variable>(_inDirtyVars);

        for (Instr i : _bb.getInstrs()) {
            if (i.operation == Operation.BINDING_LOAD) continue;

            // Process calls specially -- these are the sites of binding stores!
            if (i instanceof CallInstr) {
                CallInstr call = (CallInstr) i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    // At this call site, a binding will get allocated if it has not been already!
                    bindingAllocated = true;

                    IRClosure cl = (IRClosure) ((MetaObject) o).scope;
                    CFG cl_cfg = cl.getCFG();
                    BindingStorePlacementProblem cl_bsp = new BindingStorePlacementProblem();
                    cl_bsp.setup(cl_cfg);
                    cl_bsp.compute_MOP_Solution();
                    cl_cfg.setDataFlowSolution(cl_bsp.getName(), cl_bsp);

                    // If the call is an eval, or if the callee can capture this method's binding, we have to spill all variables.
                    boolean spillAllVars = call.canBeEval() || call.canCaptureCallersBinding();

                    // - If all variables have to be spilled, then those variables will no longer be dirty after the call site
                    // - If a variable is used in the closure (FIXME: Strictly only those vars that are live at the call site -- 
                    //   but we dont have this info!), it has to be spilt. So, these variables are no longer dirty after the call site.
                    // - If a variable is (re)defined in the closure, it will be saved inside the closure.  So, these variables
                    //   won't be dirty after the call site either!
                    Set<Variable> newDirtyVars = new HashSet<Variable>(dirtyVars);
                    for (Variable v : dirtyVars) {
                        if (spillAllVars || cl_bsp.scopeUsesVariable(v) || cl_bsp.scopeDefinesVariable(v)) {
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                } // Call has no closure && it requires stores
                else if (call.requiresBinding()) {
                    dirtyVars.clear();
                    bindingAllocated = true;
                }
            }

            Variable v = i.getResult();

            if ((v != null) && (v instanceof LocalVariable)) dirtyVars.add(v);
            if (i.operation.isReturn()) dirtyVars.clear();
        }

        if (_outDirtyVars.equals(dirtyVars) && (_outBindingAllocated == bindingAllocated)) return false;

        _outDirtyVars = dirtyVars;
        _outBindingAllocated = bindingAllocated;
        return true;
    }

    @Override
    public String toString() {
        return "";
    }

    public void addStoreAndBindingAllocInstructions() {
        BindingStorePlacementProblem bsp = (BindingStorePlacementProblem) _prob;
        CFG cfg = bsp.getCFG();
        IRExecutionScope s = cfg.getScope();
        ListIterator<Instr> instrs = _bb.getInstrs().listIterator();
        Set<Variable> dirtyVars = new HashSet<Variable>(_inDirtyVars);
        boolean bindingAllocated = _inBindingAllocated;

        // If this is the exit BB, we need a binding story on exit only for vars that are both:
        //
        //   (a) dirty,
        //   (b) live on exit from the closure
        //       condition reqd. because the variable could be dirty but not used outside.
        //         Ex: s=0; a.each { |i| j = i+1; sum += j; }; puts sum
        //       i,j are dirty inside the block, but not used outside

        boolean amExitBB = (_bb == cfg.getExitBB());
        if (amExitBB) {
/**
            LiveVariablesProblem lvp = (LiveVariablesProblem)cfg.getDataFlowSolution(DataFlowConstants.LVP_NAME);
            java.util.Collection<Variable> liveVars = lvp.getVarsLiveOnEntry();
            System.out.println("\n[In Exit BB] For CFG " + cfg + ":");
            System.out.println("\t--> Dirty vars here   : " + java.util.Arrays.toString(dirtyVars.toArray()));
            System.out.println("\t--> Vars live on entry: " + (liveVars == null ? "NONE" : java.util.Arrays.toString(liveVars.toArray())));
            liveVars = lvp.getVarsLiveOnExit();
            System.out.println("\t--> Vars live on exit : " + (liveVars == null ? "NONE" : java.util.Arrays.toString(liveVars.toArray())));
**/
            LiveVariablesProblem lvp = (LiveVariablesProblem)cfg.getDataFlowSolution(DataFlowConstants.LVP_NAME);
            java.util.Collection<Variable> liveVars = lvp.getVarsLiveOnExit();
            if (liveVars != null)
                dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
            else
                dirtyVars.clear();
        }

        while (instrs.hasNext()) {
            Instr i = instrs.next();
            if (i.operation == Operation.BINDING_LOAD) continue;

            if (i instanceof CallInstr) {
                CallInstr call = (CallInstr) i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    CFG cl_cfg = ((IRClosure) ((MetaObject) o).scope).getCFG();
                    BindingStorePlacementProblem cl_bsp = (BindingStorePlacementProblem) cl_cfg.getDataFlowSolution(bsp.getName());

                    // Add a binding allocation instruction, if necessary
                    instrs.previous();
                    if (!bindingAllocated) {
                        instrs.add(new AllocateBindingInstr(s));
                        bindingAllocated = true;
                    }

                    // If the call is an eval, or if the callee can capture this method's binding,
                    // we have to spill all variables.
                    boolean spillAllVars = call.canBeEval() || call.canCaptureCallersBinding();

                    // Unless we have to spill everything, spill only those dirty variables that are:
                    // - used in the closure (FIXME: Strictly only those vars that are live at the call site -- but we dont have this info!)
                    Set<Variable> newDirtyVars = new HashSet<Variable>(dirtyVars);
                    for (Variable v : dirtyVars) {
                        if (spillAllVars || cl_bsp.scopeUsesVariable(v)) {
                            // FIXME: This may not need check for local variable if it is guaranteed to only be local variables.
                            instrs.add(new StoreToBindingInstr(s, v.getName(), v));
                            newDirtyVars.remove(v);
                        } // These variables will be spilt inside the closure -- so they will no longer be dirty after the call site!
                        else if (cl_bsp.scopeDefinesVariable(v)) {
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                    instrs.next();

                    // add stores in the closure
                    ((BindingStorePlacementProblem) cl_cfg.getDataFlowSolution(bsp.getName())).addStoreAndBindingAllocInstructions();
                } // Call has no closure && it requires stores
                else if (call.requiresBinding()) {
                    instrs.previous();
                    if (!bindingAllocated) {
                        instrs.add(new AllocateBindingInstr(s));
                        bindingAllocated = true;
                    }
                    for (Variable v : dirtyVars) {
                        instrs.add(new StoreToBindingInstr(s, v.getName(), v));
                    }
                    instrs.next();
                    dirtyVars.clear();
                }
            } else if ((i instanceof ClosureReturnInstr) || (i instanceof BREAK_Instr)) {
                // At closure return and break instructions (both of which are exits from the closure),
                // we need a binding store on exit only for vars that are both:
                //
                //   (a) dirty,
                //   (b) live on exit from the closure
                //       condition reqd. because the variable could be dirty but not used outside.
                //         Ex: s=0; a.each { |i| j = i+1; sum += j; }; puts sum
                //       i,j are dirty inside the block, but not used outside
                //
                // If this also happens to be exit BB, we would have intersected already earlier -- so no need to do it again!
                if (!amExitBB) {
/**
                    LiveVariablesProblem lvp = (LiveVariablesProblem)cfg.getDataFlowSolution(DataFlowConstants.LVP_NAME);
                    java.util.Collection<Variable> liveVars = lvp.getVarsLiveOnEntry();
                    System.out.println("\n[@Closure Instr<" + i + ">] For CFG " + cfg + ":");
                    System.out.println("\t--> Dirty vars here   : " + java.util.Arrays.toString(dirtyVars.toArray()));
                    System.out.println("\t--> Vars live on entry: " + (liveVars == null ? "NONE" : java.util.Arrays.toString(liveVars.toArray())));
                    liveVars = lvp.getVarsLiveOnExit();
                    System.out.println("\t--> Vars live on exit : " + (liveVars == null ? "NONE" : java.util.Arrays.toString(liveVars.toArray())));
**/
                    LiveVariablesProblem lvp = (LiveVariablesProblem)cfg.getDataFlowSolution(DataFlowConstants.LVP_NAME);
                    java.util.Collection<Variable> liveVars = lvp.getVarsLiveOnExit();
                    if (liveVars != null)
                        dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
                    else
                        dirtyVars.clear();
                }

                instrs.previous();
                for (Variable v : dirtyVars) {
                    instrs.add(new StoreToBindingInstr(s, v.getName(), v));
                }
                instrs.next();

                // Nothing is dirty anymore -- everything that needs spilling has been spilt
                dirtyVars.clear();
            }

            Variable v = i.getResult();
            if ((v != null) && (v instanceof LocalVariable)) dirtyVars.add(v);
        }

        // If this is the exit BB, add binding stores for all vars that are still dirty
        if (amExitBB) {
            for (Variable v : dirtyVars) {
                instrs.add(new StoreToBindingInstr(s, v.getName(), v));
            }
        }
    }

    /* ---------- Package fields, methods --------- */
    Set<Variable> _inDirtyVars;     // On entry to flow graph node:  Variables that need to be stored to the heap binding
    Set<Variable> _outDirtyVars;    // On exit from flow graph node: Variables that need to be stored to the heap binding
    boolean _inBindingAllocated;   // Flag on entry to bb as to whether a binding has been allocated?
    boolean _outBindingAllocated;   // Flag on exit to bb as to whether a binding has been allocated?
}
