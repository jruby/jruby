package org.jruby.runtime.ivars;

import org.jruby.runtime.builtin.IRubyObject;

public class VariableAccessor {
    private final String name;
    private final int index;
    private final int classId;

    public VariableAccessor(String name, int index, int classId) {
        this.index = index;
        this.classId = classId;
        this.name = name;
    }

    public int getClassId() {
        return classId;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public Object get(Object object) {
        return ((IRubyObject) object).getVariable(index);
    }

    public void set(Object object, Object value) {
        ((IRubyObject) object).setVariable(index, value);
    }
    public static final VariableAccessor DUMMY_ACCESSOR = new VariableAccessor(null, -1, -1);
    
}
