package org.jruby.runtime.ivars;

import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.VariableTableManager;
import org.jruby.runtime.builtin.IRubyObject;

public class VariableAccessor {
    protected final String name;
    protected final int index;
    protected final int classId;
    protected final RubyClass realClass;

    public VariableAccessor(RubyClass realClass, String name, int index, int classId) {
        this.index = index;
        this.classId = classId;
        this.name = name;
        this.realClass = realClass;
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
        return ((RubyBasicObject) object).getVariable(index);
    }

    public void set(Object object, Object value) {
        VariableTableManager.setVariableInternal(realClass, (RubyBasicObject)object, index, value);
    }
    public static final VariableAccessor DUMMY_ACCESSOR = new VariableAccessor(null, null, -1, -1);
    
}
