package org.jruby.ir.dataflow.analyses;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.ir.IRClosure;
import org.jruby.ir.Operation;
import org.jruby.ir.dataflow.DataFlowConstants;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.BFalseInstr;
import org.jruby.ir.instructions.BTrueInstr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.OneOperandBranchInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.boxing.AluInstr;
import org.jruby.ir.instructions.boxing.BoxBooleanInstr;
import org.jruby.ir.instructions.boxing.BoxFixnumInstr;
import org.jruby.ir.instructions.boxing.BoxFloatInstr;
import org.jruby.ir.instructions.boxing.UnboxBooleanInstr;
import org.jruby.ir.instructions.boxing.UnboxFixnumInstr;
import org.jruby.ir.instructions.boxing.UnboxFloatInstr;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Float;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.util.Edge;

public class UnboxableOpsAnalysisNode extends FlowGraphNode<UnboxableOpsAnalysisProblem, UnboxableOpsAnalysisNode> {
    private class UnboxState {
        Map<Variable, Class> types;        // known types of variables
        Map<Variable, Class> unboxedVars;  // variables that exist in unboxed form of a specific type
        Set<Variable> unboxedDirtyVars;    // variables that exist in unboxed form and are dirty

        public UnboxState() {
            types = new HashMap<Variable, Class>();
            unboxedVars = new HashMap<Variable, Class>();
            unboxedDirtyVars = new HashSet<Variable>();
        }

        public UnboxState(UnboxState init) {
            types = new HashMap<Variable, Class>(init.types);
            unboxedVars = new HashMap<Variable, Class>(init.unboxedVars);
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

        public void computeMEETForUnboxedVars(UnboxState other) {
            // * If the var is available in unboxed form along only one path,
            //   it is assumed AVAILABLE unboxed on MEET.
            // * If the var is availble in unboxed forms with different types along
            //   each path, it is assumed UNAVAILABLE unboxed on MEET.
            Map<Variable, Class> otherVars = other.unboxedVars;
            for (Variable v: otherVars.keySet()) {
                Class c1 = unboxedVars.get(v);
                Class c2 = otherVars.get(v);
                if (c1 == null) {
                    unboxedVars.put(v, c2);
                } else if (c1 != c2) {
                    //
                    unboxedVars.remove(v);
                }
            }
        }

        public void computeMEET(UnboxState other) {
            computeMEETForTypes(other, false);
            computeMEETForUnboxedVars(other);
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
            for (Variable v: unboxedVars.keySet()) {
                System.out.print(" " + v + "-->" + unboxedVars.get(v));
            }
            System.out.println("------");
            System.out.print("-- Unboxed dirty vars:");
            for (Variable v: unboxedDirtyVars) {
                System.out.print(" " + v);
            }
            System.out.println("------");
        }
    }

    public UnboxableOpsAnalysisNode(UnboxableOpsAnalysisProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        outState = new UnboxState();
    }

    @Override
    public void buildDataFlowVars(Instr i) {
        // Nothing to do -- because we are going to simply use variables as our data flow variables
        // rather than build a new data flow type for it
    }

    @Override
    public void applyPreMeetHandler() {
        if (problem.getScope() instanceof IRClosure && basicBlock.isEntryBB()) {
            // If it is not null, it has already been initialized
            if (inState == null) {
                inState = new UnboxState();
            }
        } else {
            inState = new UnboxState();
        }
    }

    @Override
    public void compute_MEET(Edge e, UnboxableOpsAnalysisNode pred) {
        // Ignore rescue entries -- everything is unboxed, as necessary.
        if (!pred.basicBlock.isRescueEntry()) inState.computeMEET(pred.outState);
    }

    private Class getOperandType(UnboxState state, Operand o) {
        if (o instanceof Float) {
            return Float.class;
        } else if (o instanceof Fixnum) {
            return Fixnum.class;
        } else if (o instanceof Bignum) {
            return Bignum.class;
        } else if (o instanceof Variable) {
            return state.types.get((Variable) o);
        } else {
            return null;
        }
    }

    private void setOperandType(UnboxState state, Variable v, Class newType) {
        if (v != null && newType != null) {
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
                if (problem.getScope() instanceof IRClosure) {
                    markLocalVariables(varsToBox, state.unboxedDirtyVars);
                }
            }
        }

        if (isDFBarrier) {
            // All dirty unboxed local vars will get reboxed.
            markLocalVariables(varsToBox, state.unboxedDirtyVars);

            // We have to re-unbox local variables as necessary since we don't
            // know how they are going to change once we get past this instruction.
            List<Variable> lvs = new ArrayList<Variable>();
            markLocalVariables(lvs, state.unboxedVars.keySet());

            state.unboxedVars.keySet().removeAll(lvs);
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

    @Override
    public void initSolution() {
        tmpState = new UnboxState(inState);
    }

    @Override
    public void applyTransferFunction(Instr i) {

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
                tmpState.unboxedVars.put(dst, srcType);
            } else if (srcType == Fixnum.class) {
                dirtied = true;
                tmpState.unboxedVars.put(dst, srcType);
            }
        } else if (i instanceof ClosureAcceptingInstr) {
            Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
            // Process calls specially -- these are what we want to optimize!
            if (i instanceof CallBase && o == null) {
                CallBase c = (CallBase)i;
                MethAddr m = c.getMethodAddr();
                Operand  r = c.getReceiver();
                Operand[] args = c.getCallArgs();
                if (dst != null && args.length == 1 && m.resemblesALUOp()) {
                    Operand a = args[0];
                    Class receiverType = getOperandType(tmpState, r);
                    Class argType = getOperandType(tmpState, a);
                    // Optimistically assume that call is an ALU op
                    if (receiverType == Float.class ||
                        (receiverType == Fixnum.class && argType == Float.class))
                    {
                        dirtied = true;

                        Class dstType = m.getUnboxedResultType(Float.class);
                        setOperandType(tmpState, dst, dstType);
                        tmpState.unboxedVars.put(dst, dstType);

                        // If 'r' and 'a' are not already in unboxed forms at this point,
                        // they will get unboxed after this, because we want to opt. this call
                        if (r instanceof Variable) {
                            tmpState.unboxedVars.put((Variable)r, Float.class);
                        }
                        if (a instanceof Variable) {
                            tmpState.unboxedVars.put((Variable)a, Float.class);
                        }
                    } else if (receiverType == Float.class ||
                            (receiverType == Fixnum.class && argType == Fixnum.class))
                    {
                        dirtied = true;

                        Class dstType = m.getUnboxedResultType(Fixnum.class);
                        setOperandType(tmpState, dst, dstType);
                        tmpState.unboxedVars.put(dst, dstType);

                        // If 'r' and 'a' are not already in unboxed forms at this point,
                        // they will get unboxed after this, because we want to opt. this call
                        if (r instanceof Variable) {
                            tmpState.unboxedVars.put((Variable)r, Fixnum.class);
                        }
                        if (a instanceof Variable) {
                            tmpState.unboxedVars.put((Variable)a, Fixnum.class);
                        }
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

                    UnboxableOpsAnalysisNode exitNode  = subProblem.getExitNode();
                    UnboxableOpsAnalysisNode entryNode = subProblem.getEntryNode();

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
            tmpState.unboxedDirtyVars.add(dst);
        } else {
            // Since the instruction didn't run in unboxed form,
            // dirty unboxed vars will have to get boxed here.
            updateUnboxedVarsInfo(i, tmpState, dst, hasExceptionsRescued(), hitDFBarrier);
        }
    }

    @Override
    public boolean solutionChanged() {
        return !tmpState.equals(outState);
    }

    @Override
    public void finalizeSolution() {
        outState = tmpState;
    }

    private boolean matchingTypes(Class c, TemporaryVariableType t) {
        switch (t) {
        case FLOAT: return c == Float.class;
        case FIXNUM: return c == Fixnum.class;
        case BOOLEAN: return c == UnboxedBoolean.class;
        default: return c != Float.class && c != Boolean.class && c != Fixnum.class;
        }
    }

    private TemporaryLocalVariable getUnboxedVar(Class reqdType, Map<Variable, TemporaryLocalVariable> unboxMap, Variable v, boolean createNew) {
        TemporaryLocalVariable unboxedVar = unboxMap.get(v);
        // FIXME: This is a bit broken -- SSA will eliminate this need for type verification
        if ((unboxedVar == null && createNew) || !matchingTypes(reqdType, unboxedVar.getType())) {
            unboxedVar = problem.getScope().getNewUnboxedVariable(reqdType);
            unboxMap.put(v, unboxedVar);
        } else if (unboxedVar == null) {
            // FIXME: throw an exception here
            System.out.println("ERROR: No unboxed var for : " + v);
        }
        return unboxedVar;
    }

    private TemporaryLocalVariable getUnboxedVar(Class reqdType, Map<Variable, TemporaryLocalVariable> unboxMap, Variable v) {
        return getUnboxedVar(reqdType, unboxMap, v, true);
    }

    public void boxVar(UnboxState state, Class reqdType, Map<Variable, TemporaryLocalVariable> unboxMap, Variable v, List<Instr> newInstrs) {
        TemporaryLocalVariable unboxedV = getUnboxedVar(reqdType, unboxMap, v);
        TemporaryVariableType vType = unboxedV.getType();
        if (vType == TemporaryVariableType.BOOLEAN) {
            // boolean literals are lightweight enough that they dont need unboxed variants.
            newInstrs.add(new BoxBooleanInstr(v, unboxedV));
        } else if (vType == TemporaryVariableType.FLOAT) { // SSS FIXME: This is broken
            newInstrs.add(new BoxFloatInstr(v, unboxedV));
        } else if (vType == TemporaryVariableType.FIXNUM) { // CON FIXME: So this is probably broken too
            newInstrs.add(new BoxFixnumInstr(v, unboxedV));
        }
        state.unboxedDirtyVars.remove(v);
        // System.out.println("BOXING for " + v);
    }

    public void unboxVar(UnboxState state, Class reqdType, Map<Variable, TemporaryLocalVariable> unboxMap, Variable v, List<Instr> newInstrs) {
        Variable unboxedV = getUnboxedVar(reqdType, unboxMap, v);
        if (reqdType == UnboxedBoolean.class) {
            // boolean literals are lightweight enough that they dont need unboxed variants.
            newInstrs.add(new UnboxBooleanInstr(unboxedV, v));
        } else if (reqdType == Float.class) { // SSS FIXME: This is broken
            newInstrs.add(new UnboxFloatInstr(unboxedV, v));
        } else if (reqdType == Fixnum.class) { // CON FIXME: So this is probably broken too
            newInstrs.add(new UnboxFixnumInstr(unboxedV, v));
        }
        state.unboxedVars.put(v, reqdType);
        // System.out.println("UNBOXING for " + v + " with type " + vType);
    }

    private Operand unboxOperand(UnboxState state, Class reqdType, Map<Variable, TemporaryLocalVariable> unboxMap, Operand arg, List<Instr> newInstrs) {
        if (arg instanceof Variable) {
            Variable v = (Variable)arg;
            boolean isUnboxed = state.unboxedVars.get(v) == reqdType;
            // Get a temp var for 'v' if we dont already have one
            TemporaryLocalVariable unboxedVar = getUnboxedVar(reqdType, unboxMap, v);
            // Unbox if 'v' is not already unboxed
            if (!isUnboxed) {
                unboxVar(state, reqdType, unboxMap, v, newInstrs);
            }

            return unboxedVar;
        } else {
            if (arg instanceof Float) {
                return new UnboxedFloat(((Float)arg).getValue());
            } else if (arg instanceof Fixnum) {
                return new UnboxedFixnum(((Fixnum)arg).getValue());
            } else if (arg instanceof org.jruby.ir.operands.Boolean) {
                return new UnboxedBoolean(((Boolean)arg).isTrue());
            }
            // This has to be a known operand like (UnboxedBoolean, etc.)
            return arg;
        }
    }

    private Operand getUnboxedOperand(UnboxState state, Map<Variable, TemporaryLocalVariable> unboxMap, Operand arg) {
        if (arg instanceof Variable) {
            Variable v = (Variable)arg;
            Class unboxedType = state.unboxedVars.get(v);
            return unboxedType == null ? arg : getUnboxedVar(unboxedType, unboxMap, v);
        } else {
            if (arg instanceof Float) {
                return new UnboxedFloat(((Float)arg).getValue());
            } else if (arg instanceof Fixnum) {
                return new UnboxedFixnum(((Fixnum)arg).getValue());
            } else if (arg instanceof Boolean) {
                return new UnboxedBoolean(((Boolean)arg).isTrue());
            }
            return arg;
        }
    }

    private void markLocalVariables(Collection<Variable> varsToBox, Set<Variable> varsToCheck) {
        for (Variable v: varsToCheck) {
            if (v instanceof LocalVariable) varsToBox.add(v);
        }
    }

    private void boxRequiredVars(Instr i, UnboxState state, Map<Variable, TemporaryLocalVariable> unboxMap, Variable dst, boolean hasRescuer, boolean isDFBarrier, List<Instr> newInstrs) {
        // Special treatment for instructions that can raise exceptions
        boolean isClosure = problem.getScope() instanceof IRClosure;
        HashSet<Variable> varsToBox = new HashSet<Variable>();
        if (i.canRaiseException()) {
            if (hasRescuer) {
                // If we are going to be rescued,
                // box all unboxed dirty vars before we execute the instr
                varsToBox.addAll(state.unboxedDirtyVars);
            } else if (isClosure) {
                // We are going to exit if an exception is raised.
                // So, only need to bother with dirty live local vars for closures
                markLocalVariables(varsToBox, state.unboxedDirtyVars);
            }
        }

        if (isClosure && (i instanceof ReturnInstr || i instanceof BreakInstr)) {
            markLocalVariables(varsToBox, state.unboxedDirtyVars);
        }

        if (isDFBarrier) {
            // All dirty unboxed (local) vars will get reboxed.
            markLocalVariables(varsToBox, state.unboxedDirtyVars);

            // We have to re-unbox local variables as necessary since we don't
            // know how they are going to change once we get past this instruction.
            List<Variable> lvs = new ArrayList<Variable>();
            markLocalVariables(lvs, state.unboxedVars.keySet());

            state.unboxedVars.keySet().removeAll(lvs);
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
            boxVar(state, state.unboxedVars.get(v), unboxMap, v, newInstrs);
        }

        // Add 'i' itself
        if (isBranch) {
            OneOperandBranchInstr bi = (OneOperandBranchInstr)i;
            Operand a = bi.getArg1();
            Operand ua = getUnboxedOperand(state, unboxMap, a);
            if (ua == a) {
                newInstrs.add(i);
            } else if (op == Operation.B_TRUE) {
                newInstrs.add(new BTrueInstr(Operation.B_TRUE, ua, bi.getJumpTarget()));
            } else {
                newInstrs.add(new BFalseInstr(Operation.B_FALSE, ua, bi.getJumpTarget()));
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

    public void unbox(Map<Variable, TemporaryLocalVariable> unboxMap) {
        // System.out.println("BB : " + basicBlock + " in " + problem.getScope().getName());
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

        CFG cfg = getCFG();

        // Compute UNION(unboxedVarsIn(all-successors)) - this.unboxedVarsOut
        // All vars in this new set have to be unboxed on exit from this BB
        HashMap<Variable, Class> succUnboxedVars = new HashMap<Variable, Class>();
        for (BasicBlock b: cfg.getOutgoingDestinations(basicBlock)) {
            if (b.isExitBB()) continue;

            Map<Variable, Class> xVars = problem.getFlowGraphNode(b).inState.unboxedVars;
            for (Variable v2: xVars.keySet()) {
                // VERY IMPORTANT: Pay attention!
                //
                // Technically, the successors of this node may not all agree on what
                // the unboxed type ought to be for 'v2'. For example, one successor might
                // want 'v2' in Fixnum form and other might want it in Float form. If that
                // happens, we have to add unboxing instructions for each of those expected
                // types. However, for now, we are going to punt and assume that our successors
                // agree on unboxed types for 'v2'.
                succUnboxedVars.put(v2, xVars.get(v2));
            }
        }

        // Same caveat as above applies here
        for (Variable v3: outState.unboxedVars.keySet()) {
            succUnboxedVars.remove(v3);
        }

        // Only worry about vars live on exit from the BB
        LiveVariablesProblem lvp = (LiveVariablesProblem)problem.getScope().getDataFlowSolution(DataFlowConstants.LVP_NAME);
        BitSet liveVarsSet = lvp.getFlowGraphNode(basicBlock).getLiveInBitSet();

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
                for (Variable v: succUnboxedVars.keySet()) {
                    if (liveVarsSet.get(lvp.getDFVar(v))) {
                        unboxVar(tmpState, succUnboxedVars.get(v), unboxMap, v, newInstrs);
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
                    if (srcType == Float.class || srcType == Fixnum.class) {
                        Operand unboxedSrc = src instanceof Variable ? getUnboxedVar(srcType, unboxMap, (Variable)src) : src;
                        TemporaryLocalVariable unboxedDst = getUnboxedVar(srcType, unboxMap, dst);
                        newInstrs.add(new CopyInstr(Operation.COPY, unboxedDst, unboxedSrc));
                        tmpState.unboxedVars.put(dst, srcType);
                        dirtied = true;
                    }
                } else if (i instanceof ClosureAcceptingInstr) {
                    Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
                    if (i instanceof CallBase && o == null) {
                        CallBase c = (CallBase)i;
                        MethAddr m = c.getMethodAddr();
                        Operand  r = c.getReceiver();
                        Operand[] args = c.getCallArgs();
                        if (dst != null && args.length == 1 && m.resemblesALUOp()) {
                            Operand a = args[0];
                            Class receiverType = getOperandType(tmpState, r);
                            Class argType = getOperandType(tmpState, a);
                            // Optimistically assume that call is an ALU op
                            Operation unboxedOp;
                            if ((receiverType == Float.class || (receiverType == Fixnum.class && argType == Float.class)) &&
                                (unboxedOp = m.getUnboxedOp(Float.class)) != null)
                            {
                                dirtied = true;

                                Class dstType = m.getUnboxedResultType(Float.class);
                                setOperandType(tmpState, dst, dstType);
                                tmpState.unboxedVars.put(dst, dstType);

                                TemporaryLocalVariable unboxedDst = getUnboxedVar(dstType, unboxMap, dst);
                                r = unboxOperand(tmpState, Float.class, unboxMap, r, newInstrs);
                                a = unboxOperand(tmpState, Float.class, unboxMap, a, newInstrs);
                                newInstrs.add(new AluInstr(unboxedOp, unboxedDst, r, a));
                            } else if ((receiverType == Float.class || (receiverType == Fixnum.class && argType == Fixnum.class)) &&
                                    (unboxedOp = m.getUnboxedOp(Fixnum.class)) != null)
                            {
                                dirtied = true;

                                Class dstType = m.getUnboxedResultType(Fixnum.class);
                                setOperandType(tmpState, dst, dstType);
                                tmpState.unboxedVars.put(dst, dstType);

                                TemporaryLocalVariable unboxedDst = getUnboxedVar(dstType, unboxMap, dst);
                                r = unboxOperand(tmpState, Fixnum.class, unboxMap, r, newInstrs);
                                a = unboxOperand(tmpState, Fixnum.class, unboxMap, a, newInstrs);
                                newInstrs.add(new AluInstr(unboxedOp, unboxedDst, r, a));
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
                            UnboxableOpsAnalysisNode exitNode  = subProblem.getExitNode();

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
                tmpState.unboxedDirtyVars.add(dst);
            } else {
                // Since the instruction didn't run in unboxed form,
                // dirty unboxed vars will have to get boxed here.
                boxRequiredVars(i, tmpState, unboxMap, dst, hasExceptionsRescued(), hitDFBarrier, newInstrs);
            }
        }

        // Add unboxing instrs.
        if (!unboxedLiveVars) {
            for (Variable v: succUnboxedVars.keySet()) {
                if (liveVarsSet.get(lvp.getDFVar(v))) {
                    unboxVar(tmpState, succUnboxedVars.get(v), unboxMap, v, newInstrs);
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
