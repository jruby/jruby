package org.jruby.ir.dataflow.analyses;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.dataflow.DataFlowConstants;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.AllocateBindingInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.ClosureReturnInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;

public class StoreLocalVarPlacementNode extends FlowGraphNode {
    public StoreLocalVarPlacementNode(DataFlowProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        inDirtyVars = new HashSet<LocalVariable>();
        outDirtyVars = new HashSet<LocalVariable>();

        // For closure scopes, the heap binding will already have been allocated in the parent scope
        // So, don't even bother with the binding allocation in closures!
        if (problem.getScope().cfg().getScope() instanceof IRClosure) {
            inBindingAllocated = outBindingAllocated = true;
        } else {
            inBindingAllocated = outBindingAllocated = false;
        }
    }

    public void buildDataFlowVars(Instr i) {
        // Nothing to do -- because we are going to simply use non-closure, non-self, non-block LocalVariables as our data flow variables
        // rather than build a new data flow type for it
    }

    public void initSolnForNode() {
        // Not doing this right now because transfer function updates
        // in set for exc target nodes (rescue blocks & exit blocks)
        //
        // Clear inDirtyVars for the next pass
        // inDirtyVars = new HashSet<LocalVariable>();
    }

    public void compute_MEET(BasicBlock source, FlowGraphNode pred) {
        StoreLocalVarPlacementNode n = (StoreLocalVarPlacementNode) pred;
        inDirtyVars.addAll(n.outDirtyVars);

        // For binding allocation, we are using the and operator -- so only if the binding has been allocated
        // on all incoming paths do we consider that a binding has been allocated 
        inBindingAllocated = inBindingAllocated && n.outBindingAllocated;
    }

    public boolean applyTransferFunction() {
        boolean bindingAllocated = inBindingAllocated;

        Set<LocalVariable> dirtyVars = new HashSet<LocalVariable>(inDirtyVars);

        for (Instr i : basicBlock.getInstrs()) {
            // Process calls specially -- these are the sites of binding stores!
            if (i instanceof CallBase) {
                CallBase call = (CallBase) i;
                // At this call site, a binding will get allocated if it has not been already!
                Operand o = call.getClosureArg(null);
                if (o != null && o instanceof WrappedIRClosure) {
                    // In this first pass, the current scope and the call's closure are considered
                    // independent of each other which means any variable that is used by the variable
                    // will get spilled into the binding.  This is clearly conservative, but simplifies
                    // the analysis.
                    bindingAllocated = true;
                    IRClosure cl = ((WrappedIRClosure) o).getClosure();

                    // If the call is a dataflow barrier, we have to spill everything here
                    boolean spillAllVars = call.isDataflowBarrier();

                    // - If all variables have to be spilled, then those variables will no longer be dirty after the call site
                    // - If a variable is used in the closure (FIXME: Strictly only those vars that are live at the call site -- 
                    //   but we dont have this info!), it has to be spilt. So, these variables are no longer dirty after the call site.
                    // - If a variable is (re)defined in the closure, it will always be loaded after the call. So, we have to always
                    //   spill it before the call in the scenario that the closure never gets executed! So, it won't be dirty after
                    //   the call site.
                    Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                    for (LocalVariable v : dirtyVars) {
                        if (spillAllVars || cl.usesLocalVariable(v) || cl.definesLocalVariable(v)) {
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                } else if (call.isDataflowBarrier()) { // Call has no closure && it requires stores
                    bindingAllocated = true;
                    dirtyVars.clear();
                } else if (call.canSetDollarVars()) {
                    bindingAllocated = true;
                }
            }

            // If this instruction can raise an exception, then add current dirty-vars
            // to the inDirtyVars of the exception target
            if (i.canRaiseException()) {
                Set<LocalVariable> excTargetIn = ((StoreLocalVarPlacementNode)getExceptionTargetNode()).inDirtyVars;
                excTargetIn.addAll(dirtyVars);
            }

            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr) i).getResult();

                // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
                if (v instanceof LocalVariable && !((LocalVariable) v).isSelf()) dirtyVars.add((LocalVariable) v);
            }

            if (i.getOperation().isReturn()) dirtyVars.clear();
        }

        if (outDirtyVars.equals(dirtyVars) && outBindingAllocated == bindingAllocated) return false;

        outDirtyVars = dirtyVars;
        outBindingAllocated = bindingAllocated;

        return true;
    }

    @Override
    public String toString() {
        return "";
    }

    private TemporaryVariable getLocalVarReplacement(LocalVariable v, IRScope scope, Map<Operand, Operand> varRenameMap) {
         TemporaryVariable value = (TemporaryVariable)varRenameMap.get(v);
         if (value == null) {
             value = scope.getNewTemporaryVariable("%t_" + v.getName());
             varRenameMap.put(v, value);
         }
         return value;
    }

    private void addClosureExitStoreLocalVars(IRScope scope, ListIterator<Instr> instrs, Set<LocalVariable> dirtyVars, Map<Operand, Operand> varRenameMap) {
        boolean isEvalScript = scope instanceof IREvalScript;
        for (LocalVariable v : dirtyVars) {
            if (isEvalScript || !(v instanceof ClosureLocalVariable) || (scope != ((ClosureLocalVariable)v).definingScope)) {
                instrs.add(new StoreLocalVarInstr(getLocalVarReplacement(v, scope, varRenameMap), scope, v));
            }
        }
    }

    public void addStoreAndBindingAllocInstructions(Map<Operand, Operand> varRenameMap, Set<LocalVariable> excTargetDirtyVars) {
        StoreLocalVarPlacementProblem bsp = (StoreLocalVarPlacementProblem) problem;
        IRScope scope = bsp.getScope();

        boolean isEvalScript                   = scope instanceof IREvalScript;
        boolean addAllocateBindingInstructions = false; // SSS: This is going to be useful during JIT -- we are far away from there at this time
        boolean bindingAllocated               = inBindingAllocated;

        ListIterator<Instr> instrs    = basicBlock.getInstrs().listIterator();
        Set<LocalVariable>  dirtyVars = new HashSet<LocalVariable>(inDirtyVars);

        // If this is the exit BB, we need a binding store on exit only for vars that are both:
        //
        //   (a) dirty,
        //   (b) live on exit from the closure
        //       condition reqd. because the variable could be dirty but not used outside.
        //         Ex: s=0; a.each { |i| j = i+1; sum += j; }; puts sum
        //       i,j are dirty inside the block, but not used outside
        //
        // LVA results for eval scripts cannot be used
        boolean amExitBB = basicBlock == scope.cfg().getExitBB();
        if (!isEvalScript && amExitBB) {
            LiveVariablesProblem lvp = (LiveVariablesProblem)scope.getDataFlowSolution(DataFlowConstants.LVP_NAME);
            java.util.Collection<LocalVariable> liveVars = lvp.getVarsLiveOnScopeExit();
            if (liveVars != null) {
                dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
            } else {
                dirtyVars.clear();
            }
        }

        while (instrs.hasNext()) {
            Instr i = instrs.next();

            if (i instanceof CallBase) {
                CallBase call = (CallBase) i;
                Operand o = call.getClosureArg(null);
                if (o != null && o instanceof WrappedIRClosure) {
                    IRClosure cl = ((WrappedIRClosure) o).getClosure();

                    instrs.previous();
                    if (addAllocateBindingInstructions) {
                       // Add a binding allocation instruction, if necessary
                       if (!bindingAllocated) {
                           instrs.add(new AllocateBindingInstr(scope));
                           bindingAllocated = true;
                       }
                    }

                    // If the call is a dataflow barrier, we have to spill everything here
                    boolean spillAllVars = call.isDataflowBarrier();

                    // Unless we have to spill everything, spill only those dirty variables that are:
                    // - used in the closure (FIXME: Strictly only those vars that are live at the call site -- but we dont have this info!)
                    Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                    for (LocalVariable v : dirtyVars) {
                        // We have to spill the var that is defined in the closure as well because the load var pass
                        // will attempt to load the var always.  So, if the call doesn't actually call the closure,
                        // we'll be in trouble in that scenario!
                        if (spillAllVars || cl.usesLocalVariable(v) || cl.definesLocalVariable(v)) {
                            instrs.add(new StoreLocalVarInstr(getLocalVarReplacement(v, scope, varRenameMap), scope, v));
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                    instrs.next();
                } else if (call.isDataflowBarrier()) { // Call has no closure && it requires stores
                    instrs.previous();
                    if (addAllocateBindingInstructions) {
                        if (!bindingAllocated) {
                            instrs.add(new AllocateBindingInstr(scope));
                            bindingAllocated = true;
                        }
                    }
                    for (LocalVariable v : dirtyVars) {
                        instrs.add(new StoreLocalVarInstr(getLocalVarReplacement(v, scope, varRenameMap), scope, v));
                    }
                    instrs.next();
                    dirtyVars.clear();
                } else if (call.canSetDollarVars()) {
                    if (addAllocateBindingInstructions) {
                        if (!bindingAllocated) {
                            instrs.add(new AllocateBindingInstr(scope));
                            bindingAllocated = true;
                        }
                    }
                }
            } else if ((i instanceof ClosureReturnInstr) || (i instanceof BreakInstr)) {
                // At closure return and break instructions (both of which are exits from the closure),
                // we need a binding store on exit only for vars that are both:
                //
                //   (a) dirty,
                //   (b) live on exit from the closure
                //       condition reqd. because the variable could be dirty but not used outside.
                //         Ex: s=0; a.each { |i| j = i+1; sum += j; return if j < 5; sum += 1; }; puts sum
                //       i,j are dirty inside the block, but not used outside
                //
                // If this also happens to be exit BB, we would have intersected already earlier -- so no need to do it again!
                //
                // LVA results for eval scripts cannot be used
                if (!isEvalScript && !amExitBB) {
                    LiveVariablesProblem lvp = (LiveVariablesProblem)scope.getDataFlowSolution(DataFlowConstants.LVP_NAME);
                    java.util.Collection<LocalVariable> liveVars = lvp.getVarsLiveOnScopeExit();
                    if (liveVars != null) {
                        dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
                    } else {
                        dirtyVars.clear();
                    }
                }

                instrs.previous();
                addClosureExitStoreLocalVars(scope, instrs, dirtyVars, varRenameMap);
                instrs.next();

                // Nothing is dirty anymore -- everything that needs spilling has been spilt
                dirtyVars.clear();
            }

            // These variables would have to be spilled into the binding if this
            // instruction raised an exception and exited this scope.
            if ((excTargetDirtyVars != null) && i.canRaiseException()) excTargetDirtyVars.addAll(dirtyVars);

            if (i instanceof ResultInstr) {
                Variable v = ((ResultInstr) i).getResult();

                // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
                if (v instanceof LocalVariable && !((LocalVariable) v).isSelf()) {
                    LocalVariable lv = (LocalVariable) v;
                    dirtyVars.add(lv);

                    // Make sure there is a replacement tmp-var allocated for lv
                    getLocalVarReplacement(lv, scope, varRenameMap);
                }
            }             
        }

        // If this is the exit BB, add binding stores for all vars that are still dirty
        if (amExitBB) addClosureExitStoreLocalVars(scope, instrs, dirtyVars, varRenameMap);
    }

    Set<LocalVariable> inDirtyVars;   // On entry to flow graph node:  Variables that need to be stored to the heap binding
    Set<LocalVariable> outDirtyVars;  // On exit from flow graph node: Variables that need to be stored to the heap binding
    boolean inBindingAllocated;       // Flag on entry to bb as to whether a binding has been allocated?
    boolean outBindingAllocated;      // Flag on exit to bb as to whether a binding has been allocated?
}
