package org.jruby.compiler.ir;

import java.util.HashMap;
import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.operands.Label;

public abstract class IRScopeImpl implements IRScope {
    private RubyModule containerModule; // Live version of container

    // ENEBO: These collections are initliazed on construction, but the rest
    //   are init()'d.  This can't be right can it?

    // Index values to guarantee we don't assign same internal index twice
    private int nextClosureIndex = 0;

    // Keeps track of types of prefix indexes for variables and labels
    private Map<String, Integer> nextVarIndex = new HashMap<String, Integer>();

    public RubyModule getContainerModule() {
//        System.out.println("GET: container module of " + getName() + " with hc " + hashCode() + " to " + containerModule.getName());
        return containerModule;
    }

    public int getNextClosureId() {
        nextClosureIndex++;

        return nextClosureIndex;
    }

    public Label getNewLabel(String prefix) {
        return new Label(prefix + "_" + allocateNextPrefixedName(prefix));
    }

    public Label getNewLabel() {
        return getNewLabel("LBL");
    }

    // Enebo: We should just make n primitive int and not take the hash hit
    protected int allocateNextPrefixedName(String prefix) {
        int index = getPrefixCountSize(prefix);
        
        nextVarIndex.put(prefix, index + 1);
        
        return index;
    }

	 protected void resetVariableCounter(String prefix) {
        nextVarIndex.remove(prefix);
	 }

    protected int getPrefixCountSize(String prefix) {
        Integer index = nextVarIndex.get(prefix);

        if (index == null) return 0;

        return index.intValue();
    }
}
