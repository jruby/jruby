package org.jruby.compiler.ir.dataflow.analyses;

import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.compiler.ir.dataflow.FlowGraphNode;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.LoadFromBindingInstr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.ClosureLocalVariable;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.BasicBlock;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class BindingLoadPlacementNode extends FlowGraphNode {
    public BindingLoadPlacementNode(DataFlowProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        inRequiredLoads = new HashSet<LocalVariable>();
        outRequiredLoads = new HashSet<LocalVariable>();
    }

    public void buildDataFlowVars(Instr i) {
        // Nothing to do -- because we are going to simply use non-closure, non-self, non-block LocalVariables as our data flow variables
        // rather than build a new data flow type for it
    }

    public void initSolnForNode() {
        if (basicBlock == problem.getScope().cfg().getExitBB()) {
            inRequiredLoads = ((BindingLoadPlacementProblem) problem).getLoadsOnScopeExit();
        }
    }

    public void compute_MEET(BasicBlock source, FlowGraphNode pred) {
        BindingLoadPlacementNode n = (BindingLoadPlacementNode) pred;
        inRequiredLoads.addAll(n.outRequiredLoads);
    }

    public boolean applyTransferFunction() {
        BindingLoadPlacementProblem blp = (BindingLoadPlacementProblem) problem;
        Set<LocalVariable> reqdLoads = new HashSet<LocalVariable>(inRequiredLoads);

        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        while (it.hasPrevious()) {
            Instr i = it.previous();
            //System.out.println("-----\nInstr " + i);
            //System.out.println("Before: " + java.util.Arrays.toString(reqdLoads.toArray()));

            if (i.getOperation() == Operation.BINDING_STORE) continue;

            // Right away, clear the variable defined by this instruction -- it doesn't have to be loaded!
            Variable r = i.getResult();

            if (r != null) reqdLoads.remove(r);

            // Process calls specially -- these are the sites of binding loads!
            if (i instanceof CallInstr) {
                CallInstr call = (CallInstr) i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    IRClosure cl = (IRClosure) ((MetaObject) o).scope;
                    BindingLoadPlacementProblem cl_blp = new BindingLoadPlacementProblem();
                    cl_blp.initLoadsOnScopeExit(reqdLoads);
                    cl_blp.setup(cl);
                    cl_blp.compute_MOP_Solution();
                    cl.setDataFlowSolution(cl_blp.getName(), cl_blp);

                    // Variables defined in the closure do not need to be loaded anymore at
                    // program points before the call.
                    Set<LocalVariable> newReqdLoads = new HashSet<LocalVariable>(reqdLoads);
                    for (LocalVariable v : reqdLoads) {
                        if (cl_blp.scopeDefinesVariable(v)) {
                           newReqdLoads.remove((LocalVariable)v);
                        }
                    }
                    reqdLoads = newReqdLoads;
                }
                // In this case, we are going to blindly load everything -- so, at the call site, pending loads dont carry over!
                if (call.targetRequiresCallersBinding()) {
                    reqdLoads.clear();
                }
            }

            // The variables used as arguments will need to be loaded
            // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
            for (Variable x : i.getUsedVariables()) {
                if ((x instanceof LocalVariable) && !((LocalVariable)x).isSelf()) {
                    reqdLoads.add((LocalVariable)x);
                }
            }
            //System.out.println("After: " + java.util.Arrays.toString(reqdLoads.toArray()));
        }

        // At the beginning of the scope, required loads can be discarded.
        if (basicBlock == problem.getScope().cfg().getEntryBB()) reqdLoads.clear();

        if (outRequiredLoads.equals(reqdLoads)) {
            //System.out.println("\n For CFG " + _prob.getCFG() + " BB " + _bb.getID());
            //System.out.println("\t--> IN reqd loads   : " + java.util.Arrays.toString(_inReqdLoads.toArray()));
            //System.out.println("\t--> OUT reqd loads  : " + java.util.Arrays.toString(_outReqdLoads.toArray()));
            return false;
        } else {
            outRequiredLoads = reqdLoads;
            return true;
        }
    }

    @Override
    public String toString() {
        return "";
    }

    public void addLoads() {
        BindingLoadPlacementProblem blp = (BindingLoadPlacementProblem) problem;
        IRExecutionScope s = blp.getScope().cfg().getScope();
        List<Instr> instrs = basicBlock.getInstrs();
        ListIterator<Instr> it = instrs.listIterator(instrs.size());
        Set<LocalVariable> reqdLoads = new HashSet<LocalVariable>(inRequiredLoads);
        while (it.hasPrevious()) {
            Instr i = it.previous();

            if (i.getOperation() == Operation.BINDING_STORE) continue;

            // Right away, clear the variable defined by this instruction -- it doesn't have to be loaded!
            Variable r = i.getResult();

            if (r != null) reqdLoads.remove(r);

            if (i instanceof CallInstr) {
                CallInstr call = (CallInstr) i;
                Operand o = call.getClosureArg();
                if ((o != null) && (o instanceof MetaObject)) {
                    IRClosure scope = (IRClosure) ((MetaObject) o).scope;
                    BindingLoadPlacementProblem cl_blp = (BindingLoadPlacementProblem) scope.getDataFlowSolution(blp.getName());

                    // Only those variables that are defined in the closure, and are in the required loads set 
                    // will need to be loaded from the binding after the call!  Rest can wait ..
                    Set<LocalVariable> newReqdLoads = new HashSet<LocalVariable>(reqdLoads);
                    it.next();
                    for (LocalVariable v : reqdLoads) {
                        if (cl_blp.scopeDefinesVariable(v)) {
                            it.add(new LoadFromBindingInstr(v, s, v.getName()));
                            it.previous();
                            newReqdLoads.remove(v);
                        }
                    }
                    it.previous();
                    reqdLoads = newReqdLoads;

                    // add loads in the closure
                    ((BindingLoadPlacementProblem) scope.getDataFlowSolution(blp.getName())).addLoads();
                } 

                // In this case, we are going to blindly load everything
                if (call.targetRequiresCallersBinding()) {
                    it.next();
                    for (LocalVariable v : reqdLoads) {
                        it.add(new LoadFromBindingInstr(v, s, v.getName()));
                        it.previous();
                    }
                    it.previous();
                    reqdLoads.clear();
                }
            }

            // The variables used as arguments will need to be loaded
            // %self is local to every scope and never crosses scope boundaries and need not be spilled/refilled
            for (Variable x : i.getUsedVariables()) {
                if ((x instanceof LocalVariable) && !((LocalVariable)x).isSelf()) {
                    reqdLoads.add((LocalVariable)x);
                }
            }
        }

        // Load first use of variables in closures
        if ((s instanceof IRClosure) && (basicBlock == problem.getScope().cfg().getEntryBB())) {
            // System.out.println("\n[In Entry BB] For CFG " + _prob.getCFG() + ":");
            // System.out.println("\t--> Reqd loads   : " + java.util.Arrays.toString(reqdLoads.toArray()));
            for (LocalVariable v : reqdLoads) {
                if (blp.scopeUsesVariable(v)) {
                    if (v instanceof ClosureLocalVariable) {
                        IRClosure definingScope = ((ClosureLocalVariable)v).definingScope;
                        
                        if ((s != definingScope) && s.nestedInClosure(definingScope)) {
                            it.add(new LoadFromBindingInstr(v, s, v.getName()));
                        }
                    } else {
                        it.add(new LoadFromBindingInstr(v, s, v.getName()));
                    }
                }
            }
        }
    }

    // On entry to flow graph node:  Variables that need to be loaded from the heap binding
    Set<LocalVariable> inRequiredLoads;
    // On exit from flow graph node: Variables that need to be loaded from the heap binding
    Set<LocalVariable> outRequiredLoads;    
}
