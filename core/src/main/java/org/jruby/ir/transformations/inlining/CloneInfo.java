package org.jruby.ir.transformations.inlining;

import java.util.HashMap;
import java.util.Map;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Variable;

/**
 * Base class for cloning context object.  Simple cloning and inline cloning both have
 * some common state and logic such as needing to maintain references to new constructed
 * replacement labels and variables.
 */
public abstract class CloneInfo {
    protected Map<Label, Label> labelRenameMap = new HashMap<>();
    protected Map<Variable, Variable> variableRenameMap = new HashMap<>();
    protected IRScope scope; // clone and inlining use this field differently.

    // Only used by subclasses
    protected CloneInfo(IRScope scope) {
        this.scope = scope;
    }

    public SimpleCloneInfo cloneForCloningClosure(IRClosure clonedClosure) {
        // If cloning for ensure block cloning we want to propagate that to child closure clones
        boolean ensureClone = this instanceof SimpleCloneInfo && ((SimpleCloneInfo) this).isEnsureBlockCloneMode();
        SimpleCloneInfo clone = new SimpleCloneInfo(clonedClosure, ensureClone);

        clone.variableRenameMap.putAll(variableRenameMap);

        return clone;
    }

    /**
     * @return The IRScope this cloning operation is happening in (or is coming from).
     */
    public IRScope getScope() {
        return scope;
    }

    protected abstract Label getRenamedLabelSimple(Label l);

    /**
     * Return a new instance of a label for the newly cloned scope.  Maps are maintained
     * because Labels expect to share the same instance across a CFG.
     *
     * @param label to be renamed.
     * @return the new Label
     */
    public Label getRenamedLabel(Label label) {
        if (Label.UNRESCUED_REGION_LABEL.equals(label)) return label; // Special case -- is there a way to avoid this?

        Label newLabel = this.labelRenameMap.get(label);
        if (newLabel == null) {
            newLabel = getRenamedLabelSimple(label);
            this.labelRenameMap.put(label, newLabel);
        }
        return newLabel;
    }

    /**
     * How do we rename %self?
     *
     * @param self to be renamed
     * @return the new self or itself
     */
    protected abstract Variable getRenamedSelfVariable(Variable self);

    /**
     * How are typical variables renamed if they were not yet found in the variable renaming map?
     *
     * @param variable to be renamed
     * @return the new variable
     */
    protected abstract Variable getRenamedVariableSimple(Variable variable);

    /**
     * Return a new instance of a variable for the newly cloned scope.  Maps are maintained
     * because Variables typically share the same instance accross a CFG (of the same lexical depth).
     *
     * @param variable to be renamed
     * @return the new Variable
     */
    public Variable getRenamedVariable(Variable variable) {
        if (variable instanceof Self) getRenamedSelfVariable(variable);

        Variable newVariable = variableRenameMap.get(variable);
        if (newVariable == null) {
            newVariable = getRenamedVariableSimple(variable);
            variableRenameMap.put(variable, newVariable);
        }

        return newVariable;
    }
}