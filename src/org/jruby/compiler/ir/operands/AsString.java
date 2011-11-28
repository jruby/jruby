package org.jruby.compiler.ir.operands;

import java.util.List;
import java.util.Map;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AsString extends Operand {
    final private Operand source; 

    public AsString(Operand source) {
        if (source == null) source = new StringLiteral("");
        this.source = source;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return ((IRubyObject)source.retrieve(context, self, currDynScope, temp)).asString();
    }

    @Override
    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap, boolean force) {
        Operand newSource = source.getSimplifiedOperand(valueMap, force);
        return (newSource == source) ? this : new AsString(newSource);
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        source.addUsedVariables(l);
    }

    @Override
    public String toString() {
        return "#{" + source + "}";
    }
}
