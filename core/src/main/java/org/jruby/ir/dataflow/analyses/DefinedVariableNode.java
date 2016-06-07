package org.jruby.ir.dataflow.analyses;

import org.jruby.dirgra.Edge;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.IRFlags;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class DefinedVariableNode extends FlowGraphNode<DefinedVariablesProblem, DefinedVariableNode> {
    public DefinedVariableNode(DefinedVariablesProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
        setSize = problem.getDFVarsCount();
        // 'null' acts as the TOP for this dataflow analysis
        out = null;
    }

    private void addDFVar(Variable v) {
        if (!problem.dfVarExists(v)) problem.addDFVar(v);
    }

    @Override
    public void buildDataFlowVars(Instr i) {
        if (i instanceof ResultInstr) addDFVar(((ResultInstr) i).getResult());
        for (Variable x: i.getUsedVariables()) {
            addDFVar(x);
        }
    }

    @Override
    public void applyPreMeetHandler() {
        // 'null' acts as the TOP for this dataflow analysis
        in = null;
    }

    @Override
    public void compute_MEET(Edge e, DefinedVariableNode pred) {
        // If pred.out is TOP, in doesn't change.
        if (pred.out != null) {
            // if in is TOP, init in to a bitset with all 1's
            // so the intersection computes the right value.
            if (in == null) {
                in = new BitSet(setSize);
                in.set(0, setSize);
            }

            in.and(pred.out);
        }
    }

    @Override
    public void initSolution() {
        tmp = in == null ? new BitSet(setSize) : (BitSet) in.clone();
    }

    @Override
    public void applyTransferFunction(Instr i) {
        // v is defined
        if (i instanceof ResultInstr) {
            tmp.set(problem.getDFVar(((ResultInstr) i).getResult()));
        }

        // Variables that belong to outer scopes should always
        // be considered defined.
        for (Variable v: i.getUsedVariables()) {
            if (v instanceof LocalVariable && ((LocalVariable)v).getScopeDepth() > 0) {
                tmp.set(problem.getDFVar(v));
            }
        }
    }

    public void identifyInits(Set<Variable> undefinedVars) {
        int parentScopeDepth = 1;
        if (problem.getScope().getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE)) {
            parentScopeDepth = 0;
        }

        initSolution();
        for (Instr i: basicBlock.getInstrs()) {
            // Variables that belong to outer scopes should always
            // be considered defined.
            for (Variable v: i.getUsedVariables()) {
                if (!v.isSelf()) {
                    if (v instanceof LocalVariable && ((LocalVariable)v).getScopeDepth() >= parentScopeDepth) {
                        tmp.set(problem.getDFVar(v));
                    }

                    if (!tmp.get(problem.getDFVar(v))) {
                        // System.out.println("Variable " + v + " in instr " + i + " isn't defined!");
                        undefinedVars.add(v);
                    }
                }
            }

            // v is defined
            if (i instanceof ResultInstr) {
                tmp.set(problem.getDFVar(((ResultInstr) i).getResult()));
            }
        }
    }

    @Override
    public boolean solutionChanged() {
        return !tmp.equals(out);
    }

    @Override
    public void finalizeSolution() {
        out = tmp;
    }

    private BitSet in;      // Variables defined at entry of this node
    private BitSet out;     // Variables defined at exit of node
    private BitSet tmp;  // Temporary state while applying transfer function
    private int setSize;    // Size of the "this.in" and "this.out" bit sets
}
