package org.jruby.runtime.component;

import org.jruby.runtime.builtin.Variable;
import org.jruby.util.IdUtil;

public class VariableEntry<BaseObjectType> implements Variable<BaseObjectType> {
    public final String name;
    public final BaseObjectType value;
    
    public VariableEntry(String name, BaseObjectType value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }
    
    public BaseObjectType getValue() {
        return value;
    }
    
    public boolean isClassVariable() {
        return IdUtil.isClassVariable(name);
    }
    
    public boolean isConstant() {
        return IdUtil.isConstant(name);
    }
    
    public boolean isInstanceVariable() {
        return IdUtil.isInstanceVariable(name);
    }

    public boolean isRubyVariable() {
        char c;
        return name.length() > 0 && ((c = name.charAt(0)) == '@' || (c <= 'Z' && c >= 'A'));
    }

    @Override
    public String toString() {
        return "Name: " + getName();
    }
}
