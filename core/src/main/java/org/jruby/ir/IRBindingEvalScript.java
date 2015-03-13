package org.jruby.ir;

import org.jruby.EvalType;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;

/**
 * Explicit Binding Evals must allow preserving values between evals.  They do this is a special hidden scope.
 */
public class IRBindingEvalScript extends IREvalScript {
    private IRScope nearestNonEvalScope;
    private int     nearestNonEvalScopeDepth;

    public IRBindingEvalScript(IRManager manager, IRScope lexicalParent, String fileName, int lineNumber, StaticScope staticScope, EvalType evalType) {
        super(manager, lexicalParent, fileName, lineNumber, staticScope, evalType);

        int n = 0;
        IRScope s = lexicalParent;
        while (s instanceof IRBindingEvalScript) {
            n++;
            s = s.getLexicalParent();
        }

        this.nearestNonEvalScope = s;
        this.nearestNonEvalScopeDepth = n;
        this.nearestNonEvalScope.initEvalScopeVariableAllocator(false);

        if (!getManager().isDryRun() && staticScope != null) {
            // SSS FIXME: This is awkward!
            if (evalType == EvalType.MODULE_EVAL) {
                staticScope.setScopeType(getScopeType());
            } else {
                staticScope.setScopeType(this.nearestNonEvalScope.getScopeType());
            }
        }

    }

    @Override
    public LocalVariable lookupExistingLVar(String name) {
        return nearestNonEvalScope.evalScopeVars.get(name);
    }

    @Override
    protected LocalVariable findExistingLocalVariable(String name, int scopeDepth) {
        // Look in the nearest non-eval scope's shared eval scope vars first.
        // If you dont find anything there, look in the nearest non-eval scope's regular vars.
        LocalVariable lvar = lookupExistingLVar(name);
        if (lvar != null || scopeDepth == 0) return lvar;
        else return nearestNonEvalScope.findExistingLocalVariable(name, scopeDepth-nearestNonEvalScopeDepth-1);
    }

    @Override
    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        // Reduce lookup depth by 1 since the AST seems to be adding
        // an additional static/dynamic scope for which there is no
        // corresponding IRScope.
        //
        // FIXME: Investigate if this is something left behind from
        // 1.8 mode support. Or if we need to introduce the additional
        // IRScope object.
        int lookupDepth = isModuleOrInstanceEval() ? scopeDepth - 1 : scopeDepth;
        LocalVariable lvar = findExistingLocalVariable(name, lookupDepth);
        if (lvar == null) lvar = getNewLocalVariable(name, lookupDepth);
        // Create a copy of the variable usable at the right depth
        if (lvar.getScopeDepth() != scopeDepth) lvar = lvar.cloneForDepth(scopeDepth);

        return lvar;
    }

    @Override
    public LocalVariable getNewLocalVariable(String name, int depth) {
        assert depth == nearestNonEvalScopeDepth: "Local variable depth in IREvalScript:getNewLocalVariable for " + name + " must be " + nearestNonEvalScopeDepth + ".  Got " + depth;
        LocalVariable lvar = new ClosureLocalVariable(name, 0, nearestNonEvalScope.evalScopeVars.size());
        nearestNonEvalScope.evalScopeVars.put(name, lvar);
        // CON: unsure how to get static scope to reflect this name as in IRClosure and IRMethod
        return lvar;
    }

    @Override
    public LocalVariable getNewFlipStateVariable() {
        String flipVarName = "%flip_" + allocateNextPrefixedName("%flip");
        LocalVariable v = lookupExistingLVar(flipVarName);
        if (v == null) {
            v = getNewLocalVariable(flipVarName, 0);
        }
        return v;
    }

    @Override
    public int getUsedVariablesCount() {
        return nearestNonEvalScope.evalScopeVars.size()+ getPrefixCountSize("%flip");
    }
}
