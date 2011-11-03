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
import org.jruby.compiler.ir.instructions.BreakInstr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.ClosureLocalVariable;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.Set;
import java.util.HashSet;
import java.util.ListIterator;

public class BindingStorePlacementNode extends FlowGraphNode {
    public BindingStorePlacementNode(DataFlowProblem prob, BasicBlock n) {
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
        // Nothing to do
    }

    public void compute_MEET(BasicBlock source, FlowGraphNode pred) {
        BindingStorePlacementNode n = (BindingStorePlacementNode) pred;
        inDirtyVars.addAll(n.outDirtyVars);

        // For binding allocation, we are using the and operator -- so only if the binding has been allocated
        // on all incoming paths do we consider that a binding has been allocated 
        inBindingAllocated = inBindingAllocated && n.outBindingAllocated;
    }

    public boolean applyTransferFunction() {
        boolean bindingAllocated = inBindingAllocated;

        BindingStorePlacementProblem bsp = (BindingStorePlacementProblem) problem;
        Set<LocalVariable> dirtyVars = new HashSet<LocalVariable>(inDirtyVars);

        for (Instr i : basicBlock.getInstrs()) {
            if (i.getOperation() == Operation.BINDING_LOAD) continue;

            // Process calls specially -- these are the sites of binding stores!
            if (i instanceof CallInstr) {
                CallInstr call = (CallInstr) i;
                // At this call site, a binding will get allocated if it has not been already!
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    bindingAllocated = true;

                    IRClosure cl = (IRClosure) ((MetaObject) o).scope;
                    BindingStorePlacementProblem cl_bsp = new BindingStorePlacementProblem();
                    cl_bsp.setup(cl);
                    cl_bsp.compute_MOP_Solution();
                    cl.setDataFlowSolution(cl_bsp.getName(), cl_bsp);

                    // If the call is an eval, or if the callee can capture this method's binding, we have to spill all variables.
                    boolean spillAllVars = call.canBeEval() || call.targetRequiresCallersBinding();

                    // - If all variables have to be spilled, then those variables will no longer be dirty after the call site
                    // - If a variable is used in the closure (FIXME: Strictly only those vars that are live at the call site -- 
                    //   but we dont have this info!), it has to be spilt. So, these variables are no longer dirty after the call site.
                    // - If a variable is (re)defined in the closure, it will be saved inside the closure.  So, these variables
                    //   won't be dirty after the call site either!
                    Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                    for (LocalVariable v : dirtyVars) {
                        if (spillAllVars || cl_bsp.scopeUsesVariable(v) || cl_bsp.scopeDefinesVariable(v)) {
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                } else if (call.targetRequiresCallersBinding()) { // Call has no closure && it requires stores
                    bindingAllocated = true;
                    dirtyVars.clear();
                } else if (call.canSetDollarVars()) {
                    bindingAllocated = true;
                }
            }

            Variable v = i.getResult();

            // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
            if ((v != null) && (v instanceof LocalVariable) && !((LocalVariable)v).isSelf()) {
                dirtyVars.add((LocalVariable)v);
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

    public void addStoreAndBindingAllocInstructions(Set<LocalVariable> callsiteDirtyVars) {
        boolean addAllocateBindingInstructions = false; // SSS: This is going to be useful during JIT -- we are far away from there at this time

        BindingStorePlacementProblem bsp = (BindingStorePlacementProblem) problem;
        IRExecutionScope s = bsp.getScope();
        ListIterator<Instr> instrs = basicBlock.getInstrs().listIterator();
        Set<LocalVariable> dirtyVars = new HashSet<LocalVariable>(inDirtyVars);
        boolean bindingAllocated = inBindingAllocated;

        // If this is the exit BB, we need a binding story on exit only for vars that are both:
        //
        //   (a) dirty,
        //   (b) live on exit from the closure
        //       condition reqd. because the variable could be dirty but not used outside.
        //         Ex: s=0; a.each { |i| j = i+1; sum += j; }; puts sum
        //       i,j are dirty inside the block, but not used outside

        boolean amExitBB = basicBlock == s.cfg().getExitBB();
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
            LiveVariablesProblem lvp = (LiveVariablesProblem)s.getDataFlowSolution(DataFlowConstants.LVP_NAME);
            if (lvp != null) {
                java.util.Collection<Variable> liveVars = lvp.getVarsLiveOnExit();
                if (liveVars != null) {
                    dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
                } else {
                    dirtyVars.clear();
                }
            }
        }

        while (instrs.hasNext()) {
            Instr i = instrs.next();
            if (i.getOperation() == Operation.BINDING_LOAD) continue;

            if (i instanceof CallInstr) {
                CallInstr call = (CallInstr) i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    IRClosure scope = (IRClosure) ((MetaObject) o).scope;

                    BindingStorePlacementProblem cl_bsp = (BindingStorePlacementProblem) scope.getDataFlowSolution(bsp.getName());

                    instrs.previous();
                    if (addAllocateBindingInstructions) {
                       // Add a binding allocation instruction, if necessary
                       if (!bindingAllocated) {
                           instrs.add(new AllocateBindingInstr(s));
                           bindingAllocated = true;
                       }
                    }

                    // If the call is an eval, or if the callee can capture this method's binding,
                    // we have to spill all variables.
                    boolean spillAllVars = call.canBeEval() || call.targetRequiresCallersBinding();

                    // Unless we have to spill everything, spill only those dirty variables that are:
                    // - used in the closure (FIXME: Strictly only those vars that are live at the call site -- but we dont have this info!)
                    Set<LocalVariable> newDirtyVars = new HashSet<LocalVariable>(dirtyVars);
                    for (Variable v : dirtyVars) {
                        if (spillAllVars || cl_bsp.scopeUsesVariable(v)) {
                            // FIXME: This may not need check for local variable if it is guaranteed to only be local variables.
                            instrs.add(new StoreToBindingInstr(s, v.getName(), v));
                            newDirtyVars.remove(v);
                        } else if (cl_bsp.scopeDefinesVariable(v)) {
                            // These variables will be spilt inside the closure -- so they will no longer be dirty after the call site!
                            newDirtyVars.remove(v);
                        }
                    }
                    dirtyVars = newDirtyVars;
                    instrs.next();

                    // add stores in the closure
                    ((BindingStorePlacementProblem) scope.getDataFlowSolution(bsp.getName())).addStoreAndBindingAllocInstructions();
                } else if (call.targetRequiresCallersBinding()) { // Call has no closure && it requires stores
                    instrs.previous();
                    if (addAllocateBindingInstructions) {
                        if (!bindingAllocated) {
                            instrs.add(new AllocateBindingInstr(s));
                            bindingAllocated = true;
                        }
                    }
                    for (LocalVariable v : dirtyVars) {
                        instrs.add(new StoreToBindingInstr(s, v.getName(), v));
                    }
                    instrs.next();
                    dirtyVars.clear();
                } else if (call.canSetDollarVars()) {
                    if (addAllocateBindingInstructions) {
                        if (!bindingAllocated) {
                            instrs.add(new AllocateBindingInstr(s));
                            bindingAllocated = true;
                        }
                    }
                }

                // Add all the remaining dirty local vars into callsiteDirtyVars
                // These variables would have to be spilled into the binding if this
                // call raised an exception and exited this scope.
                if ((callsiteDirtyVars != null) && call.canRaiseException()) callsiteDirtyVars.addAll(dirtyVars);
            } else if ((i instanceof ClosureReturnInstr) || (i instanceof BreakInstr)) {
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
                    LiveVariablesProblem lvp = (LiveVariablesProblem)s.getDataFlowSolution(DataFlowConstants.LVP_NAME);
                    if (lvp != null) {
                        java.util.Collection<Variable> liveVars = lvp.getVarsLiveOnExit();
                        if (liveVars != null) {
                            dirtyVars.retainAll(liveVars); // Intersection with variables live on exit from the scope
                        } else {
                            dirtyVars.clear();
                        }
                    }
                }

                instrs.previous();
                addClosureExitBindingStores(s, instrs, dirtyVars);
                instrs.next();

                // Nothing is dirty anymore -- everything that needs spilling has been spilt
                dirtyVars.clear();
            }

            Variable v = i.getResult();
            // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
            if ((v != null) && (v instanceof LocalVariable) && !((LocalVariable)v).isSelf()) {
                dirtyVars.add((LocalVariable)v);
            }
        }

        // If this is the exit BB, add binding stores for all vars that are still dirty
        if (amExitBB) addClosureExitBindingStores(s, instrs, dirtyVars);
    }

    private void addClosureExitBindingStores(IRExecutionScope s, ListIterator<Instr> instrs, Set<LocalVariable> dirtyVars) {
        for (Variable v : dirtyVars) {
            if (v instanceof ClosureLocalVariable) {
                IRClosure definingScope = ((ClosureLocalVariable)v).definingScope;
                
                if ((s != definingScope) && s.nestedInClosure(definingScope)) {
                    instrs.add(new StoreToBindingInstr(s, v.getName(), v));
                }
            } else {
                instrs.add(new StoreToBindingInstr(s, v.getName(), v));
            }
        }
    }

    Set<LocalVariable> inDirtyVars;     // On entry to flow graph node:  Variables that need to be stored to the heap binding
    Set<LocalVariable> outDirtyVars;    // On exit from flow graph node: Variables that need to be stored to the heap binding
    boolean inBindingAllocated;   // Flag on entry to bb as to whether a binding has been allocated?
    boolean outBindingAllocated;   // Flag on exit to bb as to whether a binding has been allocated?
}
