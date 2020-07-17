package org.jruby.ir.passes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jruby.ir.IRClosure;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;

public class LiveVariableAnalysis extends CompilerPass {

    private static final List<Class<? extends CompilerPass>> DEPENDENCIES =
        Collections.<Class<? extends CompilerPass>>singletonList(OptimizeDynScopesPass.class);

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public String getLabel() {
        return "Live Variable Analysis";
    }

    @Override
    public Object previouslyRun(FullInterpreterContext fic) {
        return fic.getDataFlowProblems().get(LiveVariablesProblem.NAME);
    }

    private void collectNonLocalDirtyVars(FullInterpreterContext fic, Set<LocalVariable> vars, int minDepth) {
        for (BasicBlock bb: fic.getCFG().getBasicBlocks()) {
            for (Instr i: bb.getInstrs()) {
                // Collect local vars belonging to an outer scope dirtied here
                if (i instanceof ResultInstr) {
                    Variable res = ((ResultInstr)i).getResult();
                    if (res instanceof LocalVariable && ((LocalVariable)res).getScopeDepth() > minDepth) {
                        vars.add((LocalVariable)res);
                    }
                }

                // When encountering nested closures, increase minDepth by 1
                // so that we continue to collect vars belong to outer scopes.
                if (i instanceof ClosureAcceptingInstr) {
                    Operand clArg = ((ClosureAcceptingInstr)i).getClosureArg();
                    if (clArg instanceof WrappedIRClosure) {
                        collectNonLocalDirtyVars(((WrappedIRClosure)clArg).getClosure().getFullInterpreterContext(), vars, minDepth+1);
                    }
                }
            }
        }
    }

    @Override
    public Object execute(FullInterpreterContext fic, Object... data) {
        LiveVariablesProblem lvp = new LiveVariablesProblem(fic);

        if (fic.getScope() instanceof IRClosure) {
            // We have to conservatively assume that any dirtied variables
            // that belong to an outer scope are live on exit.
            Set<LocalVariable> nlVars = new HashSet<LocalVariable>();

            collectNonLocalDirtyVars(fic, nlVars, fic.isDynamicScopeEliminated() ? -1 : 0);

            // Init DF vars from this set
            for (Variable v: nlVars) {
                lvp.addDFVar(v);
            }
            lvp.setVarsLiveOnScopeExit(nlVars);
        }

        lvp.compute_MOP_Solution();
        fic.getDataFlowProblems().put(LiveVariablesProblem.NAME, lvp);

        return lvp;
    }

    @Override
    public boolean invalidate(FullInterpreterContext fic) {
        super.invalidate(fic);
        fic.getDataFlowProblems().put(LiveVariablesProblem.NAME, null);
        return true;
    }
}
