package org.jruby.ir.passes;

import java.util.Set;

import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.dataflow.analyses.DefinedVariablesProblem;

public class AddMissingInitsPass extends CompilerPass {
    @Override
    public String getLabel() {
        return "AddMissingInitsPass";
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        // Make sure flags are computed
        scope.computeScopeFlags();

        // Find undefined vars
        DefinedVariablesProblem p = new DefinedVariablesProblem(scope);
        p.compute_MOP_Solution();
        Set<Variable> undefinedVars = p.findUndefinedVars();

        // Add inits to entry
        BasicBlock bb = scope.getCFG().getEntryBB();
        int i = 0;
        Variable first = null;
        for (Variable v : undefinedVars) {
            // System.out.println("Adding missing init for " + v + " in " + scope);
            if (first == null) {
                bb.getInstrs().add(i++, new CopyInstr(v, new Nil()));
                first = v;
            } else {
                bb.getInstrs().add(i++, new CopyInstr(v, first));
            }
        }

        return null;
    }
}
