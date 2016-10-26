package org.jruby.ir.dataflow.analyses;

import org.jruby.dirgra.Edge;
import org.jruby.ir.dataflow.FlowGraphNode;
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.representations.BasicBlock;

import java.util.*;

public class DefinedVariableNode extends FlowGraphNode<DefinedVariablesProblem, DefinedVariableNode> {
    public DefinedVariableNode(DefinedVariablesProblem prob, BasicBlock n) {
        super(prob, n);
    }

    @Override
    public void init() {
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
        setSize = problem.getDFVarsCount();
        in = null;
    }

    @Override
    public void compute_MEET(Edge e, DefinedVariableNode pred) {
        BitSet predState =  basicBlock.isRescueEntry() ? pred.in : pred.out;

        // If pred.out is TOP, in doesn't change.
        if (predState != null) {
            // if in is TOP, init in to a bitset with all 1's
            // so the intersection computes the right value.
            if (in == null) {
                // Make sure 'in' and 'out' are the same size!
                int n = predState.size();
                in = new BitSet(n);
                in.set(0, n);
            }

            in.and(predState);
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

    private void identifyUndefinedVarsInClosure(Set<Variable> undefinedVars, IRClosure cl, int nestingLevel) {
        int clBaseDepth = nestingLevel + (cl.getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE) ? 0 : 1);
        cl.setUpUseDefLocalVarMaps();
        for (LocalVariable lv: cl.getUsedLocalVariables()) {
            // This can happen where an outer scope variable
            // is not used in this scope but is used in a nested
            // scope. Ex: ~jruby/bin/ast:21
            if (problem.getDFVar(lv) == null) {
                continue;
            }

            // Find variables which belong to the problem.getScope()
            if (lv.getScopeDepth() == clBaseDepth && !tmp.get(problem.getDFVar(lv))) {
                // We want lv suitable for initializing in this scope
                undefinedVars.add(lv.getScopeDepth() == 0 ? lv : lv.cloneForDepth(0));
                tmp.set(problem.getDFVar(lv));
            }
        }

        // Recurse
        for (IRClosure nestedCl: cl.getClosures()) {
            identifyUndefinedVarsInClosure(undefinedVars, nestedCl, nestingLevel + 1);
        }
    }

    public void identifyInits(Set<Variable> undefinedVars) {
        int parentScopeDepth = problem.getScope().getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE) ? 0 : 1;

        initSolution();
        for (Instr i: basicBlock.getInstrs()) {
            for (Variable v: i.getUsedVariables()) {
                if (!v.isSelf()) {
                    if (v instanceof LocalVariable) {
                        LocalVariable lv = (LocalVariable) v;
                        // Variables that belong to outer scopes
                        // are considered already defined.
                        if (lv.getScopeDepth() < parentScopeDepth && !tmp.get(problem.getDFVar(v))) {
                            // We want lv suitable for initializing in this scope
                            undefinedVars.add(lv.getScopeDepth() == 0 ? lv : lv.cloneForDepth(0));
                        }
                        tmp.set(problem.getDFVar(lv));
                    } else if (v instanceof TemporaryLocalVariable) {
                        TemporaryLocalVariable tlv = (TemporaryLocalVariable) v;
                        if (!tmp.get(problem.getDFVar(v))) {
                            undefinedVars.add(tlv);
                        }
                        tmp.set(problem.getDFVar(tlv));
                    }
                }
            }

            if (i instanceof ClosureAcceptingInstr) {
                // Find all variables used in the closure and
                // figure out if they are defined are not.
                Operand o = ((ClosureAcceptingInstr)i).getClosureArg();
                if (o != null && o instanceof WrappedIRClosure) {
                    identifyUndefinedVarsInClosure(undefinedVars, ((WrappedIRClosure)o).getClosure(), 0);
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

    private String printSet(BitSet set) {
        StringBuilder buf = new StringBuilder();
        int count = 0;
        for (int i = 0; i < set.size(); i++) {
            if (set.get(i)) {
                count++;
                buf.append(' ').append(problem.getVariable(i));
                if (count % 10 == 0) buf.append("\t\n");
            }
        }

        if (count % 10 != 0) buf.append("\t\t");
        return buf.append('\n').toString();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\tVars defined on Entry: ");
        if (in == null) {
            System.out.println("-- NO in!");
        } else {
            buf.append(printSet(in));
        }

        buf.append("\n\tVars defined on Exit: ");
        if (out == null) {
            System.out.println("-- NO out!");
        } else {
            buf.append(printSet(out));
        }

        return buf.append('\n').toString();
    }

    private BitSet in;      // Variables defined at entry of this node
    private BitSet out;     // Variables defined at exit of node
    private BitSet tmp;     // Temporary state while applying transfer function
    private int setSize;    // Size of the "this.in" and "this.out" bit sets
}
