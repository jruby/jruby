package org.jruby.ir.passes;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.instructions.ClosureAcceptingInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
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
    public Object previouslyRun(IRScope scope) {
        return scope.getLiveVariablesProblem();
    }

    private void collectNonLocalDirtyVars(IRClosure cl, Set<LocalVariable> vars, int minDepth) {
        for (BasicBlock bb: cl.getCFG().getBasicBlocks()) {
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
                        collectNonLocalDirtyVars(((WrappedIRClosure)clArg).getClosure(), vars, minDepth+1);
                    }
                }
            }
        }
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        // Make sure flags are computed
        scope.computeScopeFlags();

        LiveVariablesProblem lvp = new LiveVariablesProblem(scope);

        if (scope instanceof IRClosure) {
            // We have to conservatively assume that any dirtied variables
            // that belong to an outer scope are live on exit.
            Set<LocalVariable> nlVars = new HashSet<LocalVariable>();
            EnumSet<IRFlags> flags = scope.getExecutionContext().getFlags();

            collectNonLocalDirtyVars((IRClosure)scope, nlVars, flags.contains(IRFlags.DYNSCOPE_ELIMINATED) ? -1 : 0);

            // Init DF vars from this set
            for (Variable v: nlVars) {
                lvp.addDFVar(v);
            }
            lvp.setVarsLiveOnScopeExit(nlVars);
        }

        lvp.compute_MOP_Solution();
        scope.putLiveVariablesProblem(lvp);

        return lvp;
    }

    @Override
    public boolean invalidate(IRScope scope) {
        super.invalidate(scope);
        scope.putLiveVariablesProblem(null);
        return true;
    }
}
