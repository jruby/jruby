package org.jruby.ir.dataflow.analyses;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.BFalseInstr;
import org.jruby.ir.instructions.BTrueInstr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.OneOperandBranchInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.boxing.AluInstr;
import org.jruby.ir.instructions.boxing.BoxFloatInstr;
import org.jruby.ir.instructions.boxing.UnboxFloatInstr;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.util.Edge;

public class UnboxableOpsAnalysisNode extends FlowGraphNode {
    private class UnboxState {
        Set<Variable> unboxedVars;      // variables that exist in unboxed form
        Set<Variable> unboxedDirtyVars; // variables that exist in unboxed form and are dirty
        Map<Variable, Class> types;     // known types of variables

        public UnboxState() {
            types = new HashMap<Variable, Class>();
            unboxedVars = new HashSet<Variable>();
            unboxedDirtyVars = new HashSet<Variable>();
        }

        public UnboxState(UnboxState init) {
            types = new HashMap<Variable, Class>(init.types);
            unboxedVars = new HashSet<Variable>(init.unboxedVars);
            unboxedDirtyVars = new HashSet<Variable>(init.unboxedDirtyVars);
        }

        public void computeMEETForTypes(UnboxState other, boolean localVarsOnly) {
            Map<Variable, Class> otherTypes = other.types;
            for (Variable v: otherTypes.keySet()) {
                if (!localVarsOnly || v instanceof LocalVariable) {
                    Class c1 = types.get(v);
                    Class c2 = otherTypes.get(v);
                    if (c1 == null) {
                        types.put(v, c2);  // TOP --> class
                    } else if (c1 != c2) {
                        types.put(v, Object.class); // TOP/class --> BOTTOM
                    }
                }
            }
        }

        public void computeMEET(UnboxState other) {
            computeMEETForTypes(other, false);
            // Aggressive: Set union
            unboxedVars.addAll(other.unboxedVars);
            unboxedDirtyVars.addAll(other.unboxedDirtyVars);
        }

        public boolean equals(UnboxState other) {
            return types.equals(other.types) &&
                unboxedVars.equals(other.unboxedVars) &&
                unboxedDirtyVars.equals(other.unboxedDirtyVars);
        }

        public void debugOut() {
            System.out.print("-- Known types:");
            for (Variable v: types.keySet()) {
                if (types.get(v) != Object.class) {
                    System.out.println(v + "-->" + types.get(v));
                }
            }
            System.out.print("-- Unboxed vars:");
            for (Variable v: unboxedVars) {
                System.out.print(" " + v);
            }
            System.out.println("------");
            System.out.print("-- Unboxed dirty vars:");
            for (Variable v: unboxedDirtyVars) {
                System.out.print(" " + v);
            }
            System.out.println("------");
        }
    }

    public UnboxableOpsAnalysisNode(DataFlowProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        outState = new UnboxState();
    }

    public void buildDataFlowVars(Instr i) {
        // Nothing to do -- because we are going to simply use variables as our data flow variables
        // rather than build a new data flow type for it
    }

    public void applyPreMeetHandler() {
        IRScope s = problem.getScope();
        if (s instanceof IRClosure && basicBlock == s.getCFG().getEntryBB()) {
            // If it is not null, it has already been initialized
            if (inState == null) {
                inState = new UnboxState();
            }
        } else {
            inState = new UnboxState();
        }
    }

    public void compute_MEET(Edge e, BasicBlock source, FlowGraphNode pred) {
        // Ignore rescue entries -- everything is unboxed, as necessary.
        if (!source.isRescueEntry()) {
            inState.computeMEET(((UnboxableOpsAnalysisNode)pred).outState);
        }
    }

    private Class getOperandType(UnboxState state, Operand o) {
        if (o instanceof Float) {
            return Float.class;
        } else if (o instanceof Fixnum) {
            return Fixnum.class;
        } else if (o instanceof Variable) {
            return state.types.get((Variable)o);
        } else {
            return null;
        }
    }

    private void setOperandType(UnboxState state, Variable v, Class newType) {
        Class vType = state.types.get(v);
        if (newType == null) {
            state.types.remove(v);
        } else {
            state.types.put(v, newType);
        }
    }

    private void updateUnboxedVarsInfo(Instr i, UnboxState state, Variable dst, boolean hasRescuer, boolean isDFBarrier) {
        HashSet<Variable> varsToBox = new HashSet<Variable>();
        // Special treatment for instructions that can raise exceptions
        if (i.canRaiseException()) {
            if (hasRescuer) {
                // If we are going to be rescued,
                // box all unboxed dirty vars before we execute the instr.
                state.unboxedDirtyVars.clear();
            } else {
                // We are going to exit if an exception is raised.
                // So, only need to bother with dirty live local vars for closures
                if (this.problem.getScope() instanceof IRClosure) {
                    for (Variable v: state.unboxedDirtyVars) {
                        if (v instanceof LocalVariable) {
                            varsToBox.add(v);
                        }
                    }
                }
            }
        }

        if (isDFBarrier) {
            // All dirty unboxed local vars will get reboxed.
            for (Variable v: state.unboxedDirtyVars) {
                if (v instanceof LocalVariable) {
                    varsToBox.add(v);
                }
            }

            // We have to re-unbox local variables as necessary since we don't
            // know how they are going to change once we get past this instruction.
            List<Variable> lvs = new ArrayList<Variable>();
            for (Variable v: state.unboxedVars) {
                if (v instanceof LocalVariable) {
                    lvs.add(v);
                }
            }
            state.unboxedVars.removeAll(lvs);
        }

        // Update set of unboxed dirty vars
        state.unboxedDirtyVars.removeAll(varsToBox);

        // FIXME: Also global variables .. see LVA / StoreLocalVar analysis.

        // B_TRUE and B_FALSE have unboxed forms and their operands
        // needn't get boxed back.
        Operation op = i.getOperation();
        if (op != Operation.B_TRUE && op != Operation.B_FALSE) {
            // Vars used by this instruction that only exist in unboxed form
            // will have to get boxed before it is executed
            state.unboxedDirtyVars.removeAll(i.getUsedVariables());
        }

        // If the instruction writes into 'dst', it will be in boxed form.
        if (dst != null) {
            state.unboxedVars.remove(dst);
            state.unboxedDirtyVars.remove(dst);
        }
    }

    public void initSolution() {
        tmpState = new UnboxState(inState);
    }

    public void applyTransferFunction(Instr i) {
        // Rescue node, if any
        boolean scopeBindingHasEscaped = problem.getScope().bindingHasEscaped();

        Variable dst = null;
        boolean dirtied = false;
        boolean hitDFBarrier = false;
        if (i instanceof ResultInstr) {
            dst = ((ResultInstr)i).getResult();
        }

        if (i instanceof CopyInstr) {
            // Copies are easy
            Operand src = ((CopyInstr)i).getSource();
            Class srcType = getOperandType(tmpState, src);
            setOperandType(tmpState, dst, srcType);

            // If we have an unboxed type for 'src', we can leave this unboxed.
            //
            // FIXME: However, if 'src' is a constant, this could unnecessarily
            // leave 'src' unboxed and lead to a boxing instruction further down
            // at the use site of 'dst'. This indicates that leaving this unboxed
            // should ideally be done 'on-demand'. This indicates that this could
            // be a backward-flow algo OR that this algo should be run on a
            // dataflow graph / SSA graph.
            if (srcType == Float.class) {
                dirtied = true;
            }
        } else if (i instanceof CallBase) {
            // Process calls specially -- these are what we want to optimize!
            CallBase c = (CallBase)i;
            Operand  o = c.getClosureArg(null);
            if (o == null) {
                MethAddr m = c.getMethodAddr();
                Operand  r = c.getReceiver();
                Operand[] args = c.getCallArgs();
                if (args.length == 1 && m.resemblesALUOp()) {
                    Operand a = args[0];
                    Class receiverType = getOperandType(tmpState, r);
                    Class argType = getOperandType(tmpState, a);
                    // Optimistically assume that call is an ALU op
                    if (receiverType == Float.class ||
                        (receiverType == Fixnum.class && argType == Float.class))
                    {
                        setOperandType(tmpState, dst, Float.class);

                        // If 'r' and 'a' are not already in unboxed forms at this point,
                        // they will get unboxed after this, because we want to opt. this call
                        if (r instanceof Variable) {
                            tmpState.unboxedVars.add((Variable)r);
                        }
                        if (a instanceof Variable) {
                            tmpState.unboxedVars.add((Variable)a);
                        }
                        dirtied = true;
                    } else {
                        if (receiverType == Fixnum.class && argType == Fixnum.class) {
                            setOperandType(tmpState, dst, Fixnum.class);
                        } else {
                            setOperandType(tmpState, dst, Object.class);
                        }

                        if (c.targetRequiresCallersBinding()) {
                            hitDFBarrier = true;
                        }
                    }
                } else {
                    setOperandType(tmpState, dst, Object.class);
                }
            } else {
                if (o instanceof WrappedIRClosure) {
                    // Fetch the nested unboxing-analysis problem, creating one if necessary
                    IRClosure cl = ((WrappedIRClosure)o).getClosure();
                    UnboxableOpsAnalysisProblem subProblem = (UnboxableOpsAnalysisProblem)cl.getDataFlowSolution(DataFlowConstants.UNBOXING);
                    if (subProblem == null) {
                        subProblem = new UnboxableOpsAnalysisProblem();
                        subProblem.setup(cl);
                        cl.setDataFlowSolution(DataFlowConstants.UNBOXING, subProblem);
                    }

                    UnboxableOpsAnalysisNode exitNode  = (UnboxableOpsAnalysisNode)subProblem.getExitNode();
                    UnboxableOpsAnalysisNode entryNode = (UnboxableOpsAnalysisNode)subProblem.getEntryNode();

                    // Init it to MEET(state-on-entry, state-on-exit).
                    // The meet is required to account for participation of the closure in a loop.
                    // Ex: f = 0.0; n.times { f += 10; }
                    entryNode.inState = new UnboxState();
                    for (Variable v: tmpState.types.keySet()) {
                        if (v instanceof LocalVariable) {
                            entryNode.inState.types.put(v, tmpState.types.get(v));
                        }
                    }
                    entryNode.inState.computeMEET(exitNode.outState);

                    // Compute solution
                    subProblem.compute_MOP_Solution();

                    // Update types to MEET(new-state-on-exit, current-state)
                    tmpState.computeMEETForTypes(exitNode.outState, true);

                    // As for unboxed var state, since binding can escape in
                    // arbitrary ways in the general case, assume the worst for now.
                    // If we are guaranteed that the closure binding is not used
                    // outside the closure itself, we can avoid worst-case behavior
                    // and only clear vars that are modified in the closure.
                    hitDFBarrier = true;
                } else {
                    // Black hole -- cannot analyze
                    hitDFBarrier = true;
                }
            }
        } else {
            // We dont know how to optimize this instruction.
            // So, we assume we dont know type of the result.
            // TOP/class --> BOTTOM
            setOperandType(tmpState, dst, Object.class);
        }

        if (dirtied) {
            tmpState.unboxedVars.add(dst);
            tmpState.unboxedDirtyVars.add(dst);
        } else {
            // Since the instruction didn't run in unboxed form,
            // dirty unboxed vars will have to get boxed here.
            updateUnboxedVarsInfo(i, tmpState, dst, hasExceptionsRescued(), hitDFBarrier);
        }
    }

    public boolean solutionChanged() {
        return !tmpState.equals(outState);
    }

    public void finalizeSolution() {
        outState = tmpState;
    }

    private TemporaryVariable getUnboxedVar(Map<Variable, TemporaryVariable> unboxMap, Variable v, boolean createNew) {
        TemporaryVariable unboxedVar = unboxMap.get(v);
        if (unboxedVar == null && createNew) {
            unboxedVar = this.problem.getScope().getNewFloatVariable();
            unboxMap.put(v, unboxedVar);
        } else if (unboxedVar == null) {
            // FIXME: throw an exception here
            System.out.println("ERROR: No unboxed var for : " + v);
        }
        return unboxedVar;
    }

    private TemporaryVariable getUnboxedVar(Map<Variable, TemporaryVariable> unboxMap, Variable v) {
        return getUnboxedVar(unboxMap, v, true);
    }

    private Operand getUnboxedOperand(Set<Variable> unboxedVars, Map<Variable, TemporaryVariable> unboxMap, Operand arg, List<Instr> newInstrs, boolean unbox) {
        if (arg instanceof Variable) {
            Variable v = (Variable)arg;
            boolean isUnboxed = unboxedVars.contains(v);
            if (unbox) {
                // Get a temp var for 'v' if we dont already have one
                TemporaryVariable unboxedVar = getUnboxedVar(unboxMap, v);
                // Unbox if 'v' is not already unboxed
                if (!isUnboxed) {
                    // System.out.println("guo: UNBOXING for " + v);
                    newInstrs.add(new UnboxFloatInstr(unboxedVar, v));
                    unboxedVars.add(v);
                }

                return unboxedVar;
            } else {
                // Get a temp var for 'v' if we dont already have one
                // Else, don't unbox
                return isUnboxed ? getUnboxedVar(unboxMap, v) : arg;
            }
        } else {
            return arg;
        }
    }

    private Operand getUnboxedOperand(Set<Variable> unboxedVars, Map<Variable, TemporaryVariable> unboxMap, Operand arg, List<Instr> newInstrs) {
        return getUnboxedOperand(unboxedVars, unboxMap, arg, newInstrs, true);
    }

    private void boxRequiredVars(Instr i, UnboxState state, Map<Variable, TemporaryVariable> unboxMap, Variable dst, boolean hasRescuer, boolean isDFBarrier, List<Instr> newInstrs) {
        // Special treatment for instructions that can raise exceptions
        boolean isClosure = this.problem.getScope() instanceof IRClosure;
        HashSet<Variable> varsToBox = new HashSet<Variable>();
        if (i.canRaiseException()) {
            if (hasRescuer) {
                // If we are going to be rescued,
                // box all unboxed dirty vars before we execute the instr
                varsToBox.addAll(state.unboxedDirtyVars);
            } else if (isClosure) {
                // We are going to exit if an exception is raised.
                // So, only need to bother with dirty live local vars for closures
                for (Variable v: state.unboxedDirtyVars) {
                    if (v instanceof LocalVariable) {
                        varsToBox.add(v);
                    }
                }
            }
        }

        if (isClosure && (i instanceof ReturnInstr || i instanceof BreakInstr)) {
            for (Variable v: state.unboxedDirtyVars) {
                if (v instanceof LocalVariable) {
                    varsToBox.add(v);
                }
            }
        }

        if (isDFBarrier) {
            // All dirty unboxed (local) vars will get reboxed.
            for (Variable v: state.unboxedDirtyVars) {
                if (v instanceof LocalVariable) {
                    varsToBox.add(v);
                }
            }

            // We have to re-unbox local variables as necessary since we don't
            // know how they are going to change once we get past this instruction.
            List<Variable> lvs = new ArrayList<Variable>();
            for (Variable v: state.unboxedVars) {
                if (v instanceof LocalVariable) {
                    lvs.add(v);
                }
            }
            state.unboxedVars.removeAll(lvs);
        }

        // B_TRUE and B_FALSE have unboxed forms and their operands
        // needn't get boxed back.
        Operation op = i.getOperation();
        boolean isBranch = op == Operation.B_TRUE || op == Operation.B_FALSE;
        if (!isBranch) {
            // Vars used by this instruction that only exist in unboxed form
            // will have to get boxed before it is executed
            for (Variable v: i.getUsedVariables()) {
                if (state.unboxedDirtyVars.contains(v)) {
                    varsToBox.add(v);
                }
            }
        }

        // Add boxing instrs.
        for (Variable v: varsToBox) {
            newInstrs.add(new BoxFloatInstr(v, getUnboxedVar(unboxMap, v)));
            state.unboxedDirtyVars.remove(v);
        }

        // Add 'i' itself
        if (isBranch) {
            OneOperandBranchInstr bi = (OneOperandBranchInstr)i;
            Operand a = bi.getArg1();
            Operand ua = getUnboxedOperand(state.unboxedVars, unboxMap, a, newInstrs, false);
            if (ua == a) {
                newInstrs.add(i);
            } else if (op == Operation.B_TRUE) {
                newInstrs.add(new BTrueInstr(Operation.B_TRUE_UNBOXED, ua, bi.getJumpTarget()));
            } else {
                newInstrs.add(new BFalseInstr(Operation.B_FALSE_UNBOXED, ua, bi.getJumpTarget()));
            }
        } else {
            newInstrs.add(i);
        }

        // If the instruction writes into 'dst', it will be in boxed form.
        if (dst != null) {
            state.unboxedVars.remove(dst);
            state.unboxedDirtyVars.remove(dst);
        }
    }


    public void unbox(Map<Variable, TemporaryVariable> unboxMap) {
        // System.out.println("BB : " + basicBlock + " in " + this.problem.getScope().getName());
        // System.out.println("-- known types on entry:");
        // for (Variable v: inState.types.keySet()) {
        //     if (inState.types.get(v) != Object.class) {
        //         System.out.println(v + "-->" + inState.types.get(v));
        //     }
        // }
        // System.out.print("-- unboxed vars on entry:");
        // for (Variable v: inState.unboxedVars) {
        //     System.out.print(" " + v);
        // }
        // System.out.println("------");
        // System.out.print("-- unboxed vars on exit:");
        // for (Variable v: outState.unboxedVars) {
        //     System.out.print(" " + v);
        // }
        // System.out.println("------");

        // Compute UNION(unboxedVarsIn(all-successors)) - this.unboxedVarsOut
        // All vars in this new set have to be unboxed on exit from this BB
        boolean scopeBindingHasEscaped = problem.getScope().bindingHasEscaped();
        Set<Variable> succUnboxedVars = new HashSet<Variable>();
        CFG cfg = problem.getScope().cfg();

        for (Edge e: cfg.getOutgoingEdges(basicBlock)) {
            BasicBlock b = (BasicBlock)e.getDestination().getData();
            if (b != cfg.getExitBB()) {
                UnboxableOpsAnalysisNode x = (UnboxableOpsAnalysisNode)problem.getFlowGraphNode(b);
                succUnboxedVars.addAll(x.inState.unboxedVars);
            }
        }

        succUnboxedVars.removeAll(outState.unboxedVars);

        // Only worry about vars live on exit from the BB
        LiveVariablesProblem lvp = (LiveVariablesProblem)problem.getScope().getDataFlowSolution(DataFlowConstants.LVP_NAME);
        BitSet liveVarsSet = ((LiveVariableNode)lvp.getFlowGraphNode(basicBlock)).getLiveInBitSet();

        // Rescue node, if any
        IRScope scope = this.problem.getScope();
        boolean isClosure = scope instanceof IRClosure;

        List<Instr> newInstrs = new ArrayList<Instr>();
        boolean unboxedLiveVars = false;

        initSolution();

        for (Instr i : basicBlock.getInstrs()) {
            Variable dst = null;
            boolean dirtied = false;
            boolean hitDFBarrier = false;
            // System.out.println("ORIG: " + i);
            if (i.getOperation().transfersControl()) {
                // Add unboxing instrs.
                for (Variable v: succUnboxedVars) {
                    if (liveVarsSet.get(lvp.getDFVar(v).getId())) {
                        // System.out.println("suv: UNBOXING for " + v);
                        newInstrs.add(new UnboxFloatInstr(getUnboxedVar(unboxMap, v), v));
                        tmpState.unboxedVars.add(v);
                    }
                }
                unboxedLiveVars = true;
            } else {
                if (i instanceof ResultInstr) {
                    dst = ((ResultInstr) i).getResult();
                }

                if (i instanceof CopyInstr) {
                    // Copies are easy
                    Operand src = ((CopyInstr)i).getSource();
                    Class srcType = getOperandType(tmpState, src);
                    setOperandType(tmpState, dst, srcType);

                    // If we have an unboxed type for 'src', we can leave this unboxed.
                    //
                    // FIXME: However, if 'src' is a constant, this could unnecessarily
                    // leave 'src' unboxed and lead to a boxing instruction further down
                    // at the use site of 'dst'. This indicates that leaving this unboxed
                    // should ideally be done 'on-demand'. This indicates that this could
                    // be a backward-flow algo OR that this algo should be run on a
                    // dataflow graph / SSA graph.
                    if (srcType == Float.class) {
                        Operand unboxedSrc = src instanceof Variable ? getUnboxedVar(unboxMap, (Variable)src) : src;
                        TemporaryVariable unboxedDst = getUnboxedVar(unboxMap, dst);
                        newInstrs.add(new CopyInstr(Operation.COPY_UNBOXED, unboxedDst, unboxedSrc));
                        dirtied = true;
                    }
                } else if (i instanceof CallBase) {
                    // Process calls specially -- these are what we want to optimize!
                    CallBase c = (CallBase)i;
                    Operand  o = c.getClosureArg(null);
                    if (o == null) {
                        MethAddr m = c.getMethodAddr();
                        Operand  r = c.getReceiver();
                        Operand[] args = c.getCallArgs();
                        if (args.length == 1 && m.resemblesALUOp()) {
                            Operand a = args[0];
                            Class receiverType = getOperandType(tmpState, r);
                            Class argType = getOperandType(tmpState, a);
                            // Optimistically assume that call is an ALU op
                            if (receiverType == Float.class ||
                                (receiverType == Fixnum.class && argType == Float.class))
                            {
                                setOperandType(tmpState, dst, Float.class);
                                r = getUnboxedOperand(tmpState.unboxedVars, unboxMap, r, newInstrs);
                                a = getUnboxedOperand(tmpState.unboxedVars, unboxMap, a, newInstrs);
                                TemporaryVariable unboxedDst = getUnboxedVar(unboxMap, dst);
                                newInstrs.add(new AluInstr(m.getUnboxedOp(Float.class), unboxedDst, r, a));
                                dirtied = true;
                            } else {
                                if (receiverType == Fixnum.class && argType == Fixnum.class) {
                                    setOperandType(tmpState, dst, Fixnum.class);
                                } else {
                                    setOperandType(tmpState, dst, Object.class);
                                }

                                if (c.targetRequiresCallersBinding()) {
                                    hitDFBarrier = true;
                                }
                            }
                        } else {
                            setOperandType(tmpState, dst, Object.class);
                        }
                    } else {
                        if (o instanceof WrappedIRClosure) {
                            // Since binding can escape in arbitrary ways in the general case,
                            // assume the worst for now. If we are guaranteed that the closure binding
                            // is not used outside the closure itself, we can avoid worst-case behavior.
                            hitDFBarrier = true;

                            // Fetch the nested unboxing-analysis problem, creating one if necessary
                            IRClosure cl = ((WrappedIRClosure)o).getClosure();
                            UnboxableOpsAnalysisProblem subProblem = (UnboxableOpsAnalysisProblem)cl.getDataFlowSolution(DataFlowConstants.UNBOXING);
                            UnboxableOpsAnalysisNode exitNode  = (UnboxableOpsAnalysisNode)subProblem.getExitNode();

                            // Compute solution
                            subProblem.unbox();

                            // Update types to MEET(new-state-on-exit, current-state)
                            tmpState.computeMEETForTypes(exitNode.outState, true);

                            // As for unboxed var state, since binding can escape in
                            // arbitrary ways in the general case, assume the worst for now.
                            // If we are guaranteed that the closure binding is not used
                            // outside the closure itself, we can avoid worst-case behavior
                            // and only clear vars that are modified in the closure.
                            hitDFBarrier = true;
                        } else {
                            // Cannot analyze
                            hitDFBarrier = true;
                        }
                    }
                } else {
                    // We dont know how to optimize this instruction.
                    // So, we assume we dont know type of the result.
                    // TOP/class --> BOTTOM
                    setOperandType(tmpState, dst, Object.class);
                }
            }

            if (dirtied) {
                tmpState.unboxedVars.add(dst);
                tmpState.unboxedDirtyVars.add(dst);
            } else {
                // Since the instruction didn't run in unboxed form,
                // dirty unboxed vars will have to get boxed here.
                boxRequiredVars(i, tmpState, unboxMap, dst, hasExceptionsRescued(), hitDFBarrier, newInstrs);
            }
        }

        // Add unboxing instrs.
        if (!unboxedLiveVars) {
            for (Variable v: succUnboxedVars) {
                if (liveVarsSet.get(lvp.getDFVar(v).getId())) {
                    // System.out.println("suv: UNBOXING for " + v);
                    newInstrs.add(new UnboxFloatInstr(getUnboxedVar(unboxMap, v), v));
                }
            }
        }

/*
        System.out.println("------");
        for (Instr i : newInstrs) {
            System.out.println("NEW: " + i);
        }
*/

        basicBlock.replaceInstrs(newInstrs);
    }

    @Override
    public String toString() {
        return "";
    }

    UnboxState inState;
    UnboxState outState;
    UnboxState tmpState;
}
