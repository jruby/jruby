package org.jruby.ir.passes;

import org.jruby.ir.IRScope;

import java.util.Arrays;
import java.util.List;

public class LinearizeCFG extends CompilerPass {
    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(CFGBuilder.class);

    @Override
    public String getLabel() {
        return "Linearize CFG";
    }

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        scope.buildLinearization();

        return null;
    }

    @Override
    public boolean invalidate(IRScope scope) {
        super.invalidate(scope);
        scope.resetLinearizationData();
        return true;
    }
}
